import {useMemo, useState} from 'react'
import {Link, useNavigate} from 'react-router-dom'
import {Button} from '@/components/ui/button'
import InputBasic from '@/components/form/InputBasic'
import {AuthShell} from '@/features/auth/components/AuthShell'
import {useSignupStore} from '@/features/auth/stores/useSignupStore'
import {ROUTES} from '@/constants/routes'
// import {authApi} from '@/features/auth/api' // 백엔드 연동 시 활성화
import {normalizeApiError} from '@/lib/api-response'

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const passwordRegex = [
    {regex: /.{8,}/, text: '8자 이상'},
    {regex: /[a-z]/, text: '소문자 포함'},
    {regex: /[A-Z]/, text: '대문자 포함'},
    {regex: /[0-9]/, text: '숫자 포함'},
    {regex: /[!@#$%^&*.]/, text: '특수문자 포함'},
]

export default function SignupPage() {
    const navigate = useNavigate()
    const {
        nickname, setNickname, email, setEmail,
        isNicknameChecked, setIsNicknameChecked,
        isEmailVerified, setIsEmailVerified,
        password, setPassword, reset,
    } = useSignupStore()
    const [formErrors, setFormErrors] = useState({nickname: '', email: '', password: ''})

    const strength = useMemo(
        () => passwordRegex.map(req => ({met: req.regex.test(password), text: req.text})),
        [password],
    )
    const strengthScore = useMemo(() => strength.filter(req => req.met).length, [strength])

    const sendEmailCode = async () => {
        if (!email) {
            setFormErrors(prev => ({...prev, email: '이메일을 입력해주세요'}))
            return
        }
        if (!EMAIL_REGEX.test(email)) {
            setFormErrors(prev => ({...prev, email: '올바른 이메일 형식이 아니에요'}))
            return
        }
        // await authApi.sendEmailCode(email)
        navigate(ROUTES.emailVerify)
    }

    const checkNickname = async () => {
        if (!nickname) {
            setFormErrors(prev => ({...prev, nickname: '닉네임을 입력해주세요'}))
            return
        }
        try {
            // await authApi.checkNickname(nickname)
            setIsNicknameChecked(true)
        } catch {
            setFormErrors(prev => ({...prev, nickname: '이미 사용중인 닉네임이에요'}))
        }
    }

    const handleSignup = async () => {
        const newErrors = {nickname: '', email: '', password: ''}

        if (!nickname) {
            newErrors.nickname = '닉네임을 입력해주세요'
        }
        if (!email) {
            newErrors.email = '이메일을 입력해주세요'
        } else if (!EMAIL_REGEX.test(email)) {
            newErrors.email = '올바른 이메일 형식이 아니에요'
        } else if (!isEmailVerified) {
            newErrors.email = '이메일 인증을 완료해주세요'
        }
        if (strengthScore < 5) {
            newErrors.password = '비밀번호 조건을 모두 만족해주세요'
        }

        if (newErrors.password || newErrors.nickname || newErrors.email) {
            setFormErrors(newErrors)
            return
        }

        try {
            // await authApi.signup({ nickname, email, password })
            reset()
            navigate(ROUTES.login)
        } catch (err) {
            const apiError = normalizeApiError(err)
            if (apiError.field === 'nickname') {
                setFormErrors(prev => ({...prev, nickname: apiError.message}))
            } else {
                setFormErrors(prev => ({...prev, email: apiError.message}))
            }
        }
    }

    return (
        <AuthShell
            title="회원가입"
            footer={
                <span className="text-sm text-muted-foreground">
                    이미 회원이신가요?{' '}
                    <Link to={ROUTES.login} className="font-medium text-foreground underline-offset-4 hover:underline">
                        로그인
                    </Link>
                </span>
            }
        >
            <form
                className="flex flex-col gap-6"
                onSubmit={e => {
                    e.preventDefault()
                    handleSignup()
                }}
            >
                <InputBasic
                    label="닉네임"
                    placeholder="바람이분당구"
                    value={nickname}
                    error={formErrors.nickname}
                    onChange={v => {
                        setNickname(v)
                        setFormErrors(prev => ({...prev, nickname: ''}))
                        setIsNicknameChecked(false)
                    }}
                    addonButton={{
                        label: isNicknameChecked ? '사용가능' : '중복확인',
                        disabled: isNicknameChecked,
                        onClick: checkNickname,
                    }}
                />
                <InputBasic
                    label="이메일"
                    type="email"
                    placeholder="example@email.com"
                    value={email}
                    error={formErrors.email}
                    autoComplete="email"
                    onChange={v => {
                        setEmail(v)
                        setFormErrors(prev => ({...prev, email: ''}))
                        setIsEmailVerified(false)
                    }}
                    addonButton={{
                        label: isEmailVerified ? '인증완료' : '인증요청',
                        disabled: isEmailVerified,
                        onClick: sendEmailCode,
                    }}
                />

                <div className="flex flex-col gap-2">
                    <InputBasic
                        label="비밀번호"
                        type="password"
                        placeholder="비밀번호를 입력하세요"
                        value={password}
                        autoComplete="new-password"
                        onChange={v => {
                            setPassword(v)
                            setFormErrors(prev => ({...prev, password: ''}))
                        }}
                    />

                    {formErrors.password && (
                        <span className="text-xs text-destructive">{formErrors.password}</span>
                    )}
                </div>

                <Button type="submit" className="w-full cursor-pointer">
                    회원가입
                </Button>
            </form>
        </AuthShell>
    )
}
