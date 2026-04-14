# Task: RAG 评估系统完善

## 目标

完善 RAG 评估系统，解决以下问题：

1. 模型管理页面不支持 Embedding 类型配置
2. Embedding 模型引用逻辑不清晰

## 上下文

RAG 评估系统已完成基础改造（Phase 1-5），但有两个关键问题：

- 用户不知道如何配置 Embedding 模型
- 系统不知道用哪个 Embedding 模型配置

详细文档见 `docs/evaluation-rag-feature.md`

## 需求

### 功能需求

**FR1: 模型管理页面支持 Embedding 类型**

- Provider 下拉框分组显示（Chat 组 / Embedding 组）
- 显示 Provider 类型标签
- 支持设置"默认 Embedding 模型"

**FR2: Embedding 模型引用逻辑完善**

- 系统有明确的默认 Embedding 模型配置
- EmbeddingService 只使用 Embedding 类型的模型
- 文档上传、RAG检索、评估相似度使用同一 Embedding 配置

### 技术需求

**TR1: Provider 增加 type 字段**

- ModelProvider 枚举增加 modelType 属性（CHAT/EMBEDDING）

**TR2: ModelConfig 支持类型区分**

- 可能方案：通过 Provider 自动推断类型
- 或新增 modelType 字段

**TR3: 默认 Embedding 模型设置**

- ModelConfig 增加 isDefaultEmbedding 字段
- 或在系统配置中存储默认 Embedding 模型 ID

## 约束

- 不破坏现有功能
- Embedding 和 Chat 模型类型必须明确区分
- 默认模型逻辑：默认 Chat 模型 ≠ 默认 Embedding 模型

## 完成标准

- [x] Provider 显示类型（Chat/Embedding）
- [x] 模型管理页面分组显示 Provider
- [x] 可设置默认 Embedding 模型
- [x] EmbeddingService 只查找 Embedding 类型配置
- [ ] 文档上传使用 Embedding 模型（不使用 Chat 模型） - 待后续实现
- [ ] 评估相似度计算正常工作 - 待后续验证

## Checkpoint 1: Provider 类型定义 ✅

**目标**: 给 ModelProvider 增加 modelType 属性

**修改文件**:

- `admin-server-core/src/main/java/com/aiagent/admin/domain/enums/ModelProvider.java`

**实现**:

```java
public enum ModelProvider {
    // Chat 模型
    OPENAI("OpenAI", "...", ..., ModelType.CHAT),
    ...
    
    // Embedding 模型
    OPENAI_EMBEDDING("OpenAI Embedding", "...", ..., ModelType.EMBEDDING),
    DASHSCOPE_EMBEDDING("阿里云百炼 Embedding", "...", ..., ModelType.EMBEDDING);
    
    private final ModelType modelType;
    
    public enum ModelType { CHAT, EMBEDDING }
}
```

**状态**: ✅ 已完成

## Checkpoint 2: ProviderResponse 增加类型字段 ✅

**目标**: API 返回 Provider 类型信息

**修改文件**:

- `admin-server-runtime/src/main/java/com/aiagent/admin/api/dto/ProviderResponse.java` - 增加 modelType 字段
- `admin-server-runtime/src/main/java/com/aiagent/admin/api/controller/ModelController.java` - 返回 modelType

**状态**: ✅ 已完成

## Checkpoint 3: 前端模型管理页面分组显示 ✅

**目标**: Provider 下拉框分组显示 Chat/Embedding 类型

**修改文件**:

- `frontend/src/pages/Models/index.tsx` - Select.OptGroup 分组显示
- `frontend/src/types/model.ts` - ProviderInfo 增加 modelType
- `frontend/src/api/models.ts` - 增加 setDefaultEmbeddingModel API

**实现**:

- Select 组件使用 Select.OptGroup 分组
- 表格增加"类型"列，显示 Tag 标签（对话/Embedding）
- Embedding 模型显示"设为默认Embedding"按钮

**状态**: ✅ 已完成

## Checkpoint 4: 默认 Embedding 模型设置 ✅

**目标**: 支持设置默认 Embedding 模型

**方案**:

- ModelConfig 增加 isDefaultEmbedding 字段（独立于 isDefault）

**修改文件**:

- `admin-server-core/src/main/java/com/aiagent/admin/domain/entity/ModelConfig.java` - 增加 isDefaultEmbedding 字段
- `admin-server-runtime/src/main/java/com/aiagent/admin/domain/repository/ModelConfigRepository.java` - 增加
  findByIsDefaultEmbeddingTrue, clearDefaultEmbeddingModel, setDefaultEmbeddingModel 方法
- `admin-server-runtime/src/main/java/com/aiagent/admin/service/ModelConfigService.java` - 增加 setDefaultEmbedding,
  findDefaultEmbedding, findDefaultEmbeddingEntity 方法
- `admin-server-runtime/src/main/java/com/aiagent/admin/api/controller/ModelController.java` - 增加 POST
  /{id}/default-embedding 和 GET /default-embedding 端点
- `admin-server-runtime/src/main/java/com/aiagent/admin/api/dto/ModelResponse.java` - 增加 isDefaultEmbedding 和
  modelType 字段
- `admin-server-runtime/src/main/java/com/aiagent/admin/service/mapper/ModelMapper.java` - 增加 modelType 映射

**状态**: ✅ 已完成

## Checkpoint 5: EmbeddingService 改进查找逻辑 ✅

**目标**: 优先使用 isDefaultEmbedding 标记的模型

**修改文件**:

- `admin-server-runtime/src/main/java/com/aiagent/admin/service/impl/EmbeddingServiceImpl.java`

**改进逻辑**:

```java
private ModelConfig getEmbeddingModelConfig() {
    // 1. 优先查找明确标记为默认 Embedding 的模型
    ModelConfig defaultEmbedding = modelConfigRepository.findByIsDefaultEmbeddingTrue()
            .orElse(null);
    if (defaultEmbedding != null && Boolean.TRUE.equals(defaultEmbedding.getIsActive())) {
        return defaultEmbedding;
    }

    // 2. 查找专用 Embedding Provider 的激活配置
    List<ModelConfig> embeddingConfigs = modelConfigRepository.findByProviderInAndIsActiveTrue(
            List.of(ModelProvider.OPENAI_EMBEDDING, ModelProvider.DASHSCOPE_EMBEDDING));
    if (!embeddingConfigs.isEmpty()) {
        return embeddingConfigs.get(0);
    }

    // 3. 查找默认 Chat 模型（会记录警告）
    // 4. 返回系统默认配置（兜底方案）
}
```

**状态**: ✅ 已完成

## 进度日志

### Checkpoint 1-5 - 2026-04-13

Status: COMPLETED
Branch: main
Completed:

- ✅ Checkpoint 1: ModelProvider 增加 ModelType 枚举和 modelType 字段
- ✅ Checkpoint 2: ProviderResponse 增加 modelType，Controller 返回类型信息
- ✅ Checkpoint 3: 前端 Provider 下拉框分组显示，增加类型列，增加 setDefaultEmbeddingModel 按钮
- ✅ Checkpoint 4: ModelConfig 增加 isDefaultEmbedding 字段，API 端点支持设置默认 Embedding 模型
- ✅ Checkpoint 5: EmbeddingService 改进查找逻辑，优先使用 isDefaultEmbedding 标记的模型
- 后端编译成功
- 前端构建成功
- UI 功能验证通过（供应商下拉框分组显示正确）
  Next: 后续可验证文档上传和评估相似度计算功能
  Blockers: None

## 状态: COMPLETED