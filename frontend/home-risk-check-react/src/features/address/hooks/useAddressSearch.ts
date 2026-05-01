import { useInfiniteQuery } from '@tanstack/react-query'
import { toast } from 'sonner'
import { addressApi } from '../api'
import type { JusoPage } from '../types'

const PAGE_SIZE = 20

export const useAddressSearch = (keyword: string) =>
    useInfiniteQuery<JusoPage>({
        queryKey: ['address', keyword],
        queryFn: async ({ pageParam }) => {
            const data = await addressApi.searchJuso({
                keyword,
                page: pageParam as number,
            })
            if (data.errorMessage) toast.error(data.errorMessage)
            return data
        },
        enabled: !!keyword,
        initialPageParam: 1,
        getNextPageParam: (lastPage) => {
            const nextCount = lastPage.page * PAGE_SIZE
            return nextCount < lastPage.total ? lastPage.page + 1 : undefined
        },
    })
