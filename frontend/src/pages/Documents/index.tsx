import React, {useEffect, useState} from 'react'
import type {UploadProps} from 'antd'
import {Badge, Button, Card, Descriptions, message, Popconfirm, Space, Table, Tabs, Tag, Upload} from 'antd'
import {DeleteOutlined, EyeOutlined, FileTextOutlined, UploadOutlined} from '@ant-design/icons'
import type {ColumnsType} from 'antd/es/table'
import {deleteDocument, getDocumentChunks, getSupportedTypes, listDocuments, uploadDocument} from '@/api/documents'
import {Document, DocumentChunk, DocumentStatus} from '@/types/document'

const DocumentPage: React.FC = () => {
    const [documents, setDocuments] = useState<Document[]>([])
    const [chunks, setChunks] = useState<DocumentChunk[]>([])
    const [loading, setLoading] = useState(false)
    const [chunksLoading, setChunksLoading] = useState(false)
    const [selectedDocument, setSelectedDocument] = useState<Document | null>(null)
    const [supportedTypes, setSupportedTypes] = useState<string[]>([])
    const [uploading, setUploading] = useState(false)

    useEffect(() => {
        fetchDocuments()
        fetchSupportedTypes()
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
                setSupportedTypes(res.data?.map(t => t.contentType) || [])
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
                setChunks(res.data.content || [])
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

    const handleSelectDocument = (record: Document) => {
        setSelectedDocument(record)
        if (record.status === 'COMPLETED') {
            fetchChunks(record.id)
        } else {
            setChunks([])
        }
    }

    const uploadProps: UploadProps = {
        name: 'file',
        showUploadList: false,
        beforeUpload: async (file) => {
            setUploading(true)
            try {
                const res = await uploadDocument(file)
                if (res.success) {
                    message.success(`${file.name} 上传成功，正在处理...`)
                    fetchDocuments()
                } else {
                    message.error('上传失败')
                }
            } catch {
                message.error('上传失败')
            } finally {
                setUploading(false)
            }
            return false
        }
    }

    const statusMap: Record<DocumentStatus, { status: 'success' | 'processing' | 'error', text: string }> = {
        PROCESSING: {status: 'processing', text: '处理中'},
        COMPLETED: {status: 'success', text: '已完成'},
        FAILED: {status: 'error', text: '失败'}
    }

    const contentTypeMap: Record<string, string> = {
        'application/pdf': 'PDF',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document': 'Word',
        'text/plain': 'TXT',
        'text/markdown': 'Markdown',
        'text/csv': 'CSV'
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
            render: (type: string) => contentTypeMap[type] || type
        },
        {
            title: '状态',
            dataIndex: 'status',
            key: 'status',
            render: (status: DocumentStatus) => (
                <Badge status={statusMap[status]?.status || 'default'} text={statusMap[status]?.text || status}/>
            )
        },
        {title: '分块数', dataIndex: 'totalChunks', key: 'totalChunks'},
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
                    {record.status === 'COMPLETED' && (
                        <Button type="link" icon={<EyeOutlined/>} onClick={() => handleSelectDocument(record)}>
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
            render: (content: string) => (
                <div style={{maxWidth: 600, whiteSpace: 'pre-wrap'}}>
                    {content.substring(0, 200)}...
                </div>
            )
        },
        {
            title: '创建时间',
            dataIndex: 'createdAt',
            key: 'createdAt',
            render: (date: string) => new Date(date).toLocaleString()
        }
    ]

    return (
        <div>
            <div style={{marginBottom: 16, display: 'flex', justifyContent: 'space-between'}}>
                <h2>文档管理</h2>
                <Upload {...uploadProps}>
                    <Button icon={<UploadOutlined/>} loading={uploading} type="primary">
                        上传文档
                    </Button>
                </Upload>
            </div>

            <Card style={{marginBottom: 16}}>
                <Space>
                    <span>支持的文件类型:</span>
                    {supportedTypes.map(type => (
                        <Tag key={type}>{contentTypeMap[type] || type}</Tag>
                    ))}
                </Space>
            </Card>

            <Tabs defaultActiveKey="list" items={[
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
                {
                    key: 'chunks',
                    label: selectedDocument ? `${selectedDocument.name} - 分块` : '文档分块（请选择文档）',
                    children: selectedDocument ? (
                        selectedDocument.status === 'COMPLETED' ? (
                            <Card>
                                <Descriptions column={4} size="small" style={{marginBottom: 16}}>
                                    <Descriptions.Item label="文件名">{selectedDocument.name}</Descriptions.Item>
                                    <Descriptions.Item
                                        label="类型">{contentTypeMap[selectedDocument.contentType] || selectedDocument.contentType}</Descriptions.Item>
                                    <Descriptions.Item label="分块数">{selectedDocument.totalChunks}</Descriptions.Item>
                                    <Descriptions.Item
                                        label="状态">{statusMap[selectedDocument.status]?.text}</Descriptions.Item>
                                </Descriptions>
                                <Table
                                    columns={chunkColumns}
                                    dataSource={chunks}
                                    rowKey="id"
                                    loading={chunksLoading}
                                    pagination={{pageSize: 10}}
                                />
                            </Card>
                        ) : (
                            <Card>
                                <div style={{textAlign: 'center', padding: 40}}>
                                    <Badge status={statusMap[selectedDocument.status]?.status || 'default'}
                                           text={statusMap[selectedDocument.status]?.text}/>
                                    <br/>
                                    <span style={{color: '#999'}}>文档正在处理中，请稍后查看</span>
                                </div>
                            </Card>
                        )
                    ) : (
                        <Card>
                            <div style={{textAlign: 'center', padding: 40}}>
                                请从左侧列表选择一个已处理的文档查看分块
                            </div>
                        </Card>
                    )
                }
            ]}/>
        </div>
    )
}

export default DocumentPage