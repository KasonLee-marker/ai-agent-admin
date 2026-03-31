# Task: Prompt 管理服务实现

## 目标
实现 Prompt 管理模块的后端服务，支持 Prompt 模板的 CRUD、版本控制和分类管理。

## 上下文
这是 AI Agent Admin 项目的第一个核心模块。Prompt 管理是整个平台的基础，其他模块（对话调试、评估）都依赖于此。

## 需求

### 功能需求
- [ ] Prompt 模板 CRUD（创建、读取、更新、删除）
- [ ] 版本控制（每次更新生成新版本，支持历史回溯）
- [ ] 分类标签管理（支持多级分类）
- [ ] 变量占位符支持（{{variable}} 格式）
- [ ] 搜索和筛选（按名称、分类、标签）
- [ ] 批量操作（批量删除、批量分类）

### 技术需求
- [ ] 使用 Spring Boot 3.2.x
- [ ] 使用 JPA + MySQL/H2
- [ ] RESTful API 设计
- [ ] 单元测试覆盖率 > 70%
- [ ] API 文档（OpenAPI/Swagger）

## 数据模型

```java
// PromptTemplate 实体
@Entity
public class PromptTemplate {
    @Id
    private String id;
    private String name;
    private String content;
    private String description;
    private String category;
    private String tags; // JSON 数组
    private Integer version;
    private String variables; // JSON 数组，提取的变量名
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
}

// PromptVersion 实体（版本历史）
@Entity
public class PromptVersion {
    @Id
    private String id;
    private String promptId;
    private Integer version;
    private String content;
    private String changeLog;
    private LocalDateTime createdAt;
}
```

## API 设计

```
GET    /api/v1/prompts              # 列表查询（支持分页、筛选）
GET    /api/v1/prompts/{id}         # 获取详情
POST   /api/v1/prompts              # 创建
PUT    /api/v1/prompts/{id}         # 更新（自动生成新版本）
DELETE /api/v1/prompts/{id}         # 删除
GET    /api/v1/prompts/{id}/versions # 获取版本历史
POST   /api/v1/prompts/{id}/rollback # 回滚到指定版本
```

## 约束

- 不使用 Nacos 配置中心
- 数据库使用 H2（开发）或 MySQL（生产）
- 代码遵循分层架构（domain -> service -> api）
- 所有 API 返回统一格式：{code, message, data}

## 完成标准

- [ ] 所有 API 实现并通过测试
- [ ] 单元测试覆盖率 > 70%
- [ ] API 文档生成
- [ ] 代码通过审查

## 检查点

- Checkpoint 1: 实体设计和数据库表创建
- Checkpoint 2: CRUD API 实现
- Checkpoint 3: 版本控制功能实现
- Checkpoint 4: 测试和文档完成

## 进度日志

### Checkpoint 1 - 待开始
Status: [IN_PROGRESS]
Completed: 
Next: 创建实体类和数据库表
Blockers: None
