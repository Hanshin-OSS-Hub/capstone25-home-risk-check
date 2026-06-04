import { useRef, useState } from "react";
import { Camera } from "lucide-react";
import { UserAvatar } from "@/features/community/components/UserAvatar";
import type { UserProfile } from "@/features/user/types";

interface ProfileSummaryProps {
    profile: UserProfile;
    /** 이미지 선택 완료 시 자동 호출 (PUT /api/users/me/profile-image 연결 예정) */
    onUploadImage?: (file: File) => void;
}

export function ProfileSummary({ profile, onUploadImage }: ProfileSummaryProps) {
    const fileRef = useRef<HTMLInputElement>(null);
    const [preview, setPreview] = useState<string | null>(null);
    const createdAt = "2025-12-01T00:00:00";
    const [year, month, day] = createdAt.split("T")[0].split("-");

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file) return;
        setPreview(URL.createObjectURL(file)); // 즉시 미리보기
        onUploadImage?.(file); // 선택 즉시 업로드 요청
        e.target.value = ""; // 동일 파일 재선택 허용
    };

    return (
        <div className="flex items-center gap-3">
            <button
                type="button"
                onClick={() => fileRef.current?.click()}
                className="relative shrink-0 cursor-pointer rounded-full"
                aria-label="프로필 이미지 변경"
            >
                <UserAvatar src={preview ?? profile.profileImageUrl ?? undefined} className="size-16" />
                <span className="absolute -bottom-0.5 -right-0.5 flex size-5 items-center justify-center rounded-full bg-foreground text-background ring-2 ring-background">
                    <Camera className="size-3" />
                </span>
            </button>
            <input
                ref={fileRef}
                type="file"
                accept="image/*"
                className="hidden"
                onChange={handleFileChange}
            />
            <div className="flex flex-col gap-2">
                <span className="text-base font-medium leading-none">{profile.nickname}</span>
                <span className="text-sm leading-none text-muted-foreground">{profile.email}</span>
                <span className="text-xs leading-none text-muted-foreground">{year}년 {month}월 {day}일 가입</span>
            </div>
        </div>
    );
}
