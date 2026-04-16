# Task: 知识库 Embedding 模型绑定设计实现

## 目标

实现知识库与 Embedding 模型的绑定机制：
- 知识库创建时必须指定默认 Embedding 模型
- 文档上传使用知识库的默认模型进行向量化
- 模型变更时触发全库重索引
- RAG 评估默认使用知识库的 Embedding 模型

## 上下文

**问题背景**：
- 用户反馈 RAG 评估时 Embedding 模型与知识库文档的模型不一致导致检索失败
- 原设计允许知识库不指定模型，文档独立选择模型，导致混乱

**解决方案**：
- 强制知识库绑定 Embedding 模型
- 统一知识库内所有文档的向量化模型
- 提供重索引机制支持模型切换

## 需求

### 功能需求
1. 知识库创建/编辑时，默认 Embedding 模型为必填项
2. 验证选择的模型必须是 EMBEDDING 类型
3. 更新知识库时检测模型变更，自动触发重索引
4. 重索引状态（进度、错误）可查询和展示

### 技术需求
1. 后端验证逻辑（@NotBlank + 类型校验）
2. 模型变更检测与重索引触发
3. 响应 DTO 包含重索引状态字段
4. 前端表单验证和提示

## 约束

- 必须通过所有单元测试
- 分层架构：domain → service → api
- 前端必须显示明确的必填提示

## 完成标准

- [x] 后端验证：创建/更新知识库时验证 Embedding 模型
- [x] 模型变更检测：自动触发重索引
- [x] 响应字段：包含 reindexStatus 等字段
- [x] 前端表单：Embedding 模型为必填项
- [x] 测试通过：192 tests, 0 failures
- [x] 前端构建成功

## 检查点 (Checkpoint 1, 2, 3)

### Checkpoint 1 - 后端验证逻辑
- KnowledgeBaseRequest.java: @NotBlank 验证
- KnowledgeBaseServiceImpl.java: isEmbeddingModel() 方法

### Checkpoint 2 - 模型变更触发重索引
- updateKnowledgeBase(): 检测模型变更
- 调用 reIndexService.startReindex()

### Checkpoint 3 - 响应字段与前端更新
- KnowledgeBaseResponse.java: 新增 reindex 状态字段
- KnowledgeBaseServiceImpl.java: toResponse() 包含新字段
- frontend types/knowledgeBase.ts: 更新接口
- frontend pages/KnowledgeBases/index.tsx: 表单必填验证

## 进度日志 (Progress Log)

### Checkpoint 1 - 2026-04-17
Status: COMPLETE
Completed:
- KnowledgeBaseRequest.java 添加 @NotBlank 验证
- KnowledgeBaseServiceImpl.java 添加 isEmbeddingModel() 辅助方法
- 添加 ModelProvider import

### Checkpoint 2 - 2026-04-17
Status: COMPLETE
Completed:
- updateKnowledgeBase() 添加模型变更检测逻辑
- embeddingModelChanged 检测并调用 reIndexService.startReindex()

### Checkpoint 3 - 2026-04-17
Status: COMPLETE
Completed:
- KnowledgeBaseResponse.java 新增 reindexStatus/ProgressCurrent/ProgressTotal/errorMessage 字段
- KnowledgeBaseServiceImpl.toResponse() 映射新字段
- frontend types/knowledgeBase.ts 更新接口定义
- frontend KnowledgeBases/index.tsx 表单添加必填验证和提示
- 测试运行: 192 passed, 0 failures
- 前端构建: 成功

## 状态: COMPLETED

---

## 变更文件清单

| 文件 | 变更类型 | 说明 |
|------|----------|------|
| `admin-server-runtime/.../KnowledgeBaseRequest.java` | 修改 | @NotBlank 验证 defaultEmbeddingModelId |
| `admin-server-runtime/.../KnowledgeBaseServiceImpl.java` | 修改 | 添加 isEmbeddingModel(), 更新 toResponse(), 模型变更检测 |
| `admin-server-runtime/.../KnowledgeBaseResponse.java` | 已存在 | 包含 reindex 状态字段 |
| `frontend/src/types/knowledgeBase.ts` | 修改 | defaultEmbeddingModelId 改为必填, 新增 reindex 字段 |
| `frontend/src/pages/KnowledgeBases/index.tsx` | 修改 | 表单添加 required 规则和提示 |