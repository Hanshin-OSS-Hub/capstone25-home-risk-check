import { useMutation } from '@tanstack/react-query'
import { analysisApi } from '../api'
import type { AnalyzeRequest } from '../types'

export const useAnalyze = () =>
    useMutation({
        mutationFn: ({ signal, ...req }: AnalyzeRequest & { signal?: AbortSignal }) =>
            analysisApi.analyze(req, signal),
    })
