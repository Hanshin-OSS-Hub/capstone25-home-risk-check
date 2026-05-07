import {Avatar, AvatarImage} from "@/components/ui/avatar.tsx";
import {Heart, MessageSquareText} from "lucide-react";

interface CommunityPostDetailProps {
    onCommentClick: () => void
}

export function CommunityPostDetail({ onCommentClick }: CommunityPostDetailProps) {
    return (
        <div className="flex flex-col gap-4 border-b pb-6">
            <div className="flex items-center gap-2">
                <Avatar size="lg">
                    <AvatarImage src="https://github.com/shadcn.png"/>
                </Avatar>
                <div className="flex flex-col gap-2">
                    <span className="font-medium text-base leading-none">바람이분당구</span>
                    <span className="text-muted-foreground text-xs leading-none">카테고리&middot;1시간전</span>
                </div>
            </div>
            <h1 className="text-xl font-semibold">
                분당구에 바람이 많이 부는 이유가 뭘까요?
            </h1>
            <p className="text-base text-muted-foreground">
                최근 분당구에 바람이 많이 부는 것 같아요. 혹시 이유 아시는 분 계신가요?
            </p>
            <div className="flex items-center gap-3 text-muted-foreground">
                <div className="flex items-center gap-1">
                    <Heart className="h-5 w-5 cursor-pointer"/> 12
                </div>
                <div className="flex items-center gap-1">
                    <MessageSquareText className="h-5 w-5 cursor-pointer" onClick={onCommentClick}/> 53
                </div>
            </div>
        </div>
    )
}
