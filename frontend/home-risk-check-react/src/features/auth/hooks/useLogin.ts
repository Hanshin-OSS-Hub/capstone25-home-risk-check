import { useMutation, useQueryClient } from '@tanstack/react-query'
import { authApi } from '../api'
import type { LoginRequest } from '../types'
import { AUTH_QUERY_KEY } from '@/hooks/use-auth'

export const useLogin = () => {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: (body: LoginRequest) => authApi.login(body),
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: AUTH_QUERY_KEY })
        },
    })
}
