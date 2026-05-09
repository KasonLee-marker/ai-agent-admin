# MCP Runtime Docker 镜像构建指南

## 前提条件

1. Docker Desktop 已安装并运行
2. 配置了镜像加速器（中国境内必须）

## 配置 Docker 镜像加速

### Windows Docker Desktop

1. 打开 Docker Desktop → Settings → Docker Engine
2. 添加 registry-mirrors：

```json
{
  "registry-mirrors": [
    "https://docker.mirrors.ustc.edu.cn",
    "https://hub-mirror.c.163.com",
    "https://mirror.baidubce.com"
  ]
}
```

3. Apply & Restart

### 验证配置

```bash
docker info | findstr "Registry Mirrors"
```

## 构建镜像

### 方法 1：使用 build.bat（Windows）

```bash
cd docker/mcp-runtime
build.bat
```

### 方法 2：手动构建

```bash
cd docker/mcp-runtime

# 拉取基础镜像（使用加速器）
docker pull node:20-slim

# 构建 MCP Runtime 镜像
docker build -t ai-agent-admin/mcp-runtime:latest .
```

## 验证构建

```bash
# 查看镜像
docker images | findstr mcp-runtime

# 测试运行
docker run -d -p 18080:8080 --name mcp-runtime-test ai-agent-admin/mcp-runtime:latest

# 查看日志
docker logs mcp-runtime-test

# 测试 API
curl http://localhost:18080/health

# 停止并删除测试容器
docker stop mcp-runtime-test
docker rm mcp-runtime-test
```

## 常见问题

### 1. 构建时出现 "failed to authorize"

原因：无法访问 Docker Hub
解决：配置镜像加速器（见上文）

### 2. 构建时出现 "no such host"

原因：DNS 解析失败
解决：检查网络连接，或尝试使用手机热点

### 3. Windows 上使用 WSL2 后端

确保 WSL2 可以访问外网：

```bash
wsl -d docker-desktop
curl -v https://registry-1.docker.io/v2/
```

## 镜像说明

| 配置项  | 说明                     |
|------|------------------------|
| 基础镜像 | node:20-slim           |
| 预装软件 | Python 3, pip, uv, npm |
| 端口   | 8080（HTTP API）         |
| 工作目录 | /mcp-host              |
| 日志目录 | /var/log/mcp           |
| 数据目录 | /mcp-servers           |

## 国内镜像源配置

镜像内已配置：

- npm: https://registry.npmmirror.com
- pip: https://pypi.tuna.tsinghua.edu.cn/simple
- uv: https://pypi.tuna.tsinghua.edu.cn/simple
