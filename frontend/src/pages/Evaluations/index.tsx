import React, {useEffect, useState} from 'react'
import {
    Badge,
    Button,
    Card,
    Col,
    Form,
    Input,
    message,
    Modal,
    Popconfirm,
    Progress,
    Row,
    Select,
    Space,
    Spin,
    Statistic,
    Table,
    Tabs,
    Tag
} from 'antd'
import {
    BarChartOutlined,
    DeleteOutlined,
    EditOutlined,
    PlayCircleOutlined,
    PlusOutlined,
    StopOutlined
} from '@ant-design/icons'
import type {ColumnsType} from 'antd/es/table'
import {
    cancelEvaluation,
    createEvaluation,
    deleteEvaluation,
    getEvaluationMetrics,
    getEvaluationResults,
    listEvaluations,
    runEvaluation,
    updateEvaluation
} from '@/api/evaluations'
import {listDatasets} from '@/api/datasets'
import {listPrompts} from '@/api/prompts'
import {listModels} from '@/api/models'
import {
    CreateEvaluationRequest,
    EvaluationJob,
    EvaluationMetrics,
    EvaluationResult,
    EvaluationStatus
} from '@/types/evaluation'
import {Dataset} from '@/types/dataset'
import {PromptTemplate} from '@/types/prompt'
import {ModelConfig} from '@/types/model'

