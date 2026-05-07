import type { ComponentType } from 'react'
import { createBrowserRouter } from 'react-router-dom'
import RootLayout from '@/components/layouts/RootLayout'
import AuthLayout from '@/components/layouts/AuthLayout'
import { ROUTES, ROUTE_PATTERNS } from '@/constants/routes'
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
            { path: ROUTES.addressSearch, lazy: lazyPage(() => import('@/pages/AddressSearchPage')) },
            { path: ROUTE_PATTERNS.analysisResult, lazy: lazyPage(() => import('@/pages/AnalysisResultPage')) },
            { path: ROUTES.community, lazy: lazyPage(() => import('@/pages/CommunityMainPage')) },
            { path: ROUTE_PATTERNS.communityDetail, lazy: lazyPage(() => import('@/pages/CommunityDetailPage')) },

            // 로그인 필요 (PrivateLayout은 의도적 비활성화)
            {
                // element: <PrivateLayout/>,
                children: [
                    { path: ROUTES.analysis, lazy: lazyPage(() => import('@/pages/AnalysisPage')) },
                    { path: ROUTES.communityNew, lazy: lazyPage(() => import('@/pages/CommunityCreatePage')) },
                    { path: ROUTES.placeSearch, lazy: lazyPage(() => import('@/pages/PlaceSearchPage')) },
                    { path: ROUTES.communityPollNew, lazy: lazyPage(() => import('@/pages/PollCreatePage')) },
                ],
            },
        ],
    },
    {
        element: <AuthLayout />,
        children: [
            { path: ROUTES.login, lazy: lazyPage(() => import('@/pages/LoginPage')) },
            { path: ROUTES.signup, lazy: lazyPage(() => import('@/pages/SignupPage')) },
            { path: ROUTES.emailVerify, lazy: lazyPage(() => import('@/pages/EmailVerifyPage')) },
        ],
    },
])
