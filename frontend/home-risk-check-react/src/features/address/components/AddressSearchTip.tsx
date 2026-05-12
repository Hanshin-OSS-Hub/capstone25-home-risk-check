export default function AddressSearchTip() {
    return (
        <div className="flex flex-col gap-2">
            <span className="font-bold">Tip</span>
            <span className="mb-4 text-sm text-gray-500">
                아래와 같은 조합으로 검색하면 더 정확합니다
            </span>
            <span>도로명 + 건물번호</span>
            <span className="text-blue-500">예) 판교역로 166</span>
            <span>지역명 + 번지</span>
            <span className="text-blue-500">예) 백현동 532</span>
            <span>건물명</span>
            <span className="text-blue-500">예) 분당 주공</span>
        </div>
    )
}
