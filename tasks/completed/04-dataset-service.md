# Dataset Service 模块开发任务

## 目标
实现数据集管理服务模块，支持数据集的 CRUD、版本控制、数据项管理、导入导出功能。

## 技术栈
- Spring Boot 3.2.x
- Spring Data JPA
- Lombok + MapStruct
- Jackson (JSON处理)

## 已完成内容

### 1. 实体类 (core模块)
- [x] Dataset.java - 数据集实体
- [x] DatasetItem.java - 数据项实体

### 2. Repository (runtime模块)
- [x] DatasetRepository.java
- [x] DatasetItemRepository.java

### 3. DTOs (runtime/api/dto)
- [x] DatasetCreateRequest.java
- [x] DatasetUpdateRequest.java
- [x] DatasetResponse.java
- [x] DatasetItemCreateRequest.java
- [x] DatasetItemUpdateRequest.java
- [x] DatasetItemResponse.java
- [x] DatasetImportRequest.java
- [x] DatasetVersionCreateRequest.java

### 4. Mappers (runtime/service/mapper)
- [x] DatasetMapper.java
- [x] DatasetItemMapper.java

### 5. Service (runtime/service)
- [x] DatasetService.java - 接口定义
- [x] DatasetServiceImpl.java - 实现类
  - 数据集CRUD操作
  - 数据项CRUD操作
  - 版本控制（创建新版本时复制数据项）
  - JSON/CSV导入导出

### 6. Controller (runtime/api/controller)
- [x] DatasetController.java
  - REST API端点
  - 分页查询支持
  - 文件导出（下载）

### 7. 单元测试
- [x] DatasetServiceImplTest.java - 20个测试用例
- [x] DatasetControllerTest.java - 16个测试用例

## 测试覆盖率

| 类 | 指令覆盖 | 分支覆盖 |
|---|---|---|
| DatasetServiceImpl | 88% | 65% |
| DatasetController | 96% | 50% |

**总计: 36个测试全部通过**

## API端点

### 数据集管理
- `POST /api/v1/datasets` - 创建数据集
- `GET /api/v1/datasets/{id}` - 获取数据集
- `GET /api/v1/datasets` - 分页查询数据集
- `PUT /api/v1/datasets/{id}` - 更新数据集
- `DELETE /api/v1/datasets/{id}` - 删除数据集（软删除）

### 数据项管理
- `POST /api/v1/datasets/{id}/items` - 创建数据项
- `GET /api/v1/datasets/{id}/items` - 分页查询数据项
- `GET /api/v1/datasets/{id}/items/all` - 查询所有数据项（按版本）
- `GET /api/v1/datasets/items/{itemId}` - 获取数据项
- `PUT /api/v1/datasets/items/{itemId}` - 更新数据项
- `DELETE /api/v1/datasets/items/{itemId}` - 删除数据项

### 导入导出
- `POST /api/v1/datasets/import` - 导入数据集
- `POST /api/v1/datasets/{id}/import` - 导入数据项到现有数据集
- `GET /api/v1/datasets/{id}/export/json` - 导出为JSON
- `GET /api/v1/datasets/{id}/export/csv` - 导出为CSV

### 版本管理
- `POST /api/v1/datasets/{id}/versions` - 创建新版本

## 关键设计决策

1. **软删除**: 使用 `status` 字段标记删除状态 (DELETED)，而非物理删除
2. **版本控制**: 通过 `version` 字段实现，创建新版本时复制当前版本的数据项
3. **数据项排序**: 使用 `sequence` 字段维护顺序
4. **导入导出格式**:
   - JSON: 包含完整元数据（名称、描述、分类、标签、数据项）
   - CSV: 仅导出数据项（input, output, metadata）

## 文件清单

```
admin-server-core/src/main/java/com/aiagent/admin/domain/entity/
├── Dataset.java
└── DatasetItem.java

admin-server-runtime/src/main/java/com/aiagent/admin/domain/repository/
├── DatasetRepository.java
└── DatasetItemRepository.java

admin-server-runtime/src/main/java/com/aiagent/admin/api/dto/
├── DatasetCreateRequest.java
├── DatasetUpdateRequest.java
├── DatasetResponse.java
├── DatasetItemCreateRequest.java
├── DatasetItemUpdateRequest.java
├── DatasetItemResponse.java
├── DatasetImportRequest.java
└── DatasetVersionCreateRequest.java

admin-server-runtime/src/main/java/com/aiagent/admin/service/
├── DatasetService.java
└── impl/DatasetServiceImpl.java

admin-server-runtime/src/main/java/com/aiagent/admin/service/mapper/
├── DatasetMapper.java
└── DatasetItemMapper.java

admin-server-runtime/src/main/java/com/aiagent/admin/api/controller/
└── DatasetController.java

admin-server-runtime/src/test/java/com/aiagent/admin/service/
└── DatasetServiceImplTest.java

admin-server-runtime/src/test/java/com/aiagent/admin/api/controller/
└── DatasetControllerTest.java
```

## 状态
**已完成** - 2026-04-08
