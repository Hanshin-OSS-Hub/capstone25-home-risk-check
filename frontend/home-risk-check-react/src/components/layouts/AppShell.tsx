//웹, 태블릿 화면 레이아웃
import { Outlet } from 'react-router-dom'
import Navbar from "@/components/layouts/Navbar"
import Footer from "@/components/layouts/Footer.tsx";

export function AppShell() {
    return (
        <>
            <div className="min-h-screen flex flex-col">
                <Navbar />
                <main className="flex-1 px-4 my-6">
                    <div className="flex flex-col gap-6 max-w-(--breakpoint-sm) mx-auto w-full">
                        <Outlet />
                    </div>
                </main>
                <Footer />
            </div>
        </>
    )
}