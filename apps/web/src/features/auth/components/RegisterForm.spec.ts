import { http, HttpResponse } from 'msw'
import { afterAll, afterEach, beforeAll, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import RegisterForm from './RegisterForm.vue'
import { server } from '@/test/msw-server'

describe('RegisterForm', () => {
  beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
  afterEach(() => server.resetHandlers())
  afterAll(() => server.close())

  it('shows email field errors returned by the server', async () => {
    server.use(
      http.get('/api/v1/auth/csrf', () => new HttpResponse(null, { status: 204 })),
      http.post('/api/v1/auth/register', () => HttpResponse.json({ code: 'VALIDATION_FAILED', message: '请检查输入', fieldErrors: { email: '邮箱格式不正确' } }, { status: 400 })),
    )
    const wrapper = mount(RegisterForm, { global: { plugins: [ElementPlus] } })

    await wrapper.get('input[name="email"]').setValue('invalid')
    await wrapper.get('input[name="password"]').setValue('Password123')
    await wrapper.get('input[name="displayName"]').setValue('Candidate')
    await wrapper.get('form').trigger('submit.prevent')

    await vi.waitFor(() => expect(wrapper.get('[data-field-error="email"]').text()).toBe('邮箱格式不正确'))
  })
})
