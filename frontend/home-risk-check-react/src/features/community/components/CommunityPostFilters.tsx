import InputBasic from "@/components/InputBasic.tsx";
import {Button} from "@/components/ui/button.tsx";
import {cn} from "@/lib/utils.ts";
import {COMMUNITY_CATEGORIES} from "@/constants/category.ts";
import {
    Select,
    SelectContent,
    SelectGroup,
    SelectItem,
    SelectLabel,
    SelectTrigger,
    SelectValue
} from '@/components/ui/select';

interface CommunityPostFiltersProps {
    keyword: string
    sort: string
    category: string
    onKeywordChange: (keyword: string) => void
    onSortChange: (sort: string) => void
    onCategoryChange: (category: string) => void
    onQuerySubmit: (keyword: string) => void
}

export function CommunityPostFilters({
    keyword,
    sort,
    category,
    onKeywordChange,
    onSortChange,
    onCategoryChange,
    onQuerySubmit,
}: CommunityPostFiltersProps) {
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

            <div className="flex items-center gap-1">
                <Select value={sort} onValueChange={(value) => onSortChange(value)}>
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
                            onClick={() => onCategoryChange(cat.key)}
                            variant="secondary"
                        >
                            {cat.label}
                        </Button>
                    ))}
                </div>
            </div>
        </>
    )
}
