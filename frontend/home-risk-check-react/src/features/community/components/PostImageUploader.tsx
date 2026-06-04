import {
    FileUpload,
    FileUploadItem,
    FileUploadItemDelete,
    FileUploadItemPreview,
    FileUploadList,
    FileUploadTrigger,
} from "@/components/ui/file-upload";
import { Plus, X } from "lucide-react";
import type { Ref } from "react";

interface PostImageUploaderProps {
    images: File[];
    onChange: (images: File[]) => void;
    triggerRef: Ref<HTMLButtonElement>;
    maxFiles?: number;
    maxSize?: number;
}

export default function PostImageUploader({
    images,
    onChange,
    triggerRef,
    maxFiles = 10,
    maxSize = 5 * 1024 * 1024,
}: PostImageUploaderProps) {
    const handlePreview = (file: File) => {
        const url = URL.createObjectURL(file);
        const win = window.open(url, "_blank");
        const revoke = () => URL.revokeObjectURL(url);
        if (win) win.addEventListener("beforeunload", revoke);
        else setTimeout(revoke, 60_000);
    };

    return (
        <FileUpload
            accept="image/*"
            maxFiles={maxFiles}
            maxSize={maxSize}
            value={images}
            onValueChange={onChange}
            multiple
        >
            <FileUploadTrigger ref={triggerRef} className="hidden" />
            {images.length > 0 && (
                <FileUploadList className="flex flex-row gap-2 overflow-x-auto no-scrollbar">
                    {images.map((image, idx) => (
                        <FileUploadItem
                            key={`${image.name}-${image.size}-${image.lastModified}`}
                            value={image}
                            className="relative size-26 shrink-0 border-none p-0"
                        >
                            <FileUploadItemPreview
                                className="size-full cursor-pointer rounded-lg"
                                onClick={() => handlePreview(image)}
                            />
                            <span className="absolute bottom-1.5 left-1.5 rounded bg-black/60 px-1.5 py-0.5 text-[10px] font-medium text-white">
                                {idx + 1}/{images.length}
                            </span>
                            <FileUploadItemDelete asChild>
                                <button
                                    type="button"
                                    className="absolute right-1.5 top-1.5 flex size-5 cursor-pointer items-center justify-center rounded-full bg-black/60 text-white"
                                >
                                    <X className="size-3" />
                                </button>
                            </FileUploadItemDelete>
                        </FileUploadItem>
                    ))}
                    {images.length < maxFiles && (
                        <FileUploadTrigger asChild>
                            <button
                                type="button"
                                className="flex size-26 shrink-0 cursor-pointer flex-col items-center justify-center gap-1 rounded-lg border border-dashed border-border text-muted-foreground hover:bg-muted"
                            >
                                <Plus className="size-5" />
                                <span className="text-xs">추가</span>
                            </button>
                        </FileUploadTrigger>
                    )}
                </FileUploadList>
            )}
        </FileUpload>
    );
}
