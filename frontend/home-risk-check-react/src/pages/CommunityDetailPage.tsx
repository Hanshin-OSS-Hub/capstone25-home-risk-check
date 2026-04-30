import {
    InputGroup,
    InputGroupAddon,
    InputGroupButton,
    InputGroupTextarea,
} from "@/components/ui/input-group";
import {useEffect, useState, useRef} from 'react'
import {useParams} from 'react-router-dom'
import {communityApi} from '@/features/community/api'
import {Avatar, AvatarImage} from "@/components/ui/avatar.tsx";
import {Heart, MessageSquareText} from "lucide-react";
import type {Comment, CommentTree} from "@/types/comment.ts";

function buildCommentTree(flat: Comment[]): CommentTree[] {
    const map = new Map<number, CommentTree>()
    const roots: CommentTree[] = []

    flat.forEach((c) => {
        map.set(c.commentId, { ...c, children: [] })
    })

    map.forEach((comment) => {
        if (comment.parentCommentId === null) {
            roots.push(comment)
        } else {
            const parent = map.get(comment.parentCommentId)
            if (parent) {
                parent.children.push(comment)
            }
        }
    })
    return roots
}

function CommentItem({ comment, isReply = false, onReplyClick }: { comment: any, isReply?: boolean, onReplyClick?: () => void }) {
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

export default function CommunityDetailPage() {
    const { postId } = useParams<{ postId: string }>()
    const [comments, setComments] = useState<any[]>([])
    const textareaRef = useRef<HTMLTextAreaElement>(null)
    const replyRef = useRef<HTMLTextAreaElement>(null)
    const [openReplyId, setOpenReplyId] = useState<number | null>(null)

    const focusTextarea = () => {
        textareaRef.current?.focus()
    }

    useEffect(() => {
        if (openReplyId !== null) replyRef.current?.focus()
    }, [openReplyId])

    useEffect(() => {
        if (!postId) return
        communityApi.getComments(postId)
            .then(data => setComments(buildCommentTree(data)))
            .catch(err => console.error(err))
    }, [postId])

    return (
        <>
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
                        <MessageSquareText className="h-5 w-5 cursor-pointer" onClick={focusTextarea}/> 53
                    </div>
                </div>
            </div>
            <div className="flex items-start gap-2">
                <Avatar size="default">
                    <AvatarImage src="https://github.com/shadcn.png"/>
                </Avatar>
                <InputGroup className="!rounded-xl bg-gray-100 border-none">
                    <InputGroupTextarea
                        ref={textareaRef}
                        className="min-h-10 p-3"
                        placeholder="댓글을 작성해주세요"
                    />
                    <InputGroupAddon align="block-end" className="pt-0">
                        <InputGroupButton className="ml-auto rounded-xl cursor-pointer" size="sm" variant="default">
                            댓글 작성
                        </InputGroupButton>
                    </InputGroupAddon>
                </InputGroup>
            </div>
            <div className="flex flex-col gap-4">
                {comments
                    .filter(comment => comment.parentCommentId === null)
                    .map(comment => (
                    <div key={comment.commentId} className="flex flex-col gap-4">
                        <CommentItem comment={comment}
                                     onReplyClick={() => {
                                         setOpenReplyId(prev => (prev === comment.commentId ? null : comment.commentId));
                                     }}
                        />
                        {comment.children.map((reply: any) => (
                            <div key={reply.commentId} className="ml-10">
                                <CommentItem comment={reply} isReply />
                            </div>
                        ))}
                        {openReplyId === comment.commentId && (
                            <div className="flex items-start gap-2">
                                <Avatar size="default">
                                    <AvatarImage src="https://github.com/shadcn.png"/>
                                </Avatar>
                                <InputGroup className="!rounded-xl bg-gray-100 min-h-10 border-none">
                                    <InputGroupTextarea
                                        ref={replyRef}
                                        className="min-h-10 p-3"
                                        placeholder="댓글을 작성해주세요"
                                    />
                                    <InputGroupAddon align="block-end" className="pt-0">
                                        <InputGroupButton className="ml-auto rounded-xl cursor-pointer" size="sm" variant="default">
                                            댓글 작성
                                        </InputGroupButton>
                                    </InputGroupAddon>
                                </InputGroup>
                            </div>
                        )}
                    </div>
                ))}
            </div>
        </>
    )
}