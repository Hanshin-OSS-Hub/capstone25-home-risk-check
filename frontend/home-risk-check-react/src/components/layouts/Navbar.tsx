import { Button } from "@/components/ui/button";
import { Logo } from "@/components/layouts/logo";
import { NavMenu } from "@/components/layouts/NavMenu.tsx";
import { NavigationSheet } from "@/components/layouts/NavigationSheet.tsx";
import {Link} from "react-router-dom";
import { ROUTES } from "@/constants/routes";
import {Popover, PopoverContent, PopoverTrigger} from "@/components/ui/popover.tsx";
import {CircleUser, LogOut} from "lucide-react";
import {UserAvatar} from "@/features/community/components/UserAvatar.tsx";
import {useAuth} from "@/features/auth/hooks/useAuth";
import {useLogout} from "@/features/auth/hooks/useLogout";

const Navbar = () => {
    const { isLoggedIn } = useAuth();
    const logout = useLogout();

    return (
        <div className="px-4 py-2">
            <nav className="h-16 max-w-(--breakpoint-sm) mx-auto w-full rounded-xl">
                <div className="mx-auto flex h-full items-center justify-between">
                    <Logo />

                    {/* Desktop Menu */}
                    <NavMenu className="hidden md:flex"/>

                    <div className="flex items-center gap-3">
                        {isLoggedIn ? (
                            <Popover>
                                <PopoverTrigger asChild>
                                    <Button
                                        variant="ghost"
                                        size="icon"
                                        className="size-8 cursor-pointer text-muted-foreground hover:text-foreground"
                                        aria-label="더보기"
                                    >
                                        <UserAvatar/>
                                    </Button>
                                </PopoverTrigger>
                                <PopoverContent align="end" side="bottom" className="w-fit p-1 text-xs gap-0 rounded-lg font-medium">
                                    <Link to={ROUTES.mypage} className="flex items-center gap-1 cursor-pointer hover:bg-muted p-2 rounded-lg">
                                        <CircleUser size={14} />
                                        <span>마이페이지</span>
                                    </Link>
                                    <button
                                        type="button"
                                        onClick={() => logout.mutate()}
                                        className="flex w-full items-center gap-1 cursor-pointer hover:bg-muted p-2 rounded-lg"
                                    >
                                        <LogOut size={14} />
                                        <span>로그아웃</span>
                                    </button>
                                </PopoverContent>
                            </Popover>
                        ) : (
                            <Link to={ROUTES.login}>
                                <Button size="sm" className="cursor-pointer">
                                    로그인
                                </Button>
                            </Link>
                        )}

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
