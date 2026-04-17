import React, {useEffect, useRef, useState} from 'react'
import {
    Alert,
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
    Switch,
    Table,
    Tabs,
    Tag,
    Tooltip
} from 'antd'
import {
    BarChartOutlined,
    DeleteOutlined,
    EditOutlined,
    PlayCircleOutlined,
    PlusOutlined,
    ReloadOutlined,
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
    rerunEvaluation,
    runEvaluation,
    updateEvaluation
} from '@/api/evaluations'
import {listDatasets} from '@/api/datasets'
import {listPrompts} from '@/api/prompts'
import {listModels} from '@/api/models'
import {listAllKnowledgeBases} from '@/api/knowledgeBase'
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
    const [knowledgeBases, setKnowledgeBases] = useState<{ id: string, name: string }[]>([])
    const [loading, setLoading] = useState(false)
    const [resultsLoading, setResultsLoading] = useState(false)
    const [modalVisible, setModalVisible] = useState(false)
    const [editingEvaluation, setEditingEvaluation] = useState<EvaluationJob | null>(null)
    const [selectedEvaluation, setSelectedEvaluation] = useState<EvaluationJob | null>(null)
    const [activeTab, setActiveTab] = useState<string>('list')
    const [form] = Form.useForm()
    // 表单联动状态
    const [enableRag, setEnableRag] = useState(false)
    const [selectedDatasetId, setSelectedDatasetId] = useState<string | null>(null)
    const [selectedKnowledgeBaseId, setSelectedKnowledgeBaseId] = useState<string | null>(null)
    const [selectedEmbeddingModelId, setSelectedEmbeddingModelId] = useState<string | null>(null)

    useEffect(() => {
        fetchEvaluations()
        fetchOptions()
    }, [])

    // 静默刷新进度（不设置 loading，避免表格闪烁）
    const refreshProgress = async () => {
        try {
            const res = await listEvaluations()
            if (res.success) {
                setEvaluations(res.data.content || [])
            }
        } catch {
            // ignore
        }
    }

    // RUNNING 状态任务进度轮询（静默刷新，500ms 高频轮询）
    const pollingRef = useRef<ReturnType<typeof setInterval> | null>(null)

    useEffect(() => {
        const runningTasks = evaluations.filter(e => e.status === 'RUNNING')

        if (runningTasks.length > 0 && !pollingRef.current) {
            // 有运行中的任务且当前没有轮询，启动轮询
            pollingRef.current = setInterval(refreshProgress, 500)
        } else if (runningTasks.length === 0 && pollingRef.current) {
            // 没有运行中的任务且有轮询，停止轮询
            clearInterval(pollingRef.current)
            pollingRef.current = null
        }

        return () => {
            // 组件卸载时清理
            if (pollingRef.current) {
                clearInterval(pollingRef.current)
                pollingRef.current = null
            }
        }
    }, [evaluations])

    // 监听选中任务的完成状态，自动加载结果
    useEffect(() => {
        if (selectedEvaluation) {
            // 从最新列表中同步任务状态
            const updatedTask = evaluations.find(e => e.id === selectedEvaluation.id)
            if (updatedTask && updatedTask.status !== selectedEvaluation.status) {
                setSelectedEvaluation(updatedTask)
                // 任务完成时自动加载结果
                if (updatedTask.status === 'COMPLETED') {
                    fetchResults(updatedTask.id)
                    setActiveTab('results')
                }
            }
        }
    }, [evaluations, selectedEvaluation?.id])

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
            const [dsRes, ptRes, mdRes, kbRes] = await Promise.all([
                listDatasets(),
                listPrompts(),
                listModels(),
                listAllKnowledgeBases()
            ])
            if (dsRes.success) setDatasets(dsRes.data.content || [])
            if (ptRes.success) setPrompts(ptRes.data.content || [])
            if (mdRes.success) setModels(mdRes.data || [])
            if (kbRes.success) setKnowledgeBases((kbRes.data || []).map((kb: {
                id: string,
                name: string
            }) => ({id: kb.id, name: kb.name})))
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
        // 重置联动状态
        setEnableRag(false)
        setSelectedDatasetId(null)
        setSelectedKnowledgeBaseId(null)
        setSelectedEmbeddingModelId(null)
        setModalVisible(true)
    }

    const handleEdit = (record: EvaluationJob) => {
        setEditingEvaluation(record)
        // 设置表单值（Switch 需要单独处理 valuePropName="checked"）
        form.setFieldsValue({
            name: record.name,
            description: record.description,
            datasetId: record.datasetId,
            promptTemplateId: record.promptTemplateId,
            modelConfigId: record.modelConfigId,
            embeddingModelId: record.embeddingModelId,
            knowledgeBaseId: record.knowledgeBaseId,
            enableRag: record.enableRag || false
        })
        // 初始化联动状态
        setEnableRag(record.enableRag || false)
        setSelectedDatasetId(record.datasetId || null)
        setSelectedKnowledgeBaseId(record.knowledgeBaseId || null)
        setSelectedEmbeddingModelId(record.embeddingModelId || null)
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

    const handleRerun = async (id: string) => {
        try {
            await rerunEvaluation(id)
            message.success('重新评估任务已启动')
            // 清空旧结果
            setResults([])
            setMetrics(null)
            // 保持在列表页，不跳转到结果页
            setActiveTab('list')
            // 重新获取列表，fetchEvaluations 会更新 evaluations，然后 useEffect 会同步 selectedEvaluation
            fetchEvaluations()
        } catch {
            message.error('重新评估失败')
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
            setActiveTab('results')  // 自动切换到结果 Tab
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

    // 获取数据集选择后的提示
    const getDatasetHint = (): { type: 'info' | 'warning', message: string } | null => {
        if (!selectedDatasetId) return null
        const dataset = datasets.find(d => d.id === selectedDatasetId)
        if (!dataset) return null

        // 查询数据集的数据项是否有output字段
        // 这里简化处理：如果itemCount > 0则认为有期望输出
        if (dataset.itemCount > 0) {
            return {
                type: 'info',
                message: '建议选择Embedding模型以计算语义相似度'
            }
        } else {
            return {
                type: 'warning',
                message: '当前数据集无期望输出，无法计算语义相似度和AI评分'
            }
        }
    }

    // 获取Embedding模型提示
    const getEmbeddingHint = (): string | undefined => {
        if (!selectedEmbeddingModelId) {
            if (enableRag) {
                return 'RAG评估建议选择Embedding模型计算语义相似度'
            }
            const datasetHint = getDatasetHint()
            if (datasetHint && datasetHint.type === 'info') {
                return '建议选择Embedding模型'
            }
        }
        return undefined
    }

    const getProgress = (job: EvaluationJob) => {
        if (job.totalItems === 0) return 0
        return Math.round((job.completedItems / job.totalItems) * 100)
    }

    const evaluationColumns: ColumnsType<EvaluationJob> = [
        {title: '名称', dataIndex: 'name', key: 'name', width: 120, ellipsis: true},
        {title: '描述', dataIndex: 'description', key: 'description', width: 100, ellipsis: true},
        {
            title: '状态',
            dataIndex: 'status',
            key: 'status',
            width: 80,
            render: (status: EvaluationStatus) => (
                <Tag color={statusMap[status]?.color}>{statusMap[status]?.text}</Tag>
            )
        },
        {
            title: '进度',
            key: 'progress',
            width: 150,
            render: (_, record) => (
                record.status === 'RUNNING' ? (
                    <Progress
                        percent={getProgress(record)}
                        size="small"
                        status="active"
                        strokeColor={{
                            '0%': '#108ee9',
                            '100%': '#87d068',
                        }}
                        strokeWidth={6}
                        format={() => `${record.completedItems}/${record.totalItems}`}
                    />
                ) : `${record.completedItems}/${record.totalItems}`
            )
        },
        {
            title: '创建时间',
            dataIndex: 'createdAt',
            key: 'createdAt',
            width: 160,
            render: (date: string) => new Date(date).toLocaleString()
        },
        {
            title: '更新时间',
            dataIndex: 'updatedAt',
            key: 'updatedAt',
            width: 160,
            render: (date: string) => new Date(date).toLocaleString()
        },
        {
            title: '操作',
            key: 'action',
            width: 120,
            fixed: 'right',
            render: (_, record) => (
                <Space onClick={(e) => e.stopPropagation()}>
                    {record.status === 'PENDING' && (
                        <Tooltip title="运行">
                            <Button type="link" icon={<PlayCircleOutlined/>} onClick={() => handleRun(record.id)} />
                        </Tooltip>
                    )}
                    {record.status === 'RUNNING' && (
                        <Tooltip title="取消">
                            <Button type="link" icon={<StopOutlined/>} onClick={() => handleCancel(record.id)} />
                        </Tooltip>
                    )}
                    {(record.status === 'COMPLETED' || record.status === 'FAILED' || record.status === 'CANCELLED') && (
                        <>
                            <Tooltip title="重新评估">
                                <Button type="link" icon={<ReloadOutlined/>} onClick={() => handleRerun(record.id)} />
                            </Tooltip>
                            <Tooltip title="查看结果">
                                <Button type="link" icon={<BarChartOutlined/>}
                                        onClick={() => handleSelectEvaluation(record)} />
                            </Tooltip>
                        </>
                    )}
                    <Tooltip title="编辑">
                        <Button type="link" icon={<EditOutlined/>} onClick={() => handleEdit(record)} />
                    </Tooltip>
                    <Popconfirm title="确定删除?" onConfirm={() => handleDelete(record.id)}>
                        <Tooltip title="删除">
                            <Button type="link" danger icon={<DeleteOutlined/>} />
                        </Tooltip>
                    </Popconfirm>
                </Space>
            )
        }
    ]

    const resultColumns: ColumnsType<EvaluationResult> = [
        {title: '序号', key: 'index', width: 60, render: (_, __, index) => index + 1},
        {title: '输入', dataIndex: 'input', key: 'input', ellipsis: true},
        {title: '期望输出', dataIndex: 'expectedOutput', key: 'expectedOutput', ellipsis: true},
        {title: '实际输出', dataIndex: 'actualOutput', key: 'actualOutput', ellipsis: true},
        {
            title: 'AI得分',
            dataIndex: 'score',
            key: 'score',
            width: 80,
            render: (score?: number) => score ?
                <Tag color={score >= 80 ? 'success' : score >= 50 ? 'warning' : 'error'}>{score.toFixed(0)}</Tag> : '-'
        },
        {
            title: '相似度',
            dataIndex: 'semanticSimilarity',
            key: 'semanticSimilarity',
            width: 80,
            render: (sim?: number) => sim ?
                <Tag
                    color={sim >= 0.8 ? 'success' : sim >= 0.5 ? 'warning' : 'error'}>{(sim * 100).toFixed(0)}%</Tag> : '-'
        },
        {
            title: '检索得分',
            dataIndex: 'retrievalScore',
            key: 'retrievalScore',
            width: 80,
            render: (score?: number) => score ?
                <Tag
                    color={score >= 0.8 ? 'success' : score >= 0.5 ? 'warning' : 'error'}>{(score * 100).toFixed(0)}%</Tag> : '-'
        },
        {
            title: '忠实度',
            dataIndex: 'faithfulness',
            key: 'faithfulness',
            width: 80,
            render: (f?: number) => f ?
                <Tag color={f >= 0.8 ? 'success' : f >= 0.5 ? 'warning' : 'error'}>{(f * 100).toFixed(0)}%</Tag> : '-'
        },
        {
            title: '评分理由',
            dataIndex: 'scoreReason',
            key: 'scoreReason',
            ellipsis: true,
            render: (reason?: string) => reason || '-'
        },
        {
            title: '耗时',
            dataIndex: 'latencyMs',
            key: 'latencyMs',
            width: 80,
            render: (ms?: number) => ms ? `${ms}ms` : '-'
        }
    ]

    return (
        <div>
            <div style={{marginBottom: 16, display: 'flex', justifyContent: 'space-between'}}>
                <div>
                    <h2 style={{marginBottom: 0}}>评估系统</h2>
                    <p style={{color: '#666', marginTop: 4}}>选择提示词、模型和数据集运行评估任务，对比不同配置的效果</p>
                </div>
                <Space>
                    {activeTab === 'results' && selectedEvaluation && (
                        <Button onClick={() => {
                            setActiveTab('list');
                            setSelectedEvaluation(null);
                            setResults([]);
                            setMetrics(null);
                        }}>
                            返回列表
                        </Button>
                    )}
                    <Button type="primary" icon={<PlusOutlined/>} onClick={handleCreate}>
                        新建评估任务
                    </Button>
                </Space>
            </div>

            <Tabs activeKey={activeTab} onChange={setActiveTab} items={[
                {
                    key: 'list',
                    label: '评估任务列表',
                    children: (
                        <Table
                            columns={evaluationColumns}
                            dataSource={evaluations}
                            rowKey="id"
                            loading={loading}
                            scroll={{x: 1200}}
                            onRow={(record) => ({
                                onClick: () => handleSelectEvaluation(record),
                                style: {cursor: 'pointer'}
                            })}
                        />
                    )
                },
                ...(selectedEvaluation && selectedEvaluation.status === 'COMPLETED' ? [{
                    key: 'results',
                    label: `${selectedEvaluation.name} - 结果`,
                    children: (
                        <Spin spinning={resultsLoading}>
                            <Card>
                                <Row gutter={16} style={{marginBottom: 24}}>
                                    <Col span={4}>
                                        <Statistic title="总数" value={metrics?.totalItems || 0}/>
                                    </Col>
                                    <Col span={4}>
                                        <Statistic title="成功数" value={metrics?.successCount || 0}
                                                   valueStyle={{color: '#3f8600'}}/>
                                    </Col>
                                    <Col span={4}>
                                        <Statistic title="失败数" value={metrics?.failedCount || 0}
                                                   valueStyle={{color: '#cf1322'}}/>
                                    </Col>
                                    <Col span={4}>
                                        <Statistic title="成功率" value={metrics?.successRate || 0} suffix="%"
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
                    )
                }] : [])
            ]}/>

            <Modal
                title={editingEvaluation ? '编辑评估任务' : '新建评估任务'}
                open={modalVisible}
                onOk={handleSubmit}
                onCancel={() => setModalVisible(false)}
                width={650}
            >
                <Form form={form} layout="vertical">
                    <Form.Item name="name" label="名称" rules={[{required: true, message: '名称不能为空'}]}>
                        <Input maxLength={200} showCount/>
                    </Form.Item>
                    <Form.Item name="description" label="描述">
                        <Input.TextArea rows={2} maxLength={1000} showCount/>
                    </Form.Item>
                    <Form.Item
                        name="datasetId"
                        label="数据集"
                        rules={[{required: true, message: '请选择数据集'}]}
                    >
                        <Select
                            onChange={(value) => setSelectedDatasetId(value)}
                            placeholder="请选择数据集"
                        >
                            {datasets.map(d => (
                                <Select.Option key={d.id} value={d.id}>
                                    {d.name} ({d.itemCount}项)
                                </Select.Option>
                            ))}
                        </Select>
                    </Form.Item>
                    {/* 数据集选择后的提示 */}
                    {selectedDatasetId && getDatasetHint() && (
                        <Alert
                            type={getDatasetHint()!.type}
                            message={getDatasetHint()!.message}
                            showIcon
                            style={{marginBottom: 16}}
                        />
                    )}
                    <Form.Item name="promptTemplateId" label="Prompt模板" help="可选，不选则使用系统默认模板">
                        <Select allowClear placeholder="可选，不选则使用默认模板">
                            {prompts.map(p => (
                                <Select.Option key={p.id} value={p.id}>{p.name}</Select.Option>
                            ))}
                        </Select>
                    </Form.Item>
                    <Form.Item name="modelConfigId" label="对话模型" help="可选，不选则使用系统默认对话模型">
                        <Select allowClear placeholder="可选，不选则使用系统默认">
                            {models.filter(m => m.modelType === 'CHAT').map(m => (
                                <Select.Option key={m.id} value={m.id}>{m.name}</Select.Option>
                            ))}
                        </Select>
                    </Form.Item>
                    <Form.Item
                        name="embeddingModelId"
                        label="Embedding模型"
                        help={getEmbeddingHint()}
                    >
                        <Select
                            allowClear
                            placeholder="用于计算语义相似度"
                            onChange={(value) => setSelectedEmbeddingModelId(value)}
                        >
                            {models.filter(m => m.modelType === 'EMBEDDING').map(m => (
                                <Select.Option key={m.id} value={m.id}>{m.name}</Select.Option>
                            ))}
                        </Select>
                    </Form.Item>
                    <Form.Item
                        name="knowledgeBaseId"
                        label={
                            enableRag
                                ? <span>知识库 <span style={{color: '#ff4d4f'}}>*</span></span>
                                : '知识库（RAG评估）'
                        }
                        rules={[{
                            required: enableRag,
                            message: '启用RAG评估时必须选择知识库'
                        }]}
                    >
                        <Select
                            allowClear
                            placeholder={enableRag ? '请选择知识库' : '可选，用于RAG评估'}
                            onChange={(value) => setSelectedKnowledgeBaseId(value)}
                            style={enableRag && !selectedKnowledgeBaseId ? {borderColor: '#1890ff'} : undefined}
                        >
                            {knowledgeBases.map(kb => (
                                <Select.Option key={kb.id} value={kb.id}>{kb.name}</Select.Option>
                            ))}
                        </Select>
                    </Form.Item>
                    <Form.Item
                        name="enableRag"
                        label="启用RAG评估"
                        valuePropName="checked"
                        help={enableRag ? '开启后将检索知识库内容，计算检索指标和语义相似度' : '开启后需要选择知识库'}
                    >
                        <Switch
                            onChange={(checked) => {
                                setEnableRag(checked)
                                // 开启RAG时，如果知识库未选择，触发验证
                                if (checked && !selectedKnowledgeBaseId) {
                                    form.validateFields(['knowledgeBaseId']).catch(() => {
                                    })
                                }
                            }}
                        />
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    )
}

export default EvaluationPage