import React, {useState, useEffect, useRef} from 'react'
import {Layout, List, Input, Button, Card, Select, message, Empty, Spin} from 'antd'
import {SendOutlined, PlusOutlined} from '@ant-design/icons'
import {createSession, listSessions, sendMessage, getSessionMessages} from '@/api/chat'
import {ChatSession, ChatMessage, CreateSessionRequest} from '@/types/chat'

const {Sider, Content} = Layout

const ChatPage: React.FC = () => {
    const [sessions, setSessions] = useState<ChatSession[]>([])
    const [currentSession, setCurrentSession] = useState<ChatSession | null>(null)
    const [messages, setMessages] = useState<ChatMessage[]>([])
    const [inputValue, setInputValue] = useState('')
    const [loading, setLoading] = useState(false)
    const [sending, setSending] = useState(false)
    const messagesEndRef = useRef<HTMLDivElement>(null)

    useEffect(() => {
        fetchSessions()
    }, [])

    useEffect(() => {
        if (currentSession) {
            fetchMessages(currentSession.id)
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

    const handleCreateSession = async () => {
        try {
            const request: CreateSessionRequest = {title: '新对话'}
            const res = await createSession(request)
            if (res.success) {
                setCurrentSession(res.data)
                setMessages([])
                fetchSessions()
                message.success('创建成功')
            }
        } catch {
            message.error('创建失败')
        }
    }

    const handleSend = async () => {
        if (!inputValue.trim() || !currentSession) return

        setSending(true)
        try {
            await sendMessage({
                sessionId: currentSession.id,
                content: inputValue.trim(),
            })
            setInputValue('')
            fetchMessages(currentSession.id)
        } catch {
            message.error('发送失败')
        } finally {
            setSending(false)
        }
    }

    const renderMessage = (msg: ChatMessage) => {
        const isUser = msg.role === 'USER'
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
                        backgroundColor: isUser ? '#e6f7ff' : '#f5f5f5',
                    }}
                >
                    <div style={{fontSize: 12, color: '#999', marginBottom: 4}}>
                        {isUser ? '用户' : 'AI'}
                        {msg.modelName && ` (${msg.modelName})`}
                        {msg.latencyMs && ` - ${msg.latencyMs}ms`}
                    </div>
                    <div style={{whiteSpace: 'pre-wrap'}}>{msg.content}</div>
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
                                placeholder="输入消息..."
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
        </Layout>
    )
}

export default ChatPage