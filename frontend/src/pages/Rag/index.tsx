import React, {useEffect, useRef, useState} from 'react'
import {Button, Card, Collapse, Divider, Empty, Input, Layout, List, message, Select, Space, Tag} from 'antd'
import {ClearOutlined, FileTextOutlined, SendOutlined} from '@ant-design/icons'
import {ragChat, vectorSearch} from '@/api/rag'
import {listDocuments} from '@/api/documents'
import {RagChatResponse, RagSource, VectorSearchResult} from '@/types/rag'
import {Document} from '@/types/document'

const {Sider, Content} = Layout

interface ChatMessage {
    id: string
    role: 'user' | 'assistant'
    content: string
    sources?: RagSource[]
    timestamp: Date
}

const RagPage: React.FC = () => {
    const [documents, setDocuments] = useState<Document[]>([])
    const [selectedDocIds, setSelectedDocIds] = useState<string[]>([])
    const [messages, setMessages] = useState<ChatMessage[]>([])
    const [inputValue, setInputValue] = useState('')
    const [sending, setSending] = useState(false)
    const [sessionId, setSessionId] = useState<string | null>(null)
    const [mode, setMode] = useState<'rag' | 'search'>('rag')
    const [searchResults, setSearchResults] = useState<VectorSearchResult[]>([])
    const messagesEndRef = useRef<HTMLDivElement>(null)

    useEffect(() => {
        fetchDocuments()
    }, [])

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({behavior: 'smooth'})
    }, [messages])

    const fetchDocuments = async () => {
        try {
            const res = await listDocuments()
            if (res.success) {
                setDocuments(res.data.content?.filter(d => d.status === 'COMPLETED') || [])
            }
        } catch {
            // ignore
        }
    }

    const handleSend = async () => {
        if (!inputValue.trim()) return

        const userMessage: ChatMessage = {
            id: Date.now().toString(),
            role: 'user',
            content: inputValue.trim(),
            timestamp: new Date()
        }
        setMessages(prev => [...prev, userMessage])
        setInputValue('')
        setSending(true)

        try {
            if (mode === 'rag') {
                const res = await ragChat({
                    query: userMessage.content,
                    documentIds: selectedDocIds.length > 0 ? selectedDocIds : undefined,
                    topK: 5,
                    sessionId: sessionId || undefined
                })

                if (res.success) {
                    const data: RagChatResponse = res.data
                    setSessionId(data.sessionId)

                    const assistantMessage: ChatMessage = {
                        id: (Date.now() + 1).toString(),
                        role: 'assistant',
                        content: data.answer,
                        sources: data.sources,
                        timestamp: new Date()
                    }
                    setMessages(prev => [...prev, assistantMessage])
                }
            } else {
                const res = await vectorSearch({
                    query: userMessage.content,
                    topK: 10,
                    threshold: 0.5
                })

                if (res.success) {
                    setSearchResults(res.data || [])
                }
            }
        } catch {
            message.error('请求失败')
        } finally {
            setSending(false)
        }
    }

    const handleClear = () => {
        setMessages([])
        setSearchResults([])
        setSessionId(null)
    }

    const renderSource = (source: RagSource, index: number) => (
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
                        <Tag>相似度: {source.score.toFixed(3)}</Tag>
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

    const renderMessage = (msg: ChatMessage) => (
        <div
            key={msg.id}
            style={{
                marginBottom: 16,
                display: 'flex',
                flexDirection: msg.role === 'user' ? 'row-reverse' : 'row'
            }}
        >
            <Card
                size="small"
                style={{
                    maxWidth: '80%',
                    backgroundColor: msg.role === 'user' ? '#e6f7ff' : '#f5f5f5'
                }}
            >
                <div style={{fontSize: 12, color: '#999', marginBottom: 4}}>
                    {msg.role === 'user' ? '用户' : 'AI'}
                    <span style={{marginLeft: 8}}>{msg.timestamp.toLocaleTimeString()}</span>
                </div>
                <div style={{whiteSpace: 'pre-wrap'}}>{msg.content}</div>
                {msg.sources && msg.sources.length > 0 && (
                    <>
                        <Divider style={{margin: '8px 0'}}/>
                        <div style={{fontSize: 12, color: '#666', marginBottom: 8}}>
                            参考来源 ({msg.sources.length}个):
                        </div>
                        <div style={{maxHeight: 300, overflow: 'auto'}}>
                            {msg.sources.map((s, i) => renderSource(s, i))}
                        </div>
                    </>
                )}
            </Card>
        </div>
    )

    return (
        <Layout style={{height: 'calc(100vh - 150px)', background: '#fff'}}>
            <Sider width={250} style={{background: '#fafafa', borderRight: '1px solid #eee'}}>
                <div style={{padding: 16}}>
                    <div style={{marginBottom: 12}}>
                        <span style={{fontWeight: 500}}>模式选择</span>
                    </div>
                    <Select
                        value={mode}
                        onChange={setMode}
                        style={{width: '100%'}}
                        options={[
                            {value: 'rag', label: 'RAG 对话'},
                            {value: 'search', label: '向量检索'}
                        ]}
                    />
                </div>
                <Divider style={{margin: '12px 0'}}/>
                <div style={{padding: 16}}>
                    <div style={{marginBottom: 12}}>
                        <span style={{fontWeight: 500}}>文档筛选</span>
                    </div>
                    <Select
                        mode="multiple"
                        allowClear
                        placeholder="选择文档（可选）"
                        value={selectedDocIds}
                        onChange={setSelectedDocIds}
                        style={{width: '100%'}}
                        options={documents.map(d => ({
                            value: d.id,
                            label: `${d.name} (${d.totalChunks}块)`
                        }))}
                    />
                </div>
                <Divider style={{margin: '12px 0'}}/>
                <div style={{padding: 16}}>
                    <Button icon={<ClearOutlined/>} onClick={handleClear} block>
                        清空对话
                    </Button>
                </div>
            </Sider>
            <Content style={{display: 'flex', flexDirection: 'column'}}>
                {mode === 'rag' ? (
                    <>
                        <div style={{flex: 1, overflow: 'auto', padding: 16}}>
                            {messages.length === 0 ? (
                                <Empty description="开始 RAG 对话，输入问题将自动检索相关文档"/>
                            ) : (
                                messages.map(renderMessage)
                            )}
                            <div ref={messagesEndRef}/>
                        </div>
                        <div style={{padding: 16, borderTop: '1px solid #eee'}}>
                            <Input.TextArea
                                value={inputValue}
                                onChange={(e) => setInputValue(e.target.value)}
                                placeholder="输入问题..."
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
                    <>
                        <div style={{padding: 16, borderBottom: '1px solid #eee'}}>
                            <Space.Compact style={{width: '100%'}}>
                                <Input
                                    value={inputValue}
                                    onChange={(e) => setInputValue(e.target.value)}
                                    placeholder="输入搜索内容..."
                                    onPressEnter={handleSend}
                                />
                                <Button type="primary" icon={<SendOutlined/>} onClick={handleSend} loading={sending}>
                                    搜索
                                </Button>
                            </Space.Compact>
                        </div>
                        <div style={{flex: 1, overflow: 'auto', padding: 16}}>
                            {searchResults.length === 0 ? (
                                <Empty description="输入内容进行向量相似度搜索"/>
                            ) : (
                                <List
                                    dataSource={searchResults}
                                    renderItem={(item, index) => (
                                        <List.Item>
                                            <Card size="small" style={{width: '100%'}}>
                                                <Space style={{marginBottom: 8}}>
                                                    <Tag color="blue">{index + 1}</Tag>
                                                    <FileTextOutlined/>
                                                    <span>{item.documentName}</span>
                                                    <Tag color="green">相似度: {item.score.toFixed(3)}</Tag>
                                                </Space>
                                                <div style={{whiteSpace: 'pre-wrap', fontSize: 13}}>
                                                    {item.content}
                                                </div>
                                            </Card>
                                        </List.Item>
                                    )}
                                />
                            )}
                        </div>
                    </>
                )}
            </Content>
        </Layout>
    )
}

export default RagPage