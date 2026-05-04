import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import { router } from '@/routes/router.tsx'
import { RouterProvider } from 'react-router-dom'
import { QueryProvider } from '@/app/providers/QueryProvider'
import { ToasterProvider } from '@/app/providers/ToasterProvider'

createRoot(document.getElementById('root')!).render(
    <StrictMode>
        <QueryProvider>
            <RouterProvider router={router} />
            <ToasterProvider />
        </QueryProvider>
    </StrictMode>,
)
