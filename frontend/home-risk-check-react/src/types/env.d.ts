/// <reference types="vite/client" />

interface ImportMetaEnv {
    readonly VITE_API_URL: string
    readonly VITE_USE_MOCK?: 'true' | 'false'
    readonly VITE_JUSO_API_KEY?: string
    readonly VITE_KAKAO_MAP_KEY?: string
}
