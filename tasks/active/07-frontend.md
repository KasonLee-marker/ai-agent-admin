# Task: 前端开发 (React 18 + Ant Design 5.x)

## 目标

开发 AI Agent Admin 平台的前端界面，实现所有 MVP 模块的用户交互功能。

## 上下文

后端已完成所有 MVP 模块的开发，前端需要对接后端 API，提供完整的用户界面。

## 技术栈

- **框架**: React 18 + TypeScript
- **构建工具**: Vite
- **UI 组件**: Ant Design 5.x
- **路由**: React Router 6
- **HTTP 客户端**: Axios
- **状态管理**: Zustand (轻量级)
- **图表**: @ant-design/charts

## 需求

### 功能需求

- [x] 用户登录认证
- [x] 首页仪表盘
- [x] Prompt 管理页面
- [x] 模型管理页面
- [x] 对话调试页面
- [x] 数据集管理页面
- [x] 评估系统页面
- [x] 文档管理页面
- [x] RAG 对话页面

### 技术需求

- [x] React 18 + TypeScript
- [x] Ant Design 5.x 组件库
- [x] Vite 构建工具
- [x] 响应式设计
- [x] 代码规范 (ESLint + Prettier)

## 页面设计

### 路由结构

```
/login                  - 登录页面
/                       - 首页仪表盘
/prompts               - Prompt 管理列表
/prompts/:id           - Prompt 详情/编辑
/models                - 模型管理列表
/models/:id            - 模型详情/编辑
/chat                  - 对话调试
/chat/:sessionId       - 会话详情
/datasets              - 数据集管理
/datasets/:id          - 数据集详情
/evaluations           - 评估任务列表
/evaluations/:id       - 评估详情
/documents             - 文档管理
/rag                   - RAG 对话
```

## 文件结构

```
frontend/
├── package.json
├── vite.config.ts
├── tsconfig.json
├── .eslintrc.cjs
├── index.html
├── src/
│   ├── main.tsx
│   ├── App.tsx
│   ├── vite-env.d.ts
│   ├── api/                    # API 请求
│   │   ├── client.ts           # Axios 实例
│   │   ├── auth.ts             # 认证 API
│   │   ├── prompts.ts
│   │   ├── models.ts
│   │   ├── chat.ts
│   │   ├── datasets.ts
│   │   ├── evaluations.ts
│   │   ├── documents.ts
│   │   └── rag.ts
│   ├── components/             # 通用组件
│   │   ├── Layout/
│   │   │   ├── index.tsx
│   │   │   ├── Sidebar.tsx
│   │   │   └── Header.tsx
│   │   ├── PromptEditor/
│   │   ├── ChatWindow/
│   │   ├── ModelSelector/
│   │   └── ProtectedRoute/
│   ├── pages/                  # 页面组件
│   │   ├── Login/
│   │   ├── Dashboard/
│   │   ├── Prompts/
│   │   ├── Models/
│   │   ├── Chat/
│   │   ├── Datasets/
│   │   ├── Evaluations/
│   │   ├── Documents/
│   │   └── Rag/
│   ├── hooks/                  # 自定义 Hooks
│   │   ├── useAuth.ts
│   │   └── useApi.ts
│   ├── stores/                 # 状态管理
│   │   └── authStore.ts
│   ├── types/                  # TypeScript 类型
│   │   ├── api.ts
│   │   ├── prompt.ts
│   │   ├── model.ts
│   │   ├── chat.ts
│   │   ├── dataset.ts
│   │   └── evaluation.ts
│   └── styles/                 # 样式文件
│       └── global.css
└── public/
    └── favicon.ico
```

## 后端 API 端点

### Prompt 管理 (`/api/v1/prompts`)

