import { Toaster } from '@/components/ui/sonner'

export function ToasterProvider() {
    return (
        <Toaster
            position="top-center"
            toastOptions={{
                classNames: {
                    toast: '!flex',
                    content: '!flex-1',
                    actionButton: '!ml-0',
                    cancelButton: '!ml-0',
                },
            }}
        />
    )
}
