import {useState} from 'react'
import {Link, useNavigate} from 'react-router-dom'
import {Button} from '@/components/ui/button'
import InputBasic from '@/components/form/InputBasic'
import {AuthShell} from '@/features/auth/components/AuthShell'
import {useLogin} from '@/features/auth/hooks/useLogin'
import {ROUTES} from '@/constants/routes'
import {showToast} from '@/lib/notify'
import {getApiErrorMessage} from '@/lib/api-response'

export default function LoginPage() {
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const navigate = useNavigate()
    const login = useLogin()

    const handleLogin = async () => {
        try {
            await login.mutateAsync({email, password})
            navigate(ROUTES.home, {replace: true})
        } catch (error) {
            showToast({
                message: getApiErrorMessage(error, '로그인에 실패했습니다. 다시 시도해주세요.'),
                variant: 'error',
            })
        }
    }

    return (
        <AuthShell
            title="로그인"
            footer={
                <span className="text-sm text-muted-foreground">
                    계정이 없으신가요?{' '}
                    <Link to={ROUTES.signup} className="font-medium text-foreground underline-offset-4 hover:underline">
                        회원가입
                    </Link>
                </span>
            }
        >
            <form
                className="flex flex-col gap-6"
                onSubmit={e => {
                    e.preventDefault()
                    handleLogin()
                }}
            >
                <InputBasic
                    label="이메일"
                    type="email"
                    placeholder="example@email.com"
                    value={email}
                    autoComplete="email"
                    onChange={setEmail}
                />
                <InputBasic
                    label="비밀번호"
                    type="password"
                    placeholder="비밀번호를 입력하세요"
                    value={password}
                    autoComplete="current-password"
                    onChange={setPassword}
                />
                <Button type="submit" className="w-full cursor-pointer" disabled={login.isPending}>
                    {login.isPending ? '로그인 중…' : '로그인'}
                </Button>
                <Link
                    to={ROUTES.home}
                    className="mx-auto text-xs text-muted-foreground underline-offset-4 hover:underline"
                >
                    비밀번호를 잊어버리셨나요?
                </Link>
            </form>
        </AuthShell>
    )
}
