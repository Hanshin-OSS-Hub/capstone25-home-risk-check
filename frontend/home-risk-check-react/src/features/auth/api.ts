import { api } from '@/lib/axios'
import type { LoginRequest, SignupRequest, User, VerifyEmailRequest } from './types'

export const authApi = {
    me: async (): Promise<User | null> => {
        try {
            const res = await api.get<User>('/api/auth/me')
            return res.data
        } catch {
            return null
        }
    },

    login: async (body: LoginRequest): Promise<void> => {
        await api.post('/api/auth/login', body)
    },

    logout: async (): Promise<void> => {
        await api.post('/api/auth/logout')
    },

    reissue: async (): Promise<void> => {
        await api.post('/api/auth/reissue')
    },

    signup: async (body: SignupRequest): Promise<void> => {
        await api.post('/api/auth/signup', body)
    },

    sendEmailCode: async (email: string): Promise<void> => {
        await api.post('/api/auth/send-code', { email })
    },

    verifyEmailCode: async ({ email, code }: VerifyEmailRequest): Promise<void> => {
        await api.post('/api/auth/verify-code', { email, code })
    },

    checkNickname: async (nickname: string): Promise<void> => {
        await api.get('/api/auth/check-nickname', { params: { nickname } })
    },
}
