import {
    Select,
    SelectContent,
    SelectGroup,
    SelectItem,
    SelectLabel,
    SelectTrigger,
    SelectValue
} from "@/components/ui/select.tsx";
import { useState } from "react";
import InputBasic from "@/components/InputBasic.tsx";
import {Textarea} from "@/components/ui/textarea.tsx";
import {Image} from "lucide-react";
import React from "react";
import {
    FileUpload,
    FileUploadItem,
    FileUploadItemDelete,
    FileUploadItemPreview,
    FileUploadList,
    FileUploadTrigger,
} from "@/components/ui/file-upload";
import {Button} from "@/components/ui/button.tsx";
import {X} from "lucide-react";

export default function CommunityDetailPage() {
    const [title, setTitle] = useState('')
    const [content, setContent] = useState('')
    const [files, setFiles] = React.useState<File[]>([]);

    const handlePreview = (file: File) => {
        const url = URL.createObjectURL(file);
        window.open(url, "_blank");
    };

    return (
        <>
            <div className="flex flex-col gap-2">
                <Select>
                    <SelectTrigger className="rounded-xl w-full max-w-48 h-12!">
                        <SelectValue placeholder="카테고리를 선택해주세요"/>
                    </SelectTrigger>
                    <SelectContent>
                        <SelectGroup>
                            <SelectLabel>카테고리</SelectLabel>
                            <SelectItem value='latest'>최신순</SelectItem>
                            <SelectItem value='popular'>인기순</SelectItem>
                        </SelectGroup>
                    </SelectContent>
                </Select>

                <InputBasic
                    placeholder="제목을 입력해주세요"
                    value={title}
                    onChange={setTitle}
                    onKeyDown={(e) => {
                        if (e.key === 'Enter') e.altKey
                    }}
                    isClearable={false}
                />

                <Textarea
                    value={content}
                    onChange={(e) => setContent(e.target.value)}
                    placeholder="내용을 작성해주세요"
                    className="bg-gray-100 rounded-xl min-h-65 max-h-65 no-scrollbar border-none rounded-b-none"
                />

                <FileUpload
                    accept="image/*"
                    maxFiles={10}
                    maxSize={5 * 1024 * 1024}
                    value={files}
                    onValueChange={setFiles}
                    className="-mt-2"
                    multiple
                >
                    <div className="flex items-start p-3 rounded-b-xl bg-gray-200">
                        <FileUploadTrigger className="flex items-center gap-0.5 cursor-pointer text-sm text-muted-foreground">
                            <Image size={22}/>
                            <span>사진</span>
                        </FileUploadTrigger>
                    </div>
                    <FileUploadList className="grid grid-rows-1 auto-cols-[20%] grid-flow-col overflow-x-scroll no-scrollbar mt-2 pt-2.5">
                        {files.map((file, index) => (
                            <FileUploadItem
                                key={index}
                                value={file}
                                className="relative aspect-square p-0 border-none"
                            >
                                <FileUploadItemPreview className="size-full rounded-xl cursor-pointer" onClick={() => handlePreview(file)}/>
                                <FileUploadItemDelete asChild>
                                    <Button
                                        variant="secondary"
                                        size="icon"
                                        className="absolute -top-2.5 -right-2.5 size-6 cursor-pointer"
                                    >
                                        <X className="size-3" />
                                    </Button>
                                </FileUploadItemDelete>
                            </FileUploadItem>
                        ))}
                    </FileUploadList>
                </FileUpload>
                <div className="flex justify-end mt-2">
                    <Button className="rounded-xl">글쓰기</Button>
                </div>
            </div>
        </>
    )
}