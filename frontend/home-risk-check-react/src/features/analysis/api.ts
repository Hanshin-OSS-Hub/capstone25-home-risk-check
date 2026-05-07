import { api } from '@/lib/axios'
import { unwrapApiData, type ApiEnvelope } from '@/lib/api-response'
import type { AnalyzeRequest, AnalyzeResult } from './types'

export const analysisApi = {
    analyze: async (req: AnalyzeRequest, signal?: AbortSignal): Promise<AnalyzeResult> => {
        const formData = new FormData()
        formData.append('address', req.address)
        formData.append('detailAddress', req.detailAddress)
        formData.append('deposit', req.deposit)
        req.registryFiles.forEach(f => formData.append('registryFiles', f))
        req.buildingFiles.forEach(f => formData.append('buildingFiles', f))

        const res = await api.post<ApiEnvelope<AnalyzeResult>>('/api/analyze', formData, {
            headers: { 'Content-Type': 'multipart/form-data' },
            signal,
        })
        return unwrapApiData(res.data)
    },
}
