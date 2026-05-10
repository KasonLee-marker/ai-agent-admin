# MCP Stdio Runtime - 单容器多进程模式
# 预装 Node.js、Python、uv，配置国内镜像源

FROM node:20-slim

# 代理配置（构建时传入）
ARG HTTP_PROXY
ARG HTTPS_PROXY
ARG NO_PROXY

ENV HTTP_PROXY=${HTTP_PROXY}
ENV http_proxy=${HTTP_PROXY}
ENV HTTPS_PROXY=${HTTPS_PROXY}
ENV https_proxy=${HTTPS_PROXY}
ENV NO_PROXY=${NO_PROXY}
ENV no_proxy=${NO_PROXY}

# apt 代理配置
RUN if [ -n "$HTTP_PROXY" ]; then \
        echo "Acquire::http::Proxy \"$HTTP_PROXY\";" >> /etc/apt/apt.conf.d/99proxy; \
        echo "Acquire::https::Proxy \"$HTTPS_PROXY\";" >> /etc/apt/apt.conf.d/99proxy; \
    fi

LABEL maintainer="AI Agent Admin"
LABEL description="MCP Server Runtime - 支持多 stdio MCP Server 进程"

# 安装系统依赖（apt 代理已通过 apt.conf 配置）
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    python3-venv \
    curl \
    ca-certificates \
    supervisor \
    && rm -rf /var/lib/apt/lists/*

# 配置国内镜像源
RUN npm config set registry https://registry.npmmirror.com

# 使用 pipx 安装 uv（避免系统 Python 污染）
RUN pip3 install --break-system-packages pipx && \
    pipx install uv

# 将 pipx 路径添加到 PATH
ENV PATH="/root/.local/bin:${PATH}"

# uv 使用国内镜像
ENV UV_DEFAULT_INDEX=https://pypi.tuna.tsinghua.edu.cn/simple

# 创建工作目录
WORKDIR /mcp-host

# 先复制依赖文件（利用 Docker 缓存层）
COPY requirements.txt .
RUN pip3 install --break-system-packages --no-cache-dir -r requirements.txt

# 复制 MCP Host 服务代码
COPY . .

# 创建必要的目录
RUN mkdir -p /var/log/mcp /var/run/mcp /mcp-servers

# 暴露 HTTP API 端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

# 启动 MCP Host Service
CMD ["python3", "main.py"]
