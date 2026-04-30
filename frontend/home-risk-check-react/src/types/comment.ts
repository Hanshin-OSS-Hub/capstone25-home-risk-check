export type Comment = {
    commentId: number
    parentCommentId: number | null
    authorNickname: string
    content: string
    createdAt: string
    likes: number
}

export type CommentTree = Comment & {
    children: CommentTree[]
}