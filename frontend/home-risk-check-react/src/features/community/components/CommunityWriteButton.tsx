import {Link} from 'react-router-dom'
import {Button} from "@/components/ui/button.tsx";
import {cn} from "@/lib/utils.ts";
import {ROUTES} from '@/constants/routes'
import {PlusIcon} from "lucide-react";

interface CommunityWriteButtonProps {
    isTop: boolean
}

export function CommunityWriteButton({ isTop }: CommunityWriteButtonProps) {
    return (
        <div className="sticky bottom-6 flex justify-end -mt-6">
            <Link to={ROUTES.communityNew}>
                <Button
                    className={cn(
                        "rounded-full h-12 cursor-pointer transition-all duration-300 overflow-hidden",
                        isTop ? "w-22 gap-1" : "w-12"
                    )}
                    size="icon-lg"
                >
                    <PlusIcon/>
                    <span className={cn(
                        "transition-all duration-300 ease-out overflow-hidden whitespace-nowrap",
                        isTop ? "max-w-fit opacity-100" : "max-w-0 opacity-0",
                    )}>
                        글쓰기
                    </span>
                </Button>
            </Link>
        </div>
    )
}
