import { useEffect, useRef, useState } from 'react'
import { communityCreateStore } from '@/features/community/stores/communityCreateStore'
import type { Poll } from '@/features/community/types'

interface Args {
    initial: Poll | null
    /** 새로 만드는 경우(true): 미커밋 unmount 시 store.poll 을 비움. 편집(false): 보존. */
    isNew: boolean
}

/**
 * 투표 작성/편집 중 임시 상태 관리.
 * - draft: 화면 편집용 로컬 상태
 * - commit(): "완료" 시 store.poll 에 반영
 * - 미커밋 unmount: isNew 면 store.poll = null 로 폐기
 */
export function usePollDraft({ initial, isNew }: Args) {
    const setPoll = communityCreateStore(s => s.setPoll)
    const [draft, setDraft] = useState<Poll | null>(initial)
    const committedRef = useRef(!isNew)

    useEffect(() => {
        return () => {
            if (!committedRef.current) setPoll(null)
        }
    }, [setPoll])

    const commit = () => {
        if (!draft) return
        committedRef.current = true
        setPoll(draft)
    }

    return { draft, setDraft, commit }
}
