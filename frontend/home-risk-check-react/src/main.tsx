import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import { router } from '@/routes/router.tsx'
import { RouterProvider } from 'react-router-dom'
import { QueryProvider } from '@/app/providers/QueryProvider'
import { ToasterProvider } from '@/app/providers/ToasterProvider'
import { queryClient } from '@/app/providers/QueryProvider'
import { ROUTES } from '@/constants/routes'
import { AUTH_QUERY_KEY } from '@/features/auth/hooks/useAuth'

// 인터셉터에서 발행하는 인증 만료 이벤트 — 캐시 무효화 + 라우터 이동
window.addEventListener('auth:expired', () => {
    queryClient.setQueryData(AUTH_QUERY_KEY, null)
    void router.navigate(ROUTES.login, { replace: true })
})

createRoot(document.getElementById('root')!).render(
    <StrictMode>
        <QueryProvider>
            <RouterProvider router={router} />
            <ToasterProvider />
        </QueryProvider>
    </StrictMode>,
)
