import { http, HttpResponse } from 'msw'
import { afterAll, afterEach, beforeAll, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import LoginForm from './LoginForm.vue'
import { server } from '@/test/msw-server'

describe('LoginForm', () => {
  beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
  afterEach(() => server.resetHandlers())
  afterAll(() => server.close())

  it('shows a server authentication error in a focusable summary', async () => {
    server.use(
      http.get('/api/v1/auth/csrf', () => new HttpResponse(null, { status: 204 })),
      http.post('/api/v1/auth/login', () => HttpResponse.json({ code: 'INVALID_CREDENTIALS', message: '邮箱或密码错误', fieldErrors: { email: '邮箱或密码错误', password: '邮箱或密码错误' } }, { status: 401 })),
    )
    const wrapper = mount(LoginForm, { global: { plugins: [createPinia(), ElementPlus] } })

    await wrapper.get('input[name="email"]').setValue('candidate@example.test')
    await wrapper.get('input[name="password"]').setValue('wrong-password')
    await wrapper.get('form').trigger('submit.prevent')

    await vi.waitFor(() => expect(wrapper.get('[role="alert"]').text()).toContain('邮箱或密码错误'))
    expect(wrapper.get('[role="alert"]').attributes('tabindex')).toBe('-1')
    expect(wrapper.get('[data-field-error="email"]').text()).toContain('邮箱或密码错误')
    expect(wrapper.get('[data-field-error="password"]').text()).toContain('邮箱或密码错误')
    expect(wrapper.get('input[name="email"]').attributes('aria-describedby')).toBe('login-email-error')
    expect(wrapper.get('input[name="password"]').attributes('aria-describedby')).toBe('login-password-error')
  })
})
