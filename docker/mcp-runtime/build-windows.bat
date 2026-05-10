@echo off
REM Build MCP Runtime Docker Image for Windows (with China mirrors)

echo ==========================================
echo Building MCP Runtime Docker Image
echo Using China mirrors for faster build
echo ==========================================
echo.

REM Get the script directory
cd /d "%~dp0"

REM Check if Docker is running
docker info >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Docker is not running or not installed!
    echo Please start Docker Desktop first.
    pause
    exit /b 1
)

REM Build the image with China mirror optimizations
echo [INFO] Building with China mirrors...
docker build -f Dockerfile.windows -t ai-agent-admin/mcp-runtime:latest .

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ==========================================
    echo Build Successful!
    echo ==========================================
    echo.
    echo Image: ai-agent-admin/mcp-runtime:latest
    echo.
    echo Run with:
    echo   docker run -d -p 18080:8080 --name mcp-runtime ai-agent-admin/mcp-runtime:latest
    echo.
    echo Or use:
    echo   docker run -d -p 18080:8080 --name mcp-runtime -v mcp-servers:/mcp-servers ai-agent-admin/mcp-runtime:latest
    echo.
) else (
    echo.
    echo ==========================================
    echo Build Failed!
    echo ==========================================
    echo.
    echo Troubleshooting:
    echo 1. Check Docker Desktop is running
    echo 2. Try: docker pull registry.cn-hangzhou.aliyuncs.com/library/node:20-slim
    echo 3. Check network connection
    echo.
    pause
    exit /b 1
)

pause
