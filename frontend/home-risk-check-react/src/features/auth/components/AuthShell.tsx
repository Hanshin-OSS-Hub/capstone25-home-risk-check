import type {ReactNode} from "react"

interface AuthShellProps {
    title: string
    description?: ReactNode
    children: ReactNode
    footer?: ReactNode
}

/** 로그인/회원가입/이메일 인증 화면의 공통 셸 — 카드 없이 중앙 정렬된 제목 + 본문 + 푸터. */
export function AuthShell({title, description, children, footer}: AuthShellProps) {
    return (
        <div className="flex flex-col gap-6">
            <div className="flex flex-col items-center gap-2 text-center">
                <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
                {description && (
                    <p className="text-sm text-muted-foreground">{description}</p>
                )}
            </div>
            {children}
            {footer && (
                <div className="flex justify-center">{footer}</div>
            )}
        </div>
    )
}
