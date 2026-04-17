# Task: 评估系统进度刷新与全局按钮精简

## 目标

1. 评估任务重新评估后自动展示进度并轮询刷新
2. 全系统表格操作按钮精简为图标+Tooltip形式，减少横向空间占用

## 上下文

当前评估系统存在两个问题：
1. 点击"重新评估"后任务进入 RUNNING 状态，但用户需要手动刷新才能看到进度变化
2. 操作按钮过多（最多5个），每个按钮包含图标+文字，横向排列需要滚动

全局问题：所有管理页面（Prompt、数据集、知识库等）的表格操作按钮都采用图标+文字形式，导致操作列宽度较大，表格需要横向滚动。

## 需求

### 功能需求

#### 1. 评估进度自动刷新

- RUNNING 状态任务自动轮询进度（3秒间隔）
- 所有 RUNNING 任务完成后停止轮询
- 页面顶部显示轮询状态提示（如"自动刷新中..."）
- 重新评估后自动选中该任务行
- 任务完成后自动加载结果并切换到结果Tab

#### 2. 全局按钮精简

**精简规则**：
- 所有表格操作按钮改为**仅图标**形式（去掉文字）
- 每个图标按钮包裹 `Tooltip`，hover 时显示操作名称
- 按钮使用 `type="link"` 保持链接样式
- 删除按钮保持 `danger` 属性（红色）

**按钮图标标准化**：

| 操作 | 图标 | Tooltip 文字 |
|------|------|-------------|
| 查看/进入/详情 | `EyeOutlined` 或 `FolderOutlined` | "查看" / "进入" |
| 编辑 | `EditOutlined` | "编辑" |
| 删除 | `DeleteOutlined` | "删除" |
| 运行 | `PlayCircleOutlined` | "运行" |
| 取消 | `StopOutlined` | "取消" |
| 重新评估 | `ReloadOutlined` | "重新评估" |
| 查看结果 | `BarChartOutlined` | "查看结果" |
| 向量化 | `PlayCircleOutlined` | "向量化" |
| 查看分块 | `EyeOutlined` | "查看分块" |
| 历史 | `HistoryOutlined` | "历史版本" |
| 导出 | `DownloadOutlined` | "导出" |

**操作列宽度调整**：
- 评估系统：280px → 120px（最多4个图标按钮）
- Prompt管理：调整为 100px
- 数据集管理：调整为 100px
- 知识库管理：调整为 100px
- 文档列表：调整为 120px

### 技术需求

- 使用 `setInterval` 实现轮询，组件卸载时清理
- 使用 `useEffect` 监听 RUNNING 状态任务
- Tooltip 使用 Ant Design `<Tooltip>` 组件
- 保持现有功能逻辑不变，仅修改 UI 展示

## 约束

- 不改变后端 API
- 不改变按钮点击逻辑
- 保持 Popconfirm 删除确认机制
- Tooltip 延迟显示 0.5 秒

## 完成标准

- [x] 评估 RUNNING 任务自动轮询进度
- [x] 轮询状态提示可见
- [x] 重新评估后自动选中任务
- [x] 所有页面操作按钮改为图标+Tooltip
- [x] 操作列宽度合理，无需横向滚动
- [x] 前端构建成功
- [ ] 手动测试各页面按钮功能正常

## 检查点

### Checkpoint 1 - 评估进度轮询

1. 添加 `useEffect` 监听 RUNNING 状态
2. 实现 3 秒轮询逻辑
3. 添加轮询状态提示
4. 重新评估后自动选中任务
5. 任务完成后自动加载结果

**验证**：点击重新评估，进度条自动更新

### Checkpoint 2 - 评估系统按钮精简

1. 操作列按钮改为图标形式
2. 添加 Tooltip 包裹
3. 调整列宽至 120px
4. 测试各按钮功能

**验证**：操作列宽度减少，无需滚动

### Checkpoint 3 - Prompt管理按钮精简

1. 编辑/历史/删除按钮改为图标+Tooltip
2. 调整列宽
3. 测试功能

