import { useQuery, useQueryClient } from '@tanstack/react-query'
import { authApi } from '@/features/auth/api'
import type { LoginRequest, User } from '@/features/auth/types'

export type { User } from '@/features/auth/types'

export const AUTH_QUERY_KEY = ['auth', 'me'] as const

export const useAuth = () => {
    const queryClient = useQueryClient()

    const { data: user, isLoading } = useQuery<User | null>({
        queryKey: AUTH_QUERY_KEY,
        queryFn: authApi.me,
        staleTime: 1000 * 60 * 5,
        retry: false,
    })

    const login = async (body: LoginRequest) => {
        await authApi.login(body)
        await queryClient.invalidateQueries({ queryKey: AUTH_QUERY_KEY })
    }

    const logout = async () => {
        await authApi.logout()
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
