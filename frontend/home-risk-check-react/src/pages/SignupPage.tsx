import {Input} from '@/components/ui/input'
import {Label} from '@/components/ui/label'
import {Button} from '@/components/ui/button'
import InputBasic from '@/components/InputBasic.tsx'
import {EyeIcon, EyeOffIcon, CheckIcon, XIcon} from 'lucide-react'
import {cn} from '@/lib/utils'
import {useMemo, useState} from 'react'
import {Link} from 'react-router-dom'
import {useNavigate} from 'react-router-dom'
import {signupStore} from '@/store/signupStore'
import axios from 'axios'

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const passwordRegex = [
    {regex: /.{8,}/, text: '8자 이상'},
    {regex: /[a-z]/, text: '소문자 포함'},
    {regex: /[A-Z]/, text: '대문자 포함'},
    {regex: /[0-9]/, text: '숫자 포함'},
    {regex: /[!@#$%^&*.]/, text: '특수문자 포함'},
]

const getColor = (score: number) => {
    if (score === 0) return 'bg-border'
    if (score <= 1) return 'bg-destructive'
    if (score <= 2) return 'bg-orange-500 '
    if (score <= 3) return 'bg-amber-500'
    if (score === 4) return 'bg-yellow-400'
    return 'bg-green-500'
}

export default function SignupPage() {
    const navigate = useNavigate()
    const {nickname, setNickname, email, setEmail, isEmailVerified, setIsEmailVerified, password, setPassword, reset } = signupStore()
    const [formErrors, setFormErrors] = useState({nickname: '', email: '', password: ''})
    const [isVisible, setIsVisible] = useState(false)
    const [isNicknameChecked, setIsNicknameChecked] = useState(false)
    const toggleVisibility = () => setIsVisible(prevState => !prevState)

    const strength = passwordRegex.map(req => ({
        met: req.regex.test(password),
        text: req.text
    }))

    const strengthScore = useMemo(() => {
        return strength.filter(req => req.met).length
    }, [strength])

    const sendEmailCode = async () => {
        if (!email) {
            setFormErrors(prev => ({
                ...prev,
                email: '이메일을 입력해주세요'
            }))
            return
        }

        if (!EMAIL_REGEX.test(email)) {
            setFormErrors(prev => ({
                ...prev,
                email: '올바른 이메일 형식이 아니에요'
            }))
            return
        }
        // await axios.post('/api/auth/send-code', { email })
        navigate('/email-verify')
    }

    const checkNickname = async () => {
        if(!nickname){
            setFormErrors(prev => ({
                ...prev,
                nickname: '닉네임을 입력해주세요'
            }))
            return
        }

        try {
            // await axios.get(`/api/auth/check-nickname, {nickname}`)
            setIsNicknameChecked(true)
        } catch (err){
            setFormErrors(prev => ({
                ...prev,
                nickname: '이미 사용중인 닉네임이에요'
            }))
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
            // await axios.post('/api/auth/signup', { nickname, email, password })
            reset()
            navigate('/login')
        } catch (err) {
            if (axios.isAxiosError(err)) {
                const data = err.response?.data
                if (data?.field === 'nickname') {
                    setFormErrors(prev => ({...prev, nickname: '이미 사용 중인 닉네임이에요.'}))
                } else {
                    setFormErrors(prev => ({...prev, email: '이미 사용 중인 이메일이에요.'}))
                }
            }
        }
    }

    return (
        <div className="flex flex-col gap-6">
            <InputBasic label="닉네임"
                        placeholder="바람이분당구"
                        value={nickname}
                        error={formErrors.nickname}
                        onChange={(v) => {
                            setNickname(v)
                            setFormErrors(prev => ({...prev, nickname: ''}))
                            setIsNicknameChecked(false)
                        }}
                        addonButton={{
                            label: isNicknameChecked ? "사용가능" : "중복확인",
                            disabled: isNicknameChecked,
                            onClick:  checkNickname,
                        }}
            />
            <InputBasic label="이메일"
                        placeholder="example@Email.com"
                        value={email}
                        error={formErrors.email}
                        onChange={(v) => {
                            setEmail(v)
                            setFormErrors(prev => ({...prev, email: ''}))
                            setIsEmailVerified(false)
                        }}
                        addonButton={{
                            label: isEmailVerified ? "인증완료" : "인증요청",
                            disabled: isEmailVerified,
                            onClick: sendEmailCode,
                        }}
            />
            <div className="w-full space-y-4">
                <Label htmlFor="password" className="font-medium text-xs mb-2">비밀번호</Label>
                <div className='relative'>
                    <Input
                        id="password"
                        type={isVisible ? 'text' : 'password'}
                        placeholder='Password'
                        value={password}
                        onChange={e => {
                            setPassword(e.target.value)
                            setFormErrors(prev => ({...prev, password: ''}))
                        }}
                        className='pr-8 h-12 bg-gray-100 border-none rounded-xl'
                    />
                    <Button
                        variant='ghost'
                        size='icon'
                        onClick={toggleVisibility}
                        className='text-muted-foreground focus-visible:ring-ring/50 absolute top-1/2 -translate-y-1/2 right-0 rounded-l-none hover:bg-transparent'
                    >
                        {isVisible ? <EyeOffIcon/> : <EyeIcon/>}
                        <span className='sr-only'>{isVisible ? 'Hide password' : 'Show password'}</span>
                    </Button>
                </div>
                <div className='flex h-1 w-full gap-1'>
                    {Array.from({length: 5}).map((_, index) => (
                        <span
                            key={index}
                            className={cn(
                                'h-full flex-1 rounded-full transition-all duration-500 ease-out',
                                index < strengthScore ? getColor(strengthScore) : 'bg-border'
                            )}
                        />
                    ))}
                </div>
                <ul className='space-y-1.5 mb-0'>
                    {strength.map((req, index) => (
                        <li key={index} className='flex items-center gap-2'>
                            {req.met ? (
                                <CheckIcon className='size-4 text-green-600 dark:text-green-400'/>
                            ) : (
                                <XIcon className='text-muted-foreground size-4'/>
                            )}
                            <span
                                className={cn('text-xs', req.met ? 'text-green-600 dark:text-green-400' : 'text-muted-foreground')}>
                                {req.text}
                                <span
                                    className='sr-only'>{req.met ? ' - Requirement met' : ' - Requirement not met'}</span>
                            </span>
                        </li>
                    ))}
                </ul>
                {formErrors.password && (
                    <span className="text-xs text-red-700">{formErrors.password}</span>
                )}
            </div>
            <Button className="h-12 rounded-xl w-full cursor-pointer" onClick={handleSignup}>
                회원가입
            </Button>
            <div className="text-center text-xs">
                <span className="text-muted-foreground">이미 회원이신가요?&nbsp;<Link to="/login" className="underline">로그인</Link></span>
            </div>
        </div>
    )
}
