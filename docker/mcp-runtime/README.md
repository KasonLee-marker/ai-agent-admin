# MCP Runtime Docker 镜像

单个容器运行多个 stdio MCP Server 进程的管理方案。

## 包含组件

- **Node.js 20** - 运行基于 npm/npx 的 MCP Server
- **Python 3.11** - 运行 Python MCP Server
- **uv** - 高性能 Python 包管理器
- **MCP Host Service** - FastAPI 进程管理器

## 国内镜像配置

- npm: https://registry.npmmirror.com
- pip: https://pypi.tuna.tsinghua.edu.cn/simple
- uv: https://pypi.tuna.tsinghua.edu.cn/simple

## 构建

```bash
./build.sh [tag]
```

## 运行

```bash
# 直接运行
docker run -d -p 8080:8080 --name mcp-runtime ai-agent-admin/mcp-runtime:latest

# 带数据卷（保留日志和进程状态）
docker run -d \
  -p 8080:8080 \
  -v mcp-logs:/var/log/mcp \
  -v mcp-servers:/mcp-servers \
  --name mcp-runtime \
  ai-agent-admin/mcp-runtime:latest
```

## API 接口

| 端点                     | 方法   | 说明               |
|------------------------|------|------------------|
| `/health`              | GET  | 健康检查             |
| `/servers`             | GET  | 列出所有进程           |
| `/servers/{id}/start`  | POST | 启动 MCP Server 进程 |
| `/servers/{id}/stop`   | POST | 停止进程             |
| `/servers/{id}/status` | GET  | 获取进程状态           |
| `/servers/{id}/logs`   | GET  | 获取进程日志           |
| `/servers/{id}/stats`  | GET  | 获取进程资源占用         |
| `/rpc/{id}`            | POST | 发送 JSON-RPC 请求   |

## 示例

```bash
# 启动 memory server
curl -X POST http://localhost:8080/servers/memory/start \
  -H "Content-Type: application/json" \
  -d '{"command": ["npx", "-y", "@modelcontextprotocol/server-memory"]}'

# 调用工具
curl -X POST http://localhost:8080/rpc/memory \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"tools/list","params":{},"id":1}'

# 查看日志
curl http://localhost:8080/servers/memory/logs?lines=50
```
