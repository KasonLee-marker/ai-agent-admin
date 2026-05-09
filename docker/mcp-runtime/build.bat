@echo off
REM Build MCP Runtime Docker image for Windows

echo ==========================================
echo Building MCP Runtime Docker Image
echo ==========================================
echo.

REM Get the script directory
cd /d "%~dp0"
cd ..

REM Build the image
docker build -t ai-agent-admin/mcp-runtime:latest .

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
) else (
    echo.
    echo ==========================================
    echo Build Failed!
    echo ==========================================
    exit /b 1
)

pause
