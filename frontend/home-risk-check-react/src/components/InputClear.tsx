'use client'

import { useId, useRef } from 'react'

import { CircleXIcon } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {cn} from '@/lib/utils'

interface InputClearProps {
    isReadOnly?: boolean
    label?: string
    placeholder: string
    value: string
    onChange: (value: string) => void
    className?: string
}

const InputClear = ({isReadOnly=false, label, placeholder, value, onChange, className}: InputClearProps) => {
    const inputRef = useRef<HTMLInputElement>(null)
    const id = useId()

    const handleClearInput = () => {
        onChange('')
        inputRef.current?.focus()
    }

    return (
        <div className='w-full flex flex-col gap-2'>
            <span className="font-medium text-xs block">{label}</span>
            <div className='relative'>
                <Input
                    readOnly={isReadOnly}
                    ref={inputRef}
                    id={id}
                    type='text'
                    placeholder={placeholder}
                    value={value}
                    onChange={e => onChange(e.target.value)}
                    className={cn("pr-8 h-12  bg-gray-100 border-none rounded-xl", className)}
                />
                {value && (
                    <Button
                        variant='ghost'
                        size='icon'
                        onClick={handleClearInput}
                        className='text-muted-foreground focus-visible:ring-ring/50 absolute right-0 top-1/2 -translate-y-1/2 rounded-l-none hover:bg-transparent cursor-pointer'
                    >
                        <CircleXIcon/>
                        <span className='sr-only'>Clear input</span>
                    </Button>
                )}
            </div>
        </div>
    )
}

export default InputClear
