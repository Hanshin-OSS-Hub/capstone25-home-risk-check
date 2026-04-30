import { useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/lib/axios'

export interface User {
    id: number
    email: string
    nickname: string
    role: 'USER' | 'ADMIN'
}

export const AUTH_QUERY_KEY = ['auth', 'me'] as const

export const useAuth = () => {
    const queryClient = useQueryClient()

    const { data: user, isLoading } = useQuery<User | null>({
        queryKey: AUTH_QUERY_KEY,
        queryFn: async () => {
            try {
                const res = await api.get('/api/auth/me')
                return res.data
            } catch {
                return null
            }
        },
        staleTime: 1000 * 60 * 5,
        retry: false,
    })

    const login = async (email: string, password: string) => {
        await api.post('/api/auth/login', { email, password })
        await queryClient.invalidateQueries({queryKey: AUTH_QUERY_KEY})
    }

    const logout = async () => {
        await api.post('/api/auth/logout')
        queryClient.setQueryData(AUTH_QUERY_KEY, null)
    }

    return {
        user: user ?? null,
        isLoading,
        isLoggedIn: !!user,
        login,
        logout,
    }
}