-- 安装 pgvector 扩展
-- 如果报错 "could not open extension control file"，说明需要先下载 pgvector DLL

CREATE
EXTENSION IF NOT EXISTS vector;

-- 验证安装
SELECT *
FROM pg_extension
WHERE extname = 'vector';

-- 查看 pgvector 版本
SELECT extname, extversion
FROM pg_extension
WHERE extname = 'vector';