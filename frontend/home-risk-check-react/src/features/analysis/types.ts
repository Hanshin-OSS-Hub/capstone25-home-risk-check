export interface AnalyzeRequest {
    address: string
    detailAddress: string
    deposit: string
    registryFiles: File[]
    buildingFiles: File[]
}

// 백엔드 명세 미정 — 후속 단계에서 정밀 타입으로 교체
export type AnalyzeResult = Record<string, unknown>
