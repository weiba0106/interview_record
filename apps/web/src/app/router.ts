import { createRouter, createWebHistory, type RouteLocationNormalizedLoaded } from 'vue-router'
import { useAuthStore } from '@/shared/auth/auth.store'
import type { AuthStatus } from '@/shared/auth/auth.types'
import DashboardView from '@/views/DashboardView.vue'
import ForgotPasswordView from '@/views/ForgotPasswordView.vue'
import LoginView from '@/views/LoginView.vue'
import RegisterView from '@/views/RegisterView.vue'
import ResetPasswordView from '@/views/ResetPasswordView.vue'
import VerifyEmailView from '@/views/VerifyEmailView.vue'

export function resolveGuard(to: Pick<RouteLocationNormalizedLoaded, 'meta'>, auth: Pick<{ status: AuthStatus }, 'status'>) {
  if (to.meta.requiresAuth && auth.status === 'guest') return { name: 'login' }
  if (to.meta.guestOnly && auth.status === 'authenticated') return { name: 'app' }
  return undefined
}

export function createAppRouter() {
  const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
      { path: '/', redirect: '/app' },
      { path: '/login', name: 'login', component: LoginView, meta: { guestOnly: true } },
      { path: '/register', name: 'register', component: RegisterView, meta: { guestOnly: true } },
      { path: '/verify-email', name: 'verify-email', component: VerifyEmailView },
      { path: '/forgot-password', name: 'forgot-password', component: ForgotPasswordView, meta: { guestOnly: true } },
      { path: '/reset-password', name: 'reset-password', component: ResetPasswordView, meta: { guestOnly: true } },
      { path: '/app', name: 'app', component: DashboardView, meta: { requiresAuth: true } },
    ],
  })
  router.beforeEach(async (to) => {
    const auth = useAuthStore()
    if (auth.status === 'unknown') {
      try { await auth.loadCurrentUser() } catch { /* views retain a retryable error state */ }
    }
    return resolveGuard(to, auth)
  })
  return router
}

export default createAppRouter()
