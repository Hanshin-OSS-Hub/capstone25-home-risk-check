export interface JusoItem {
    roadAddr: string
    roadAddrPart1: string
    jibunAddr: string
    bdNm: string
    [key: string]: unknown
}

export interface JusoPage {
    list: JusoItem[]
    total: number
    page: number
    /** errorCode !== '0' 인 경우의 사용자 메시지 (요청 자체는 성공) */
    errorMessage?: string
}

export interface SearchJusoParams {
    keyword: string
    page: number
}
