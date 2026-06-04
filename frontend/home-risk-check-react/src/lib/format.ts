/** 숫자 문자열을 만/억/조 단위 한글 통화로 (예: "12000000" → "1,200만원"). 빈 값/0은 "0원". */
export const formatKoreanCurrency = (input: string) => {
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

/** 숫자 문자열에 천단위 콤마 (빈 값은 ''). */
export const formatNumberWithComma = (value: string) => {
    if (!value) return ''
    return Number(value).toLocaleString()
}

/** 원 단위 number를 억/만 한글 통화로 (예: 350000000 → "3억 5,000만원"). 0 이하/비유한은 "0원". */
export const formatKRW = (value: number) => {
    if (!Number.isFinite(value) || value <= 0) return '0원'
    const eok = Math.floor(value / 100_000_000)
    const man = Math.floor((value % 100_000_000) / 10_000)
    const parts: string[] = []
    if (eok > 0) parts.push(`${eok.toLocaleString()}억`)
    if (man > 0) parts.push(`${man.toLocaleString()}만`)
    return parts.length ? `${parts.join(' ')}원` : `${value.toLocaleString()}원`
}
