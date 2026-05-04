import { toast } from 'sonner'

type ToastVariant = 'default' | 'success' | 'error' | 'warning' | 'info'

interface ShowToastOptions {
    message: string
    /** sonner variant. 'default' 외엔 색상/아이콘이 입혀짐. */
    variant?: ToastVariant
    /** 미지정 시 — 액션 버튼 있으면 Infinity, 없으면 sonner 기본값. */
    duration?: number
    actionLabel?: string
    cancelLabel?: string
    onConfirm?: () => void
    onCancel?: () => void
}

/**
 * 단일 진입점 토스트 헬퍼.
 * - 단순 알림: variant + message (+ duration)
 * - 사용자 확인: actionLabel/cancelLabel + onConfirm/onCancel
 */
export const showToast = ({
    message,
    variant = 'default',
    duration,
    actionLabel,
    cancelLabel,
    onConfirm,
    onCancel,
}: ShowToastOptions) => {
    const hasAction = !!(actionLabel && onConfirm)
    const hasCancel = !!(cancelLabel && onCancel)
    const fn = variant === 'default' ? toast : toast[variant]

    fn(message, {
        position: 'top-center',
        duration: duration ?? (hasAction ? Infinity : undefined),
        ...(hasAction && {
            action: { label: actionLabel!, onClick: onConfirm! },
        }),
        ...(hasCancel && {
            cancel: { label: cancelLabel!, onClick: onCancel! },
        }),
    })
}
