import React, {useEffect, useRef, useState} from 'react'
import {
    Button,
    Card,
    Collapse,
    Descriptions,
    Divider,
    Empty,
    Form,
    Input,
    Layout,
    List,
    message,
    Modal,
    Radio,
    Select,
    Slider,
    Space,
    Spin,
    Switch,
    Tag,
    Tooltip
} from 'antd'
import {
    DeleteOutlined,
    EditOutlined,
    FileTextOutlined,
    InfoCircleOutlined,
    LoadingOutlined,
    MessageOutlined,
    RobotOutlined,
    SendOutlined,
    ToolOutlined
} from '@ant-design/icons'
import {useSearchParams} from 'react-router-dom'
import {
    createSession,
    deleteSession,
    getSessionMessages,
    listSessions,
    sendMessageStream,
    sendMessageStreamAgent,
    updateSession
} from '@/api/chat'
import {listModels} from '@/api/models'
import {listPrompts} from '@/api/prompts'
import {listAllKnowledgeBases} from '@/api/knowledgeBase'
import {listAgents} from '@/api/agent'
import {ChatMessage, ChatSession, CreateSessionRequest, ToolCallDisplay, VectorSearchResult} from '@/types/chat'
import {ModelConfig} from '@/types/model'
import {PromptTemplate} from '@/types/prompt'
import {KnowledgeBase} from '@/types/knowledgeBase'
import {Agent} from '@/types/agent'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import rehypeHighlight from 'rehype-highlight'
import 'highlight.js/styles/github.css'

const {Sider, Content} = Layout

/**
 * 对话调试页面
 * <p>
 * 支持两种模式：
 * 1. 普通对话：选择模型和提示词模板进行对话
 * 2. Agent对话：关联Agent，使用Agent的模型、系统提示词和工具
 * </p>
 */
