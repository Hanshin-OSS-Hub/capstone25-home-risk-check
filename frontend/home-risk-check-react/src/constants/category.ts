export type CommunityCategoryKey = 'all' | 'damage' | 'fraud' | 'law' | 'region' | 'question'
export const COMMUNITY_CATEGORIES: { key: CommunityCategoryKey; label: string }[] = [
    { key: 'all',      label: '전체' },
    { key: 'damage',   label: '⚠️ 피해 사례' },
    { key: 'fraud',    label: '🚨 사기 의심' },
    { key: 'law',      label: '🧑‍⚖️ 대응/법률' },
    { key: 'region',   label: '🔍 지역 정보' },
    { key: 'question', label: '❓ 질문' },
]