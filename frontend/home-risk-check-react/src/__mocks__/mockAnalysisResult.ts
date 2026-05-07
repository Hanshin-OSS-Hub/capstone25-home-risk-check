import type { AnalyzeResult } from '@/features/analysis/types'

export const mockAnalysisResult: AnalyzeResult = {
    task_id: 'dbc2defe-8440-4323-8f53-aac7356de418',
    status: 'COMPLETED',
    progress: 100,
    result: {
        address: '인천광역시 부평구 삼산동 167-15',
        deposit: 35_000_000,
        market_price: 61_000_000,
        price_source: 'DB_Trade',
        risk_score: 8.4,
        risk_level: 'DANGER',
        major_risk_factors: [
            {
                type: 'SHORT_OWNERSHIP',
                severity: 'MEDIUM',
                message: '건물 소유 기간이 짧음',
            },
            {
                type: 'OLD_BUILDING',
                severity: 'LOW',
                message: '건물 연식 32년으로 노후화됨',
            },
        ],
        hug_result: {
            is_eligible: true,
            safe_limit: 62_370_000,
            coverage_ratio: 100.0,
            message: '가입 가능 (안전 ✅)',
        },
        details: {
            jeonse_ratio: 57.4,
            senior_debt: 0,
            is_illegal_building: false,
            is_trust: false,
            building_age: 31.6,
            ownership_duration_months: 4,
        },
        recommendations: [
            'HUG 보증보험 가입을 권장합니다',
            '계약 전 법무사 자문을 통한 권리 관계 검토 권장',
        ],
    },
}
