import { useQuery } from '@tanstack/react-query'
import { communityApi } from '../api'
import { communityKeys } from '../queryKeys'
import type { Post } from '../types'

export const usePost = (postId: string | undefined) =>
    useQuery<Post>({
        queryKey: communityKeys.post(postId),
        queryFn: () => communityApi.getPost(postId!),
        enabled: !!postId,
    })
