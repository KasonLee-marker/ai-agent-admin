import React, {useState} from 'react'
import {Form, Input, Button, Card, message} from 'antd'
import {UserOutlined, LockOutlined} from '@ant-design/icons'
import {useNavigate} from 'react-router-dom'
import {useAuthStore} from '@/stores/authStore'

const LoginPage: React.FC = () => {
    const [loading, setLoading] = useState(false)
    const navigate = useNavigate()
    const {login} = useAuthStore()

    const onFinish = async (values: { username: string; password: string }) => {
        setLoading(true)
        try {
            const success = await login(values.username, values.password)
            if (success) {
                message.success('登录成功')
                navigate('/')
            } else {
                message.error('用户名或密码错误')
            }
        } finally {
            setLoading(false)
        }
    }

    return (
        <div style={{
            height: '100vh',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            background: '#f0f2f5',
        }}>
            <Card title="AI Agent Admin 登录" style={{width: 400}}>
                <Form
                    name="login"
                    onFinish={onFinish}
                    autoComplete="off"
                    size="large"
                >
                    <Form.Item
                        name="username"
                        rules={[{required: true, message: '请输入用户名'}]}
                    >
                        <Input prefix={<UserOutlined/>} placeholder="用户名: admin"/>
                    </Form.Item>

                    <Form.Item
                        name="password"
                        rules={[{required: true, message: '请输入密码'}]}
                    >
                        <Input.Password prefix={<LockOutlined/>} placeholder="密码: admin123"/>
                    </Form.Item>

                    <Form.Item>
                        <Button type="primary" htmlType="submit" loading={loading} block>
                            登录
                        </Button>
                    </Form.Item>
                </Form>
                <div style={{textAlign: 'center', color: '#999'}}>
                    默认账号: admin / admin123
                </div>
            </Card>
        </div>
    )
}

export default LoginPage