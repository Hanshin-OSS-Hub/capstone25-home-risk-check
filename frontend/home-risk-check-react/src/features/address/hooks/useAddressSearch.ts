import { useInfiniteQuery } from '@tanstack/react-query'
import { showToast } from '@/lib/notify.ts'
import { addressApi } from '../api'
import { addressKeys } from '../queryKeys'
import type { JusoPage } from '../types'

const PAGE_SIZE = 20

export const useAddressSearch = (keyword: string) =>
    useInfiniteQuery<JusoPage>({
        queryKey: addressKeys.search(keyword),
        queryFn: async ({ pageParam }) => {
            const data = await addressApi.searchJuso({
                keyword,
                page: pageParam as number,
            })
            if (data.errorMessage) showToast({ message: data.errorMessage, variant: 'warning' })
            return data
        },
        enabled: !!keyword,
        initialPageParam: 1,
        getNextPageParam: (lastPage) => {
            const nextCount = lastPage.page * PAGE_SIZE
            return nextCount < lastPage.total ? lastPage.page + 1 : undefined
        },
    })
