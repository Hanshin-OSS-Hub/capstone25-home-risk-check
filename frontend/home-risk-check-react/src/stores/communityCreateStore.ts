import { create } from 'zustand'

interface Poll {
    options: {
        id: string;
        text: string;
    }[];
    multipleChoice: boolean;
}

interface CommunityCreateStore {
    category: string
    title: string
    content: string
    images: File[]
    poll: Poll | null
    placeLat: number | null
    placeLng: number | null

    setCategory: (category: string) => void
    setTitle: (title: string) => void
    setContent: (content: string) => void
    setImages: (images: File[]) => void
    setPoll: (poll: Poll | null) => void
    setPlaceLat: (placeLat: number | null) => void
    setPlaceLng: (placeLng: number | null) => void

    reset: () => void
}

export const communityCreateStore = create<CommunityCreateStore>((set) => ({
    category: '',
    title: '',
    content: '',
    images: [],
    poll: null,
    placeLat: null,
    placeLng: null,
    setCategory: (category) => set({ category }),
    setTitle: (title) => set({ title }),
    setContent: (content) => set({ content }),
    setImages: (images) => set({ images }),
    setPoll: (poll) => set({ poll }),
    setPlaceLat: (placeLat) => set({ placeLat }),
    setPlaceLng: (placeLng) => set({ placeLng }),
    reset: () => set({ title: '', content: '', category: '', images: [], poll: null, placeLat: null, placeLng:  null }),
}))