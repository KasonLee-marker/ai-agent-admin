-- 创建 ai-agent-admin 专用数据库用户
-- 执行方式: docker exec -i agent-postgres psql -U adminuser -d admindb < create_user.sql

-- 创建新用户
CREATE
USER adminuser WITH PASSWORD 'adminpass123';

-- 授予数据库权限
GRANT ALL PRIVILEGES ON DATABASE
admindb TO adminuser;

-- 授予 schema 权限
GRANT USAGE ON SCHEMA
public TO adminuser;
GRANT CREATE
ON SCHEMA public TO adminuser;

-- 授予所有表权限
GRANT ALL PRIVILEGES ON ALL
TABLES IN SCHEMA public TO adminuser;

-- 授予序列权限
GRANT ALL PRIVILEGES ON ALL
SEQUENCES IN SCHEMA public TO adminuser;

-- 设置默认权限，以后新建的表自动授权
ALTER
DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO adminuser;
ALTER
DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO adminuser;

-- 验证
SELECT 'User adminuser created successfully' AS status;
