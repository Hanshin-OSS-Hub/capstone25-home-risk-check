import { useMutation, useQueryClient } from '@tanstack/react-query'
import { communityApi } from '../api'
import type { CreatePostArgs } from '../types'
import { POSTS_QUERY_KEY } from './usePosts'

export const useCreatePost = () => {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: ({ body, images }: CreatePostArgs) => communityApi.createPost(body, images),
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: [POSTS_QUERY_KEY] })
        },
    })
}
