import type { CommunityCategoryKey } from '@/constants/category'

export interface PollOption {
    id: string
    text: string
}

export interface Poll {
    options: PollOption[]
    multipleChoice: boolean
}

export interface Post {
    id: number
    category: CommunityCategoryKey | string
    title: string
    content?: string
    authorNickname?: string
    likes?: number
    commentCount?: number
    createdAt?: string
    image?: string
}

export type PostSort = 'latest' | 'popular'

export interface GetPostsParams {
    sort?: string
    category?: string
    query?: string
}

export interface CreatePostBody {
    category: string
    title: string
    content: string
    placeLat: number | null
    placeLng: number | null
    poll: Poll | null
}

export interface Comment {
    commentId: number
    parentCommentId: number | null
    authorNickname: string
    content: string
    createdAt: string
    likes: number
}

export interface CommentTree extends Comment {
    children: CommentTree[]
}