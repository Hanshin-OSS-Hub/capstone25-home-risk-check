import { api } from '@/lib/axios'
import { unwrapApiData, type ApiEnvelope } from '@/lib/api-response'
import { mockComments } from '@/__mocks__/mockComments'
import {mockBlogPosts} from "@/__mocks__/mockBlogPosts.ts";
import type { CreatePostBody, GetPostsParams, Post, Comment } from './types'

const isMock = () => import.meta.env.VITE_USE_MOCK === 'true'

export const communityApi = {
    getPosts: async (params: GetPostsParams): Promise<Post[]> => {
        if (isMock()) return mockBlogPosts
        const res = await api.get<ApiEnvelope<Post[]>>('/api/posts', { params })
        return unwrapApiData(res.data)
    },

    getPost: async (postId: string): Promise<Post> => {
        const res = await api.get<ApiEnvelope<Post>>(`/api/posts/${postId}`)
        return unwrapApiData(res.data)
    },

    getComments: async (postId: string): Promise<Comment[]> => {
        if (isMock()) return mockComments
        const res = await api.get<ApiEnvelope<Comment[]>>(`/api/posts/${postId}/comments`)
        return unwrapApiData(res.data)
    },

    createPost: async (body: CreatePostBody, images: File[]): Promise<{ id: number }> => {
        const formData = new FormData()
        formData.append(
            'post',
            new Blob([JSON.stringify(body)], { type: 'application/json' }),
        )
        images.forEach(image => formData.append('images', image))
        const res = await api.post<ApiEnvelope<{ id: number }>>('/api/posts', formData)
        return unwrapApiData(res.data)
    },
}
