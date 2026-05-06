import { useMutation, useQueryClient } from '@tanstack/react-query'
import { communityApi } from '../api'
import { communityKeys } from '../queryKeys'
import type { CreatePostArgs } from '../types'

export const useCreatePost = () => {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: ({ body, images }: CreatePostArgs) => communityApi.createPost(body, images),
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: communityKeys.postsRoot() })
        },
    })
}
