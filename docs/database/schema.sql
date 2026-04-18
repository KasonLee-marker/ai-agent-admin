-- AI Agent Admin 数据库初始化脚本
-- PostgreSQL + pgvector
-- 适用于本地 Docker 环境
-- 最后更新: 2026-04-18

-- 启用向量扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 全文搜索更新函数
CREATE OR REPLACE FUNCTION public.update_content_tsv() RETURNS trigger
    LANGUAGE plpgsql
AS
$$
BEGIN
    NEW.content_tsv := to_tsvector('simple', COALESCE(NEW.content, ''));
    RETURN NEW;
END;
$$;

-- ==================== Prompt 管理模块 ====================

-- Prompt 模板表
CREATE TABLE IF NOT EXISTS prompt_templates (
                                                id         VARCHAR(255) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
                                                content    VARCHAR(5000) NOT NULL,
    description VARCHAR(1000),
    category VARCHAR(100),
    tags VARCHAR(500),
                                                version    INTEGER       NOT NULL,
    variables VARCHAR(1000),
                                                created_at TIMESTAMP(6)  NOT NULL,
                                                updated_at TIMESTAMP(6)  NOT NULL,
    created_by VARCHAR(100)
);

-- Prompt 版本历史表
CREATE TABLE IF NOT EXISTS prompt_versions (
                                               id         VARCHAR(255) PRIMARY KEY,
                                               prompt_id  VARCHAR(36)   NOT NULL REFERENCES prompt_templates (id) ON DELETE CASCADE,
    version INTEGER NOT NULL,
                                               content    VARCHAR(5000) NOT NULL,
                                               change_log VARCHAR(1000),
                                               created_at TIMESTAMP(6)  NOT NULL
);

-- ==================== 模型管理模块 ====================

-- 模型配置表
CREATE TABLE IF NOT EXISTS model_config
(
    id                   VARCHAR(64) PRIMARY KEY,
    name                 VARCHAR(100) NOT NULL,
    provider             VARCHAR(20)  NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    api_key VARCHAR(500),
    base_url VARCHAR(500),
    temperature          DOUBLE PRECISION,
    top_p                DOUBLE PRECISION,
    max_tokens           INTEGER,
    extra_params         VARCHAR(2000),
    is_default BOOLEAN DEFAULT FALSE,
    is_active            BOOLEAN,
    is_default_embedding BOOLEAN,
    embedding_dimension  INTEGER,
    embedding_table_name VARCHAR(50),
    health_status        VARCHAR(20),
    last_health_check    TIMESTAMP(6),
    created_at           TIMESTAMP(6),
    updated_at           TIMESTAMP(6),
    CONSTRAINT model_config_provider_check CHECK (provider IN
                                                  ('OPENAI', 'ANTHROPIC', 'SILICONFLOW', 'MOONSHOT', 'ZHIPU',
                                                   'DASHSCOPE', 'DEEPSEEK', 'OLLAMA', 'OPENAI_EMBEDDING',
                                                   'DASHSCOPE_EMBEDDING')),
    CONSTRAINT model_config_health_status_check CHECK (health_status IN ('HEALTHY', 'UNHEALTHY', 'UNKNOWN'))
);

-- ==================== 对话调试模块 ====================

-- 对话会话表
CREATE TABLE IF NOT EXISTS chat_sessions (
                                             id             VARCHAR(64) PRIMARY KEY,
                                             title          VARCHAR(200) NOT NULL,
                                             model_id       VARCHAR(64),
                                             prompt_id      VARCHAR(64),
                                             system_message VARCHAR(500),
                                             is_active      BOOLEAN      NOT NULL,
                                             message_count  INTEGER      NOT NULL,
                                             created_at     TIMESTAMP(6) NOT NULL,
                                             updated_at     TIMESTAMP(6) NOT NULL,
    created_by VARCHAR(100)
);

