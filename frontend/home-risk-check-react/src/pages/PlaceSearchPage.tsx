import InputBasic from "@/components/InputBasic.tsx";
import {useEffect, useRef, useState} from "react";
import {useLocation, useNavigate} from "react-router-dom";
import {communityCreateStore} from "@/features/community/stores/communityCreateStore";
import {ROUTES} from "@/constants/routes";
import mapPinMarker from '@/assets/mapPinMarker.png'

export default function PlaceSearchPage() {
    const [address, setAddress] = useState('')
    const [placeResponse, setPlaceResponse] = useState<kakao.maps.services.Place[]>([])
    const setPlaceLat = communityCreateStore(s => s.setPlaceLat)
    const setPlaceLng = communityCreateStore(s => s.setPlaceLng)
    const navigate = useNavigate()
    const location = useLocation()
    const mapRef = useRef<HTMLDivElement>(null)
    const mapInstance = useRef<kakao.maps.Map | null>(null);
    const [showGuide, setShowGuide] = useState(true);
    const [opacity, setOpacity] = useState(true);

    useEffect(() => {
        const fadeTimer = setTimeout(() => setOpacity(false), 2000);
        const removeTimer = setTimeout(() => setShowGuide(false), 3000); // DOM에서 제거
        return () => {
            clearTimeout(fadeTimer);
            clearTimeout(removeTimer);
        };
    }, []);

    useEffect(() => {
        if (location.state?.address) {
            setAddress(location.state.address)
            navigate(location.pathname, { replace: true, state: null })
        }
    }, [location.state, location.pathname, navigate])

    // ① 지도 초기화 — 마운트 시 1회만
    useEffect(() => {
        const { kakao } = window;
        if (mapInstance.current) return;

        const map = new kakao.maps.Map(mapRef.current, {
            center: new kakao.maps.LatLng(37.5665, 126.9780),
            level: 4,
            minLevel: 3,
            maxLevel: 5,
        });

        const markerImage = new kakao.maps.MarkerImage(
            mapPinMarker,
            new kakao.maps.Size(80, 80),
            new kakao.maps.Point(40, 62),
        );

        const ps = new kakao.maps.services.Places(map);
        let markers: kakao.maps.Marker[] = [];
        let debounceTimer: ReturnType<typeof setTimeout> | undefined;

        const clearMarkers = () => {
            markers.forEach(m => m.setMap(null));
            markers = [];
        };

        const displayMarker = (place: kakao.maps.services.Place) => {
            const marker = new kakao.maps.Marker({
                map,
                position: new kakao.maps.LatLng(Number(place.y), Number(place.x)),
                image: markerImage,
            });
            markers.push(marker);
        };

        const isValidPlace = (place: kakao.maps.services.Place) => {
            const category = place.category_name;
            return (
                (category.includes("부동산 > 주거시설 > 아파트") && !category.includes("아파트 동")) ||
                (category.includes("부동산 > 주거시설 > 오피스텔") && !category.includes("오피스텔 동")) ||
                category.includes("부동산 > 주거시설 > 빌라,주택")
            );
        };

        const searchPlaces = () => {
            clearMarkers();

            ps.keywordSearch("주거시설", (data, status) => {
                if (status !== kakao.maps.services.Status.OK) {
                    setPlaceResponse([]);
                    return;
                }
                const valid = data.filter(isValidPlace);
                valid.forEach(displayMarker);
                setPlaceResponse(valid);
            }, { useMapBounds: true });
        };

        const handleDragEnd = () => {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(searchPlaces, 300);
        };
        kakao.maps.event.addListener(map, "dragend", handleDragEnd);

        mapInstance.current = map;
        const initialTimer = setTimeout(searchPlaces, 300);

        return () => {
            kakao.maps.event.removeListener(map, "dragend", handleDragEnd);
            clearTimeout(debounceTimer);
            clearTimeout(initialTimer);
            clearMarkers();
            mapInstance.current = null;
        };
    }, []); // ← 빈 배열, 마운트 1회

    // ② 주소 변경 시 지도 이동 — address 바뀔 때만
    useEffect(() => {
        if (!address || !mapInstance.current) return;

        const { kakao } = window;
        const geocoder = new kakao.maps.services.Geocoder();

        geocoder.addressSearch(address, (result, status) => {
            if (status === kakao.maps.services.Status.OK && result[0]) {
                const coords = new kakao.maps.LatLng(Number(result[0].y), Number(result[0].x));
                mapInstance.current?.setCenter(coords);
            }
        });
    }, [address]); // ← address만 의존

    return (
        <>
            <div className="flex flex-col gap-2">
                <div onClick={() => navigate(ROUTES.addressSearch, {state: {from: location.pathname}, replace:true})}>
                    <InputBasic
                        placeholder="장소명을 입력해주세요"
                        value={address}
                        onChange={setAddress}
                        isClearable={false}
                        isReadOnly={true}
                    />
                </div>
                <div style={{ position: "relative" }}>
                    <div ref={mapRef} className="w-full h-60 rounded-xl"/>
                    {showGuide && (
                        <div className={`absolute inset-0 bg-black/40 flex items-center justify-center z-10 rounded-xl
                            transition-opacity duration-500 ${opacity ? 'opacity-100' : 'opacity-0'}`}>
                            <span className="text-white text-sm">
                                지도를 움직여 장소를 선택하세요
                            </span>
                        </div>
                    )}
                </div>
                <div className="divide-y overflow-y-auto max-h-60 no-scrollbar">
                    {placeResponse.map((item) => (
                        <div
                            key={item.id}
                            className="p-4 cursor-pointer hover:bg-gray-50 active:bg-gray-100"
                            onMouseEnter={() => {
                                const { kakao } = window;
                                const coords = new kakao.maps.LatLng(Number(item.y), Number(item.x));
                                mapInstance.current?.setCenter(coords);
                            }}
                            onClick={() => {
                                setPlaceLat(Number(item.y));
                                setPlaceLng(Number(item.x));
                                navigate(ROUTES.communityNew);
                            }}
                        >
                            <p className="font-medium text-sm">
                                {item.place_name}
                            </p>
                            <p className="text-xs text-gray-500 mt-1">
                                {item.address_name}
                            </p>
                        </div>
                    ))}
                </div>
            </div>
        </>
    )
}