import { useEffect, useMemo } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { CheckCircle2 } from 'lucide-react'
import GaugeChart from '@/features/analysis/components/GaugeChart'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { formatKRW } from '@/lib/format'
import { ROUTES } from '@/constants/routes'
import { mockAnalysisResult } from '@/__mocks__/mockAnalysisResult'
import {
    RISK_LEVEL_META,
    SEVERITY_META,
    isRiskLevel,
    isSeverity,
} from '@/features/analysis/constants'
import type { AnalyzeResult } from '@/features/analysis/types'

const isMock = () => import.meta.env.VITE_USE_MOCK === 'true'

export default function AnalysisResultPage() {
    const location = useLocation()
    const navigate = useNavigate()
    const stateResult = (location.state as { result?: AnalyzeResult } | null)?.result
    const payload = stateResult ?? (isMock() ? mockAnalysisResult : undefined)

    useEffect(() => {
        if (!payload) navigate(ROUTES.analysis, { replace: true })
    }, [payload, navigate])

    const result = payload?.result

    const meta = useMemo(() => {
        const level = result?.risk_level ?? 'MEDIUM'
        return isRiskLevel(level) ? RISK_LEVEL_META[level] : RISK_LEVEL_META.MEDIUM
    }, [result?.risk_level])

    if (!result) return null

    const gaugeScore = Math.min(100, Math.max(0, Math.round(result.risk_score * 10)))
    const hug = result.hug_result
    const details = result.details

    return (
        <>
            <div className="flex flex-col gap-1">
                <h1 className="text-xl font-semibold tracking-tight">분석이 끝났어요</h1>
                <p className="text-sm text-muted-foreground">결과를 확인해주세요</p>
            </div>

            {/* 헤더: 게이지 + 점수 + 위험 등급 + 주소 (UI는 무채색, 게이지·점수·등급만 색) */}
            <div className="flex flex-col items-center gap-3 rounded-lg bg-muted p-5">
                <span className="line-clamp-2 text-center font-medium text-foreground">
                    {result.address}
                </span>
                <div className="flex flex-col items-center gap-1">
                    <GaugeChart score={gaugeScore} />
                    <span className={cn('rounded-md px-3 py-0.5 text-sm font-semibold', meta.chip)}>
                        {meta.label}
                    </span>
                </div>
            </div>

            {/* 핵심 지표 */}
            <section className="flex flex-col gap-2">
                <h2 className="text-sm font-semibold">핵심 지표</h2>
                <div className="grid grid-cols-2 gap-2">
                    <Stat label="보증금" value={formatKRW(result.deposit)} />
                    <Stat label="시세" value={formatKRW(result.market_price)} />
                    <Stat
                        label="전세가율"
                        value={`${details.jeonse_ratio.toFixed(1)}%`}
                        accent={details.jeonse_ratio >= 80 ? 'warn' : 'default'}
                    />
                    <Stat label="건물 연식" value={`${Math.round(details.building_age)}년`} />
                    <Stat
                        label="선순위 채권"
                        value={details.senior_debt > 0 ? formatKRW(details.senior_debt) : '없음'}
                        accent={details.senior_debt > 0 ? 'warn' : 'default'}
                    />
                    <Stat
                        label="소유 기간"
                        value={`${details.ownership_duration_months}개월`}
                        accent={details.ownership_duration_months < 12 ? 'warn' : 'default'}
                    />
                </div>
            </section>

            {/* HUG 보증보험 — 평탄화: 상태는 칩, 수치는 핵심지표와 같은 타일 */}
            <section className="flex flex-col gap-2">
                <div className="flex items-center justify-between">
                    <h2 className="text-sm font-semibold">HUG 전세보증보험</h2>
                    <span
                        className={cn(
                            'rounded-md px-2 py-0.5 text-xs font-medium',
                            hug.is_eligible ? 'bg-emerald-100 text-emerald-700' : 'bg-red-100 text-red-700',
                        )}
                    >
                        {hug.is_eligible ? '가입 가능' : '가입 불가'}
                    </span>
                </div>
                <div className="grid grid-cols-2 gap-2">
                    <Stat label="안전 한도" value={formatKRW(hug.safe_limit)} />
                    <Stat label="보증 비율" value={`${hug.coverage_ratio.toFixed(0)}%`} />
                </div>
            </section>

            {/* 주요 위험 요인 */}
            <section className="flex flex-col gap-2">
                <h2 className="text-sm font-semibold">주요 위험 요인</h2>
                {result.major_risk_factors.length === 0 ? (
                    <div className="flex items-center gap-1.5 rounded-lg border border-border px-4 py-3.5 text-sm text-muted-foreground">
                        <CheckCircle2 className="size-4 text-emerald-600" />
                        특이 위험 요인이 발견되지 않았습니다.
                    </div>
                ) : (
                    <div className="flex flex-col rounded-lg bg-muted px-4">
                        {result.major_risk_factors.map((factor, idx) => {
                            const sev = isSeverity(factor.severity)
                                ? SEVERITY_META[factor.severity]
                                : SEVERITY_META.LOW
                            return (
                                <div
                                    key={`${factor.type}-${idx}`}
                                    className={cn(
                                        'flex items-center justify-between gap-3 py-3.5',
                                        idx > 0 && 'border-t border-border',
                                    )}
                                >
                                    <div className="flex flex-col gap-0.5">
                                        <p className="text-sm font-medium text-muted-foreground">{factor.message}</p>
                                    </div>
                                    <span className={cn('shrink-0 rounded-md px-2 py-0.5 text-xs font-medium', sev.chip)}>
                                        {sev.label}
                                    </span>
                                </div>
                            )
                        })}
                    </div>
                )}
            </section>

            {/* 권장 사항 — 위험 신호가 아니므로 무채색, 구분선 리스트 */}
            {result.recommendations.length > 0 && (
                <section className="flex flex-col gap-2">
                    <h2 className="text-sm font-semibold">권장 사항</h2>
                    <div className="flex flex-col rounded-lg bg-muted px-4">
                        {result.recommendations.map((text, idx) => (
                            <p
                                key={text}
                                className={cn(
                                    'py-3 text-sm font-medium text-muted-foreground',
                                    idx > 0 && 'border-t border-border',
                                )}
                            >
                                {text}
                            </p>
                        ))}
                    </div>
                </section>
            )}

            <Button
                onClick={() => navigate(ROUTES.analysis)}
                className="w-full cursor-pointer"
            >
                다시 분석하기
            </Button>
        </>
    )
}

interface StatProps {
    label: string
    value: string
    accent?: 'default' | 'warn'
}

function Stat({ label, value, accent = 'default' }: StatProps) {
    return (
        <div className="flex flex-col gap-1 rounded-lg bg-muted px-3 py-3">
            <span className="text-xs font-medium text-muted-foreground">{label}</span>
            <span className={cn('text-sm font-semibold', accent === 'warn' ? 'text-amber-700' : 'text-foreground')}>
                {value}
            </span>
        </div>
    )
}
