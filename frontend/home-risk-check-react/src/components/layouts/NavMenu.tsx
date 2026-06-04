import {Link} from "react-router-dom";
import type { ComponentProps } from "react";
import {
    NavigationMenu,
    NavigationMenuItem,
    NavigationMenuList,
} from "@/components/ui/navigation-menu";
import { ROUTES } from "@/constants/routes";
import {Button} from "@/components/ui/button.tsx";

export const NavMenu = (props: ComponentProps<typeof NavigationMenu>) => (
    <NavigationMenu {...props}>
        <NavigationMenuList className="space-x-0 data-[orientation=vertical]:flex-col data-[orientation=vertical]:items-start data-[orientation=vertical]:justify-start">
            <NavigationMenuItem>
                <Button asChild size="sm" variant="ghost">
                    <Link to={ROUTES.analysis}>AI 분석</Link>
                </Button>
            </NavigationMenuItem>
            <NavigationMenuItem>
                <Button asChild size="sm" variant="ghost">
                    <Link to={ROUTES.community}>커뮤니티</Link>
                </Button>
            </NavigationMenuItem>
            <NavigationMenuItem>
                <Button asChild size="sm" variant="ghost">
                    <Link to="#">부동산 정보</Link>
                </Button>
            </NavigationMenuItem>
        </NavigationMenuList>
    </NavigationMenu>
);