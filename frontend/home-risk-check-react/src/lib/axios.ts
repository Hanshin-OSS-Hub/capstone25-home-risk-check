// lib/axios.ts
import axios from 'axios'

export const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL,
    withCredentials: true, // httpOnly 쿠키 자동 전송
})

let isRefreshing = false
let failedQueue: Array<{
    resolve: (value: any) => void
    reject: (reason: any) => void
}> = []

// 갱신 중 실패한 요청들 처리
const processQueue = (error: any, token = null) => {
    failedQueue.forEach(({ resolve, reject }) => {
        error ? reject(error) : resolve(token)
    })
    failedQueue = []
}

api.interceptors.response.use(
    res => res,
    async err => {
        const originalRequest = err.config

        if (err.response?.status === 401 && !originalRequest._retry) {
            if (isRefreshing) {
                // 갱신 중이면 대기열에 추가
                return new Promise((resolve, reject) => {
                    failedQueue.push({ resolve, reject })
                }).then(() => api(originalRequest))
            }

            originalRequest._retry = true
            isRefreshing = true

            try {
                await api.post('/api/auth/reissue')
                processQueue(null)
                return api(originalRequest)
            } catch (refreshError) {
                processQueue(refreshError)
                window.location.href = '/login' // refresh도 만료 → 로그아웃
                return Promise.reject(refreshError)
            } finally {
                isRefreshing = false
            }
        }

        return Promise.reject(err)
    }
)