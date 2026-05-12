import type { Comment, CommentTree } from '../types'

export function buildCommentTree(flat: Comment[]): CommentTree[] {
    const map = new Map<number, CommentTree>()
    const roots: CommentTree[] = []

    flat.forEach(c => {
        map.set(c.commentId, { ...c, children: [] })
    })

    map.forEach(comment => {
        if (comment.parentCommentId === null) {
            roots.push(comment)
        } else {
            const parent = map.get(comment.parentCommentId)
            if (parent) parent.children.push(comment)
        }
    })

    return roots
}
