import { useEffect, useRef, useState } from 'react'
import { useCommunityCreateStore } from '@/features/community/stores/useCommunityCreateStore'
import type { Poll } from '@/features/community/types'

interface Args {
    initial: Poll | null
}

const createEmptyPoll = (): Poll => ({
    options: [
        { id: crypto.randomUUID(), text: '' },
        { id: crypto.randomUUID(), text: '' },
    ],
    multipleChoice: false,
})

/**
 * 투표 작성/편집 중 임시 상태 관리.
 * - draft: 화면 편집용 로컬 상태
 * - commit(): "완료" 시 store.poll 에 반영
 * - 미커밋 unmount: isNew 면 store.poll = null 로 폐기
 */
export function usePollDraft({ initial }: Args) {
    const setPoll = useCommunityCreateStore(s => s.setPoll)
    const isNewRef = useRef(initial === null)
    const [draft, setDraft] = useState<Poll>(() => initial ?? createEmptyPoll())
    const committedRef = useRef(false)

    useEffect(() => () => {
        if (isNewRef.current && !committedRef.current) setPoll(null)
    }, [setPoll])

    const commit = () => {
        committedRef.current = true
        setPoll(draft)
    }

    return { draft, setDraft, commit }
}
