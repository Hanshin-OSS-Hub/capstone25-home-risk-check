import {
    InputGroup,
    InputGroupAddon,
    InputGroupButton,
    InputGroupTextarea,
} from "@/components/ui/input-group";
import {UserAvatar} from "@/features/community/components/UserAvatar";
import type {Ref} from 'react'

interface CommentComposerProps {
    textareaRef?: Ref<HTMLTextAreaElement>
}

export function CommentComposer({ textareaRef }: CommentComposerProps) {
    return (
        <>
            <div className="flex items-start gap-2">
                <UserAvatar size="default"/>
                <InputGroup className="rounded-lg! bg-muted border-transparent">
                    <InputGroupTextarea
                        ref={textareaRef}
                        className="min-h-10 p-3"
                        placeholder="댓글을 작성해주세요"
                    />
                    <InputGroupAddon align="block-end" className="pt-0">
                        <InputGroupButton className="ml-auto rounded-lg cursor-pointer" size="sm" variant="default">
                            댓글 작성
                        </InputGroupButton>
                    </InputGroupAddon>
                </InputGroup>
            </div></>
    )
}