### Checkpoint 4 - 数据集管理按钮精简

1. 数据集列表按钮精简
2. 数据项列表按钮精简
3. 调整列宽
4. 测试功能

### Checkpoint 5 - 知识库管理按钮精简

1. 知识库列表按钮精简
2. 文档列表按钮精简
3. 调整列宽
4. 测试功能

### Checkpoint 6 - 最终验证

1. 前端构建：`npm run build`
2. 手动测试所有页面按钮功能
3. 验证评估进度轮询正常工作

## 进度日志

### Checkpoint 1 - 2026-04-18

Status: COMPLETED
Branch: main (直接修改)
Completed:
- 添加 polling 状态变量
- 实现 useEffect 监听 RUNNING 任务，3秒轮询
- 添加 Alert 显示"自动刷新中..."
- handleRerun 改为自动选中任务并清空旧结果
- 添加 useEffect 同步 selectedEvaluation 与 evaluations 列表状态
- 任务完成时自动加载结果并切换 Tab
Next: Checkpoint 2
Blockers: None

### Checkpoint 2 - 2026-04-18

Status: COMPLETED
Completed:
- 操作列按钮改为图标+Tooltip形式
- 调整列宽从280px到120px
- 添加 Tooltip import
Next: Checkpoint 3
Blockers: None

### Checkpoint 3 - 2026-04-18

Status: COMPLETED
Completed:
- Prompt管理编辑/历史/删除按钮改为图标+Tooltip
- 调整列宽为100px
- 添加 Tooltip import
Next: Checkpoint 4
Blockers: None

### Checkpoint 4 - 2026-04-18

Status: COMPLETED
Completed:
- 数据集列表按钮改为图标+Tooltip
- 数据项列表按钮改为图标+Tooltip
- 调整列宽：数据集100px，数据项80px
- 添加 EyeOutlined import
Next: Checkpoint 5
Blockers: None

### Checkpoint 5 - 2026-04-18

Status: COMPLETED
Completed:
- 知识库列表按钮改为图标+Tooltip（进入/编辑/删除）
- 文档列表按钮改为图标+Tooltip（向量化/查看分块/删除）
- 调整列宽：知识库100px，文档100px
Next: Checkpoint 6
Blockers: None

### Checkpoint 6 - 2026-04-18

Status: COMPLETED
Completed:
- 前端构建成功 (npm run build)
- 无 TypeScript 错误
- 前端运行在 http://localhost:5173
Next: 手动测试
Blockers: None

---

## 技术实现细节

### 进度轮询实现

```typescript
// Evaluations/index.tsx
const [polling, setPolling] = useState(false)

useEffect(() => {
    const runningTasks = evaluations.filter(e => e.status === 'RUNNING')
    if (runningTasks.length === 0) {
        setPolling(false)
        return
    }

    setPolling(true)
    const interval = setInterval(() => {
        fetchEvaluations()
    }, 3000)

    return () => clearInterval(interval)
}, [evaluations])

// 轮询提示 UI
{polling && (
    <Alert
        message="自动刷新中..."
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
    />
)}

// handleRerun 改进
const handleRerun = async (id: string) => {
    try {
        await rerunEvaluation(id)
        message.success('重新评估任务已启动')
        // 自动选中该任务
        const task = evaluations.find(e => e.id === id)
        if (task) {
            setSelectedEvaluation(task)
        }
        fetchEvaluations()
    } catch {
        message.error('重新评估失败')
    }
}
```

### 按钮精简示例

```typescript
// 之前
<Button type="link" icon={<EditOutlined/>} onClick={() => handleEdit(record)}>
    编辑
</Button>

// 之后
<Tooltip title="编辑">
    <Button type="link" icon={<EditOutlined/>} onClick={() => handleEdit(record)} />
</Tooltip>
```

### 操作列宽度计算

每个图标按钮约占 30px 宽度：
- 4 个按钮 = 120px（评估系统最多状态）
- 3 个按钮 = 90px（常规页面）

建议统一设置为 120px，留有余量。