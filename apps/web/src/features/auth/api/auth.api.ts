import { request } from '@/shared/api/http'
import type { CurrentUser, LoginCredentials } from '@/shared/auth/auth.types'

export interface RegisterPayload { email: string; password: string; displayName: string; timeZone: string }

export async function getCurrentUser(): Promise<CurrentUser> { return (await request<CurrentUser>({ method: 'get', url: '/me' })).data }
export async function login(credentials: LoginCredentials): Promise<void> { await request({ method: 'post', url: '/auth/login', data: credentials }) }
export async function logout(): Promise<void> { await request({ method: 'post', url: '/auth/logout' }) }
export async function register(payload: RegisterPayload): Promise<void> { await request({ method: 'post', url: '/auth/register', data: payload }) }
export async function verifyEmail(token: string): Promise<void> { await request({ method: 'post', url: '/auth/verify-email', data: { token } }) }
export async function resendVerification(email: string): Promise<void> { await request({ method: 'post', url: '/auth/resend-verification', data: { email } }) }
export async function requestPasswordReset(email: string): Promise<void> { await request({ method: 'post', url: '/auth/forgot-password', data: { email } }) }
export async function resetPassword(token: string, newPassword: string): Promise<void> { await request({ method: 'post', url: '/auth/reset-password', data: { token, newPassword } }) }
