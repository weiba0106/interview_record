import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getCurrentUser, login as loginRequest, logout as logoutRequest } from '@/features/auth/api/auth.api'
import { isApiRequestError, type ApiRequestError } from '@/shared/api/error'
import type { AuthStatus, CurrentUser, LoginCredentials } from './auth.types'

export const useAuthStore = defineStore('auth', () => {
  const status = ref<AuthStatus>('unknown')
  const user = ref<CurrentUser | null>(null)
  const error = ref<ApiRequestError | null>(null)
  const isAuthenticated = computed(() => status.value === 'authenticated')

  async function loadCurrentUser(): Promise<void> {
    status.value = 'loading'; error.value = null
    try {
      user.value = await getCurrentUser(); status.value = 'authenticated'
    } catch (caught) {
      user.value = null
      if (isApiRequestError(caught) && caught.status === 401) { status.value = 'guest'; return }
      error.value = isApiRequestError(caught) ? caught : null; status.value = 'unknown'; throw caught
    }
  }

  async function login(credentials: LoginCredentials): Promise<void> {
    error.value = null
    try { await loginRequest(credentials); await loadCurrentUser() }
    catch (caught) { error.value = isApiRequestError(caught) ? caught : null; throw caught }
  }

  async function logout(): Promise<void> {
    error.value = null
    try { await logoutRequest() }
    finally { user.value = null; status.value = 'guest' }
  }

  return { status, user, error, isAuthenticated, loadCurrentUser, login, logout }
})
