import { api } from '@/lib/axios'
import type { AnalyzeRequest, AnalyzeResult } from './types'

export const analysisApi = {
    analyze: async (req: AnalyzeRequest, signal?: AbortSignal): Promise<AnalyzeResult> => {
        const formData = new FormData()
        formData.append('address', req.address)
        formData.append('detailAddress', req.detailAddress)
        formData.append('deposit', req.deposit)
        req.registryFiles.forEach(f => formData.append('registryFiles', f))
        req.buildingFiles.forEach(f => formData.append('buildingFiles', f))

        const res = await api.post<AnalyzeResult>('/api/analyze', formData, {
            headers: { 'Content-Type': 'multipart/form-data' },
            signal,
        })
        return res.data
    },
}
