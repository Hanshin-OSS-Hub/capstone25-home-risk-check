import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import { router } from '@/routes/router.tsx'
import { RouterProvider } from 'react-router-dom'
import { QueryProvider } from '@/app/providers/QueryProvider'
import { ToasterProvider } from '@/app/providers/ToasterProvider'

// 인터셉터에서 발행하는 인증 만료 이벤트를 라우터로 위임
window.addEventListener('auth:expired', () => {
    void router.navigate('/login', { replace: true })
})

createRoot(document.getElementById('root')!).render(
    <StrictMode>
        <QueryProvider>
            <RouterProvider router={router} />
            <ToasterProvider />
        </QueryProvider>
    </StrictMode>,
)
