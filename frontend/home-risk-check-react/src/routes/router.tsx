import type { ComponentType } from 'react'
import { createBrowserRouter } from 'react-router-dom'
import RootLayout from '@/components/layouts/RootLayout'
import AuthLayout from '@/components/layouts/AuthLayout'
// PrivateLayout: 의도적으로 비활성화 상태(인증 도입 전)
// import PrivateLayout from '@/components/layouts/PrivateLayout'

const lazyPage = (factory: () => Promise<{ default: ComponentType }>) => async () => {
    const { default: Component } = await factory()
    return { Component }
}

export const router = createBrowserRouter([
    {
        element: <RootLayout />,
        children: [
            // 로그인 불필요
            { path: '/address-search', lazy: lazyPage(() => import('@/pages/AddressSearchPage')) },
            { path: '/analysis-result', lazy: lazyPage(() => import('@/pages/AnalysisResultPage')) },
            { path: '/community', lazy: lazyPage(() => import('@/pages/CommunityMainPage')) },
            { path: '/community/:postId', lazy: lazyPage(() => import('@/pages/CommunityDetailPage')) },

            // 로그인 필요 (PrivateLayout은 의도적 비활성화)
            {
                // element: <PrivateLayout/>
                children: [
                    { path: '/analysis', lazy: lazyPage(() => import('@/pages/AnalysisPage')) },
                    { path: '/community/new', lazy: lazyPage(() => import('@/pages/CommunityCreatePage')) },
                    { path: '/place-search', lazy: lazyPage(() => import('@/pages/PlaceSearchPage')) },
                    { path: '/community/poll/new', lazy: lazyPage(() => import('@/pages/PollCreatePage')) },
                ],
            },
        ],
    },
    {
        element: <AuthLayout />,
        children: [
            { path: '/login', lazy: lazyPage(() => import('@/pages/LoginPage')) },
            { path: '/signup', lazy: lazyPage(() => import('@/pages/SignupPage')) },
            { path: '/email-verify', lazy: lazyPage(() => import('@/pages/EmailVerifyPage')) },
        ],
    },
])
