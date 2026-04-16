import React, {useEffect, useRef, useState} from 'react'
import {
    Button,
    Card,
    Collapse,
    Divider,
    Empty,
    Input,
    Layout,
    List,
    message,
    Select,
    Slider,
    Space,
    Switch,
    Tag
} from 'antd'
import {ClearOutlined, FileTextOutlined, InfoCircleOutlined, PlusOutlined, SendOutlined} from '@ant-design/icons'
import {ragChatStream, vectorSearch} from '@/api/rag'
import {listDocuments} from '@/api/documents'
import {listModels} from '@/api/models'
import {listPrompts} from '@/api/prompts'
import {listAllKnowledgeBases} from '@/api/knowledgeBase'
import {RagSource, VectorSearchResult} from '@/types/rag'
import {Document} from '@/types/document'
import {ModelConfig} from '@/types/model'
import {PromptTemplate} from '@/types/prompt'
import {KnowledgeBase} from '@/types/knowledgeBase'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import rehypeHighlight from 'rehype-highlight'
import 'highlight.js/styles/github.css'

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
    const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBase[]>([])
    const [prompts, setPrompts] = useState<PromptTemplate[]>([])
    const [models, setModels] = useState<ModelConfig[]>([])
    const [embeddingModels, setEmbeddingModels] = useState<ModelConfig[]>([])
    const [rerankModels, setRerankModels] = useState<ModelConfig[]>([])
    const [selectedDocIds, setSelectedDocIds] = useState<string[]>([])
    const [selectedKnowledgeBaseId, setSelectedKnowledgeBaseId] = useState<string | null>(null)
    const [selectedPromptId, setSelectedPromptId] = useState<string | null>(null)
    const [selectedModelId, setSelectedModelId] = useState<string | null>(null)
    const [selectedEmbeddingModelId, setSelectedEmbeddingModelId] = useState<string | null>(null)
    const [selectedRerankModelId, setSelectedRerankModelId] = useState<string | null>(null)
    const [enableRerank, setEnableRerank] = useState(false)
    const [topK, setTopK] = useState(5)
    const [threshold, setThreshold] = useState(0.5)
    const [strategy, setStrategy] = useState('VECTOR')
    const [messages, setMessages] = useState<ChatMessage[]>([])
    const [inputValue, setInputValue] = useState('')
    const [sending, setSending] = useState(false)
    const [sessionId, setSessionId] = useState<string | null>(null)
    const [mode, setMode] = useState<'rag' | 'search'>('rag')
    const [searchResults, setSearchResults] = useState<VectorSearchResult[]>([])
    const messagesEndRef = useRef<HTMLDivElement>(null)

    useEffect(() => {
        fetchDocuments()
        fetchKnowledgeBases()
        fetchPrompts()
        fetchModels()
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

    const fetchModels = async () => {
        try {
            const res = await listModels({isActive: true})
            if (res.success) {
                // CHAT 类型模型用于 RAG 对话
                const chatModels = res.data.filter(m => m.modelType === 'CHAT')
                setModels(chatModels)
                const defaultChatModel = chatModels.find(m => m.isDefault)
                if (defaultChatModel) {
                    setSelectedModelId(defaultChatModel.id)
                }

                // EMBEDDING 类型模型用于向量检索
                const embedModels = res.data.filter(m => m.modelType === 'EMBEDDING')
                setEmbeddingModels(embedModels)
                const defaultEmbedModel = embedModels.find(m => m.isDefaultEmbedding)
                if (defaultEmbedModel) {
                    setSelectedEmbeddingModelId(defaultEmbedModel.id)
                }

                // RERANK 类型模型用于重排序
                const rerankModelList = res.data.filter(m => m.modelType === 'RERANK')
                setRerankModels(rerankModelList)
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

        // 添加一个临时的 AI 消息用于流式显示
        const tempAiMessage: ChatMessage = {
            id: (Date.now() + 1).toString(),
            role: 'assistant',
            content: '',
            timestamp: new Date()
        }
        setMessages(prev => [...prev, tempAiMessage])

        try {
            if (mode === 'rag') {
                // 使用流式方法
                await ragChatStream(
                    {
                        question: userMessage.content,
                        promptTemplateId: selectedPromptId || undefined,
                        documentIds: selectedDocIds.length > 0 ? selectedDocIds : undefined,
                        topK: topK,
                        threshold: threshold,
                        sessionId: sessionId || undefined,
                        modelId: selectedModelId || undefined,
                        embeddingModelId: selectedEmbeddingModelId || undefined,
                        knowledgeBaseId: selectedKnowledgeBaseId || undefined,
                        strategy: strategy,
                        enableRerank: enableRerank,
                        rerankModelId: enableRerank && selectedRerankModelId ? selectedRerankModelId : undefined
                    },
                    (text) => {
                        // 实时更新流式文本
                        setMessages(prev => {
                            const newMessages = [...prev]
                            const aiMsgIndex = newMessages.findIndex(m => m.id === tempAiMessage.id)
                            if (aiMsgIndex !== -1) {
                                newMessages[aiMsgIndex] = {
                                    ...newMessages[aiMsgIndex],
                                    content: text,
                                }
                            }
                            return newMessages
                        })
                    },
                    (newSessionId) => {
                        // 流式完成
                        setSending(false)
                        // 如果返回了新的 sessionId，更新状态
                        if (newSessionId && !sessionId) {
                            setSessionId(newSessionId)
                        }
                    },
                    (error) => {
                        // 错误处理
                        message.error(`请求失败: ${error.message}`)
                        setSending(false)
                        // 移除临时消息
                        setMessages(prev => prev.filter(m => m.id !== tempAiMessage.id))
                    }
                )
            } else {
                // 向量检索模式（保持同步）
                const res = await vectorSearch({
                    query: userMessage.content,
                    topK: 10,
                    threshold: 0.5
                })

                if (res.success) {
                    setSearchResults(res.data || [])
                }
                setSending(false)
            }
        } catch {
            message.error('请求失败')
            setSending(false)
        }
    }

    const handleClear = () => {
        setMessages([])
        setSearchResults([])
        setSessionId(null)
    }

    // 新对话按钮处理
    const handleNewConversation = () => {
        setMessages([])
        setSessionId(null)
        message.success('已开始新对话')
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
                {msg.role === 'user' ? (
                    <div style={{whiteSpace: 'pre-wrap'}}>{msg.content}</div>
                ) : (
                    <div style={{fontSize: 14}}>
                        <ReactMarkdown
                            remarkPlugins={[remarkGfm]}
                            rehypePlugins={[rehypeHighlight]}
                        >
                            {msg.content || '...'}
                        </ReactMarkdown>
                    </div>
                )}
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
            <Sider width={250} style={{background: '#fafafa', borderRight: '1px solid #eee', overflow: 'auto'}}>
                <div style={{
                    padding: '8px 12px',
                    background: '#e6f7ff',
                    borderBottom: '1px solid #91d5ff',
                    fontSize: 12,
                    color: '#1890ff'
                }}>
                    <InfoCircleOutlined style={{marginRight: 4}}/>
                    基于知识库文档进行智能问答，检索相关内容生成答案
                </div>
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
                        <span style={{fontWeight: 500}}>知识库筛选</span>
                    </div>
                    <Select
                        allowClear
                        placeholder="选择知识库（可选）"
                        value={selectedKnowledgeBaseId}
                        onChange={setSelectedKnowledgeBaseId}
                        style={{width: '100%'}}
                        options={knowledgeBases.map(kb => ({
                            value: kb.id,
                            label: `${kb.name} (${kb.documentCount}文档)`
                        }))}
                    />
                </div>
                <Divider style={{margin: '12px 0'}}/>
                <div style={{padding: 16}}>
                    <div style={{marginBottom: 12}}>
                        <span style={{fontWeight: 500}}>检索配置</span>
                    </div>
                    <div style={{marginBottom: 12}}>
                        <div style={{fontSize: 12, color: '#666', marginBottom: 4}}>返回数量 (topK)</div>
                        <Slider
                            min={1}
                            max={20}
                            value={topK}
                            onChange={setTopK}
                            marks={{1: '1', 5: '5', 10: '10', 20: '20'}}
                        />
                    </div>
                    <div style={{marginBottom: 12}}>
                        <div style={{fontSize: 12, color: '#666', marginBottom: 4}}>相似度阈值</div>
                        <Slider
                            min={0}
                            max={1}
                            step={0.1}
                            value={threshold}
                            onChange={setThreshold}
                            marks={{0: '0', 0.5: '0.5', 1: '1'}}
                        />
                    </div>
                    <div>
                        <div style={{fontSize: 12, color: '#666', marginBottom: 4}}>检索策略</div>
                        <Select
                            value={strategy}
                            onChange={setStrategy}
                            style={{width: '100%'}}
                            options={[
                                {value: 'VECTOR', label: '向量检索'},
                                {value: 'BM25', label: 'BM25 关键词'},
                                {value: 'HYBRID', label: '混合检索'}
                            ]}
                        />
                    </div>
                    <div style={{marginTop: 12}}>
                        <div style={{fontSize: 12, color: '#666', marginBottom: 4}}>
                            <Space>
                                启用 Rerank 重排序
                                <InfoCircleOutlined title="使用 Rerank 模型对检索结果进行二次排序，提高检索精度"/>
                            </Space>
                        </div>
                        <Switch
                            checked={enableRerank}
                            onChange={setEnableRerank}
                            disabled={rerankModels.length === 0}
                        />
                        {enableRerank && rerankModels.length > 0 && (
                            <Select
                                allowClear
                                placeholder="选择 Rerank 模型"
                                value={selectedRerankModelId}
                                onChange={setSelectedRerankModelId}
                                style={{width: '100%', marginTop: 8}}
                                options={rerankModels.map(m => ({
                                    value: m.id,
                                    label: m.name
                                }))}
                            />
                        )}
                        {rerankModels.length === 0 && (
                            <div style={{fontSize: 12, color: '#999', marginTop: 4}}>
                                请先在模型管理中添加 Rerank 模型
                            </div>
                        )}
                    </div>
                </div>
                <Divider style={{margin: '12px 0'}}/>
                <div style={{padding: 16}}>
                    <div style={{marginBottom: 12}}>
                        <span style={{fontWeight: 500}}>对话模型</span>
                    </div>
                    <Select
                        allowClear
                        placeholder="选择模型（可选）"
                        value={selectedModelId}
                        onChange={setSelectedModelId}
                        style={{width: '100%'}}
                        options={models.map(m => ({
                            value: m.id,
                            label: m.isDefault ? `${m.name} (默认)` : m.name
                        }))}
                    />
                </div>
                <Divider style={{margin: '12px 0'}}/>
                <div style={{padding: 16}}>
                    <div style={{marginBottom: 12}}>
                        <span style={{fontWeight: 500}}>检索模型</span>
                    </div>
                    <Select
                        allowClear
                        placeholder="选择 Embedding 模型"
                        value={selectedEmbeddingModelId}
                        onChange={setSelectedEmbeddingModelId}
                        style={{width: '100%'}}
                        options={embeddingModels.map(m => ({
                            value: m.id,
                            label: m.isDefaultEmbedding ? `${m.name} (默认)` : m.name
                        }))}
                    />
                </div>
                <Divider style={{margin: '12px 0'}}/>
                <div style={{padding: 16}}>
                    <div style={{marginBottom: 12}}>
                        <span style={{fontWeight: 500}}>提示词模板</span>
                    </div>
                    <Select
                        allowClear
                        placeholder="选择模板（可选）"
                        value={selectedPromptId}
                        onChange={setSelectedPromptId}
                        style={{width: '100%'}}
                        options={prompts.map(p => ({
                            value: p.id,
                            label: p.name
                        }))}
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
                    <Space direction="vertical" style={{width: '100%'}}>
                        <Button icon={<PlusOutlined/>} onClick={handleNewConversation} block type="primary">
                            新对话
                        </Button>
                        {messages.length > 0 && (
                            <Button icon={<ClearOutlined/>} onClick={handleClear} block>
                                清空对话
                            </Button>
                        )}
                    </Space>
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