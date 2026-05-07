import { useEffect, useRef } from "react";
import { Button } from "@/components/ui/button.tsx";
import { X } from "lucide-react";
import mapPinMarker from "@/assets/mapPinMarker.png";
import {showToast} from "@/lib/notify.ts";

interface PostPlacePreviewProps {
    lat: number;
    lng: number;
    onRemove: () => void;
}

export default function PostPlacePreview({ lat, lng, onRemove }: PostPlacePreviewProps) {
    const mapRef = useRef<HTMLDivElement>(null);
    const markerRef = useRef<kakao.maps.Marker | null>(null);
    const mapInstance = useRef<kakao.maps.Map | null>(null);

    const handleRemove = () => {
        showToast({
            message: "장소를 삭제할까요?",
            actionLabel: "삭제",
            cancelLabel: "취소",
            onConfirm: onRemove,
            onCancel: () => {},
        });
    };

    useEffect(() => {
        const { kakao } = window;
        const pos = new kakao.maps.LatLng(lat, lng);
        const markerImage = new kakao.maps.MarkerImage(
            mapPinMarker,
            new kakao.maps.Size(80, 80),
            new kakao.maps.Point(40, 62),
        );

        if (!mapInstance.current) {
            mapInstance.current = new kakao.maps.Map(mapRef.current, { center: pos, level: 3 });
            mapInstance.current.setDraggable(false);
            mapInstance.current.setZoomable(false);
        } else {
            mapInstance.current.setCenter(pos);
        }

        if (markerRef.current) markerRef.current.setMap(null);
        markerRef.current = new kakao.maps.Marker({
            position: pos,
            map: mapInstance.current,
            image: markerImage,
            clickable: false,
        });

        return () => {
            markerRef.current?.setMap(null);
        };
    }, [lat, lng]);

    return (
        <div ref={mapRef} className="w-full h-50 rounded-xl relative">
            <Button
                size="icon"
                className="absolute top-2 right-4 size-5 z-10 cursor-pointer"
                onClick={handleRemove}
            >
                <X className="size-3" />
            </Button>
        </div>
    );
}
