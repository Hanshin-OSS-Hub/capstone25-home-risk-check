/**
 * 카카오 맵 SDK 최소 타입 선언.
 * 사용 중인 API 만 노출 — 필요 시 점진적으로 확장한다.
 * 모듈로 인식되지 않도록 import/export 를 두지 않는다.
 */
declare namespace kakao.maps {
    class LatLng {
        constructor(lat: number, lng: number)
        getLat(): number
        getLng(): number
    }

    class Size {
        constructor(width: number, height: number)
    }

    class Point {
        constructor(x: number, y: number)
    }

    interface MapOptions {
        center: LatLng
        level?: number
        minLevel?: number
        maxLevel?: number
    }

    class Map {
        constructor(container: HTMLElement | null, options: MapOptions)
        setCenter(position: LatLng): void
        getCenter(): LatLng
        setDraggable(draggable: boolean): void
        setZoomable(zoomable: boolean): void
    }

    interface MarkerImageOptions {
        offset?: Point
        spriteOrigin?: Point
        spriteSize?: Size
    }

    class MarkerImage {
        constructor(src: string, size: Size, options?: MarkerImageOptions | Point)
    }

    interface MarkerOptions {
        position: LatLng
        map?: Map
        image?: MarkerImage
        clickable?: boolean
    }

    class Marker {
        constructor(options: MarkerOptions)
        setMap(map: Map | null): void
        setPosition(position: LatLng): void
    }

    namespace event {
        function addListener(target: unknown, type: string, handler: (...args: unknown[]) => void): void
        function removeListener(target: unknown, type: string, handler: (...args: unknown[]) => void): void
    }

    namespace services {
        enum Status {
            OK = 'OK',
            ZERO_RESULT = 'ZERO_RESULT',
            ERROR = 'ERROR',
        }

        interface Place {
            id: string
            place_name: string
            address_name: string
            road_address_name?: string
            category_name: string
            x: string
            y: string
        }

        interface KeywordSearchOptions {
            useMapBounds?: boolean
            location?: LatLng
            radius?: number
            category_group_code?: string
        }

        interface GeocoderResult {
            address_name: string
            x: string
            y: string
            address?: { address_name: string }
            road_address?: { address_name: string } | null
        }

        type SearchCallback<T> = (data: T[], status: Status) => void

        class Places {
            constructor(map?: Map)
            keywordSearch(
                keyword: string,
                callback: SearchCallback<Place>,
                options?: KeywordSearchOptions,
            ): void
        }

        class Geocoder {
            coord2Address(lng: number, lat: number, callback: SearchCallback<GeocoderResult>): void
            addressSearch(addr: string, callback: SearchCallback<GeocoderResult>): void
        }
    }
}

interface Window {
    kakao: typeof kakao
}
