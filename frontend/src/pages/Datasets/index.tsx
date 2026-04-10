import React, {useEffect, useState} from 'react'
import {Badge, Button, Card, Descriptions, Form, Input, message, Modal, Popconfirm, Space, Table, Tabs} from 'antd'
import {DeleteOutlined, DownloadOutlined, EditOutlined, PlusOutlined, UploadOutlined} from '@ant-design/icons'
import type {ColumnsType} from 'antd/es/table'
import {
    createDataset,
    createDatasetItem,
    deleteDataset,
    deleteDatasetItem,
    listDatasetItems,
    listDatasets,
    updateDataset
} from '@/api/datasets'
import {CreateDatasetItemRequest, CreateDatasetRequest, Dataset, DatasetItem, DatasetStatus} from '@/types/dataset'

const DatasetPage: React.FC = () => {
    const [datasets, setDatasets] = useState<Dataset[]>([])
    const [items, setItems] = useState<DatasetItem[]>([])
    const [loading, setLoading] = useState(false)
    const [itemsLoading, setItemsLoading] = useState(false)
    const [modalVisible, setModalVisible] = useState(false)
    const [itemModalVisible, setItemModalVisible] = useState(false)
    const [editingDataset, setEditingDataset] = useState<Dataset | null>(null)
    const [selectedDataset, setSelectedDataset] = useState<Dataset | null>(null)
    const [form] = Form.useForm()
    const [itemForm] = Form.useForm()

    useEffect(() => {
        fetchDatasets()
    }, [])

    const fetchDatasets = async () => {
        setLoading(true)
        try {
            const res = await listDatasets()
            if (res.success) {
                setDatasets(res.data.content || [])
            }
        } finally {
            setLoading(false)
        }
    }

    const fetchItems = async (datasetId: string) => {
        setItemsLoading(true)
        try {
            const res = await listDatasetItems(datasetId)
            if (res.success) {
                setItems(res.data.content || [])
            }
        } finally {
            setItemsLoading(false)
        }
    }

    const handleCreate = () => {
        setEditingDataset(null)
        form.resetFields()
        setModalVisible(true)
    }

    const handleEdit = (record: Dataset) => {
        setEditingDataset(record)
        form.setFieldsValue(record)
        setModalVisible(true)
    }

    const handleDelete = async (id: string) => {
        try {
            await deleteDataset(id)
            message.success('删除成功')
            fetchDatasets()
            if (selectedDataset?.id === id) {
                setSelectedDataset(null)
                setItems([])
            }
        } catch {
            message.error('删除失败')
        }
    }

    const handleSubmit = async () => {
        try {
            const values = await form.validateFields()
            if (editingDataset) {
                await updateDataset(editingDataset.id, values)
                message.success('更新成功')
            } else {
                await createDataset(values as CreateDatasetRequest)
                message.success('创建成功')
            }
            setModalVisible(false)
            fetchDatasets()
        } catch {
            message.error('操作失败')
        }
    }

    const handleSelectDataset = (record: Dataset) => {
        setSelectedDataset(record)
        fetchItems(record.id)
    }

    const handleCreateItem = () => {
        itemForm.resetFields()
        setItemModalVisible(true)
    }

    const handleDeleteItem = async (itemId: string) => {
        if (!selectedDataset) return
        try {
            await deleteDatasetItem(selectedDataset.id, itemId)
            message.success('删除成功')
            fetchItems(selectedDataset.id)
        } catch {
            message.error('删除失败')
        }
    }

    const handleSubmitItem = async () => {
        if (!selectedDataset) return
        try {
            const values = await itemForm.validateFields()
            await createDatasetItem(selectedDataset.id, values as CreateDatasetItemRequest)
            message.success('添加成功')
            setItemModalVisible(false)
            fetchItems(selectedDataset.id)
        } catch {
            message.error('添加失败')
        }
    }

    const statusMap: Record<DatasetStatus, { status: 'success' | 'processing' | 'default', text: string }> = {
        DRAFT: {status: 'default', text: '草稿'},
        ACTIVE: {status: 'success', text: '活跃'},
        ARCHIVED: {status: 'processing', text: '已归档'}
    }

    const datasetColumns: ColumnsType<Dataset> = [
        {title: '名称', dataIndex: 'name', key: 'name'},
        {title: '描述', dataIndex: 'description', key: 'description', ellipsis: true},
        {title: '版本', dataIndex: 'version', key: 'version'},
        {
            title: '状态',
            dataIndex: 'status',
            key: 'status',
            render: (status: DatasetStatus) => (
                <Badge status={statusMap[status]?.status || 'default'} text={statusMap[status]?.text || status}/>
            )
        },
        {title: '数据项数', dataIndex: 'itemCount', key: 'itemCount'},
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
                    <Button type="link" onClick={() => handleSelectDataset(record)}>
                        查看
                    </Button>
                    <Button type="link" icon={<EditOutlined/>} onClick={() => handleEdit(record)}>
                        编辑
                    </Button>
                    <Popconfirm title="确定删除?" onConfirm={() => handleDelete(record.id)}>
                        <Button type="link" danger icon={<DeleteOutlined/>}>
                            删除
                        </Button>
                    </Popconfirm>
                </Space>
            )
        }
    ]

    const itemColumns: ColumnsType<DatasetItem> = [
        {title: '序号', dataIndex: 'id', key: 'id', width: 80},
        {title: '输入', dataIndex: 'input', key: 'input', ellipsis: true},
        {title: '期望输出', dataIndex: 'expectedOutput', key: 'expectedOutput', ellipsis: true},
        {
            title: '操作',
            key: 'action',
            width: 100,
            render: (_, record) => (
                <Popconfirm title="确定删除?" onConfirm={() => handleDeleteItem(record.id)}>
                    <Button type="link" danger icon={<DeleteOutlined/>}>
                        删除
                    </Button>
                </Popconfirm>
            )
        }
    ]

    return (
        <div>
            <div style={{marginBottom: 16, display: 'flex', justifyContent: 'space-between'}}>
                <h2>数据集管理</h2>
                <Space>
                    <Button icon={<UploadOutlined/>}>导入</Button>
                    <Button type="primary" icon={<PlusOutlined/>} onClick={handleCreate}>
                        新建数据集
                    </Button>
                </Space>
            </div>

            <Tabs defaultActiveKey="list" items={[
                {
                    key: 'list',
                    label: '数据集列表',
                    children: (
                        <Table
                            columns={datasetColumns}
                            dataSource={datasets}
                            rowKey="id"
                            loading={loading}
                            onRow={(record) => ({
                                onClick: () => handleSelectDataset(record),
                                style: {cursor: 'pointer'}
                            })}
                        />
                    )
                },
                {
                    key: 'items',
                    label: selectedDataset ? `${selectedDataset.name} - 数据项` : '数据项（请选择数据集）',
                    children: selectedDataset ? (
                        <Card>
                            <Descriptions column={4} size="small" style={{marginBottom: 16}}>
                                <Descriptions.Item label="数据集">{selectedDataset.name}</Descriptions.Item>
                                <Descriptions.Item label="版本">{selectedDataset.version}</Descriptions.Item>
                                <Descriptions.Item
                                    label="状态">{statusMap[selectedDataset.status]?.text}</Descriptions.Item>
                                <Descriptions.Item label="数据项数">{selectedDataset.itemCount}</Descriptions.Item>
                            </Descriptions>
                            <div style={{marginBottom: 16}}>
                                <Space>
                                    <Button icon={<PlusOutlined/>} onClick={handleCreateItem}>
                                        添加数据项
                                    </Button>
                                    <Button icon={<DownloadOutlined/>}>
                                        导出
                                    </Button>
                                </Space>
                            </div>
                            <Table
                                columns={itemColumns}
                                dataSource={items}
                                rowKey="id"
                                loading={itemsLoading}
                                pagination={{pageSize: 10}}
                            />
                        </Card>
                    ) : (
                        <Card>
                            <div style={{textAlign: 'center', padding: 40}}>
                                请从左侧列表选择一个数据集查看数据项
                            </div>
                        </Card>
                    )
                }
            ]}/>

            <Modal
                title={editingDataset ? '编辑数据集' : '新建数据集'}
                open={modalVisible}
                onOk={handleSubmit}
                onCancel={() => setModalVisible(false)}
            >
                <Form form={form} layout="vertical">
                    <Form.Item name="name" label="名称" rules={[{required: true}]}>
                        <Input/>
                    </Form.Item>
                    <Form.Item name="description" label="描述">
                        <Input.TextArea rows={3}/>
                    </Form.Item>
                </Form>
            </Modal>

            <Modal
                title="添加数据项"
                open={itemModalVisible}
                onOk={handleSubmitItem}
                onCancel={() => setItemModalVisible(false)}
                width={600}
            >
                <Form form={itemForm} layout="vertical">
                    <Form.Item name="input" label="输入" rules={[{required: true}]}>
                        <Input.TextArea rows={4}/>
                    </Form.Item>
                    <Form.Item name="expectedOutput" label="期望输出">
                        <Input.TextArea rows={4}/>
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    )
}

export default DatasetPage