import { Button } from "@/components/ui/button.tsx";
import { X } from "lucide-react";
import { showToast } from "@/lib/notify.ts";

interface PostPlacePreviewProps {
    name?: string | null;
    address?: string | null;
    onRemove: () => void;
}

export default function PostPlacePreview({ name, address, onRemove }: PostPlacePreviewProps) {
    const handleRemove = () => {
        showToast({
            message: "장소를 삭제할까요?",
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
                    <span className="text-base font-medium">장소</span>
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
                <span className="font-medium text-xs text-muted-foreground">{name}</span>
                <span>&middot;</span>
                <span className="font-medium text-xs text-muted-foreground">{address}</span>
            </div>
        </div>
    );
}
