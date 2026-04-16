import React, {useEffect, useState} from 'react'
import type {UploadProps} from 'antd'
import {
    Alert,
    Badge,
    Button,
    Card,
    Col,
    Descriptions,
    Divider,
    Form,
    Input,
    InputNumber,
    message,
    Modal,
    Popconfirm,
    Progress,
    Radio,
    Row,
    Select,
    Space,
    Statistic,
    Table,
    Tag,
    Tooltip,
    Upload
} from 'antd'
import {
    ArrowLeftOutlined,
    DeleteOutlined,
    EditOutlined,
    EyeOutlined,
    FileTextOutlined,
    FolderOutlined,
    PlayCircleOutlined,
    PlusOutlined,
    QuestionCircleOutlined,
    ReloadOutlined,
    StopOutlined,
    UploadOutlined
} from '@ant-design/icons'
import type {ColumnsType} from 'antd/es/table'
import {
    cancelReindex,
    createKnowledgeBase,
    deleteKnowledgeBase,
    getReindexProgress,
    listAllKnowledgeBases,
    startReindex,
    updateKnowledgeBase
} from '@/api/knowledgeBase'
import {
    deleteDocument,
    getDocumentChunks,
    getSupportedTypes,
    listDocuments,
    startEmbedding,
    uploadDocument
} from '@/api/documents'
import {listModels} from '@/api/models'
import {KnowledgeBase, KnowledgeBaseRequest, ReindexProgressResponse} from '@/types/knowledgeBase'
import {Document, DocumentChunk, DocumentStatus, SupportedType} from '@/types/document'
import {ModelConfig} from '@/types/model'

const statusConfig: Record<DocumentStatus, {
    status: 'success' | 'processing' | 'error' | 'warning' | undefined,
    text: string
}> = {
    PROCESSING: {status: 'processing', text: '正在提取'},
    SEMANTIC_PROCESSING: {status: 'processing', text: '语义切分中'},
    CHUNKED: {status: 'warning', text: '已分块'},
    EMBEDDING: {status: 'processing', text: '正在向量化'},
    COMPLETED: {status: 'success', text: '已完成'},
    FAILED: {status: 'error', text: '失败'},
    DELETED: {status: undefined, text: '已删除'}
}

