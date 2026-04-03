-- AI Agent Admin 数据库初始化脚本
-- PostgreSQL + pgvector
-- 适用于本地 Docker 环境

-- 启用向量扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- ==================== Prompt 管理模块 ====================

-- Prompt 模板表
CREATE TABLE IF NOT EXISTS prompt_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    description VARCHAR(1000),
    category VARCHAR(100),
    tags VARCHAR(500),
    version INTEGER DEFAULT 1,
    variables VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100)
);

-- Prompt 版本历史表
CREATE TABLE IF NOT EXISTS prompt_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    prompt_id UUID NOT NULL REFERENCES prompt_templates(id) ON DELETE CASCADE,
    version INTEGER NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100)
);

-- ==================== 模型管理模块 ====================

-- 模型配置表
CREATE TABLE IF NOT EXISTS model_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    api_key VARCHAR(500),
    base_url VARCHAR(500),
    temperature DECIMAL(3,2) DEFAULT 0.7,
    max_tokens INTEGER DEFAULT 2048,
    is_default BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100)
);

-- ==================== 对话调试模块 ====================

-- 对话会话表
CREATE TABLE IF NOT EXISTS chat_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(200),
    model_config_id UUID REFERENCES model_configs(id),
    system_prompt TEXT,
    temperature DECIMAL(3,2),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100)
);

-- 对话消息表
CREATE TABLE IF NOT EXISTS chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    tokens INTEGER,
    latency_ms INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==================== 数据集管理模块 ====================

-- 数据集表
CREATE TABLE IF NOT EXISTS datasets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    category VARCHAR(100),
    tags VARCHAR(500),
    version INTEGER DEFAULT 1,
    status VARCHAR(20) DEFAULT 'DRAFT',
    item_count INTEGER DEFAULT 0,
    source_type VARCHAR(50),
    source_path VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100)
);

-- 数据集项表
CREATE TABLE IF NOT EXISTS dataset_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dataset_id UUID NOT NULL REFERENCES datasets(id) ON DELETE CASCADE,
    version INTEGER DEFAULT 1,
    sequence INTEGER DEFAULT 0,
    input_data TEXT NOT NULL,
    output_data TEXT,
    metadata TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==================== 文档检索/RAG模块 ====================

-- 文档表
CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    content TEXT,
    file_type VARCHAR(50),
    file_size BIGINT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    embedding vector(1536),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100)
);

-- ==================== 索引 ====================

CREATE INDEX IF NOT EXISTS idx_prompt_category ON prompt_templates(category);
CREATE INDEX IF NOT EXISTS idx_prompt_tags ON prompt_templates USING gin(tags gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_model_provider ON model_configs(provider);
CREATE INDEX IF NOT EXISTS idx_model_active ON model_configs(is_active);

CREATE INDEX IF NOT EXISTS idx_chat_session_status ON chat_sessions(status);
CREATE INDEX IF NOT EXISTS idx_chat_messages_session ON chat_messages(session_id);

CREATE INDEX IF NOT EXISTS idx_dataset_status ON datasets(status);
CREATE INDEX IF NOT EXISTS idx_dataset_category ON datasets(category);
CREATE INDEX IF NOT EXISTS idx_dataset_items_dataset ON dataset_items(dataset_id);
CREATE INDEX IF NOT EXISTS idx_dataset_items_version ON dataset_items(dataset_id, version);

CREATE INDEX IF NOT EXISTS idx_documents_status ON documents(status);

-- 向量索引（用于相似度搜索）
CREATE INDEX IF NOT EXISTS idx_documents_embedding ON documents USING ivfflat (embedding vector_cosine_ops);

-- ==================== 初始数据 ====================

-- 插入默认模型配置（百炼 API）
INSERT INTO model_configs (name, provider, model_name, api_key, base_url, is_default, is_active)
VALUES ('DashScope Qwen', 'DASHSCOPE', 'qwen3.5-omni-plus-2026-03-15', 
        'sk-852f050ac5514871b39b3e8d7ffcc490', 
        'https://dashscope.aliyuncs.com/compatible-mode/v1', 
        TRUE, TRUE)
ON CONFLICT DO NOTHING;
