import { useIsMobile } from '@/hooks/use-is-mobile.tsx'
import { AppShell } from './AppShell'
import { MobileShell } from './MobileShell'
import { ScrollRestoration } from 'react-router-dom'

export default function RootLayout() {
    const isMobile = useIsMobile()

    if (isMobile === null) return null // 초기 깜빡임 방지

    return isMobile ? <><ScrollRestoration /><MobileShell/></> : <><ScrollRestoration/><AppShell/></>
}