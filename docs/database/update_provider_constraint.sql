-- 更新 model_config 表的 provider 检查约束
-- 添加新的 EMBEDDING 类型 Provider
-- 执行方式: docker exec agentx-postgres psql -U adminuser -d admindb -f /path/to/this/file.sql
-- 或直接执行下方 SQL

-- 删除旧的检查约束
ALTER TABLE model_config DROP CONSTRAINT IF EXISTS model_config_provider_check;

-- 创建新的检查约束，包含所有 Provider
-- CHAT 类型: OPENAI, ANTHROPIC, SILICONFLOW, MOONSHOT, ZHIPU, DASHSCOPE, DEEPSEEK, OLLAMA
-- EMBEDDING 类型: OPENAI_EMBEDDING, DASHSCOPE_EMBEDDING
ALTER TABLE model_config
    ADD CONSTRAINT model_config_provider_check
        CHECK (provider IN (
                            'OPENAI',
                            'ANTHROPIC',
                            'SILICONFLOW',
                            'MOONSHOT',
                            'ZHIPU',
                            'DASHSCOPE',
                            'DEEPSEEK',
                            'OLLAMA',
                            'OPENAI_EMBEDDING',
                            'DASHSCOPE_EMBEDDING'
            ));

-- 验证约束已更新
SELECT conname, pg_get_constraintdef(oid)
FROM pg_constraint
WHERE conrelid = 'model_config'::regclass AND conname = 'model_config_provider_check';