"""
MCP Host Service - 容器内多进程 MCP Server 管理器

功能：
1. 启动/停止/管理多个 MCP Server 进程
2. JSON-RPC 请求路由到对应进程
3. 进程状态监控和日志收集
4. HTTP API 供外部调用
"""

import asyncio
import json
import logging
import os
import shutil
import signal
import subprocess
import sys
import threading
import time
from contextlib import asynccontextmanager
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Dict, List, Optional

import psutil
from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.responses import PlainTextResponse
from pydantic import BaseModel, Field

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler('/var/log/mcp/host.log', encoding='utf-8')
    ]
)
logger = logging.getLogger('mcp-host')

# 全局状态
processes: Dict[str, 'McpProcess'] = {}
process_lock = threading.RLock()
LOG_DIR = Path('/var/log/mcp')
LOG_DIR.mkdir(exist_ok=True)


@dataclass
class McpProcess:
    """MCP Server 进程封装"""
    server_id: str
    process: subprocess.Popen
    command: List[str]
    env: Dict[str, str]
    start_time: float = field(default_factory=time.time)
    request_id_counter: int = 0
    pending_requests: Dict[int, asyncio.Future] = field(default_factory=dict)
    reader_task: Optional[asyncio.Task] = None
    writer_task: Optional[asyncio.Task] = None
    lock: threading.RLock = field(default_factory=threading.RLock)

    @property
    def pid(self) -> int:
        return self.process.pid

    @property
    def is_running(self) -> bool:
        return self.process.poll() is None

    def next_request_id(self) -> int:
        with self.lock:
            self.request_id_counter += 1
            return self.request_id_counter


class StartServerRequest(BaseModel):
    """启动 MCP Server 请求"""
    command: List[str] = Field(..., description="启动命令，如 ['npx', '-y', 'server-memory']")
    env: Dict[str, str] = Field(default_factory=dict, description="环境变量")
    args: List[str] = Field(default_factory=list, description="额外参数（已废弃，合并到 command）")


class JsonRpcRequest(BaseModel):
    """JSON-RPC 请求"""
    jsonrpc: str = Field(default="2.0")
    method: str = Field(..., description="方法名")
    params: Optional[Dict[str, Any]] = Field(default=None)
    id: Optional[int] = Field(default=None)


class ServerStatusResponse(BaseModel):
    """服务器状态响应"""
    server_id: str
    status: str  # running, stopped, error
    pid: Optional[int]
    uptime_seconds: float
    command: List[str]


class ProcessStats(BaseModel):
    """进程统计信息"""
    cpu_percent: float
    memory_mb: float
    memory_percent: float


def setup_server_workspace(server_id: str) -> Path:
    """为每个 MCP Server 创建工作目录"""
    workspace = Path(f'/mcp-servers/{server_id}')
    workspace.mkdir(parents=True, exist_ok=True)
    return workspace


def get_log_file(server_id: str) -> Path:
    """获取日志文件路径"""
    return LOG_DIR / f'{server_id}.log'


async def read_process_output(server_id: str, proc: McpProcess):
    """持续读取进程 stdout，处理 JSON-RPC 响应"""
    log_file = get_log_file(server_id)

    try:
        while proc.is_running:
            try:
                # 非阻塞读取
                line = await asyncio.get_event_loop().run_in_executor(
                    None, proc.process.stdout.readline
                )
                if not line:
                    break

                line_str = line.decode('utf-8', errors='replace').strip()
                if not line_str:
                    continue

                # 记录到日志文件
                with open(log_file, 'a', encoding='utf-8') as f:
                    f.write(f'{line_str}\n')

                # 解析 JSON-RPC 响应
                try:
                    response = json.loads(line_str)
                    if 'id' in response and response['id'] is not None:
                        request_id = response['id']
                        if request_id in proc.pending_requests:
                            future = proc.pending_requests.pop(request_id)
                            if not future.done():
                                future.set_result(response)
                except json.JSONDecodeError:
                    logger.debug(f'[{server_id}] Non-JSON output: {line_str[:100]}')

            except Exception as e:
                logger.error(f'[{server_id}] Error reading output: {e}')
                break

    except Exception as e:
        logger.error(f'[{server_id}] Reader task error: {e}')
    finally:
        logger.info(f'[{server_id}] Reader task stopped')


async def write_process_input(server_id: str, proc: McpProcess):
    """处理进程输入（主要用于保持管道打开）"""
    # 这个任务主要是保持 stdin 管道打开
    # 实际写入由 call_rpc 方法直接完成
    while proc.is_running:
        await asyncio.sleep(1)


