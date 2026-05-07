export interface AnalyzeRequest {
    address: string
    detailAddress: string
    deposit: string
    registryFiles: File[]
    buildingFiles: File[]
}

export type RiskLevel = 'SAFE' | 'LOW' | 'MEDIUM' | 'HIGH' | 'DANGER'
export type Severity = 'LOW' | 'MEDIUM' | 'HIGH'

export interface RiskFactor {
    type: string
    severity: Severity | string
    message: string
}

export interface HugResult {
    is_eligible: boolean
    safe_limit: number
    coverage_ratio: number
    message: string
}

export interface AnalysisDetails {
    jeonse_ratio: number
    senior_debt: number
    is_illegal_building: boolean
    is_trust: boolean
    building_age: number
    ownership_duration_months: number
}

export interface AnalysisResultData {
    address: string
    deposit: number
    market_price: number
    price_source: string
    risk_score: number
    risk_level: RiskLevel | string
    major_risk_factors: RiskFactor[]
    hug_result: HugResult
    details: AnalysisDetails
    recommendations: string[]
    _debug_info?: unknown
}

export interface AnalyzeResult {
    task_id: string
    status: 'COMPLETED' | 'PENDING' | 'FAILED' | string
    progress: number
    result: AnalysisResultData
}
