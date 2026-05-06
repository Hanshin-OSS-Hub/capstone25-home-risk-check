import { useQuery } from '@tanstack/react-query'
import { communityApi } from '../api'
import { communityKeys } from '../queryKeys'
import type { GetPostsParams, Post } from '../types'

export const usePosts = (params: GetPostsParams) =>
    useQuery<Post[]>({
        queryKey: communityKeys.posts(params),
        queryFn: () => communityApi.getPosts(params),
    })
