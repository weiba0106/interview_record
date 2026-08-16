import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { AUTH_SESSION_FLAG, redirectToExpiredLogin, request } from './http'
import { server } from '@/test/msw-server'

describe('http session expiry handling', () => {
  beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
  beforeEach(() => {
    sessionStorage.clear()
    server.use(
      http.get('/api/v1/auth/csrf', () => new HttpResponse(null, { status: 204 })),
    )
  })
  afterEach(() => server.resetHandlers())
  afterAll(() => server.close())

  it('flags session expiry when a signed-in session hits 401', async () => {
    sessionStorage.setItem(AUTH_SESSION_FLAG, '1')
    server.use(
      http.get('/api/v1/schedules', () => HttpResponse.json({ code: 'UNAUTHENTICATED', message: '会话已过期' }, { status: 401 })),
    )

    await expect(request({ method: 'get', url: '/schedules' })).rejects.toMatchObject({ status: 401 })
    expect(sessionStorage.getItem('interview-record.session-expired')).toBe('1')
  })

  it('does not flag fresh visitors hitting 401', async () => {
    server.use(
      http.get('/api/v1/positions', () => HttpResponse.json({ code: 'UNAUTHENTICATED', message: '未登录' }, { status: 401 })),
    )

    await expect(request({ method: 'get', url: '/positions' })).rejects.toMatchObject({ status: 401 })
    expect(sessionStorage.getItem('interview-record.session-expired')).toBeNull()
  })

  it('does not flag auth endpoints like a failed login', async () => {
    sessionStorage.setItem(AUTH_SESSION_FLAG, '1')
    server.use(
      http.post('/api/v1/auth/login', () => HttpResponse.json({ code: 'INVALID_CREDENTIALS', message: '邮箱或密码错误' }, { status: 401 })),
    )

    await expect(request({ method: 'post', url: '/auth/login', data: {} })).rejects.toMatchObject({ status: 401 })
    expect(sessionStorage.getItem('interview-record.session-expired')).toBeNull()
  })

  it('marks the expired flag through the pure redirect helper', () => {
    redirectToExpiredLogin()

    expect(sessionStorage.getItem('interview-record.session-expired')).toBe('1')
  })
})
