import React, {useEffect, useState} from 'react'
import {Button, Form, Input, List, message, Modal, Popconfirm, Space, Table, Tag, Tooltip} from 'antd'
import {
    DeleteOutlined,
    EditOutlined,
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
    listMcpServers,
    refreshMcpTools,
    updateMcpServer,
    updateMcpServerFromJson
} from '@/api/mcp'
import {McpServer} from '@/types/mcp'
import {Tool} from '@/types/agent'

const McpServerPage: React.FC = () => {
    const [data, setData] = useState<McpServer[]>([])
    const [loading, setLoading] = useState(false)
    const [modalVisible, setModalVisible] = useState(false)
    const [toolPreviewVisible, setToolPreviewVisible] = useState(false)
    const [previewTools, setPreviewTools] = useState<Tool[]>([])
    const [previewServerName, setPreviewServerName] = useState('')
    const [editingServer, setEditingServer] = useState<McpServer | null>(null)
    const [refreshingId, setRefreshingId] = useState<string | null>(null)
    const [form] = Form.useForm()

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
    "server-name": {
      "url": "https://mcp-server.example.com/sse"
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

    const handleDelete = async (id: string) => {
        try {
            await deleteMcpServer(id)
            message.success('删除成功')
            fetchData()
        } catch {
            message.error('删除失败')
        }
    }

    // 查看已保存的工具列表
    const handleViewTools = async (record: McpServer) => {
        try {
            const res = await getMcpServerTools(record.id)
            if (res.success) {
                const tools = res.data || []
                if (tools.length > 0) {
                    setPreviewTools(tools)
                    setPreviewServerName(record.name)
                    setToolPreviewVisible(true)
                } else {
                    message.info('暂无已保存的工具，请先刷新工具列表')
                }
            }
        } catch {
            message.error('获取工具列表失败')
        }
    }

    // 刷新工具列表（从MCP Server重新获取）
    const handleRefreshTools = async (record: McpServer) => {
        setRefreshingId(record.id)
        try {
            const res = await refreshMcpTools(record.id)
            if (res.success) {
                const tools = res.data || []
                message.success(`发现 ${tools.length} 个工具`)
                fetchData()
                // 刷新后自动显示工具列表
                if (tools.length > 0) {
                    // 获取已保存的工具列表显示
                    handleViewTools(record)
                }
            }
        } catch {
            message.error('刷新工具失败')
        } finally {
            setRefreshingId(null)
        }
    }

    const handleSubmit = async () => {
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
                // 更新：分别更新描述和配置
                await updateMcpServer(editingServer.id, {
                    description: values.description
                })
                await updateMcpServerFromJson(editingServer.id, configJson)
                message.success('更新成功')
            } else {
                // 创建：先从 JSON 创建，再更新描述
                const created = await createMcpServerFromJson(configJson)
                if (created.success && created.data && created.data.length > 0 && values.description) {
                    await updateMcpServer(created.data[0].id, {
                        description: values.description
                    })
                }
                message.success('创建成功')
            }

            setModalVisible(false)
            fetchData()
        } catch {
            message.error(editingServer ? '更新失败' : '创建失败')
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
                    <Popconfirm title="确定删除此 MCP Server?" onConfirm={() => handleDelete(record.id)}>
                        <Tooltip title="删除">
                            <Button type="text" size="small" danger icon={<DeleteOutlined/>}/>
                        </Tooltip>
                    </Popconfirm>
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
                                    <div>
                                        <p>请输入 MCP Server 配置 JSON，格式如下：</p>
                                        <pre style={{fontSize: 11, margin: 0}}>
{`// 远程 SSE Server
{
  "mcpServers": {
    "server-name": {
      "url": "https://..."
    }
  }
}

// 本地 Stdio Server
{
  "mcpServers": {
    "memory": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-memory"]
    }
  }
}`}
                    </pre>
                                        <p style={{marginTop: 4}}>系统根据 url 或 command 自动识别类型</p>
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
                            placeholder={`{
  "mcpServers": {
    "server-name": {
      "url": "https://..."
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
        </div>
    )
}

export default McpServerPage