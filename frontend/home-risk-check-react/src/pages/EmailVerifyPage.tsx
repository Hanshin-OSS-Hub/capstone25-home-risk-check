import {Button} from '@/components/ui/button'
import InputCode from '@/components/InputCode'
import {useState} from 'react'
import {useNavigate} from 'react-router-dom'
import {signupStore} from '@/store/signupStore'

export default function EmailVerifyPage() {
    const navigate = useNavigate()
    const [code, setCode] = useState('')
    const [codeError, setCodeError] = useState('')
    const { email, setIsEmailVerified } = signupStore()

    const verifyEmailCode = async () => {
        if (!code) {
            setCodeError('코드를 입력해주세요')
            return
        }

        try {
            // await axios.post('/api/auth/verify-code', { email, code })
            setIsEmailVerified(true)
            navigate('/signup')
        } catch {
            setCodeError('인증 코드가 올바르지 않아요.')
        }
    }

    const handleResend = async () => {
        try {
            // await axios.post('/api/auth/send-code', { email })
            setCode('')
            setCodeError('')
        } catch {
            setCodeError('인증 코드 발송에 실패했어요. 다시 시도해 주세요.')
        }
    }

    return (
        <div className="flex flex-col gap-4">
            <div className="flex flex-col items-center">
                <span className="mb-2 font-medium">이메일 본인 인증</span>
                <span>
                            <span className="font-medium">{email}</span>
                    &nbsp;주소로
                        </span>
                <span>전송된 코드를 입력해주세요</span>
            </div>
            <div>
                <InputCode
                    value={code}
                    onChange={(v) => {
                        setCode(v)
                        setCodeError('')
                    }}
                />
                {codeError && (
                    <span className="font-medium text-xs text-red-700">{codeError}</span>
                )}
            </div>
            <span className="mx-auto text-xs underline cursor-pointer w-fit"
                  onClick={handleResend}>코드 재전송</span>
            <Button
                className="h-12 rounded-xl w-full cursor-pointer"
                onClick={verifyEmailCode}
            >
                인증하기
            </Button>
        </div>
    )
}
