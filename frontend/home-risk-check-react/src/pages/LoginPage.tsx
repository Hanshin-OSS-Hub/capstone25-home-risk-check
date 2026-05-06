import {Button} from '@/components/ui/button'
import InputBasic from '@/components/InputBasic.tsx'
import {useState} from 'react'
import {Link, useNavigate} from 'react-router-dom'
import {useLogin} from '@/features/auth/hooks/useLogin'
import {ROUTES} from '@/constants/routes'
import {showToast} from '@/lib/notify.ts'
import {getApiErrorMessage} from '@/lib/api-response'

export default function LoginPage() {
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const navigate = useNavigate()
    const login = useLogin()

    const handleLogin = async () => {
        try {
            // 토큰은 httpOnly 쿠키로 서버에서 내려옴 — 클라이언트는 저장하지 않음
            await login.mutateAsync({ email, password })
            navigate(ROUTES.home, { replace: true })
        } catch (error) {
            showToast({
                message: getApiErrorMessage(error, '로그인에 실패했습니다. 다시 시도해주세요.'),
                variant: 'error',
            })
        }
    }

    return (
        <div className="flex flex-col gap-6">
            <InputBasic label="이메일" placeholder="example@Email.com" value={email} onChange={setEmail}/>
            <InputBasic label="비밀번호" placeholder="Password" value={password} onChange={setPassword}/>
            <Button className="h-12 rounded-xl w-full cursor-pointer" onClick={handleLogin}>
                로그인
            </Button>
            <div className="flex flex-col gap-4 items-center text-xs">
                <Link to={ROUTES.home}>비밀번호를 잊어버리셨나요?</Link>
                <span className="text-muted-foreground">계정이 없으신가요?&nbsp;<Link to={ROUTES.signup} className="underline">회원가입</Link></span>
            </div>
        </div>
    )
}
