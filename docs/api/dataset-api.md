# Dataset API 文档

数据集管理模块 REST API 文档。

## 基础信息

- **Base URL**: `/api/v1/datasets`
- **Content-Type**: `application/json`

## 数据集管理

### 1. 创建数据集

```http
POST /api/v1/datasets
```

**请求体**:
```json
{
  "name": "训练数据集",
  "description": "用于模型训练的数据集",
  "category": "训练",
  "tags": "nlp,classification",
  "sourceType": "MANUAL"
}
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "训练数据集",
    "description": "用于模型训练的数据集",
    "category": "训练",
    "tags": "nlp,classification",
    "version": 1,
    "status": "DRAFT",
    "itemCount": 0,
    "sourceType": "MANUAL",
    "createdAt": "2025-04-03T16:45:00",
    "updatedAt": "2025-04-03T16:45:00"
  }
}
```

### 2. 获取数据集列表

```http
GET /api/v1/datasets?page=0&size=20&sort=createdAt,desc
```

**查询参数**:
- `page`: 页码（从0开始）
- `size`: 每页数量
- `sort`: 排序字段
- `category`: 按分类筛选
- `status`: 按状态筛选
- `keyword`: 按名称搜索

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [...],
    "totalElements": 100,
    "totalPages": 5,
    "number": 0,
    "size": 20
  }
}
```

### 3. 获取单个数据集

```http
GET /api/v1/datasets/{datasetId}
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "训练数据集",
    "description": "用于模型训练的数据集",
    "category": "训练",
    "tags": "nlp,classification",
    "version": 1,
    "status": "DRAFT",
    "itemCount": 50,
    "sourceType": "MANUAL",
    "createdAt": "2025-04-03T16:45:00",
    "updatedAt": "2025-04-03T16:45:00"
  }
}
```

### 4. 更新数据集

```http
PUT /api/v1/datasets/{datasetId}
```

**请求体**:
```json
{
  "name": "更新后的名称",
  "description": "更新后的描述",
  "category": "测试",
  "tags": "updated,tags"
}
```

### 5. 删除数据集

```http
DELETE /api/v1/datasets/{datasetId}
```

### 6. 发布数据集

```http
POST /api/v1/datasets/{datasetId}/publish
```

将数据集状态从 `DRAFT` 改为 `PUBLISHED`。

### 7. 创建新版本

```http
POST /api/v1/datasets/{datasetId}/versions
```

创建数据集的新版本。

## 数据集项管理

### 1. 添加数据项

```http
POST /api/v1/datasets/{datasetId}/items
```

**请求体**:
```json
{
  "inputData": "什么是机器学习？",
  "outputData": "机器学习是人工智能的一个分支...",
  "metadata": "{\"source\": \"wiki\", \"quality\": \"high\"}",
  "sequence": 1
}
```

### 2. 批量添加数据项

```http
POST /api/v1/datasets/{datasetId}/items/batch
```

**请求体**:
```json
{
  "items": [
    {
      "inputData": "问题1",
      "outputData": "答案1",
      "sequence": 1
    },
    {
      "inputData": "问题2",
      "outputData": "答案2",
      "sequence": 2
    }
  ]
}
```

### 3. 获取数据项列表

```http
GET /api/v1/datasets/{datasetId}/items?page=0&size=50
```

### 4. 更新数据项

```http
PUT /api/v1/datasets/{datasetId}/items/{itemId}
```

**请求体**:
```json
{
  "inputData": "更新后的问题",
  "outputData": "更新后的答案",
  "metadata": "{\"updated\": true}"
}
```

### 5. 删除数据项

```http
DELETE /api/v1/datasets/{datasetId}/items/{itemId}
```

## 导入导出

### 1. 导入数据

```http
POST /api/v1/datasets/{datasetId}/import
Content-Type: multipart/form-data
```

**请求参数**:
- `file`: 文件（JSON 或 CSV 格式）
- `format`: `JSON` 或 `CSV`

**JSON 格式示例**:
```json
[
  {
    "input": "问题1",
    "output": "答案1",
    "metadata": {...}
  },
  {
    "input": "问题2",
    "output": "答案2"
  }
]
```

**CSV 格式示例**:
```csv
input,output,metadata
"问题1","答案1","{\"source\":\"wiki\"}"
"问题2","答案2",""
```

### 2. 导出数据

```http
GET /api/v1/datasets/{datasetId}/export?format=JSON
```

**查询参数**:
- `format`: `JSON` 或 `CSV`

**响应**: 文件下载

## 状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 404 | 资源不存在 |
| 409 | 资源冲突（如名称重复） |
| 500 | 服务器内部错误 |

## 枚举值

### DatasetStatus
- `DRAFT` - 草稿
- `PUBLISHED` - 已发布
- `ARCHIVED` - 已归档

### DatasetItemStatus
- `ACTIVE` - 有效
- `DISABLED` - 禁用

### SourceType
- `MANUAL` - 手动创建
- `IMPORTED` - 导入
- `GENERATED` - 生成
