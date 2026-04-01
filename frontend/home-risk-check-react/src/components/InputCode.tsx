
import { InputOTP, InputOTPGroup, InputOTPSlot } from '@/components/ui/input-otp'

interface InputCodeProps {
    value:    string
    onChange: (value: string) => void
}

const InputCode = ({ value, onChange }: InputCodeProps) => {
    return (
        <div className='w-full flex flex-col gap-2'>
            {/*<span className="font-medium text-xs ">인증 코드</span>*/}
            <InputOTP maxLength={6} value={value} onChange={(v) => {
                const filtered = v.replace(/[^A-Za-z0-9]/g, '').toUpperCase()
                onChange(filtered)
            }} className="w-full" inputMode="text" pattern="[A-Za-z0-9]*">
                <InputOTPGroup className='w-full gap-2 *:data-[slot=input-otp-slot]:bg-muted *:data-[slot=input-otp-slot]:flex-1 *:data-[slot=input-otp-slot]:h-12 *:data-[slot=input-otp-slot]:rounded-md *:data-[slot=input-otp-slot]:border *:data-[slot=input-otp-slot]:border-transparent *:data-[slot=input-otp-slot]:shadow-none'>
                    <InputOTPSlot index={0} />
                    <InputOTPSlot index={1} />
                    <InputOTPSlot index={2} />
                    <InputOTPSlot index={3} />
                    <InputOTPSlot index={4} />
                    <InputOTPSlot index={5} />
                </InputOTPGroup>
            </InputOTP>
        </div>
    )
}

export default InputCode
