import InputClear from '@/components/InputClear'
import InputFile from '@/components/InputFile'
import {Button} from '@/components/ui/button'
import {Card, CardTitle, CardContent} from '@/components/ui/card'
import {useEffect, useRef, useState} from 'react'
import {useNavigate, useLocation} from 'react-router-dom'
import axios from 'axios'

export default function AnalysisPage() {
    const [address, setAddress] = useState('')
    const [detailAddress, setDetailAddress] = useState('')
    const [deposit, setDeposit] = useState('')
    const [registryFiles, setRegistryFiles] = useState<File[]>([]); // 등기부등본
    const [buildingFiles, setBuildingFiles] = useState<File[]>([]); // 건축물대장
    const [isLoading, setIsLoading] = useState(false)
    const navigate = useNavigate()
    const location = useLocation()
    const abortControllerRef = useRef<AbortController | null>(null)

    useEffect(() => {
        if (location.state?.address) {
            setAddress(location.state.address)
            // state 초기화
            window.history.replaceState({}, document.title)
        }
    }, [location.state])

    useEffect(() => {
        return () => {
            abortControllerRef.current?.abort()
        }
    }, [])

    const formatKoreanCurrency = (input: string) => {
        const numeric = input.replace(/[^0-9]/g, '')
        const amount = Number(numeric)
        const result: string[] = []
        const unitNames = ["", "만", "억", "조", "경", "해"]

        if (!amount || !numeric) return '0원'

        let value = amount
        let unitIndex = 0

        while (value > 0) {
            const chunk = value % 10000
            if (chunk > 0) {
                result.unshift(`${chunk.toLocaleString()}${unitNames[unitIndex]}`)
            }
            value = Math.floor(value / 10000)
            unitIndex++
        }
        return result.join(' ') + '원'
    }

    const formatNumberWithComma = (value: string) => {
        if (!value) return ''
        return Number(value).toLocaleString()
    }

    const handleAnalysisRequest = async () => {
        abortControllerRef.current = new AbortController()
        setIsLoading(true)

        const formData = new FormData()

        formData.append("address", address)
        formData.append("detailAddress", detailAddress)
        formData.append("deposit", deposit)

        registryFiles.forEach((file) => {
            formData.append("registryFiles", file)
        })

        buildingFiles.forEach((file) => {
            formData.append("buildingFiles", file)
        })

        try {
            const res = await axios.post("/api/analyze", formData, {
                headers: {"Content-Type": "multipart/form-data"},
                signal: abortControllerRef.current.signal,
            })
            // sessionStorage에 백업 저장
            sessionStorage.setItem("analysisResult", JSON.stringify(res.data))
            navigate("/analysis/result", {state: {result: res.data}})
        } catch (err) {
            if (axios.isCancel(err)) return
        } finally {
            setIsLoading(false)
        }
    }

    return (
        <>
            <div className="px-4 my-6">
                <div className="flex flex-col gap-6 max-w-(--breakpoint-md) mx-auto w-full">
                    <h1 className="font-medium">분석에 필요한 <br/> 정보를 입력해주세요</h1>
                    <Card className="p-4 rounded-xl bg-sky-50 ring-0 gap-2">
                        <CardTitle className="text-sm text-blue-500">분석을 시작하기 전에 확인해주세요!</CardTitle>
                        <CardContent className="p-0">
                            <ul className="space-y-2">
                                {[
                                    "분석 결과는 참고용으로만 활용해주세요.",
                                    "업로드한 파일은 분석 후 즉시 삭제되며 어떠한 형태로도 저장되지 않습니다.",
                                    "정확한 분석을 위해 최신 등기부등본과 건축물대장을 업로드해주세요.",
                                    "서비스 이용 과정에서 발생하는 판단 및 선택의 책임은 사용자에게 있어요.",
                                ].map((text) => (
                                    <li key={text}
                                        className="flex items-start gap-2 text-xs text-muted-foreground list-none">
                                        <span className="mt-1.5 w-1 h-1 rounded-full bg-muted-foreground shrink-0"/>
                                        <span>{text}</span>
                                    </li>
                                ))}
                            </ul>
                        </CardContent>
                    </Card>
                    <div className="flex flex-col sm:flex-row sm:items-end gap-2">
                        <InputClear label="도로명 주소" placeholder="예) 서울시 강남구 테헤란로 123" value={address} onChange={setAddress}/>
                        <Button className="h-12 rounded-xl cursor-pointer" onClick={() => navigate('/address-search')}>주소 검색</Button>
                    </div>
                    <InputClear label="상세 주소" placeholder="예) 101동 202호" value={detailAddress}
                                onChange={setDetailAddress}/>
                    <InputClear label="보증금" placeholder="예) 12000000" value={formatNumberWithComma(deposit)} onChange={(val) => {
                        const onlyNumber = val.replace(/[^0-9]/g, '')
                        setDeposit(onlyNumber)
                    }}/>
                    <p className="flex items-center text-sm bg-gray-100 px-4 py-2 h-12 rounded-xl text-muted-foreground -mt-4">
                        {formatKoreanCurrency(deposit)}
                    </p>
                    <InputFile label="등기부등본" placeholder="파일을 드래그하거나 클릭하여 업로드해주세요"
                               fileIssueUrl="https://www.iros.go.kr/index.jsp" files={registryFiles}
                               onValueChange={setRegistryFiles}/>
                    <InputFile label="건축물대장" placeholder="파일을 드래그하거나 클릭하여 업로드해주세요"
                               fileIssueUrl="https://www.gov.kr/mw/AA020InfoCappView.do?CappBizCD=15000000098"
                               files={buildingFiles} onValueChange={setBuildingFiles}/>
                    <Button
                        onClick={handleAnalysisRequest}
                        disabled={isLoading}
                        className="w-full h-12 bg-blue-500 hover:bg-blue-600 text-white rounded-xl cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        {isLoading ? "분석 중..." : "분석하기"}
                    </Button>
                </div>
            </div>
        </>
    )
}