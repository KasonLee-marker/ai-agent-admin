import React, {useEffect, useState} from 'react'
import {Button, Form, Input, message, Modal, Popconfirm, Space, Table, Tag} from 'antd'
import {DeleteOutlined, EditOutlined, HistoryOutlined, PlusOutlined} from '@ant-design/icons'
import type {ColumnsType} from 'antd/es/table'
import {createPrompt, deletePrompt, listPrompts, updatePrompt} from '@/api/prompts'
import {PromptTemplate, PromptTemplateCreateRequest} from '@/types/prompt'

const PromptListPage: React.FC = () => {
    const [data, setData] = useState<PromptTemplate[]>([])
    const [loading, setLoading] = useState(false)
    const [modalVisible, setModalVisible] = useState(false)
    const [editingPrompt, setEditingPrompt] = useState<PromptTemplate | null>(null)
    const [form] = Form.useForm()

    useEffect(() => {
        fetchData()
    }, [])

    const fetchData = async () => {
        setLoading(true)
        try {
            const res = await listPrompts()
            if (res.success) {
                setData(res.data.content || [])
            }
        } finally {
            setLoading(false)
        }
    }

    const handleCreate = () => {
        setEditingPrompt(null)
        form.resetFields()
        setModalVisible(true)
    }

    const handleEdit = (record: PromptTemplate) => {
        setEditingPrompt(record)
        form.setFieldsValue(record)
        setModalVisible(true)
    }

    const handleDelete = async (id: string) => {
        try {
            await deletePrompt(id)
            message.success('删除成功')
            fetchData()
        } catch {
            message.error('删除失败')
        }
    }

    const handleSubmit = async () => {
        try {
            const values = await form.validateFields()
            if (editingPrompt) {
                await updatePrompt(editingPrompt.id, values)
                message.success('更新成功')
            } else {
                await createPrompt(values as PromptTemplateCreateRequest)
                message.success('创建成功')
            }
            setModalVisible(false)
            fetchData()
        } catch {
            message.error('操作失败')
        }
    }

    const columns: ColumnsType<PromptTemplate> = [
        {title: '名称', dataIndex: 'name', key: 'name'},
        {title: '分类', dataIndex: 'category', key: 'category'},
        {
            title: '标签',
            dataIndex: 'tags',
            key: 'tags',
            render: (tags: string[]) => tags?.map(t => <Tag key={t}>{t}</Tag>)
        },
        {title: '版本', dataIndex: 'version', key: 'version'},
        {
            title: '更新时间',
            dataIndex: 'updatedAt',
            key: 'updatedAt',
            render: (date: string) => new Date(date).toLocaleString()
        },
        {
            title: '操作',
            key: 'action',
            render: (_, record) => (
                <Space>
                    <Button type="link" icon={<EditOutlined/>} onClick={() => handleEdit(record)}>
                        编辑
                    </Button>
                    <Button type="link" icon={<HistoryOutlined/>}>
                        历史
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
                <h2>Prompt 管理</h2>
                <Button type="primary" icon={<PlusOutlined/>} onClick={handleCreate}>
                    新建 Prompt
                </Button>
            </div>

            <Table
                columns={columns}
                dataSource={data}
                rowKey="id"
                loading={loading}
            />

            <Modal
                title={editingPrompt ? '编辑 Prompt' : '新建 Prompt'}
                open={modalVisible}
                onOk={handleSubmit}
                onCancel={() => setModalVisible(false)}
                width={600}
            >
                <Form form={form} layout="vertical">
                    <Form.Item name="name" label="名称" rules={[{required: true}]}>
                        <Input/>
                    </Form.Item>
                    <Form.Item name="description" label="描述">
                        <Input.TextArea rows={2}/>
                    </Form.Item>
                    <Form.Item name="category" label="分类">
                        <Input/>
                    </Form.Item>
                    <Form.Item name="content" label="内容" rules={[{required: true}]}>
                        <Input.TextArea rows={6}/>
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    )
}

export default PromptListPage