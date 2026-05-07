import { useRef } from "react";
import { useNavigate } from "react-router-dom";
import { useShallow } from "zustand/react/shallow";
import { Button } from "@/components/ui/button.tsx";
import { showToast } from "@/lib/notify.ts";
import { communityCreateStore } from "@/features/community/stores/communityCreateStore";
import { useDirtyBlocker } from "@/hooks/use-dirty-blocker";
import { useCreatePost } from "@/features/community/hooks/useCreatePost";
import { ROUTES } from "@/constants/routes";
import { getApiErrorMessage } from "@/lib/api-response";
import PostCreateForm from "@/features/community/components/PostCreateForm";
import PostImageUploader from "@/features/community/components/PostImageUploader";
import PostPlacePreview from "@/features/community/components/PostPlacePreview";
import PostPollPreview from "@/features/community/components/PostPollPreview";

export default function CommunityCreatePage() {
    const { category, title, content, poll, images, placeLat, placeLng } =
        communityCreateStore(
            useShallow(s => ({
                category: s.category,
                title: s.title,
                content: s.content,
                poll: s.poll,
                images: s.images,
                placeLat: s.placeLat,
                placeLng: s.placeLng,
            })),
        );
    const {
        setCategory, setTitle, setContent,
        setPoll, setImages, setPlaceLat, setPlaceLng, reset,
    } = communityCreateStore.getState();

    const navigate = useNavigate();
    const triggerRef = useRef<HTMLButtonElement | null>(null);
    const createPost = useCreatePost();

    const handleSubmit = async () => {
        try{
            await createPost.mutateAsync({ body: { category, title, content, poll, placeLat, placeLng }, images })
        } catch (error) {
            showToast({
                message: getApiErrorMessage(error, '글 작성에 실패했습니다. 다시 시도해주세요.'),
                variant: 'error',
            })
        }
    };

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

    return (
        <div className="flex flex-col gap-4">
            <PostCreateForm
                category={category}
                onCategoryChange={setCategory}
                title={title}
                onTitleChange={setTitle}
                content={content}
                onContentChange={setContent}
                onPhotoClick={() => triggerRef.current?.click()}
                onPlaceClick={() => navigate(ROUTES.placeSearch)}
                onPollClick={() => navigate(ROUTES.communityPollNew)}
            />

            <PostImageUploader
                images={images}
                onChange={setImages}
                triggerRef={triggerRef}
            />

            {placeLat !== null && placeLng !== null && (
                <PostPlacePreview
                    lat={placeLat}
                    lng={placeLng}
                    onRemove={() => {
                        setPlaceLat(null)
                        setPlaceLng(null)
                    }}
                />
            )}

            {poll &&
                <PostPollPreview
                        poll={poll}
                        onRemove={() => setPoll(null)}
                />
            }

            <div className="flex justify-end">
                <Button className="rounded-xl cursor-pointer" onClick={handleSubmit} disabled={createPost.isPending}>
                    글쓰기
                </Button>
            </div>
        </div>
    );
}
