import { api } from '@/lib/axios'
import type { Comment } from '@/types/comment'
import { mockComments } from '@/__mocks__/mockComments'
import type { CreatePostBody, GetPostsParams, Post } from './types'

const isMock = () => import.meta.env.VITE_USE_MOCK === 'true'

export const communityApi = {
    getPosts: async (params: GetPostsParams): Promise<Post[]> => {
        const res = await api.get<Post[]>('/api/posts', { params })
        return res.data
    },

    getPost: async (postId: string): Promise<Post> => {
        const res = await api.get<Post>(`/api/posts/${postId}`)
        return res.data
    },

    getComments: async (postId: string): Promise<Comment[]> => {
        if (isMock()) return mockComments
        const res = await api.get<Comment[]>(`/api/posts/${postId}/comments`)
        return res.data
    },

    createPost: async (body: CreatePostBody, images: File[]): Promise<{ id: number }> => {
        const formData = new FormData()
        formData.append(
            'post',
            new Blob([JSON.stringify(body)], { type: 'application/json' }),
        )
        images.forEach(image => formData.append('images', image))
        const res = await api.post<{ id: number }>('/api/posts', formData)
        return res.data
    },
}
