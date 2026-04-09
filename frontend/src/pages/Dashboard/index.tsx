import React from 'react'
import {Card, Row, Col, Statistic} from 'antd'
import {FileTextOutlined, CloudServerOutlined, MessageOutlined, DatabaseOutlined} from '@ant-design/icons'

const DashboardPage: React.FC = () => {
    return (
        <div>
            <h2 style={{marginBottom: 24}}>仪表盘</h2>
            <Row gutter={16}>
                <Col span={6}>
                    <Card>
                        <Statistic
                            title="Prompt 模板"
                            value={0}
                            prefix={<FileTextOutlined/>}
                        />
                    </Card>
                </Col>
                <Col span={6}>
                    <Card>
                        <Statistic
                            title="模型配置"
                            value={0}
                            prefix={<CloudServerOutlined/>}
                        />
                    </Card>
                </Col>
                <Col span={6}>
                    <Card>
                        <Statistic
                            title="对话会话"
                            value={0}
                            prefix={<MessageOutlined/>}
                        />
                    </Card>
                </Col>
                <Col span={6}>
                    <Card>
                        <Statistic
                            title="数据集"
                            value={0}
                            prefix={<DatabaseOutlined/>}
                        />
                    </Card>
                </Col>
            </Row>

            <Card style={{marginTop: 24}}>
                <h3>欢迎使用 AI Agent Admin</h3>
                <p>这是一个企业内网 AI Agent 管理平台，提供以下功能：</p>
                <ul>
                    <li><strong>Prompt 管理</strong> - 创建、编辑、版本控制 Prompt 模板</li>
                    <li><strong>模型管理</strong> - 配置和管理多个 AI 模型</li>
                    <li><strong>对话调试</strong> - 实时测试和调试 AI 对话</li>
                    <li><strong>数据集管理</strong> - 管理训练和测试数据</li>
                    <li><strong>评估系统</strong> - 评估和比较模型性能</li>
                    <li><strong>文档管理</strong> - 上传和管理知识库文档</li>
                    <li><strong>RAG 对话</strong> - 基于知识库的智能问答</li>
                </ul>
            </Card>
        </div>
    )
}

export default DashboardPage