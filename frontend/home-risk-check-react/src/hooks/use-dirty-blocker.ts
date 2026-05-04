import { useEffect } from 'react'
import { useBlocker } from 'react-router-dom'
import { showToast } from '@/lib/notify.ts'

interface Options {
    isDirty: boolean
    /** 차단을 건너뛸 경로(prefix). 같은 폼의 보조 페이지로 이동할 때 사용. */
    allowPaths?: string[]
    /** 사용자가 "나가기" 선택 시 실행 (예: store reset). */
    onLeave?: () => void
    message?: string
}

/**
 * 더러운 폼에서 다른 경로로 이동 시 토스트로 확인을 받는다.
 * 사용처에서 useBlocker + useEffect + toast 를 직접 조합하지 않게 함.
 */
export function useDirtyBlocker({
    isDirty,
    allowPaths = [],
    onLeave,
    message = '작성 중인 내용이 사라집니다.',
}: Options) {
    const blocker = useBlocker(({ currentLocation, nextLocation }) =>
        isDirty &&
        currentLocation.pathname !== nextLocation.pathname &&
        !allowPaths.some(p => nextLocation.pathname.startsWith(p)),
    )

    useEffect(() => {
        if (blocker.state !== 'blocked') return
        showToast({
            message,
            actionLabel: '나가기',
            cancelLabel: '취소',
            onConfirm: () => {
                onLeave?.()
                blocker.proceed()
            },
            onCancel: () => blocker.reset(),
        })
    }, [blocker, message, onLeave])
}
