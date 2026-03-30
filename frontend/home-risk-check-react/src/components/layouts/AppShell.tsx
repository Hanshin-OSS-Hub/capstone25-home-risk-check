//웹, 태블릿 화면 레이아웃
import { Outlet } from 'react-router-dom'
import Navbar from "@/components/layouts/Navbar"

export function AppShell() {
    return (
        <>
            <Navbar />
            <Outlet />
        </>
    )
}