const KnowledgeBasesPage: React.FC = () => {
    // 知识库列表状态
    const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBase[]>([])
    const [embeddingModels, setEmbeddingModels] = useState<ModelConfig[]>([])
    const [loading, setLoading] = useState(false)

    // 知识库详情状态
    const [selectedKb, setSelectedKb] = useState<KnowledgeBase | null>(null)
    const [documents, setDocuments] = useState<Document[]>([])
    const [chunks, setChunks] = useState<DocumentChunk[]>([])
    const [documentsLoading, setDocumentsLoading] = useState(false)
    const [supportedTypes, setSupportedTypes] = useState<SupportedType[]>([])

    // Modal 状态
    const [kbModalVisible, setKbModalVisible] = useState(false)
    const [editingKb, setEditingKb] = useState<KnowledgeBase | null>(null)
    const [uploadModalVisible, setUploadModalVisible] = useState(false)
    const [uploadFile, setUploadFile] = useState<File | null>(null)
    const [uploading, setUploading] = useState(false)
    const [embedModalVisible, setEmbedModalVisible] = useState(false)
    const [embedDocumentId, setEmbedDocumentId] = useState<string | null>(null)
    const [chunkModalVisible, setChunkModalVisible] = useState(false)
    const [selectedChunk, setSelectedChunk] = useState<DocumentChunk | null>(null)
    const [selectedDocument, setSelectedDocument] = useState<Document | null>(null)

    // 重索引状态
    const [reindexModalVisible, setReindexModalVisible] = useState(false)
    const [reindexModelId, setReindexModelId] = useState<string | null>(null)
    const [reindexProgress, setReindexProgress] = useState<ReindexProgressResponse | null>(null)

    const [kbForm] = Form.useForm()
    const [uploadForm] = Form.useForm()

    useEffect(() => {
        fetchKnowledgeBases()
        fetchModels()
        fetchSupportedTypes()
    }, [])

    // 文档处理进度轮询（支持 PROCESSING 和 SEMANTIC_PROCESSING）
    useEffect(() => {
        const pollingDocuments = documents.filter(d => ['PROCESSING', 'SEMANTIC_PROCESSING'].includes(d.status))
        if (pollingDocuments.length === 0) return

        const interval = setInterval(async () => {
            // 重新获取文档列表以更新状态
            fetchDocuments(selectedKb?.id)
        }, 3000)

        return () => clearInterval(interval)
    }, [documents, selectedKb])

    // 重索引进度轮询
    useEffect(() => {
        if (!selectedKb || reindexProgress?.status !== 'IN_PROGRESS') return

        const interval = setInterval(async () => {
            try {
                const res = await getReindexProgress(selectedKb.id)
                if (res.success && res.data) {
                    setReindexProgress(res.data)
                    // 如果完成或失败，停止轮询并刷新知识库信息
                    if (['COMPLETED', 'FAILED'].includes(res.data.status)) {
                        fetchKnowledgeBases()
                        fetchDocuments(selectedKb.id)
                    }
                }
            } catch {
                // ignore
            }
        }, 2000)

        return () => clearInterval(interval)
    }, [selectedKb, reindexProgress?.status])

    const fetchKnowledgeBases = async () => {
        setLoading(true)
        try {
            const res = await listAllKnowledgeBases()
            if (res.success) {
                setKnowledgeBases(res.data || [])
            }
        } finally {
            setLoading(false)
        }
    }

    const fetchModels = async () => {
        try {
            const res = await listModels({isActive: true})
            if (res.success) {
                const embedModels = res.data.filter(m => m.modelType === 'EMBEDDING')
                setEmbeddingModels(embedModels)
            }
        } catch {
            // ignore
        }
    }

    const fetchSupportedTypes = async () => {
        try {
            const res = await getSupportedTypes()
            if (res.success) {
                setSupportedTypes(res.data || [])
            }
        } catch {
            // ignore
        }
    }

    const fetchDocuments = async (knowledgeBaseId?: string) => {
        setDocumentsLoading(true)
        try {
            const res = await listDocuments()
            if (res.success) {
                const allDocs = res.data.content || []
                // 按知识库筛选
                if (knowledgeBaseId) {
                    setDocuments(allDocs.filter(d => d.knowledgeBaseId === knowledgeBaseId))
                } else {
                    setDocuments(allDocs)
                }
            }
        } finally {
            setDocumentsLoading(false)
        }
    }

    const fetchChunks = async (documentId: string) => {
        try {
            const res = await getDocumentChunks(documentId)
            if (res.success) {
                setChunks(Array.isArray(res.data) ? res.data : [])
            }
        } catch {
            setChunks([])
        }
    }

    // 进入知识库详情
    const handleEnterKb = async (kb: KnowledgeBase) => {
        setSelectedKb(kb)
        setDocuments([])
        setChunks([])
        setSelectedDocument(null)
        fetchDocuments(kb.id)
    }

    // 返回知识库列表
    const handleBackToList = () => {
        setSelectedKb(null)
        setDocuments([])
        setChunks([])
        setSelectedDocument(null)
        fetchKnowledgeBases()
    }

    // 知识库 CRUD
    const handleCreateKb = () => {
        setEditingKb(null)
        kbForm.resetFields()
        setKbModalVisible(true)
    }

    const handleEditKb = (kb: KnowledgeBase) => {
        setEditingKb(kb)
        kbForm.setFieldsValue({
            name: kb.name,
            description: kb.description,
            defaultEmbeddingModelId: kb.defaultEmbeddingModelId
        })
        setKbModalVisible(true)
    }

    const handleDeleteKb = async (id: string) => {
        try {
            await deleteKnowledgeBase(id)
            message.success('删除成功')
            fetchKnowledgeBases()
        } catch (err: any) {
            message.error(err.response?.data?.message || '删除失败')
        }
    }

    const handleKbSubmit = async () => {
        try {
            const values = await kbForm.validateFields()
            const request: KnowledgeBaseRequest = {
                name: values.name,
                description: values.description,
                defaultEmbeddingModelId: values.defaultEmbeddingModelId
            }
            if (editingKb) {
                await updateKnowledgeBase(editingKb.id, request)
                message.success('更新成功')
            } else {
                await createKnowledgeBase(request)
                message.success('创建成功')
            }
            setKbModalVisible(false)
            fetchKnowledgeBases()
        } catch (err: any) {
            message.error(err.response?.data?.message || '操作失败')
        }
    }

    // 文档操作
    const handleDeleteDoc = async (id: string) => {
        try {
            await deleteDocument(id)
            message.success('删除成功')
            fetchDocuments(selectedKb?.id)
            if (selectedDocument?.id === id) {
                setSelectedDocument(null)
                setChunks([])
            }
        } catch {
            message.error('删除失败')
        }
    }

    const handleViewChunks = (doc: Document) => {
        setSelectedDocument(doc)
        if (['COMPLETED', 'CHUNKED'].includes(doc.status)) {
            fetchChunks(doc.id)
        } else {
            setChunks([])
        }
    }

    const handleStartEmbedding = async () => {
        if (!embedDocumentId || !selectedKb?.defaultEmbeddingModelId) {
            message.error('知识库未设置默认 Embedding 模型')
            return
        }
        try {
            // 使用知识库默认模型
            const res = await startEmbedding(embedDocumentId, selectedKb.defaultEmbeddingModelId)
            if (res.success) {
                message.success('开始计算向量')
                setEmbedModalVisible(false)
                fetchDocuments(selectedKb?.id)
            }
        } catch (e: unknown) {
            const error = e as { response?: { data?: { message?: string } } }
            message.error(error.response?.data?.message || '启动失败')
        }
    }

    const handleUploadSubmit = async () => {
        if (!uploadFile) {
            message.error('请选择文件')
            return
        }
        const values = await uploadForm.validateFields()
        setUploading(true)
        try {
            const res = await uploadDocument(uploadFile, {
                name: values.name,
                knowledgeBaseId: selectedKb?.id, // 自动关联当前知识库
                chunkStrategy: values.chunkStrategy,
                chunkSize: values.chunkSize,
                chunkOverlap: values.chunkOverlap,
                embeddingModelId: selectedKb?.defaultEmbeddingModelId // 使用知识库默认模型
            })
            if (res.success) {
                message.success(`${uploadFile.name} 上传成功`)
                setUploadModalVisible(false)
                uploadForm.resetFields()
                setUploadFile(null)
                fetchDocuments(selectedKb?.id)
            }
        } catch (e: unknown) {
            const error = e as { response?: { data?: { message?: string } } }
            message.error(error.response?.data?.message || '上传失败')
        } finally {
            setUploading(false)
        }
    }

    // 重索引操作
    const handleStartReindex = async () => {
        if (!selectedKb || !reindexModelId) return
        try {
            const res = await startReindex(selectedKb.id, {newEmbeddingModelId: reindexModelId})
            if (res.success) {
                message.success('开始重索引')
                setReindexProgress(res.data)
                setReindexModalVisible(false)
            }
        } catch (e: unknown) {
            const error = e as { response?: { data?: { message?: string } } }
            message.error(error.response?.data?.message || '启动失败')
        }
    }

    const handleCancelReindex = async () => {
        if (!selectedKb) return
        try {
            await cancelReindex(selectedKb.id)
            message.success('已取消重索引')
            setReindexProgress(null)
        } catch (e: unknown) {
            const error = e as { response?: { data?: { message?: string } } }
            message.error(error.response?.data?.message || '取消失败')
        }
    }

    const handleOpenReindexModal = async () => {
        if (!selectedKb) return
        // 获取当前进度
        try {
            const res = await getReindexProgress(selectedKb.id)
            if (res.success && res.data) {
                setReindexProgress(res.data)
                if (res.data.status === 'IN_PROGRESS') {
                    // 已经在进行中，不打开 Modal
                    message.info('重索引正在进行中')
                    return
                }
            }
        } catch {
            // ignore
        }
        setReindexModelId(null)
        setReindexModalVisible(true)
    }

    // 知识库列表表格列
    const kbColumns: ColumnsType<KnowledgeBase> = [
        {
            title: '知识库名称',
            dataIndex: 'name',
            key: 'name',
            render: (text: string) => (
                <Space>
                    <FolderOutlined style={{color: '#1890ff'}}/>
                    <span style={{fontWeight: 500}}>{text}</span>
                </Space>
            )
        },
        {title: '描述', dataIndex: 'description', key: 'description', ellipsis: true, render: (t) => t || '-'},
        {title: '默认模型', dataIndex: 'defaultEmbeddingModelName', key: 'model', render: (t) => t || '-'},
        {title: '文档数', dataIndex: 'documentCount', key: 'docCount', width: 80},
        {title: '分块数', dataIndex: 'chunkCount', key: 'chunkCount', width: 80},
        {
            title: '创建时间',
            dataIndex: 'createdAt',
            key: 'createdAt',
            width: 160,
            render: (d) => new Date(d).toLocaleString()
        },
        {
            title: '操作',
            key: 'action',
            width: 200,
            render: (_, record) => (
                <Space>
                    <Button type="link" onClick={() => handleEnterKb(record)}>
                        进入
                    </Button>
                    <Button type="link" icon={<EditOutlined/>} onClick={() => handleEditKb(record)}>
                        编辑
                    </Button>
                    <Popconfirm title="确定删除?" onConfirm={() => handleDeleteKb(record.id)}>
                        <Button type="link" danger icon={<DeleteOutlined/>}>删除</Button>
                    </Popconfirm>
                </Space>
            )
        }
    ]

    // 文档列表表格列
    const docColumns: ColumnsType<Document> = [
        {
            title: '文件名',
            dataIndex: 'name',
            key: 'name',
            render: (name: string) => <Space><FileTextOutlined/>{name}</Space>
        },
        {
            title: '状态',
            dataIndex: 'status',
            key: 'status',
            render: (status: DocumentStatus, record) => {
                const config = statusConfig[status] || {status: 'default', text: status}
                return (
                    <Space direction="vertical" size="small">
                        <Badge status={config.status} text={config.text}/>
                        {status === 'SEMANTIC_PROCESSING' && record.semanticProgressTotal && record.semanticProgressTotal > 0 && (
                            <Progress
                                percent={Math.round(((record.semanticProgressCurrent || 0) / record.semanticProgressTotal) * 100)}
                                size="small" showInfo={false}/>
                        )}
                    </Space>
                )
            }
        },
        {title: '分块数', dataIndex: 'chunksCreated', key: 'chunks'},
        {
            title: '分块策略', dataIndex: 'chunkStrategy', key: 'strategy', render: (s) => {
                const map: Record<string, string> = {
                    FIXED_SIZE: '固定大小',
                    PARAGRAPH: '按段落',
                    SENTENCE: '按句子',
                    RECURSIVE: '递归',
                    SEMANTIC: '语义'
                }
                return map[s || 'FIXED_SIZE'] || s
            }
        },
        {
            title: '创建时间',
            dataIndex: 'createdAt',
            key: 'createdAt',
            width: 160,
            render: (d) => new Date(d).toLocaleString()
        },
        {
            title: '操作',
            key: 'action',
            width: 200,
            render: (_, record) => (
                <Space>
                    {record.status === 'CHUNKED' && (
                        <Button type="link" icon={<PlayCircleOutlined/>} onClick={() => {
                            setEmbedDocumentId(record.id);
                            setEmbedModalVisible(true);
                        }}>
                            向量化
                        </Button>
                    )}
                    {['COMPLETED', 'CHUNKED'].includes(record.status) && (
                        <Button type="link" icon={<EyeOutlined/>} onClick={() => handleViewChunks(record)}>
                            分块
                        </Button>
                    )}
                    <Popconfirm title="确定删除?" onConfirm={() => handleDeleteDoc(record.id)}>
                        <Button type="link" danger icon={<DeleteOutlined/>}>删除</Button>
                    </Popconfirm>
                </Space>
            )
        }
    ]

    // 分块列表表格列
    const chunkColumns: ColumnsType<DocumentChunk> = [
        {title: '序号', dataIndex: 'chunkIndex', key: 'index', width: 60},
        {
            title: '内容',
            dataIndex: 'content',
            key: 'content',
            ellipsis: true,
            render: (content: string, record) => (
                <Space direction="vertical">
                    <div style={{maxWidth: 400, whiteSpace: 'pre-wrap', overflow: 'hidden'}}>
                        {content.substring(0, 100)}...
                    </div>
                    <Button type="link" size="small" onClick={() => {
                        setSelectedChunk(record);
                        setChunkModalVisible(true);
                    }}>
                        查看完整
                    </Button>
                </Space>
            )
        },
        {
            title: '创建时间',
            dataIndex: 'createdAt',
            key: 'createdAt',
            width: 160,
            render: (d) => new Date(d).toLocaleString()
        }
    ]

    const uploadProps: UploadProps = {
        name: 'file',
        showUploadList: false,
        beforeUpload: (file) => {
            setUploadFile(file)
            uploadForm.setFieldValue('name', file.name)
            setUploadModalVisible(true)
            return false
        },
        accept: '.pdf,.docx,.txt,.md,.markdown,.csv'
    }

    // 知识库列表视图
    const renderKbList = () => (
        <div>
            <div style={{marginBottom: 16, display: 'flex', justifyContent: 'space-between'}}>
                <div>
                    <h2 style={{marginBottom: 0}}>知识库管理</h2>
                    <p style={{color: '#666', marginTop: 4}}>组织和管理文档集合，点击"进入"查看知识库下的文档</p>
                </div>
                <Button type="primary" icon={<PlusOutlined/>} onClick={handleCreateKb}>新建知识库</Button>
            </div>

            <Row gutter={[16, 16]} style={{marginBottom: 16}}>
                {knowledgeBases.map(kb => (
                    <Col span={6} key={kb.id}>
                        <Card hoverable onClick={() => handleEnterKb(kb)}>
                            <Space direction="vertical" size="small" style={{width: '100%'}}>
                                <Space>
                                    <FolderOutlined style={{fontSize: 20, color: '#1890ff'}}/>
                                    <span style={{fontWeight: 500}}>{kb.name}</span>
                                </Space>
                                <div
                                    style={{color: '#666', fontSize: 12, overflow: 'hidden', textOverflow: 'ellipsis'}}>
                                    {kb.description || '暂无描述'}
                                </div>
                                <Space split={<span style={{color: '#d9d9d9'}}>|</span>}>
                                    <span><FileTextOutlined/> {kb.documentCount} 文档</span>
                                    <span>{kb.chunkCount} 分块</span>
                                </Space>
                            </Space>
                        </Card>
                    </Col>
                ))}
            </Row>

            <Table columns={kbColumns} dataSource={knowledgeBases} rowKey="id" loading={loading}
                   scroll={{x: 'max-content'}}/>
        </div>
    )

    // 知识库详情视图
    const renderKbDetail = () => (
        <div>
            <div style={{marginBottom: 16, display: 'flex', justifyContent: 'space-between'}}>
                <Space>
                    <Button icon={<ArrowLeftOutlined/>} onClick={handleBackToList}>返回</Button>
                    <Divider type="vertical"/>
                    <FolderOutlined style={{fontSize: 20, color: '#1890ff'}}/>
                    <h2 style={{marginBottom: 0}}>{selectedKb?.name}</h2>
                </Space>
                <Space>
                    <Upload {...uploadProps}>
                        <Button icon={<UploadOutlined/>} type="primary">上传文档</Button>
                    </Upload>
                    <Button icon={<ReloadOutlined/>} onClick={handleOpenReindexModal}>
                        重索引
                    </Button>
                    <Button icon={<EditOutlined/>} onClick={() => handleEditKb(selectedKb!)}>编辑知识库</Button>
                </Space>
            </div>

            {/* 重索引进度条 */}
            {reindexProgress && reindexProgress.status === 'IN_PROGRESS' && (
                <Card style={{marginBottom: 16, background: '#e6f7ff'}}>
                    <Space direction="vertical" style={{width: '100%'}}>
                        <Space>
                            <Badge status="processing" text="正在重索引"/>
                            <span>目标模型: {embeddingModels.find(m => m.id === reindexProgress.newEmbeddingModelId)?.name || '-'}</span>
                            <Button type="link" danger icon={<StopOutlined/>} onClick={handleCancelReindex}>
                                取消
                            </Button>
                        </Space>
                        <Progress
                            percent={reindexProgress.percentage}
                            status="active"
                            format={() => `${reindexProgress.current} / ${reindexProgress.total}`}
                        />
                    </Space>
                </Card>
            )}

            {/* 重索引失败提示 */}
            {reindexProgress && reindexProgress.status === 'FAILED' && (
                <Card style={{marginBottom: 16, background: '#fff2f0'}}>
                    <Space>
                        <Badge status="error" text="重索引失败"/>
                        <span style={{color: '#cf1322'}}>{reindexProgress.errorMessage}</span>
                        <Button type="link" onClick={() => setReindexProgress(null)}>
                            关闭
                        </Button>
                    </Space>
                </Card>
            )}

            <Card style={{marginBottom: 16}}>
                <Descriptions column={4} size="small">
                    <Descriptions.Item label="描述">{selectedKb?.description || '-'}</Descriptions.Item>
                    <Descriptions.Item
                        label="默认模型">{selectedKb?.defaultEmbeddingModelName || '-'}</Descriptions.Item>
                    <Descriptions.Item label="文档数">
                        <Statistic value={selectedKb?.documentCount || 0} valueStyle={{fontSize: 14}}/>
                    </Descriptions.Item>
                    <Descriptions.Item label="分块数">
                        <Statistic value={selectedKb?.chunkCount || 0} valueStyle={{fontSize: 14}}/>
                    </Descriptions.Item>
                </Descriptions>
            </Card>

            {selectedDocument ? (
                <Card>
                    <Button icon={<ArrowLeftOutlined/>} onClick={() => {
                        setSelectedDocument(null);
                        setChunks([]);
                    }} style={{marginBottom: 16}}>
                        返回文档列表
                    </Button>
                    {['COMPLETED', 'CHUNKED'].includes(selectedDocument.status) ? (
                        <>
                            <Descriptions column={4} size="small" style={{marginBottom: 16}}>
                                <Descriptions.Item label="文件名">{selectedDocument.name}</Descriptions.Item>
                                <Descriptions.Item label="状态">
                                    <Badge status={statusConfig[selectedDocument.status]?.status}
                                           text={statusConfig[selectedDocument.status]?.text}/>
                                </Descriptions.Item>
                                <Descriptions.Item label="分块数">{selectedDocument.chunksCreated}</Descriptions.Item>
                                <Descriptions.Item
                                    label="策略">{selectedDocument.chunkStrategy || 'FIXED_SIZE'}</Descriptions.Item>
                            </Descriptions>
                            <Table columns={chunkColumns} dataSource={chunks} rowKey="id" loading={false}
                                   pagination={{pageSize: 10}}/>
                        </>
                    ) : (
                        <div style={{textAlign: 'center', padding: 40, color: '#666'}}>
                            文档正在处理中，请稍后再查看分块
                        </div>
                    )}
                </Card>
            ) : (
                <Table
                    columns={docColumns}
                    dataSource={documents}
                    rowKey="id"
                    loading={documentsLoading}
                    scroll={{x: 'max-content'}}
                />
            )}
        </div>
    )

    return (
        <div>
            {selectedKb ? renderKbDetail() : renderKbList()}

            {/* 知识库编辑 Modal */}
            <Modal
                title={editingKb ? '编辑知识库' : '新建知识库'}
                open={kbModalVisible}
                onOk={handleKbSubmit}
                onCancel={() => setKbModalVisible(false)}
                width={500}
            >
                <Form form={kbForm} layout="vertical">
                    <Form.Item name="name" label="名称" rules={[{required: true}]}>
                        <Input placeholder="如：技术文档库"/>
                    </Form.Item>
                    <Form.Item name="description" label="描述">
                        <Input.TextArea rows={3}/>
                    </Form.Item>
                    <Form.Item
                        name="defaultEmbeddingModelId"
                        label="默认 Embedding 模型"
                        rules={[{required: true, message: '请选择 Embedding 模型'}]}
                    >
                        <Select
                            placeholder="选择默认模型（必填）"
                            options={embeddingModels.map(m => ({
                                value: m.id,
                                label: m.isDefaultEmbedding ? `${m.name} (系统默认)` : m.name
                            }))}
                        />
                    </Form.Item>
                    <Alert
                        message="文档上传时将使用此模型进行向量计算。如需更换模型，可在编辑时触发重索引。"
                        type="info"
                        showIcon
                        style={{marginTop: 8}}
                    />
                </Form>
            </Modal>

            {/* 上传配置 Modal */}
            <Modal
                title="上传文档"
                open={uploadModalVisible}
                onCancel={() => {
                    setUploadModalVisible(false);
                    uploadForm.resetFields();
                    setUploadFile(null);
                }}
                onOk={handleUploadSubmit}
                confirmLoading={uploading}
                width={600}
            >
                <Form form={uploadForm} layout="vertical"
                      initialValues={{chunkStrategy: 'FIXED_SIZE', chunkSize: 500, chunkOverlap: 100}}>
                    <Form.Item label="文件">
                        <Space>
                            <FileTextOutlined/>
                            <span>{uploadFile?.name}</span>
                            <Tag>{supportedTypes.find(t => uploadFile?.name.toLowerCase().endsWith(t.extension))?.displayName || '未知'}</Tag>
                        </Space>
                    </Form.Item>
                    <Form.Item name="name" label="文档名称">
                        <Input placeholder="可选，默认使用文件名"/>
                    </Form.Item>
                    <Form.Item name="chunkStrategy" label={
                        <Space>
                            <span>分块策略</span>
                            <Tooltip
                                title="固定大小：按字符数分块；按段落：双换行分隔；按句子：句号分隔；递归：段落→句子→固定；语义：Embedding API 计算相似度">
                                <QuestionCircleOutlined style={{color: '#1890ff'}}/>
                            </Tooltip>
                        </Space>
                    }>
                        <Radio.Group>
                            <Radio.Button value="FIXED_SIZE">固定大小</Radio.Button>
                            <Radio.Button value="PARAGRAPH">按段落</Radio.Button>
                            <Radio.Button value="SENTENCE">按句子</Radio.Button>
                            <Radio.Button value="RECURSIVE">递归</Radio.Button>
                            <Radio.Button value="SEMANTIC">语义</Radio.Button>
                        </Radio.Group>
                    </Form.Item>
                    <Form.Item shouldUpdate={(prev, curr) => prev.chunkStrategy !== curr.chunkStrategy}>
                        {({getFieldValue}) => {
                            const strategy = getFieldValue('chunkStrategy')
                            return (
                                <>
                                    {['FIXED_SIZE', 'SENTENCE', 'RECURSIVE'].includes(strategy) && (
                                        <Form.Item name="chunkSize" label="分块大小">
                                            <InputNumber min={100} max={2000} step={100}/>
                                        </Form.Item>
                                    )}
                                    {['FIXED_SIZE', 'SENTENCE', 'RECURSIVE', 'PARAGRAPH'].includes(strategy) && (
                                        <Form.Item name="chunkOverlap" label="重叠大小">
                                            <InputNumber min={0} max={500} step={50}/>
                                        </Form.Item>
                                    )}
                                    {strategy === 'SEMANTIC' && (
                                        <Form.Item label="Embedding 模型">
                                            <Input
                                                disabled
                                                value={selectedKb?.defaultEmbeddingModelName || embeddingModels.find(m => m.id === selectedKb?.defaultEmbeddingModelId)?.name || '未设置'}
                                                style={{background: '#f5f5f5', color: '#666'}}
                                            />
                                            <div style={{fontSize: 12, color: '#999', marginTop: 4}}>
                                                文档使用知识库默认模型，如需更换请在知识库设置中修改
                                            </div>
                                        </Form.Item>
                                    )}
                                </>
                            )
                        }}
                    </Form.Item>
                </Form>
            </Modal>

            {/* Embedding Modal - 使用知识库默认模型 */}
            <Modal
                title="开始向量化"
                open={embedModalVisible}
                onCancel={() => setEmbedModalVisible(false)}
                onOk={handleStartEmbedding}
                okText="开始向量化"
                width={400}
            >
                <div style={{marginBottom: 16}}>
                    <span style={{color: '#666'}}>将使用知识库默认 Embedding 模型进行向量化：</span>
                </div>
                <Input
                    disabled
                    style={{width: '100%', background: '#f5f5f5', color: '#333'}}
                    value={selectedKb?.defaultEmbeddingModelName || embeddingModels.find(m => m.id === selectedKb?.defaultEmbeddingModelId)?.name || '未设置'}
                />
                <div style={{fontSize: 12, color: '#999', marginTop: 8}}>
                    文档向量将存储到 {embeddingModels.find(m => m.id === selectedKb?.defaultEmbeddingModelId)?.embeddingDimension || '未知'} 维向量表
                </div>
            </Modal>

            {/* 分块内容 Modal */}
            <Modal
                title={`分块 #${selectedChunk?.chunkIndex}`}
                open={chunkModalVisible}
                onCancel={() => setChunkModalVisible(false)}
                footer={[<Button onClick={() => setChunkModalVisible(false)}>关闭</Button>]}
                width={800}
            >
                <div style={{
                    whiteSpace: 'pre-wrap',
                    maxHeight: 500,
                    overflow: 'auto',
                    padding: 16,
                    background: '#f5f5f5'
                }}>
                    {selectedChunk?.content}
                </div>
            </Modal>

            {/* 重索引 Modal */}
            <Modal
                title="切换 Embedding 模型并重索引"
                open={reindexModalVisible}
                onCancel={() => setReindexModalVisible(false)}
                onOk={handleStartReindex}
                okText="开始重索引"
                okButtonProps={{disabled: !reindexModelId}}
                width={500}
            >
                <div style={{marginBottom: 16}}>
                    <Alert
                        message="注意：重索引将重新计算所有分块的向量，可能需要较长时间"
                        type="warning"
                        showIcon
                    />
                </div>
                <div style={{marginBottom: 16}}>
                    <span>当前模型: {selectedKb?.defaultEmbeddingModelName || embeddingModels.find(m => m.isDefaultEmbedding)?.name || '-'}</span>
                </div>
                <Form layout="vertical">
                    <Form.Item label="选择新的 Embedding 模型">
                        <Select
                            style={{width: '100%'}}
                            value={reindexModelId}
                            onChange={setReindexModelId}
                            options={embeddingModels.map(m => ({
                                value: m.id,
                                label: m.isDefaultEmbedding ? `${m.name} (系统默认)` : m.name,
                                disabled: m.id === selectedKb?.defaultEmbeddingModelId
                            }))}
                            placeholder="选择新模型"
                        />
                    </Form.Item>
                </Form>
                <div style={{color: '#666', fontSize: 12}}>
                    重索引期间，知识库的文档检索可能会受到影响。完成后将自动切换到新模型。
                </div>
            </Modal>
        </div>
    )
}

export default KnowledgeBasesPage