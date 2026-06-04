import {
    Select,
    SelectContent,
    SelectGroup,
    SelectItem,
    SelectLabel,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select.tsx";
import { Button } from "@/components/ui/button.tsx";
import { Textarea } from "@/components/ui/textarea.tsx";
import { Image, MapPin, Vote } from "lucide-react";
import { COMMUNITY_CATEGORIES } from "@/constants/category.ts";

interface PostCreateFormProps {
    category: string;
    onCategoryChange: (value: string) => void;
    title: string;
    onTitleChange: (value: string) => void;
    content: string;
    onContentChange: (value: string) => void;
    onPhotoClick: () => void;
    onPollClick: () => void;
    onPlaceClick: () => void;
}

export default function PostCreateForm({
    category,
    onCategoryChange,
    title,
    onTitleChange,
    content,
    onContentChange,
    onPhotoClick,
    onPollClick,
    onPlaceClick,
}: PostCreateFormProps) {
    return (
        <div className="flex flex-col gap-2">
            <Select value={category} onValueChange={onCategoryChange}>
                <SelectTrigger className="w-full h-11! rounded-none border-0 border-b border-border bg-transparent px-0 shadow-none">
                    <SelectValue placeholder="카테고리를 선택해주세요" />
                </SelectTrigger>
                <SelectContent>
                    <SelectGroup>
                        <SelectLabel>카테고리</SelectLabel>
                        {COMMUNITY_CATEGORIES
                            .filter((cat) => cat.key !== "all")
                            .map((cat) => (
                                <SelectItem key={cat.key} value={cat.key}>
                                    {cat.label}
                                </SelectItem>
                            ))}
                    </SelectGroup>
                </SelectContent>
            </Select>

            {/* 제목 — 배경 없는 입력 (구분선만) */}
            <input
                value={title}
                onChange={(e) => onTitleChange(e.target.value)}
                placeholder="제목을 입력해주세요"
                className="w-full border-b border-border bg-transparent py-3 text-lg font-medium outline-none placeholder:text-muted-foreground placeholder:font-normal"
            />

            {/* 내용 — 배경 없는 textarea */}
            <Textarea
                value={content}
                onChange={(e) => onContentChange(e.target.value)}
                placeholder="내용을 작성해주세요"
                className="min-h-60 max-h-80 resize-none border-0 border-b border-border rounded-none bg-transparent px-0 text-base shadow-none focus-visible:ring-0 no-scrollbar dark:bg-transparent"
            />

            {/* 첨부 도구 — 구분선 + 아웃라인 칩으로 재배치 */}
            <div className="flex items-center border-border border-b pb-2">
                <Button type="button" variant="ghost" size="sm" className="flex gap-1 px-1.5 rounded-lg cursor-pointer" onClick={onPhotoClick}>
                    <Image className="size-4" /> 사진
                </Button>
                <Button type="button" variant="ghost" size="sm" className="flex gap-1 px-1.5 rounded-lg cursor-pointer" onClick={onPlaceClick}>
                    <MapPin className="size-4" /> 장소
                </Button>
                <Button type="button" variant="ghost" size="sm" className="flex gap-1 px-1.5 rounded-lg cursor-pointer" onClick={onPollClick}>
                    <Vote className="size-4" /> 투표
                </Button>
            </div>
        </div>
    );
}
