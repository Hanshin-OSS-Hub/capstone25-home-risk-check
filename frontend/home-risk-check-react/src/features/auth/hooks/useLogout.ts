import { useMutation, useQueryClient } from '@tanstack/react-query'
import { authApi } from '../api'
import { AUTH_QUERY_KEY } from './useAuth'

export const useLogout = () => {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: () => authApi.logout(),
        onSuccess: () => {
            queryClient.setQueryData(AUTH_QUERY_KEY, null)
        },
    })
}
