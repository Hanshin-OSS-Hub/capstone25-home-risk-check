import axios, { type InternalAxiosRequestConfig } from 'axios'
import { handleAuthExpired } from '@/lib/auth'

declare module 'axios' {
    export interface InternalAxiosRequestConfig {
        _retry?: boolean
    }
}

export const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL,
    withCredentials: true, // httpOnly 쿠키 자동 전송
})

type FailedRequest = {
    resolve: () => void
    reject: (reason: unknown) => void
}

let isRefreshing = false
let failedQueue: FailedRequest[] = []

const processQueue = (error: unknown) => {
    failedQueue.forEach(({ resolve, reject }) => {
        if (error) reject(error)
        else resolve()
    })
    failedQueue = []
}

const REISSUE_PATH = '/api/auth/reissue'

api.interceptors.response.use(
    res => res,
    async err => {
        const originalRequest = err.config as InternalAxiosRequestConfig | undefined
        const status = err.response?.status

        // 인터셉트 대상이 아닌 경우 즉시 반환
        if (
            !originalRequest ||
            status !== 401 ||
            originalRequest._retry ||
            originalRequest.url?.includes(REISSUE_PATH)
        ) {
            return Promise.reject(err)
        }

        if (isRefreshing) {
            // 갱신 중이면 대기열에 추가 후 재시도
            return new Promise<void>((resolve, reject) => {
                failedQueue.push({ resolve, reject })
            }).then(() => api(originalRequest))
        }

        originalRequest._retry = true
        isRefreshing = true

        try {
            await api.post(REISSUE_PATH)
            processQueue(null)
            return api(originalRequest)
        } catch (refreshError) {
            processQueue(refreshError)
            // 라우팅 일관성 + 상태 보존을 위해 직접 location.href 대신 이벤트 emit
            handleAuthExpired()
            return Promise.reject(refreshError)
        } finally {
            isRefreshing = false
        }
    },
)
