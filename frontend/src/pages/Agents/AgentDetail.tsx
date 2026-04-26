import React, {useEffect, useRef, useState} from 'react'
import {
    Badge,
    Button,
    Card,
    Collapse,
    Descriptions,
    Divider,
    Form,
    Input,
    List,
    message,
    Modal,
    Popconfirm,
    Select,
    Space,
    Spin,
    Tabs,
    Tag,
    Tooltip,
    Typography
} from 'antd'
import {
    ApiOutlined,
    CheckCircleOutlined,
    DeleteOutlined,
    EditOutlined,
    FolderOutlined,
    HistoryOutlined,
    InboxOutlined,
    PlusOutlined,
    RocketOutlined,
    SendOutlined,
    ToolOutlined,
    UndoOutlined
} from '@ant-design/icons'
import {useNavigate, useParams} from 'react-router-dom'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import rehypeHighlight from 'rehype-highlight'
import 'highlight.js/styles/github.css'
import {
    bindTool,
    deleteAgent,
    executeAgentStream,
    getAgent,
    getAgentTools,
    getExecutionLogs,
    listTools,
    unbindTool,
    updateAgent,
    updateAgentStatus
} from '@/api/agent'
import {Agent, AgentExecutionLog, AgentStreamingEvent, Tool, ToolBinding, ToolBindingRequest} from '@/types/agent'
import {ToolCallDisplay} from '@/types/chat'
import {listModels} from '@/api/models'
import {ModelConfig} from '@/types/model'

