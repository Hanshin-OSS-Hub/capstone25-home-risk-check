import type { GetPostsParams } from './types'

export const communityKeys = {
    all: ['community'] as const,
    postsRoot: () => [...communityKeys.all, 'posts'] as const,
    posts: (params: GetPostsParams) => [...communityKeys.postsRoot(), params] as const,
    post: (postId: string | undefined) => [...communityKeys.all, 'post', postId] as const,
    comments: (postId: string | undefined) => [...communityKeys.all, 'comments', postId] as const,
}
