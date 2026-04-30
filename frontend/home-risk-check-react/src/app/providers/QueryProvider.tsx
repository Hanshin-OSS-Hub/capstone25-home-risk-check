import { useState, type ReactNode } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { AxiosError } from 'axios'

export function QueryProvider({ children }: { children: ReactNode }) {
    const [client] = useState(
        () =>
            new QueryClient({
                defaultOptions: {
                    queries: {
                        staleTime: 30_000,
                        refetchOnWindowFocus: false,
                        retry: (failureCount, error) => {
                            const status = (error as AxiosError | undefined)?.response?.status
                            if (status === 401 || status === 403 || status === 404) return false
                            return failureCount < 1
                        },
                    },
                    mutations: {
                        retry: false,
                    },
                },
            }),
    )

    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
}
