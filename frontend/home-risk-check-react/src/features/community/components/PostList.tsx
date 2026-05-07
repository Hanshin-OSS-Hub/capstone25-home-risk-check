import {PostListItem} from './PostListItem'
import type {Post} from '@/features/community/types'

interface PostListProps {
    posts: Post[]
}

export function PostList({ posts }: PostListProps) {
    return (
        <div className="flex flex-col gap-4 divide-y">
            {posts.map((post) => (
                <PostListItem key={post.id} post={post} />
            ))}
        </div>
    )
}
