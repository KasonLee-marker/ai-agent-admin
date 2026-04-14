# frontend-start

启动前端开发服务器（端口 5173）

## 使用方式

```
/frontend-start
```

## 功能说明

智能启动前端开发服务器：

1. 检查 5173 端口是否被占用
2. 如果被占用，杀掉占用进程
3. 启动 `npm run dev`
4. **只维持 5173 端口运行**，其他端口（5174、5175等）如有启动需杀掉

## 重要规则

**只维持 5173 端口启动前端，端口冲突就杀掉！**

- 前端只需一个窗口运行
- 如果 Vite 自动切换到 5174、5175 等端口，说明这些是多余的，需要杀掉
- 只保留 5173 端口的进程

## 执行步骤

### Step 1: 检查端口占用

```bash
netstat -ano | grep 5173
```

如果输出中有进程 PID，记录下来。

### Step 2: 杀掉占用 5173 端口的进程（如果存在）

如果 Step 1 找到 PID，执行：

```bash
taskkill //PID <PID> //F
```

注意：只杀掉占用 5173 端口的特定进程，不要杀掉所有 node 进程。

### Step 3: 启动前端

```bash
cd E:/workspace/ideaProjects/ai-agent-admin/frontend && npm run dev
```

在后台运行此命令。

### Step 4: 杀掉其他多余端口

检查是否有 5174、5175 等端口启动：

```bash
netstat -ano | grep -E "5174|5175" | grep LISTENING
```

如果有，杀掉这些进程：

```bash
taskkill //F //PID <多余的PID>
```

### Step 5: 验证启动成功

等待 3 秒后检查：

```bash
curl -s http://localhost:5173 | head -5
```

如果返回 HTML 内容，说明启动成功。

## 注意事项

- 不要使用 `taskkill /F /IM node.exe`，这会杀掉所有 node 进程
- 只针对特定端口（5173）的进程进行清理
- 启动命令使用 `run_in_background` 参数
- **前端只需一个窗口，不要启动多个端口**