// components/PrivateRoute.tsx
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '@/hooks/use-auth'

export default function PrivateLayout() {
    const { user, isLoading } = useAuth()
    const location = useLocation()

    if (isLoading) return (
        <div className="flex items-center justify-center h-screen">
            <span className="text-gray-400">로딩 중...</span>
        </div>
    )

    if (!user) return (
        // 로그인 후 원래 페이지로 돌아오기 위해 현재 경로 저장
        <Navigate to="/login" state={{ from: location.pathname }} replace />
    )

    return <Outlet />
}