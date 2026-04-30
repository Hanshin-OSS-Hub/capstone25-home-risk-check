import InputBasic from "@/components/InputBasic.tsx";
import {useEffect, useState} from 'react'
import {useSearchParams, Link} from 'react-router-dom'
import { api } from '@/lib/axios'
import {cn} from "@/lib/utils.ts";
import {Button} from "@/components/ui/button.tsx";
import {Badge} from "@/components/ui/badge.tsx";
import {Heart, MessageSquareText, PlusIcon} from "lucide-react";
import {
    Select,
    SelectContent,
    SelectGroup,
    SelectItem,
    SelectLabel,
    SelectTrigger,
    SelectValue
} from '@/components/ui/select'

const blogPosts = [
    {
        category: "Technology",
        title: "A beginner",
        description:
            "동해물과 백두산이 마르고 닳도록 하나님이 보우하사 우리나라 만세. 무궁화 삼천리 화려 강산 대한 사람 대한으로 길이 보전하세.",
        readTime: "5 min read",
        date: "Nov 20, 2024",
        image:
            "https://cdn.pixabay.com/photo/2021/08/27/18/50/water-6579313_1280.jpg",
    },
    {
        category: "Business",
        title: "Understanding React Server Components",
        description:
            "Lorem ipsum dolor sit, amet consectetur adipisicing elit.",
        readTime: "8 min read",
        date: "Nov 18, 2024",
        image:
            "https://cdn.pixabay.com/photo/2020/02/13/06/49/seascape-4844697_1280.jpg",
    },
    {
        category: "Finance",
        title: "10 Useful Shadcn UI Components You Should Know",
        description:
            "Lorem ipsum dolor sit, amet consectetur adipisicing elit. Ipsa contur minus dicta accusantium quos, ratione suscipit id adipisci voluptatibus. Nulla sint repudiandae fugiat tenetur dolores.",
        readTime: "6 min read",
        date: "Nov 15, 2024",
        image:
            "https://cdn.pixabay.com/photo/2021/08/13/12/51/sea-6543041_1280.jpg",
    },
    {
        category: "Health",
        title: "Building a Personal Blog with Next.js",
        description:
            "Lorem ipsum dolor sit, amet consectetur adipisicing elit. Ipsa consequatur minus dicta accusantium quos, ratione suscipit id adipisci voluptatibus. Nulla sint repudiandae fugiat tenetur dolores.",
        readTime: "10 min read",
        date: "Nov 12, 2024",
        image:
            "https://cdn.pixabay.com/photo/2017/06/22/20/24/dewdrops-2432391_1280.jpg",
    },
    {
        category: "Lifestyle",
        title: "The Complete Guide to TypeScript for Beginners",
        description:
            "Lorem ipsum dolor sit, amet consectetur adipisicing elit. Ipsa consequatur minus dicta accusantium quos, ratione suscipit id adipisci voluptatibus. Nulla sint repudiandae fugiat tenetur dolores.",
        readTime: "12 min read",
        date: "Nov 10, 2024",
        image:
            "https://cdn.pixabay.com/photo/2013/07/21/13/00/rose-165819_1280.jpg",
    },
    {
        category: "Politics",
        title: "Optimizing Web Performance with Next.js",
        description:
            "Lorem ipsum dolor sit, amet consectetur adipisicing elit. Ipsa consequatur minus dicta accusantium quos, ratione suscipit id adipisci voluptatibus. Nulla sint repudiandae fugiat tenetur dolores.",
        readTime: "7 min read",
        date: "Nov 8, 2024",
        image:
            "https://cdn.pixabay.com/photo/2021/08/12/10/38/mountains-6540497_1280.jpg",
    },
    {
        category: "Science",
        title: "Deploying Full-Stack Apps on Vercel",
        description:
            "Lorem ipsum dolor sit, amet consectetur adipisicing elit. Ipsa consequatur minus dicta accusantium quos, ratione suscipit id adipisci voluptatibus. Nulla sint repudiandae fugiat tenetur dolores.",
        readTime: "9 min read",
        date: "Nov 5, 2024",
        image:
            "https://cdn.pixabay.com/photo/2016/03/27/18/54/technology-1283624_1280.jpg",
    },
    {
        category: "Sports",
        title: "Getting Started with Modern Web Development",
        description:
            "Lorem ipsum dolor sit, amet consectetur adipisicing elit. Ipsa consequatur minus dicta accusantium quos, ratione suscipit id adipisci voluptatibus. Nulla sint repudiandae fugiat tenetur dolores.",
        readTime: "11 min read",
        date: "Nov 2, 2024",
        image:
            "https://cdn.pixabay.com/photo/2017/08/30/12/45/girl-2696947_1280.jpg",
    },
];

