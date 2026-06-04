import {Outlet, Link} from "react-router-dom"
import {Logo} from "@/components/layouts/logo"
import {ROUTES} from "@/constants/routes"

export default function AuthLayout() {
    return (
        <div className="flex min-h-svh flex-col items-center justify-center gap-2 bg-background p-6 md:p-10">
            <Link to={ROUTES.home} aria-label="홈으로" className="opacity-90 transition-opacity hover:opacity-100">
                <Logo/>
            </Link>
            <main className="w-full max-w-sm">
                <Outlet/>
            </main>
        </div>
    )
}