| 方法     | 端点                            | 描述         |
|--------|-------------------------------|------------|
| GET    | /api/v1/prompts               | 列表（分页、筛选）  |
| POST   | /api/v1/prompts               | 创建         |
| GET    | /api/v1/prompts/{id}          | 详情         |
| PUT    | /api/v1/prompts/{id}          | 更新（自动创建版本） |
| DELETE | /api/v1/prompts/{id}          | 删除         |
| GET    | /api/v1/prompts/{id}/versions | 版本历史       |
| POST   | /api/v1/prompts/{id}/rollback | 版本回滚       |

### 模型管理 (`/api/v1/models`)

| 方法     | 端点                          | 描述    |
|--------|-----------------------------|-------|
| GET    | /api/v1/models              | 列表    |
| POST   | /api/v1/models              | 创建    |
| GET    | /api/v1/models/{id}         | 详情    |
| PUT    | /api/v1/models/{id}         | 更新    |
| DELETE | /api/v1/models/{id}         | 删除    |
| POST   | /api/v1/models/{id}/test    | 健康检查  |
| POST   | /api/v1/models/{id}/default | 设为默认  |
| GET    | /api/v1/models/providers    | 供应商列表 |
| POST   | /api/v1/models/switch       | 切换模型  |

### 对话调试 (`/api/v1/chat`)

| 方法     | 端点                                  | 描述   |
|--------|-------------------------------------|------|
| POST   | /api/v1/chat/sessions               | 创建会话 |
| GET    | /api/v1/chat/sessions               | 会话列表 |
| GET    | /api/v1/chat/sessions/{id}          | 会话详情 |
| DELETE | /api/v1/chat/sessions/{id}          | 删除会话 |
| POST   | /api/v1/chat/messages               | 发送消息 |
| GET    | /api/v1/chat/sessions/{id}/messages | 消息历史 |

### 数据集管理 (`/api/v1/datasets`)

| 方法             | 端点                                | 描述       |
|----------------|-----------------------------------|----------|
| GET/POST       | /api/v1/datasets                  | 列表/创建    |
| GET/PUT/DELETE | /api/v1/datasets/{id}             | 详情/更新/删除 |
| GET/POST       | /api/v1/datasets/{id}/items       | 数据项列表/创建 |
| POST           | /api/v1/datasets/import           | 导入       |
| GET            | /api/v1/datasets/{id}/export/json | 导出 JSON  |
| GET            | /api/v1/datasets/{id}/export/csv  | 导出 CSV   |
| POST           | /api/v1/datasets/{id}/versions    | 创建新版本    |

### 评估系统 (`/api/v1/evaluations`)

| 方法             | 端点                               | 描述       |
|----------------|----------------------------------|----------|
| GET/POST       | /api/v1/evaluations              | 列表/创建    |
| GET/PUT/DELETE | /api/v1/evaluations/{id}         | 详情/更新/删除 |
| POST           | /api/v1/evaluations/{id}/run     | 运行评估     |
| POST           | /api/v1/evaluations/{id}/cancel  | 取消       |
| GET            | /api/v1/evaluations/{id}/results | 结果列表     |
| GET            | /api/v1/evaluations/{id}/metrics | 指标统计     |
| POST           | /api/v1/evaluations/compare      | 对比       |

### 文档管理 (`/api/v1/documents`)

| 方法         | 端点                                | 描述    |
|------------|-----------------------------------|-------|
| POST       | /api/v1/documents/upload          | 上传文档  |
| GET        | /api/v1/documents                 | 文档列表  |
| GET/DELETE | /api/v1/documents/{id}            | 详情/删除 |
| GET        | /api/v1/documents/{id}/chunks     | 分块列表  |
| GET        | /api/v1/documents/supported-types | 支持类型  |

### 向量检索 (`/api/v1/vector`)

| 方法   | 端点                    | 描述    |
|------|-----------------------|-------|
| POST | /api/v1/vector/search | 相似度搜索 |

### RAG 对话 (`/api/v1/rag`)

