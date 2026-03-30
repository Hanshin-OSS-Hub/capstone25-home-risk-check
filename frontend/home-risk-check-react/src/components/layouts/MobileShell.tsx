//모바일 화면 레이아웃
import { Outlet } from 'react-router-dom'
// import { BottomTabBar } from './components/BottomTabBar'

export function MobileShell() {
    return (
        <div className="flex flex-col h-dvh">
            <main className="flex-1 overflow-y-auto">
                <Outlet />
            </main>
            {/*<BottomTabBar />*/}
        </div>
    )
}