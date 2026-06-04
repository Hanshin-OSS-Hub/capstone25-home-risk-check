import { useState } from "react";
import { Check } from "lucide-react";
import { Button } from "@/components/ui/button.tsx";
import InputBasic from "@/components/form/InputBasic.tsx";
import { cn } from "@/lib/utils.ts";
import { showToast } from "@/lib/notify.ts";

type RowKey = "nickname" | "password";

interface AccountMenuProps {
    nickname: string;
    onWithdraw?: () => void;
}

export function AccountMenu({ nickname, onWithdraw }: AccountMenuProps) {
    const [openKey, setOpenKey] = useState<RowKey | null>(null);

    // 닉네임 폼
    const [nicknameInput, setNicknameInput] = useState(nickname);
    // 비밀번호 폼
    const [currentPw, setCurrentPw] = useState("");
    const [newPw, setNewPw] = useState("");
    const [confirmPw, setConfirmPw] = useState("");

    const toggle = (key: RowKey) => {
        setOpenKey(prev => (prev === key ? null : key));
        if (key === "nickname") setNicknameInput(nickname);
        if (key === "password") {
            setCurrentPw("");
            setNewPw("");
            setConfirmPw("");
        }
    };

    const close = () => setOpenKey(null);

    // 엔드포인트 연결 전 스텁
    const saveNickname = () => {
        showToast({ message: "닉네임 변경 요청 (엔드포인트 연결 예정)", variant: "info" });
        close();
    };
    const savePassword = () => {
        showToast({ message: "비밀번호 변경 요청 (엔드포인트 연결 예정)", variant: "info" });
        close();
    };

    return (
        <div className="flex flex-col gap-1">
            {/* 닉네임 */}
            <div className="flex flex-col gap-3 py-2">
                <div className="flex items-center gap-4">
                    <span className="w-24 shrink-0 text-sm font-medium">닉네임</span>
                    <span className="flex-1 truncate text-sm">{nickname}</span>
                    <Button variant="secondary" size="sm" className="shrink-0 cursor-pointer" onClick={() => toggle("nickname")}>
                        설정
                    </Button>
                </div>
                {openKey === "nickname" && (
                    <div className="flex flex-col gap-3 pl-0 sm:pl-28">
                        <InputBasic
                            placeholder="새 닉네임을 입력해주세요"
                            value={nicknameInput}
                            onChange={setNicknameInput}
                            isClearable
                        />
                        <div className="flex items-center justify-end gap-2">
                            <Button variant="ghost" size="sm" className="cursor-pointer" onClick={close}>취소</Button>
                            <Button size="sm" className="cursor-pointer" disabled={!nicknameInput.trim()} onClick={saveNickname}>저장</Button>
                        </div>
                    </div>
                )}
            </div>

            {/* 비밀번호 */}
            <div className="flex flex-col gap-3 py-2">
                <div className="flex items-center gap-4">
                    <span className="w-24 shrink-0 text-sm font-medium">비밀번호</span>
                    <span className="flex-1 truncate text-sm tracking-widest">•••••</span>
                    <Button variant="secondary" size="sm" className="shrink-0 cursor-pointer" onClick={() => toggle("password")}>
                        설정
                    </Button>
                </div>
                {openKey === "password" && (
                    <div className="flex flex-col gap-3 pl-0 sm:pl-28">
                        <div className="flex flex-col gap-1.5">
                            <InputBasic
                                type="password"
                                placeholder="현재 비밀번호"
                                value={currentPw}
                                onChange={setCurrentPw}
                                autoComplete="current-password"
                            />
                            <p className="flex items-center gap-1.5 pl-0.5 text-xs text-muted-foreground">
                                <Check className="size-3.5" /> 확인을 위해 현재 비밀번호를 다시 입력해 주세요.
                            </p>
                        </div>

                        <div className="flex flex-col gap-1.5">
                            <InputBasic
                                type="password"
                                placeholder="새 비밀번호"
                                value={newPw}
                                onChange={setNewPw}
                                autoComplete="new-password"
                            />
                            <p className="flex items-center gap-1.5 pl-0.5 text-xs text-muted-foreground">
                                <Check className="size-3.5" /> 새 비밀번호를 입력해주세요
                            </p>
                        </div>

                        <div className="flex flex-col gap-1.5">
                            <InputBasic
                                type="password"
                                placeholder="새 비밀번호 확인"
                                value={confirmPw}
                                onChange={setConfirmPw}
                                autoComplete="new-password"
                                error={confirmPw && confirmPw !== newPw ? "새 비밀번호가 일치하지 않아요." : undefined}
                            />
                            <p className="flex items-center gap-1.5 pl-0.5 text-xs text-muted-foreground">
                                <Check className="size-3.5" /> 확인을 위해 새 비밀번호를 다시 입력해 주세요.
                            </p>
                        </div>

                        <div className="flex items-center justify-between pt-1">
                            <Button
                                variant="secondary"
                                size="sm"
                                className="cursor-pointer"
                                onClick={() => showToast({ message: "비밀번호 찾기 (엔드포인트 연결 예정)", variant: "info" })}
                            >
                                비밀번호 찾기
                            </Button>
                            <div className="flex items-center gap-2">
                                <Button variant="ghost" size="sm" className="cursor-pointer" onClick={close}>취소</Button>
                                <Button
                                    size="sm"
                                    className="cursor-pointer"
                                    disabled={!currentPw || !newPw || newPw !== confirmPw}
                                    onClick={savePassword}
                                >
                                    저장
                                </Button>
                            </div>
                        </div>
                    </div>
                )}
            </div>

            {/* 회원 탈퇴 */}
            <div className="flex items-center gap-4 py-2">
                <span className="w-24 shrink-0 text-sm font-medium">회원 탈퇴</span>
                <span className="flex-1 truncate text-sm text-muted-foreground">계정을 영구적으로 삭제합니다</span>
                <Button
                    variant="destructive"
                    size="sm"
                    className={cn("shrink-0 cursor-pointer text-destructive hover:text-destructive")}
                    onClick={onWithdraw}
                >
                    탈퇴
                </Button>
            </div>
        </div>
    );
}
