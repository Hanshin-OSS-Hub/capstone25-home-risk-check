import { useQuery } from '@tanstack/react-query'
import { authApi } from '../api'
import { authKeys } from '../queryKeys'
import type { User } from '../types'

/**
 * 현재 로그인 사용자 조회 전용 훅 (read-only).
 * 로그인/로그아웃 액션은 useLogin / useLogout 사용.
 */
export const useAuth = () => {
    const { data, isLoading, isFetched } = useQuery<User | null>({
        queryKey: authKeys.me(),
        queryFn: authApi.me,
        staleTime: 1000 * 60 * 5,
        retry: false,
    })

    return {
        user: data ?? null,
        isLoading,
        isLoggedIn: isFetched && !!data,
    }
}
