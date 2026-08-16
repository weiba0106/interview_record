import { createRouter, createWebHistory, type RouteLocationNormalizedLoaded } from 'vue-router'
import { useAuthStore } from '@/shared/auth/auth.store'
import type { AuthStatus } from '@/shared/auth/auth.types'
import DashboardView from '@/views/DashboardView.vue'
import CompaniesView from '@/views/CompaniesView.vue'
import CompanyDetailView from '@/views/CompanyDetailView.vue'
import PositionsView from '@/views/PositionsView.vue'
import PositionFormView from '@/views/PositionFormView.vue'
import PositionDetailView from '@/views/PositionDetailView.vue'
import SchedulesView from '@/views/SchedulesView.vue'
import ScheduleDetailView from '@/views/ScheduleDetailView.vue'
import QuestionBankView from '@/views/QuestionBankView.vue'
import ForgotPasswordView from '@/views/ForgotPasswordView.vue'
import LoginView from '@/views/LoginView.vue'
import RegisterView from '@/views/RegisterView.vue'
import ResetPasswordView from '@/views/ResetPasswordView.vue'
import VerifyEmailView from '@/views/VerifyEmailView.vue'
import SettingsView from '@/views/SettingsView.vue'
import InsightsView from '@/views/InsightsView.vue'
import PublicShareView from '@/views/PublicShareView.vue'

export function resolveGuard(to: Pick<RouteLocationNormalizedLoaded, 'meta'>, auth: Pick<{ status: AuthStatus }, 'status'>) {
  if (to.meta.requiresAuth && auth.status !== 'authenticated') return { name: 'login' }
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
      { path: '/share/:token', name: 'public-share', component: PublicShareView },
      { path: '/app', name: 'app', component: DashboardView, meta: { requiresAuth: true } },
      { path: '/app/companies', name: 'companies', component: CompaniesView, meta: { requiresAuth: true } },
      { path: '/app/companies/:id', name: 'company-detail', component: CompanyDetailView, meta: { requiresAuth: true } },
      { path: '/app/positions', name: 'positions', component: PositionsView, meta: { requiresAuth: true } },
      { path: '/app/positions/new', name: 'new-position', component: PositionFormView, meta: { requiresAuth: true } },
      { path: '/app/positions/:id', name: 'position-detail', component: PositionDetailView, meta: { requiresAuth: true } },
      { path: '/app/positions/:id/edit', name: 'edit-position', component: PositionFormView, meta: { requiresAuth: true } },
      { path: '/app/schedules', name: 'schedules', component: SchedulesView, meta: { requiresAuth: true } },
      { path: '/app/schedules/:id', name: 'schedule-detail', component: ScheduleDetailView, meta: { requiresAuth: true } },
      { path: '/app/questions', name: 'question-bank', component: QuestionBankView, meta: { requiresAuth: true } },
      { path: '/app/insights', name: 'insights', component: InsightsView, meta: { requiresAuth: true } },
      { path: '/app/settings', name: 'settings', component: SettingsView, meta: { requiresAuth: true } },
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
