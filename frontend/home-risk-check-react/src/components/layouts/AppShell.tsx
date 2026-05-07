//웹, 태블릿 화면 레이아웃
import { Outlet } from 'react-router-dom'
import Navbar from "@/components/layouts/Navbar"

export function AppShell() {
    return (
        <>
            <Navbar />
            <div className="px-4 my-6">
                <div className="flex flex-col gap-6 max-w-(--breakpoint-sm) mx-auto w-full">
                    <Outlet />
                </div>
            </div>
        </>
    )
}