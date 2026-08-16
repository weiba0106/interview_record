import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import { createMemoryHistory, createRouter } from 'vue-router'
import AppShell from './components/AppShell.vue'
import { useAuthStore } from '@/shared/auth/auth.store'
import { getPreferences, updatePreferences, type Preferences } from '@/features/preferences/api/preferences.api'

vi.mock('@/features/preferences/api/preferences.api', () => ({
  getPreferences: vi.fn<() => Promise<Preferences>>(),
  updatePreferences: vi.fn<(preferences: Preferences) => Promise<Preferences>>(),
}))

async function mountShell() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/app', name: 'app', component: { template: '<div />' } }],
  })
  await router.push('/app')
  await router.isReady()
  const pinia = createPinia()
  const auth = useAuthStore(pinia)
  auth.$patch({
    status: 'authenticated',
    user: {
      id: '42', email: 'user@example.com', displayName: '小林', emailVerified: true,
      timeZone: 'Asia/Shanghai', theme: 'GRAPHITE_CORAL',
    },
  })
  return {
    wrapper: mount(AppShell, {
      global: {
        plugins: [pinia, router, ElementPlus],
        stubs: { RouterLink: { template: '<a v-bind="$attrs"><slot /></a>', props: ['to'] } },
      },
    }),
    auth,
  }
}

describe('AppShell', () => {
  beforeEach(() => {
    vi.mocked(getPreferences).mockResolvedValue({
      displayName: '小林', timeZone: 'Asia/Shanghai', theme: 'GRAPHITE_CORAL',
      interviewReminderOffsets: [1440, 30], deadlineReminderOffsets: [1440],
    })
    vi.mocked(updatePreferences).mockImplementation(async (preferences) => preferences)
  })

  it('opens a four-theme picker from the top bar and persists a selection', async () => {
    const { wrapper, auth } = await mountShell()

    await wrapper.get('[data-action="open-theme-picker"]').trigger('click')
    expect(wrapper.findAll('[data-theme-option]').map((item) => item.attributes('data-theme-option')))
      .toEqual(['INDIGO', 'FOREST_TEAL', 'WARM_APRICOT', 'GRAPHITE_CORAL'])

    await wrapper.get('[data-theme-option="FOREST_TEAL"]').trigger('click')

    expect(document.documentElement.dataset.theme).toBe('FOREST_TEAL')
    expect(auth.user?.theme).toBe('FOREST_TEAL')
    expect(updatePreferences).toHaveBeenCalledWith(expect.objectContaining({ theme: 'FOREST_TEAL' }))
  })

  it('uses the compact desktop navigation while keeping labels available to assistive technology', async () => {
    const { wrapper } = await mountShell()

    expect(wrapper.get('.shell-sidebar').attributes('data-layout')).toBe('compact')
    expect(wrapper.get('.shell-nav-link[aria-label="概览"]').attributes('title')).toBe('概览')
  })

  it('closes the theme picker with Escape', async () => {
    const { wrapper } = await mountShell()

    await wrapper.get('[data-action="open-theme-picker"]').trigger('click')
    expect(wrapper.find('[role="listbox"]').exists()).toBe(true)

    await wrapper.get('[data-action="open-theme-picker"]').trigger('keydown', { key: 'Escape' })
    expect(wrapper.find('[role="listbox"]').exists()).toBe(false)
  })

  it('toggles dark mode and persists it through the preference API', async () => {
    const { wrapper } = await mountShell()
    const updated = vi.mocked(updatePreferences)
    updated.mockImplementation(async (preferences) => preferences)

    await wrapper.get('[data-action="toggle-dark"]').trigger('click')

    expect(document.documentElement.dataset.dark).toBe('true')
    expect(updatePreferences).toHaveBeenCalledWith(expect.objectContaining({ darkMode: true }))
    expect(localStorage.getItem('interview-record.dark')).toBe('1')

    await wrapper.get('[data-action="toggle-dark"]').trigger('click')

    expect(document.documentElement.dataset.dark).toBeUndefined()
    expect(updatePreferences).toHaveBeenCalledWith(expect.objectContaining({ darkMode: false }))
  })
})
