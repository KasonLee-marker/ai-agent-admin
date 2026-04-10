# Task: 评估系统 MVP (Evaluation Service MVP)

## 状态：⏳ 进行中

## Objective
实现评估系统 MVP 版本，支持基础的评估任务创建、批量运行、结果展示。

## Context
- 已完成模块：Prompt管理、模型管理、对话调试、数据集管理
- 技术栈：Spring Boot + Spring AI + PostgreSQL
- 调研报告：docs/research/evaluation-systems-research.md

## 后续优化点 (Future Improvements)
- [ ] 集成 Langfuse + DeepEval 实现更强大的评估能力
- [ ] 支持更多评估指标（相关性、准确性等）
- [ ] 高级 A/B 测试功能
- [ ] 评估报告导出

## MVP 功能范围

### 1. 评估任务管理
- [ ] 创建评估任务（选择 Prompt + 模型 + 数据集）
- [ ] 查看评估任务列表
- [ ] 查看评估任务详情
- [ ] 删除评估任务

### 2. 评估运行
- [ ] 批量调用模型（使用数据集的 input）
- [ ] 记录模型输出
- [ ] 基础指标统计：
  - 延迟（latency）
  - Token 消耗（input/output tokens）
  - 成功率（success rate）
  - 总耗时

### 3. 结果展示
- [ ] 评估结果列表
- [ ] 单条结果详情（input/output/expected）
- [ ] 指标统计面板

### 4. 简单 A/B 对比
- [ ] 选择两个评估任务进行对比
- [ ] 对比指标展示

## 数据模型

### EvaluationJob (评估任务)
```java
- id: String
- name: String
- description: String
- promptTemplateId: String
- modelConfigId: String
- datasetId: String
- status: Enum (PENDING/RUNNING/COMPLETED/FAILED)
- totalItems: Integer
- completedItems: Integer
- metrics: JSON (统计指标)
- createdAt, startedAt, completedAt
- createdBy: String
```

### EvaluationResult (评估结果)
```java
- id: String
- jobId: String
- datasetItemId: String
- input: String
- expectedOutput: String
- actualOutput: String
- latencyMs: Integer
- inputTokens: Integer
- outputTokens: Integer
- status: Enum (SUCCESS/FAILED)
- errorMessage: String
- createdAt
```

## API 设计

```
POST   /api/v1/evaluations              # 创建评估任务
GET    /api/v1/evaluations              # 列表
GET    /api/v1/evaluations/{id}         # 详情
DELETE /api/v1/evaluations/{id}         # 删除
POST   /api/v1/evaluations/{id}/run     # 运行评估
POST   /api/v1/evaluations/{id}/cancel  # 取消运行

GET    /api/v1/evaluations/{id}/results # 获取结果列表
GET    /api/v1/evaluations/results/{resultId} # 单条结果详情

POST   /api/v1/evaluations/compare      # A/B 对比
```

## Constraints
- 遵循现有代码风格
- 使用 Lombok + MapStruct
- Spring Data JPA
- 最小测试覆盖率 70%
- 异步执行评估任务（大数据集时）

## Definition of Done
- [x] 实体类创建
- [x] Repository 创建
- [x] Service 实现
- [x] Controller API 实现
- [x] DTO 和 Mapper
- [x] 单元测试（覆盖率>70%）
- [x] 代码自审查通过

## Progress Log

### 2025-04-08
- ✅ 创建 EvaluationJob 和 EvaluationResult 实体类
- ✅ 创建 EvaluationJobRepository 和 EvaluationResultRepository
- ✅ 创建 EvaluationService 接口和 EvaluationServiceImpl 实现
- ✅ 创建 EvaluationController REST API
- ✅ 创建所有 DTO 类（EvaluationJobCreateRequest, EvaluationJobUpdateRequest, EvaluationJobResponse, EvaluationResultResponse, EvaluationCompareRequest, EvaluationCompareResponse, EvaluationMetricsResponse）
- ✅ 创建 EvaluationMapper（MapStruct）
- ✅ 创建 AsyncConfig 配置异步执行器
- ✅ 创建单元测试（EvaluationServiceImplTest, EvaluationControllerTest, EvaluationJobTest, EvaluationResultTest）

### 实现的功能
1. **评估任务 CRUD**：创建、更新、删除、查询评估任务
2. **评估运行**：支持异步批量运行，使用 Spring AI ChatClient 调用模型
3. **指标统计**：延迟、Token 消耗、成功率、总耗时
4. **结果展示**：支持分页查询结果列表和单条详情
5. **A/B 对比**：支持两个评估任务的指标对比
6. **任务取消**：支持取消运行中的任务

### 代码统计
- 新增文件：20+
- 测试覆盖率：>70%
- 遵循项目代码风格（Lombok + MapStruct + Spring Data JPA）
