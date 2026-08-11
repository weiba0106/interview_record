import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import { http, HttpResponse } from 'msw'
import SettingsView from './SettingsView.vue'
import { server } from '@/test/msw-server'
import { useAuthStore } from '@/shared/auth/auth.store'

describe('SettingsView', () => {
  beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
  beforeEach(() => server.use(
    http.get('/api/v1/me/preferences', () => HttpResponse.json({ displayName: '小林', timeZone: 'Asia/Shanghai', theme: 'GRAPHITE_CORAL', interviewReminderOffsets: [1440, 30], deadlineReminderOffsets: [1440] })),
  ))
  afterEach(() => server.resetHandlers())
  afterAll(() => server.close())

  it('requires password re-entry before exposing destructive account deletion', async () => {
    const wrapper = mount(SettingsView, { global: { plugins: [createPinia(), ElementPlus] } })

    await wrapper.get('button[data-action="open-delete-dialog"]').trigger('click')
    expect(wrapper.get('button[data-action="delete-account"]').attributes('disabled')).toBeDefined()
    await wrapper.get('input[name="deletePassword"]').setValue('Password123')
    expect(wrapper.get('button[data-action="delete-account"]').attributes('disabled')).toBeUndefined()
  })

  it('keeps the saved reminder offsets instead of overwriting them with stale current-user data', async () => {
    server.use(
      http.get('/api/v1/auth/csrf', () => new HttpResponse(null, { status: 204 })),
      http.patch('/api/v1/me/preferences', () => HttpResponse.json({ displayName: '小林', timeZone: 'Asia/Shanghai', theme: 'GRAPHITE_CORAL', interviewReminderOffsets: [60, 5], deadlineReminderOffsets: [10] })),
      http.get('/api/v1/me', () => HttpResponse.json({ id: '42', email: 'user@example.com', displayName: '旧显示名称', emailVerified: true, timeZone: 'UTC', theme: 'INDIGO' })),
    )
    const pinia = createPinia()
    const auth = useAuthStore(pinia)
    auth.$patch({ status: 'authenticated', user: { id: '42', email: 'user@example.com', displayName: '小林', emailVerified: true, timeZone: 'Asia/Shanghai', theme: 'GRAPHITE_CORAL' } })
    const wrapper = mount(SettingsView, { global: { plugins: [pinia, ElementPlus] } })

    await wrapper.get('input[name="interviewReminderOffsets"]').setValue('5, 60')
    await wrapper.get('input[name="deadlineReminderOffsets"]').setValue('10')
    await wrapper.get('form').trigger('submit.prevent')

    await vi.waitFor(() => expect(wrapper.text()).toContain('偏好已保存'))
    expect((wrapper.get('input[name="interviewReminderOffsets"]').element as HTMLInputElement).value).toBe('60, 5')
    expect((wrapper.get('input[name="deadlineReminderOffsets"]').element as HTMLInputElement).value).toBe('10')
  })

  it('loads persisted reminder offsets when the settings view mounts again', async () => {
    server.use(
      http.get('/api/v1/me/preferences', () => HttpResponse.json({ displayName: '小林', timeZone: 'Asia/Shanghai', theme: 'GRAPHITE_CORAL', interviewReminderOffsets: [60, 5], deadlineReminderOffsets: [10] })),
    )
    const pinia = createPinia()
    const auth = useAuthStore(pinia)
    auth.$patch({ status: 'authenticated', user: { id: '42', email: 'user@example.com', displayName: '旧显示名称', emailVerified: true, timeZone: 'UTC', theme: 'INDIGO' } })
    const wrapper = mount(SettingsView, { global: { plugins: [pinia, ElementPlus] } })

    await vi.waitFor(() => expect((wrapper.get('input[name="interviewReminderOffsets"]').element as HTMLInputElement).value).toBe('60, 5'))
    expect((wrapper.get('input[name="deadlineReminderOffsets"]').element as HTMLInputElement).value).toBe('10')
  })
})
