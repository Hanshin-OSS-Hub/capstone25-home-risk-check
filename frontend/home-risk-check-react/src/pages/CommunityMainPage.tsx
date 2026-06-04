import {useEffect, useState} from 'react'
import {useSearchParams} from 'react-router-dom'
import { usePosts } from '@/features/community/hooks/usePosts'
import {CommunityPostFilters} from '@/features/community/components/CommunityPostFilters'
import {PostList} from '@/features/community/components/PostList'
import {CommunityWriteButton} from '@/features/community/components/CommunityWriteButton'

export default function CommunityMainPage() {
    const [searchParams, setSearchParams] = useSearchParams()
    const [keyword, setKeyword] = useState('')
    const sort = searchParams.get('sort') || 'latest'
    const category = searchParams.get('category') || 'all'
    const query = searchParams.get('query') || ''
    const [isTop, setIsTop] = useState(true)

    // 서버 상태
    const { data: posts = [], isLoading, isError } = usePosts({ sort, category, query })

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
            <CommunityPostFilters
                keyword={keyword}
                sort={sort}
                category={category}
                postCount={posts.length}
                onKeywordChange={setKeyword}
                onSortChange={handleSortChange}
                onCategoryChange={handleCategoryChange}
                onQuerySubmit={handleQueryChange}
            />
            {isLoading ? (
                <div className="flex flex-col gap-4">
                    {Array.from({ length: 4 }).map((_, i) => (
                        <div key={i} className="flex gap-4 pb-4">
                            <div className="flex flex-1 flex-col gap-2">
                                <div className="h-4 w-16 animate-pulse rounded bg-muted" />
                                <div className="h-4 w-3/4 animate-pulse rounded bg-muted" />
                                <div className="h-3 w-full animate-pulse rounded bg-muted" />
                                <div className="h-3 w-24 animate-pulse rounded bg-muted" />
                            </div>
                            <div className="size-22 shrink-0 animate-pulse rounded-lg bg-muted" />
                        </div>
                    ))}
                </div>
            ) : isError ? (
                <p className="py-10 text-center text-sm text-muted-foreground">게시글을 불러오지 못했어요.</p>
            ) : posts.length === 0 ? (
                <p className="py-10 text-center text-sm text-muted-foreground">게시글이 없어요.</p>
            ) : (
                <PostList posts={posts} />
            )}
            <CommunityWriteButton isTop={isTop} />
        </>
    )
}
