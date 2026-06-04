import { create } from 'zustand'
import type { Poll } from '@/features/community/types'

interface CommunityCreateStore {
    category: string
    title: string
    content: string
    images: File[]
    poll: Poll | null
    placeLat: number | null
    placeLng: number | null
    placeName: string | null
    placeAddress: string | null

    setCategory: (category: string) => void
    setTitle: (title: string) => void
    setContent: (content: string) => void
    setImages: (images: File[]) => void
    setPoll: (poll: Poll | null) => void
    setPlaceLat: (placeLat: number | null) => void
    setPlaceLng: (placeLng: number | null) => void
    setPlaceName: (placeName: string | null) => void
    setPlaceAddress: (placeAddress: string | null) => void

    reset: () => void
}

export const useCommunityCreateStore = create<CommunityCreateStore>((set) => ({
    category: '',
    title: '',
    content: '',
    images: [],
    poll: null,
    placeLat: null,
    placeLng: null,
    placeName: null,
    placeAddress: null,
    setCategory: (category) => set({ category }),
    setTitle: (title) => set({ title }),
    setContent: (content) => set({ content }),
    setImages: (images) => set({ images }),
    setPoll: (poll) => set({ poll }),
    setPlaceLat: (placeLat) => set({ placeLat }),
    setPlaceLng: (placeLng) => set({ placeLng }),
    setPlaceName: (placeName) => set({ placeName }),
    setPlaceAddress: (placeAddress) => set({ placeAddress }),
    reset: () => set({ title: '', content: '', category: '', images: [], poll: null, placeLat: null, placeLng: null, placeName: null, placeAddress: null }),
}))
