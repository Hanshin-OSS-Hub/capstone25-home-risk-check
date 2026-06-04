import {Link} from 'react-router-dom'
import {Badge} from "@/components/ui/badge.tsx";
import {ROUTES} from '@/constants/routes'
import {Heart, MessageSquareText} from "lucide-react";
import type {Post} from '@/features/community/types'

interface PostListItemProps {
    post: Post
}

export function PostListItem({ post }: PostListItemProps) {
    return (
        <Link to={ROUTES.communityDetail(post.id)}>
            <div className="flex gap-4 pb-4">
                <div className="flex flex-1 flex-col gap-2">
                    <Badge variant="secondary" className="rounded-md text-muted-foreground">
                        {post.category}
                    </Badge>
                    <h3 className="line-clamp-1 text-ellipsis font-medium text-sm sm:text-base">
                        {post.title}
                    </h3>
                    <p className="line-clamp-1 text-ellipsis text-sm text-muted-foreground">
                        {post.content}
                    </p>
                    <div className="flex items-center gap-0.5 text-muted-foreground text-xs">
                        바람이 분당구
                        <span>&middot;</span>
                        1일전
                    </div>
                    <div className="flex items-center gap-4 font-medium text-muted-foreground text-sm">
                        <div className="flex items-center gap-1">
                            <Heart className="h-4 w-4"/> 12
                        </div>
                        <div className="flex items-center gap-1">
                            <MessageSquareText className="h-4 w-4"/> 53
                        </div>
                    </div>
                </div>
                <div className="shrink-0 w-22 aspect-square self-center-safe rounded-lg bg-muted">
                    {/* 게시글 이미지 영역*/}
                </div>
            </div>
        </Link>
    )
}
