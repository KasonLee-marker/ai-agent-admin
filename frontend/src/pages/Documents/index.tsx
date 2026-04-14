import React, {useEffect, useState} from 'react'
import type {UploadProps} from 'antd'
import {
    Badge,
    Button,
    Card,
    Descriptions,
    Form,
    Input,
    InputNumber,
    message,
    Modal,
    Popconfirm,
    Progress,
    Radio,
    Select,
    Space,
    Table,
    Tabs,
    Tag,
    Upload
} from 'antd'
import {
    ArrowLeftOutlined,
    DeleteOutlined,
    EyeOutlined,
    FileTextOutlined,
    FullscreenOutlined,
    InfoCircleOutlined,
    PlayCircleOutlined,
    UploadOutlined
} from '@ant-design/icons'
import type {ColumnsType} from 'antd/es/table'
import {
    deleteDocument,
    getDocumentChunks,
    getSupportedTypes,
    listDocuments,
    startEmbedding,
    uploadDocument
} from '@/api/documents'
import {listModels} from '@/api/models'
import {Document, DocumentChunk, DocumentStatus, SupportedType} from '@/types/document'
import {ModelConfig} from '@/types/model'

const DocumentPage: React.FC = () => {
    const [documents, setDocuments] = useState<Document[]>([])
    const [chunks, setChunks] = useState<DocumentChunk[]>([])
    const [embeddingModels, setEmbeddingModels] = useState<ModelConfig[]>([])
    const [loading, setLoading] = useState(false)
    const [chunksLoading, setChunksLoading] = useState(false)
    const [selectedDocument, setSelectedDocument] = useState<Document | null>(null)
    const [supportedTypes, setSupportedTypes] = useState<SupportedType[]>([])
    const [uploading, setUploading] = useState(false)
    const [chunkModalVisible, setChunkModalVisible] = useState(false)
    const [selectedChunk, setSelectedChunk] = useState<DocumentChunk | null>(null)
    const [uploadModalVisible, setUploadModalVisible] = useState(false)
    const [embedModalVisible, setEmbedModalVisible] = useState(false)
    const [embedDocumentId, setEmbedDocumentId] = useState<string | null>(null)
    const [selectedEmbedModelId, setSelectedEmbedModelId] = useState<string | null>(null)
    const [embedLoading, setEmbedLoading] = useState(false)
    const [activeTab, setActiveTab] = useState<'list' | 'chunks'>('list')
    const [uploadForm] = Form.useForm()
    const [uploadFile, setUploadFile] = useState<File | null>(null)

    useEffect(() => {
        fetchDocuments()
        fetchSupportedTypes()
        fetchEmbeddingModels()
    }, [])

    const fetchDocuments = async () => {
        setLoading(true)
        try {
            const res = await listDocuments()
            if (res.success) {
                setDocuments(res.data.content || [])
            }
        } finally {
            setLoading(false)
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

    const fetchEmbeddingModels = async () => {
        try {
            const res = await listModels({isActive: true})
            if (res.success) {
                // 只显示 EMBEDDING 类型模型
                const embedModels = res.data.filter(m => m.modelType === 'EMBEDDING')
                setEmbeddingModels(embedModels)
                // 自动选择默认模型
                const defaultModel = embedModels.find(m => m.isDefaultEmbedding)
                if (defaultModel) {
                    setSelectedEmbedModelId(defaultModel.id)
                }
            }
        } catch {
            // ignore
        }
    }

    const fetchChunks = async (documentId: string) => {
        setChunksLoading(true)
        try {
            const res = await getDocumentChunks(documentId)
            if (res.success) {
                // API 返回的是数组，不是分页格式
                setChunks(Array.isArray(res.data) ? res.data : [])
            }
        } finally {
            setChunksLoading(false)
        }
    }

    const handleDelete = async (id: string) => {
        try {
            await deleteDocument(id)
            message.success('删除成功')
            fetchDocuments()
            if (selectedDocument?.id === id) {
                setSelectedDocument(null)
                setChunks([])
            }
        } catch {
            message.error('删除失败')
        }
    }

    const handleOpenEmbedModal = (id: string) => {
        setEmbedDocumentId(id)
        setEmbedModalVisible(true)
    }

    const handleStartEmbedding = async () => {
        if (!embedDocumentId) return
        setEmbedLoading(true)
        try {
            const res = await startEmbedding(embedDocumentId, selectedEmbedModelId || undefined)
            if (res.success) {
                message.success('开始计算向量，请稍后查看进度')
                setEmbedModalVisible(false)
                setEmbedDocumentId(null)
                fetchDocuments()
            }
        } catch (e: unknown) {
            const error = e as { response?: { data?: { message?: string } } }
            message.error(error.response?.data?.message || '启动失败')
        } finally {
            setEmbedLoading(false)
        }
    }

    const handleSelectDocument = (record: Document) => {
        setSelectedDocument(record)
        // 只选中文档但不跳转 tab，也不加载分块
        setChunks([])
    }

    const handleViewChunks = (record: Document) => {
        setSelectedDocument(record)
        setActiveTab('chunks')
        if (['COMPLETED', 'CHUNKED'].includes(record.status)) {
            fetchChunks(record.id)
        } else {
            setChunks([])
        }
    }

    const handleBackToList = () => {
        setActiveTab('list')
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
                chunkStrategy: values.chunkStrategy,
                chunkSize: values.chunkSize,
                chunkOverlap: values.chunkOverlap
            })
            if (res.success) {
                message.success(`${uploadFile.name} 上传成功，正在处理...`)
                setUploadModalVisible(false)
                uploadForm.resetFields()
                setUploadFile(null)
                fetchDocuments()
            } else {
                message.error('上传失败')
            }
        } catch (e: unknown) {
            const error = e as { response?: { data?: { message?: string } } }
            message.error(error.response?.data?.message || '上传失败')
        } finally {
            setUploading(false)
        }
    }

    const statusConfig: Record<DocumentStatus, {
        status: 'success' | 'processing' | 'error' | 'warning' | undefined,
        text: string,
        color: string
    }> = {
        PROCESSING: {status: 'processing', text: '正在提取', color: 'blue'},
        CHUNKED: {status: 'warning', text: '已分块', color: 'orange'},
        EMBEDDING: {status: 'processing', text: '正在向量化', color: 'cyan'},
        COMPLETED: {status: 'success', text: '已完成', color: 'green'},
        FAILED: {status: 'error', text: '失败', color: 'red'},
        DELETED: {status: undefined, text: '已删除', color: 'default'}
    }

    const documentColumns: ColumnsType<Document> = [
        {
            title: '文件名',
            dataIndex: 'name',
            key: 'name',
            render: (name: string) => (
                <Space>
                    <FileTextOutlined/>
                    {name}
                </Space>
            )
        },
        {
            title: '类型',
            dataIndex: 'contentType',
            key: 'contentType',
            render: (type: string) => {
                const typeInfo = supportedTypes.find(t => t.contentType === type)
                return typeInfo?.displayName || type
            }
        },
        {
            title: '状态',
            dataIndex: 'status',
            key: 'status',
            render: (status: DocumentStatus, record) => {
                const config = statusConfig[status] || {status: 'default', text: status, color: 'default'}
                return (
                    <Space direction="vertical" size="small">
                        <Badge status={config.status} text={config.text}/>
                        {status === 'EMBEDDING' && record.chunksEmbedded && record.chunksCreated && (
                            <Progress
                                percent={Math.round((record.chunksEmbedded / record.chunksCreated) * 100)}
                                size="small"
                                showInfo={false}
                            />
                        )}
                    </Space>
                )
            }
        },
        {title: '分块数', dataIndex: 'chunksCreated', key: 'chunksCreated'},
        {
            title: '分块策略',
            dataIndex: 'chunkStrategy',
            key: 'chunkStrategy',
            render: (strategy: string) => strategy === 'PARAGRAPH' ? '按段落' : '固定大小'
        },
        {
            title: '创建时间',
            dataIndex: 'createdAt',
            key: 'createdAt',
            render: (date: string) => new Date(date).toLocaleString()
        },
        {
            title: '操作',
            key: 'action',
            render: (_, record) => (
                <Space>
                    {record.status === 'CHUNKED' && (
                        <Button type="link" icon={<PlayCircleOutlined/>}
                                onClick={() => handleOpenEmbedModal(record.id)}>
                            开始向量化
                        </Button>
                    )}
                    {['COMPLETED', 'CHUNKED'].includes(record.status) && (
                        <Button type="link" icon={<EyeOutlined/>}
                                onClick={() => handleViewChunks(record)}>
                            查看分块
                        </Button>
                    )}
                    <Popconfirm title="确定删除?" onConfirm={() => handleDelete(record.id)}>
                        <Button type="link" danger icon={<DeleteOutlined/>}>
                            删除
                        </Button>
                    </Popconfirm>
                </Space>
            )
        }
    ]

    const chunkColumns: ColumnsType<DocumentChunk> = [
        {title: '序号', dataIndex: 'chunkIndex', key: 'chunkIndex', width: 60},
        {
            title: '内容',
            dataIndex: 'content',
            key: 'content',
            ellipsis: true,
            render: (content: string, record) => (
                <Space direction="vertical" style={{width: '100%'}}>
                    <div style={{
                        maxWidth: 500,
                        whiteSpace: 'pre-wrap',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis'
                    }}>
                        {content.substring(0, 150)}...
                    </div>
                    <Button type="link" size="small" icon={<FullscreenOutlined/>}
                            onClick={() => {
                                setSelectedChunk(record)
                                setChunkModalVisible(true)
                            }}>
                        查看完整内容
                    </Button>
                </Space>
            )
        },
        {
            title: '创建时间',
            dataIndex: 'createdAt',
            key: 'createdAt',
            width: 180,
            render: (date: string) => new Date(date).toLocaleString()
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

    return (
        <div>
            <div style={{marginBottom: 16, display: 'flex', justifyContent: 'space-between'}}>
                <div>
                    <h2 style={{marginBottom: 0}}>文档管理</h2>
                    <p style={{color: '#666', marginTop: 4}}>
                        上传知识库文档用于 RAG 检索，支持分块策略选择，向量计算异步执行
                    </p>
                </div>
                <Upload {...uploadProps}>
                    <Button icon={<UploadOutlined/>} type="primary">
                        上传文档
                    </Button>
                </Upload>
            </div>

            <Card style={{marginBottom: 16}}>
                <Space>
                    <span>支持的文件类型:</span>
                    {supportedTypes.map(type => (
                        <Tag key={type.extension} color="blue">{type.displayName}</Tag>
                    ))}
                </Space>
            </Card>

            <Tabs activeKey={activeTab} onChange={(key) => setActiveTab(key as 'list' | 'chunks')} items={[
                {
                    key: 'list',
                    label: '文档列表',
                    children: (
                        <Table
                            columns={documentColumns}
                            dataSource={documents}
                            rowKey="id"
                            loading={loading}
                            onRow={(record) => ({
                                onClick: () => handleSelectDocument(record),
                                style: {cursor: 'pointer'}
                            })}
                        />
                    )
                },
                ...(selectedDocument ? [{
                    key: 'chunks',
                    label: `${selectedDocument.name} - 分块详情`,
                    children: (
                        <Card>
                            <Button icon={<ArrowLeftOutlined/>} onClick={handleBackToList} style={{marginBottom: 16}}>
                                返回文档列表
                            </Button>
                            {['COMPLETED', 'CHUNKED'].includes(selectedDocument.status) ? (
                                <>
                                    <Descriptions column={4} size="small" style={{marginBottom: 16}}>
                                        <Descriptions.Item label="文件名">{selectedDocument.name}</Descriptions.Item>
                                        <Descriptions.Item label="类型">
                                            {supportedTypes.find(t => t.contentType === selectedDocument.contentType)?.displayName || selectedDocument.contentType}
                                        </Descriptions.Item>
                                        <Descriptions.Item
                                            label="分块数">{selectedDocument.chunksCreated}</Descriptions.Item>
                                        <Descriptions.Item label="状态">
                                            <Badge status={statusConfig[selectedDocument.status]?.status}
                                                   text={statusConfig[selectedDocument.status]?.text}/>
                                        </Descriptions.Item>
                                        <Descriptions.Item label="分块策略">
                                            {selectedDocument.chunkStrategy === 'PARAGRAPH' ? '按段落' : '固定大小'}
                                        </Descriptions.Item>
                                        <Descriptions.Item
                                            label="分块大小">{selectedDocument.chunkSize || '-'}</Descriptions.Item>
                                        <Descriptions.Item
                                            label="重叠大小">{selectedDocument.chunkOverlap || '-'}</Descriptions.Item>
                                        <Descriptions.Item
                                            label="已向量化">{selectedDocument.chunksEmbedded || 0}</Descriptions.Item>
                                    </Descriptions>
                                    <Table
                                        columns={chunkColumns}
                                        dataSource={chunks}
                                        rowKey="id"
                                        loading={chunksLoading}
                                        pagination={{pageSize: 10}}
                                    />
                                </>
                            ) : (
                                <div style={{textAlign: 'center', padding: 40}}>
                                    <Badge status={statusConfig[selectedDocument.status]?.status || 'default'}
                                           text={statusConfig[selectedDocument.status]?.text}/>
                                    <br/>
                                    <span style={{color: '#999'}}>
                                        {selectedDocument.status === 'EMBEDDING' && '文档正在计算向量，请稍后...'}
                                        {selectedDocument.status === 'PROCESSING' && '文档正在提取文本，请稍后...'}
                                        {selectedDocument.status === 'FAILED' && `处理失败: ${selectedDocument.errorMessage}`}
                                    </span>
                                </div>
                            )}
                        </Card>
                    )
                }] : [])
            ]}/>

            {/* 上传配置 Modal */}
            <Modal
                title="上传文档配置"
                open={uploadModalVisible}
                onCancel={() => {
                    setUploadModalVisible(false)
                    uploadForm.resetFields()
                    setUploadFile(null)
                }}
                onOk={handleUploadSubmit}
                confirmLoading={uploading}
                width={500}
            >
                <Form form={uploadForm} layout="vertical" initialValues={{
                    chunkStrategy: 'FIXED_SIZE',
                    chunkSize: 500,
                    chunkOverlap: 50
                }}>
                    <Form.Item label="文件">
                        <Space>
                            <FileTextOutlined/>
                            <span>{uploadFile?.name}</span>
                            <Tag>{supportedTypes.find(t => uploadFile?.name.toLowerCase().endsWith(t.extension))?.displayName || '未知类型'}</Tag>
                        </Space>
                    </Form.Item>

                    <Form.Item name="name" label="文档名称">
                        <Input placeholder="可选，默认使用文件名"/>
                    </Form.Item>

                    <Form.Item name="chunkStrategy" label="分块策略">
                        <Radio.Group>
                            <Radio.Button value="FIXED_SIZE">固定大小</Radio.Button>
                            <Radio.Button value="PARAGRAPH">按段落</Radio.Button>
                        </Radio.Group>
                    </Form.Item>

                    <Form.Item shouldUpdate={(prev, curr) => prev.chunkStrategy !== curr.chunkStrategy}>
                        {({getFieldValue}) => (
                            getFieldValue('chunkStrategy') === 'FIXED_SIZE' && (
                                <>
                                    <Form.Item name="chunkSize" label="分块大小（字符数）">
                                        <InputNumber min={100} max={2000} step={100}/>
                                    </Form.Item>
                                    <Form.Item name="chunkOverlap" label="重叠大小（字符数）">
                                        <InputNumber min={0} max={200} step={10}/>
                                    </Form.Item>
                                </>
                            )
                        )}
                    </Form.Item>
                </Form>
            </Modal>

            {/* Embedding 模型选择 Modal */}
            <Modal
                title="选择 Embedding 模型"
                open={embedModalVisible}
                onCancel={() => {
                    setEmbedModalVisible(false)
                    setEmbedDocumentId(null)
                }}
                onOk={handleStartEmbedding}
                confirmLoading={embedLoading}
                width={500}
            >
                <div style={{marginBottom: 16}}>
                    <span>请选择用于计算文档向量的 Embedding 模型：</span>
                </div>
                <Select
                    style={{width: '100%'}}
                    value={selectedEmbedModelId}
                    onChange={setSelectedEmbedModelId}
                    options={embeddingModels.map(m => ({
                        value: m.id,
                        label: m.isDefaultEmbedding ? `${m.name} (默认)` : m.name
                    }))}
                    placeholder="选择 Embedding 模型"
                />

                {/* 显示当前选择模型的维度 */}
                {selectedEmbedModelId && (
                    <Card size="small" style={{marginTop: 16, background: '#f0f5ff'}}>
                        <Space>
                            <InfoCircleOutlined style={{color: '#1890ff'}}/>
                            <span>
                                当前选择模型维度：
                                <strong style={{color: '#1890ff'}}>
                                    {embeddingModels.find(m => m.id === selectedEmbedModelId)?.embeddingDimension || '未知'}维
                                </strong>
                            </span>
                        </Space>
                    </Card>
                )}

                <div style={{marginTop: 16, color: '#666', fontSize: 12}}>
                    <div style={{marginBottom: 8}}>💡 提示：不同 Embedding
                        模型产生的向量维度不同，将存储到不同的向量表中。
                    </div>
                    <div style={{paddingLeft: 8, borderLeft: '2px solid #ddd'}}>
                        <div>例如：</div>
                        <div>- text-embedding-3-large → 3072维</div>
                        <div>- text-embedding-ada-002 / text-embedding-3-small → 1536维</div>
                        <div>- text-embedding-v1 / text-embedding-v3 → 1024维</div>
                    </div>
                </div>
            </Modal>

            {/* 分块内容 Modal */}
            <Modal
                title={`分块 #${selectedChunk?.chunkIndex} 完整内容`}
                open={chunkModalVisible}
                onCancel={() => setChunkModalVisible(false)}
                footer={[
                    <Button key="close" onClick={() => setChunkModalVisible(false)}>
                        关闭
                    </Button>
                ]}
                width={800}
            >
                <div style={{
                    whiteSpace: 'pre-wrap',
                    maxHeight: 500,
                    overflow: 'auto',
                    padding: 16,
                    background: '#f5f5f5',
                    borderRadius: 4
                }}>
                    {selectedChunk?.content}
                </div>
                <Descriptions column={2} size="small" style={{marginTop: 16}}>
                    <Descriptions.Item label="分块ID">{selectedChunk?.id}</Descriptions.Item>
                    <Descriptions.Item label="文档ID">{selectedChunk?.documentId}</Descriptions.Item>
                    <Descriptions.Item label="字符数">{selectedChunk?.content?.length}</Descriptions.Item>
                    <Descriptions.Item label="创建时间">
                        {selectedChunk?.createdAt ? new Date(selectedChunk.createdAt).toLocaleString() : '-'}
                    </Descriptions.Item>
                </Descriptions>
            </Modal>
        </div>
    )
}

export default DocumentPage