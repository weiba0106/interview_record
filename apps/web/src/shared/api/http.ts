import axios, { type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { toApiRequestError } from './error'

export const httpClient = axios.create({
  baseURL: '/api/v1', withCredentials: true, xsrfCookieName: 'XSRF-TOKEN', xsrfHeaderName: 'X-XSRF-TOKEN',
})

export const AUTH_SESSION_FLAG = 'interview-record.authed'

let csrfReady = false

/** 认证与公开分享相关接口的 401 不触发“会话过期”跳转（登录失败、未验证等是正常业务结果）。 */
const AUTH_EXEMPT_PATHS = [
  '/auth/login', '/auth/register', '/auth/csrf', '/auth/resend-verification',
  '/auth/forgot-password', '/auth/reset-password', '/auth/verify-email', '/me', '/shares/',
]

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

function hadSession(): boolean {
  try { return sessionStorage.getItem(AUTH_SESSION_FLAG) === '1' } catch { return false }
}

function isAuthExempt(url?: string): boolean {
  if (!url) return false
  return AUTH_EXEMPT_PATHS.some((path) => url.includes(path))
}

/** 会话过期后统一回到登录页并提示；纯函数便于测试。 */
export function redirectToExpiredLogin(): void {
  try { sessionStorage.setItem('interview-record.session-expired', '1') } catch { /* 忽略存储不可用 */ }
  if (typeof location !== 'undefined' && typeof location.assign === 'function') {
    location.assign('/login?expired=1')
  }
}

export async function request<T>(config: AxiosRequestConfig): Promise<AxiosResponse<T>> {
  const mutation = isMutation(config)
  if (mutation) await ensureCsrf()
  try {
    return await httpClient.request<T>(config)
  } catch (error) {
    const normalized = toApiRequestError(error)
    // 曾登录过的用户中途 401 视为会话过期；未登录访客与认证类接口交给守卫和表单处理
    if (normalized.status === 401 && hadSession() && !isAuthExempt(config.url)) {
      redirectToExpiredLogin()
    }
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
