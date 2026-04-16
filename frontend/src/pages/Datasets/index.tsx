import React, {useEffect, useState} from 'react'
import {
    Button,
    Card,
    Descriptions,
    Form,
    Input,
    message,
    Modal,
    Popconfirm,
    Space,
    Table,
    Tabs,
    Tooltip,
    Upload
} from 'antd'
import {DeleteOutlined, DownloadOutlined, EditOutlined, PlusOutlined, UploadOutlined} from '@ant-design/icons'
import type {ColumnsType} from 'antd/es/table'
import {
    createDataset,
    createDatasetItem,
    deleteDataset,
    deleteDatasetItem,
    exportDatasetCsv,
    exportDatasetJson,
    importItemsToDataset,
    listDatasetItems,
    listDatasets,
    updateDataset,
    updateDatasetItem
} from '@/api/datasets'
import {CreateDatasetItemRequest, CreateDatasetRequest, Dataset, DatasetItem} from '@/types/dataset'

const DatasetPage: React.FC = () => {
    const [datasets, setDatasets] = useState<Dataset[]>([])
    const [items, setItems] = useState<DatasetItem[]>([])
    const [loading, setLoading] = useState(false)
    const [itemsLoading, setItemsLoading] = useState(false)
    const [modalVisible, setModalVisible] = useState(false)
    const [itemModalVisible, setItemModalVisible] = useState(false)
    const [editingDataset, setEditingDataset] = useState<Dataset | null>(null)
    const [editingItem, setEditingItem] = useState<DatasetItem | null>(null)
    const [selectedDataset, setSelectedDataset] = useState<Dataset | null>(null)
    const [activeTab, setActiveTab] = useState<string>('list')
    const [importModalVisible, setImportModalVisible] = useState(false)
    // 默认导出格式为 JSON（后续可扩展添加格式选择 UI）
    const exportFormat: 'json' | 'csv' = 'json'
    const [importLoading, setImportLoading] = useState(false)
    const [form] = Form.useForm()
    const [itemForm] = Form.useForm()

    // 截断文本显示，超过长度显示 Tooltip
    const truncateText = (text: string | undefined, maxLength: number = 50) => {
        if (!text) return '-'
        if (text.length <= maxLength) return text
        return (
            <Tooltip title={text}>
                <span style={{cursor: 'pointer'}}>{text.substring(0, maxLength)}...</span>
            </Tooltip>
        )
    }

    useEffect(() => {
        fetchDatasets()
    }, [])

    const fetchDatasets = async () => {
        setLoading(true)
        try {
            const res = await listDatasets()
            if (res.success) {
                setDatasets(res.data.content || [])
                // 如果当前选中了数据集，更新其状态以反映最新的itemCount
                if (selectedDataset) {
                    const updated = res.data.content?.find((d: Dataset) => d.id === selectedDataset.id)
                    if (updated) {
                        setSelectedDataset(updated)
                    }
                }
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
        setActiveTab('items')  // 自动切换到数据项tab
    }

    const handleCreateItem = () => {
        setEditingItem(null)
        itemForm.resetFields()
        setItemModalVisible(true)
    }

    const handleEditItem = (record: DatasetItem) => {
        setEditingItem(record)
        itemForm.setFieldsValue({
            input: record.input,
            output: record.output,
            expectedDocIds: record.expectedDocIds,
            context: record.context
        })
        setItemModalVisible(true)
    }

    const handleDeleteItem = async (itemId: string) => {
        if (!selectedDataset) return
        try {
            await deleteDatasetItem(selectedDataset.id, itemId)
            message.success('删除成功')
            fetchItems(selectedDataset.id)
            fetchDatasets() // 刷新数据集列表以更新itemCount
        } catch {
            message.error('删除失败')
        }
    }

    const handleSubmitItem = async () => {
        if (!selectedDataset) return
        try {
            const values = await itemForm.validateFields()
            if (editingItem) {
                await updateDatasetItem(editingItem.id, values)
                message.success('更新成功')
            } else {
                await createDatasetItem(selectedDataset.id, values as CreateDatasetItemRequest)
                message.success('添加成功')
            }
            setItemModalVisible(false)
            // 刷新数据项列表
            fetchItems(selectedDataset.id)
            // 同时刷新数据集列表以更新itemCount
            fetchDatasets()
        } catch {
            message.error('操作失败')
        }
    }

    // 导出数据项
    const handleExport = async () => {
        if (!selectedDataset) return
        try {
            if (exportFormat === 'json') {
                await exportDatasetJson(selectedDataset.id)
                message.success('导出 JSON 成功')
            } else {
                await exportDatasetCsv(selectedDataset.id)
                message.success('导出 CSV 成功')
            }
        } catch {
            message.error('导出失败')
        }
    }

    // 打开导入弹窗
    const handleOpenImport = () => {
        setImportModalVisible(true)
    }

    // 解析导入文件
    const parseImportFile = (file: File): Promise<{ input: string; output?: string }[]> => {
        return new Promise((resolve, reject) => {
            const reader = new FileReader()
            reader.onload = (e) => {
                try {
                    const content = e.target?.result as string
                    if (file.name.endsWith('.json')) {
                        const data = JSON.parse(content)
                        if (Array.isArray(data)) {
                            resolve(data.map(item => ({
                                input: item.input || '',
                                output: item.output || undefined
                            })))
                        } else {
                            reject(new Error('JSON 文件格式错误，应为数组'))
                        }
                    } else if (file.name.endsWith('.csv')) {
                        const lines = content.split('\n').filter(line => line.trim())
                        // 跳过标题行（如果有）
                        const startIndex = lines[0].includes('input') ? 1 : 0
                        const items = lines.slice(startIndex).map(line => {
                            const parts = line.split(',')
                            return {
                                input: parts[0]?.trim() || '',
                                output: parts[1]?.trim() || undefined
                            }
                        })
                        resolve(items)
                    } else {
                        reject(new Error('不支持的文件格式，请使用 JSON 或 CSV'))
                    }
                } catch (err) {
                    reject(new Error('文件解析失败'))
                }
            }
            reader.onerror = () => reject(new Error('文件读取失败'))
            reader.readAsText(file)
        })
    }

    // 导入数据项
    const handleImport = async (file: File) => {
        if (!selectedDataset) return
        setImportLoading(true)
        try {
            const items = await parseImportFile(file)
            if (items.length === 0) {
                message.warning('文件中没有数据')
                return
            }
            await importItemsToDataset(selectedDataset.id, items)
            message.success(`成功导入 ${items.length} 条数据`)
            setImportModalVisible(false)
            fetchItems(selectedDataset.id)
        } catch (err: any) {
            message.error(err.message || '导入失败')
        } finally {
            setImportLoading(false)
        }
    }

    const datasetColumns: ColumnsType<Dataset> = [
        {title: '名称', dataIndex: 'name', key: 'name'},
        {title: '描述', dataIndex: 'description', key: 'description', ellipsis: true},
        {title: '版本', dataIndex: 'version', key: 'version'},
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
            width: 180,
            fixed: 'right' as const,
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
        {
            title: '序号',
            key: 'index',
            width: 60,
            render: (_, __, index) => index + 1
        },
        {
            title: '输入',
            dataIndex: 'input',
            key: 'input',
            width: 200,
            render: (text: string) => truncateText(text, 80)
        },
        {
            title: '期望输出',
            dataIndex: 'output',
            key: 'output',
            width: 200,
            render: (text: string) => truncateText(text, 80)
        },
        {
            title: '操作',
            key: 'action',
            width: 120,
            fixed: 'right' as const,
            render: (_, record) => (
                <Space>
                    <Button type="link" icon={<EditOutlined/>} onClick={() => handleEditItem(record)}>
                        编辑
                    </Button>
                    <Popconfirm title="确定删除?" onConfirm={() => handleDeleteItem(record.id)}>
                        <Button type="link" danger icon={<DeleteOutlined/>}>
                            删除
                        </Button>
                    </Popconfirm>
                </Space>
            )
        }
    ]

    return (
        <div>
            <div style={{marginBottom: 16, display: 'flex', justifyContent: 'space-between'}}>
                <div>
                    <h2 style={{marginBottom: 0}}>数据集管理</h2>
                    <p style={{color: '#666', marginTop: 4}}>管理测试数据用于批量评估，输入是发给 AI
                        的内容，期望输出是参考答案（可选）</p>
                </div>
                <Space>
                    {activeTab === 'items' && selectedDataset && (
                        <Button onClick={() => {
                            setActiveTab('list');
                            setSelectedDataset(null);
                            setItems([]);
                        }}>
                            返回列表
                        </Button>
                    )}
                    <Button icon={<UploadOutlined/>} onClick={handleOpenImport}>导入</Button>
                    <Button type="primary" icon={<PlusOutlined/>} onClick={handleCreate}>
                        新建数据集
                    </Button>
                </Space>
            </div>

            <Tabs activeKey={activeTab} onChange={setActiveTab} items={[
                {
                    key: 'list',
                    label: '数据集列表',
                    children: (
                        <Table
                            columns={datasetColumns}
                            dataSource={datasets}
                            rowKey="id"
                            loading={loading}
                            scroll={{x: 'max-content'}}
                            onRow={(record) => ({
                                onClick: () => handleSelectDataset(record),
                                style: {cursor: 'pointer'}
                            })}
                        />
                    )
                },
                ...(selectedDataset ? [{
                    key: 'items',
                    label: `${selectedDataset.name} - 数据项`,
                    children: (
                        <Card>
                            <Descriptions column={3} size="small" style={{marginBottom: 16}}>
                                <Descriptions.Item label="数据集">{selectedDataset.name}</Descriptions.Item>
                                <Descriptions.Item label="版本">{selectedDataset.version}</Descriptions.Item>
                                <Descriptions.Item label="数据项数">{selectedDataset.itemCount}</Descriptions.Item>
                            </Descriptions>
                            <div style={{marginBottom: 16}}>
                                <Space>
                                    <Button icon={<PlusOutlined/>} onClick={handleCreateItem}>
                                        添加数据项
                                    </Button>
                                    <Button icon={<DownloadOutlined/>} onClick={handleExport}>
                                        导出
                                    </Button>
                                    <Button icon={<UploadOutlined/>} onClick={handleOpenImport}>
                                        导入
                                    </Button>
                                </Space>
                            </div>
                            <Table
                                columns={itemColumns}
                                dataSource={items}
                                rowKey="id"
                                loading={itemsLoading}
                                scroll={{x: true}}
                                pagination={{pageSize: 10}}
                            />
                        </Card>
                    )
                }] : [])
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
                title={editingItem ? '编辑数据项' : '添加数据项'}
                open={itemModalVisible}
                onOk={handleSubmitItem}
                onCancel={() => setItemModalVisible(false)}
                width={600}
            >
                <Form form={itemForm} layout="vertical">
                    <Form.Item name="input" label="输入" rules={[{required: true}]}>
                        <Input.TextArea rows={4}/>
                    </Form.Item>
                    <Form.Item name="output" label="期望输出（可选）">
                        <Input.TextArea rows={4}/>
                    </Form.Item>
                    <Form.Item name="expectedDocIds" label="期望文档ID（RAG评估）"
                               help='JSON数组格式，如：["docId1", "docId2"]'>
                        <Input placeholder='["docId1", "docId2"]'/>
                    </Form.Item>
                    <Form.Item name="context" label="参考上下文（RAG评估）"
                               help="用于评估答案是否忠实于检索内容">
                        <Input.TextArea rows={3}/>
                    </Form.Item>
                </Form>
            </Modal>

            <Modal
                title="导入数据项"
                open={importModalVisible}
                onCancel={() => setImportModalVisible(false)}
                footer={null}
            >
                <div style={{marginBottom: 16}}>
                    <p style={{color: '#666'}}>支持 JSON 或 CSV 格式文件：</p>
                    <ul style={{color: '#999', fontSize: 12}}>
                        <li>JSON: 数组格式，每项包含 input 和 output 字段</li>
                        <li>CSV: 第一列为 input，第二列为 output（可选标题行）</li>
                    </ul>
                </div>
                <Upload
                    accept=".json,.csv"
                    showUploadList={false}
                    beforeUpload={(file) => {
                        handleImport(file)
                        return false
                    }}
                >
                    <Button icon={<UploadOutlined/>} loading={importLoading}>
                        选择文件
                    </Button>
                </Upload>
            </Modal>
        </div>
    )
}

export default DatasetPage