import {useEffect, useMemo, useState, useRef} from 'react'
import {useParams} from 'react-router-dom'
import {useComments} from '@/features/community/hooks/useComments'
import {buildCommentTree} from '@/features/community/utils/buildCommentTree'
import {CommunityPostDetail} from '@/features/community/components/CommunityPostDetail'
import {CommentComposer} from '@/features/community/components/CommentComposer'
import {CommentList} from '@/features/community/components/CommentList'
import {Button} from "@/components/ui/button.tsx";

export default function CommunityDetailPage() {
    const { postId } = useParams<{ postId: string }>()
    const { data: flatComments = [], isLoading: isCommentsLoading } = useComments(postId)
    const comments = useMemo(() => buildCommentTree(flatComments), [flatComments])
    const textareaRef = useRef<HTMLTextAreaElement>(null)
    const replyRef = useRef<HTMLTextAreaElement>(null)
    const [openReplyId, setOpenReplyId] = useState<number | null>(null)
    const [isComposerOpen, setIsComposerOpen] = useState(false)
    const showComposer = comments.length > 0 || isComposerOpen

    const focusTextarea = () => {
        textareaRef.current?.focus()
    }

    useEffect(() => {
        if (openReplyId !== null) replyRef.current?.focus()
    }, [openReplyId])

    return (
        <>
            <CommunityPostDetail onCommentClick={focusTextarea} />
            {isCommentsLoading ? (
                <p className="py-6 text-center text-sm text-muted-foreground">댓글을 불러오는 중…</p>
            ) : (
            <>
            {showComposer ? (
                <CommentComposer textareaRef={textareaRef} />
            ) : (
                <div className="flex flex-col items-center justify-center">
                    <p className="text-base">아직 댓글이 없어요.</p>
                    <p className="text-sm text-muted-foreground">
                        첫 번째 댓글을 남겨주세요.
                    </p>

                    <Button
                        className="mt-2 rounded-lg cursor-pointer"
                        onClick={() => setIsComposerOpen(true)}
                    >
                        댓글 작성하기
                    </Button>
                </div>
            )}
            <CommentList
                comments={comments}
                openReplyId={openReplyId}
                replyRef={replyRef}
                onReplyToggle={(commentId) => {
                    setOpenReplyId(prev => (prev === commentId ? null : commentId));
                }}
            />
            </>
            )}
        </>
    )
}
