import {useState} from 'react'
import {useNavigate} from 'react-router-dom'
import {Button} from '@/components/ui/button'
import InputCode from '@/components/form/InputCode'
import {AuthShell} from '@/features/auth/components/AuthShell'
import {useSignupStore} from '@/features/auth/stores/useSignupStore'
import {ROUTES} from '@/constants/routes'
// import {authApi} from '@/features/auth/api' // 백엔드 연동 시 활성화

export default function EmailVerifyPage() {
    const navigate = useNavigate()
    const [code, setCode] = useState('')
    const [codeError, setCodeError] = useState('')
    const {email, setIsEmailVerified} = useSignupStore()

    const verifyEmailCode = async () => {
        if (!code) {
            setCodeError('코드를 입력해주세요')
            return
        }
        try {
            // await authApi.verifyEmailCode({ email, code })
            setIsEmailVerified(true)
            navigate(ROUTES.signup)
        } catch {
            setCodeError('인증 코드가 올바르지 않아요.')
        }
    }

    const handleResend = async () => {
        try {
            // await authApi.sendEmailCode(email)
            setCode('')
            setCodeError('')
        } catch {
            setCodeError('인증 코드 발송에 실패했어요. 다시 시도해 주세요.')
        }
    }

    return (
        <AuthShell
            title="이메일 인증"
            description={
                <>
                    <span className="font-medium text-foreground">{email || '입력하신 이메일'}</span>
                    {' '}주소로 전송된<br/>6자리 코드를 입력해주세요
                </>
            }
        >
            <form
                className="flex flex-col items-center gap-6"
                onSubmit={e => {
                    e.preventDefault()
                    verifyEmailCode()
                }}
            >
                <div className="flex flex-col items-center gap-2">
                    <InputCode
                        value={code}
                        onChange={v => {
                            setCode(v)
                            setCodeError('')
                        }}
                    />
                    {codeError && (
                        <span className="text-xs text-destructive">{codeError}</span>
                    )}
                </div>

                <span className="ml-auto text-xs font-medium -mt-4">
                    03:23
                </span>

                <button
                    type="button"
                    onClick={handleResend}
                    className="text-xs text-muted-foreground underline-offset-4 hover:underline cursor-pointer"
                >
                    코드 재전송
                </button>

                <Button type="submit" className="w-full cursor-pointer">
                    인증하기
                </Button>
            </form>
        </AuthShell>
    )
}
