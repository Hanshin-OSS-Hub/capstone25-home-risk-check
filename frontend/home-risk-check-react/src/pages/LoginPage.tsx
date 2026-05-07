import {Button} from '@/components/ui/button'
import InputBasic from '@/components/InputBasic.tsx'
import {useState} from 'react'
import {Link} from 'react-router-dom'
import {useNavigate} from 'react-router-dom'
import axios from 'axios'

export default function LoginPage() {
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const navigate = useNavigate()

    const handleLogin = async () => {
        try {
            const res = await axios.post("/api/login", {
                email,
                password,
            })

            const token = res.data.accessToken

            localStorage.setItem("accessToken", token)

            navigate("/")
        } catch (err) {
            alert(err)
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
                <Link to="/">비밀번호를 잊어버리셨나요?</Link>
                <span className="text-muted-foreground">계정이 없으신가요?&nbsp;<Link to="/signup" className="underline">회원가입</Link></span>
            </div>
        </div>
    )
}
