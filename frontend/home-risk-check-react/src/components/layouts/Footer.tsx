import {ROUTES} from "@/constants/routes.ts";
import {Link} from "react-router-dom";
import {Separator} from "@/components/ui/separator.tsx";
import {ExternalLink} from "lucide-react";

const Navbar = () => {
    return (
        <footer className="border-t py-8 mt-10">
            <div className="max-w-(--breakpoint-sm) mx-auto px-4 flex flex-col items-center gap-4">
                <div className="flex flex-wrap justify-center items-center gap-3 text-sm text-muted-foreground">
                    <Link to={ROUTES.analysis}>AI 분석</Link>
                    <Separator orientation="vertical" />
                    <Link to={ROUTES.community}>커뮤니티</Link>
                    <Separator orientation="vertical" />
                    <Link to="#">부동산 정보</Link>
                    <Separator orientation="vertical" />
                    <a href="https://github.com/Hanshin-OSS-Hub/capstone25-home-risk-check" rel="noopener noreferrer" target="_blank" className="flex items-center gap-1">
                        GitHub
                        <ExternalLink size={16}/>
                    </a>
                </div>

                <p className="text-xs text-muted-foreground">
                    © Hanshin University Capstone. Team Home-Risk-Check
                </p>
            </div>
        </footer>
    );
};

export default Navbar;
