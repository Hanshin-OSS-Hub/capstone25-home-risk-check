import { useEffect, useRef, useState } from "react"
import { useNavigate } from "react-router-dom"
import { useInfiniteQuery } from "@tanstack/react-query"
import axios from "axios"
import InputClear from "@/components/InputClear"
import { Toaster } from "@/components/ui/sonner"
import { toast } from "sonner"

export default function AddressSearchPage() {
    const [keyword, setKeyword] = useState("")
    const [debouncedKeyword, setDebouncedKeyword] = useState("")
    const navigate = useNavigate()
    const observerRef = useRef<HTMLDivElement | null>(null)
    const API_KEY = import.meta.env.VITE_JUSO_API_KEY

    useEffect(() => {
        const delay = setTimeout(() => {
            setDebouncedKeyword(keyword)
        }, 300)

        return () => clearTimeout(delay)
    }, [keyword])

    const validateKeyword = (value: string): string => {
        if (!value) return value

        const specialRegex = /[%=><]/g
        if (specialRegex.test(value)) {
            toast.error("% = > < 와 같은 특수문자는 사용할 수 없습니다.")
            return value.replace(specialRegex, "")
        }

        const sqlKeywords = [
            "OR", "SELECT", "INSERT", "DELETE",
            "UPDATE", "CREATE", "DROP", "EXEC",
            "UNION", "FETCH", "DECLARE", "TRUNCATE"
        ]

        let filtered = value

        for (const word of sqlKeywords) {
            if (new RegExp(word, "gi").test(filtered)) {
                toast.error(`${word}와(과) 같은 문자는 사용할 수 없습니다.`)
                filtered = filtered.replace(new RegExp(word, "gi"), "")
                break
            }
        }

        return filtered
    }

    const fetchAddress = async ({ pageParam = 1 }) => {
        //스프링 프록시 예정
        const res = await axios.get("https://www.juso.go.kr/addrlink/addrLinkApi.do",
            {
                params: {
                    confmKey: API_KEY,
                    currentPage: pageParam,
                    countPerPage: 20,
                    keyword: debouncedKeyword,
                    resultType: "json",
                },
            }
        )

        const common = res.data.results.common

        if (common.errorCode !== "0") {
            toast.error(`${common.errorMessage}`)
        }

        return {
            list: res.data.results.juso || [],
            total: Number(common.totalCount),
            page: pageParam,
        }
    }

    const {
        data,
        fetchNextPage,
        hasNextPage,
        isFetching,
        isFetchingNextPage,
    } = useInfiniteQuery({
        queryKey: ["address", debouncedKeyword],
        queryFn: fetchAddress,
        enabled: !!debouncedKeyword,
        initialPageParam: 1,
        getNextPageParam: (lastPage) => {
            const nextCount = lastPage.page * 20
            return nextCount < lastPage.total
                ? lastPage.page + 1
                : undefined
        },
    })

    const results = data?.pages.flatMap((page) => page.list) ?? []

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
            {threshold: 1 }
        )

        observer.observe(observerRef.current)

        return () => observer.disconnect()
    }, [hasNextPage, isFetchingNextPage, fetchNextPage])

    const handleSelectAddress = (item: any) => {
        navigate("/analysis", {
            state: { address: item.roadAddrPart1 },
        })
    }

    return (
        <div className="px-4 my-6">
            <div className="flex flex-col max-w-(--breakpoint-md) w-full mx-auto gap-6">
                <InputClear
                    placeholder="예) 판교역로 166, 분당 주공, 백현동 532"
                    value={keyword}
                    onChange={(val) => {
                        setKeyword(validateKeyword(val))
                    }}
                />

                {/* 검색 팁 */}
                {!keyword && (
                    <div className="flex flex-col gap-2">
                        <span className="font-bold">Tip</span>
                        <span className="mb-4 text-sm text-gray-500">
                            아래와 같은 조합으로 검색하면 더 정확합니다
                        </span>
                        <span>도로명 + 건물번호</span>
                        <span className="text-blue-500">예) 판교역로 166</span>
                        <span>지역명 + 번지</span>
                        <span className="text-blue-500">예) 백현동 532</span>
                        <span>건물명</span>
                        <span className="text-blue-500">예) 분당 주공</span>
                    </div>
                )}

                {/* 최초 로딩 */}
                {isFetching && results.length === 0 && (
                    <p className="text-center py-6 text-gray-400">
                        검색 중...
                    </p>
                )}

                {/* 결과 없음 */}
                {!isFetching && results.length === 0 && keyword && (
                    <p className="text-center py-6 text-gray-400">
                        검색 결과가 없습니다
                    </p>
                )}

                {/* 결과 리스트 */}
                <div className="divide-y">
                    {results.map((item, idx) => (
                        <div
                            key={`${item.roadAddr}-${idx}`}
                            onClick={() => handleSelectAddress(item)}
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

                {/* 추가 로딩 */}
                {isFetchingNextPage && (
                    <p className="text-center py-4 text-gray-400">
                        더 불러오는 중...
                    </p>
                )}

                {/* 무한스크롤 트리거 */}
                <div ref={observerRef} className="h-10" />

                <Toaster position="top-center" />
            </div>
        </div>
    )
}