/**
 * 라우트 path 단일 진실 소스.
 * - 정적 path 는 string 리터럴.
 * - 동적 segment 가 있는 경로는 함수.
 * - router 등록용(params 토큰 포함) 은 ROUTE_PATTERNS 로 따로 보관.
 */
export const ROUTES = {
    home: '/',
    login: '/login',
    signup: '/signup',
    emailVerify: '/email-verify',
    addressSearch: '/address-search',
    analysis: '/analysis',
    analysisResult: (analysisId: string | number) => `/analysis/${analysisId}`,
    community: '/community',
    communityDetail: (postId: string | number) => `/community/${postId}`,
    communityNew: '/community/new',
    communityPollNew: '/community/poll/new',
    placeSearch: '/place-search',
} as const

export const ROUTE_PATTERNS = {
    analysisResult: '/analysis/:analysisId',
    communityDetail: '/community/:postId',
} as const
