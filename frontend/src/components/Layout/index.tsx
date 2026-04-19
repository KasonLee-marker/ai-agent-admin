import React, {useState} from 'react'
import type {MenuProps} from 'antd'
import {Layout, Menu, theme} from 'antd'
import {
    CloudServerOutlined,
    DashboardOutlined,
    DatabaseOutlined,
    FileTextOutlined,
    FolderOutlined,
    LineChartOutlined,
    LogoutOutlined,
    MessageOutlined,
} from '@ant-design/icons'
import {Outlet, useLocation, useNavigate} from 'react-router-dom'
import {useAuthStore} from '@/stores/authStore'

const {Header, Sider, Content} = Layout

const menuItems: MenuProps['items'] = [
    {key: '/', icon: <DashboardOutlined/>, label: '仪表盘'},
    {
        key: 'config-group',
        type: 'group' as const,
        label: '基础配置',
        children: [
            {key: '/models', icon: <CloudServerOutlined/>, label: '模型管理'},
            {key: '/prompts', icon: <FileTextOutlined/>, label: 'Prompt 管理'},
        ]
    },
    {
        key: 'test-group',
        type: 'group' as const,
        label: '测试调试',
        children: [
            {key: '/chat', icon: <MessageOutlined/>, label: '对话调试'},
        ]
    },
    {
        key: 'eval-group',
        type: 'group' as const,
        label: '评估优化',
        children: [
            {key: '/datasets', icon: <DatabaseOutlined/>, label: '数据集管理'},
            {key: '/evaluations', icon: <LineChartOutlined/>, label: '评估系统'},
        ]
    },
    {
        key: 'kb-group',
        type: 'group' as const,
        label: '知识库',
        children: [
            {key: '/knowledge-bases', icon: <FolderOutlined/>, label: '知识库管理'},
        ]
    },
]

const MainLayout: React.FC = () => {
    const [collapsed, setCollapsed] = useState(false)
    const navigate = useNavigate()
    const location = useLocation()
    const {username, logout} = useAuthStore()
    const {token: {colorBgContainer, borderRadiusLG}} = theme.useToken()

    const handleMenuClick = ({key}: { key: string }) => {
        navigate(key)
    }

    const handleLogout = () => {
        logout()
        navigate('/login')
    }

    return (
        <Layout style={{minHeight: '100vh'}}>
            <Sider collapsible collapsed={collapsed} onCollapse={setCollapsed}>
                <div style={{
                    height: 32,
                    margin: 16,
                    background: 'rgba(255, 255, 255, 0.2)',
                    borderRadius: 6,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: '#fff',
                    fontWeight: 'bold',
                }}>
                    {collapsed ? 'AI' : 'AI Agent Admin'}
                </div>
                <Menu
                    theme="dark"
                    selectedKeys={[location.pathname]}
                    mode="inline"
                    items={menuItems}
                    onClick={handleMenuClick}
                />
            </Sider>
            <Layout>
                <Header style={{
                    padding: '0 24px',
                    background: colorBgContainer,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                }}>
                    <span style={{fontSize: 18, fontWeight: 500}}>AI Agent Admin</span>
                    <div style={{display: 'flex', alignItems: 'center', gap: 16}}>
                        <span>欢迎, {username}</span>
                        <LogoutOutlined
                            style={{cursor: 'pointer', fontSize: 16}}
                            onClick={handleLogout}
                        />
                    </div>
                </Header>
                <Content style={{margin: '16px'}}>
                    <div style={{
                        padding: 24,
                        minHeight: 360,
                        background: colorBgContainer,
                        borderRadius: borderRadiusLG,
                    }}>
                        <Outlet/>
                    </div>
                </Content>
            </Layout>
        </Layout>
    )
}

export default MainLayout