-- 创建 ai-agent-admin 专用数据库用户
-- 执行方式: docker exec -i agentx-postgres psql -U agentx -d aiagent < create_user.sql

-- 创建新用户
CREATE USER aiagent_admin WITH PASSWORD 'AiAgent@2026';

-- 授予数据库权限
GRANT ALL PRIVILEGES ON DATABASE aiagent TO aiagent_admin;

-- 授予 schema 权限
GRANT USAGE ON SCHEMA public TO aiagent_admin;
GRANT CREATE ON SCHEMA public TO aiagent_admin;

-- 授予所有表权限
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO aiagent_admin;

-- 授予序列权限
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO aiagent_admin;

-- 设置默认权限，以后新建的表自动授权
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO aiagent_admin;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO aiagent_admin;

-- 验证
SELECT 'User aiagent_admin created successfully' AS status;
