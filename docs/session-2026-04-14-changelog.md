# 会话变更记录 (2026-04-14)

## 一、已完成改动

### 1. pgvector 扩展安装

- **问题**：健康检查时报错 `Failed to create pgvector extension`
- **解决**：通过 Docker 执行 SQL 安装 pgvector 扩展

```sql
docker
exec agentx-postgres psql -U agentx -d admindb -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

- **结果**：pgvector 0.8.2 安装成功

### 2. HealthCheckService 事务问题修复

- **文件**：`admin-server-runtime/src/main/java/com/aiagent/admin/service/HealthCheckService.java`
- **问题**：`testEmbeddingApiConnection` 设置维度/表名后保存，但 `updateHealthStatus` 重新读取并覆盖，导致维度丢失
- **解决**：改为在 `healthCheck` 方法中统一保存所有修改（健康状态、维度、表名）
- **关键改动**：
    - 移除 `testEmbeddingApiConnection` 中的单独保存
    - 在 `healthCheck` 方法末尾统一调用 `modelConfigRepository.save(config)`

### 3. 段落分块策略修复

- **文件**：`admin-server-runtime/src/main/java/com/aiagent/admin/service/impl/DocumentServiceImpl.java`
- **问题**：`text.replaceAll("\\s+", " ")` 把换行符也替换成空格，导致段落分割失败
- **解决**：
    - 段落模式只替换空格和制表符：`text.replaceAll("[ \\t]+", " ")`
    - 使用更准确的分割正则：`text.split("\\n\\s*\\n+")`
    - 如果单个段落超过 chunkSize，按句子边界进一步分割

### 4. 文档管理前端交互改进

- **文件**：`frontend/src/pages/Documents/index.tsx`
- **改动**：
    - 添加 `activeTab` 状态控制 Tab 切换
    - 分块 Tab 动态生成（未选中文档时不显示）
    - "查看分块"按钮对 CHUNKED 和 COMPLETED 状态都显示
    - 点击"查看分块"跳转到分块详情 Tab
    - 分块详情页添加"返回文档列表"按钮
    - 新增图标：`ArrowLeftOutlined`, `InfoCircleOutlined`

### 5. Embedding 模型选择提示改进

- **文件**：`frontend/src/pages/Documents/index.tsx`
- **改动**：
    - 选择模型后实时显示当前模型的向量维度
    - 提示信息添加"例如"前缀和格式优化
    - 用蓝色卡片高亮显示当前选择模型的维度

### 6. 类型定义完善

- **文件**：`frontend/src/types/model.ts`
- **新增字段**：
  ```typescript
  embeddingDimension?: number;  // 向量维度
  embeddingTableName?: string;  // 向量表名
  ```
- **文件**：`admin-server-runtime/src/main/java/com/aiagent/admin/api/dto/ModelResponse.java`
- **新增字段**：
  ```java
  private Integer embeddingDimension;
  private String embeddingTableName;
  ```

### 7. MapStruct 映射修复（新）

- **问题**：API 返回 `embeddingDimension: null`，但数据库有值
- **根因**：新增字段后没有重新编译，MapStruct 生成的 Impl 类缺少映射
- **解决**：执行 `mvn clean compile` 重新生成 `ModelMapperImpl.java`
- **验证**：API 正确返回 `embeddingDimension: 1024`

### 8. 分块内容显示修复（新）

- **文件**：`frontend/src/pages/Documents/index.tsx`
- **问题**：分块表格显示"暂无数据"，但 API 有返回数据
- **根因**：前端使用 `res.data.content`，但后端返回直接数组 `res.data: [...]`
- **解决**：改为 `Array.isArray(res.data) ? res.data : []`

### 9. 单元测试编写与修复

- **新增测试文件**：
    - `VectorTableServiceImplTest.java` (7 tests)
    - `EmbeddingStorageServiceImplTest.java` (8 tests)
    - `HealthCheckServiceEmbeddingTest.java` (6 tests)
- **修复现有测试**：
    - `DocumentServiceImplTest.java` - 添加缺失的 Mock 依赖
    - `DocumentControllerTest.java` - 从 `@WebMvcTest` 改为纯 Mockito 单元测试
- **测试结果**：171 tests passed

---

## 二、待完成事项

### 1. 功能验证（部分完成）

- [x] 重新执行 embedding 模型健康检查，验证维度正确保存 ✅
- [x] 测试文档上传 + 段落分块策略是否正确 ✅
- [ ] 测试文档向量化流程是否正常

### 2. 前端优化

- [ ] 文档上传后自动刷新列表（当前需要手动刷新）
- [ ] 向量化进度实时更新（当前需要刷新页面）
- [ ] 分块内容搜索/过滤功能

### 3. 后端优化

- [ ] Embedding 模型配置变更时的警告提示（用户修改已使用的模型）
- [ ] 向量检索性能优化（考虑 HNSW 索引）
- [ ] 批量 Embedding API 调用优化

### 4. 数据库相关

- [ ] 确认 PostgreSQL 持久化配置
- [ ] 考虑向量表定期清理机制（删除文档时清理对应向量）

---

## 三、关键代码位置

| 功能     | 文件路径                                                                                                 |
|--------|------------------------------------------------------------------------------------------------------|
| 健康检查   | `admin-server-runtime/src/main/java/com/aiagent/admin/service/HealthCheckService.java`               |
| 文档分块   | `admin-server-runtime/src/main/java/com/aiagent/admin/service/impl/DocumentServiceImpl.java`         |
| 向量表管理  | `admin-server-runtime/src/main/java/com/aiagent/admin/service/impl/VectorTableServiceImpl.java`      |
| 向量存储   | `admin-server-runtime/src/main/java/com/aiagent/admin/service/impl/EmbeddingStorageServiceImpl.java` |
| 模型映射   | `admin-server-runtime/src/main/java/com/aiagent/admin/service/mapper/ModelMapper.java`               |
| 文档管理页面 | `frontend/src/pages/Documents/index.tsx`                                                             |
| 模型类型定义 | `frontend/src/types/model.ts`                                                                        |

---

## 四、当前 Git 状态

```
Modified files:
- admin-server-runtime/src/main/java/com/aiagent/admin/service/HealthCheckService.java
- admin-server-runtime/src/main/java/com/aiagent/admin/service/impl/DocumentServiceImpl.java
- admin-server-runtime/src/main/java/com/aiagent/admin/api/dto/ModelResponse.java
- frontend/src/pages/Documents/index.tsx
- frontend/src/types/model.ts
- docs/session-2026-04-14-changelog.md

New test files:
- admin-server-runtime/src/test/java/com/aiagent/admin/service/impl/VectorTableServiceImplTest.java
- admin-server-runtime/src/test/java/com/aiagent/admin/service/impl/EmbeddingStorageServiceImplTest.java
- admin-server-runtime/src/test/java/com/aiagent/admin/service/HealthCheckServiceEmbeddingTest.java
```