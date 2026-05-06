import { useMutation, useQueryClient } from '@tanstack/react-query'
import { authApi } from '../api'
import { authKeys } from '../queryKeys'
import type { LoginRequest } from '../types'

export const useLogin = () => {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: (body: LoginRequest) => authApi.login(body),
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: authKeys.me() })
        },
    })
}
