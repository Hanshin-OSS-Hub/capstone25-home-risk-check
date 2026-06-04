import { useState } from "react";
import {User, FileSearch, FileText, EllipsisVertical, Trash2, type LucideIcon} from "lucide-react";
import { cn } from "@/lib/utils.ts";
import { showToast } from "@/lib/notify.ts";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover.tsx";
import { ProfileSummary } from "@/features/user/components/ProfileSummary";
import { AccountMenu } from "@/features/user/components/AccountMenu";
import { RISK_LEVEL_META, isRiskLevel } from "@/features/analysis/constants";
import { PostList } from "@/features/community/components/PostList";
import { mockBlogPosts } from "@/__mocks__/mockBlogPosts";
import type { Post } from "@/features/community/types";
import type { UserProfile, MyAnalysisHistoryItem } from "@/features/user/types";
import {Button} from "@/components/ui/button.tsx";

// 백엔드 연동 전 표시용 placeholder
// - 프로필: GET /api/users/me
// - 분석 이력: GET /api/analysis/history
const MOCK_PROFILE: UserProfile = {
    id: 1,
    email: "user@example.com",
    nickname: "바람이분당구",
    profileImageUrl: null,
    role: "USER",
    createdAt: "2025-12-01T00:00:00",
    updatedAt: "2026-06-01T00:00:00",
};

const MOCK_HISTORY: MyAnalysisHistoryItem[] = [
    { id: 1, address: "인천광역시 부평구 삼산동 167-15", riskLevel: "DANGER", riskScore: 8.4, createdAt: "2026-05-30T10:00:00" },
    { id: 2, address: "서울특별시 강남구 역삼동 123-4", riskLevel: "LOW", riskScore: 2.1, createdAt: "2026-05-12T10:00:00" },
];

// 내 게시글: GET /api/posts/me
const MOCK_POSTS: Post[] = mockBlogPosts;

type TabKey = "profile" | "analysis" | "posts";

const TABS: { key: TabKey; label: string; icon: LucideIcon }[] = [
    { key: "profile", label: "프로필", icon: User },
    { key: "analysis", label: "분석 기록", icon: FileSearch },
    { key: "posts", label: "내 게시글", icon: FileText },
];

export default function MyPage() {
    const [tab, setTab] = useState<TabKey>("profile");
    const [history, setHistory] = useState<MyAnalysisHistoryItem[]>(MOCK_HISTORY);

    // 백엔드/인증 연동 시 실제 동작으로 대체
    const notReady = () => showToast({ message: "곧 제공될 기능이에요.", variant: "info" });
    const handleUploadImage = () =>
        showToast({ message: "프로필 이미지 업로드 요청 (엔드포인트 연결 예정)", variant: "info" });
    const handleDeleteHistory = (id: number) => {
        setHistory(prev => prev.filter(h => h.id !== id));
        showToast({ message: "분석 기록 삭제 (엔드포인트 연결 예정)", variant: "info" });
    };

    return (
        <div className="flex gap-6">
            {/* 사이드 탭 */}
            <nav className="flex w-fit shrink-0 flex-col gap-1">
                {TABS.map(({ key, label, icon: Icon }) => (
                    <button
                        key={key}
                        type="button"
                        onClick={() => setTab(key)}
                        className={cn(
                            "flex cursor-pointer items-center gap-2 rounded-lg px-3 py-2.5 text-left text-sm font-medium transition-colors",
                            tab === key
                                ? "bg-muted font-semibold text-foreground"
                                : "text-muted-foreground hover:bg-muted/50 hover:text-foreground",
                        )}
                    >
                        <Icon className="size-4 shrink-0" />
                        {label}
                    </button>
                ))}
            </nav>

            {/* 콘텐츠 */}
            <div className="min-w-0 flex-1">
                {tab === "profile" && (
                    <div className="flex flex-col gap-6">
                        <ProfileSummary profile={MOCK_PROFILE} onUploadImage={handleUploadImage} />
                        <AccountMenu nickname={MOCK_PROFILE.nickname} onWithdraw={notReady} />
                    </div>
                )}

                {tab === "analysis" && (
                    <section className="flex flex-col gap-3">
                        {history.length === 0 ? (
                            <p className="rounded-lg border border-border px-4 py-10 text-center text-sm text-muted-foreground">
                                아직 분석 기록이 없어요.
                            </p>
                        ) : (
                            <table className="w-full table-fixed text-sm">
                                <colgroup>
                                    <col />
                                    <col className="w-28" />
                                    <col className="w-28" />
                                    <col className="w-10" />
                                </colgroup>
                                <thead>
                                    <tr className="border-b border-border text-xs text-muted-foreground">
                                        <th className="py-2.5 text-left font-medium">분석 주소</th>
                                        <th className="py-2.5 text-left font-medium">결과</th>
                                        <th className="py-2.5 text-left font-medium">분석 일자</th>
                                        <th className="py-2.5" />
                                    </tr>
                                </thead>
                                <tbody>
                                    {history.map(item => {
                                        const meta = isRiskLevel(item.riskLevel) ? RISK_LEVEL_META[item.riskLevel] : RISK_LEVEL_META.MEDIUM;
                                        const date = new Date(item.createdAt).toLocaleDateString("ko-KR");
                                        return (
                                            <tr key={item.id} className="border-b border-border last:border-0">
                                                <td className="py-3 pr-3">
                                                    <span className="block truncate font-medium">{item.address}</span>
                                                </td>
                                                <td className="py-3">
                                                    <span className={cn("inline-block rounded-md px-2 py-0.5 text-xs font-semibold", meta.chip)}>
                                                        {item.riskScore.toFixed(1)}
                                                    </span>
                                                </td>
                                                <td className="py-3 whitespace-nowrap text-muted-foreground">{date}</td>
                                                <td className="py-3 text-right">
                                                    <Popover>
                                                        <PopoverTrigger asChild>
                                                            <Button
                                                                variant="ghost"
                                                                size="icon"
                                                                className="size-8 cursor-pointer text-muted-foreground hover:text-foreground"
                                                                aria-label="더보기"
                                                            >
                                                                <EllipsisVertical />
                                                            </Button>
                                                        </PopoverTrigger>
                                                        <PopoverContent align="end" side="bottom" className="w-fit p-1 text-xs gap-0 rounded-lg font-medium">
                                                            <div
                                                                onClick={() => handleDeleteHistory(item.id)}
                                                                className="flex items-center gap-1 cursor-pointer hover:bg-muted p-2 rounded-lg"
                                                            >
                                                                <Trash2 size={14} />
                                                                <span>삭제</span>
                                                            </div>
                                                        </PopoverContent>
                                                    </Popover>
                                                </td>
                                            </tr>
                                        );
                                    })}
                                </tbody>
                            </table>
                        )}
                    </section>
                )}

                {tab === "posts" && (
                    <section className="flex flex-col gap-3">
                        {MOCK_POSTS.length === 0 ? (
                            <p className="rounded-lg border border-border px-4 py-10 text-center text-sm text-muted-foreground">
                                아직 작성한 게시글이 없어요.
                            </p>
                        ) : (
                            <PostList posts={MOCK_POSTS} />
                        )}
                    </section>
                )}
            </div>
        </div>
    );
}
