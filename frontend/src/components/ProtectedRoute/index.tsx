import React from 'react'
import {Navigate, useLocation} from 'react-router-dom'
import {useAuthStore} from '@/stores/authStore'

interface ProtectedRouteProps {
    children: React.ReactNode
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({children}) => {
    const {isLoggedIn} = useAuthStore()
    const location = useLocation()

    if (!isLoggedIn) {
        return <Navigate to="/login" state={{from: location}} replace/>
    }

    return <>{children}</>
}

export default ProtectedRoute