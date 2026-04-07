// store/useSignupStore.ts
import { create } from 'zustand'

interface SignupStore {
    nickname: string
    email: string
    password : string
    isNicknameChecked: boolean
    isEmailVerified: boolean
    setNickname : (nickname: string) => void
    setEmail: (email: string) => void
    setPassword : (password: string) => void
    setIsNicknameChecked: (v: boolean) => void
    setIsEmailVerified: (v: boolean) => void
    reset: () => void
}

export const signupStore = create<SignupStore>((set) => ({
    nickname: '',
    email: '',
    password: '',
    isNicknameChecked: false,
    isEmailVerified: false,
    setNickname: (nickname) => set({nickname}),
    setEmail: (email) => set({ email }),
    setPassword: (password) => set({password}),
    setIsNicknameChecked: (v) => set({isNicknameChecked: v}),
    setIsEmailVerified: (v) => set({ isEmailVerified: v }),
    reset: () => set({ email: '', isEmailVerified: false }),
}))