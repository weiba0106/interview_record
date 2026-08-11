import { request } from '@/shared/api/http'
import type { CurrentUser } from '@/shared/auth/auth.types'

export interface Preferences {
  displayName: string
  timeZone: string
  theme: CurrentUser['theme']
  interviewReminderOffsets: number[]
  deadlineReminderOffsets: number[]
}

export async function getPreferences(): Promise<Preferences> {
  return (await request<Preferences>({ method: 'get', url: '/me/preferences' })).data
}

export async function updatePreferences(preferences: Preferences): Promise<Preferences> {
  return (await request<Preferences>({ method: 'patch', url: '/me/preferences', data: preferences })).data
}

export async function deleteAccount(password: string): Promise<void> {
  await request({ method: 'delete', url: '/me', data: { password } })
}
