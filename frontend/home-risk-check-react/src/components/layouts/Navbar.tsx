import { Button } from "@/components/ui/button";
import { Logo } from "@/components/layouts/logo";
import { NavMenu } from "@/components/layouts/NavMenu.tsx";
import { NavigationSheet } from "@/components/layouts/NavigationSheet.tsx";
import { Link } from "react-router-dom";

const Navbar = () => {
    return (
        <div className="px-4 py-2">
            <nav className="h-16 max-w-(--breakpoint-md) mx-auto w-full rounded-xl">
                <div className="mx-auto flex h-full items-center justify-between">
                    <Logo />

                    {/* Desktop Menu */}
                    <NavMenu className="hidden md:block" />

                    <div className="flex items-center gap-3">
                        <Button className="rounded-xl cursor-pointer">
                            <Link to="/signup">회원가입</Link>
                        </Button>

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
