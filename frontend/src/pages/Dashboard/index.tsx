import React, {useEffect, useState} from 'react'
import {Card, Col, Row, Spin, Statistic} from 'antd'
import {
    CheckCircleOutlined,
    CloudServerOutlined,
    DatabaseOutlined,
    FileSearchOutlined,
    FileTextOutlined,
    MessageOutlined
} from '@ant-design/icons'
import {listPrompts} from '@/api/prompts'
import {listModels} from '@/api/models'
import {listSessions} from '@/api/chat'
import {listDatasets} from '@/api/datasets'
import {listDocuments} from '@/api/documents'

const DashboardPage: React.FC = () => {
    const [loading, setLoading] = useState(true)
    const [stats, setStats] = useState({
        promptCount: 0,
        modelCount: 0,
        sessionCount: 0,
        datasetCount: 0,
        documentCount: 0
    })

    useEffect(() => {
        fetchStats()
    }, [])

    const fetchStats = async () => {
        setLoading(true)
        try {
            const [promptsRes, modelsRes, sessionsRes, datasetsRes, documentsRes] = await Promise.all([
                listPrompts().catch(() => ({success: false, data: {content: [], totalElements: 0}})),
                listModels().catch(() => ({success: false, data: []})),
                listSessions().catch(() => ({success: false, data: {content: [], totalElements: 0}})),
                listDatasets().catch(() => ({success: false, data: {content: [], totalElements: 0}})),
                listDocuments().catch(() => ({success: false, data: {content: [], totalElements: 0}}))
            ])

            setStats({
                promptCount: promptsRes.success ? (promptsRes.data.totalElements || promptsRes.data.content?.length || 0) : 0,
                modelCount: modelsRes.success ? (modelsRes.data.length || 0) : 0,
                sessionCount: sessionsRes.success ? (sessionsRes.data.totalElements || sessionsRes.data.content?.length || 0) : 0,
                datasetCount: datasetsRes.success ? (datasetsRes.data.totalElements || datasetsRes.data.content?.length || 0) : 0,
                documentCount: documentsRes.success ? (documentsRes.data.totalElements || documentsRes.data.content?.length || 0) : 0
            })
        } finally {
            setLoading(false)
        }
    }

    return (
        <div>
            <h2 style={{marginBottom: 24}}>仪表盘</h2>
            <Spin spinning={loading}>
                <Row gutter={16}>
                    <Col span={4}>
                        <Card>
                            <Statistic
                                title="Prompt 模板"
                                value={stats.promptCount}
                                prefix={<FileTextOutlined/>}
                            />
                        </Card>
                    </Col>
                    <Col span={4}>
                        <Card>
                            <Statistic
                                title="模型配置"
                                value={stats.modelCount}
                                prefix={<CloudServerOutlined/>}
                            />
                        </Card>
                    </Col>
                    <Col span={4}>
                        <Card>
                            <Statistic
                                title="对话会话"
                                value={stats.sessionCount}
                                prefix={<MessageOutlined/>}
                            />
                        </Card>
                    </Col>
                    <Col span={4}>
                        <Card>
                            <Statistic
                                title="数据集"
                                value={stats.datasetCount}
                                prefix={<DatabaseOutlined/>}
                            />
                        </Card>
                    </Col>
                    <Col span={4}>
                        <Card>
                            <Statistic
                                title="文档数量"
                                value={stats.documentCount}
                                prefix={<FileSearchOutlined/>}
                            />
                        </Card>
                    </Col>
                    <Col span={4}>
                        <Card>
                            <Statistic
                                title="系统状态"
                                value="正常"
                                prefix={<CheckCircleOutlined style={{color: '#52c41a'}}/>}
                                valueStyle={{color: '#52c41a'}}
                            />
                        </Card>
                    </Col>
                </Row>
            </Spin>

            <Card style={{marginTop: 24}}>
                <h3>欢迎使用 AI Agent Admin</h3>
                <p>这是一个企业内网 AI Agent 管理平台，提供以下功能：</p>
                <Row gutter={[16, 16]}>
                    <Col span={12}>
                        <Card size="small" title="Prompt 管理">
                            创建、编辑、版本控制 Prompt 模板，支持变量占位符和分类标签。
                        </Card>
                    </Col>
                    <Col span={12}>
                        <Card size="small" title="模型管理">
                            配置和管理多个 AI 模型，支持 OpenAI、DashScope 等供应商。
                        </Card>
                    </Col>
                    <Col span={12}>
                        <Card size="small" title="对话调试">
                            实时测试和调试 AI 对话，支持多轮对话和流式响应。
                        </Card>
                    </Col>
                    <Col span={12}>
                        <Card size="small" title="数据集管理">
                            管理训练和测试数据，支持导入导出和版本控制。
                        </Card>
                    </Col>
                    <Col span={12}>
                        <Card size="small" title="评估系统">
                            评估和比较模型性能，支持批量测试和 A/B 对比。
                        </Card>
                    </Col>
                    <Col span={12}>
                        <Card size="small" title="文档管理 & RAG">
                            上传知识库文档，支持向量检索和 RAG 智能问答。
                        </Card>
                    </Col>
                </Row>
            </Card>
        </div>
    )
}

export default DashboardPage