import { useEffect, useMemo } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import {
    ShieldCheck,
    ShieldAlert,
    AlertTriangle,
    CheckCircle2,
} from 'lucide-react'
import GaugeChart from '@/components/GaugeChart'
import { Card, CardContent, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { ROUTES } from '@/constants/routes'
import { mockAnalysisResult } from '@/__mocks__/mockAnalysisResult'
import type {
    AnalyzeResult,
    RiskLevel,
    Severity,
} from '@/features/analysis/types'

const isMock = () => import.meta.env.VITE_USE_MOCK === 'true'

const RISK_LEVEL_META: Record<
    RiskLevel,
    { label: string; tone: string; chip: string; ring: string }
> = {
    SAFE: {
        label: '안전',
        tone: 'text-emerald-700',
        chip: 'bg-emerald-100 text-emerald-700',
        ring: 'ring-emerald-200',
    },
    LOW: {
        label: '양호',
        tone: 'text-lime-700',
        chip: 'bg-lime-100 text-lime-700',
        ring: 'ring-lime-200',
    },
    MEDIUM: {
        label: '주의',
        tone: 'text-amber-700',
        chip: 'bg-amber-100 text-amber-700',
        ring: 'ring-amber-200',
    },
    HIGH: {
        label: '경고',
        tone: 'text-orange-700',
        chip: 'bg-orange-100 text-orange-700',
        ring: 'ring-orange-200',
    },
    DANGER: {
        label: '위험',
        tone: 'text-red-700',
        chip: 'bg-red-100 text-red-700',
        ring: 'ring-red-200',
    },
}

const SEVERITY_META: Record<Severity, { label: string; chip: string }> = {
    LOW: { label: '낮음', chip: 'bg-blue-50 text-blue-600' },
    MEDIUM: { label: '중간', chip: 'bg-amber-50 text-amber-700' },
    HIGH: { label: '높음', chip: 'bg-red-50 text-red-600' },
}

const PRICE_SOURCE_LABEL: Record<string, string> = {
    DB_Trade: '실거래가 DB',
    DB_Official: '공시지가 DB',
    Estimated: '추정 시세',
}

const formatKRW = (value: number) => {
    if (!Number.isFinite(value) || value <= 0) return '0원'
    const eok = Math.floor(value / 100_000_000)
    const man = Math.floor((value % 100_000_000) / 10_000)
    const parts: string[] = []
    if (eok > 0) parts.push(`${eok.toLocaleString()}억`)
    if (man > 0) parts.push(`${man.toLocaleString()}만`)
    return parts.length ? `${parts.join(' ')}원` : `${value.toLocaleString()}원`
}

const isRiskLevel = (value: string): value is RiskLevel =>
    value in RISK_LEVEL_META

const isSeverity = (value: string): value is Severity =>
    value in SEVERITY_META

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
            <h1 className="font-medium">
                분석이 끝났어요 <br />
                결과를 확인해주세요
            </h1>

            {/* 헤더 카드: 게이지 + 위험 등급 + 주소 */}
            <div className={`flex flex-col items-center gap-2 p-4 rounded-xl bg-gray-50 ${meta.ring}`}>
                <span className="font-semibold text-gray-800 line-clamp-2">
                        {result.address}
                </span>

                <div className="flex flex-col items-center">
                    <GaugeChart score={gaugeScore} />
                    <span className={`text-2xl font-bold ${meta.tone}`}>
                            {result.risk_score.toFixed(1)}
                        </span>
                    <span className={`px-3 py-0.5 rounded-md text-sm font-semibold ${meta.chip}`}>
                        {meta.label}
                    </span>
                </div>
            </div>

            {/* 핵심 지표 그리드 */}
            <section className="flex flex-col gap-2">
                <h2 className="text-sm font-semibold">핵심 지표</h2>
                <div className="grid grid-cols-2 gap-2">
                    <Stat label="보증금" value={formatKRW(result.deposit)} />
                    <Stat label="시세" value={formatKRW(result.market_price)} />
                    {/*<span className="text-xs text-gray-500">*/}
                    {/*    {PRICE_SOURCE_LABEL[result.price_source] ?? result.price_source} 기준*/}
                    {/*</span>*/}
                    <Stat
                        label="전세가율"
                        value={`${details.jeonse_ratio.toFixed(1)}%`}
                        accent={details.jeonse_ratio >= 80 ? 'warn' : 'default'}
                    />
                    <Stat
                        label="건물 연식"
                        value={`${Math.round(details.building_age)}년`}
                    />
                    <Stat
                        label="선순위 채권"
                        value={
                            details.senior_debt > 0
                                ? formatKRW(details.senior_debt)
                                : '없음'
                        }
                        accent={details.senior_debt > 0 ? 'warn' : 'default'}
                    />
                    <Stat
                        label="소유 기간"
                        value={`${details.ownership_duration_months}개월`}
                        accent={
                            details.ownership_duration_months < 12 ? 'warn' : 'default'
                        }
                    />
                </div>
            </section>

            {/* HUG 보증보험 */}
            <section className="flex flex-col gap-2">
                <h2 className="text-sm font-semibold">
                    HUG 전세보증보험
                </h2>
                <Card
                    className={`p-4 rounded-xl gap-2 ring-0 ${hug.is_eligible ? 'bg-emerald-50' : 'bg-red-50'}`}
                >
                    <CardTitle
                        className={`flex items-center gap-1 text-sm font-medium ${
                            hug.is_eligible ? 'text-emerald-800' : 'text-red-800'
                        }`}
                    >
                        {hug.is_eligible ? (
                            <ShieldCheck className="size-4" />
                        ) : (
                            <ShieldAlert className="size-4" />
                        )}
                        {hug.is_eligible ? '가입 가능' : '가입 불가'}
                    </CardTitle>
                    <CardContent className="p-0 flex flex-col gap-3">
                        <div className="grid grid-cols-2 gap-2">
                            <div className="flex flex-col rounded-xl bg-white px-3 py-2">
                                <span className="text-xs font-medium text-gray-800">
                                    안전 한도
                                </span>
                                <span className="text-sm font-semibold text-gray-800">
                                    {formatKRW(hug.safe_limit)}
                                </span>
                            </div>
                            <div className="flex flex-col rounded-xl bg-white px-3 py-2">
                                <span className="text-xs font-medium text-gray-800">
                                    보증 비율
                                </span>
                                <span className="text-sm font-semibold text-gray-800">
                                    {hug.coverage_ratio.toFixed(0)}%
                                </span>
                            </div>
                        </div>
                    </CardContent>
                </Card>
            </section>

            {/* 주요 위험 요인 */}
            <section className="flex flex-col gap-2">
                <h2 className="text-sm font-semibold">
                    주요 위험 요인
                </h2>
                {result.major_risk_factors.length === 0 ? (
                    <div className="flex items-center gap-1 p-4 rounded-xl font-medium bg-emerald-50 text-emerald-700 text-sm">
                        <CheckCircle2 className="size-4" />
                        특이 위험 요인이 발견되지 않았습니다.
                    </div>
                ) : (
                    <ul className="flex flex-col gap-2">
                        {result.major_risk_factors.map((factor, idx) => {
                            const sev = isSeverity(factor.severity)
                                ? SEVERITY_META[factor.severity]
                                : SEVERITY_META.LOW
                            return (
                                <li
                                    key={`${factor.type}-${idx}`}
                                    className="flex items-start gap-2 p-3 rounded-xl bg-gray-50"
                                >
                                    <AlertTriangle className="mt-0.5 size-4 text-gray-500 shrink-0" />
                                    <div className="flex-1 flex flex-col gap-1">
                                        <p className="text-sm font-medium text-gray-800">
                                            {factor.message}
                                        </p>
                                        <span className="text-xs text-gray-500">
                                            {factor.type}
                                        </span>
                                    </div>
                                    {/*<span className={`shrink-0 self-start px-2 py-0.5 rounded-md text-xs font-medium ${sev.chip}`}>*/}
                                    {/*    {sev.label}*/}
                                    {/*</span>*/}
                                </li>
                            )
                        })}
                    </ul>
                )}
            </section>

            {/* 권장 사항 */}
            {result.recommendations.length > 0 && (
                <section className="flex flex-col gap-2">
                    <h2 className="text-sm font-semibold">권장 사항</h2>
                    <Card className="p-4 rounded-xl bg-sky-50 ring-0 gap-2">
                        <CardTitle className="text-sm text-blue-600">
                            이런 점을 고려해보세요
                        </CardTitle>
                        <CardContent className="p-0">
                            <ul className="space-y-2">
                                {result.recommendations.map((text) => (
                                    <li
                                        key={text}
                                        className="flex items-start gap-2 text-sm text-muted-foreground list-none"
                                    >
                                        <span className="mt-2 w-1 h-1 rounded-full bg-muted-foreground shrink-0"/>
                                        <span>{text}</span>
                                    </li>
                                ))}
                            </ul>
                        </CardContent>
                    </Card>
                </section>
            )}

            <Button
                onClick={() => navigate(ROUTES.analysis)}
                className="w-full h-12 rounded-xl cursor-pointer"
            >
                새로 분석하기
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
        <div className="flex flex-col gap-1 px-3 py-3 rounded-xl bg-gray-50">
            <span className="font-medium text-xs text-gray-800">{label}</span>
            <span
                className={`text-sm font-semibold ${
                    accent === 'warn' ? 'text-amber-700' : 'text-gray-800'
                }`}
            >
                {value}
            </span>
        </div>
    )
}