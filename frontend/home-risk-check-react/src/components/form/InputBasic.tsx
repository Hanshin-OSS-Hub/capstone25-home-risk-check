import {useId, useRef, useState} from 'react'
import {CircleXIcon, EyeIcon, EyeOffIcon} from 'lucide-react'
import {cn} from '@/lib/utils'
import {Field, FieldDescription, FieldLabel} from "@/components/ui/field"
import {InputGroup, InputGroupAddon, InputGroupButton, InputGroupInput} from "@/components/ui/input-group"

interface InputClearProps {
    isReadOnly?: boolean
    isClearable?: boolean
    label?: string
    type?: 'text' | 'email' | 'password'
    placeholder: string
    error?: string
    value: string
    autoComplete?: string
    onChange: (value: string) => void
    onKeyDown?: (e: React.KeyboardEvent<HTMLInputElement>) => void
    className?: string
    addonButton?: AddonButton
}

interface AddonButton {
    label: string
    disabled?: boolean
    onClick: () => void
}

const InputBasic = ({
                        isReadOnly = false,
                        isClearable = false,
                        label,
                        type = 'text',
                        placeholder,
                        error,
                        value,
                        autoComplete,
                        onChange,
                        onKeyDown,
                        className,
                        addonButton
                    }: InputClearProps) => {
    const inputRef = useRef<HTMLInputElement>(null)
    const id = useId()
    const [isPasswordVisible, setIsPasswordVisible] = useState(false)

    const isPassword = type === 'password'
    const resolvedType = isPassword ? (isPasswordVisible ? 'text' : 'password') : type

    const handleClearInput = () => {
        onChange('')
        inputRef.current?.focus()
    }

    return (
        <Field className="gap-2">
            {label && (
                <FieldLabel htmlFor={id} className="text-sm font-medium">
                    {label}
                </FieldLabel>
            )}
            <InputGroup
                className={cn(
                    "h-11 rounded-lg border-transparent bg-muted transition-[color,box-shadow]",
                    "focus-within:border-ring focus-within:ring-[3px] focus-within:ring-ring/30",
                    error && "border-destructive focus-within:border-destructive focus-within:ring-destructive/20",
                    className,
                )}
            >
                <InputGroupInput
                    id={id}
                    readOnly={isReadOnly}
                    ref={inputRef}
                    type={resolvedType}
                    placeholder={placeholder}
                    value={value}
                    autoComplete={autoComplete}
                    aria-invalid={error ? true : undefined}
                    onChange={e => onChange(e.target.value)}
                    onKeyDown={onKeyDown}
                />
                <InputGroupAddon align="inline-end" className="gap-0.5">
                    {value && isClearable && !isPassword && (
                        <InputGroupButton
                            type="button"
                            aria-label="입력 지우기"
                            onClick={handleClearInput}
                            className="text-muted-foreground hover:text-foreground cursor-pointer"
                        >
                            <CircleXIcon/>
                        </InputGroupButton>
                    )}
                    {isPassword && (
                        <InputGroupButton
                            type="button"
                            size="icon-xs"
                            aria-label={isPasswordVisible ? '비밀번호 숨기기' : '비밀번호 표시'}
                            onClick={() => setIsPasswordVisible(prev => !prev)}
                            className="text-muted-foreground hover:text-foreground cursor-pointer"
                        >
                            {isPasswordVisible ? <EyeOffIcon/> : <EyeIcon/>}
                        </InputGroupButton>
                    )}
                    {addonButton && (
                        <InputGroupButton
                            type="button"
                            onClick={addonButton.onClick}
                            disabled={addonButton.disabled}
                            className="font-medium text-foreground/70 hover:text-foreground disabled:text-muted-foreground cursor-pointer"
                        >
                            {addonButton.label}
                        </InputGroupButton>
                    )}
                </InputGroupAddon>
            </InputGroup>
            {error && (
                <FieldDescription className="text-xs text-destructive">{error}</FieldDescription>
            )}
        </Field>
    )
}

export default InputBasic
