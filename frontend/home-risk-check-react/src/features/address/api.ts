import axios from 'axios'
import type { JusoPage, SearchJusoParams } from './types'

// ⚠️ 보안: 정부 주소 API를 클라이언트에서 직접 호출 → API 키가 번들에 포함됨.
// 백엔드 프록시 도입 시 이 파일의 baseURL/params 만 교체하면 됨 (호출 지점은 여기 단 한 곳).
const JUSO_BASE_URL = 'https://www.juso.go.kr/addrlink/addrLinkApi.do'

export const addressApi = {
    searchJuso: async ({ keyword, page }: SearchJusoParams): Promise<JusoPage> => {
        const res = await axios.get(JUSO_BASE_URL, {
            params: {
                confmKey: import.meta.env.VITE_JUSO_API_KEY,
                currentPage: page,
                countPerPage: 20,
                keyword,
                resultType: 'json',
            },
        })

        const common = res.data?.results?.common
        const list = (res.data?.results?.juso ?? []) as JusoPage['list']
        const total = Number(common?.totalCount ?? 0)
        const errorMessage = common?.errorCode && common.errorCode !== '0' ? common.errorMessage : undefined

        return { list, total, page, errorMessage }
    },
}
