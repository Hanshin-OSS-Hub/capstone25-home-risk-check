import { Button } from "@/components/ui/button";
import { Logo } from "@/components/layouts/logo";
import { NavMenu } from "@/components/layouts/NavMenu.tsx";
import { NavigationSheet } from "@/components/layouts/NavigationSheet.tsx";
import { Link } from "react-router-dom";
import { ROUTES } from "@/constants/routes";

const Navbar = () => {
    return (
        <div className="px-4 py-2">
            <nav className="h-16 max-w-(--breakpoint-sm) mx-auto w-full rounded-xl">
                <div className="mx-auto flex h-full items-center justify-between">
                    <Logo />

                    {/* Desktop Menu */}
                    <NavMenu className="hidden md:block" />

                    <div className="flex items-center gap-3">
                        <Link to={ROUTES.signup}>
                            <Button className="rounded-xl cursor-pointer">
                                회원가입
                            </Button>
                        </Link>

                        {/* Mobile Menu */}
                        <div className="md:hidden">
                            <NavigationSheet />
                        </div>
                    </div>
                </div>
            </nav>
        </div>
    );
};

export default Navbar;
