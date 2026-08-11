import axios from 'axios'

export interface ApiError {
  code: string
  message: string
  fieldErrors: Record<string, string>
  traceId?: string
}

export class ApiRequestError extends Error {
  constructor(public readonly status: number | undefined, public readonly apiError: ApiError) {
    super(apiError.message)
    this.name = 'ApiRequestError'
  }
}

export function toApiRequestError(error: unknown): ApiRequestError {
  if (error instanceof ApiRequestError) return error
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as Partial<ApiError> | undefined
    return new ApiRequestError(error.response?.status, {
      code: data?.code ?? 'REQUEST_FAILED',
      message: data?.message ?? '请求暂时无法完成，请稍后重试',
      fieldErrors: data?.fieldErrors ?? {},
      traceId: data?.traceId,
    })
  }
  return new ApiRequestError(undefined, { code: 'REQUEST_FAILED', message: '请求暂时无法完成，请稍后重试', fieldErrors: {} })
}

export function isApiRequestError(error: unknown): error is ApiRequestError {
  return error instanceof ApiRequestError
}