const categories = [
    {key: "all", label: "전체"},
    {key: "damage", label: "⚠️ 피해 사례"},
    {key: "fraud", label: "🚨 사기 의심"},
    {key: "law", label: "🧑‍⚖️ 대응/법률"},
    {key: "region", label: "🔍 지역 정보"},
    {key: "question", label: "❓ 질문"},
];

export default function CommunityMainPage() {
    const [searchParams, setSearchParams] = useSearchParams()
    const [posts, setPosts] = useState([])
    const [keyword, setKeyword] = useState('')

    const sort = searchParams.get('sort') || 'latest'
    const category = searchParams.get('category') || 'all'
    const query = searchParams.get('query') || ''

    const [isTop, setIsTop] = useState(true);

    useEffect(() => {
        const handleScroll = () => {
            if (window.scrollY > 100) {
                setIsTop(false);
            } else {
                setIsTop(true);
            }
        };

        window.addEventListener("scroll", handleScroll);

        return () => {
            window.removeEventListener("scroll", handleScroll);
        };
    }, []);

    useEffect(() => {
        (async () => {
            try {
                const res = await api.get('/api/posts', {
                    params: {sort, category, query}
                })
                setPosts(res.data)
            } catch (e) {
                console.error(e)
            }
        })()
    }, [sort, category, query])

    const handleSortChange = (newSort: string) => {
        setSearchParams({
            sort: newSort,
            category,
            ...(query && {query})
        })
    }

    const handleCategoryChange = (newCategory: string) => {
        setSearchParams({
            sort,
            category: newCategory,
            ...(query && {query})
        })
    }

    const handleSearch = () => {
        setSearchParams({
            sort,
            category,
            ...(keyword.trim() && {query: keyword.trim()})
        })
    }

    return (
        <>
            <InputBasic
                placeholder="검색어를 입력해주세요"
                value={keyword}
                onChange={setKeyword}
                onKeyDown={(e) => {
                    if (e.key === 'Enter') handleSearch()
                }}
                isClearable={true}
            />

            <div className="flex items-center gap-1">
                <Select value={sort} onValueChange={(value) => handleSortChange(value)}>
                    <SelectTrigger className="rounded-xl">
                        <SelectValue placeholder="정렬">
                            {sort === 'latest' ? '최신순' : '인기순'}
                        </SelectValue>
                    </SelectTrigger>
                    <SelectContent>
                        <SelectGroup>
                            <SelectLabel>정렬</SelectLabel>
                            <SelectItem value='latest'>최신순</SelectItem>
                            <SelectItem value='popular'>인기순</SelectItem>
                        </SelectGroup>
                    </SelectContent>
                </Select>
                <div className="flex gap-1 overflow-x-scroll whitespace-nowrap no-scrollbar">
                    {categories.map((cat) => (
                        <Button
                            key={cat.key}
                            className={cn(
                                "rounded-xl text-sm cursor-pointer font-normal bg-gray-100 text-gray-800",
                                category === cat.key && "bg-black text-white hover:bg-black"
                            )}
                            onClick={() => handleCategoryChange(cat.key)}
                            variant="secondary"
                        >
                            {cat.label}
                        </Button>
                    ))}
                </div>
            </div>

            <div className="flex flex-col gap-4 divide-y">
                {blogPosts.map((post) => (
                    <Link to={`/community/${post.title}`}>
                        <div className="flex gap-4 pb-4" key={post.title}>
                            <div className="flex flex-1 flex-col gap-2">
                                <Badge variant="secondary" className="rounded-sm text-gray-700">
                                    {post.category}
                                </Badge>
                                <h3 className="line-clamp-1 text-ellipsis font-medium text-sm sm:text-base">
                                    {post.title}
                                </h3>
                                <p className="line-clamp-1 text-ellipsis text-sm text-muted-foreground">
                                    {post.description}
                                </p>
                                <div className="flex items-center gap-0.5 text-muted-foreground text-xs">
                                    바람이 분당구
                                    <span>&middot;</span>
                                    1일전
                                </div>
                                <div className="flex items-center gap-4 font-medium text-muted-foreground text-sm">
                                    <div className="flex items-center gap-1">
                                        <Heart className="h-4 w-4"/> 12
                                    </div>
                                    <div className="flex items-center gap-1">
                                        <MessageSquareText className="h-4 w-4"/> 53
                                    </div>
                                </div>
                            </div>
                            <div className="shrink-0 w-22 aspect-square self-start rounded-lg bg-gray-200">
                                {/* 게시글 이미지 영역*/}
                            </div>
                        </div>
                    </Link>
                ))}
            </div>
            <div className="sticky bottom-6 flex justify-end -mt-6">
                <Link to="/community/new">
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
        </>
    )
}