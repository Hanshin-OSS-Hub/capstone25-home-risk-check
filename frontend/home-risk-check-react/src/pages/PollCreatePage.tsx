import InputBasic from "@/components/InputBasic.tsx";
import {Button} from "@/components/ui/button.tsx";
import {Switch} from "@/components/ui/switch.tsx";
import {communityCreateStore} from "@/features/community/stores/communityCreateStore";
import {usePollDraft} from "@/features/community/hooks/usePollDraft";
import {useNavigate, useLocation} from "react-router-dom";

export default function PollCreatePage() {
    const poll = communityCreateStore(s => s.poll)
    const navigate = useNavigate()
    const { state } = useLocation()
    const isNew = !!(state as { isNew?: boolean } | null)?.isNew

    const { draft: localPoll, setDraft: setLocalPoll, commit } = usePollDraft({ initial: poll, isNew })

    const updateOption = (id: string, text: string) => {
        if (!localPoll) return
        setLocalPoll({
            ...localPoll,
            options: localPoll.options.map(o =>
                o.id === id ? { ...o, text } : o
            ),
        })
    }

    const addOption = () => {
        if (!localPoll || localPoll.options.length >= 5) return
        setLocalPoll({
            ...localPoll,
            options: [...localPoll.options, { id: crypto.randomUUID(), text: '' }],
        })
    }

    const removeOption = (id: string) => {
        if (!localPoll || localPoll.options.length <= 2) return
        setLocalPoll({
            ...localPoll,
            options: localPoll.options.filter(o => o.id !== id),
        })
    }

    const handleDone = () => {
        commit()
        navigate(-1)
    }

    return (
        <>
            {localPoll && (
                <div className="flex flex-col gap-2">
                    {localPoll.options.map((opt, index) => (
                        <div key={opt.id} className="flex gap-2 items-center">
                            <InputBasic
                                placeholder="항목 입력"
                                value={opt.text}
                                onChange={(value) => updateOption(opt.id, value)}
                                addonButton={
                                    index >= 2
                                        ? {
                                            label: "삭제",
                                            onClick: () => removeOption(opt.id),
                                        }
                                        : undefined
                                }
                            />
                        </div>
                    ))}

                    <Button
                        className="rounded-xl cursor-pointer"
                        onClick={addOption}
                        disabled={localPoll.options.length >= 5}
                    >
                        + 항목 추가
                    </Button>

                    <div className="flex items-center justify-between mt-2">
                        <div className="flex items-center gap-2">
                            복수 선택 가능
                            <Switch
                                aria-label='Medium switch'
                                className='data-[size=default]:h-6 data-[size=default]:w-10 [&_span]:group-data-[size=default]/switch:size-5 data-[state=checked]:[&_span]:translate-x-[19px] data-[state=unchecked]:[&_span]:translate-x-[2px] cursor-pointer'
                                checked={localPoll?.multipleChoice ?? false}
                                onCheckedChange={(checked) =>
                                    setLocalPoll(prev => prev && { ...prev, multipleChoice: checked })
                                }
                            />
                        </div>
                        <Button
                            className="rounded-xl cursor-pointer"
                            onClick={handleDone}
                        >
                            완료
                        </Button>
                    </div>
                </div>
            )}
        </>
    )
}
