import { useIsMobile } from '@/hooks/use-is-mobile.tsx'
import { AppShell } from './AppShell'
import { MobileShell } from './MobileShell'
import { ScrollRestoration } from 'react-router-dom'

export default function RootLayout() {
    const isMobile = useIsMobile()
    const Shell = isMobile ? MobileShell : AppShell

    return (
        <>
            <ScrollRestoration />
            <Shell />
        </>
    )
}