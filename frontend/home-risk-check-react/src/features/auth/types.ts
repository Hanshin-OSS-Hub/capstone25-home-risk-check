export type UserRole = 'USER' | 'ADMIN'

export interface User {
    id: number
    email: string
    nickname: string
    role: UserRole
}

export interface LoginRequest {
    email: string
    password: string
}

export interface SignupRequest {
    email: string
    password: string
    nickname: string
}

export interface VerifyEmailRequest {
    email: string
    code: string
}
