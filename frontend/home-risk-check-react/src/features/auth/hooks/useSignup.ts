import { useMutation } from '@tanstack/react-query'
import { authApi } from '../api'
import type { SignupRequest } from '../types'

export const useSignup = () =>
    useMutation({
        mutationFn: (body: SignupRequest) => authApi.signup(body),
    })

export const useCheckNickname = () =>
    useMutation({
        mutationFn: (nickname: string) => authApi.checkNickname(nickname),
    })
