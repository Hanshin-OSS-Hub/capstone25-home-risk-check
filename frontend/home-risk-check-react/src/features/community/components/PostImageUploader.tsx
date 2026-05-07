import {
    FileUpload,
    FileUploadItem,
    FileUploadItemDelete,
    FileUploadItemPreview,
    FileUploadList,
    FileUploadTrigger,
} from "@/components/ui/file-upload";
import { Button } from "@/components/ui/button.tsx";
import { X } from "lucide-react";
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
            <FileUploadList className="grid grid-rows-1 auto-cols-[20%] grid-flow-col overflow-x-scroll no-scrollbar pt-2.5">
                {images.map((image) => (
                    <FileUploadItem
                        key={`${image.name}-${image.size}-${image.lastModified}`}
                        value={image}
                        className="relative aspect-square p-0 border-none"
                    >
                        <FileUploadItemPreview
                            className="size-full rounded-xl cursor-pointer"
                            onClick={() => handlePreview(image)}
                        />
                        <FileUploadItemDelete asChild>
                            <Button
                                size="icon"
                                className="absolute -top-2 -right-2 size-5 cursor-pointer"
                            >
                                <X className="size-3" />
                            </Button>
                        </FileUploadItemDelete>
                    </FileUploadItem>
                ))}
            </FileUploadList>
        </FileUpload>
    );
}
