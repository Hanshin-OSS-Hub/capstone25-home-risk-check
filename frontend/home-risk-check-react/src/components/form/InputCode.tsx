
import { InputOTP, InputOTPGroup, InputOTPSlot } from '@/components/ui/input-otp'

interface InputCodeProps {
    value:    string
    onChange: (value: string) => void
}

const InputCode = ({ value, onChange }: InputCodeProps) => {
    return (
        <div className='flex flex-col gap-2'>
            <InputOTP maxLength={6} value={value} onChange={(v) => {
                const filtered = v.replace(/[^0-9]/g, '').toUpperCase()
                onChange(filtered)
            }} inputMode="text" pattern="[0-9]*">
                <InputOTPGroup className='justify-center gap-5 *:data-[slot=input-otp-slot]:bg-muted *:data-[slot=input-otp-slot]:size-12 *:data-[slot=input-otp-slot]:rounded-lg *:data-[slot=input-otp-slot]:border *:data-[slot=input-otp-slot]:border-transparent *:data-[slot=input-otp-slot]:text-lg *:data-[slot=input-otp-slot]:shadow-none'>
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
