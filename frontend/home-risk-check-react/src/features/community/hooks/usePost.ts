import { useQuery } from '@tanstack/react-query'
import { communityApi } from '../api'
import type { Post } from '../types'

export const usePost = (postId: string | undefined) =>
    useQuery<Post>({
        queryKey: ['community-post', postId],
        queryFn: () => communityApi.getPost(postId!),
        enabled: !!postId,
    })