const AgentDetailPage: React.FC = () => {
    const {id} = useParams<{ id: string }>()
    const navigate = useNavigate()
    const [agent, setAgent] = useState<Agent | null>(null)
    const [models, setModels] = useState<ModelConfig[]>([])
    const [toolBindings, setToolBindings] = useState<ToolBinding[]>([])
    const [builtinTools, setBuiltinTools] = useState<Tool[]>([])
    const [executionLogs, setExecutionLogs] = useState<AgentExecutionLog[]>([])
    const [loading, setLoading] = useState(false)
    const [editModalVisible, setEditModalVisible] = useState(false)
    const [toolModalVisible, setToolModalVisible] = useState(false)
    const [form] = Form.useForm()

    // 测试对话状态
    const [testMessage, setTestMessage] = useState('')
    const [testLoading, setTestLoading] = useState(false)

    // 流式执行状态
    const [streamingContent, setStreamingContent] = useState('')
    const [streamingToolCalls, setStreamingToolCalls] = useState<ToolCallDisplay[]>([])
    const [streamingDuration, setStreamingDuration] = useState(0)
    const streamingStartTimeRef = useRef(0)

    useEffect(() => {
        if (id) {
            fetchAgent()
            fetchToolBindings()
            fetchExecutionLogs()
        }
        fetchModels()
        fetchBuiltinTools()
    }, [id])

    const fetchAgent = async () => {
        if (!id) return
        setLoading(true)
        try {
            const res = await getAgent(id)
            if (res.success) {
                setAgent(res.data)
            }
        } finally {
            setLoading(false)
        }
    }

    const fetchToolBindings = async () => {
        if (!id) return
        try {
            const res = await getAgentTools(id)
            if (res.success) {
                setToolBindings(res.data || [])
            }
        } catch {
            // ignore
        }
    }

    const fetchExecutionLogs = async () => {
        if (!id) return
        try {
            const res = await getExecutionLogs(id, 20)
            if (res.success) {
                setExecutionLogs(res.data || [])
            }
        } catch {
            // ignore
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
            const res = await listTools()
            if (res.success) {
                setBuiltinTools(res.data || [])
            }
        } catch {
            // ignore
        }
    }

    const handleEdit = () => {
        if (agent) {
            form.setFieldsValue({
                name: agent.name,
                description: agent.description,
                modelId: agent.modelId,
                systemPrompt: agent.systemPrompt,
                config: agent.config
            })
            setEditModalVisible(true)
        }
    }

    const handleUpdate = async () => {
        if (!id) return
        try {
            const values = await form.validateFields()
            await updateAgent(id, values)
            message.success('更新成功')
            setEditModalVisible(false)
            fetchAgent()
        } catch {
            message.error('更新失败')
        }
    }

    const handleDelete = async () => {
        if (!id) return
        try {
            await deleteAgent(id)
            message.success('删除成功')
            navigate('/agents')
        } catch {
            message.error('删除失败')
        }
    }

    const handlePublish = async () => {
        if (!id) return
        try {
            await updateAgentStatus(id, {status: 'PUBLISHED'})
            message.success('发布成功')
            fetchAgent()
        } catch {
            message.error('发布失败')
        }
    }

    const handleArchive = async () => {
        if (!id) return
        try {
            await updateAgentStatus(id, {status: 'ARCHIVED'})
            message.success('已归档')
            fetchAgent()
        } catch {
            message.error('归档失败')
        }
    }

    const handleRestore = async () => {
        if (!id) return
        try {
            await updateAgentStatus(id, {status: 'DRAFT'})
            message.success('已恢复到草稿状态')
            fetchAgent()
        } catch {
            message.error('恢复失败')
        }
    }

    const handleBindTool = async (tool: Tool) => {
        if (!id) return
        try {
            const binding: ToolBindingRequest = {
                toolId: tool.id,
                enabled: true,
                config: {}
            }
            await bindTool(id, binding)
            message.success('绑定成功')
            setToolModalVisible(false)
            fetchToolBindings()
            fetchAgent()
        } catch {
            message.error('绑定失败')
        }
    }

    // 批量绑定 MCP 工具包
    const handleBindMcpPackage = async (tools: Tool[]) => {
        if (!id) return
        try {
            const bindings: ToolBindingRequest[] = tools.map(tool => ({
                toolId: tool.id,
                enabled: true,
                config: {}
            }))
            // 逐个绑定
            for (const binding of bindings) {
                await bindTool(id, binding)
            }
            message.success(`已绑定 ${tools.length} 个工具`)
            setToolModalVisible(false)
            fetchToolBindings()
            fetchAgent()
        } catch {
            message.error('绑定失败')
        }
    }

    // 分组工具：内置工具和 MCP 工具包
    const groupedTools = React.useMemo(() => {
        const builtin = builtinTools.filter(t => t.type === 'BUILTIN')
        const mcpTools = builtinTools.filter(t => t.type === 'MCP')

        // MCP 工具按 Server 分组
        const mcpPackages: Record<string, { serverName: string; tools: Tool[] }> = {}
        mcpTools.forEach(tool => {
            const serverName = (tool.config as Record<string, unknown>)?.mcpServerName as string || '未知 MCP Server'
            if (!mcpPackages[serverName]) {
                mcpPackages[serverName] = {serverName, tools: []}
            }
            mcpPackages[serverName].tools.push(tool)
        })

        return {builtin, mcpPackages}
    }, [builtinTools])

    const handleUnbindTool = async (toolId: string) => {
        if (!id) return
        try {
            await unbindTool(id, toolId)
            message.success('解绑成功')
            fetchToolBindings()
            fetchAgent()
        } catch {
            message.error('解绑失败')
        }
    }

    const handleTestExecute = async () => {
        if (!id || !testMessage.trim()) return

        // 重置流式状态
        setTestLoading(true)
        setStreamingContent('')
        setStreamingToolCalls([])
        setStreamingDuration(0)
        streamingStartTimeRef.current = Date.now()

        executeAgentStream(
            id,
            {message: testMessage},
            (event: AgentStreamingEvent) => {
                switch (event.type) {
                    case 'MODEL_OUTPUT':
                        if (event.content) {
                            // 累加模型输出内容，而不是替换
                            setStreamingContent(prev => prev + event.content)
                        }
                        break
                    case 'TOOL_START':
                        if (event.toolCall) {
                            setStreamingToolCalls(prev => [...prev, {
                                toolName: event.toolCall!.toolName,
                                args: event.toolCall!.args,
                                status: 'running',
                                durationMs: 0
                            }])
                        }
                        break
                    case 'TOOL_END':
                        if (event.toolCall) {
                            setStreamingToolCalls(prev => prev.map(tc =>
                                tc.toolName === event.toolCall!.toolName
                                    ? {
                                        ...tc,
                                        result: event.toolCall!.result,
                                        status: event.toolCall!.success ? 'completed' : 'error',
                                        durationMs: event.toolCall!.durationMs,
                                        errorMessage: event.toolCall!.errorMessage
                                    }
                                    : tc
                            ))
                        }
                        break
                    case 'ERROR':
                        message.error(event.content || '执行出错')
                        break
                    case 'DONE':
                        setStreamingDuration(Date.now() - streamingStartTimeRef.current)
                        break
                }
            },
            () => {
                setTestLoading(false)
                fetchExecutionLogs()
            },
            (error: Error) => {
                message.error(`执行失败: ${error.message}`)
                setTestLoading(false)
            }
        )
    }

    const statusMap = {
        DRAFT: {status: 'default' as const, text: '草稿'},
        PUBLISHED: {status: 'success' as const, text: '已发布'},
        ARCHIVED: {status: 'warning' as const, text: '已归档'},
    }

    if (loading) {
        return <Spin style={{marginTop: 100}}/>
    }

    if (!agent) {
        return <div>Agent 不存在</div>
    }

    const tabItems = [
        {
            key: 'info',
            label: '基本信息',
            icon: <EditOutlined/>,
            children: (
                <div>
                    <Descriptions bordered column={2}>
                        <Descriptions.Item label="名称">{agent.name}</Descriptions.Item>
                        <Descriptions.Item label="版本">{agent.version}</Descriptions.Item>
                        <Descriptions.Item label="描述">{agent.description || '-'}</Descriptions.Item>
                        <Descriptions.Item label="状态">
                            <Badge {...statusMap[agent.status]} />
                        </Descriptions.Item>
                        <Descriptions.Item label="模型">{agent.modelName}</Descriptions.Item>
                        <Descriptions.Item label="Temperature">{agent.config?.temperature || 0.7}</Descriptions.Item>
                        <Descriptions.Item label="Max Tokens">{agent.config?.maxTokens || 4096}</Descriptions.Item>
                    </Descriptions>

                    <Divider>系统提示词</Divider>
                    <Card size="small">
                        <ReactMarkdown remarkPlugins={[remarkGfm]} rehypePlugins={[rehypeHighlight]}>
                            {agent.systemPrompt || '未配置系统提示词'}
                        </ReactMarkdown>
                    </Card>

                    <Divider>操作</Divider>
                    <Space>
                        <Button icon={<EditOutlined/>} onClick={handleEdit}>
                            编辑配置
                        </Button>
                        {agent.status === 'DRAFT' && (
                            <Button type="primary" icon={<RocketOutlined/>} onClick={handlePublish}>
                                发布 Agent
                            </Button>
                        )}
                        {agent.status === 'PUBLISHED' && (
                            <Button icon={<InboxOutlined/>} onClick={handleArchive}>
                                归档
                            </Button>
                        )}
                        {agent.status === 'ARCHIVED' && (
                            <Button type="primary" icon={<UndoOutlined/>} onClick={handleRestore}>
                                恢复到草稿
                            </Button>
                        )}
                        <Popconfirm title="确定删除此 Agent?" onConfirm={handleDelete}>
                            <Button danger icon={<DeleteOutlined/>}>
                                删除
                            </Button>
                        </Popconfirm>
                    </Space>
                </div>
            )
        },
        {
            key: 'tools',
            label: '工具绑定',
            icon: <ToolOutlined/>,
            children: (
                <div>
                    <Button
                        type="primary"
                        icon={<PlusOutlined/>}
                        onClick={() => setToolModalVisible(true)}
                        style={{marginBottom: 16}}
                    >
                        添加工具
                    </Button>

                    <List
                        grid={{gutter: 16, column: 2}}
                        dataSource={toolBindings}
                        renderItem={(binding) => (
                            <List.Item>
                                <Card
                                    size="small"
                                    title={
                                        <Space>
                                            <strong>{binding.toolName}</strong>
                                            {binding.enabled ? <Tag color="green">启用</Tag> : <Tag>禁用</Tag>}
                                        </Space>
                                    }
                                    extra={
                                        <Popconfirm title="确定解绑?"
                                                    onConfirm={() => handleUnbindTool(binding.toolId)}>
                                            <Tooltip title="解绑">
                                                <Button type="text" size="small" danger icon={<DeleteOutlined/>}/>
                                            </Tooltip>
                                        </Popconfirm>
                                    }
                                >
                                    <p style={{color: '#666', marginBottom: 0}}>
                                        {binding.toolDescription || '无描述'}
                                    </p>
                                </Card>
                            </List.Item>
                        )}
                    />
                </div>
            )
        },
        {
            key: 'test',
            label: '测试对话',
            icon: <SendOutlined/>,
            children: (
                <div>
                    <Card size="small" style={{marginBottom: 16}}>
                        <div style={{marginBottom: 12, color: '#666'}}>
                            流式测试：实时显示 AI 响应和工具调用过程，点击展开查看详情
                        </div>
                        <Input.TextArea
                            placeholder="输入测试消息，例如：计算 2+3*4"
                            value={testMessage}
                            onChange={(e) => setTestMessage(e.target.value)}
                            rows={3}
                            onPressEnter={(e) => {
                                if (!e.shiftKey) {
                                    e.preventDefault()
                                    handleTestExecute()
                                }
                            }}
                        />
                        <Button
                            type="primary"
                            icon={<SendOutlined/>}
                            onClick={handleTestExecute}
                            loading={testLoading}
                            style={{marginTop: 8}}
                        >
                            发送
                        </Button>
                    </Card>

                    <Divider>多轮对话</Divider>
                    <Button
                        type="primary"
                        icon={<PlusOutlined/>}
                        onClick={() => navigate(`/chat?agentId=${id}`)}
                    >
                        创建完整对话
                    </Button>
                    <div style={{marginTop: 8, color: '#666', fontSize: 12}}>
                        创建完整对话后，可进行多轮对话、流式输出，实时查看 Agent 工具调用
                    </div>

                    {(streamingContent || streamingToolCalls.length > 0) && (
                        <Card size="small" title="AI 响应" style={{marginTop: 16}}>
                            {/* 工具调用实时显示 */}
                            {streamingToolCalls.length > 0 && (
                                <div style={{marginBottom: 12}}>
                                    <Divider orientation="left" style={{margin: '8px 0'}}>
                                        <Space>
                                            <ToolOutlined/>
                                            工具调用
                                        </Space>
                                    </Divider>
                                    <Collapse ghost>
                                        {streamingToolCalls.map((call, idx) => (
                                            <Collapse.Panel
                                                key={idx}
                                                header={
                                                    <Space>
                                                        <Tag
                                                            color={call.status === 'running' ? 'blue' : call.status === 'completed' ? 'green' : 'red'}>
                                                            {call.status === 'running' ? '执行中' : call.status === 'completed' ? '完成' : '失败'}
                                                        </Tag>
                                                        <strong>{call.toolName}</strong>
                                                        {call.durationMs &&
                                                            <span style={{color: '#666'}}>{call.durationMs}ms</span>}
                                                    </Space>
                                                }
                                            >
                                                <Descriptions size="small" column={1}>
                                                    <Descriptions.Item label="参数">
                                                        <Tooltip title="点击查看完整参数">
                              <pre style={{margin: 0, fontSize: 12, maxWidth: 400, overflow: 'auto'}}>
                                {JSON.stringify(call.args, null, 2)}
                              </pre>
                                                        </Tooltip>
                                                    </Descriptions.Item>
                                                    {call.result !== undefined && call.result !== null && (
                                                        <Descriptions.Item label="结果">
                                                            <Tooltip title="工具执行返回结果">
                                <pre style={{margin: 0, fontSize: 12, maxWidth: 400, overflow: 'auto', color: 'green'}}>
                                  {JSON.stringify(call.result, null, 2)}
                                </pre>
                                                            </Tooltip>
                                                        </Descriptions.Item>
                                                    )}
                                                    {call.errorMessage && (
                                                        <Descriptions.Item label="错误">
                                                            <Tag color="red">{call.errorMessage}</Tag>
                                                        </Descriptions.Item>
                                                    )}
                                                </Descriptions>
                                            </Collapse.Panel>
                                        ))}
                                    </Collapse>
                                </div>
                            )}

                            {/* 流式输出内容 */}
                            <Typography.Paragraph>
                                <ReactMarkdown remarkPlugins={[remarkGfm]} rehypePlugins={[rehypeHighlight]}>
                                    {streamingContent || '等待响应...'}
                                </ReactMarkdown>
                            </Typography.Paragraph>

                            {/* 执行统计 */}
                            {streamingDuration > 0 && (
                                <div style={{marginTop: 8, color: '#666'}}>
                                    执行耗时: {streamingDuration}ms | 工具调用: {streamingToolCalls.length} 次
                                </div>
                            )}
                        </Card>
                    )}
                </div>
            )
        },
        {
            key: 'logs',
            label: '执行日志',
            icon: <HistoryOutlined/>,
            children: (
                <List
                    dataSource={executionLogs}
                    renderItem={(log) => (
                        <List.Item>
                            <Card size="small" style={{width: '100%'}}>
                                <Descriptions size="small" column={4}>
                                    <Descriptions.Item label="输入">
                                        <Tooltip title={log.inputSummary}>
                                            <span style={{
                                                maxWidth: 150,
                                                overflow: 'hidden',
                                                textOverflow: 'ellipsis',
                                                display: 'inline-block',
                                                whiteSpace: 'nowrap'
                                            }}>
                                                {log.inputSummary}
                                            </span>
                                        </Tooltip>
                                    </Descriptions.Item>
                                    <Descriptions.Item label="输出">
                                        <Tooltip title={
                                            <div style={{maxWidth: 400, whiteSpace: 'pre-wrap'}}>
                                                {log.outputSummary}
                                            </div>
                                        }>
                                            <span style={{
                                                maxWidth: 150,
                                                overflow: 'hidden',
                                                textOverflow: 'ellipsis',
                                                display: 'inline-block',
                                                whiteSpace: 'nowrap'
                                            }}>
                                                {log.outputSummary}
                                            </span>
                                        </Tooltip>
                                    </Descriptions.Item>
                                    <Descriptions.Item label="工具调用">
                                        {log.toolCalls && log.toolCalls.length > 0 ? (
                                            <Tooltip
                                                styles={{
                                                    root: {maxWidth: 600, maxHeight: 400},
                                                    body: {maxHeight: 380, overflow: 'auto', paddingRight: 8}
                                                }}
                                                title={
                                                    <div>
                                                        {log.toolCalls.map((call, idx) => (
                                                            <div key={idx} style={{
                                                                marginBottom: idx < log.toolCalls!.length - 1 ? 12 : 0,
                                                                padding: '8px 0',
                                                                borderBottom: idx < log.toolCalls!.length - 1 ? '1px dashed #555' : 'none'
                                                            }}>
                                                                <div style={{fontWeight: 'bold', marginBottom: 4}}>
                                                                    <Tag color={call.success ? 'green' : 'red'}
                                                                         style={{marginRight: 4}}>
                                                                        {call.success ? '成功' : '失败'}
                                                                    </Tag>
                                                                    {call.toolName}
                                                                    <span style={{
                                                                        marginLeft: 8,
                                                                        color: '#aaa'
                                                                    }}>{call.durationMs}ms</span>
                                                                </div>
                                                                <div style={{fontSize: 12}}>
                                                                    <div
                                                                        style={{color: '#aaa', marginBottom: 2}}>参数:
                                                                    </div>
                                                                    <pre style={{
                                                                        margin: 0,
                                                                        padding: '4px 8px',
                                                                        background: '#333',
                                                                        borderRadius: 4,
                                                                        fontSize: 11,
                                                                        maxHeight: 100,
                                                                        overflow: 'auto',
                                                                        whiteSpace: 'pre-wrap',
                                                                        wordBreak: 'break-all'
                                                                    }}>
                                                                        {JSON.stringify(call.args, null, 2)}
                                                                    </pre>
                                                                    {call.result !== undefined && call.result !== null && (
                                                                        <div style={{marginTop: 4}}>
                                                                            <div style={{
                                                                                color: '#aaa',
                                                                                marginBottom: 2
                                                                            }}>结果:
                                                                            </div>
                                                                            <pre style={{
                                                                                margin: 0,
                                                                                padding: '4px 8px',
                                                                                background: '#1a472a',
                                                                                borderRadius: 4,
                                                                                fontSize: 11,
                                                                                maxHeight: 150,
                                                                                overflow: 'auto',
                                                                                whiteSpace: 'pre-wrap',
                                                                                wordBreak: 'break-all',
                                                                                color: '#7fc97f'
                                                                            }}>
                                                                                {typeof call.result === 'string' ? call.result : JSON.stringify(call.result, null, 2)}
                                                                            </pre>
                                                                        </div>
                                                                    )}
                                                                    {call.errorMessage && (
                                                                        <div style={{marginTop: 4, color: '#ff6b6b'}}>
                                                                            错误: {call.errorMessage}
                                                                        </div>
                                                                    )}
                                                                </div>
                                                            </div>
                                                        ))}
                                                    </div>
                                                }
                                            >
                                                <Tag color="blue" style={{cursor: 'pointer'}}>
                                                    <ToolOutlined style={{marginRight: 4}}/>
                                                    {log.toolCallCount} 次
                                                </Tag>
                                            </Tooltip>
                                        ) : (
                                            <span>{log.toolCallCount} 次</span>
                                        )}
                                    </Descriptions.Item>
                                    <Descriptions.Item label="耗时">{log.durationMs}ms</Descriptions.Item>
                                </Descriptions>
                                <div style={{marginTop: 8, color: '#666'}}>
                                    {log.createdAt} | {log.success ? <Tag color="green">成功</Tag> :
                                    <Tag color="red">失败</Tag>}
                                </div>
                            </Card>
                        </List.Item>
                    )}
                />
            )
        }
    ]

    return (
        <div>
            <div style={{marginBottom: 16}}>
                <Button onClick={() => navigate('/agents')}>
                    返回列表
                </Button>
            </div>

            <Card title={`Agent: ${agent.name}`}>
                <Tabs items={tabItems}/>
            </Card>

            {/* 编辑 Agent Modal */}
            <Modal
                title="编辑 Agent"
                open={editModalVisible}
                onOk={handleUpdate}
                onCancel={() => setEditModalVisible(false)}
                width={600}
            >
                <Form form={form} layout="vertical">
                    <Form.Item name="name" label="名称" rules={[{required: true}]}>
                        <Input/>
                    </Form.Item>
                    <Form.Item name="description" label="描述">
                        <Input.TextArea rows={2}/>
                    </Form.Item>
                    <Form.Item name="modelId" label="模型" rules={[{required: true}]}>
                        <Select>
                            {models.map(m => (
                                <Select.Option key={m.id} value={m.id}>
                                    {m.name} ({m.modelName})
                                </Select.Option>
                            ))}
                        </Select>
                    </Form.Item>
                    <Form.Item name="systemPrompt" label="系统提示词">
                        <Input.TextArea rows={4}/>
                    </Form.Item>
                    <Form.Item name={['config', 'temperature']} label="Temperature">
                        <Input type="number" min={0} max={2} step={0.1}/>
                    </Form.Item>
                    <Form.Item name={['config', 'maxTokens']} label="Max Tokens">
                        <Input type="number" min={1} max={100000}/>
                    </Form.Item>
                </Form>
            </Modal>

            {/* 工具选择 Modal */}
            <Modal
                title="添加工具"
                open={toolModalVisible}
                onCancel={() => setToolModalVisible(false)}
                footer={null}
                width={700}
            >
                <Tabs defaultActiveKey="builtin">
                    <Tabs.TabPane
                        tab={<span><ToolOutlined/> 内置工具</span>}
                        key="builtin"
                    >
                        {groupedTools.builtin
                            .filter(tool => !toolBindings.find(b => b.toolId === tool.id))
                            .map(tool => (
                                <Card
                                    key={tool.id}
                                    size="small"
                                    style={{marginBottom: 8, cursor: 'pointer'}}
                                    onClick={() => handleBindTool(tool)}
                                    hoverable
                                >
                                    <div>
                                        <strong>{tool.name}</strong>
                                        <Tag style={{marginLeft: 8}} color="green">内置</Tag>
                                    </div>
                                    <p style={{color: '#666', marginTop: 4, marginBottom: 0}}>
                                        {tool.description}
                                    </p>
                                </Card>
                            ))}
                        {groupedTools.builtin.filter(tool => !toolBindings.find(b => b.toolId === tool.id)).length === 0 && (
                            <p style={{color: '#666', textAlign: 'center', marginTop: 20}}>所有内置工具都已绑定</p>
                        )}
                    </Tabs.TabPane>

                    <Tabs.TabPane
                        tab={<span><ApiOutlined/> MCP 工具包</span>}
                        key="mcp"
                    >
                        {Object.entries(groupedTools.mcpPackages).map(([serverName, pkg]) => {
                            const availableTools = pkg.tools.filter(t => !toolBindings.find(b => b.toolId === t.id))
                            const boundCount = pkg.tools.length - availableTools.length
                            return (
                                <Card
                                    key={serverName}
                                    size="small"
                                    style={{marginBottom: 12}}
                                    title={
                                        <Space>
                                            <FolderOutlined/>
                                            <strong>{serverName}</strong>
                                            <Tag color="blue">{availableTools.length} 个可用工具</Tag>
                                            {boundCount > 0 && <Tag color="orange">{boundCount} 个已绑定</Tag>}
                                        </Space>
                                    }
                                >
                                    <div style={{marginBottom: 8}}>
                                        <Button
                                            type="primary"
                                            size="small"
                                            icon={<CheckCircleOutlined/>}
                                            disabled={availableTools.length === 0}
                                            onClick={() => handleBindMcpPackage(availableTools)}
                                        >
                                            绑定全部 ({availableTools.length})
                                        </Button>
                                    </div>
                                    <Collapse ghost>
                                        <Collapse.Panel header="展开查看工具详情" key="tools">
                                            <List
                                                size="small"
                                                dataSource={availableTools}
                                                renderItem={(tool) => (
                                                    <List.Item
                                                        style={{cursor: 'pointer'}}
                                                        onClick={() => handleBindTool(tool)}
                                                    >
                                                        <List.Item.Meta
                                                            title={<span>{tool.name} <Tag color="purple"
                                                                                          style={{marginLeft: 4}}>MCP</Tag></span>}
                                                            description={tool.description}
                                                        />
                                                        <Button type="link" size="small">绑定</Button>
                                                    </List.Item>
                                                )}
                                            />
                                        </Collapse.Panel>
                                    </Collapse>
                                </Card>
                            )
                        })}
                        {Object.keys(groupedTools.mcpPackages).length === 0 && (
                            <div style={{textAlign: 'center', marginTop: 20}}>
                                <p style={{color: '#666'}}>暂无 MCP 工具包</p>
                                <Button type="link" onClick={() => navigate('/mcp-servers')}>
                                    前往 MCP Server 配置页面添加
                                </Button>
                            </div>
                        )}
                    </Tabs.TabPane>
                </Tabs>
            </Modal>
        </div>
    )
}

export default AgentDetailPage