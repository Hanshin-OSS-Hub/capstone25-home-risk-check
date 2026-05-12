import { useMutation } from '@tanstack/react-query'
import { authApi } from '../api'
import type { VerifyEmailRequest } from '../types'

export const useSendEmailCode = () =>
    useMutation({
        mutationFn: (email: string) => authApi.sendEmailCode(email),
    })

export const useVerifyEmailCode = () =>
    useMutation({
        mutationFn: (req: VerifyEmailRequest) => authApi.verifyEmailCode(req),
    })
