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

export const router = createBrowserRouter([
    {
        element: <RootLayout />,
        children: [
            // { path: '/', element: <HomePage /> },
            { path: '/analysis', element: <AnalysisPage /> },
            { path: '/address-search', element: <AddressSearchPage /> },
            { path: '/analysis-result', element: <AnalysisResultPage /> },
            { path: '/community', element: <CommunityMainPage /> },
            { path: '/community/:id', element: <CommunityDetailPage /> }
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