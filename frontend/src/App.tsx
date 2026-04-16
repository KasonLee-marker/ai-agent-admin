import React, {useEffect} from 'react'
import {BrowserRouter, Navigate, Route, Routes} from 'react-router-dom'
import MainLayout from '@/components/Layout'
import ProtectedRoute from '@/components/ProtectedRoute'
import LoginPage from '@/pages/Login'
import DashboardPage from '@/pages/Dashboard'
import PromptListPage from '@/pages/Prompts'
import ModelListPage from '@/pages/Models'
import ChatPage from '@/pages/Chat'
import DatasetPage from '@/pages/Datasets'
import EvaluationPage from '@/pages/Evaluations'
import DocumentPage from '@/pages/Documents'
import RagPage from '@/pages/Rag'
import KnowledgeBasesPage from '@/pages/KnowledgeBases'
import {useAuthStore} from '@/stores/authStore'

const App: React.FC = () => {
    const {init} = useAuthStore()

    useEffect(() => {
        init()
    }, [init])

    return (
        <BrowserRouter>
            <Routes>
                <Route path="/login" element={<LoginPage/>}/>
                <Route
                    path="/"
                    element={
                        <ProtectedRoute>
                            <MainLayout/>
                        </ProtectedRoute>
                    }
                >
                    <Route index element={<DashboardPage/>}/>
                    <Route path="prompts" element={<PromptListPage/>}/>
                    <Route path="models" element={<ModelListPage/>}/>
                    <Route path="chat" element={<ChatPage/>}/>
                    <Route path="datasets" element={<DatasetPage/>}/>
                    <Route path="evaluations" element={<EvaluationPage/>}/>
                    <Route path="documents" element={<DocumentPage/>}/>
                    <Route path="knowledge-bases" element={<KnowledgeBasesPage/>}/>
                    <Route path="rag" element={<RagPage/>}/>
                </Route>
                <Route path="*" element={<Navigate to="/" replace/>}/>
            </Routes>
        </BrowserRouter>
    )
}

export default App