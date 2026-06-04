import { Button } from "@/components/ui/button.tsx";
import { X } from "lucide-react";
import { showToast } from "@/lib/notify.ts";
import type { Poll } from "@/features/community/types";

interface PostPollPreviewProps {
    poll: Poll;
    onRemove: () => void;
}

export default function PostPollPreview({ poll, onRemove }: PostPollPreviewProps) {
    const handleRemove = () => {
        showToast({
            message: "투표를 삭제할까요?",
            actionLabel: "삭제",
            cancelLabel: "취소",
            onConfirm: onRemove,
            onCancel: () => {},
        });
    };

    return (
        <div className="rounded-lg p-3 bg-muted">
            <div className="flex items-center justify-between">
                <div className="flex items-center gap-1">
                    {/*<Vote className="size-5" />*/}
                    <span className="text-base font-medium">투표</span>
                </div>
                <Button
                    size="icon"
                    variant="ghost"
                    className="size-7 cursor-pointer text-muted-foreground hover:text-foreground"
                    onClick={handleRemove}
                >
                    <X className="size-4" />
                </Button>
            </div>
            <div className="flex items-center gap-1">
                <span className="font-medium text-xs text-muted-foreground">{poll.multipleChoice ? "복수 선택 가능" : "1개 선택 가능"}</span>
                <span>&middot;</span>
                <span className="font-medium text-xs text-muted-foreground">{poll.options.length}개 항목</span>
            </div>
        </div>
    );
}
