import {
    Select,
    SelectContent,
    SelectGroup,
    SelectItem,
    SelectLabel,
    SelectTrigger,
    SelectValue
} from "@/components/ui/select.tsx";
import {
    FileUpload,
    FileUploadItem,
    FileUploadItemDelete,
    FileUploadItemPreview,
    FileUploadList,
    FileUploadTrigger,
} from "@/components/ui/file-upload";
import { showToast } from "@/lib/notify.ts"
import InputBasic from "@/components/InputBasic.tsx";
import {Textarea} from "@/components/ui/textarea.tsx";
import {Button} from "@/components/ui/button.tsx";
import {useEffect, useRef} from "react";
import {useNavigate} from "react-router-dom";
import {communityCreateStore} from "@/features/community/stores/communityCreateStore";
import {useDirtyBlocker} from "@/hooks/use-dirty-blocker";
import {ROUTES} from "@/constants/routes";
// import {communityApi} from '@/features/community/api' // 백엔드 연동 시 활성화
import {Image, MapPin, X, Vote} from "lucide-react";
import mapPinMarker from '@/assets/mapPinMarker.png'
import {COMMUNITY_CATEGORIES} from "@/constants/category.ts";

export default function CommunityCreatePage() {
    const {
        category, setCategory,
        title, setTitle,
        content, setContent,
        poll, setPoll,
        images, setImages,
        placeLat, setPlaceLat,
        placeLng, setPlaceLng,
        reset
    } = communityCreateStore()
    const navigate = useNavigate()
    const mapRef = useRef<HTMLDivElement>(null)
    const markerRef = useRef<any>(null)
    const mapInstance = useRef<any>(null);

    useEffect(() => {
        if (!placeLat || !placeLng) return
        const kakao = (window as any).kakao
        const pos = new kakao.maps.LatLng(placeLat, placeLng)
        const markerImage = new kakao.maps.MarkerImage(
            mapPinMarker,
            new kakao.maps.Size(80, 80),
            new kakao.maps.Point(40, 62),
        );

        if (!mapInstance.current) {
            mapInstance.current = new kakao.maps.Map(mapRef.current, { center: pos, level: 3 })
            mapInstance.current.setDraggable(false)
            mapInstance.current.setZoomable(false)
        } else {
            mapInstance.current.setCenter(pos)
        }

        if (markerRef.current) markerRef.current.setMap(null)
        markerRef.current = new kakao.maps.Marker({
            position: pos,
            map: mapInstance.current,
            image: markerImage,
            clickable: false,
        })

        return () => {
            markerRef.current?.setMap(null)
        }
    }, [placeLat, placeLng])

    const handlePreview = (file: File) => {
        const url = URL.createObjectURL(file);
        window.open(url, "_blank");
    };

    const handleSubmit = async () => {
        try {
            // await communityApi.createPost(
            //     { category, title, content, placeLat, placeLng, poll },
            //     images,
            // )
            reset()
        } catch (err) {
            showToast({
                message: (err as any)?.response?.data?.message ?? '요청에 실패했습니다.',
                variant: 'error',
            })
        }
    }

    const isDirty =
        category !== "" ||
        title.trim() !== "" ||
        content.trim() !== "" ||
        images.length > 0 ||
        poll !== null ||
        placeLat !== null ||
        placeLng !== null;

    useDirtyBlocker({
        isDirty,
        allowPaths: [ROUTES.placeSearch, ROUTES.communityPollNew],
        onLeave: reset,
    });

    const deletePoll = () => {
        showToast({
            message: "투표를 삭제할까요?",
            actionLabel: "삭제",
            cancelLabel: "취소",
            onConfirm: () => setPoll(null),
            onCancel: () => {},
        })
    }

    const handleNavigateToPoll = () => {
        const isNew = !poll
        if (isNew) {
            setPoll({
                options: [
                    { id: crypto.randomUUID(), text: '' },
                    { id: crypto.randomUUID(), text: '' },
                ],
                multipleChoice: false,
            })
        }
        navigate(ROUTES.communityPollNew, { state: { isNew } })
    }

    return (
        <>
            <div className="flex flex-col gap-2">
                <Select value={category} onValueChange={setCategory}>
                    <SelectTrigger className="rounded-xl w-full max-w-48 h-12!">
                        <SelectValue placeholder="카테고리를 선택해주세요"/>
                    </SelectTrigger>
                    <SelectContent>
                        <SelectGroup>
                            <SelectLabel>카테고리</SelectLabel>
                            {COMMUNITY_CATEGORIES
                                .filter((cat) => cat.key !== 'all')
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
                    onChange={setTitle}
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
                    value={images}
                    onValueChange={setImages}
                    className="-mt-2"
                    multiple
                >
                    <div className="flex gap-2.5 px-4 py-3 rounded-b-xl bg-gray-200">
                        <FileUploadTrigger className="flex items-center gap-0.5 cursor-pointer text-sm text-muted-foreground">
                            <Image size={22}/>
                            <span>사진</span>
                        </FileUploadTrigger>
                        <div className="flex items-center gap-0.5 cursor-pointer text-sm text-muted-foreground" onClick={handleNavigateToPoll}>
                            <Vote size={22}/>
                            <span>투표</span>
                        </div>
                        <div className="flex items-center gap-0.5 cursor-pointer text-sm text-muted-foreground" onClick={() =>  navigate(ROUTES.placeSearch)}>
                            <MapPin size={22}/>
                            <span>장소</span>
                        </div>
                    </div>
                    <FileUploadList className="grid grid-rows-1 auto-cols-[20%] grid-flow-col overflow-x-scroll no-scrollbar pt-2.5">
                        {images.map((image, index) => (
                            <FileUploadItem
                                key={index}
                                value={image}
                                className="relative aspect-square p-0 border-none"
                            >
                                <FileUploadItemPreview className="size-full rounded-xl cursor-pointer" onClick={() => handlePreview(image)}/>
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
                {placeLat && placeLng && (
                    <>
                        <div ref={mapRef} className="w-full h-50 mt-2.5 rounded-xl relative">
                            <Button
                                size="icon"
                                className="absolute top-2 right-4 size-6 z-10 cursor-pointer "
                                onClick={() => {
                                    setPlaceLat(null);
                                    setPlaceLng(null);
                                }}
                            >
                                <X className="size-3" />
                            </Button>
                        </div>
                        {/*<div className="flex flex-col bg-gray-200 px-4 py-3 -mt-2 rounded-b-xl">*/}
                        {/*    <span className="text-sm">{buildingName}</span>*/}
                        {/*    <span className="text-xs">{address}</span>*/}
                        {/*</div>*/}
                    </>
                )}
                {poll && (
                    <div className="border rounded-xl px-4 py-3 mt-2">
                        <div className="flex items-center justify-between text-sm">
                            <div className="flex items-center gap-1">
                                <Vote size={20}/>
                                <span className="font-medium">투표</span>
                                <span className="text-xs">&middot; {poll.multipleChoice ? "복수 선택 가능" : "1개 선택 가능"}</span>
                            </div>
                            <Button
                                size="icon"
                                className="size-6 z-10 cursor-pointer "
                                onClick={deletePoll}
                            >
                                <X className="size-3" />
                            </Button>
                        </div>
                        <div className="flex flex-col gap-2 mt-4">
                            {poll.options.map((opt) => (
                                <div
                                    key={opt.id}
                                    className="px-4 py-2 rounded-xl bg-gray-100 text-sm"
                                >
                                    {opt.text || "항목을 입력해주세요"}
                                </div>
                            ))}
                        </div>
                    </div>
                )}
                <div className="flex justify-end">
                    <Button className="rounded-xl cursor-pointer" onClick={handleSubmit}>글쓰기</Button>
                </div>
            </div>
        </>
    )
}