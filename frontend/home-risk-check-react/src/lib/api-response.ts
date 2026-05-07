import axios from 'axios'

type ApiMetaCode = string | number

interface ApiMeta {
    code?: ApiMetaCode
    message?: string
}

export interface ApiEnvelope<T> {
    meta?: ApiMeta
    data: T
}

export interface NormalizedApiError {
    status?: number
    metaCode?: ApiMetaCode
    message: string
    field?: string
    isCanceled: boolean
    original: unknown
}

const DEFAULT_ERROR_MESSAGE = '요청에 실패했습니다.'

const isRecord = (value: unknown): value is Record<string, unknown> =>
    typeof value === 'object' && value !== null

const isApiEnvelope = (value: unknown): value is ApiEnvelope<unknown> =>
    isRecord(value) && isRecord(value.meta) && 'data' in value

const getStringValue = (value: unknown) =>
    typeof value === 'string' ? value : undefined

//성공 응답의 data만 반환
export const unwrapApiData = <T>(value: ApiEnvelope<T> | T): T => {
    if (isApiEnvelope(value)) return value.data as T
    return value as T
}

export const normalizeApiError = (
    error: unknown,
    fallbackMessage = DEFAULT_ERROR_MESSAGE,
): NormalizedApiError => {
    if (axios.isCancel(error)) {
        return {
            message: fallbackMessage,
            isCanceled: true,
            original: error,
        }
    }

    if (!axios.isAxiosError(error)) {
        return {
            message: error instanceof Error ? error.message : fallbackMessage,
            isCanceled: false,
            original: error,
        }
    }

    const responseBody = error.response?.data
    const envelope = isApiEnvelope(responseBody) ? responseBody : undefined
    const data = isRecord(envelope?.data) ? envelope.data : undefined
    const rawBody = isRecord(responseBody) ? responseBody : undefined

    return {
        status: error.response?.status,
        metaCode: envelope?.meta?.code,
        message:
            envelope?.meta?.message ??
            getStringValue(rawBody?.message) ??
            error.message ??
            fallbackMessage,
        field: getStringValue(data?.field ?? rawBody?.field),
        isCanceled: false,
        original: error,
    }
}

export const getApiErrorMessage = (
    error: unknown,
    fallbackMessage = DEFAULT_ERROR_MESSAGE,
) => normalizeApiError(error, fallbackMessage).message

export const getApiErrorStatus = (error: unknown) =>
    normalizeApiError(error).status

export const isCanceledApiError = (error: unknown) =>
    normalizeApiError(error).isCanceled
