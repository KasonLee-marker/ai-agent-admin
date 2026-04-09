# Task: 文档检索/RAG 服务实现

## 目标
实现文档检索和 RAG（检索增强生成）模块，支持文档上传、自动向量化、相似度检索和 RAG 对话。

## 状态：✅ 已完成

## 已完成内容

### 1. 实体类 (admin-server-core)
- [x] Document.java - 文档实体
- [x] DocumentChunk.java - 文档分块实体

### 2. Repository (admin-server-runtime)
- [x] DocumentRepository.java
- [x] DocumentChunkRepository.java

### 3. DTOs (admin-server-runtime/api/dto)
- [x] DocumentUploadRequest.java
- [x] DocumentResponse.java
- [x] DocumentChunkResponse.java
- [x] VectorSearchRequest.java
- [x] VectorSearchResult.java
- [x] RagChatRequest.java
- [x] RagChatResponse.java

### 4. Mappers (admin-server-runtime/service/mapper)
- [x] DocumentMapper.java
- [x] DocumentChunkMapper.java

### 5. Services (admin-server-runtime/service)
- [x] DocumentService.java - 接口
- [x] DocumentServiceImpl.java - 实现（文档上传、分块、向量存储）
- [x] RagService.java - 接口
- [x] RagServiceImpl.java - 实现（RAG对话）

### 6. Controllers (admin-server-runtime/api/controller)
- [x] DocumentController.java
- [x] VectorController.java
- [x] RagController.java

### 7. 测试
- [x] DocumentServiceImplTest.java
- [x] RagServiceImplTest.java
- [x] DocumentControllerTest.java

### 8. 配置
- [x] pom.xml 添加 spring-ai-pgvector-store、pdfbox、poi 依赖
- [x] application.yml 添加 pgvector 配置

## API端点

### 文档管理
- `POST /api/v1/documents/upload` - 上传文档
- `GET /api/v1/documents/{id}` - 获取文档详情
- `GET /api/v1/documents` - 分页查询文档列表
- `DELETE /api/v1/documents/{id}` - 删除文档
- `GET /api/v1/documents/{id}/chunks` - 获取文档分块
- `GET /api/v1/documents/{id}/status` - 获取处理状态
- `GET /api/v1/documents/supported-types` - 获取支持的文件类型

### 向量检索
- `POST /api/v1/vector/search` - 相似度搜索

### RAG对话
- `POST /api/v1/rag/chat` - RAG对话

## 支持的文档类型
- PDF (application/pdf)
- Word (application/vnd.openxmlformats-officedocument.wordprocessingml.document)
- 纯文本 (text/plain)
- Markdown (text/markdown)
- CSV (text/csv)

## 进度日志

### Checkpoint 1 - 待开始
Status: [PENDING]
Completed: 
Next: 搭建 PostgreSQL + pgvector 环境
Blockers: None
