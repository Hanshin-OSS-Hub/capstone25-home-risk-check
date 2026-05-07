import { useEffect, useRef } from "react"
import { useAddressSearch } from "@/features/address/hooks/useAddressSearch"
import { useDebouncedValue } from "@/hooks/use-debounced-value"
import type { JusoItem } from "@/features/address/types"

interface Props {
    keyword: string
    onSelect: (item: JusoItem) => void
}

export default function AddressSearchResults({ keyword, onSelect }: Props) {
    const debouncedKeyword = useDebouncedValue(keyword, 300)
    const observerRef = useRef<HTMLDivElement | null>(null)
    const {
        data,
        fetchNextPage,
        hasNextPage,
        isLoading,
        isFetching,
        isFetchingNextPage,
    } = useAddressSearch(debouncedKeyword)
    const results = data?.pages.flatMap((page) => page.list) ?? []
    const isDebouncing = !!keyword && keyword !== debouncedKeyword

    useEffect(() => {
        if (!observerRef.current) return

        const observer = new IntersectionObserver(
            (entries) => {
                if (
                    entries[0].isIntersecting &&
                    hasNextPage &&
                    !isFetchingNextPage
                ) {
                    void fetchNextPage()
                }
            },
            { threshold: 1 }
        )

        observer.observe(observerRef.current)

        return () => observer.disconnect()
    }, [hasNextPage, isFetchingNextPage, fetchNextPage])

    return (
        <>
            {isLoading || isDebouncing ? (
                <p className="text-center py-6 text-gray-400">
                    검색 중...
                </p>
            ) : results.length > 0 ? (
                <div className="divide-y">
                    {results.map((item, idx) => (
                        <div
                            key={`${item.roadAddr}-${idx}`}
                            onClick={() => onSelect(item)}
                            className="p-4 cursor-pointer hover:bg-gray-50 active:bg-gray-100"
                        >
                            <p className="font-medium text-sm">
                                {item.roadAddr}
                            </p>

                            <p className="text-xs text-gray-500 mt-1">
                                {item.jibunAddr}
                            </p>
                        </div>
                    ))}
                </div>
            ) : keyword && !isFetching ? (
                <p className="text-center py-6 text-gray-400">
                    검색 결과가 없습니다
                </p>
            ) : null}

            {isFetchingNextPage && (
                <p className="text-center py-6 text-gray-400">
                    더 불러오는 중...
                </p>
            )}

            <div ref={observerRef} className="h-10" />
        </>
    )
}
