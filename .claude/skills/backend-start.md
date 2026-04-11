# backend-start

启动后端 Spring Boot 应用

## 使用方式

```
/backend-start [profile]
```

参数：

- `profile`: 启动配置，可选 `dev` 或 `prod`，默认 `dev`

## 功能说明

启动后端服务：

- dev profile: 使用 H2 内存数据库
- prod profile: 使用 PostgreSQL

## 执行步骤

### Step 1: 检查 8080 端口占用

```bash
netstat -ano | grep 8080
```

如果被占用，杀掉对应 PID 进程。

### Step 2: 启动后端

```bash
cd E:/workspace/ideaProjects/ai-agent-admin/admin-server-start && mvn spring-boot:run -Dspring-boot.run.profiles=<profile>
```

### Step 3: 验证启动成功

等待启动完成后检查：

```bash
curl -s http://localhost:8080/api/v1/models
```