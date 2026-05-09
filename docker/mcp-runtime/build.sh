#!/bin/bash
# Build MCP Runtime Docker image

IMAGE_NAME="ai-agent-admin/mcp-runtime"
TAG=${1:-latest}

echo "Building MCP Runtime image: ${IMAGE_NAME}:${TAG}"

cd "$(dirname "$0")"

docker build -t ${IMAGE_NAME}:${TAG} .

if [ $? -eq 0 ]; then
    echo "✅ Build successful: ${IMAGE_NAME}:${TAG}"
    echo ""
    echo "Run with:"
    echo "  docker run -d -p 8080:8080 --name mcp-runtime ${IMAGE_NAME}:${TAG}"
else
    echo "❌ Build failed"
    exit 1
fi