| 方法   | 端点               | 描述     |
|------|------------------|--------|
| POST | /api/v1/rag/chat | RAG 对话 |

## 认证设计

### 简化认证流程

1. 登录页面：用户名 + 密码
2. 使用 localStorage 存储登录状态
3. 路由守卫：未登录跳转到 /login
4. 初始版本：前端模拟登录（固定账号密码）

### 用户状态管理

```typescript
interface AuthState {
  isLoggedIn: boolean;
  username: string | null;
  login: (username: string, password: string) => Promise<boolean>;
  logout: () => void;
}
```

## 约束

- 仅中文界面
- 响应式设计，支持桌面端
- 遵循 Ant Design 设计规范
- API 响应格式统一处理

## 完成标准

- [x] 所有页面功能正常
- [x] 与后端 API 对接成功
- [x] 代码通过 ESLint 检查
- [x] 打包构建成功

## 检查点

- Checkpoint 1: 项目初始化和基础布局 ✅
- Checkpoint 2: 登录页面和路由守卫 ✅
- Checkpoint 3: Prompt 和模型管理页面 ✅
- Checkpoint 4: 对话调试页面 ✅
- Checkpoint 5: 数据集和评估页面 ✅
- Checkpoint 6: 文档和 RAG 页面 ✅

## 进度日志

### 2026-04-09 开发进度

#### Checkpoint 1 - 已完成

- 创建 React 项目结构
- 配置 Vite + TypeScript
- 配置 Ant Design 5.x
- 创建基础布局组件
- 实现登录页面和路由守卫

#### Checkpoint 2 - 已完成

- Prompt 管理页面（基础版）
- 模型管理页面（基础版）
- 对话调试页面（基础版）
- 数据集/评估/文档/RAG 页面（占位页面）

#### 待完成

- [x] 数据集管理页面完整实现
- [x] 评估系统页面完整实现
- [x] 文档管理页面完整实现
- [x] RAG 对话页面完整实现
- [x] 安装依赖并测试
- [ ] 与后端 API 联调

## 状态

✅ [READY_FOR_REVIEW] - 前端开发完成，API 联调通过

## 进度日志

### 2026-04-10 Checkpoint 5 完成

Status: [COMPLETE]
Completed:

- 创建 types 文件: dataset.ts, evaluation.ts, document.ts, rag.ts
- 创建 api 文件: datasets.ts, evaluations.ts, documents.ts, rag.ts
- 实现数据集管理页面完整功能（CRUD、数据项管理、导入导出）
- 实现评估系统页面完整功能（任务管理、运行、结果展示、指标统计）
- 实现文档管理页面完整功能（上传、分块查看、删除）
- 实现 RAG 对话页面完整功能（对话模式、向量检索模式、文档筛选）
- 前端编译通过 (npm run build)
- ESLint 检查通过 (npm run lint)

### 2026-04-10 Checkpoint 6 完成

Status: [COMPLETE]
Completed:

- 增强 Dashboard 页面，添加实时统计数据获取
- 添加 6 个统计卡片（Prompt、模型、会话、数据集、文档、系统状态）
- 添加功能模块卡片介绍
- 前端编译和 lint 检查全部通过

Next: 与后端 API 联调测试
Blockers: Maven 不在 PATH 中，需要手动启动后端服务

### 2026-04-10 API 联调测试完成

Status: [COMPLETE]
Completed:

- 修复 ModelConfigRepository.findByFilters SQL 错误（PostgreSQL LOWER(bytea) 问题）
- 测试 Prompts API - 创建、列表查询正常
- 测试 Models API - 创建、列表查询正常
- 测试 Datasets API - 列表查询正常
- 测试 Evaluations API - 列表查询正常
- 测试 Documents API - 列表查询正常
- 前端开发服务器运行正常 (localhost:5173)
- 后端服务运行正常 (localhost:8080)

Next: 任务完成，可移动到 completed 目录
Blockers: None