const ChatPage: React.FC = () => {
    const [searchParams, setSearchParams] = useSearchParams()
    const [sessions, setSessions] = useState<ChatSession[]>([])
    const [models, setModels] = useState<ModelConfig[]>([])
    const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBase[]>([])
    const [prompts, setPrompts] = useState<PromptTemplate[]>([])
    const [agents, setAgents] = useState<Agent[]>([])
    const [currentSession, setCurrentSession] = useState<ChatSession | null>(null)
    const [currentAgent, setCurrentAgent] = useState<Agent | null>(null)
    const [messages, setMessages] = useState<ChatMessage[]>([])
    const [inputValue, setInputValue] = useState('')
    const [selectedModelId, setSelectedModelId] = useState<string | null>(null)
    const [loading, setLoading] = useState(false)
    const [sending, setSending] = useState(false)
    const [sessionModalVisible, setSessionModalVisible] = useState(false)
    const [editSessionModalVisible, setEditSessionModalVisible] = useState(false)
    const [editingSession, setEditingSession] = useState<ChatSession | null>(null)
    const [sessionForm] = Form.useForm()
    const [editSessionForm] = Form.useForm()
    const messagesEndRef = useRef<HTMLDivElement>(null)

    // 新建对话模式：'normal' 或 'agent'
    const [sessionMode, setSessionMode] = useState<'normal' | 'agent'>('normal')

    // RAG 配置状态
    const [enableRag, setEnableRag] = useState(false)
    const [ragTopK, setRagTopK] = useState(5)
    const [ragThreshold, setRagThreshold] = useState(0.3)
    const [ragStrategy, setRagStrategy] = useState('VECTOR')

    // 根据检索策略返回阈值范围和默认值
    const getThresholdConfig = (strategy: string) => {
        switch (strategy) {
            case 'BM25':
                return {
                    min: 0,
                    max: 0.5,
                    step: 0.01,
                    default: 0.05,
                    marks: {0: '0', 0.1: '0.1', 0.3: '0.3', 0.5: '0.5'},
                    tip: 'BM25 分数范围: 0.01-0.5，建议阈值: 0.01-0.1'
                }
            case 'HYBRID':
                return {
                    min: 0,
                    max: 0.05,
                    step: 0.005,
                    default: 0.01,
                    marks: {0: '0', 0.02: '0.02', 0.04: '0.04', 0.05: '0.05'},
                    tip: '混合检索使用 RRF 融合分数，范围: 0-0.05，建议阈值: 0.005-0.02'
                }
            case 'VECTOR':
            default:
                return {
                    min: 0,
                    max: 1,
                    step: 0.1,
                    default: 0.3,
                    marks: {0: '0', 0.3: '0.3', 0.5: '0.5', 0.7: '0.7', 1: '1'},
                    tip: '向量相似度范围: 0-1，建议阈值: 0.3-0.7'
                }
        }
    }

    const handleStrategyChange = (strategy: string) => {
        setRagStrategy(strategy)
        const config = getThresholdConfig(strategy)
        setRagThreshold(config.default)
    }

    const [isStreaming, setIsStreaming] = useState(false)

    // 分组会话：普通对话和Agent对话
    const normalSessions = sessions.filter(s => !s.agentId)
    const agentSessions = sessions.filter(s => s.agentId)

    useEffect(() => {
        fetchSessions()
        fetchModels()
        fetchPrompts()
        fetchKnowledgeBases()
        fetchAgents()
    }, [])

    // 处理 URL 参数中的 agentId
    useEffect(() => {
        const agentIdParam = searchParams.get('agentId')
        if (agentIdParam && agents.length > 0) {
            const agent = agents.find(a => a.id === agentIdParam)
            if (agent) {
                handleCreateSessionWithAgent(agent)
                setSearchParams({})
            }
        }
    }, [agents, searchParams])

    useEffect(() => {
        if (currentSession) {
            fetchMessages(currentSession.id)
            setSelectedModelId(currentSession.modelId || getDefaultModelId())
            // 加载关联的Agent信息
            if (currentSession.agentId) {
                const agent = agents.find(a => a.id === currentSession.agentId)
                setCurrentAgent(agent || null)
            } else {
                setCurrentAgent(null)
            }
        }
    }, [currentSession, agents])

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({behavior: 'smooth'})
    }, [messages])

    const fetchSessions = async () => {
        setLoading(true)
        try {
            const res = await listSessions()
            if (res.success) {
                setSessions(res.data.content || [])
            }
        } finally {
            setLoading(false)
        }
    }

    const fetchModels = async () => {
        try {
            const res = await listModels()
            if (res.success) {
                const chatModels = res.data.filter(m => m.modelType === 'CHAT')
                setModels(chatModels)
            }
        } catch {
            // ignore
        }
    }

    const fetchPrompts = async () => {
        try {
            const res = await listPrompts()
            if (res.success) {
                setPrompts(res.data.content || [])
            }
        } catch {
            // ignore
        }
    }

    const fetchKnowledgeBases = async () => {
        try {
            const res = await listAllKnowledgeBases()
            if (res.success) {
                setKnowledgeBases(res.data || [])
            }
        } catch {
            // ignore
        }
    }

    const fetchAgents = async () => {
        try {
            const res = await listAgents()
            if (res.success) {
                setAgents(res.data || [])
            }
        } catch {
            // ignore
        }
    }

    const getDefaultModelId = () => {
        const defaultModel = models.find(m => m.isDefault)
        return defaultModel?.id || null
    }

    const fetchMessages = async (sessionId: string) => {
        try {
            const res = await getSessionMessages(sessionId)
            if (res.success) {
                setMessages(res.data.messages || [])
            }
        } catch {
            setMessages([])
        }
    }

    const handleCreateSession = (mode?: 'normal' | 'agent') => {
        const actualMode = mode || sessionMode
        setSessionMode(actualMode)
        sessionForm.resetFields()
        if (actualMode === 'normal') {
            sessionForm.setFieldsValue({
                title: '新对话',
                modelId: getDefaultModelId()
            })
        } else {
            sessionForm.setFieldsValue({
                title: ''
            })
        }
        setSessionModalVisible(true)
    }

    const handleCreateSessionSubmit = async () => {
        try {
            const values = await sessionForm.validateFields()

            // Agent模式必须选择Agent
            if (sessionMode === 'agent' && !values.agentId) {
                message.error('Agent模式下必须选择一个Agent')
                return
            }

            const request: CreateSessionRequest = {
                title: values.title,
                modelId: sessionMode === 'agent' ? undefined : values.modelId,
                promptId: sessionMode === 'agent' ? undefined : values.promptId,
                systemMessage: sessionMode === 'agent' ? undefined : values.systemMessage,
                agentId: sessionMode === 'agent' ? values.agentId : undefined,
                enableRag: enableRag,
                knowledgeBaseId: enableRag ? values.knowledgeBaseId : undefined,
                ragTopK: enableRag ? ragTopK : undefined,
                ragThreshold: enableRag ? ragThreshold : undefined,
                ragStrategy: enableRag ? ragStrategy : undefined,
                ragEmbeddingModelId: enableRag ? values.ragEmbeddingModelId : undefined
            }
            const res = await createSession(request)
            if (res.success) {
                setCurrentSession(res.data)
                setSelectedModelId(res.data.modelId || getDefaultModelId())
                setMessages([])
                fetchSessions()
                setSessionModalVisible(false)
                setEnableRag(false)
                message.success('创建成功')
            }
        } catch {
            message.error('创建失败')
        }
    }

    const handleCreateSessionWithAgent = async (agent: Agent) => {
        try {
            const request: CreateSessionRequest = {
                title: `对话: ${agent.name}`,
                modelId: agent.modelId,
                systemMessage: agent.systemPrompt,
                agentId: agent.id
            }
            const res = await createSession(request)
            if (res.success) {
                setCurrentSession(res.data)
                setSelectedModelId(agent.modelId)
                setMessages([])
                fetchSessions()
                message.success(`已创建与 Agent "${agent.name}" 关联的对话`)
            }
        } catch {
            message.error('创建会话失败')
        }
    }

    const handleEditSession = (session: ChatSession) => {
        setEditingSession(session)
        setSessionMode(session.agentId ? 'agent' : 'normal')
        editSessionForm.setFieldsValue({
            title: session.title,
            modelId: session.modelId,
            promptId: session.promptId,
            systemMessage: session.systemMessage,
            agentId: session.agentId
        })
        setEnableRag(session.enableRag || false)
        setRagTopK(session.ragTopK || 5)
        setRagStrategy(session.ragStrategy || 'VECTOR')
        const strategy = session.ragStrategy || 'VECTOR'
        const config = getThresholdConfig(strategy)
        let threshold = session.ragThreshold || config.default
        if (threshold > config.max || threshold < config.min) {
            threshold = config.default
        }
        setRagThreshold(threshold)
        editSessionForm.setFieldsValue({
            knowledgeBaseId: session.knowledgeBaseId,
            ragEmbeddingModelId: session.ragEmbeddingModelId
        })
        setEditSessionModalVisible(true)
    }

    const handleEditSessionSubmit = async () => {
        if (!editingSession) return
        try {
            const values = await editSessionForm.validateFields()

            // Agent模式必须选择Agent
            if (sessionMode === 'agent' && !values.agentId) {
                message.error('Agent模式下必须选择一个Agent')
                return
            }

            const request = {
                title: values.title,
                modelId: sessionMode === 'agent' ? undefined : values.modelId,
                promptId: sessionMode === 'agent' ? undefined : values.promptId,
                systemMessage: sessionMode === 'agent' ? undefined : values.systemMessage,
                agentId: sessionMode === 'agent' ? values.agentId : (values.agentId || editingSession.agentId),
                enableRag: enableRag,
                knowledgeBaseId: enableRag ? values.knowledgeBaseId : undefined,
                ragTopK: enableRag ? ragTopK : undefined,
                ragThreshold: enableRag ? ragThreshold : undefined,
                ragStrategy: enableRag ? ragStrategy : undefined,
                ragEmbeddingModelId: enableRag ? values.ragEmbeddingModelId : undefined
            }
            const res = await updateSession(editingSession.id, request)
            if (res.success) {
                if (currentSession?.id === editingSession.id) {
                    setCurrentSession(res.data)
                }
                fetchSessions()
                setEditSessionModalVisible(false)
                setEnableRag(false)
                message.success('更新成功')
            }
        } catch {
            message.error('更新失败')
        }
    }

    const handleDeleteSession = async (sessionId: string) => {
        try {
            await deleteSession(sessionId)
            message.success('删除成功')
            if (currentSession?.id === sessionId) {
                setCurrentSession(null)
                setMessages([])
            }
            fetchSessions()
        } catch {
            message.error('删除失败')
        }
    }

    const handleSend = async () => {
        if (!inputValue.trim() || !currentSession) return

        const userContent = inputValue.trim()
        setInputValue('')
        setSending(true)
        setIsStreaming(true)

        const tempUserMessage: ChatMessage = {
            id: 'temp-user',
            sessionId: currentSession.id,
            role: 'USER',
            content: userContent,
            isError: false,
            createdAt: new Date().toISOString(),
        }
        setMessages(prev => [...prev, tempUserMessage])

        const tempAiMessage: ChatMessage = {
            id: 'temp-ai',
            sessionId: currentSession.id,
            role: 'ASSISTANT',
            content: '',
            isError: false,
            toolCalls: [],
            createdAt: new Date().toISOString(),
        }
        setMessages(prev => [...prev, tempAiMessage])

        if (currentSession.agentId && currentAgent) {
            // Agent 会话：使用结构化事件流
            const toolCallRecords: ToolCallDisplay[] = []
            let accumulatedContent = ''

            await sendMessageStreamAgent(
                {
                    sessionId: currentSession.id,
                    content: userContent,
                    modelId: selectedModelId || undefined,
                },
                (event) => {
                    setMessages(prev => {
                        const newMessages = [...prev]
                        const aiMsgIndex = newMessages.findIndex(m => m.id === 'temp-ai')
                        if (aiMsgIndex === -1) return prev

                        const aiMsg = newMessages[aiMsgIndex]
                        const updatedMsg = {...aiMsg}

                        switch (event.type) {
                            case 'MODEL_OUTPUT':
                                if (event.content) {
                                    accumulatedContent += event.content
                                    updatedMsg.content = accumulatedContent
                                }
                                break
                            case 'TOOL_START':
                                if (event.toolCall) {
                                    updatedMsg.toolCalls = [
                                        ...(updatedMsg.toolCalls || []),
                                        {
                                            toolName: event.toolCall.toolName,
                                            args: event.toolCall.args,
                                            status: 'running',
                                        }
                                    ]
                                    toolCallRecords.push({
                                        toolName: event.toolCall.toolName,
                                        args: event.toolCall.args,
                                        status: 'running',
                                    })
                                }
                                break
                            case 'TOOL_END':
                                if (event.toolCall) {
                                    updatedMsg.toolCalls = updatedMsg.toolCalls?.map(tc =>
                                        tc.toolName === event.toolCall?.toolName
                                            ? {
                                                ...tc,
                                                result: event.toolCall?.result,
                                                status: event.toolCall?.success ? 'completed' : 'error',
                                                durationMs: event.toolCall?.durationMs,
                                                errorMessage: event.toolCall?.errorMessage,
                                            }
                                            : tc
                                    )
                                    // 更新记录
                                    const recordIndex = toolCallRecords.findIndex(
                                        r => r.toolName === event.toolCall?.toolName && r.status === 'running'
                                    )
                                    if (recordIndex !== -1) {
                                        toolCallRecords[recordIndex] = {
                                            ...toolCallRecords[recordIndex],
                                            result: event.toolCall?.result,
                                            status: event.toolCall?.success ? 'completed' : 'error',
                                            durationMs: event.toolCall?.durationMs,
                                            errorMessage: event.toolCall?.errorMessage,
                                        }
                                    }
                                }
                                break
                            case 'ERROR':
                                updatedMsg.isError = true
                                updatedMsg.errorMessage = event.content
                                break
                            case 'DONE':
                                break
                        }

                        newMessages[aiMsgIndex] = updatedMsg
                        return newMessages
                    })
                },
                () => {
                    setIsStreaming(false)
                    setSending(false)
                    fetchMessages(currentSession.id)
                },
                (error) => {
                    setIsStreaming(false)
                    setSending(false)
                    message.error(`发送失败: ${error.message}`)
                    setMessages(prev => prev.filter(m => m.id !== 'temp-user' && m.id !== 'temp-ai'))
                }
            )
        } else {
            // 普通会话：使用纯文本流
            await sendMessageStream(
                {
                    sessionId: currentSession.id,
                    content: userContent,
                    modelId: selectedModelId || undefined,
                },
                (text) => {
                    setMessages(prev => {
                        const newMessages = [...prev]
                        const aiMsgIndex = newMessages.findIndex(m => m.id === 'temp-ai')
                        if (aiMsgIndex !== -1) {
                            newMessages[aiMsgIndex] = {
                                ...newMessages[aiMsgIndex],
                                content: text,
                            }
                        }
                        return newMessages
                    })
                },
                () => {
                    setIsStreaming(false)
                    setSending(false)
                    fetchMessages(currentSession.id)
                },
                (error) => {
                    setIsStreaming(false)
                    setSending(false)
                    message.error(`发送失败: ${error.message}`)
                    setMessages(prev => prev.filter(m => m.id !== 'temp-user' && m.id !== 'temp-ai'))
                }
            )
        }
    }

    const renderSource = (source: VectorSearchResult, index: number, strategy?: string) => {
        let scoreLabel = '相似度'
        if (strategy === 'BM25') {
            scoreLabel = 'BM25分数'
        } else if (strategy === 'HYBRID') {
            scoreLabel = 'RRF分数'
        }

        return (
            <Collapse
                key={source.chunkId}
                size="small"
                items={[{
                    key: '1',
                    label: (
                        <Space>
                            <Tag color="blue">{index + 1}</Tag>
                            <FileTextOutlined/>
                            <span>{source.documentName}</span>
                            <Tag>{scoreLabel}: {source.score.toFixed(3)}</Tag>
                        </Space>
                    ),
                    children: (
                        <div style={{whiteSpace: 'pre-wrap', fontSize: 13}}>
                            {source.content}
                        </div>
                    )
                }]}
            />
        )
    }

    const renderMessage = (msg: ChatMessage) => {
        const isUser = msg.role === 'USER'
        const isError = msg.isError
        const content = isError && msg.errorMessage ? `**错误:** ${msg.errorMessage}` : msg.content
        const isTempAi = msg.id === 'temp-ai'
        const hasSources = msg.sources && msg.sources.length > 0

        // 统一处理工具调用数据：支持 toolCalls（流式状态）和 toolCallRecords（后端返回）
        const toolCallData = msg.toolCalls || msg.toolCallRecords || []
        const hasToolCalls = toolCallData.length > 0

        // 获取工具调用的状态（兼容两种数据格式）
        const getToolStatus = (tc: any): 'running' | 'completed' | 'error' | 'pending' => {
            if (tc.status) return tc.status  // ToolCallDisplay 格式
            if (tc.success === true) return 'completed'
            if (tc.success === false) return 'error'
            return 'pending'
        }

        return (
            <div
                key={msg.id}
                style={{
                    display: 'flex',
                    justifyContent: isUser ? 'flex-end' : 'flex-start',
                    marginBottom: 16,
                }}
            >
                <Card
                    size="small"
                    style={{
                        maxWidth: '70%',
                        backgroundColor: isError ? '#fff2f0' : (isUser ? '#e6f7ff' : '#f5f5f5'),
                    }}
                    styles={{body: {padding: '12px 16px'}}}
                >
                    <div style={{
                        fontSize: 12,
                        color: '#999',
                        marginBottom: 4,
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center'
                    }}>
                        <span>
                            {isUser ? '用户' : (currentAgent ? `Agent: ${currentAgent.name}` : 'AI')}
                            {msg.modelName && ` (${msg.modelName})`}
                            {msg.latencyMs && ` - ${msg.latencyMs}ms`}
                            {isError && <span style={{color: '#cf1322'}}> [错误]</span>}
                            {hasSources && <Tag color="green" style={{marginLeft: 8}}>RAG</Tag>}
                        </span>
                        <span style={{marginLeft: 8}}>
                            {isTempAi && isStreaming &&
                                <LoadingOutlined style={{color: '#1890ff', marginRight: 4}} spin/>}
                            {msg.createdAt && new Date(msg.createdAt).toLocaleString('zh-CN', {
                                year: 'numeric',
                                month: '2-digit',
                                day: '2-digit',
                                hour: '2-digit',
                                minute: '2-digit'
                            })}
                        </span>
                    </div>

                    {/* 工具调用显示 - 对齐 Agent 管理页面样式 */}
                    {hasToolCalls && (
                        <div style={{marginBottom: 12}}>
                            <Divider orientation="left" style={{margin: '8px 0'}}>
                                <Space>
                                    <ToolOutlined/>
                                    工具调用 ({toolCallData.length})
                                </Space>
                            </Divider>
                            <Collapse ghost>
                                {toolCallData.map((tc, idx) => {
                                    const status = getToolStatus(tc)
                                    return (
                                        <Collapse.Panel
                                            key={idx}
                                            header={
                                                <Space>
                                                    <Tag
                                                        color={status === 'running' ? 'blue' : status === 'completed' ? 'green' : 'red'}>
                                                        {status === 'running' ? '执行中' : status === 'completed' ? '完成' : '失败'}
                                                    </Tag>
                                                    <strong>{tc.toolName}</strong>
                                                    {tc.durationMs &&
                                                        <span style={{color: '#666'}}>{tc.durationMs}ms</span>}
                                                </Space>
                                            }
                                        >
                                            <Descriptions size="small" column={1}>
                                                {tc.args && Object.keys(tc.args).length > 0 && (
                                                    <Descriptions.Item label="参数">
                                                        <pre style={{
                                                            margin: 0,
                                                            fontSize: 12,
                                                            maxWidth: 400,
                                                            overflow: 'auto'
                                                        }}>
                                                            {JSON.stringify(tc.args, null, 2)}
                                                        </pre>
                                                    </Descriptions.Item>
                                                )}
                                                {tc.result !== undefined && tc.result !== null && (
                                                    <Descriptions.Item label="结果">
                                                        <pre style={{
                                                            margin: 0,
                                                            fontSize: 12,
                                                            maxWidth: 400,
                                                            overflow: 'auto',
                                                            maxHeight: 200
                                                        }}>
                                                            {typeof tc.result === 'object'
                                                                ? JSON.stringify(tc.result, null, 2)
                                                                : String(tc.result)}
                                                        </pre>
                                                    </Descriptions.Item>
                                                )}
                                                {tc.errorMessage && (
                                                    <Descriptions.Item label="错误">
                                                        <span style={{color: '#cf1322'}}>{tc.errorMessage}</span>
                                                    </Descriptions.Item>
                                                )}
                                            </Descriptions>
                                        </Collapse.Panel>
                                    )
                                })}
                            </Collapse>
                        </div>
                    )}

                    <div className="markdown-content">
                        {isUser ? (
                            <div style={{whiteSpace: 'pre-wrap'}}>{content}</div>
                        ) : (
                            <ReactMarkdown
                                remarkPlugins={[remarkGfm]}
                                rehypePlugins={[rehypeHighlight]}
                                components={{
                                    code({node, inline, className, children, ...props}: any) {
                                        const match = /language-(\w+)/.exec(className || '')
                                        return !inline && match ? (
                                            <pre style={{
                                                background: '#f6f8fa',
                                                padding: '12px',
                                                borderRadius: '6px',
                                                overflow: 'auto',
                                                fontSize: '13px'
                                            }}>
                                                <code className={className} {...props}>{children}</code>
                                            </pre>
                                        ) : (
                                            <code style={{
                                                background: '#f6f8fa',
                                                padding: '2px 6px',
                                                borderRadius: '3px',
                                                fontSize: '13px'
                                            }} {...props}>{children}</code>
                                        )
                                    },
                                    pre({children}: any) {
                                        return <>{children}</>
                                    }
                                }}
                            >
                                {content || ''}
                            </ReactMarkdown>
                        )}
                    </div>
                    {hasSources && (
                        <>
                            <Divider style={{margin: '8px 0'}}/>
                            <div style={{fontSize: 12, color: '#666', marginBottom: 8}}>
                                参考来源 ({msg.sources!.length}个):
                            </div>
                            <div style={{maxHeight: 300, overflow: 'auto'}}>
                                {msg.sources!.map((s, i) => renderSource(s, i, currentSession?.ragStrategy))}
                            </div>
                        </>
                    )}
                </Card>
            </div>
        )
    }

    // 渲染左侧会话列表项
    const renderSessionItem = (item: ChatSession) => (
        <List.Item
            onClick={() => setCurrentSession(item)}
            style={{
                cursor: 'pointer',
                padding: '8px 12px',
                background: currentSession?.id === item.id ? '#e6f7ff' : 'transparent',
                borderBottom: '1px solid #f0f0f0',
            }}
            actions={[
                <EditOutlined
                    onClick={(e) => {
                        e.stopPropagation();
                        handleEditSession(item)
                    }}
                    style={{color: '#1890ff', fontSize: 12}}
                />,
                <DeleteOutlined
                    onClick={(e) => {
                        e.stopPropagation();
                        handleDeleteSession(item.id)
                    }}
                    style={{color: '#cf1322', fontSize: 12}}
                />
            ]}
        >
            <List.Item.Meta
                title={<span style={{fontSize: 13}}>{item.title || '未命名'}</span>}
                description={
                    <Space size={4}>
                        <span style={{fontSize: 11, color: '#999'}}>{item.messageCount || 0}条</span>
                        {item.enableRag && <Tag color="green" style={{fontSize: 10}}>RAG</Tag>}
                    </Space>
                }
            />
        </List.Item>
    )

    return (
        <Layout style={{height: 'calc(100vh - 120px)', background: '#fff'}}>
            {/* 左侧会话列表 - 分组显示 */}
            <Sider width={280} style={{background: '#fafafa', borderRight: '1px solid #eee'}}>
                {/* 新建对话按钮 */}
                <div style={{padding: 12, borderBottom: '1px solid #eee'}}>
                    <Space direction="vertical" style={{width: '100%'}} size="small">
                        <Button
                            block
                            icon={<MessageOutlined/>}
                            onClick={() => handleCreateSession('normal')}
                        >
                            普通对话
                        </Button>
                        <Button
                            type="primary"
                            block
                            icon={<RobotOutlined/>}
                            onClick={() => handleCreateSession('agent')}
                        >
                            Agent对话
                        </Button>
                    </Space>
                </div>

                {/* 分组会话列表 - 可滚动 */}
                <div style={{height: 'calc(100% - 100px)', overflow: 'auto'}}>
                    <Spin spinning={loading}>
                        {/* 普通对话组 */}
                        {normalSessions.length > 0 && (
                            <div>
                                <div style={{
                                    padding: '8px 12px',
                                    background: '#f5f5f5',
                                    fontSize: 12,
                                    color: '#666',
                                    fontWeight: 500
                                }}>
                                    <MessageOutlined style={{marginRight: 4}}/>
                                    普通对话 ({normalSessions.length})
                                </div>
                                <List
                                    dataSource={normalSessions}
                                    renderItem={renderSessionItem}
                                    style={{background: '#fff'}}
                                />
                            </div>
                        )}

                        {/* Agent对话组 */}
                        {agentSessions.length > 0 && (
                            <div>
                                <div style={{
                                    padding: '8px 12px',
                                    background: '#f5f5f5',
                                    fontSize: 12,
                                    color: '#666',
                                    fontWeight: 500,
                                    marginTop: normalSessions.length > 0 ? 8 : 0
                                }}>
                                    <RobotOutlined style={{marginRight: 4}}/>
                                    Agent对话 ({agentSessions.length})
                                </div>
                                <List
                                    dataSource={agentSessions}
                                    renderItem={(item) => (
                                        <List.Item
                                            onClick={() => setCurrentSession(item)}
                                            style={{
                                                cursor: 'pointer',
                                                padding: '8px 12px',
                                                background: currentSession?.id === item.id ? '#e6f7ff' : 'transparent',
                                                borderBottom: '1px solid #f0f0f0',
                                            }}
                                            actions={[
                                                <EditOutlined
                                                    onClick={(e) => {
                                                        e.stopPropagation();
                                                        handleEditSession(item)
                                                    }}
                                                    style={{color: '#1890ff', fontSize: 12}}
                                                />,
                                                <DeleteOutlined
                                                    onClick={(e) => {
                                                        e.stopPropagation();
                                                        handleDeleteSession(item.id)
                                                    }}
                                                    style={{color: '#cf1322', fontSize: 12}}
                                                />
                                            ]}
                                        >
                                            <List.Item.Meta
                                                title={
                                                    <Space>
                                                        <span style={{fontSize: 13}}>{item.title || '未命名'}</span>
                                                        <Tag color="purple" style={{fontSize: 10}}>
                                                            {item.agentName || 'Agent'}
                                                        </Tag>
                                                    </Space>
                                                }
                                                description={
                                                    <Space size={4}>
                                                        <span style={{
                                                            fontSize: 11,
                                                            color: '#999'
                                                        }}>{item.messageCount || 0}条</span>
                                                        {item.enableRag &&
                                                            <Tag color="green" style={{fontSize: 10}}>RAG</Tag>}
                                                    </Space>
                                                }
                                            />
                                        </List.Item>
                                    )}
                                    style={{background: '#fff'}}
                                />
                            </div>
                        )}

                        {/* 空状态 */}
                        {sessions.length === 0 && !loading && (
                            <Empty description="暂无对话" style={{marginTop: 40}}/>
                        )}
                    </Spin>
                </div>
            </Sider>

            {/* 右侧对话内容 */}
            <Content style={{display: 'flex', flexDirection: 'column'}}>
                {currentSession ? (
                    <>
                        {/* 顶部信息栏 */}
                        <div style={{
                            padding: 12,
                            borderBottom: '1px solid #eee',
                            background: currentSession.agentId ? '#f6f0ff' : '#fff'
                        }}>
                            {/* Agent模式信息卡片 */}
                            {currentSession.agentId && currentAgent && (
                                <Card size="small" style={{marginBottom: 8, background: '#fff'}}>
                                    <div style={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        justifyContent: 'space-between'
                                    }}>
                                        <Space>
                                            <RobotOutlined style={{color: '#722ed1', fontSize: 18}}/>
                                            <span style={{fontWeight: 500, fontSize: 14}}>{currentAgent.name}</span>
                                            <Tag color="purple">Agent模式</Tag>
                                        </Space>
                                        <Tooltip title={`工具数量: ${currentAgent.tools?.length || 0}`}>
                                            <Tag icon={<ToolOutlined/>} color="blue">
                                                {currentAgent.tools?.length || 0} 个工具
                                            </Tag>
                                        </Tooltip>
                                    </div>
                                    {currentAgent.description && (
                                        <div style={{marginTop: 8, fontSize: 12, color: '#666'}}>
                                            {currentAgent.description}
                                        </div>
                                    )}
                                    {currentAgent.systemPrompt && (
                                        <div style={{
                                            marginTop: 8,
                                            fontSize: 12,
                                            color: '#999',
                                            maxHeight: 60,
                                            overflow: 'hidden'
                                        }}>
                                            <InfoCircleOutlined style={{marginRight: 4}}/>
                                            系统提示词: {currentAgent.systemPrompt.slice(0, 100)}...
                                        </div>
                                    )}
                                </Card>
                            )}

                            {/* 会话标题和模型选择 */}
                            <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                                <Space>
                                    <span style={{fontWeight: 500}}>
                                        {currentSession.title || '未命名对话'}
                                    </span>
                                    {!currentSession.agentId && (
                                        <Tag color="default">普通对话</Tag>
                                    )}
                                    {currentSession.enableRag && (
                                        <Tag color="green">RAG</Tag>
                                    )}
                                </Space>
                                {/* 普通对话可选择模型 */}
                                {!currentSession.agentId && (
                                    <Space>
                                        <span style={{fontSize: 12, color: '#666'}}>模型:</span>
                                        <Select
                                            value={selectedModelId || undefined}
                                            onChange={(v) => setSelectedModelId(v || null)}
                                            style={{width: 180}}
                                            placeholder="选择模型"
                                            allowClear
                                            size="small"
                                        >
                                            {models.filter(m => m.isActive && m.modelType === 'CHAT').map(m => (
                                                <Select.Option key={m.id} value={m.id}>
                                                    {m.name}
                                                    {m.isDefault && ' [默认]'}
                                                </Select.Option>
                                            ))}
                                        </Select>
                                    </Space>
                                )}
                                {/* Agent对话显示固定模型 */}
                                {currentSession.agentId && (
                                    <Space>
                                        <span style={{fontSize: 12, color: '#666'}}>模型:</span>
                                        <Tag
                                            color="blue">{models.find(m => m.id === currentSession.modelId)?.name || currentSession.modelId}</Tag>
                                    </Space>
                                )}
                            </div>
                        </div>

                        {/* 消息列表 */}
                        <div style={{flex: 1, overflow: 'auto', padding: 16}}>
                            {messages.length === 0 ? (
                                <Empty description={currentSession.agentId ? "与 Agent 开始对话吧" : "开始对话吧"}/>
                            ) : (
                                messages.map(renderMessage)
                            )}
                            <div ref={messagesEndRef}/>
                        </div>

                        {/* 输入区域 */}
                        <div style={{padding: 12, borderTop: '1px solid #eee'}}>
                            <Input.TextArea
                                value={inputValue}
                                onChange={(e) => setInputValue(e.target.value)}
                                placeholder={currentSession.agentId ? "输入消息，Agent 将使用工具回答..." : "输入消息... (Enter发送, Shift+Enter换行)"}
                                autoSize={{minRows: 2, maxRows: 6}}
                                onPressEnter={(e) => {
                                    if (!e.shiftKey) {
                                        e.preventDefault()
                                        handleSend()
                                    }
                                }}
                            />
                            <div style={{marginTop: 8, textAlign: 'right'}}>
                                <Button type="primary" icon={<SendOutlined/>} onClick={handleSend} loading={sending}>
                                    发送
                                </Button>
                            </div>
                        </div>
                    </>
                ) : (
                    <div style={{flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center'}}>
                        <Empty description="选择或创建一个对话"/>
                    </div>
                )}
            </Content>

            {/* 新建对话 Modal */}
            <Modal
                title={sessionMode === 'agent' ? '新建 Agent 对话' : '新建普通对话'}
                open={sessionModalVisible}
                onOk={handleCreateSessionSubmit}
                onCancel={() => {
                    setSessionModalVisible(false)
                    setEnableRag(false)
                }}
                width={600}
            >
                {/* 模式切换 */}
                <div style={{marginBottom: 16}}>
                    <Radio.Group value={sessionMode} onChange={(e) => setSessionMode(e.target.value)}>
                        <Radio.Button value="normal">
                            <MessageOutlined style={{marginRight: 4}}/>
                            普通对话
                        </Radio.Button>
                        <Radio.Button value="agent">
                            <RobotOutlined style={{marginRight: 4}}/>
                            Agent对话
                        </Radio.Button>
                    </Radio.Group>
                    <div style={{fontSize: 12, color: '#666', marginTop: 4}}>
                        {sessionMode === 'normal' ? '选择模型和提示词模板进行对话' : '关联Agent，使用Agent的模型、系统提示词和工具'}
                    </div>
                </div>

                <Form form={sessionForm} layout="vertical">
                    {/* Agent模式：Agent选择 */}
                    {sessionMode === 'agent' && (
                        <>
                            <Form.Item
                                name="agentId"
                                label="选择 Agent"
                                rules={[{required: true, message: '请选择一个Agent'}]}
                            >
                                <Select
                                    placeholder="选择 Agent"
                                    onChange={(value: string | undefined) => {
                                        if (value) {
                                            const agent = agents.find(a => a.id === value)
                                            if (agent) {
                                                sessionForm.setFieldsValue({
                                                    title: `对话: ${agent.name}`
                                                })
                                            }
                                        }
                                    }}
                                >
                                    {agents.filter(a => a.status === 'PUBLISHED').map(a => (
                                        <Select.Option key={a.id} value={a.id}>
                                            <Space>
                                                <RobotOutlined/>
                                                {a.name}
                                                <Tag color="blue" style={{marginLeft: 8}}>
                                                    {a.tools?.length || 0}个工具
                                                </Tag>
                                            </Space>
                                        </Select.Option>
                                    ))}
                                </Select>
                            </Form.Item>
                            <div style={{fontSize: 12, color: '#999', marginBottom: 16}}>
                                <InfoCircleOutlined style={{marginRight: 4}}/>
                                Agent对话将自动使用Agent的模型和系统提示词，无需手动配置
                            </div>
                        </>
                    )}

                    {/* 普通模式：模型和提示词 */}
                    {sessionMode === 'normal' && (
                        <>
                            <Form.Item name="modelId" label="模型">
                                <Select placeholder="选择模型" allowClear>
                                    {models.filter(m => m.isActive).map(m => (
                                        <Select.Option key={m.id} value={m.id}>
                                            {m.name} ({m.modelName})
                                            {m.isDefault && ' [默认]'}
                                        </Select.Option>
                                    ))}
                                </Select>
                            </Form.Item>
                            <Form.Item name="promptId" label="提示词模板">
                                <Select
                                    placeholder="选择提示词模板（可选）"
                                    allowClear
                                    onChange={(value: string | undefined) => {
                                        const prompt = prompts.find(p => p.id === value)
                                        if (prompt) {
                                            sessionForm.setFieldsValue({systemMessage: prompt.content})
                                        }
                                    }}
                                >
                                    {prompts.map(p => (
                                        <Select.Option key={p.id} value={p.id}>
                                            {p.name}
                                        </Select.Option>
                                    ))}
                                </Select>
                            </Form.Item>
                            <Form.Item name="systemMessage" label="系统提示">
                                <Input.TextArea rows={3} placeholder="可选的系统提示词"/>
                            </Form.Item>
                        </>
                    )}

                    <Form.Item name="title" label="标题">
                        <Input placeholder="对话标题"/>
                    </Form.Item>

                    {/* RAG配置（两种模式都可用） */}
                    <Divider>RAG 检索增强（可选）</Divider>

                    <Form.Item label={
                        <Space>
                            启用 RAG 检索增强
                            <InfoCircleOutlined title="启用后，对话时会先从知识库检索相关文档，再生成回答"/>
                        </Space>
                    }>
                        <Switch checked={enableRag} onChange={(v) => {
                            setEnableRag(v)
                            if (!v) {
                                sessionForm.setFieldsValue({
                                    knowledgeBaseId: undefined,
                                    ragEmbeddingModelId: undefined
                                })
                            }
                        }}/>
                    </Form.Item>

                    {enableRag && (
                        <>
                            <Form.Item name="knowledgeBaseId" label="知识库"
                                       rules={[{required: true, message: '请选择知识库'}]}>
                                <Select
                                    placeholder="选择知识库"
                                    onChange={(value: string) => {
                                        const kb = knowledgeBases.find(k => k.id === value)
                                        if (kb?.defaultEmbeddingModelId) {
                                            sessionForm.setFieldsValue({ragEmbeddingModelId: kb.defaultEmbeddingModelId})
                                        }
                                    }}
                                >
                                    {knowledgeBases.map(kb => (
                                        <Select.Option key={kb.id} value={kb.id}>
                                            {kb.name} ({kb.documentCount}文档)
                                        </Select.Option>
                                    ))}
                                </Select>
                            </Form.Item>

                            <Form.Item name="ragStrategy" label="检索策略">
                                <Select
                                    value={ragStrategy}
                                    onChange={handleStrategyChange}
                                    options={[
                                        {value: 'VECTOR', label: '向量检索 (语义相似度)'},
                                        {value: 'BM25', label: 'BM25 关键词 (精确匹配)'},
                                        {value: 'HYBRID', label: '混合检索 (RRF融合)'}
                                    ]}
                                />
                            </Form.Item>

                            <Form.Item label="检索数量 (topK)">
                                <Slider
                                    min={1} max={20} value={ragTopK}
                                    onChange={(v) => {
                                        setRagTopK(v)
                                    }}
                                    marks={{1: '1', 5: '5', 10: '10', 20: '20'}}
                                />
                            </Form.Item>

                            <Form.Item label={
                                <Space>
                                    相似度阈值
                                    <InfoCircleOutlined title={getThresholdConfig(ragStrategy).tip}/>
                                </Space>
                            }>
                                <Slider
                                    min={getThresholdConfig(ragStrategy).min}
                                    max={getThresholdConfig(ragStrategy).max}
                                    step={getThresholdConfig(ragStrategy).step}
                                    value={ragThreshold}
                                    onChange={(v) => setRagThreshold(v)}
                                    marks={getThresholdConfig(ragStrategy).marks}
                                />
                            </Form.Item>
                        </>
                    )}
                </Form>
            </Modal>

            {/* 编辑对话 Modal */}
            <Modal
                title={`编辑对话 - ${sessionMode === 'agent' ? 'Agent模式' : '普通模式'}`}
                open={editSessionModalVisible}
                onOk={handleEditSessionSubmit}
                onCancel={() => {
                    setEditSessionModalVisible(false)
                    setEnableRag(false)
                }}
                width={600}
            >
                {/* 模式切换 */}
                <div style={{marginBottom: 16}}>
                    <Radio.Group value={sessionMode} onChange={(e) => setSessionMode(e.target.value)}>
                        <Radio.Button value="normal">
                            <MessageOutlined style={{marginRight: 4}}/>
                            普通对话
                        </Radio.Button>
                        <Radio.Button value="agent">
                            <RobotOutlined style={{marginRight: 4}}/>
                            Agent对话
                        </Radio.Button>
                    </Radio.Group>
                </div>

                <Form form={editSessionForm} layout="vertical">
                    {sessionMode === 'agent' && (
                        <>
                            <Form.Item
                                name="agentId"
                                label="选择 Agent"
                                rules={[{required: true, message: '请选择一个Agent'}]}
                            >
                                <Select placeholder="选择 Agent">
                                    {agents.filter(a => a.status === 'PUBLISHED').map(a => (
                                        <Select.Option key={a.id} value={a.id}>
                                            <Space>
                                                <RobotOutlined/>
                                                {a.name}
                                                <Tag color="blue">{a.tools?.length || 0}个工具</Tag>
                                            </Space>
                                        </Select.Option>
                                    ))}
                                </Select>
                            </Form.Item>
                        </>
                    )}

                    {sessionMode === 'normal' && (
                        <>
                            <Form.Item name="modelId" label="模型">
                                <Select placeholder="选择模型" allowClear>
                                    {models.filter(m => m.isActive).map(m => (
                                        <Select.Option key={m.id} value={m.id}>
                                            {m.name} ({m.modelName})
                                            {m.isDefault && ' [默认]'}
                                        </Select.Option>
                                    ))}
                                </Select>
                            </Form.Item>
                            <Form.Item name="promptId" label="提示词模板">
                                <Select placeholder="选择提示词模板（可选）" allowClear>
                                    {prompts.map(p => (
                                        <Select.Option key={p.id} value={p.id}>
                                            {p.name}
                                        </Select.Option>
                                    ))}
                                </Select>
                            </Form.Item>
                            <Form.Item name="systemMessage" label="系统提示">
                                <Input.TextArea rows={3} placeholder="可选的系统提示词"/>
                            </Form.Item>
                        </>
                    )}

                    <Form.Item name="title" label="标题">
                        <Input placeholder="对话标题"/>
                    </Form.Item>

                    <Divider>RAG 检索增强（可选）</Divider>

                    <Form.Item label="启用 RAG 检索增强">
                        <Switch checked={enableRag} onChange={(v) => {
                            setEnableRag(v)
                            if (!v) {
                                editSessionForm.setFieldsValue({
                                    knowledgeBaseId: undefined,
                                    ragEmbeddingModelId: undefined
                                })
                            }
                        }}/>
                    </Form.Item>

                    {enableRag && (
                        <>
                            <Form.Item name="knowledgeBaseId" label="知识库"
                                       rules={[{required: true, message: '请选择知识库'}]}>
                                <Select placeholder="选择知识库">
                                    {knowledgeBases.map(kb => (
                                        <Select.Option key={kb.id} value={kb.id}>
                                            {kb.name} ({kb.documentCount}文档)
                                        </Select.Option>
                                    ))}
                                </Select>
                            </Form.Item>
                            <Form.Item name="ragStrategy" label="检索策略">
                                <Select value={ragStrategy} onChange={handleStrategyChange}
                                    options={[
                                        {value: 'VECTOR', label: '向量检索'},
                                        {value: 'BM25', label: 'BM25 关键词'},
                                        {value: 'HYBRID', label: '混合检索'}
                                    ]}
                                />
                            </Form.Item>
                            <Form.Item label="检索数量">
                                <Slider min={1} max={20} value={ragTopK} onChange={setRagTopK}
                                        marks={{1: '1', 5: '5', 10: '10', 20: '20'}}/>
                            </Form.Item>
                        </>
                    )}
                </Form>
            </Modal>
        </Layout>
    )
}

export default ChatPage