-- 安装必要的扩展
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- pg_jieba 需要 shared_preload_libraries 配置
ALTER SYSTEM SET shared_preload_libraries = 'pg_jieba';

-- 创建 pg_jieba 扩展
CREATE EXTENSION IF NOT EXISTS pg_jieba;

-- 创建中文搜索配置（如果 jieba parser 存在）
-- 注：需要在 shared_preload_libraries 生效后才能创建
DO
$$
    BEGIN
        IF EXISTS (SELECT 1 FROM pg_ts_parser WHERE prsname = 'jieba') THEN
            CREATE TEXT SEARCH CONFIGURATION jieba (PARSER = jieba);
            COMMENT ON TEXT SEARCH CONFIGURATION jieba IS 'Chinese text search configuration using jieba';
            ALTER TEXT SEARCH CONFIGURATION jieba ADD MAPPING FOR n,v,a,i,e,l WITH simple;
        END IF;
    EXCEPTION
        WHEN duplicate_object THEN NULL;
    END
$$;