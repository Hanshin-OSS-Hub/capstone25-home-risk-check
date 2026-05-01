import { toast } from 'sonner'
import type { AxiosError } from 'axios'

interface ApiErrorBody {
    message?: string
}

/** 사용자에게 보일 에러 메시지를 추출. AxiosError 면 응답 message, 아니면 fallback. */
export const extractErrorMessage = (err: unknown, fallback = '요청 중 오류가 발생했습니다.'): string => {
    const axiosErr = err as AxiosError<ApiErrorBody> | undefined
    return axiosErr?.response?.data?.message ?? fallback
}

/** 사용자 토스트 + (개발 모드) 콘솔 로그. console.error 직접 호출을 대체. */
export const notifyError = (err: unknown, fallback?: string) => {
    toast.error(extractErrorMessage(err, fallback))
    if (import.meta.env.DEV) console.error(err)
}

/** 사용자에 노출하지 않고 개발 로그만 — 무시 가능한 catch 에 사용. */
export const logUnexpected = (context: string, err: unknown) => {
    if (import.meta.env.DEV) console.error(`[${context}]`, err)
}
