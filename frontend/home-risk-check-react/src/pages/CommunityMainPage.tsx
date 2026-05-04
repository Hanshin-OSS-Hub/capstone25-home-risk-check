import InputBasic from "@/components/InputBasic.tsx";
import {Button} from "@/components/ui/button.tsx";
import {Badge} from "@/components/ui/badge.tsx";
import {useEffect, useState} from 'react'
import {useSearchParams, Link} from 'react-router-dom'
import { usePosts } from '@/features/community/hooks/usePosts'
import { ROUTES } from '@/constants/routes'
import {cn} from "@/lib/utils.ts";
import {COMMUNITY_CATEGORIES} from "@/constants/category.ts";
import {Heart, MessageSquareText, PlusIcon} from "lucide-react";
import {
    Select,
    SelectContent,
    SelectGroup,
    SelectItem,
    SelectLabel,
    SelectTrigger,
    SelectValue
} from '@/components/ui/select';

export default function CommunityMainPage() {
    const [searchParams, setSearchParams] = useSearchParams()
    const [keyword, setKeyword] = useState('')
    const sort = searchParams.get('sort') || 'latest'
    const category = searchParams.get('category') || 'all'
    const query = searchParams.get('query') || ''
    const [isTop, setIsTop] = useState(true)

    // 서버 상태 — 데이터 표시는 4단계에서 PostListItem 분리와 함께 mock blogPosts 대체 예정
    const { data: blogPosts = [] } = usePosts({ sort, category, query })

    useEffect(() => {
        const handleScroll = () => {
            const next = window.scrollY <= 100
            setIsTop(prev => (prev === next ? prev : next))
        }
        window.addEventListener('scroll', handleScroll, { passive: true })
        return () => window.removeEventListener('scroll', handleScroll)
    }, [])

    const updateParams = (patch: Record<string, string | undefined>) => {
        const next = new URLSearchParams(searchParams)
        Object.entries(patch).forEach(([k, v]) => {
            if (v == null || v === '') next.delete(k)
            else next.set(k, v)
        })
        setSearchParams(next)
    }

    const handleSortChange = (sort: string) => updateParams({ sort })
    const handleCategoryChange = (category: string) => updateParams({ category })
    const handleQueryChange = (keyword: string) => updateParams({ query: keyword.trim() || undefined })

    return (
        <>
            <InputBasic
                placeholder="검색어를 입력해주세요"
                value={keyword}
                onChange={setKeyword}
                onKeyDown={(e) => {
                    if (e.key === 'Enter') handleQueryChange(keyword)
                }}
                isClearable={true}
            />

            <div className="flex items-center gap-1">
                <Select value={sort} onValueChange={(value) => handleSortChange(value)}>
                    <SelectTrigger className="rounded-xl">
                        <SelectValue placeholder="정렬">
                            {sort === 'latest' ? '최신순' : '인기순'}
                        </SelectValue>
                    </SelectTrigger>
                    <SelectContent>
                        <SelectGroup>
                            <SelectLabel>정렬</SelectLabel>
                            <SelectItem value='latest'>최신순</SelectItem>
                            <SelectItem value='popular'>인기순</SelectItem>
                        </SelectGroup>
                    </SelectContent>
                </Select>
                <div className="flex gap-1 overflow-x-scroll whitespace-nowrap no-scrollbar">
                    {COMMUNITY_CATEGORIES.map((cat) => (
                        <Button
                            key={cat.key}
                            className={cn(
                                "rounded-xl text-sm cursor-pointer font-normal bg-gray-100 text-gray-800",
                                category === cat.key && "bg-black text-white hover:bg-black"
                            )}
                            onClick={() => handleCategoryChange(cat.key)}
                            variant="secondary"
                        >
                            {cat.label}
                        </Button>
                    ))}
                </div>
            </div>

            <div className="flex flex-col gap-4 divide-y">
                {blogPosts.map((post) => (
                    <Link key={post.id} to={ROUTES.communityDetail(post.id)}>
                        <div className="flex gap-4 pb-4">
                            <div className="flex flex-1 flex-col gap-2">
                                <Badge variant="secondary" className="rounded-sm text-gray-700">
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
                            <div className="shrink-0 w-22 aspect-square self-start rounded-lg bg-gray-200">
                                {/* 게시글 이미지 영역*/}
                            </div>
                        </div>
                    </Link>
                ))}
            </div>
            <div className="sticky bottom-6 flex justify-end -mt-6">
                <Link to={ROUTES.communityNew}>
                    <Button
                        className={cn(
                            "rounded-full h-12 cursor-pointer transition-all duration-300 overflow-hidden",
                            isTop ? "w-22 gap-1" : "w-12"
                        )}
                        size="icon-lg"
                    >
                        <PlusIcon/>
                        <span className={cn(
                            "transition-all duration-300 ease-out overflow-hidden whitespace-nowrap",
                            isTop ? "max-w-fit opacity-100" : "max-w-0 opacity-0",
                        )}>
                            글쓰기
                        </span>
                    </Button>
                </Link>
            </div>
        </>
    )
}