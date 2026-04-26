import React, {useEffect, useState} from 'react'
import {Badge, Button, Card, Form, Input, message, Modal, Popconfirm, Select, Space, Table, Tag, Tooltip} from 'antd'
import {
    DeleteOutlined,
    EditOutlined,
    InboxOutlined,
    PlayCircleOutlined,
    PlusOutlined,
    RocketOutlined
} from '@ant-design/icons'
import type {ColumnsType} from 'antd/es/table'
import {useNavigate} from 'react-router-dom'
import {
    bindTool,
    createAgent,
    deleteAgent,
    getBuiltinTools,
    listAgents,
    updateAgent,
    updateAgentStatus
} from '@/api/agent'
import {Agent, CreateAgentRequest, Tool, ToolBindingRequest} from '@/types/agent'
import {listModels} from '@/api/models'
import {ModelConfig} from '@/types/model'

const AgentListPage: React.FC = () => {
    const navigate = useNavigate()
    const [data, setData] = useState<Agent[]>([])
    const [models, setModels] = useState<ModelConfig[]>([])
    const [builtinTools, setBuiltinTools] = useState<Tool[]>([])
    const [loading, setLoading] = useState(false)
    const [modalVisible, setModalVisible] = useState(false)
    const [toolModalVisible, setToolModalVisible] = useState(false)
    const [editingAgent, setEditingAgent] = useState<Agent | null>(null)
    const [selectedTools, setSelectedTools] = useState<Tool[]>([])
    const [form] = Form.useForm()
    const [keyword, setKeyword] = useState('')

    useEffect(() => {
        fetchData()
        fetchModels()
        fetchBuiltinTools()
    }, [])

    const fetchData = async () => {
        setLoading(true)
        try {
            const res = await listAgents({keyword})
            if (res.success) {
                setData(res.data || [])
            }
        } finally {
            setLoading(false)
        }
    }

    const fetchModels = async () => {
        try {
            const res = await listModels()
            if (res.success) {
                setModels(res.data?.filter(m => m.modelType === 'CHAT' && m.isActive) || [])
            }
        } catch {
            // ignore
        }
    }

    const fetchBuiltinTools = async () => {
        try {
            const res = await getBuiltinTools()
            if (res.success) {
                setBuiltinTools(res.data || [])
            }
        } catch {
            // ignore
        }
    }

    const handleCreate = () => {
        setEditingAgent(null)
        setSelectedTools([])
        form.resetFields()
        form.setFieldsValue({
            config: {temperature: 0.7, maxTokens: 4096}
        })
        setModalVisible(true)
    }

    const handleEdit = (record: Agent) => {
        setEditingAgent(record)
        form.setFieldsValue({
            name: record.name,
            description: record.description,
            modelId: record.modelId,
            systemPrompt: record.systemPrompt,
            config: record.config
        })
        setModalVisible(true)
    }

    const handleDelete = async (id: string) => {
        try {
            await deleteAgent(id)
            message.success('删除成功')
            fetchData()
        } catch {
            message.error('删除失败')
        }
    }

    const handlePublish = async (id: string) => {
        try {
            await updateAgentStatus(id, {status: 'PUBLISHED'})
            message.success('发布成功')
            fetchData()
        } catch {
            message.error('发布失败')
        }
    }

    const handleArchive = async (id: string) => {
        try {
            await updateAgentStatus(id, {status: 'ARCHIVED'})
            message.success('已归档')
            fetchData()
        } catch {
            message.error('归档失败')
        }
    }

    const handleTest = (id: string) => {
        navigate(`/agents/${id}`)
    }

    const handleSubmit = async () => {
        try {
            const values = await form.validateFields()
            const requestData: CreateAgentRequest = {
                name: values.name,
                description: values.description,
                modelId: values.modelId,
                systemPrompt: values.systemPrompt,
                config: values.config
            }

            if (editingAgent) {
                await updateAgent(editingAgent.id, requestData)
                message.success('更新成功')
            } else {
                const res = await createAgent(requestData)
                if (res.success && res.data) {
                    // 绑定选中的工具
                    for (const tool of selectedTools) {
                        const binding: ToolBindingRequest = {
                            toolId: tool.id,
                            enabled: true,
                            config: {}
                        }
                        await bindTool(res.data.id, binding)
                    }
                    message.success('创建成功')
                }
            }
            setModalVisible(false)
            fetchData()
        } catch {
            message.error('操作失败')
        }
    }

    const handleAddTools = () => {
        setToolModalVisible(true)
    }

    const handleSelectTool = (tool: Tool) => {
        if (!selectedTools.find(t => t.id === tool.id)) {
            setSelectedTools([...selectedTools, tool])
        }
        setToolModalVisible(false)
    }

    const handleRemoveTool = (toolId: string) => {
        setSelectedTools(selectedTools.filter(t => t.id !== toolId))
    }

    const statusMap = {
        DRAFT: {status: 'default' as const, text: '草稿'},
        PUBLISHED: {status: 'success' as const, text: '已发布'},
        ARCHIVED: {status: 'warning' as const, text: '已归档'},
    }

    const columns: ColumnsType<Agent> = [
        {
            title: '名称',
            dataIndex: 'name',
            key: 'name',
            render: (name: string, record) => (
                <a
                    style={{color: '#1890ff'}}
                    onClick={() => navigate(`/agents/${record.id}`)}
                >
                    {name}
                </a>
            )
        },
        {title: '描述', dataIndex: 'description', key: 'description', ellipsis: true},
        {title: '模型', dataIndex: 'modelName', key: 'modelName'},
        {
            title: '状态',
            dataIndex: 'status',
            key: 'status',
            render: (status: keyof typeof statusMap) => (
                <Badge {...statusMap[status]} />
            )
        },
        {
            title: '工具',
            key: 'tools',
            render: (_, record) => (
                <Tag>{record.tools?.length || 0} 个工具</Tag>
            )
        },
        {title: '版本', dataIndex: 'version', key: 'version'},
        {
            title: '操作',
            key: 'action',
            width: 120,
            render: (_, record) => (
                <Space size="small">
                    <Tooltip title="测试对话">
                        <Button
                            type="text"
                            size="small"
                            icon={<PlayCircleOutlined/>}
                            onClick={(e) => {
                                e.stopPropagation();
                                handleTest(record.id)
                            }}
                        />
                    </Tooltip>
                    {record.status === 'DRAFT' && (
                        <Tooltip title="发布">
                            <Button
                                type="text"
                                size="small"
                                icon={<RocketOutlined/>}
                                onClick={(e) => {
                                    e.stopPropagation();
                                    handlePublish(record.id)
                                }}
                            />
                        </Tooltip>
                    )}
                    {record.status === 'PUBLISHED' && (
                        <Tooltip title="归档">
                            <Button
                                type="text"
                                size="small"
                                icon={<InboxOutlined/>}
                                onClick={(e) => {
                                    e.stopPropagation();
                                    handleArchive(record.id)
                                }}
                            />
                        </Tooltip>
                    )}
                    <Tooltip title="编辑">
                        <Button
                            type="text"
                            size="small"
                            icon={<EditOutlined/>}
                            onClick={(e) => {
                                e.stopPropagation();
                                handleEdit(record)
                            }}
                        />
                    </Tooltip>
                    <Popconfirm title="确定删除此 Agent?" onConfirm={() => handleDelete(record.id)}>
                        <Tooltip title="删除">
                            <Button type="text" size="small" danger icon={<DeleteOutlined/>}
                                    onClick={(e) => e.stopPropagation()}/>
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
                    <h2 style={{marginBottom: 0}}>Agent 管理</h2>
                    <p style={{color: '#666', marginTop: 4}}>
                        创建 AI Agent，配置模型和工具，测试对话效果
                    </p>
                </div>
                <Button type="primary" icon={<PlusOutlined/>} onClick={handleCreate}>
                    创建 Agent
                </Button>
            </div>

            <div style={{marginBottom: 16}}>
                <Input.Search
                    placeholder="搜索 Agent 名称"
                    allowClear
                    onSearch={fetchData}
                    onChange={e => setKeyword(e.target.value)}
                    style={{width: 300}}
                />
            </div>

            <Table
                columns={columns}
                dataSource={data}
                rowKey="id"
                loading={loading}
            />

            {/* 创建/编辑 Agent Modal */}
            <Modal
                title={editingAgent ? '编辑 Agent' : '创建 Agent'}
                open={modalVisible}
                onOk={handleSubmit}
                onCancel={() => setModalVisible(false)}
                width={700}
            >
                <Form form={form} layout="vertical">
                    <Form.Item
                        name="name"
                        label="名称"
                        rules={[{required: true, message: '请输入 Agent 名称'}]}
                    >
                        <Input placeholder="例如: 数学助手"/>
                    </Form.Item>
                    <Form.Item name="description" label="描述">
                        <Input.TextArea placeholder="描述 Agent 的用途和能力" rows={2}/>
                    </Form.Item>
                    <Form.Item
                        name="modelId"
                        label="模型"
                        rules={[{required: true, message: '请选择模型'}]}
                    >
                        <Select placeholder="选择对话模型">
                            {models.map(m => (
                                <Select.Option key={m.id} value={m.id}>
                                    {m.name} ({m.modelName})
                                </Select.Option>
                            ))}
                        </Select>
                    </Form.Item>
                    <Form.Item name="systemPrompt" label="系统提示词">
                        <Input.TextArea
                            placeholder="定义 Agent 的角色、行为和能力，例如：你是一个数学助手，可以使用calculator工具计算数学表达式..."
                            rows={4}
                        />
                    </Form.Item>

                    {!editingAgent && (
                        <Form.Item label="绑定工具">
                            <div>
                                {selectedTools.length > 0 && (
                                    <Space style={{marginBottom: 8}}>
                                        {selectedTools.map(tool => (
                                            <Tag
                                                key={tool.id}
                                                closable
                                                onClose={() => handleRemoveTool(tool.id)}
                                            >
                                                {tool.name}
                                            </Tag>
                                        ))}
                                    </Space>
                                )}
                                <Button icon={<PlusOutlined/>} onClick={handleAddTools}>
                                    添加工具
                                </Button>
                            </div>
                        </Form.Item>
                    )}

                    <Card title="运行配置" size="small">
                        <Form.Item name={['config', 'temperature']} label="Temperature" extra="控制输出随机性，0-2">
                            <Input type="number" min={0} max={2} step={0.1}/>
                        </Form.Item>
                        <Form.Item name={['config', 'maxTokens']} label="Max Tokens" extra="最大输出长度">
                            <Input type="number" min={1} max={100000}/>
                        </Form.Item>
                    </Card>
                </Form>
            </Modal>

            {/* 工具选择 Modal */}
            <Modal
                title="选择工具"
                open={toolModalVisible}
                onCancel={() => setToolModalVisible(false)}
                footer={null}
                width={500}
            >
                <div>
                    {builtinTools.map(tool => (
                        <Card
                            key={tool.id}
                            size="small"
                            style={{marginBottom: 8, cursor: 'pointer'}}
                            onClick={() => handleSelectTool(tool)}
                            hoverable
                        >
                            <div style={{display: 'flex', justifyContent: 'space-between'}}>
                                <div>
                                    <strong>{tool.name}</strong>
                                    <Tag style={{marginLeft: 8}}>{tool.category}</Tag>
                                </div>
                                {selectedTools.find(t => t.id === tool.id) && (
                                    <Tag color="green">已选择</Tag>
                                )}
                            </div>
                            <p style={{color: '#666', marginTop: 4, marginBottom: 0}}>
                                {tool.description}
                            </p>
                        </Card>
                    ))}
                </div>
            </Modal>
        </div>
    )
}

export default AgentListPage