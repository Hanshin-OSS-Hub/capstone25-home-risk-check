"use client";

import {X, Download, Eye, Upload } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
    FileUpload,
    FileUploadDropzone,
    FileUploadItem,
    FileUploadItemDelete,
    FileUploadItemMetadata,
    FileUploadList,
} from "@/components/ui/file-upload";

interface FileUploadProps{
    label: string;
    placeholder: string;
    fileIssueUrl?: string;
    files: File[];
    onValueChange: (files: File[]) => void;
}

const InputFile = ({label, placeholder, fileIssueUrl, files, onValueChange}: FileUploadProps) => {
    const handleDownload = (file: File) => {
        const url = URL.createObjectURL(file);
        const a = document.createElement("a");
        a.href = url;
        a.download = file.name;
        a.click();
        URL.revokeObjectURL(url);
    };

    const handlePreview = (file: File) => {
        const url = URL.createObjectURL(file);
        window.open(url, "_blank");
    };

    return (
        <FileUpload
            maxFiles={5}
            maxSize={10 * 1024 * 1024}
            className="w-full"
            value={files}
            onValueChange={onValueChange}
            multiple
        >
            <div className="flex items-center justify-between">
                <span className="font-medium text-xs">{label}</span>
                {fileIssueUrl && (
                    <a
                        href={fileIssueUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-xs hover:underline"
                    >
                        발급 바로가기
                    </a>
                )}
            </div>
            <FileUploadDropzone className="bg-gray-100 py-4 rounded-xl">
                <div className="flex flex-col items-center gap-1 text-center text-muted-foreground">
                    <Upload/>
                    <p className="text-sm font-medium">파일 업로드</p>
                    <p className="text-xs">
                        {placeholder}
                    </p>
                </div>
            </FileUploadDropzone>
            <FileUploadList>
                {files.map((file, index) => (
                    <FileUploadItem key={index} value={file} className="bg-gray-100 border-none rounded-xl">
                        <FileUploadItemMetadata />
                        <div className="flex items-center gap-1">
                            <Button variant="ghost" size="icon" className="size-7 cursor-pointer hover:bg-gray-200" onClick={() => handlePreview(file)}>
                                <Eye className="size-4" />
                            </Button>
                            <Button variant="ghost" size="icon" className="size-7 cursor-pointer hover:bg-gray-200" onClick={() => handleDownload(file)}>
                                <Download className="size-4" />
                            </Button>
                            <FileUploadItemDelete asChild>
                                <Button variant="ghost" size="icon" className="size-7 cursor-pointer hover:bg-gray-200">
                                    <X className="size-4" />
                                </Button>
                            </FileUploadItemDelete>
                        </div>
                    </FileUploadItem>
                ))}
            </FileUploadList>
        </FileUpload>
    );
};

export default InputFile;
