export type UserRole = 'USER' | 'ADMIN'

/** GET /api/users/me (UserResponse) */
export interface UserProfile {
    id: number
    email: string
    nickname: string
    profileImageUrl: string | null
    role: UserRole
    createdAt: string
    updatedAt: string
}

/** GET /api/analysis/history 항목(요약) */
export interface MyAnalysisHistoryItem {
    id: number
    address: string
    riskLevel: string
    riskScore: number
    createdAt: string
}
