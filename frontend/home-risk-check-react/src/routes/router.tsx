import { createBrowserRouter } from 'react-router-dom'
import RootLayout from '@/components/layouts/RootLayout'
import AuthLayout from '@/components/layouts/AuthLayout'
// import { HomePage } from '@/pages/HomePage'
import AnalysisPage from '@/pages/AnalysisPage'
import AddressSearchPage from '@/pages/AddressSearchPage'
import AnalysisResultPage from '@/pages/AnalysisResultPage'
import LoginPage from '@/pages/LoginPage'
import SignupPage from '@/pages/SignupPage'
import EmailVerifyPage from '@/pages/EmailVerifyPage'
import CommunityMainPage from '@/pages/CommunityMainPage'
import CommunityDetailPage from '@/pages/CommunityDetailPage'
import CommunityCreatePage from '@/pages/CommunityCreatePage'
import PlaceSearchPage from '@/pages/PlaceSearchPage'
import PollCreatePage from '@/pages/PollCreatePage'
import PrivateLayout from '@/components/layouts/PrivateLayout'

export const router = createBrowserRouter([
    {
        element: <RootLayout />,
        children: [
            // 로그인 불필요
            { path: '/address-search', element: <AddressSearchPage /> },
            { path: '/analysis-result', element: <AnalysisResultPage /> },
            { path: '/community', element: <CommunityMainPage /> },
            { path: '/community/:id', element: <CommunityDetailPage /> },

            // 로그인 필요
            {
                element: <PrivateLayout />,
                children: [
                    { path: '/analysis', element: <AnalysisPage /> },
                    { path: '/community/new', element: <CommunityCreatePage /> },
                    { path: '/place-search', element: <PlaceSearchPage /> },
                    { path: '/community/poll/new', element: <PollCreatePage /> },
                ]
            }
        ],
    },
    {
        element: <AuthLayout />,
        children: [
            { path: '/login', element: <LoginPage /> },
            { path: '/signup', element: <SignupPage /> },
            { path: '/email-verify', element: <EmailVerifyPage/>}
        ],
    }
])