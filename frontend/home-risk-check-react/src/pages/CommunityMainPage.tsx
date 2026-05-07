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
            <CommunityPostFilters
                keyword={keyword}
                sort={sort}
                category={category}
                onKeywordChange={setKeyword}
                onSortChange={handleSortChange}
                onCategoryChange={handleCategoryChange}
                onQuerySubmit={handleQueryChange}
            />
            <PostList posts={blogPosts} />
            <CommunityWriteButton isTop={isTop} />
        </>
    )
}
