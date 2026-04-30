export {}

declare global {
    interface Window {
        // 4단계에서 useKakaoMap 도입 시 정밀 타입으로 교체
        kakao: any
    }
}
