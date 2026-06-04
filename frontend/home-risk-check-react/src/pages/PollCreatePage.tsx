import InputBasic from "@/components/form/InputBasic.tsx";
import {Button} from "@/components/ui/button.tsx";
import {Switch} from "@/components/ui/switch.tsx";
import {useCommunityCreateStore} from "@/features/community/stores/useCommunityCreateStore";
import {usePollDraft} from "@/features/community/hooks/usePollDraft";
import {useNavigate} from "react-router-dom";

export default function PollCreatePage() {
    const poll = useCommunityCreateStore(s => s.poll)
    const { draft: localPoll, setDraft: setLocalPoll, commit } = usePollDraft({ initial: poll })
    const navigate = useNavigate()

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
            <div className="font-medium">
                <h2>투표 만들기</h2>
            </div>
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
                        variant="secondary"
                        className="rounded-lg cursor-pointer"
                        onClick={addOption}
                        disabled={localPoll.options.length >= 5}
                    >
                        + 항목 추가
                    </Button>

                    <div className="text-center text-xs text-muted-foreground">
                        <span>투표는 생성 후 수정&middot;삭제할 수 없어요</span>
                    </div>

                    <div className="flex items-center justify-between mt-2">
                        <div className="flex items-center gap-2 cursor-pointer">
                            <Switch checked={localPoll?.multipleChoice ?? false}
                                    onCheckedChange={(checked) =>
                                        setLocalPoll(prev => prev && { ...prev, multipleChoice: checked })
                                    }
                                    id="multiple"
                                    className='data-[size=default]:h-6.5 data-[size=default]:w-12 [&_span]:group-data-[size=default]/switch:size-5 data-[state=checked]:[&_span]:translate-x-6 data-[state=unchecked]:[&_span]:translate-x-0.5 cursor-pointer'/>
                            <label className="font-medium text-sm cursor-pointer" htmlFor="multiple">복수 선택 허용</label>
                        </div>
                        <Button
                            className="rounded-lg cursor-pointer"
                            size="sm"
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
