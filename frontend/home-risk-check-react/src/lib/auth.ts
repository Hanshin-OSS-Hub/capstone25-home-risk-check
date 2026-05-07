import { queryClient } from '@/lib/queryClient'
import { router } from '@/routes/router'
import { ROUTES } from '@/constants/routes'
import { authKeys } from '@/features/auth/queryKeys'

// 인터셉터에서 발행하는 인증 만료 이벤트 — 캐시 무효화 + 라우터 이동
export const handleAuthExpired = () => {
    queryClient.setQueryData(authKeys.me(), null)
    void router.navigate(ROUTES.login, { replace: true })
}
