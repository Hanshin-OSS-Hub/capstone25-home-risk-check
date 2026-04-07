import { createBrowserRouter } from 'react-router-dom'
import { RootLayout } from '@/components/layouts/RootLayout'
// import { HomePage } from '@/pages/HomePage'
import AnalysisPage from '@/pages/AnalysisPage'
import AddressSearchPage from '@/pages/AddressSearchPage'
import AnalysisResultPage from '@/pages/AnalysisResultPage'
export const router = createBrowserRouter([
    {
        element: <RootLayout />,
        children: [
            // { path: '/', element: <HomePage /> },
            { path: '/analysis', element: <AnalysisPage /> },
            { path: '/address-search', element: <AddressSearchPage /> },
            { path: '/analysis-result', element: <AnalysisResultPage /> },
        ],
    },
])