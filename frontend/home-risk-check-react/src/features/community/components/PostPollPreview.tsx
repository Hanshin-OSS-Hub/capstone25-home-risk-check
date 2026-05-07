import { Button } from "@/components/ui/button.tsx";
import { Vote, X } from "lucide-react";
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
        <div className="border rounded-xl px-4 py-3">
            <div className="flex items-center justify-between text-sm">
                <div className="flex items-center gap-1">
                    <Vote size={20} />
                    <span className="font-medium">투표</span>
                    <span className="text-xs">
                        &middot; {poll.multipleChoice ? "복수 선택 가능" : "1개 선택 가능"}
                    </span>
                </div>
                <Button
                    size="icon"
                    className="size-5 z-10 cursor-pointer"
                    onClick={handleRemove}
                >
                    <X className="size-3" />
                </Button>
            </div>
            <div className="flex flex-col gap-2 mt-4">
                {poll.options.map((opt) => (
                    <div
                        key={opt.id}
                        className="px-4 py-2 rounded-xl bg-gray-100 text-sm"
                    >
                        {opt.text || "항목을 입력해주세요"}
                    </div>
                ))}
            </div>
        </div>
    );
}