async def cleanup_stopped_processes():
    """后台任务：清理已停止的进程"""
    while True:
        await asyncio.sleep(30)
        with process_lock:
            for server_id, proc in list(processes.items()):
                if not proc.is_running:
                    logger.info(f'Cleaning up stopped process: {server_id}')
                    # 清理 pending requests
                    for req_id, future in proc.pending_requests.items():
                        if not future.done():
                            future.set_exception(Exception('Process stopped'))
                    processes.pop(server_id, None)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期管理"""
    logger.info('MCP Host Service starting...')

    # 启动清理任务
    cleanup_task = asyncio.create_task(cleanup_stopped_processes())

    yield

    # 关闭时清理
    logger.info('MCP Host Service shutting down...')
    cleanup_task.cancel()

    # 停止所有进程
    with process_lock:
        for server_id, proc in processes.items():
            logger.info(f'Stopping process: {server_id}')
            try:
                proc.process.terminate()
                await asyncio.wait_for(
                    asyncio.get_event_loop().run_in_executor(None, proc.process.wait),
                    timeout=5.0
                )
            except asyncio.TimeoutError:
                proc.process.kill()


app = FastAPI(
    title="MCP Host Service",
    description="Manage multiple MCP Server processes within a single container",
    version="1.0.0",
    lifespan=lifespan
)


@app.get('/health')
async def health_check():
    """健康检查"""
    return {'status': 'healthy', 'processes': len(processes)}


@app.post('/servers/{server_id}/start')
async def start_server(server_id: str, request: StartServerRequest):
    """启动一个新的 MCP Server 进程"""

    with process_lock:
        if server_id in processes:
            existing = processes[server_id]
            if existing.is_running:
                return {
                    'status': 'already_running',
                    'pid': existing.pid,
                    'message': f'Server {server_id} is already running'
                }
            else:
                # 清理旧的
                del processes[server_id]

        # 创建工作目录
        workspace = setup_server_workspace(server_id)

        # 合并命令
        full_command = request.command
        if request.args:
            full_command = full_command + request.args

        # 准备环境变量
        env = os.environ.copy()
        env.update(request.env)

        # 启动进程
        log_file = get_log_file(server_id)
        try:
            process = subprocess.Popen(
                full_command,
                stdin=subprocess.PIPE,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,  # 合并 stderr 到 stdout
                env=env,
                cwd=str(workspace),
                bufsize=1,  # 行缓冲
                universal_newlines=False
            )
        except Exception as e:
            logger.error(f'Failed to start process {server_id}: {e}')
            raise HTTPException(500, f'Failed to start process: {e}')

        # 创建 MCP 进程对象
        mcp_proc = McpProcess(
            server_id=server_id,
            process=process,
            command=full_command,
            env=request.env
        )

        # 启动读写任务
        mcp_proc.reader_task = asyncio.create_task(
            read_process_output(server_id, mcp_proc),
            name=f'reader-{server_id}'
        )
        mcp_proc.writer_task = asyncio.create_task(
            write_process_input(server_id, mcp_proc),
            name=f'writer-{server_id}'
        )

        processes[server_id] = mcp_proc

        logger.info(f'Started MCP Server {server_id} with PID {process.pid}')

        return {
            'status': 'started',
            'pid': process.pid,
            'command': full_command
        }


@app.post('/servers/{server_id}/stop')
async def stop_server(server_id: str, timeout: int = 10):
    """停止 MCP Server 进程"""

    with process_lock:
        if server_id not in processes:
            raise HTTPException(404, f'Server {server_id} not found')

        proc = processes[server_id]

        if not proc.is_running:
            del processes[server_id]
            return {'status': 'already_stopped'}

        # 发送终止信号
        try:
            proc.process.terminate()
            await asyncio.wait_for(
                asyncio.get_event_loop().run_in_executor(None, proc.process.wait),
                timeout=timeout
            )
            del processes[server_id]
            return {'status': 'stopped', 'pid': proc.pid}
        except asyncio.TimeoutError:
            proc.process.kill()
            await asyncio.get_event_loop().run_in_executor(None, proc.process.wait)
            del processes[server_id]
            return {'status': 'killed', 'pid': proc.pid}


@app.post('/rpc/{server_id}')
async def call_rpc(server_id: str, request: JsonRpcRequest):
    """发送 JSON-RPC 请求到指定 MCP Server"""

    with process_lock:
        if server_id not in processes:
            raise HTTPException(404, f'Server {server_id} not found')

        proc = processes[server_id]

        if not proc.is_running:
            raise HTTPException(503, f'Server {server_id} is not running')

    # 分配请求 ID
    request_id = proc.next_request_id()

    # 构建请求
    rpc_request = {
        'jsonrpc': request.jsonrpc,
        'method': request.method,
        'params': request.params or {},
        'id': request_id
    }

    request_json = json.dumps(rpc_request) + '\n'

    # 创建 Future 等待响应
    future = asyncio.get_event_loop().create_future()
    proc.pending_requests[request_id] = future

    try:
        # 发送请求
        proc.process.stdin.write(request_json.encode())
        proc.process.stdin.flush()

        # 等待响应（30秒超时）
        response = await asyncio.wait_for(future, timeout=30.0)
        return response

    except asyncio.TimeoutError:
        proc.pending_requests.pop(request_id, None)
        raise HTTPException(504, 'Request timeout')
    except Exception as e:
        proc.pending_requests.pop(request_id, None)
        raise HTTPException(500, f'RPC call failed: {e}')


@app.get('/servers/{server_id}/status')
async def get_server_status(server_id: str) -> ServerStatusResponse:
    """获取 MCP Server 进程状态"""

    with process_lock:
        if server_id not in processes:
            return ServerStatusResponse(
                server_id=server_id,
                status='not_found',
                pid=None,
                uptime_seconds=0,
                command=[]
            )

        proc = processes[server_id]

        if proc.is_running:
            uptime = time.time() - proc.start_time
            return ServerStatusResponse(
                server_id=server_id,
                status='running',
                pid=proc.pid,
                uptime_seconds=uptime,
                command=proc.command
            )
        else:
            return ServerStatusResponse(
                server_id=server_id,
                status='stopped',
                pid=proc.pid,
                uptime_seconds=0,
                command=proc.command
            )


@app.get('/servers/{server_id}/logs')
async def get_server_logs(server_id: str, lines: int = 100):
    """获取 MCP Server 日志"""

    log_file = get_log_file(server_id)

    if not log_file.exists():
        return {'logs': '', 'lines': 0}

    try:
        # 读取最后 N 行
        result = await asyncio.get_event_loop().run_in_executor(
            None,
            lambda: subprocess.run(
                ['tail', '-n', str(lines), str(log_file)],
                capture_output=True,
                text=True,
                encoding='utf-8',
                errors='replace'
            )
        )

        logs = result.stdout
        log_lines = logs.strip().split('\n') if logs else []

        return {
            'logs': logs,
            'lines': len(log_lines),
            'file': str(log_file)
        }
    except Exception as e:
        raise HTTPException(500, f'Failed to read logs: {e}')


@app.get('/servers/{server_id}/stats')
async def get_server_stats(server_id: str) -> Optional[ProcessStats]:
    """获取 MCP Server 进程资源统计"""

    with process_lock:
        if server_id not in processes:
            raise HTTPException(404, f'Server {server_id} not found')

        proc = processes[server_id]

        if not proc.is_running:
            raise HTTPException(503, f'Server {server_id} is not running')

        try:
            ps_proc = psutil.Process(proc.pid)
            with ps_proc.oneshot():
                cpu = ps_proc.cpu_percent(interval=0.1)
                mem_info = ps_proc.memory_info()
                mem_percent = ps_proc.memory_percent()

            return ProcessStats(
                cpu_percent=cpu,
                memory_mb=mem_info.rss / 1024 / 1024,
                memory_percent=mem_percent
            )
        except psutil.NoSuchProcess:
            raise HTTPException(404, f'Process {proc.pid} not found')


@app.get('/servers')
async def list_servers() -> List[ServerStatusResponse]:
    """列出所有 MCP Server 进程"""

    result = []
    with process_lock:
        for server_id, proc in processes.items():
            if proc.is_running:
                uptime = time.time() - proc.start_time
                result.append(ServerStatusResponse(
                    server_id=server_id,
                    status='running',
                    pid=proc.pid,
                    uptime_seconds=uptime,
                    command=proc.command
                ))
            else:
                result.append(ServerStatusResponse(
                    server_id=server_id,
                    status='stopped',
                    pid=proc.pid,
                    uptime_seconds=0,
                    command=proc.command
                ))

    return result


@app.delete('/servers/{server_id}')
async def delete_server(server_id: str):
    """删除 MCP Server（停止并清理）"""

    # 先停止
    try:
        await stop_server(server_id)
    except HTTPException:
        pass  # 可能已经不运行了

    # 清理工作目录和日志
    workspace = Path(f'/mcp-servers/{server_id}')
    log_file = get_log_file(server_id)

    if workspace.exists():
        shutil.rmtree(workspace)

    if log_file.exists():
        log_file.unlink()

    return {'status': 'deleted', 'server_id': server_id}


@app.get('/')
async def root():
    """根路径"""
    return {
        'service': 'MCP Host Service',
        'version': '1.0.0',
        'running_processes': len(processes)
    }


if __name__ == '__main__':
    import uvicorn
    uvicorn.run(
        'main:app',
        host='0.0.0.0',
        port=8080,
        log_level='info',
        access_log=True
    )
