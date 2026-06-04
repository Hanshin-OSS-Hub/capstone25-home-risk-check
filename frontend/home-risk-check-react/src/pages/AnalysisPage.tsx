import InputBasic from '@/components/form/InputBasic.tsx'
import InputFile from '@/components/form/InputFile'
import {Button} from '@/components/ui/button'
import {useEffect, useRef, useState} from 'react'
import {useNavigate, useLocation} from 'react-router-dom'
import {useAnalyze} from '@/features/analysis/hooks/useAnalyze'
import {ROUTES} from '@/constants/routes'
import {isCanceledApiError} from '@/lib/api-response'
import {formatKoreanCurrency, formatNumberWithComma} from '@/lib/format'

export default function AnalysisPage() {
    const [address, setAddress] = useState('')
    const [detailAddress] = useState('')
    const [deposit, setDeposit] = useState('')
    const [registryFiles, setRegistryFiles] = useState<File[]>([]); // 등기부등본
    const [buildingFiles, setBuildingFiles] = useState<File[]>([]); // 건축물대장
    const navigate = useNavigate()
    const location = useLocation()
    const abortControllerRef = useRef<AbortController | null>(null)
    const analyze = useAnalyze()

    useEffect(() => {
        if (location.state?.address) {
            setAddress(location.state.address)
            // router state 비우기 — 새로고침/뒤로가기 시 재주입 방지
            navigate(location.pathname, { replace: true, state: null })
        }
    }, [location.state, location.pathname, navigate])

    useEffect(() => {
        return () => {
            abortControllerRef.current?.abort()
        }
    }, [])

    const handleAnalysisRequest = async () => {
        abortControllerRef.current = new AbortController()
        try {
            const data = await analyze.mutateAsync({
                address, detailAddress, deposit, registryFiles, buildingFiles,
                signal: abortControllerRef.current.signal,
            })
            navigate(ROUTES.analysisResult(data.task_id), { state: { result: data } })
        } catch (err) {
            if (isCanceledApiError(err)) return
        }
    }

    const addDepositAmount = (amount: number) => {
        setDeposit(prev => {
            const current = Number(prev || '0')
            return String(current + amount)
        })
    }

    return (
        <>
            <div className="flex flex-col gap-1">
                <h1 className="text-xl font-semibold tracking-tight">분석에 필요한 정보를 입력해주세요</h1>
                <p className="text-sm text-muted-foreground">전세사기 위험도와 지역 안전 등급을 분석해드려요</p>
            </div>
            <div className="flex flex-col gap-2 rounded-lg bg-muted p-4">
                <p className="text-sm font-medium text-foreground">분석을 시작하기 전에 확인해주세요</p>
                <ul className="flex flex-col gap-2">
                    {[
                        "분석 결과는 참고용으로만 활용해주세요.",
                        "업로드한 파일은 분석 후 즉시 삭제되며 어떠한 형태로도 저장되지 않습니다.",
                        "정확한 분석을 위해 최신 등기부등본과 건축물대장을 업로드해주세요.",
                        "서비스 이용 과정에서 발생하는 판단 및 선택의 책임은 사용자에게 있어요.",
                    ].map((text) => (
                        <li
                            key={text}
                            className="flex items-start gap-2 text-xs text-muted-foreground list-none"
                        >
                            <span className="mt-1.5 size-1 rounded-full bg-muted-foreground/60 shrink-0"/>
                            <span>{text}</span>
                        </li>
                    ))}
                </ul>
            </div>
            <div className="flex flex-col sm:flex-row sm:items-end gap-2">
                <InputBasic label="도로명 주소"
                            placeholder="예) 서울시 강남구 테헤란로 123"
                            value={address}
                            onChange={setAddress}
                            isReadOnly={true}
                            addonButton={{
                                label:"주소 검색",
                                onClick :() => navigate(ROUTES.addressSearch, {state: {from: location.pathname}})
                            }}
                />
            </div>
            {/*<InputBasic label="상세 주소" placeholder="예) 101동 202호" value={detailAddress}*/}
            {/*       onChange={setDetailAddress} isClearable={true}/>*/}
            <InputBasic label="보증금" placeholder="예) 12000000" value={formatNumberWithComma(deposit)} isClearable={true} onChange={(val) => {
                const onlyNumber = val.replace(/[^0-9]/g, '')
                setDeposit(onlyNumber)
            }}/>
            <div className="flex items-center -mt-4 justify-between gap-2">
                <div className="text-sm text-muted-foreground px-3 py-1.5 rounded-lg bg-muted">
                    {formatKoreanCurrency(deposit)}
                </div>
                <div className="flex gap-1.5">
                    <Button
                        size="sm"
                        variant="secondary"
                        className="cursor-pointer"
                        onClick={() => addDepositAmount(1_000_000)}
                    >
                        + 1백만
                    </Button>

                    <Button
                        size="sm"
                        variant="secondary"
                        className="cursor-pointer"
                        onClick={() => addDepositAmount(10_000_000)}
                    >
                        + 1천만
                    </Button>

                    <Button
                        size="sm"
                        variant="secondary"
                        className="cursor-pointer"
                        onClick={() => addDepositAmount(100_000_000)}
                    >
                        + 1억
                    </Button>
                </div>
            </div>
            <InputFile label="등기부등본" placeholder="파일을 드래그하거나 클릭하여 업로드해주세요"
                       fileIssueUrl="https://www.iros.go.kr/index.jsp" files={registryFiles}
                       onValueChange={setRegistryFiles}/>
            <InputFile label="건축물대장" placeholder="파일을 드래그하거나 클릭하여 업로드해주세요"
                       fileIssueUrl="https://www.gov.kr/mw/AA020InfoCappView.do?CappBizCD=15000000098"
                       files={buildingFiles} onValueChange={setBuildingFiles}/>
            <Button
                onClick={handleAnalysisRequest}
                disabled={analyze.isPending}
                className="w-full cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
            >
                {analyze.isPending ? "분석 중..." : "분석하기"}
            </Button>
        </>
    )
}
