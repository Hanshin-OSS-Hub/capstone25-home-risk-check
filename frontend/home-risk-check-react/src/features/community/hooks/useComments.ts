import { useQuery } from '@tanstack/react-query'
import { communityApi } from '../api'
import type { Comment } from '../types'

export const useComments = (postId: string | undefined) =>
    useQuery<Comment[]>({
        queryKey: ['community-comments', postId],
        queryFn: () => communityApi.getComments(postId!),
        enabled: !!postId,
    })
