import { QueryClient } from '@tanstack/react-query'
import { getApiErrorStatus } from '@/lib/api-response'

export const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            staleTime: 30_000,
            refetchOnWindowFocus: false,
            retry: (failureCount, error) => {
                const status = getApiErrorStatus(error)
                if (status === 401 || status === 403 || status === 404) return false
                return failureCount < 1
            },
        },
        mutations: {
            retry: false,
        },
    },
})
