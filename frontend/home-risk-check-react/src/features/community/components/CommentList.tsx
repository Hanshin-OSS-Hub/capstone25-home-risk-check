import {CommentComposer} from './CommentComposer'
import {CommentItem} from './CommentItem'
import type {CommentTree} from '@/features/community/types.ts';
import type {Ref} from 'react'

interface CommentListProps {
    comments: CommentTree[]
    openReplyId: number | null
    replyRef: Ref<HTMLTextAreaElement>
    onReplyToggle: (commentId: number) => void
}

export function CommentList({ comments, openReplyId, replyRef, onReplyToggle }: CommentListProps) {
    return (
        <div className="flex flex-col gap-4">
            {comments.map(comment => (
                <div key={comment.commentId} className="flex flex-col gap-4">
                    <CommentItem
                        comment={comment}
                        onReplyClick={() => onReplyToggle(comment.commentId)}
                    />
                    {comment.children.map((reply: CommentTree) => (
                        <div key={reply.commentId} className="ml-10">
                            <CommentItem comment={reply} isReply />
                        </div>
                    ))}
                    <div className="flex items-center gap-2 ml-20">
                        <div className="w-10 border-t border-border" />
                            <div className="cursor-pointer text-xs font-medium text-muted-foreground">답글 10개 더보기</div>
                        <div className="w-10 border-t border-border" />
                    </div>
                    {openReplyId === comment.commentId && (
                        <CommentComposer textareaRef={replyRef} />
                    )}
                </div>
            ))}
        </div>
    )
}
