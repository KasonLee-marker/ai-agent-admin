import React, {useEffect, useState} from 'react'
import {Button, Card, Descriptions, Form, Input, List, message, Modal, Space, Table, Tag, Tooltip} from 'antd'
import {
    ContainerOutlined,
    DeleteOutlined,
    EditOutlined,
    ExclamationCircleOutlined,
    FileTextOutlined,
    PlayCircleOutlined,
    PlusOutlined,
    QuestionCircleOutlined,
    ReloadOutlined,
    StopOutlined,
    ToolOutlined
} from '@ant-design/icons'
import type {ColumnsType} from 'antd/es/table'
import {
    createMcpServerFromJson,
    deleteMcpServer,
    getMcpServerTools,
    getProcessLogs,
    getReferencingAgents,
    getRuntimeLogs,
    getRuntimeStatus,
    listMcpServers,
    refreshMcpTools,
    restartProcess,
    startRuntime,
    stopRuntime,
    updateMcpServerFromJson
} from '@/api/mcp'
import type {McpServer} from '@/types/mcp'
import type {AgentInfo, Tool} from '@/types/agent'

const McpServerPage: React.FC = () => {
    const [data, setData] = useState<McpServer[]>([])
    const [loading, setLoading] = useState(false)
    const [modalVisible, setModalVisible] = useState(false)
    const [submitLoading, setSubmitLoading] = useState(false)
    const [toolPreviewVisible, setToolPreviewVisible] = useState(false)
    const [previewTools, setPreviewTools] = useState<Tool[]>([])
    const [previewServerName, setPreviewServerName] = useState('')
    const [editingServer, setEditingServer] = useState<McpServer | null>(null)
    const [refreshingId, setRefreshingId] = useState<string | null>(null)
    const [form] = Form.useForm()

    // 删除引用确认弹窗状态
    const [deleteConfirmVisible, setDeleteConfirmVisible] = useState(false)
    const [deletingServer, setDeletingServer] = useState<McpServer | null>(null)
    const [referencingAgents, setReferencingAgents] = useState<AgentInfo[]>([])
    const [checkingReferences, setCheckingReferences] = useState(false)

    // Docker Runtime 状态
    const [runtimeStatus, setRuntimeStatus] = useState<{
        containerId?: string
        running: boolean
        state: string
        health: string
    } | null>(null)
    const [runtimeLoading, setRuntimeLoading] = useState(false)

    // 进程日志 Modal
    const [logModalVisible, setLogModalVisible] = useState(false)
    const [logServerId, setLogServerId] = useState<string>('')
    const [logServerName, setLogServerName] = useState<string>('')
    const [processLogs, setProcessLogs] = useState<string>('')
    const [logsLoading, setLogsLoading] = useState(false)

    // Runtime 日志 Modal
    const [runtimeLogModalVisible, setRuntimeLogModalVisible] = useState(false)
    const [runtimeLogs, setRuntimeLogs] = useState<string>('')
    const [runtimeLogsLoading, setRuntimeLogsLoading] = useState(false)

    useEffect(() => {
        fetchData()
        fetchRuntimeStatus()
    }, [])

    const fetchData = async () => {
        setLoading(true)
        try {
            const res = await listMcpServers()
            if (res.success) {
                setData(res.data || [])
            }
        } finally {
            setLoading(false)
        }
    }

    const fetchRuntimeStatus = async () => {
        setRuntimeLoading(true)
        try {
            const res = await getRuntimeStatus()
            if (res.success) {
                setRuntimeStatus(res.data)
            }
        } finally {
            setRuntimeLoading(false)
        }
    }

    const handleStartRuntime = async () => {
        setRuntimeLoading(true)
        try {
            const res = await startRuntime()
            if (res.success) {
                message.success('MCP Runtime 容器已启动')
                // 轮询刷新状态，直到容器真正运行
                await pollRuntimeStatus(true, 10, 1000)
            }
        } catch (error) {
            message.error('启动容器失败')
        } finally {
            setRuntimeLoading(false)
        }
    }

    const handleStopRuntime = async () => {
        setRuntimeLoading(true)
        try {
            await stopRuntime()
            message.success('MCP Runtime 容器已停止')
            // 轮询刷新状态，直到容器真正停止
            await pollRuntimeStatus(false, 10, 1000)
        } catch (error) {
            message.error('停止容器失败')
        } finally {
            setRuntimeLoading(false)
        }
    }

    // 轮询检查容器状态
    const pollRuntimeStatus = async (expectedRunning: boolean, maxAttempts: number, interval: number) => {
        for (let i = 0; i < maxAttempts; i++) {
            await new Promise(resolve => setTimeout(resolve, interval))
            try {
                const res = await getRuntimeStatus()
                if (res.success && res.data) {
                    setRuntimeStatus(res.data)
                    // 检查状态是否达到预期
                    if (res.data.running === expectedRunning) {
                        return
                    }
                }
            } catch (e) {
                // 忽略错误，继续轮询
            }
        }
        // 轮询结束，最后一次刷新
        await fetchRuntimeStatus()
    }

    const handleViewRuntimeLogs = async () => {
        setRuntimeLogsLoading(true)
        setRuntimeLogModalVisible(true)
        try {
            const res = await getRuntimeLogs(100)
            if (res.success) {
                setRuntimeLogs(res.data.logs)
            }
        } finally {
            setRuntimeLogsLoading(false)
        }
    }

    const handleCreate = () => {
        setEditingServer(null)
        form.resetFields()
        form.setFieldsValue({
            description: '',
            configJson: `{
  "mcpServers": {
    "my-mcp-server": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-memory"],
      "runtimeMode": "DOCKER"
    }
  }
}`
        })
        setModalVisible(true)
    }

    const handleEdit = (record: McpServer) => {
        setEditingServer(record)
        const serverConfig: Record<string, unknown> = {}
        if (record.transportType === 'sse') {
            serverConfig['url'] = record.url
        } else {
            serverConfig['command'] = record.command
            serverConfig['runtimeMode'] = record.runtimeMode || 'DOCKER'
            if (record.args && record.args.length > 0) {
                serverConfig['args'] = record.args
            }
        }

        const configJson = JSON.stringify({
            mcpServers: {
                [record.name]: serverConfig
            }
        }, null, 2)

        form.setFieldsValue({
            description: record.description || '',
            configJson
        })
        setModalVisible(true)
    }

    // 点击删除按钮时先检查引用
    const handleDeleteClick = async (record: McpServer) => {
        setDeletingServer(record)
        setCheckingReferences(true)
        setDeleteConfirmVisible(true)
        try {
            const res = await getReferencingAgents(record.id)
            if (res.success) {
                setReferencingAgents(res.data || [])
            } else {
                message.error(res.message || '检查引用失败')
                setDeleteConfirmVisible(false)
            }
        } catch (error: unknown) {
            const axiosError = error as { response?: { data?: { message?: string } } }
            message.error(axiosError.response?.data?.message || '检查引用失败')
            setDeleteConfirmVisible(false)
        } finally {
            setCheckingReferences(false)
        }
    }

    // 确认删除
    const handleConfirmDelete = async () => {
        if (!deletingServer) return
        try {
            const res = await deleteMcpServer(deletingServer.id) as { success: boolean; message?: string }
            if (res.success) {
                message.success('删除成功')
                setDeleteConfirmVisible(false)
                setDeletingServer(null)
                setReferencingAgents([])
                fetchData()
            } else {
                message.error(res.message || '删除失败')
            }
        } catch (error: unknown) {
            const axiosError = error as { response?: { data?: { message?: string } } }
            message.error(axiosError.response?.data?.message || '删除失败')
        }
    }

    // 取消删除
    const handleCancelDelete = () => {
        setDeleteConfirmVisible(false)
        setDeletingServer(null)
        setReferencingAgents([])
    }

    // 查看已保存的工具列表
    const handleViewTools = async (record: McpServer) => {
        try {
            const res = await getMcpServerTools(record.id) as { success: boolean; data?: Tool[]; message?: string }
            if (res.success) {
                const tools = res.data || []
                if (tools.length > 0) {
                    setPreviewTools(tools)
                    setPreviewServerName(record.name)
                    setToolPreviewVisible(true)
                } else {
                    message.info('暂无已保存的工具，请先刷新工具列表')
                }
            } else {
                message.error(res.message || '获取工具列表失败')
            }
        } catch (error: unknown) {
            const axiosError = error as { response?: { data?: { message?: string } } }
            message.error(axiosError.response?.data?.message || '获取工具列表失败')
        }
    }

    // 刷新工具列表（从MCP Server重新获取）
    const handleRefreshTools = async (record: McpServer) => {
        setRefreshingId(record.id)
        try {
            const res = await refreshMcpTools(record.id) as { success: boolean; data?: unknown[]; message?: string }
            if (res.success) {
                const tools = res.data || []
                message.success(`发现 ${tools.length} 个工具`)
                fetchData()
                // 刷新后自动显示工具列表
                if (tools.length > 0) {
                    // 获取已保存的工具列表显示
                    handleViewTools(record)
                }
            } else {
                message.error(res.message || '刷新工具失败')
            }
        } catch (error: unknown) {
            const axiosError = error as { response?: { data?: { message?: string } } }
            message.error(axiosError.response?.data?.message || '刷新工具失败')
        } finally {
            setRefreshingId(null)
        }
    }

    // 查看进程日志
    const handleViewLogs = async (record: McpServer) => {
        setLogServerId(record.id)
        setLogServerName(record.name)
        setLogModalVisible(true)
        setLogsLoading(true)
        try {
            const res = await getProcessLogs(record.id, 100)
            if (res.success) {
                setProcessLogs(res.data || '无日志')
            }
        } finally {
            setLogsLoading(false)
        }
    }

    // 重启进程
    const handleRestartProcess = async (record: McpServer) => {
        try {
            const res = await restartProcess(record.id)
            if (res.success && res.data) {
                message.success('进程重启成功')
                fetchData()
            } else {
                message.error('进程重启失败')
            }
        } catch (error) {
            message.error('进程重启失败')
        }
    }

    const handleSubmit = async () => {
        setSubmitLoading(true)
        try {
            const values = await form.validateFields()
            const configJson = values.configJson

            // 验证 JSON 格式
            try {
                JSON.parse(configJson)
            } catch {
                message.error('JSON 格式不正确')
                return
            }

            if (editingServer) {
                // 编辑：一次调用同时更新配置和描述
                const res = await updateMcpServerFromJson(editingServer.id, configJson, values.description) as {
                    success: boolean;
                    message?: string
                }
                if (res.success) {
                    message.success('更新成功')
                    setModalVisible(false)
                    fetchData()
                } else {
                    message.error(res.message || '更新失败')
                }
            } else {
                // 创建：直接传递 description，不再单独调用更新
                const res = await createMcpServerFromJson(configJson, values.description) as {
                    success: boolean;
                    message?: string
                }
                if (res.success) {
                    message.success('创建成功')
                    setModalVisible(false)
                    fetchData()
                } else {
                    message.error(res.message || '创建失败')
                }
            }
        } catch (error: unknown) {
            const axiosError = error as { response?: { data?: { message?: string } } }
            message.error(axiosError.response?.data?.message || (editingServer ? '更新失败' : '创建失败'))
        } finally {
            setSubmitLoading(false)
        }
    }

    // 状态中文映射
    const statusMap: Record<string, { text: string; color: string }> = {
        ACTIVE: {text: '正常', color: 'green'},
        INACTIVE: {text: '停用', color: 'red'},
        ERROR: {text: '异常', color: 'orange'}
    }

    // 进程状态映射
    const processStatusMap: Record<string, { text: string; color: string; icon: React.ReactNode }> = {
        RUNNING: {text: '运行中', color: 'green', icon: <ContainerOutlined/>},
        STOPPED: {text: '已停止', color: 'red', icon: <StopOutlined/>},
        ERROR: {text: '异常', color: 'orange', icon: <ExclamationCircleOutlined/>},
        NOT_FOUND: {text: '未启动', color: 'gray', icon: <ContainerOutlined/>},
        INSTALLING: {text: '安装中', color: 'blue', icon: <ContainerOutlined/>},
        // 小写状态映射
        running: {text: '运行中', color: 'green', icon: <ContainerOutlined/>},
        stopped: {text: '已停止', color: 'red', icon: <StopOutlined/>},
        error: {text: '异常', color: 'orange', icon: <ExclamationCircleOutlined/>},
        not_found: {text: '未启动', color: 'gray', icon: <ContainerOutlined/>},
        installing: {text: '安装中', color: 'blue', icon: <ContainerOutlined/>},
        unknown: {text: '未知', color: 'gray', icon: <ContainerOutlined/>}
    }

    const columns: ColumnsType<McpServer> = [
        {title: '名称', dataIndex: 'name', key: 'name', width: 150},
        {title: '描述', dataIndex: 'description', key: 'description', ellipsis: true},
        {
            title: '类型',
            dataIndex: 'transportType',
            key: 'transportType',
            width: 100,
            render: (type: string) => (
                <Tag color={type === 'sse' ? 'blue' : 'green'}>
                    {type === 'sse' ? '远程 SSE' : '本地 Stdio'}
                </Tag>
            )
        },
        {
            title: '运行模式',
            dataIndex: 'runtimeMode',
            key: 'runtimeMode',
            width: 100,
            render: (mode: string, record: McpServer) => {
                if (record.transportType === 'sse') {
                    return <Tag>不适用</Tag>
                }
                return (
                    <Tag color={mode === 'DOCKER' ? 'purple' : 'cyan'}>
                        {mode === 'DOCKER' ? 'Docker' : '本地'}
                    </Tag>
                )
            }
        },
        {
            title: '连接配置',
            key: 'config',
            ellipsis: true,
            render: (_, record) => {
                if (record.transportType === 'sse') {
                    return record.url
                }
                const cmd = record.command
                const args = record.args?.join(' ') || ''
                return `${cmd} ${args}`
            }
        },
        {
            title: '进程状态',
            key: 'processStatus',
            width: 100,
            render: (_, record) => {
                if (record.transportType === 'sse') {
                    return <Tag>远程</Tag>
                }
                const status = record.processStatus || 'NOT_FOUND'
                const mapped = processStatusMap[status] || {text: status, color: 'default', icon: null}
                return (
                    <Tag color={mapped.color} icon={mapped.icon}>
                        {mapped.text}
                    </Tag>
                )
            }
        },
        {
            title: '工具',
            key: 'tools',
            width: 80,
            render: (_, record) => {
                const toolCount = record.toolCount || 0
                return (
                    <Tooltip title={toolCount > 0 ? '点击查看工具列表' : '点击刷新后可查看工具列表'}>
                        <Tag
                            color="purple"
                            style={{cursor: toolCount > 0 ? 'pointer' : 'default'}}
                            onClick={() => toolCount > 0 && handleViewTools(record)}
                        >
                            {toolCount} 个
                        </Tag>
                    </Tooltip>
                )
            }
        },
        {
            title: '状态',
            dataIndex: 'status',
            key: 'status',
            width: 80,
            render: (status: string) => {
                const mapped = statusMap[status] || {text: status, color: 'default'}
                return <Tag color={mapped.color}>{mapped.text}</Tag>
            }
        },
        {
            title: '操作',
            key: 'action',
            width: 150,
            render: (_, record) => (
                <Space size="small">
                    <Tooltip title="编辑">
                        <Button
                            type="text"
                            size="small"
                            icon={<EditOutlined/>}
                            onClick={() => handleEdit(record)}
                        />
                    </Tooltip>
                    <Tooltip title="刷新工具列表">
                        <Button
                            type="text"
                            size="small"
                            icon={<ReloadOutlined spin={refreshingId === record.id}/>}
                            loading={refreshingId === record.id}
                            onClick={() => handleRefreshTools(record)}
                        />
                    </Tooltip>
                    {record.transportType === 'stdio' && record.runtimeMode === 'DOCKER' && (
                        <>
                            <Tooltip title="查看日志">
                                <Button
                                    type="text"
                                    size="small"
                                    icon={<FileTextOutlined/>}
                                    onClick={() => handleViewLogs(record)}
                                />
                            </Tooltip>
                            <Tooltip title="重启进程">
                                <Button
                                    type="text"
                                    size="small"
                                    icon={<ReloadOutlined/>}
                                    onClick={() => handleRestartProcess(record)}
                                />
                            </Tooltip>
                        </>
                    )}
                    <Tooltip title="删除">
                        <Button
                            type="text"
                            size="small"
                            danger
                            icon={<DeleteOutlined/>}
                            onClick={() => handleDeleteClick(record)}
                        />
                    </Tooltip>
                </Space>
            )
        }
    ]

    return (
        <div>
            {/* MCP Runtime 状态卡片 */}
            <Card
                title={<Space><ContainerOutlined/> MCP Runtime 容器状态</Space>}
                style={{marginBottom: 16}}
                loading={runtimeLoading}
                extra={
                    <Space>
                        <Button
                            size="small"
                            icon={<FileTextOutlined/>}
                            onClick={handleViewRuntimeLogs}
                        >
                            查看日志
                        </Button>
                        {runtimeStatus?.running ? (
                            <Button
                                size="small"
                                danger
                                icon={<StopOutlined/>}
                                onClick={handleStopRuntime}
                                loading={runtimeLoading}
                            >
                                停止容器
                            </Button>
                        ) : (
                            <Button
                                size="small"
                                type="primary"
                                icon={<PlayCircleOutlined/>}
                                onClick={handleStartRuntime}
                                loading={runtimeLoading}
                            >
                                启动容器
                            </Button>
                        )}
                    </Space>
                }
            >
                {runtimeStatus && (
                    <Descriptions size="small" column={4}>
                        <Descriptions.Item label="容器ID">
                            {runtimeStatus.containerId?.substring(0, 12) || 'N/A'}
                        </Descriptions.Item>
                        <Descriptions.Item label="运行状态">
                            <Tag color={runtimeStatus.running ? 'green' : 'red'}>
                                {runtimeStatus.running ? '运行中' : '已停止'}
                            </Tag>
                        </Descriptions.Item>
                        <Descriptions.Item label="状态">
                            {runtimeStatus.state === 'running' ? '运行中' : 
                             runtimeStatus.state === 'not_available' ? '不可用' : 
                             runtimeStatus.state === 'exited' ? '已退出' : 
                             runtimeStatus.state}
                        </Descriptions.Item>
                        <Descriptions.Item label="健康检查">
                            {runtimeStatus.health === 'healthy' ? '健康' : 
                             runtimeStatus.health === 'unhealthy' ? '不健康' : 
                             runtimeStatus.health === 'unknown' ? '未知' : 
                             runtimeStatus.health || '-'}
                        </Descriptions.Item>
                    </Descriptions>
                )}
                {!runtimeStatus && (
                    <div style={{color: '#999'}}>无法获取容器状态</div>
                )}
            </Card>

            <div style={{marginBottom: 16, display: 'flex', justifyContent: 'space-between'}}>
                <div>
                    <h2 style={{marginBottom: 0}}>MCP Server 配置</h2>
                    <p style={{color: '#666', marginTop: 4}}>
                        配置 MCP Server，支持远程 SSE 和本地 Stdio。Docker 模式下所有 stdio MCP Server
                        共享一个容器运行。
                    </p>
                </div>
                <Button type="primary" icon={<PlusOutlined/>} onClick={handleCreate}>
                    添加 MCP Server
                </Button>
            </div>

            <Table
                columns={columns}
                dataSource={data}
                rowKey="id"
                loading={loading}
            />

            {/* 添加/编辑 MCP Server Modal */}
            <Modal
                title={editingServer ? '编辑 MCP Server' : '添加 MCP Server'}
                open={modalVisible}
                onOk={handleSubmit}
                onCancel={() => setModalVisible(false)}
                width={700}
                confirmLoading={submitLoading}
            >
                <Form form={form} layout="vertical">
                    <Form.Item
                        name="description"
                        label="描述"
                    >
                        <Input placeholder="请输入 MCP Server 描述（可选）"/>
                    </Form.Item>

                    <Form.Item
                        name="configJson"
                        label={
                            <Space>
                                MCP Server 配置 JSON
                                <Tooltip title={
                                    <div style={{maxWidth: 400}}>
                                        <p>支持两种配置方式：</p>
                                        <p style={{color: '#1890ff', marginTop: 8}}><strong>1. 远程 SSE</strong></p>
                                        <pre style={{fontSize: 11, margin: '4px 0'}}>
{`{
  "mcpServers": {
    "server-name": {
      "url": "https://mcp-server.example.com/sse"
    }
  }
}`}
                                        </pre>
                                        <p style={{color: '#52c41a', marginTop: 8}}><strong>2. 本地 Stdio (Docker
                                            模式)</strong></p>
                                        <pre style={{fontSize: 11, margin: '4px 0'}}>
{`{
  "mcpServers": {
    "memory": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-memory"],
      "runtimeMode": "DOCKER"
    }
  }
}`}
                                        </pre>
                                        <pre style={{fontSize: 11, margin: '4px 0'}}>
{`{
  "mcpServers": {
    "github-trending": {
      "command": "uvx",
      "args": ["mcp-github-trending"],
      "runtimeMode": "DOCKER"
    }
  }
}`}
                                        </pre>
                                        <p style={{color: '#999', marginTop: 8, fontSize: 12}}>
                                            runtimeMode: DOCKER = 在 Docker 容器中运行（推荐）
                                            <br/>
                                            runtimeMode: LOCAL = 在本地子进程中运行
                                        </p>
                                    </div>
                                }>
                                    <QuestionCircleOutlined style={{color: '#1890ff'}}/>
                                </Tooltip>
                            </Space>
                        }
                        rules={[{required: true, message: '请输入配置 JSON'}]}
                    >
                        <Input.TextArea
                            rows={12}
                            placeholder={`// 支持 SSE 远程或 Stdio 本地两种方式

// 方式1: SSE 远程
{
  "mcpServers": {
    "server-name": {
      "url": "https://mcp-server.example.com/sse"
    }
  }
}

// 方式2: Stdio 本地 (Docker 模式推荐)
{
  "mcpServers": {
    "memory": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-memory"],
      "runtimeMode": "DOCKER"
    }
  }
}`}
                            style={{fontFamily: 'monospace'}}
                        />
                    </Form.Item>
                </Form>
            </Modal>

            {/* 工具预览 Modal */}
            <Modal
                title={<Space><ToolOutlined/> {previewServerName} 工具列表</Space>}
                open={toolPreviewVisible}
                onCancel={() => setToolPreviewVisible(false)}
                footer={null}
                width={600}
            >
                <List
                    dataSource={previewTools}
                    renderItem={(tool) => (
                        <List.Item>
                            <List.Item.Meta
                                title={<Space><strong>{tool.name}</strong> <Tag color="purple">MCP</Tag></Space>}
                                description={tool.description}
                            />
                        </List.Item>
                    )}
                />
            </Modal>

            {/* 进程日志 Modal */}
            <Modal
                title={`${logServerName} - 进程日志`}
                open={logModalVisible}
                onCancel={() => setLogModalVisible(false)}
                footer={
                    <Button onClick={() => setLogModalVisible(false)}>
                        关闭
                    </Button>
                }
                width={800}
            >
                <pre
                    style={{
                        background: '#f5f5f5',
                        padding: 12,
                        borderRadius: 4,
                        maxHeight: 400,
                        overflow: 'auto',
                        fontSize: 12,
                        fontFamily: 'monospace'
                    }}
                >
                    {logsLoading ? '加载中...' : processLogs}
                </pre>
            </Modal>

            {/* Runtime 日志 Modal */}
            <Modal
                title="MCP Runtime 容器日志"
                open={runtimeLogModalVisible}
                onCancel={() => setRuntimeLogModalVisible(false)}
                footer={
                    <Button onClick={() => setRuntimeLogModalVisible(false)}>
                        关闭
                    </Button>
                }
                width={900}
            >
                <pre
                    style={{
                        background: '#1a1a2e',
                        color: '#eee',
                        padding: 12,
                        borderRadius: 4,
                        maxHeight: 500,
                        overflow: 'auto',
                        fontSize: 12,
                        fontFamily: 'monospace'
                    }}
                >
                    {runtimeLogsLoading ? '加载中...' : (runtimeLogs || '暂无日志')}
                </pre>
            </Modal>

            {/* 删除确认弹窗 - 显示引用信息 */}
            <Modal
                title={
                    <Space>
                        <ExclamationCircleOutlined style={{color: '#faad14'}}/>
                        {referencingAgents.length > 0 ? '确认删除 MCP Server' : '删除 MCP Server'}
                    </Space>
                }
                open={deleteConfirmVisible}
                onOk={handleConfirmDelete}
                onCancel={handleCancelDelete}
                confirmLoading={checkingReferences}
                okText={referencingAgents.length > 0 ? '确认删除（将自动解绑）' : '确认删除'}
                okButtonProps={{danger: true}}
            >
                {checkingReferences ? (
                    <div style={{padding: 20, textAlign: 'center'}}>正在检查引用...</div>
                ) : referencingAgents.length > 0 ? (
                    <div>
                        <p>
                            <strong>{deletingServer?.name}</strong> 正在被以下 <strong>{referencingAgents.length}</strong> 个
                            Agent 引用：
                        </p>
                        <div style={{
                            maxHeight: 200,
                            overflow: 'auto',
                            border: '1px solid #d9d9d9',
                            borderRadius: 6,
                            padding: '8px 16px',
                            marginBottom: 16
                        }}>
                            <List
                                size="small"
                                dataSource={referencingAgents}
                                renderItem={(agent) => (
                                    <List.Item style={{padding: '4px 0'}}>
                                        <Tag color="blue">{agent.name}</Tag>
                                        <Tag color={agent.status === 'PUBLISHED' ? 'green' : 'default'}>
                                            {agent.status === 'DRAFT' ? '草稿' : agent.status === 'PUBLISHED' ? '已发布' : '已归档'}
                                        </Tag>
                                    </List.Item>
                                )}
                            />
                        </div>
                        <p style={{color: '#ff4d4f'}}>
                            删除后，上述 Agent 将自动解绑该 MCP Server 下的所有工具。此操作不可撤销。
                        </p>
                    </div>
                ) : (
                    <p>
                        确定要删除 <strong>{deletingServer?.name}</strong> 吗？
                        <br/>
                        <span style={{color: '#666', fontSize: 12}}>该 MCP Server 没有被任何 Agent 引用。</span>
                    </p>
                )}
            </Modal>
        </div>
    )
}

export default McpServerPage
