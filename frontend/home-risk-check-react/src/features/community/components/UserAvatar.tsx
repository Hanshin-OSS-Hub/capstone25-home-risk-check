import * as React from "react";
import { Avatar, AvatarImage } from "@/components/ui/avatar.tsx";

const DEFAULT_AVATAR_SRC = "https://github.com/shadcn.png";

interface UserAvatarProps {
    src?: string;
    /** default/sm/lg 프리셋. 임의 크기는 className으로 (예: "size-20"). */
    size?: React.ComponentProps<typeof Avatar>["size"];
    /** Avatar 루트에 병합 — size-* 로 프리셋 크기를 덮어쓸 수 있음. */
    className?: string;
}

export function UserAvatar({ src = DEFAULT_AVATAR_SRC, size = "default", className }: UserAvatarProps) {
    return (
        <Avatar size={size} className={className}>
            <AvatarImage src={src} />
        </Avatar>
    );
}
