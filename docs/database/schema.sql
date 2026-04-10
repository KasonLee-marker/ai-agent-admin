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

-- ==================== 评估系统模块 ====================

-- 评估任务表
CREATE TABLE IF NOT EXISTS evaluation_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    prompt_template_id UUID REFERENCES prompt_templates(id),
    model_config_id UUID REFERENCES model_configs(id),
    dataset_id UUID NOT NULL REFERENCES datasets(id),
    status VARCHAR(20) DEFAULT 'PENDING',
    total_items INTEGER DEFAULT 0,
    completed_items INTEGER DEFAULT 0,
    metrics JSONB,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100)
);

-- 评估结果表
CREATE TABLE IF NOT EXISTS evaluation_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES evaluation_jobs(id) ON DELETE CASCADE,
    dataset_item_id UUID REFERENCES dataset_items(id),
    input TEXT NOT NULL,
    expected_output TEXT,
    actual_output TEXT,
    latency_ms INTEGER,
    input_tokens INTEGER,
    output_tokens INTEGER,
    status VARCHAR(20) DEFAULT 'SUCCESS',
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==================== 文档检索/RAG模块 ====================

-- 文档表
CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    content_type VARCHAR(50),
    file_size BIGINT,
    file_path VARCHAR(500),
    total_chunks INTEGER DEFAULT 0,
    embedding_model VARCHAR(100),
    status VARCHAR(20) DEFAULT 'PROCESSING',
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100)
);

-- 文档分块表（向量存储）
CREATE TABLE IF NOT EXISTS document_chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    embedding VECTOR(1536),
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==================== 索引 ====================

CREATE INDEX IF NOT EXISTS idx_prompt_category ON prompt_templates(category);

CREATE INDEX IF NOT EXISTS idx_model_provider ON model_configs(provider);
CREATE INDEX IF NOT EXISTS idx_model_active ON model_configs(is_active);

CREATE INDEX IF NOT EXISTS idx_chat_session_status ON chat_sessions(status);
CREATE INDEX IF NOT EXISTS idx_chat_messages_session ON chat_messages(session_id);

CREATE INDEX IF NOT EXISTS idx_dataset_status ON datasets(status);
CREATE INDEX IF NOT EXISTS idx_dataset_category ON datasets(category);
CREATE INDEX IF NOT EXISTS idx_dataset_items_dataset ON dataset_items(dataset_id);
CREATE INDEX IF NOT EXISTS idx_dataset_items_version ON dataset_items(dataset_id, version);

CREATE INDEX IF NOT EXISTS idx_evaluation_jobs_status ON evaluation_jobs(status);
CREATE INDEX IF NOT EXISTS idx_evaluation_jobs_dataset ON evaluation_jobs(dataset_id);
CREATE INDEX IF NOT EXISTS idx_evaluation_results_job ON evaluation_results(job_id);

CREATE INDEX IF NOT EXISTS idx_documents_status ON documents(status);
CREATE INDEX IF NOT EXISTS idx_document_chunks_document ON document_chunks(document_id);

-- 向量索引（用于相似度搜索）
CREATE INDEX IF NOT EXISTS idx_document_chunks_embedding ON document_chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
