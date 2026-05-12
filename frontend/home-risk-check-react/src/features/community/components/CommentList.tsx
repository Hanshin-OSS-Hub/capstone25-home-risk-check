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
                    {openReplyId === comment.commentId && (
                        <CommentComposer textareaRef={replyRef} />
                    )}
                </div>
            ))}
        </div>
    )
}
