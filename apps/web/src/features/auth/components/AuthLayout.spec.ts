import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import AuthLayout from './AuthLayout.vue'

describe('AuthLayout', () => {
  beforeEach(() => localStorage.clear())
  afterEach(() => {
    localStorage.clear()
    delete document.documentElement.dataset.dark
  })

  function mountLayout() {
    return mount(AuthLayout, {
      props: { eyebrow: '欢迎回来', title: '登录', description: '描述' },
      global: { stubs: { RouterLink: { template: '<a v-bind="$attrs"><slot /></a>', props: ['to'] } } },
    })
  }

  it('renders the auth shell with a dark mode toggle', () => {
    const wrapper = mountLayout()

    expect(wrapper.find('[data-action="toggle-dark-auth"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('登录')
  })

  it('applies the stored dark preference on mount for logged-out pages', () => {
    localStorage.setItem('interview-record.dark', '1')

    mountLayout()

    expect(document.documentElement.dataset.dark).toBe('true')
  })

  it('toggles dark mode and mirrors it to localStorage', async () => {
    const wrapper = mountLayout()

    await wrapper.get('[data-action="toggle-dark-auth"]').trigger('click')

    expect(document.documentElement.dataset.dark).toBe('true')
    expect(localStorage.getItem('interview-record.dark')).toBe('1')

    await wrapper.get('[data-action="toggle-dark-auth"]').trigger('click')

    expect(document.documentElement.dataset.dark).toBeUndefined()
    expect(localStorage.getItem('interview-record.dark')).toBe('0')
  })
})
