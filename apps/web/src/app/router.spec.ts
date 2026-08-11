import { describe, expect, it } from 'vitest'
import type { RouteLocationNormalizedLoaded } from 'vue-router'
import { resolveGuard } from './router'

describe('authentication route guard', () => {
  it('redirects guests from protected routes to login', () => {
    const protectedRoute = { meta: { requiresAuth: true } } as Pick<RouteLocationNormalizedLoaded, 'meta'>

    expect(resolveGuard(protectedRoute, { status: 'guest' })).toEqual({ name: 'login' })
  })

  it('does not allow an unresolved current-user request to render a protected route', () => {
    const protectedRoute = { meta: { requiresAuth: true } } as Pick<RouteLocationNormalizedLoaded, 'meta'>

    expect(resolveGuard(protectedRoute, { status: 'unknown' })).toEqual({ name: 'login' })
  })

  it('does not redirect guests from public routes', () => {
    const publicRoute = { meta: {} } as Pick<RouteLocationNormalizedLoaded, 'meta'>

    expect(resolveGuard(publicRoute, { status: 'guest' })).toBeUndefined()
  })
})
