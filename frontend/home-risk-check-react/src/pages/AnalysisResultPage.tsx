import GaugeChart from '@/components/GaugeChart'

export default function AnalysisResultPage() {

    return (
        <>
            <div className="px-4 my-6">
                <div className="flex flex-col gap-6 max-w-(--breakpoint-md) mx-auto w-full">
                    <h1 className="font-medium">분석이 끝났어요 <br/> 결과를 확인해주세요</h1>
                    <div className="flex flex-col gap-4 p-4 rounded-xl items-center bg-gray-100">
                        <div className="flex flex-col items-center gap-0.5">
                            <span className="font-extrabold text-gray-700">청솔마을 한라아파트 101동 101호</span>
                            <span className="text-xs font-medium text-gray-500">경기도 성남시 분당구 정자일로 72</span>
                        </div>
                        <div className="flex flex-col items-center">
                            <GaugeChart score={36}/>
                            <span className="text-red-700 px-3 w-fit rounded-xl font-extrabold">위험</span>
                            <span className="text-xs text-gray-500">2026.0.31 16:21 기준</span>
                        </div>
                    </div>
                    <div>
                    {/*내용 추가 예정*/}
                    </div>
                </div>
            </div>
        </>
    )
}