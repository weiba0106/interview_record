import { http, HttpResponse } from 'msw'
import { afterAll, afterEach, beforeAll, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import ResetPasswordForm from './ResetPasswordForm.vue'
import { server } from '@/test/msw-server'

describe('ResetPasswordForm', () => {
  beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
  afterEach(() => server.resetHandlers())
  afterAll(() => server.close())

  it('clears both password fields after a successful reset', async () => {
    server.use(
      http.get('/api/v1/auth/csrf', () => new HttpResponse(null, { status: 204 })),
      http.post('/api/v1/auth/reset-password', () => new HttpResponse(null, { status: 204 })),
    )
    const wrapper = mount(ResetPasswordForm, { props: { token: 'opaque-token' }, global: { plugins: [ElementPlus] } })

    await wrapper.get('input[name="newPassword"]').setValue('NewPassword123')
    await wrapper.get('input[name="confirmPassword"]').setValue('NewPassword123')
    await wrapper.get('form').trigger('submit.prevent')

    await vi.waitFor(() => expect(wrapper.emitted('submitted')).toHaveLength(1))
    expect((wrapper.get('input[name="newPassword"]').element as HTMLInputElement).value).toBe('')
    expect((wrapper.get('input[name="confirmPassword"]').element as HTMLInputElement).value).toBe('')
  })

  it('associates password field errors returned by the server', async () => {
    server.use(
      http.get('/api/v1/auth/csrf', () => new HttpResponse(null, { status: 204 })),
      http.post('/api/v1/auth/reset-password', () => HttpResponse.json({ code: 'VALIDATION_FAILED', message: '请检查输入', fieldErrors: { newPassword: '密码不符合要求' } }, { status: 400 })),
    )
    const wrapper = mount(ResetPasswordForm, { props: { token: 'opaque-token' }, global: { plugins: [ElementPlus] } })

    await wrapper.get('input[name="newPassword"]').setValue('weak')
    await wrapper.get('input[name="confirmPassword"]').setValue('weak')
    await wrapper.get('form').trigger('submit.prevent')

    await vi.waitFor(() => expect(wrapper.get('[data-field-error="newPassword"]').text()).toBe('密码不符合要求'))
    expect(wrapper.get('input[name="newPassword"]').attributes('aria-describedby')).toBe('reset-password-error')
  })
})
