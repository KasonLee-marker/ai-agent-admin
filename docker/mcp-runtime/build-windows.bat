@echo off
REM MCP Runtime Docker 构建脚本 - Windows 版本
REM 用法: build-windows.bat [tag]

setlocal enabledelayedexpansion

REM 默认标签
if "%~1"=="" (
    set TAG=latest
) else (
    set TAG=%~1
)

set IMAGE_NAME=ai-agent-admin/mcp-runtime

echo ==========================================
echo   MCP Runtime Docker 构建脚本 (Windows)
echo ==========================================
echo.

REM 检查 Docker
echo [1/5] 检查 Docker...
docker --version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Docker 未安装
    exit /b 1
)
for /f "tokens=*" %%a in ('docker --version') do set DOCKER_VERSION=%%a
echo [OK] Docker 版本: %DOCKER_VERSION%

REM 检查 Docker 守护进程
echo [2/5] 检查 Docker 守护进程...
docker info >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Docker 守护进程未运行
    echo 请先启动 Docker Desktop
    exit /b 1
)
echo [OK] Docker 守护进程运行中

REM 准备构建环境
echo [3/5] 准备构建环境...
cd /d "%~dp0"
echo [OK] 工作目录: %CD%

REM 检查必要文件
echo [4/5] 检查必要文件...
if not exist "Dockerfile.windows" (
    echo [ERROR] Dockerfile.windows 不存在
    exit /b 1
)

if not exist "main.py" (
    echo [ERROR] main.py 不存在
    exit /b 1
)

if not exist "requirements.txt" (
    echo [ERROR] requirements.txt 不存在
    exit /b 1
)

echo [OK] 所有必要文件存在

REM 构建镜像
echo [5/5] 构建 Docker 镜像...
echo [INFO] 镜像名称: %IMAGE_NAME%:%TAG%
echo.

REM 启用 BuildKit
set DOCKER_BUILDKIT=1

REM 构建镜像（使用国内镜像源）
docker build ^
    -t %IMAGE_NAME%:%TAG% ^
    -f Dockerfile.windows ^
    .

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ==========================================
    echo   构建成功!
    echo ==========================================
    echo.
    echo 镜像信息:
    echo   名称: %IMAGE_NAME%:%TAG%
    for /f "tokens=*" %%a in ('docker images -q %IMAGE_NAME%:%TAG%') do echo   ID: %%a
    echo.
    echo 运行命令:
    echo   docker run -d ^
    echo     -p 18080:8080 ^
    echo     -v //var/run/docker.sock:/var/run/docker.sock ^
    echo     -v mcp-packages:/mcp-servers ^
    echo     --name mcp-runtime ^
    echo     %IMAGE_NAME%:%TAG%
    echo.
    echo 查看日志:
    echo   docker logs -f mcp-runtime
    echo.
) else (
    echo ==========================================
    echo   构建失败!
    echo ==========================================
    echo.
    echo 故障排除:
    echo 1. 检查 Docker Desktop 是否运行
    echo 2. 尝试: docker pull registry.cn-hangzhou.aliyuncs.com/library/node:20-slim
    echo 3. 检查网络连接
    echo.
    exit /b 1
)
