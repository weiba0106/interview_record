import { http, HttpResponse } from 'msw'
import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from './auth.store'
import { server } from '@/test/msw-server'

describe('auth store', () => {
  beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
  afterEach(() => {
    server.resetHandlers()
    setActivePinia(createPinia())
  })
  afterAll(() => server.close())

  it('marks a visitor as guest when the current-user endpoint returns 401', async () => {
    server.use(http.get('/api/v1/me', () => HttpResponse.json({}, { status: 401 })))
    setActivePinia(createPinia())

    const store = useAuthStore()
    await store.loadCurrentUser()

    expect(store.status).toBe('guest')
    expect(store.user).toBeNull()
  })

  it('loads the authenticated current user without persisting a session token', async () => {
    server.use(
      http.get('/api/v1/me', () =>
        HttpResponse.json({
          id: '1', email: 'candidate@example.test', displayName: 'Candidate', emailVerified: true,
          timeZone: 'Asia/Shanghai', theme: 'GRAPHITE_CORAL',
        }),
      ),
    )
    setActivePinia(createPinia())

    const store = useAuthStore()
    await store.loadCurrentUser()

    expect(store.status).toBe('authenticated')
    expect(store.user?.email).toBe('candidate@example.test')
    expect(Object.keys(store.$state)).not.toContain('token')
  })
})
