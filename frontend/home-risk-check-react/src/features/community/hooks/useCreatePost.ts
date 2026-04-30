import { useMutation, useQueryClient } from '@tanstack/react-query'
import { communityApi } from '../api'
import type { CreatePostBody } from '../types'
import { POSTS_QUERY_KEY } from './usePosts'

interface CreatePostArgs {
    body: CreatePostBody
    images: File[]
}

export const useCreatePost = () => {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: ({ body, images }: CreatePostArgs) => communityApi.createPost(body, images),
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: [POSTS_QUERY_KEY] })
        },
    })
}
