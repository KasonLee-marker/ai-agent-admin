import React, {useEffect, useRef, useState} from 'react'
import {Button, Card, Empty, Form, Input, Layout, List, message, Modal, Select, Space, Spin} from 'antd'
import {DeleteOutlined, EditOutlined, PlusOutlined, SendOutlined} from '@ant-design/icons'
import {
    createSession,
    deleteSession,
    getSessionMessages,
    listSessions,
    sendMessageStream,
    updateSession
} from '@/api/chat'
import {listModels} from '@/api/models'
import {listPrompts} from '@/api/prompts'
import {ChatMessage, ChatSession, CreateSessionRequest} from '@/types/chat'
import {ModelConfig} from '@/types/model'
import {PromptTemplate} from '@/types/prompt'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import rehypeHighlight from 'rehype-highlight'
import 'highlight.js/styles/github.css'

const {Sider, Content} = Layout

const ChatPage: React.FC = () => {
    const [sessions, setSessions] = useState<ChatSession[]>([])
    const [models, setModels] = useState<ModelConfig[]>([])
    const [prompts, setPrompts] = useState<PromptTemplate[]>([])
    const [currentSession, setCurrentSession] = useState<ChatSession | null>(null)
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

    // 流式输出状态
    const [isStreaming, setIsStreaming] = useState(false)

    useEffect(() => {
        fetchSessions()
        fetchModels()
        fetchPrompts()
    }, [])

    useEffect(() => {
        if (currentSession) {
            fetchMessages(currentSession.id)
            // 使用会话的模型或默认模型
            setSelectedModelId(currentSession.modelId || getDefaultModelId())
        }
    }, [currentSession])

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
                setModels(res.data || [])
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

    const handleCreateSession = () => {
        sessionForm.resetFields()
        sessionForm.setFieldsValue({
            title: '新对话',
            modelId: getDefaultModelId()
        })
        setSessionModalVisible(true)
    }

    const handleCreateSessionSubmit = async () => {
        try {
            const values = await sessionForm.validateFields()
            const request: CreateSessionRequest = {
                title: values.title,
                modelId: values.modelId,
                systemMessage: values.systemMessage
            }
            const res = await createSession(request)
            if (res.success) {
                setCurrentSession(res.data)
                setSelectedModelId(values.modelId)
                setMessages([])
                fetchSessions()
                setSessionModalVisible(false)
                message.success('创建成功')
            }
        } catch {
            message.error('创建失败')
        }
    }

    const handleEditSession = (session: ChatSession) => {
        setEditingSession(session)
        editSessionForm.setFieldsValue({
            title: session.title,
            modelId: session.modelId,
            promptId: session.promptId,
            systemMessage: session.systemMessage
        })
        setEditSessionModalVisible(true)
    }

    const handleEditSessionSubmit = async () => {
        if (!editingSession) return
        try {
            const values = await editSessionForm.validateFields()
            const res = await updateSession(editingSession.id, values)
            if (res.success) {
                // 如果编辑的是当前会话，更新当前会话信息
                if (currentSession?.id === editingSession.id) {
                    setCurrentSession(res.data)
                }
                fetchSessions()
                setEditSessionModalVisible(false)
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

        // 先添加用户消息到列表
        const tempUserMessage: ChatMessage = {
            id: 'temp-user',
            sessionId: currentSession.id,
            role: 'USER',
            content: userContent,
            isError: false,
            createdAt: new Date().toISOString(),
        }
        setMessages(prev => [...prev, tempUserMessage])

        // 添加一个临时的 AI 消息用于流式显示
        const tempAiMessage: ChatMessage = {
            id: 'temp-ai',
            sessionId: currentSession.id,
            role: 'ASSISTANT',
            content: '',
            isError: false,
            createdAt: new Date().toISOString(),
        }
        setMessages(prev => [...prev, tempAiMessage])

        await sendMessageStream(
            {
                sessionId: currentSession.id,
                content: userContent,
                modelId: selectedModelId || undefined,
            },
            (text) => {
                // 实时更新流式文本
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
                // 流式完成，刷新消息列表获取完整数据
                setIsStreaming(false)
                setSending(false)
                fetchMessages(currentSession.id)
            },
            (error) => {
                setIsStreaming(false)
                setSending(false)
                message.error(`发送失败: ${error.message}`)
                // 移除临时消息
                setMessages(prev => prev.filter(m => m.id !== 'temp-user' && m.id !== 'temp-ai'))
            }
        )
    }

    const renderMessage = (msg: ChatMessage) => {
        const isUser = msg.role === 'USER'
        const isError = msg.isError
        const content = isError && msg.errorMessage ? `**错误:** ${msg.errorMessage}` : msg.content
        const isTempAi = msg.id === 'temp-ai'

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
                    <div style={{fontSize: 12, color: '#999', marginBottom: 4}}>
                        {isUser ? '用户' : 'AI'}
                        {msg.modelName && ` (${msg.modelName})`}
                        {msg.latencyMs && ` - ${msg.latencyMs}ms`}
                        {isError && <span style={{color: '#cf1322'}}> [错误]</span>}
                        {isTempAi && isStreaming && <span style={{color: '#1890ff'}}> [生成中...]</span>}
                    </div>
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
                </Card>
            </div>
        )
    }

    return (
        <Layout style={{height: 'calc(100vh - 150px)', background: '#fff'}}>
            <Sider width={250} style={{background: '#fafafa', borderRight: '1px solid #eee'}}>
                <div style={{padding: 16}}>
                    <Button type="primary" icon={<PlusOutlined/>} block onClick={handleCreateSession}>
                        新建对话
                    </Button>
                </div>
                <Spin spinning={loading}>
                    <List
                        dataSource={sessions}
                        renderItem={(item) => (
                            <List.Item
                                onClick={() => setCurrentSession(item)}
                                style={{
                                    cursor: 'pointer',
                                    padding: '12px 16px',
                                    background: currentSession?.id === item.id ? '#e6f7ff' : 'transparent',
                                }}
                                actions={[
                                    <EditOutlined
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            handleEditSession(item)
                                        }}
                                        style={{color: '#1890ff'}}
                                    />,
                                    <DeleteOutlined
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            handleDeleteSession(item.id)
                                        }}
                                        style={{color: '#cf1322'}}
                                    />
                                ]}
                            >
                                <List.Item.Meta
                                    title={item.title || '未命名对话'}
                                    description={`${item.messageCount || 0} 条消息`}
                                />
                            </List.Item>
                        )}
                    />
                </Spin>
            </Sider>
            <Content style={{display: 'flex', flexDirection: 'column'}}>
                {currentSession ? (
                    <>
                        <div style={{
                            padding: 16,
                            borderBottom: '1px solid #eee',
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center'
                        }}>
                            <Space>
                                <span style={{fontWeight: 500}}>{currentSession.title || '未命名对话'}</span>
                            </Space>
                            <Space>
                                <span style={{fontSize: 12, color: '#666'}}>当前模型:</span>
                                <Select
                                    value={selectedModelId || undefined}
                                    onChange={(v) => setSelectedModelId(v || null)}
                                    style={{width: 200}}
                                    placeholder="选择模型"
                                    allowClear
                                >
                                    {models.filter(m => m.isActive).map(m => (
                                        <Select.Option key={m.id} value={m.id}>
                                            {m.name} ({m.modelName})
                                            {m.isDefault && <span style={{color: '#1890ff'}}> [默认]</span>}
                                        </Select.Option>
                                    ))}
                                </Select>
                            </Space>
                        </div>
                        <div style={{flex: 1, overflow: 'auto', padding: 16}}>
                            {messages.length === 0 ? (
                                <Empty description="开始对话吧"/>
                            ) : (
                                messages.map(renderMessage)
                            )}
                            <div ref={messagesEndRef}/>
                        </div>
                        <div style={{padding: 16, borderTop: '1px solid #eee'}}>
                            <Input.TextArea
                                value={inputValue}
                                onChange={(e) => setInputValue(e.target.value)}
                                placeholder="输入消息... (Enter发送, Shift+Enter换行)"
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

            <Modal
                title="新建对话"
                open={sessionModalVisible}
                onOk={handleCreateSessionSubmit}
                onCancel={() => setSessionModalVisible(false)}
            >
                <Form form={sessionForm} layout="vertical">
                    <Form.Item name="title" label="标题">
                        <Input placeholder="对话标题"/>
                    </Form.Item>
                    <Form.Item name="promptId" label="提示词模板">
                        <Select
                            placeholder="选择提示词模板（可选）"
                            allowClear
                            onChange={(value: string | undefined) => {
                                // 选择模板后自动填充系统消息
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
                    <Form.Item name="systemMessage" label="系统提示">
                        <Input.TextArea rows={3} placeholder="可选的系统提示词"/>
                    </Form.Item>
                </Form>
            </Modal>

            <Modal
                title="编辑对话"
                open={editSessionModalVisible}
                onOk={handleEditSessionSubmit}
                onCancel={() => setEditSessionModalVisible(false)}
            >
                <Form form={editSessionForm} layout="vertical">
                    <Form.Item name="title" label="标题">
                        <Input placeholder="对话标题"/>
                    </Form.Item>
                    <Form.Item name="promptId" label="提示词模板">
                        <Select
                            placeholder="选择提示词模板（可选）"
                            allowClear
                            onChange={(value: string | undefined) => {
                                // 选择模板后自动填充系统消息
                                const prompt = prompts.find(p => p.id === value)
                                if (prompt) {
                                    editSessionForm.setFieldsValue({systemMessage: prompt.content})
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
                    <Form.Item name="systemMessage" label="系统提示">
                        <Input.TextArea rows={3} placeholder="可选的系统提示词"/>
                    </Form.Item>
                </Form>
            </Modal>
        </Layout>
    )
}

export default ChatPage