-- 对话消息表
CREATE TABLE IF NOT EXISTS chat_messages (
                                             id            VARCHAR(64) PRIMARY KEY,
                                             session_id    VARCHAR(64)    NOT NULL REFERENCES chat_sessions (id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
                                             content       VARCHAR(10000) NOT NULL,
                                             token_count   INTEGER,
                                             latency_ms    BIGINT,
                                             model_name    VARCHAR(100),
                                             error_message VARCHAR(1000),
                                             is_error      BOOLEAN,
                                             created_at    TIMESTAMP(6)   NOT NULL,
                                             CONSTRAINT chat_messages_role_check CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM'))
);

-- ==================== 数据集管理模块 ====================

-- 数据集表
CREATE TABLE IF NOT EXISTS datasets (
                                        id         VARCHAR(255) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    category VARCHAR(100),
    tags VARCHAR(500),
                                        version    INTEGER      NOT NULL,
                                        status     VARCHAR(255) NOT NULL,
                                        item_count INTEGER      NOT NULL,
    source_type VARCHAR(50),
    source_path VARCHAR(500),
                                        created_at TIMESTAMP(6) NOT NULL,
                                        updated_at TIMESTAMP(6) NOT NULL,
                                        created_by VARCHAR(100),
                                        CONSTRAINT datasets_status_check CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED', 'DELETED'))
);

-- 数据集项表
CREATE TABLE IF NOT EXISTS dataset_items (
                                             id               VARCHAR(255) PRIMARY KEY,
                                             dataset_id       VARCHAR(100)   NOT NULL REFERENCES datasets (id) ON DELETE CASCADE,
                                             version          INTEGER        NOT NULL,
                                             sequence         INTEGER        NOT NULL,
                                             input_data       VARCHAR(10000) NOT NULL,
                                             output_data      VARCHAR(10000),
                                             context_data     VARCHAR(5000),
                                             expected_doc_ids VARCHAR(500),
                                             metadata         VARCHAR(10000),
                                             status           VARCHAR(255)   NOT NULL,
                                             created_at       TIMESTAMP(6)   NOT NULL,
                                             updated_at       TIMESTAMP(6)   NOT NULL,
                                             CONSTRAINT dataset_items_status_check CHECK (status IN ('ACTIVE', 'DISABLED', 'DELETED'))
);

-- ==================== 评估系统模块 ====================

-- 评估任务表
CREATE TABLE IF NOT EXISTS evaluation_jobs (
                                               id                      VARCHAR(255) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
                                               prompt_template_id      VARCHAR(100) REFERENCES prompt_templates (id),
                                               prompt_template_version INTEGER,
                                               model_config_id         VARCHAR(100) REFERENCES model_config (id),
                                               dataset_id              VARCHAR(100) NOT NULL REFERENCES datasets (id),
                                               document_id             VARCHAR(100),
                                               enable_rag              BOOLEAN,
                                               embedding_model_id      VARCHAR(100),
                                               knowledge_base_id       VARCHAR(100),
                                               status                  VARCHAR(255) NOT NULL,
                                               total_items             INTEGER      NOT NULL,
                                               completed_items         INTEGER      NOT NULL,
                                               success_count           INTEGER      NOT NULL,
                                               failed_count            INTEGER      NOT NULL,
                                               total_latency_ms        BIGINT,
                                               total_input_tokens      BIGINT,
                                               total_output_tokens     BIGINT,
                                               started_at              TIMESTAMP(6),
                                               completed_at            TIMESTAMP(6),
                                               error_message           VARCHAR(2000),
                                               created_at              TIMESTAMP(6) NOT NULL,
                                               updated_at              TIMESTAMP(6) NOT NULL,
                                               created_by              VARCHAR(100),
                                               CONSTRAINT evaluation_jobs_status_check CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED'))
);

-- 评估结果表
CREATE TABLE IF NOT EXISTS evaluation_results (
                                                  id                  VARCHAR(255) PRIMARY KEY,
                                                  job_id              VARCHAR(100)   NOT NULL REFERENCES evaluation_jobs (id) ON DELETE CASCADE,
                                                  dataset_item_id     VARCHAR(100)   NOT NULL,
                                                  input_data          VARCHAR(10000) NOT NULL,
                                                  expected_output     VARCHAR(10000),
                                                  actual_output       VARCHAR(10000),
                                                  rendered_prompt     VARCHAR(5000),
    latency_ms INTEGER,
    input_tokens INTEGER,
    output_tokens INTEGER,
                                                  status              VARCHAR(255)   NOT NULL,
                                                  error_message       VARCHAR(2000),
                                                  score               REAL,
                                                  score_reason        VARCHAR(2000),
                                                  semantic_similarity REAL,
                                                  faithfulness        REAL,
                                                  retrieval_score     REAL,
                                                  retrieved_doc_ids   VARCHAR(500),
                                                  created_at          TIMESTAMP(6)   NOT NULL,
                                                  CONSTRAINT evaluation_results_status_check CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED'))
);

-- ==================== 知识库管理模块 ====================

-- 知识库表
CREATE TABLE IF NOT EXISTS knowledge_bases
(
    id                         VARCHAR(64) PRIMARY KEY,
    name                       VARCHAR(100) NOT NULL,
    description                VARCHAR(500),
    document_count             INTEGER,
    chunk_count                INTEGER,
    default_embedding_model_id VARCHAR(64),
    reindex_status             VARCHAR(20),
    reindex_started_at         TIMESTAMP(6),
    reindex_completed_at       TIMESTAMP(6),
    reindex_progress_current   INTEGER,
    reindex_progress_total     INTEGER,
    reindex_error_message      VARCHAR(500),
    created_at                 TIMESTAMP(6) NOT NULL,
    updated_at                 TIMESTAMP(6) NOT NULL,
    created_by                 VARCHAR(100),
    CONSTRAINT knowledge_bases_reindex_status_check CHECK (reindex_status IN ('NONE', 'IN_PROGRESS', 'COMPLETED', 'FAILED'))
);

-- ==================== 文档检索/RAG模块 ====================

-- 文档表
CREATE TABLE IF NOT EXISTS documents (
                                         id                        VARCHAR(64) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    content_type VARCHAR(50),
    file_size BIGINT,
    file_path VARCHAR(500),
                                         status                    VARCHAR(20)  NOT NULL,
                                         total_chunks              INTEGER,
                                         chunks_created            INTEGER,
                                         chunks_embedded           INTEGER,
    embedding_model VARCHAR(100),
                                         embedding_model_id        VARCHAR(64),
                                         embedding_model_name      VARCHAR(100),
                                         embedding_dimension       INTEGER,
                                         chunk_strategy            VARCHAR(20),
                                         chunk_size                INTEGER,
                                         chunk_overlap             INTEGER,
                                         knowledge_base_id         VARCHAR(64),
                                         semantic_progress_current INTEGER,
                                         semantic_progress_total   INTEGER,
    error_message TEXT,
                                         created_at                TIMESTAMP(6) NOT NULL,
                                         updated_at                TIMESTAMP(6) NOT NULL,
                                         created_by                VARCHAR(100),
                                         CONSTRAINT documents_status_check CHECK (status IN
                                                                                  ('PROCESSING', 'SEMANTIC_PROCESSING',
                                                                                   'CHUNKED', 'EMBEDDING', 'COMPLETED',
                                                                                   'FAILED', 'DELETED'))
);

-- 文档分块表（文本存储）
CREATE TABLE IF NOT EXISTS document_chunks (
                                               id          VARCHAR(64) PRIMARY KEY,
                                               document_id VARCHAR(64)  NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
                                               content_tsv TSVECTOR,
                                               embedding   TEXT,
                                               metadata    TEXT,
                                               created_at  TIMESTAMP(6) NOT NULL
);

-- 文档向量表（1024维向量存储）
CREATE TABLE IF NOT EXISTS document_embeddings_1024
(
    chunk_id    VARCHAR(64) PRIMARY KEY,
    document_id VARCHAR(64) NOT NULL,
    embedding   VECTOR(1024),
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ==================== RAG 对话模块 ====================

-- RAG 会话表
CREATE TABLE IF NOT EXISTS rag_sessions
(
    id                 VARCHAR(64) PRIMARY KEY,
    title              VARCHAR(200),
    model_id           VARCHAR(64),
    embedding_model_id VARCHAR(64),
    knowledge_base_id  VARCHAR(64),
    message_count      INTEGER,
    created_at         TIMESTAMP(6) NOT NULL,
    updated_at         TIMESTAMP(6) NOT NULL,
    created_by         VARCHAR(100)
);

-- RAG 对话消息表
CREATE TABLE IF NOT EXISTS rag_messages
(
    id            VARCHAR(64) PRIMARY KEY,
    session_id    VARCHAR(64)  NOT NULL REFERENCES rag_sessions (id) ON DELETE CASCADE,
    role          VARCHAR(20)  NOT NULL,
    content       TEXT         NOT NULL,
    sources       TEXT,
    latency_ms    BIGINT,
    model_name    VARCHAR(100),
    error_message VARCHAR(1000),
    is_error      BOOLEAN,
    created_at    TIMESTAMP(6) NOT NULL,
    CONSTRAINT rag_messages_role_check CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM'))
);

-- ==================== 索引 ====================

-- Prompt 索引
CREATE INDEX IF NOT EXISTS idx_prompt_category ON prompt_templates(category);

-- 模型配置索引
CREATE INDEX IF NOT EXISTS idx_model_provider ON model_config (provider);
CREATE INDEX IF NOT EXISTS idx_model_active ON model_config (is_active);

-- 对话索引
CREATE INDEX IF NOT EXISTS idx_chat_session_status ON chat_sessions (is_active);
CREATE INDEX IF NOT EXISTS idx_chat_messages_session ON chat_messages(session_id);
CREATE INDEX IF NOT EXISTS idx_session_created ON chat_messages (session_id, created_at);
CREATE INDEX IF NOT EXISTS idx_session_id ON chat_messages (session_id);

-- 数据集索引
CREATE INDEX IF NOT EXISTS idx_dataset_status ON datasets(status);
CREATE INDEX IF NOT EXISTS idx_dataset_category ON datasets(category);
CREATE INDEX IF NOT EXISTS idx_dataset_id ON dataset_items (dataset_id);
CREATE INDEX IF NOT EXISTS idx_dataset_version ON dataset_items (dataset_id, version);

-- 评估索引
CREATE INDEX IF NOT EXISTS idx_job_status ON evaluation_jobs (status);
CREATE INDEX IF NOT EXISTS idx_job_dataset ON evaluation_jobs (dataset_id);
CREATE INDEX IF NOT EXISTS idx_job_created_at ON evaluation_jobs (created_at);
CREATE INDEX IF NOT EXISTS idx_result_job_id ON evaluation_results (job_id);
CREATE INDEX IF NOT EXISTS idx_result_dataset_item ON evaluation_results (dataset_item_id);
CREATE INDEX IF NOT EXISTS idx_result_status ON evaluation_results (status);

-- 文档索引
CREATE INDEX IF NOT EXISTS idx_documents_status ON documents(status);
CREATE INDEX IF NOT EXISTS idx_document_chunks_document ON document_chunks(document_id);
CREATE INDEX IF NOT EXISTS idx_document_chunks_tsv ON document_chunks USING GIN (content_tsv);
CREATE INDEX IF NOT EXISTS idx_document_embeddings_1024_document_id ON document_embeddings_1024 (document_id);

-- 向量索引（用于相似度搜索）
CREATE INDEX IF NOT EXISTS idx_document_embeddings_1024_embedding ON document_embeddings_1024 USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- RAG 索引
CREATE INDEX IF NOT EXISTS idx_rag_session_id ON rag_messages (session_id);
CREATE INDEX IF NOT EXISTS idx_rag_session_created ON rag_messages (session_id, created_at);

-- ==================== 触发器 ====================

-- 文档分块全文搜索自动更新触发器
CREATE TRIGGER trg_update_content_tsv
    BEFORE INSERT OR UPDATE
    ON document_chunks
    FOR EACH ROW
EXECUTE FUNCTION update_content_tsv();
