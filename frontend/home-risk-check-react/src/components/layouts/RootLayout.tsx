import { useIsMobile } from '@/hooks/use-is-mobile.tsx'
import { AppShell } from './AppShell'
import { MobileShell } from './MobileShell'

export default function RootLayout() {
    const isMobile = useIsMobile()

    if (isMobile === null) return null // 초기 깜빡임 방지

    return isMobile ? <MobileShell /> : <AppShell />
}