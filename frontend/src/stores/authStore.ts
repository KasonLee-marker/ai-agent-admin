import {create} from 'zustand'

interface AuthState {
    isLoggedIn: boolean
    username: string | null
    token: string | null
    login: (username: string, password: string) => Promise<boolean>
    logout: () => void
    init: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
    isLoggedIn: false,
    username: null,
    token: null,

    login: async (username: string, password: string) => {
        // 简化登录验证
        if (username === 'admin' && password === 'admin123') {
            const token = 'token-' + Date.now()
            localStorage.setItem('token', token)
            localStorage.setItem('username', username)
            set({isLoggedIn: true, username, token})
            return true
        }
        return false
    },

    logout: () => {
        localStorage.removeItem('token')
        localStorage.removeItem('username')
        set({isLoggedIn: false, username: null, token: null})
    },

    init: () => {
        const token = localStorage.getItem('token')
        const username = localStorage.getItem('username')
        if (token && username) {
            set({isLoggedIn: true, username, token})
        }
    },
}))