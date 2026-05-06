//모바일 화면 레이아웃
import { Outlet } from 'react-router-dom'
// import { BottomTabBar } from './components/BottomTabBar'

export function MobileShell() {
    return (
        <div className="flex flex-col h-dvh">
            <main className="flex-1 overflow-y-auto">
                <div className="px-4 my-6">
                    <div className="flex flex-col gap-6 max-w-(--breakpoint-sm) mx-auto w-full">
                        <Outlet />
                    </div>
                </div>
            </main>
            {/*<BottomTabBar />*/}
        </div>
    )
}