const EvaluationPage: React.FC = () => {
    const [evaluations, setEvaluations] = useState<EvaluationJob[]>([])
    const [results, setResults] = useState<EvaluationResult[]>([])
    const [metrics, setMetrics] = useState<EvaluationMetrics | null>(null)
    const [datasets, setDatasets] = useState<Dataset[]>([])
    const [prompts, setPrompts] = useState<PromptTemplate[]>([])
    const [models, setModels] = useState<ModelConfig[]>([])
    const [loading, setLoading] = useState(false)
    const [resultsLoading, setResultsLoading] = useState(false)
    const [modalVisible, setModalVisible] = useState(false)
    const [editingEvaluation, setEditingEvaluation] = useState<EvaluationJob | null>(null)
    const [selectedEvaluation, setSelectedEvaluation] = useState<EvaluationJob | null>(null)
    const [form] = Form.useForm()

    useEffect(() => {
        fetchEvaluations()
        fetchOptions()
    }, [])

    const fetchEvaluations = async () => {
        setLoading(true)
        try {
            const res = await listEvaluations()
            if (res.success) {
                setEvaluations(res.data.content || [])
            }
        } finally {
            setLoading(false)
        }
    }

    const fetchOptions = async () => {
        try {
            const [dsRes, ptRes, mdRes] = await Promise.all([
                listDatasets(),
                listPrompts(),
                listModels()
            ])
            if (dsRes.success) setDatasets(dsRes.data.content || [])
            if (ptRes.success) setPrompts(ptRes.data.content || [])
            if (mdRes.success) setModels(mdRes.data || [])
        } catch {
            // ignore
        }
    }

    const fetchResults = async (id: string) => {
        setResultsLoading(true)
        try {
            const [resRes, metricsRes] = await Promise.all([
                getEvaluationResults(id),
                getEvaluationMetrics(id)
            ])
            if (resRes.success) setResults(resRes.data.content || [])
            if (metricsRes.success) setMetrics(metricsRes.data)
        } finally {
            setResultsLoading(false)
        }
    }

    const handleCreate = () => {
        setEditingEvaluation(null)
        form.resetFields()
        setModalVisible(true)
    }

    const handleEdit = (record: EvaluationJob) => {
        setEditingEvaluation(record)
        form.setFieldsValue(record)
        setModalVisible(true)
    }

    const handleDelete = async (id: string) => {
        try {
            await deleteEvaluation(id)
            message.success('删除成功')
            fetchEvaluations()
            if (selectedEvaluation?.id === id) {
                setSelectedEvaluation(null)
                setResults([])
                setMetrics(null)
            }
        } catch {
            message.error('删除失败')
        }
    }

    const handleRun = async (id: string) => {
        try {
            await runEvaluation(id)
            message.success('评估任务已启动')
            fetchEvaluations()
        } catch {
            message.error('启动失败')
        }
    }

    const handleCancel = async (id: string) => {
        try {
            await cancelEvaluation(id)
            message.success('评估任务已取消')
            fetchEvaluations()
        } catch {
            message.error('取消失败')
        }
    }

    const handleSubmit = async () => {
        try {
            const values = await form.validateFields()
            if (editingEvaluation) {
                await updateEvaluation(editingEvaluation.id, values)
                message.success('更新成功')
            } else {
                await createEvaluation(values as CreateEvaluationRequest)
                message.success('创建成功')
            }
            setModalVisible(false)
            fetchEvaluations()
        } catch {
            message.error('操作失败')
        }
    }

    const handleSelectEvaluation = (record: EvaluationJob) => {
        setSelectedEvaluation(record)
        if (record.status === 'COMPLETED') {
            fetchResults(record.id)
        } else {
            setResults([])
            setMetrics(null)
        }
    }

    const statusMap: Record<EvaluationStatus, { color: string, text: string }> = {
        PENDING: {color: 'default', text: '待执行'},
        RUNNING: {color: 'processing', text: '运行中'},
        COMPLETED: {color: 'success', text: '已完成'},
        FAILED: {color: 'error', text: '失败'},
        CANCELLED: {color: 'warning', text: '已取消'}
    }

    const getProgress = (job: EvaluationJob) => {
        if (job.totalItems === 0) return 0
        return Math.round((job.completedItems / job.totalItems) * 100)
    }

    const evaluationColumns: ColumnsType<EvaluationJob> = [
        {title: '名称', dataIndex: 'name', key: 'name'},
        {title: '描述', dataIndex: 'description', key: 'description', ellipsis: true},
        {
            title: '状态',
            dataIndex: 'status',
            key: 'status',
            render: (status: EvaluationStatus) => (
                <Tag color={statusMap[status]?.color}>{statusMap[status]?.text}</Tag>
            )
        },
        {
            title: '进度',
            key: 'progress',
            render: (_, record) => (
                record.status === 'RUNNING' ? (
                    <Progress percent={getProgress(record)} size="small"/>
                ) : `${record.completedItems}/${record.totalItems}`
            )
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
                    {record.status === 'PENDING' && (
                        <Button type="link" icon={<PlayCircleOutlined/>} onClick={() => handleRun(record.id)}>
                            运行
                        </Button>
                    )}
                    {record.status === 'RUNNING' && (
                        <Button type="link" icon={<StopOutlined/>} onClick={() => handleCancel(record.id)}>
                            取消
                        </Button>
                    )}
                    {record.status === 'COMPLETED' && (
                        <Button type="link" icon={<BarChartOutlined/>} onClick={() => handleSelectEvaluation(record)}>
                            结果
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

    const resultColumns: ColumnsType<EvaluationResult> = [
        {title: '序号', dataIndex: 'itemIndex', key: 'itemIndex', width: 60},
        {title: '输入', dataIndex: 'input', key: 'input', ellipsis: true},
        {title: '期望输出', dataIndex: 'expectedOutput', key: 'expectedOutput', ellipsis: true},
        {title: '实际输出', dataIndex: 'actualOutput', key: 'actualOutput', ellipsis: true},
        {
            title: '得分',
            dataIndex: 'score',
            key: 'score',
            render: (score?: number) => score ?
                <Tag color={score >= 0.8 ? 'success' : 'warning'}>{score.toFixed(2)}</Tag> : '-'
        },
        {
            title: '耗时',
            dataIndex: 'latencyMs',
            key: 'latencyMs',
            render: (ms?: number) => ms ? `${ms}ms` : '-'
        }
    ]

    return (
        <div>
            <div style={{marginBottom: 16, display: 'flex', justifyContent: 'space-between'}}>
                <h2>评估系统</h2>
                <Button type="primary" icon={<PlusOutlined/>} onClick={handleCreate}>
                    新建评估任务
                </Button>
            </div>

            <Tabs defaultActiveKey="list" items={[
                {
                    key: 'list',
                    label: '评估任务列表',
                    children: (
                        <Table
                            columns={evaluationColumns}
                            dataSource={evaluations}
                            rowKey="id"
                            loading={loading}
                            onRow={(record) => ({
                                onClick: () => handleSelectEvaluation(record),
                                style: {cursor: 'pointer'}
                            })}
                        />
                    )
                },
                {
                    key: 'results',
                    label: selectedEvaluation ? `${selectedEvaluation.name} - 结果` : '评估结果（请选择任务）',
                    children: selectedEvaluation ? (
                        selectedEvaluation.status === 'COMPLETED' ? (
                            <Spin spinning={resultsLoading}>
                                <Card>
                                    <Row gutter={16} style={{marginBottom: 24}}>
                                        <Col span={4}>
                                            <Statistic title="总数" value={metrics?.totalItems || 0}/>
                                        </Col>
                                        <Col span={4}>
                                            <Statistic title="通过数" value={metrics?.passedItems || 0}
                                                       valueStyle={{color: '#3f8600'}}/>
                                        </Col>
                                        <Col span={4}>
                                            <Statistic title="失败数" value={metrics?.failedItems || 0}
                                                       valueStyle={{color: '#cf1322'}}/>
                                        </Col>
                                        <Col span={4}>
                                            <Statistic title="通过率" value={metrics?.passRate || 0} suffix="%"
                                                       precision={1}/>
                                        </Col>
                                        <Col span={4}>
                                            <Statistic title="平均耗时" value={metrics?.averageLatencyMs || 0}
                                                       suffix="ms"/>
                                        </Col>
                                        <Col span={4}>
                                            <Statistic title="平均得分" value={metrics?.averageScore || 0}
                                                       precision={2}/>
                                        </Col>
                                    </Row>
                                    <Table
                                        columns={resultColumns}
                                        dataSource={results}
                                        rowKey="id"
                                        pagination={{pageSize: 10}}
                                    />
                                </Card>
                            </Spin>
                        ) : (
                            <Card>
                                <div style={{textAlign: 'center', padding: 40}}>
                                    <Badge
                                        status={statusMap[selectedEvaluation.status]?.color === 'success' ? 'success' : 'processing'}
                                        text={statusMap[selectedEvaluation.status]?.text}/>
                                    <br/>
                                    <Progress percent={getProgress(selectedEvaluation)}
                                              style={{maxWidth: 200, margin: '16px auto'}}/>
                                </div>
                            </Card>
                        )
                    ) : (
                        <Card>
                            <div style={{textAlign: 'center', padding: 40}}>
                                请从左侧列表选择一个评估任务查看结果
                            </div>
                        </Card>
                    )
                }
            ]}/>

            <Modal
                title={editingEvaluation ? '编辑评估任务' : '新建评估任务'}
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
                    <Form.Item name="datasetId" label="数据集" rules={[{required: true}]}>
                        <Select>
                            {datasets.map(d => (
                                <Select.Option key={d.id} value={d.id}>{d.name} ({d.itemCount}项)</Select.Option>
                            ))}
                        </Select>
                    </Form.Item>
                    <Form.Item name="promptId" label="Prompt模板">
                        <Select allowClear>
                            {prompts.map(p => (
                                <Select.Option key={p.id} value={p.id}>{p.name}</Select.Option>
                            ))}
                        </Select>
                    </Form.Item>
                    <Form.Item name="modelId" label="模型">
                        <Select allowClear>
                            {models.map(m => (
                                <Select.Option key={m.id} value={m.id}>{m.name}</Select.Option>
                            ))}
                        </Select>
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    )
}

export default EvaluationPage