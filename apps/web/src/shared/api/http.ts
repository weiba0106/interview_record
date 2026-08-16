import axios, { type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { toApiRequestError } from './error'

export const httpClient = axios.create({
  baseURL: '/api/v1', withCredentials: true, xsrfCookieName: 'XSRF-TOKEN', xsrfHeaderName: 'X-XSRF-TOKEN',
})

let csrfReady = false

function isMutation(config: AxiosRequestConfig): boolean {
  return ['post', 'put', 'patch', 'delete'].includes(config.method?.toLowerCase() ?? '')
}

async function ensureCsrf(): Promise<void> {
  if (csrfReady) return
  try {
    await httpClient.get('/auth/csrf')
    csrfReady = true
  } catch (error) {
    throw toApiRequestError(error)
  }
}

export async function request<T>(config: AxiosRequestConfig): Promise<AxiosResponse<T>> {
  const mutation = isMutation(config)
  if (mutation) await ensureCsrf()
  try {
    return await httpClient.request<T>(config)
  } catch (error) {
    const normalized = toApiRequestError(error)
    const csrfError = normalized.apiError.code === 'INVALID_CSRF_TOKEN'
    if (!mutation || !csrfError) throw normalized
    csrfReady = false
    await ensureCsrf()
    try {
      return await httpClient.request<T>(config)
    } catch (retryError) {
      throw toApiRequestError(retryError)
    }
  }
}
