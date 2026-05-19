import {BrowserRouter, Routes, Route, Navigate} from 'react-router-dom'
import Sidebar from './components/Sidebar'
import DashboardPage from './pages/DashboardPage'
import ClaimsPage from './pages/ClaimsPage'
import NewClaimPage from './pages/NewClaimPage'
import {isAuthenticated} from "./api/session.ts";
import LoginPage from "./pages/LoginPage.tsx";
import RegisterPage from "./pages/RegisterPage.tsx";
import {ProtectedRoute} from "./auth/ProtectedRoute.tsx";
import ClaimDetailPage from "./pages/ClaimDetailPage.tsx";

export default function App() {
    return (
        <BrowserRouter>
            <Sidebar />
            <main className="main-content">
                <Routes>
                    <Route
                        path="/login"
                        element={isAuthenticated() ? <Navigate to="/" replace /> : <LoginPage />}
                    />
                    <Route
                        path="/register"
                        element={isAuthenticated() ? <Navigate to="/" replace /> : <RegisterPage />}
                    />
                    <Route path="/" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
                    <Route path="/claims" element={<ProtectedRoute><ClaimsPage /></ProtectedRoute>} />
                    <Route path="/claims/new" element={<ProtectedRoute><NewClaimPage /></ProtectedRoute>} />
                    <Route path="/claims/:id" element={<ProtectedRoute><ClaimDetailPage /></ProtectedRoute>} />
                    <Route path="*" element={<Navigate to="/" replace />} />
                </Routes>
            </main>
        </BrowserRouter>
    )
}