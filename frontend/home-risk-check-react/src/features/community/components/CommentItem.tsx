import {Avatar, AvatarImage} from "@/components/ui/avatar.tsx";
import {Heart, MessageSquareText} from "lucide-react";
import type {CommentTree} from '@/features/community/types.ts';

interface CommentItemProps {
    comment: CommentTree
    isReply?: boolean
    onReplyClick?: () => void
}

export function CommentItem({ comment, isReply = false, onReplyClick }: CommentItemProps) {
    return (
        <div className="flex flex-col gap-2">
            <div className="flex items-center gap-2">
                <Avatar size="default">
                    <AvatarImage src="https://github.com/shadcn.png"/>
                </Avatar>
                <div className="flex flex-col gap-1">
                    <span className="font-medium text-xs leading-none">{comment.authorNickname}</span>
                    <span className="text-muted-foreground text-xs leading-none">{comment.createdAt}</span>
                </div>
            </div>
            <div className="ml-10 flex flex-col gap-2">
                <p className="text-sm text-muted-foreground">{comment.content}</p>
                <div className="flex items-center gap-3 text-muted-foreground text-sm">
                    <div className="flex items-center gap-1">
                        <Heart className="h-4 w-4" /> {comment.likes}
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
