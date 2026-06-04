import {UserAvatar} from "@/features/community/components/UserAvatar";
import {EllipsisVertical, Heart, MessageSquareText, Pencil, Share2, Trash2} from "lucide-react";
import {showToast} from "@/lib/notify.ts";
import {Popover, PopoverContent, PopoverTrigger} from "@/components/ui/popover.tsx";
import {Button} from "@/components/ui/button.tsx";
import {useState} from "react";
import {cn} from "@/lib/utils.ts";
import {useAuth} from "@/features/auth/hooks/useAuth";

interface CommunityPostDetailProps {
    onCommentClick: () => void
}

export function CommunityPostDetail({ onCommentClick }: CommunityPostDetailProps) {
    const { isLoggedIn } = useAuth();
    const [liked, setLiked] = useState(false);
    const likeCount = 12 + (liked ? 1 : 0); // 좋아요 요청은 백엔드 연동 시
    const copyCurrentUrl = async () => {
        try {
            await navigator.clipboard.writeText(window.location.href);
            showToast({
                message: "링크가 복사되었습니다.",
                variant: 'success',
            })
        } catch {
            showToast({
                message: "링크 복사에 실패했습니다.",
                variant: 'error',
            })
        }
    };

    return (
        <div className="flex flex-col gap-4 border-b pb-6">
            <div className="flex items-center gap-2">
                <UserAvatar size="lg"/>
                <div className="flex flex-col gap-2">
                    <span className="font-medium text-base leading-none">바람이분당구</span>
                    <span className="text-muted-foreground text-xs leading-none">카테고리&middot;1시간전</span>
                </div>
                {isLoggedIn && (
                    <Popover>
                        <PopoverTrigger asChild>
                            <Button
                                variant="ghost"
                                size="icon"
                                className="size-8 ml-auto cursor-pointer text-muted-foreground hover:text-foreground"
                                aria-label="더보기"
                            >
                                <EllipsisVertical />
                            </Button>
                        </PopoverTrigger>
                        <PopoverContent align="end" side="bottom" className="w-fit p-1 text-xs gap-0 rounded-lg font-medium">
                            <div className="flex items-center gap-1 cursor-pointer hover:bg-muted p-2 rounded-lg">
                                <Pencil size={14} />
                                <span>수정</span>
                            </div>
                            <div className="flex items-center gap-1 cursor-pointer hover:bg-muted p-2 rounded-lg">
                                <Trash2 size={14} />
                                <span>삭제</span>
                            </div>
                        </PopoverContent>
                    </Popover>
                )}
            </div>
            <h1 className="text-xl font-semibold">
                분당구에 바람이 많이 부는 이유가 뭘까요?
            </h1>
            <p className="text-base text-muted-foreground">
                최근 분당구에 바람이 많이 부는 것 같아요. 혹시 이유 아시는 분 계신가요?
            </p>
            <div className="w-full aspect-video self-start rounded-lg bg-muted">
                {/* 게시글 이미지 영역*/}
            </div>
            <div className="w-full aspect-video self-start rounded-lg bg-muted">
                {/* 게시글 이미지 영역*/}
            </div>
            <div className="flex items-center gap-4 text-sm text-muted-foreground mt-4">
                <div onClick={() => setLiked(v => !v)} className="flex items-center gap-1 rounded-full cursor-pointer">
                    <Heart className={cn("h-4 w-4", liked && "fill-red-500 text-red-500")}/> 좋아요 {likeCount}
                </div>
                <div className="flex items-center gap-1 rounded-full cursor-pointer" onClick={onCommentClick}>
                    <MessageSquareText className="h-4 w-4"/> 댓글 53
                </div>
                <div className="ml-auto flex items-center gap-1 cursor-pointer" onClick={copyCurrentUrl}>
                    <Share2 className="h-4 w-4"/> 공유하기
                </div>
            </div>
        </div>
    )
}
