-- Phase 1: Agent 和 Tool 表创建
-- 执行方式: docker exec agentx-postgres psql -U adminuser -d admindb -f /path/to/this.sql
-- 或启动应用时 JPA ddl-auto: update 会自动创建

-- Agent 表
CREATE TABLE IF NOT EXISTS agents
(
    id            VARCHAR(64) PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    description   TEXT,
    version       VARCHAR(20) DEFAULT '1.0.0',
    model_id      VARCHAR(64)  NOT NULL REFERENCES model_config (id),
    system_prompt TEXT,
    config        JSONB       DEFAULT '{
      "temperature": 0.7,
      "maxTokens": 4096
    }',
    status        VARCHAR(20) DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    created_at    TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_agents_status ON agents (status);
CREATE INDEX IF NOT EXISTS idx_agents_model_id ON agents (model_id);

-- Tool 表
CREATE TABLE IF NOT EXISTS tools
(
    id          VARCHAR(64) PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    description TEXT        NOT NULL,
    category    VARCHAR(30)          DEFAULT 'GENERAL',
    type        VARCHAR(20) NOT NULL DEFAULT 'BUILTIN' CHECK (type IN ('BUILTIN', 'CUSTOM', 'MCP')),
    schema      JSONB       NOT NULL,
    executor    VARCHAR(100),
    config      JSONB                DEFAULT '{}',
    created_at  TIMESTAMP            DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tools_type ON tools (type);
CREATE INDEX IF NOT EXISTS idx_tools_category ON tools (category);

-- Agent-Tool 绑定表
CREATE TABLE IF NOT EXISTS agent_tools
(
    id         VARCHAR(64) PRIMARY KEY,
    agent_id   VARCHAR(64) NOT NULL REFERENCES agents (id) ON DELETE CASCADE,
    tool_id    VARCHAR(64) NOT NULL REFERENCES tools (id) ON DELETE CASCADE,
    enabled    BOOLEAN   DEFAULT true,
    config     JSONB     DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (agent_id, tool_id)
);

CREATE INDEX IF NOT EXISTS idx_agent_tools_agent_id ON agent_tools (agent_id);
CREATE INDEX IF NOT EXISTS idx_agent_tools_tool_id ON agent_tools (tool_id);

-- 注释
COMMENT ON TABLE agents IS 'AI Agent 配置表，Agent = LLM + Tools + Prompt';
COMMENT ON TABLE tools IS '工具定义表，支持内置、自定义、MCP三种类型';
COMMENT ON TABLE agent_tools IS 'Agent-Tool绑定关系表';