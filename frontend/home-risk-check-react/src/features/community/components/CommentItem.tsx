import {useState} from "react";
import {UserAvatar} from "@/features/community/components/UserAvatar";
import {Heart, MessageSquareText} from "lucide-react";
import {cn} from "@/lib/utils.ts";
import type {CommentTree} from '@/features/community/types.ts';

interface CommentItemProps {
    comment: CommentTree
    isReply?: boolean
    onReplyClick?: () => void
}

export function CommentItem({ comment, isReply = false, onReplyClick }: CommentItemProps) {
    const [liked, setLiked] = useState(false);
    const likeCount = comment.likes + (liked ? 1 : 0); // 좋아요 요청은 백엔드 연동 시
    return (
        <div className="flex flex-col gap-2">
            <div className="flex items-center gap-2">
                <UserAvatar size="default"/>
                <div className="flex flex-col gap-1">
                    <span className="font-medium text-xs leading-none">{comment.authorNickname}</span>
                    <span className="text-muted-foreground text-xs leading-none">{comment.createdAt}</span>
                </div>
            </div>
            <div className="ml-10 flex flex-col gap-2">
                <p className="text-sm text-muted-foreground">{comment.content}</p>
                <div className="flex items-center gap-3 text-muted-foreground text-sm">
                    <div onClick={() => setLiked(v => !v)} className="flex items-center gap-1 cursor-pointer">
                        <Heart className={cn("h-4 w-4", liked && "fill-red-500 text-red-500")} /> {likeCount}
                    </div>
                    {!isReply && (
                        <>
                            <button className="flex items-center gap-1">
                                <MessageSquareText className="h-4 w-4" /> 23
                            </button>
                            <span className="cursor-pointer" onClick={onReplyClick}>답글 달기</span>
                        </>
                    )}
                </div>
            </div>
        </div>
    )
}
