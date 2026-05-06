import { useQuery } from '@tanstack/react-query'
import { communityApi } from '../api'
import { communityKeys } from '../queryKeys'
import type { Comment } from '../types'

export const useComments = (postId: string | undefined) =>
    useQuery<Comment[]>({
        queryKey: communityKeys.comments(postId),
        queryFn: () => communityApi.getComments(postId!),
        enabled: !!postId,
    })
