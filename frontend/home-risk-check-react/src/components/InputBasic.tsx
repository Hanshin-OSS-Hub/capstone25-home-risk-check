'use client'

import {useId, useRef} from 'react'
import {CircleXIcon} from 'lucide-react'
import {cn} from '@/lib/utils'
import {Field, FieldDescription, FieldLabel} from "@/components/ui/field"
import {InputGroup, InputGroupAddon, InputGroupInput, InputGroupButton} from "@/components/ui/input-group"

interface InputClearProps {
    isReadOnly?: boolean
    isClearable?: boolean
    label?: string
    placeholder: string
    error?: string
    value: string
    onChange: (value: string) => void
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
                        placeholder,
                        error,
                        value,
                        onChange,
                        className,
                        addonButton
                    }: InputClearProps) => {
    const inputRef = useRef<HTMLInputElement>(null)
    const id = useId()

    const handleClearInput = () => {
        onChange('')
        inputRef.current?.focus()
    }

    return (
        <Field className="gap-2">
            <FieldLabel id={id} className="font-medium text-xs">{label}</FieldLabel>
            <InputGroup className={cn("h-12 bg-gray-100 border-none rounded-xl", className)}>
                <InputGroupInput id={id} readOnly={isReadOnly}
                                 ref={inputRef}
                                 type='text'
                                 placeholder={placeholder}
                                 value={value}
                                 onChange={e => onChange(e.target.value)}
                />
                <InputGroupAddon align="inline-end" className="gap-0">
                    {value && isClearable && (
                        <InputGroupButton onClick={handleClearInput} className="cursor-pointer">
                            <CircleXIcon/>
                        </InputGroupButton>
                    )}
                    {addonButton && (
                        <InputGroupButton
                            onClick={addonButton.onClick}
                            disabled={addonButton.disabled}
                            className="cursor-pointer"
                        >
                            {addonButton.label}
                        </InputGroupButton>
                    )}
                </InputGroupAddon>
            </InputGroup>
            <FieldDescription className="text-xs text-red-700">{error}</FieldDescription>
        </Field>
    )
}

export default InputBasic
