import React, {useEffect, useState} from 'react'
import {Badge, Button, Form, Input, InputNumber, message, Modal, Popconfirm, Select, Space, Table, Tag} from 'antd'
import {ApiOutlined, CheckOutlined, DeleteOutlined, EditOutlined, PlusOutlined} from '@ant-design/icons'
import type {ColumnsType} from 'antd/es/table'
import {
    createModel,
    deleteModel,
    listModels,
    listProviders,
    setDefaultModel,
    testModel,
    updateModel
} from '@/api/models'
import {CreateModelRequest, ModelConfig, ProviderInfo} from '@/types/model'

const ModelListPage: React.FC = () => {
    const [data, setData] = useState<ModelConfig[]>([])
    const [providers, setProviders] = useState<ProviderInfo[]>([])
    const [loading, setLoading] = useState(false)
    const [modalVisible, setModalVisible] = useState(false)
    const [editingModel, setEditingModel] = useState<ModelConfig | null>(null)
    const [form] = Form.useForm()

    useEffect(() => {
        fetchData()
        fetchProviders()
    }, [])

    const fetchData = async () => {
        setLoading(true)
        try {
            const res = await listModels()
            if (res.success) {
                setData(res.data || [])
            }
        } finally {
            setLoading(false)
        }
    }

    const fetchProviders = async () => {
        try {
            const res = await listProviders()
            if (res.success) {
                setProviders(res.data || [])
            }
        } catch {
            // ignore
        }
    }

    // 当选择供应商时，自动填充默认 baseUrl
    const handleProviderChange = (providerName: string) => {
        const provider = providers.find(p => p.name === providerName)
        if (provider) {
            form.setFieldsValue({
                baseUrl: provider.defaultBaseUrl
            })
        }
    }

    const handleCreate = () => {
        setEditingModel(null)
        form.resetFields()
        setModalVisible(true)
    }

    const handleEdit = (record: ModelConfig) => {
        setEditingModel(record)
        form.setFieldsValue(record)
        setModalVisible(true)
    }

    const handleDelete = async (id: string) => {
        try {
            await deleteModel(id)
            message.success('删除成功')
            fetchData()
        } catch {
            message.error('删除失败')
        }
    }

    const handleTest = async (id: string) => {
        try {
            const res = await testModel(id)
            if (res.data.healthy) {
                message.success('模型连接正常')
            } else {
                message.error('模型连接失败')
            }
            fetchData()
        } catch {
            message.error('测试失败')
        }
    }

    const handleSetDefault = async (id: string) => {
        try {
            await setDefaultModel(id)
            message.success('已设为默认模型')
            fetchData()
        } catch {
            message.error('操作失败')
        }
    }

    const handleSubmit = async () => {
        try {
            const values = await form.validateFields()
            if (editingModel) {
                await updateModel(editingModel.id, values)
                message.success('更新成功')
            } else {
                await createModel(values as CreateModelRequest)
                message.success('创建成功')
            }
            setModalVisible(false)
            fetchData()
        } catch {
            message.error('操作失败')
        }
    }

    const healthStatusMap = {
        HEALTHY: {status: 'success' as const, text: '健康'},
        UNHEALTHY: {status: 'error' as const, text: '异常'},
        UNKNOWN: {status: 'default' as const, text: '未知'},
    }

    const columns: ColumnsType<ModelConfig> = [
        {title: '名称', dataIndex: 'name', key: 'name'},
        {title: '供应商', dataIndex: 'provider', key: 'provider'},
        {title: '模型', dataIndex: 'modelName', key: 'modelName'},
        {
            title: '状态',
            dataIndex: 'healthStatus',
            key: 'healthStatus',
            render: (status: keyof typeof healthStatusMap) => (
                <Badge {...healthStatusMap[status]} />
            )
        },
        {
            title: '默认',
            dataIndex: 'isDefault',
            key: 'isDefault',
            render: (v: boolean) => v ? <Tag color="blue">默认</Tag> : null
        },
        {
            title: '操作',
            key: 'action',
            render: (_, record) => (
                <Space>
                    <Button type="link" icon={<ApiOutlined/>} onClick={() => handleTest(record.id)}>
                        测试
                    </Button>
                    {!record.isDefault && (
                        <Button type="link" icon={<CheckOutlined/>} onClick={() => handleSetDefault(record.id)}>
                            设为默认
                        </Button>
                    )}
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

    return (
        <div>
            <div style={{marginBottom: 16, display: 'flex', justifyContent: 'space-between'}}>
                <div>
                    <h2 style={{marginBottom: 0}}>模型管理</h2>
                    <p style={{color: '#666', marginTop: 4}}>配置 AI 模型的 API 地址和密钥，测试连接后设为默认模型</p>
                </div>
                <Button type="primary" icon={<PlusOutlined/>} onClick={handleCreate}>
                    新建模型
                </Button>
            </div>

            <Table
                columns={columns}
                dataSource={data}
                rowKey="id"
                loading={loading}
            />

            <Modal
                title={editingModel ? '编辑模型' : '新建模型'}
                open={modalVisible}
                onOk={handleSubmit}
                onCancel={() => setModalVisible(false)}
                width={600}
            >
                <Form form={form} layout="vertical">
                    <Form.Item name="name" label="配置名称" rules={[{required: true, message: '请输入配置名称'}]}>
                        <Input placeholder="例如: 我的GPT-4"/>
                    </Form.Item>
                    <Form.Item name="provider" label="供应商" rules={[{required: true, message: '请选择供应商'}]}>
                        <Select onChange={handleProviderChange} placeholder="选择供应商">
                            {providers.map(p => (
                                <Select.Option key={p.name} value={p.name}>{p.displayName}</Select.Option>
                            ))}
                        </Select>
                    </Form.Item>
                    <Form.Item name="modelName" label="模型名称" rules={[{required: true, message: '请输入模型名称'}]}>
                        <Input placeholder="例如: gpt-4, claude-3-5-sonnet, qwen-max"/>
                    </Form.Item>
                    <Form.Item name="baseUrl" label="API 地址" extra="选择供应商后自动填充默认地址，可修改">
                        <Input placeholder="API Base URL"/>
                    </Form.Item>
                    <Form.Item
                        name="apiKey"
                        label="API Key"
                        extra={editingModel ? '留空则保持原有 API Key 不变' : undefined}
                        rules={[{required: !editingModel, message: '请输入 API Key'}]}
                    >
                        <Input.Password placeholder={editingModel ? '留空保持不变' : '输入您的 API Key'}/>
                    </Form.Item>
                    <Form.Item name="temperature" label="Temperature" extra="控制输出的随机性，0-2">
                        <InputNumber min={0} max={2} step={0.1} defaultValue={0.7}/>
                    </Form.Item>
                    <Form.Item name="maxTokens" label="Max Tokens" extra="最大输出长度">
                        <InputNumber min={1} max={100000} defaultValue={2048}/>
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    )
}

export default ModelListPage