import {
    Select,
    SelectContent,
    SelectGroup,
    SelectItem,
    SelectLabel,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select.tsx";
import InputBasic from "@/components/InputBasic.tsx";
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
                <SelectTrigger className="rounded-xl w-full max-w-48 h-12!">
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

            <InputBasic
                placeholder="제목을 입력해주세요"
                value={title}
                onChange={onTitleChange}
                isClearable={false}
            />

            <Textarea
                value={content}
                onChange={(e) => onContentChange(e.target.value)}
                placeholder="내용을 작성해주세요"
                className="bg-gray-100 rounded-xl min-h-65 max-h-65 no-scrollbar border-none rounded-b-none"
            />

            <div className="flex gap-2.5 px-4 py-3 rounded-b-xl bg-gray-200 -mt-4">
                <button
                    type="button"
                    className="flex items-center gap-0.5 cursor-pointer text-sm text-muted-foreground"
                    onClick={onPhotoClick}
                >
                    <Image size={22} />
                    <span>사진</span>
                </button>
                <button
                    type="button"
                    className="flex items-center gap-0.5 cursor-pointer text-sm text-muted-foreground"
                    onClick={onPlaceClick}
                >
                    <MapPin size={22} />
                    <span>장소</span>
                </button>
                <button
                    type="button"
                    className="flex items-center gap-0.5 cursor-pointer text-sm text-muted-foreground"
                    onClick={onPollClick}
                >
                    <Vote size={22} />
                    <span>투표</span>
                </button>
            </div>
        </div>
    );
}
