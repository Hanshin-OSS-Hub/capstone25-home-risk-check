import {
    InputGroup,
    InputGroupAddon,
    InputGroupButton,
    InputGroupTextarea,
} from "@/components/ui/input-group";
import {Avatar, AvatarImage} from "@/components/ui/avatar.tsx";
import type {Ref} from 'react'

interface CommentComposerProps {
    textareaRef?: Ref<HTMLTextAreaElement>
}

export function CommentComposer({ textareaRef }: CommentComposerProps) {
    return (
        <>
            <div className="flex items-start gap-2">
                <Avatar size="default">
                    <AvatarImage src="https://github.com/shadcn.png"/>
                </Avatar>
                <InputGroup className="!rounded-xl bg-gray-100 border-none">
                    <InputGroupTextarea
                        ref={textareaRef}
                        className="min-h-10 p-3"
                        placeholder="댓글을 작성해주세요"
                    />
                    <InputGroupAddon align="block-end" className="pt-0">
                        <InputGroupButton className="ml-auto rounded-xl cursor-pointer" size="sm" variant="default">
                            댓글 작성
                        </InputGroupButton>
                    </InputGroupAddon>
                </InputGroup>
            </div></>
    )
}
