# PostgreSQL with pgvector + pg_jieba 中文分词

基于 `pgvector/pgvector:pg15` 镜像，集成中文全文搜索扩展。

## 包含扩展

| 扩展       | 功能    | 用途             |
|----------|-------|----------------|
| pgvector | 向量存储  | Embedding 向量检索 |
| pg_jieba | 中文分词  | 中文全文搜索（结巴分词）   |
| pg_trgm  | 三元组匹配 | 模糊搜索后备方案       |

## 构建镜像

```bash
cd docker/postgres-zhparser
docker build -t postgres-pgvector-jieba:pg15 .
```

**构建依赖**：

需要以下源码包（已从 Gitee/GitHub 下载）：

- `pg_jieba.zip` - pg_jieba 源码
- `cppjieba.zip` - cppjieba 分词库
- `limonp.zip` - limonp 工具库

## 运行容器

```bash
docker run -d --name agentx-postgres \
    -p 5432:5432 \
    -e POSTGRES_DB=admindb \
    -e POSTGRES_USER=adminuser \
    -e POSTGRES_PASSWORD=adminpass123 \
    postgres-pgvector-jieba:pg15
```

## 初始化配置

`init.sql` 在容器启动时自动执行：

```sql
-- 安装扩展
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 配置 pg_jieba 预加载
ALTER SYSTEM SET shared_preload_libraries = 'pg_jieba';

-- 创建 pg_jieba 扩展
CREATE EXTENSION IF NOT EXISTS pg_jieba;
```

**注意**：pg_jieba 需要 `shared_preload_libraries` 配置才能正常工作。

## 中文分词使用

### 可用的 Parser

| Parser     | 说明     | 字典依赖                    |
|------------|--------|-------------------------|
| `jieba`    | 混合模式   | 需字典文件 `jieba_base.dict` |
| `jiebamp`  | 最大概率模式 | 无字典依赖（推荐）               |
| `jiebahmm` | HMM 模式 | 无字典依赖                   |
| `jiebaqry` | 查询模式   | 用于搜索查询                  |

### 示例

```sql
-- 中文分词
SELECT to_tsvector('jiebamp', '人工智能技术正在快速发展');
-- 结果: '人工智能':1 '技术':2

-- 中文搜索
SELECT * FROM document_chunks 
WHERE content_tsv_jieba @@ to_tsquery('jiebamp', '配送 | 运费');

-- 评分排序
SELECT id, ts_rank(content_tsv_jieba, to_tsquery('jiebamp', '配送')) as score
FROM document_chunks
WHERE content_tsv_jieba @@ to_tsquery('jiebamp', '配送')
ORDER BY score DESC;
```

## BM25 中文搜索集成

在 `document_chunks` 表添加 `content_tsv_jieba` 列：

```sql
-- 添加列
ALTER TABLE document_chunks ADD COLUMN content_tsv_jieba tsvector;

-- 创建 GIN 索引
CREATE INDEX idx_document_chunks_tsv_jieba ON document_chunks USING GIN(content_tsv_jieba);

-- 创建触发器函数
CREATE OR REPLACE FUNCTION update_content_tsv_jieba() RETURNS trigger AS $$
BEGIN
  NEW.content_tsv_jieba := to_tsvector('jiebamp', COALESCE(NEW.content, ''));
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 创建触发器
CREATE TRIGGER trg_update_content_tsv_jieba
BEFORE INSERT OR UPDATE ON document_chunks
FOR EACH ROW EXECUTE FUNCTION update_content_tsv_jieba();

-- 更新现有数据
UPDATE document_chunks SET content_tsv_jieba = to_tsvector('jiebamp', COALESCE(content, ''));
```

## 数据备份与恢复

```bash
# 备份
docker exec agentx-postgres pg_dump -U adminuser admindb > admindb_backup.sql

# 恢复
docker exec -i agentx-postgres psql -U adminuser -d admindb < admindb_backup.sql
```

## 注意事项

1. **jiebamp 推荐使用**：不依赖字典文件，分词稳定
2. **BM25 精确匹配**：关键词必须在文档中出现，不支持语义匹配
3. **混合检索建议**：向量检索（语义）+ BM25（关键词）融合使用