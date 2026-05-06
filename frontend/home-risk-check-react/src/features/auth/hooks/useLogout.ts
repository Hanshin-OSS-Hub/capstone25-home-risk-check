import { useMutation, useQueryClient } from '@tanstack/react-query'
import { authApi } from '../api'
import { authKeys } from '../queryKeys'

export const useLogout = () => {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: () => authApi.logout(),
        onSuccess: () => {
            queryClient.setQueryData(authKeys.me(), null)
        },
    })
}
