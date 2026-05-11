#!/bin/bash
# MCP Runtime Docker 构建脚本 - Linux/macOS 版本
# 用法: ./build-linux.sh [tag]

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 默认标签
TAG=${1:-latest}
IMAGE_NAME="ai-agent-admin/mcp-runtime"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  MCP Runtime Docker 构建脚本 (Linux)${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# 检查 Docker
echo -e "${YELLOW}[1/5] 检查 Docker...${NC}"
if ! command -v docker &> /dev/null; then
    echo -e "${RED}错误: Docker 未安装${NC}"
    exit 1
fi

DOCKER_VERSION=$(docker --version | grep -oP '\d+\.\d+\.\d+' || echo "未知")
echo -e "${GREEN}✓ Docker 版本: $DOCKER_VERSION${NC}"

# 检查 Docker 守护进程
echo -e "${YELLOW}[2/5] 检查 Docker 守护进程...${NC}"
if ! docker info &> /dev/null; then
    echo -e "${RED}错误: Docker 守护进程未运行${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Docker 守护进程运行中${NC}"

# 获取脚本所在目录
echo -e "${YELLOW}[3/5] 准备构建环境...${NC}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo -e "${GREEN}✓ 工作目录: $SCRIPT_DIR${NC}"

# 检查必要文件
echo -e "${YELLOW}[4/5] 检查必要文件...${NC}"
if [ ! -f "Dockerfile" ]; then
    echo -e "${RED}错误: Dockerfile 不存在${NC}"
    exit 1
fi

if [ ! -f "main.py" ]; then
    echo -e "${RED}错误: main.py 不存在${NC}"
    exit 1
fi

if [ ! -f "requirements.txt" ]; then
    echo -e "${RED}错误: requirements.txt 不存在${NC}"
    exit 1
fi

echo -e "${GREEN}✓ 所有必要文件存在${NC}"

# 构建镜像
echo -e "${YELLOW}[5/5] 构建 Docker 镜像...${NC}"
echo -e "${GREEN}→ 镜像名称: $IMAGE_NAME:$TAG${NC}"
echo ""

# 使用 BuildKit 加速构建
export DOCKER_BUILDKIT=1

# 构建镜像（使用国内镜像源）
docker build \
    --build-arg HTTP_PROXY="${HTTP_PROXY:-}" \
    --build-arg HTTPS_PROXY="${HTTPS_PROXY:-}" \
    --build-arg NO_PROXY="${NO_PROXY:-}" \
    -t "$IMAGE_NAME:$TAG" \
    -f Dockerfile \
    .

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}  构建成功!${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo -e "镜像信息:"
    echo -e "  名称: ${GREEN}$IMAGE_NAME:$TAG${NC}"
    echo -e "  ID: ${GREEN}$(docker images -q $IMAGE_NAME:$TAG | head -1)${NC}"
    echo ""
    echo -e "运行命令:"
    echo -e "  ${YELLOW}docker run -d \\\n${NC}"
    echo -e "    ${YELLOW}-p 18080:8080 \\\n${NC}"
    echo -e "    ${YELLOW}-v /var/run/docker.sock:/var/run/docker.sock \\\n${NC}"
    echo -e "    ${YELLOW}-v mcp-packages:/mcp-servers \\\n${NC}"
    echo -e "    ${YELLOW}--name mcp-runtime \\\n${NC}"
    echo -e "    ${YELLOW}$IMAGE_NAME:$TAG${NC}"
    echo ""
    echo -e "查看日志:"
    echo -e "  ${YELLOW}docker logs -f mcp-runtime${NC}"
    echo ""
else
    echo -e "${RED}========================================${NC}"
    echo -e "${RED}  构建失败!${NC}"
    echo -e "${RED}========================================${NC}"
    exit 1
fi
