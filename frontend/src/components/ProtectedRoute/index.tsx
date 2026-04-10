import React from 'react'
import {Navigate, useLocation} from 'react-router-dom'
import {Spin} from 'antd'
import {useAuthStore} from '@/stores/authStore'

interface ProtectedRouteProps {
    children: React.ReactNode
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({children}) => {
    const {isLoggedIn, isInitialized} = useAuthStore()
    const location = useLocation()

    // 初始化完成前显示加载状态
    if (!isInitialized) {
        return (
            <div style={{
                height: '100vh',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
            }}>
                <Spin size="large"/>
            </div>
        )
    }

    if (!isLoggedIn) {
        return <Navigate to="/login" state={{from: location}} replace/>
    }

    return <>{children}</>
}

export default ProtectedRoute