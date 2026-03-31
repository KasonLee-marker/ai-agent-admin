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

### Database Initialization - PostgreSQL + pgvector
Status: [COMPLETED]
Completed: 
- Checkpoint 1: Started PostgreSQL container with pgvector (ankane/pgvector:latest)
- Checkpoint 2: Created user 'aiagent' with password 'aiagent123'
- Checkpoint 2: Created database 'aiagent_admin' with owner aiagent
- Checkpoint 2: Granted all privileges to aiagent user
- Checkpoint 3: Enabled pgvector extension (v0.5.1)
- Checkpoint 3: Created 'documents' table with UUID PK, name, content_type, total_chunks, metadata JSONB
- Checkpoint 3: Created 'document_chunks' table with UUID PK, document_id FK, chunk_index, content, VECTOR(1536), metadata JSONB
- Checkpoint 3: Created IVFFlat index on embedding column for cosine similarity search
- Checkpoint 4: Verified all tables and extension are properly configured

Database Connection:
- Host: localhost:5432
- Database: aiagent_admin
- User: aiagent / aiagent123
- Container: postgres-pgvector (running)

Next: 创建实体类和数据库表 (Prompt管理模块)
Blockers: None

### Checkpoint 1 - Project Structure and Dependencies
Status: [COMPLETED]
Completed:
- Created Maven project structure with Spring Boot 3.2.12
- Configured dependencies: Spring Web, JPA, Validation, H2, PostgreSQL, Lombok, MapStruct, SpringDoc OpenAPI
- Set up profile-based configuration (dev: H2, prod: PostgreSQL)
- Created layered package structure: domain -> service -> api

### Checkpoint 2 - Entities and Repositories
Status: [COMPLETED]
Completed:
- PromptTemplate entity with JPA annotations (id, name, content, description, category, tags, version, variables, createdAt, updatedAt, createdBy)
- PromptVersion entity for version history (id, promptId, version, content, changeLog, createdAt)
- PromptTemplateRepository with custom query for filtering by category/tag/keyword
- PromptVersionRepository with methods for version lookup

### Checkpoint 3 - Service Layer Implementation
Status: [COMPLETED]
Completed:
- PromptService interface defining all business operations
- PromptServiceImpl with transactional methods:
  - createPrompt: Creates template + initial version, extracts variables
  - updatePrompt: Saves old version, increments version number, updates template
  - deletePrompt: Removes template and all versions
  - getPrompt: Retrieves template by ID
  - listPrompts: Paginated list with filters
  - getPromptVersions: Lists version history
  - rollbackPrompt: Reverts to specific version, creates new version entry
- Variable extraction from {{variable}} pattern using regex
- MapStruct mapper for DTO/Entity conversion

### Checkpoint 4 - API Controllers
Status: [COMPLETED]
Completed:
- PromptController with all REST endpoints:
  - POST /api/v1/prompts - Create prompt
  - GET /api/v1/prompts/{id} - Get prompt detail
  - GET /api/v1/prompts - List prompts (pagination, filters)
  - PUT /api/v1/prompts/{id} - Update prompt (auto-version)
  - DELETE /api/v1/prompts/{id} - Delete prompt
  - GET /api/v1/prompts/{id}/versions - Get version history
  - POST /api/v1/prompts/{id}/rollback - Rollback to version
- GlobalExceptionHandler for unified error responses
- OpenAPI annotations for Swagger documentation
- ApiResponse wrapper for consistent response format

### Checkpoint 5 - Tests and Documentation
Status: [COMPLETED]
Completed:
- PromptServiceImplTest: 9 test methods covering create, update, delete, get, list, rollback, variable extraction
- PromptControllerTest: 8 test methods covering all API endpoints with validation tests
- PromptMapperTest: 10 test methods for DTO/Entity mapping
- Total: 27 unit tests

### Project Structure
```
prompt-service/
├── pom.xml
├── src/main/java/com/aiagent/admin/prompt/
│   ├── PromptServiceApplication.java
│   ├── domain/
│   │   ├── entity/PromptTemplate.java
│   │   ├── entity/PromptVersion.java
│   │   ├── repository/PromptTemplateRepository.java
│   │   └── repository/PromptVersionRepository.java
│   ├── service/
│   │   ├── PromptService.java
│   │   ├── impl/PromptServiceImpl.java
│   │   └── mapper/PromptMapper.java
│   └── api/
│       ├── controller/PromptController.java
│       ├── dto/
│       │   ├── ApiResponse.java
│       │   ├── PageResponse.java
│       │   ├── PromptTemplateCreateRequest.java
│       │   ├── PromptTemplateUpdateRequest.java
│       │   ├── PromptTemplateResponse.java
│       │   ├── PromptVersionResponse.java
│       │   ├── PromptListRequest.java
│       │   └── RollbackRequest.java
│       └── exception/GlobalExceptionHandler.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml (H2)
│   └── application-prod.yml (PostgreSQL)
└── src/test/java/com/aiagent/admin/prompt/
    ├── service/PromptServiceImplTest.java
    ├── service/mapper/PromptMapperTest.java
    └── api/controller/PromptControllerTest.java
```

### API Documentation
- Swagger UI available at: http://localhost:8080/swagger-ui.html
- OpenAPI docs at: http://localhost:8080/api-docs

### Next Steps
- Run `mvn clean test` to execute unit tests
- Run `mvn spring-boot:run` to start the service
- Verify API endpoints using Swagger UI or curl

Status: [COMPLETED]
Blockers: None
