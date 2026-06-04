import InputBasic from "@/components/form/InputBasic.tsx";
import {Button} from "@/components/ui/button.tsx";
import {cn} from "@/lib/utils.ts";
import {COMMUNITY_CATEGORIES} from "@/constants/category.ts";
import {Settings2} from "lucide-react";

interface CommunityPostFiltersProps {
    keyword: string
    sort: string
    category: string
    postCount: number
    onKeywordChange: (keyword: string) => void
    onSortChange: (sort: string) => void
    onCategoryChange: (category: string) => void
    onQuerySubmit: (keyword: string) => void
}

export function CommunityPostFilters({
    keyword,
    sort,
    category,
    postCount,
    onKeywordChange,
    onSortChange,
    onCategoryChange,
    onQuerySubmit,
}: CommunityPostFiltersProps) {
    const toggleSort = () => onSortChange(sort === 'latest' ? 'popular' : 'latest')

    return (
        <>
            <InputBasic
                placeholder="검색어를 입력해주세요"
                value={keyword}
                onChange={onKeywordChange}
                onKeyDown={(e) => {
                    if (e.key === 'Enter') onQuerySubmit(keyword)
                }}
                isClearable={true}
            />

            <div className="flex gap-1 overflow-x-scroll whitespace-nowrap no-scrollbar">
                {COMMUNITY_CATEGORIES.map((cat) => (
                    <Button
                        key={cat.key}
                        className={cn(
                            "rounded-lg text-sm cursor-pointer font-normal",
                            category === cat.key && "bg-primary text-primary-foreground hover:bg-primary/90"
                        )}
                        onClick={() => onCategoryChange(cat.key)}
                        variant="secondary"
                    >
                        {cat.label}
                    </Button>
                ))}
            </div>

            <div className="flex items-center justify-between text-sm font-medium text-muted-foreground">
                <span>{postCount.toLocaleString()}개의 글</span>
                <button
                    type="button"
                    onClick={toggleSort}
                    className="flex cursor-pointer items-center gap-0.5 transition-colors hover:text-foreground"
                >
                    <Settings2 size={18}/>
                    <span>{sort === 'latest' ? '최신순' : '인기순'}</span>
                </button>
            </div>
        </>
    )
}
