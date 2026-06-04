import type { RiskLevel, Severity } from '@/features/analysis/types'

/** 위험 등급 — 의미 있는 위험 신호이므로 색을 유지한다. */
export const RISK_LEVEL_META: Record<RiskLevel, { label: string; tone: string; chip: string }> = {
    SAFE: { label: '안전', tone: 'text-emerald-700', chip: 'bg-emerald-100 text-emerald-700' },
    LOW: { label: '양호', tone: 'text-lime-700', chip: 'bg-lime-100 text-lime-700' },
    MEDIUM: { label: '주의', tone: 'text-amber-700', chip: 'bg-amber-100 text-amber-700' },
    HIGH: { label: '경고', tone: 'text-orange-700', chip: 'bg-orange-100 text-orange-700' },
    DANGER: { label: '위험', tone: 'text-red-700', chip: 'bg-red-100 text-red-700' },
}

/** 위험 요인 심각도 — LOW는 중립(무채색), MEDIUM/HIGH만 위험 신호 색. */
export const SEVERITY_META: Record<Severity, { label: string; chip: string }> = {
    LOW: { label: '낮음', chip: 'bg-muted text-muted-foreground' },
    MEDIUM: { label: '중간', chip: 'bg-amber-100 text-amber-700' },
    HIGH: { label: '높음', chip: 'bg-red-100 text-red-700' },
}

export const isRiskLevel = (value: string): value is RiskLevel => value in RISK_LEVEL_META
export const isSeverity = (value: string): value is Severity => value in SEVERITY_META
