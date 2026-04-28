// stores/communityWriteStore.ts
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
    placeLat: string
    placeLng: string

    setCategory: (category: string) => void
    setTitle: (title: string) => void
    setContent: (content: string) => void
    setImages: (images: File[]) => void
    setPoll: (poll: Poll | null) => void
    setPlaceLat: (placeLat: string) => void
    setPlaceLng: (placeLng: string) => void

    reset: () => void
}

export const communityCreateStore = create<CommunityCreateStore>((set) => ({
    category: '',
    title: '',
    content: '',
    images: [],
    poll: null,
    placeLat: '',
    placeLng: '',
    setCategory: (category) => set({ category }),
    setTitle: (title) => set({ title }),
    setContent: (content) => set({ content }),
    setImages: (images) => set({ images }),
    setPoll: (poll) => set({ poll }),
    setPlaceLat: (placeLat) => set({ placeLat }),
    setPlaceLng: (placeLng) => set({ placeLng }),
    reset: () => set({ title: '', content: '', category: '', images: [], poll: null, placeLat: '', placeLng: '' }),
}))