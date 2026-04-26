import React, {useEffect, useState} from 'react'
import {Button, Form, Input, List, message, Modal, Space, Table, Tag, Tooltip} from 'antd'
import {
    DeleteOutlined,
    EditOutlined,
    ExclamationCircleOutlined,
    PlusOutlined,
    QuestionCircleOutlined,
    ReloadOutlined,
    ToolOutlined
} from '@ant-design/icons'
import type {ColumnsType} from 'antd/es/table'
import {
    createMcpServerFromJson,
    deleteMcpServer,
    getMcpServerTools,
    getReferencingAgents,
    listMcpServers,
    refreshMcpTools,
    updateMcpServerFromJson
} from '@/api/mcp'
import {McpServer} from '@/types/mcp'
import {AgentInfo, Tool} from '@/types/agent'

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

    useEffect(() => {
        fetchData()
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

    const handleCreate = () => {
        setEditingServer(null)
        form.resetFields()
        form.setFieldsValue({
            description: '',
            configJson: `{
  "mcpServers": {
    "my-mcp-server": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-memory"]
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
        ACTIVE: {text: '运行中', color: 'green'},
        INACTIVE: {text: '已停止', color: 'red'},
        ERROR: {text: '异常', color: 'orange'}
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
            width: 100,
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
            <div style={{marginBottom: 16, display: 'flex', justifyContent: 'space-between'}}>
                <div>
                    <h2 style={{marginBottom: 0}}>MCP Server 配置</h2>
                    <p style={{color: '#666', marginTop: 4}}>
                        配置 MCP Server，支持远程 SSE 和本地 Stdio。刷新工具列表后可在 Agent 中绑定。
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
                                        <p style={{color: '#52c41a', marginTop: 8}}><strong>2. 本地 Stdio (如 npx,
                                            uvx)</strong></p>
                                        <pre style={{fontSize: 11, margin: '4px 0'}}>
{`{
  "mcpServers": {
    "memory": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-memory"]
    }
  }
}`}
                                        </pre>
                                        <pre style={{fontSize: 11, margin: '4px 0'}}>
{`{
  "mcpServers": {
    "github-trending": {
      "command": "uvx",
      "args": ["mcp-github-trending"]
    }
  }
}`}
                                        </pre>
                                        <p style={{color: '#999', marginTop: 8, fontSize: 12}}>
                                            注意：本地命令需要先在服务器上安装（如 npx/uvx）
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
                            rows={10}
                            placeholder={`// 支持 SSE 远程或 Stdio 本地两种方式

// 方式1: SSE 远程
{
  "mcpServers": {
    "server-name": {
      "url": "https://mcp-server.example.com/sse"
    }
  }
}

// 方式2: Stdio 本地 (需先安装命令)
{
  "mcpServers": {
    "memory": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-memory"]
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
