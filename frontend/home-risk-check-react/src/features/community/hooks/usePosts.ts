import { useQuery } from '@tanstack/react-query'
import { communityApi } from '../api'
import type { GetPostsParams, Post } from '../types'

export const POSTS_QUERY_KEY = 'community-posts'

export const usePosts = (params: GetPostsParams) =>
    useQuery<Post[]>({
        queryKey: [POSTS_QUERY_KEY, params],
        queryFn: () => communityApi.getPosts(params),
    })
