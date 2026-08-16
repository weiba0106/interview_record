import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { createMemoryHistory, createRouter } from 'vue-router'
import { http, HttpResponse } from 'msw'
import RegisterView from './RegisterView.vue'
import { server } from '@/test/msw-server'

async function mountView() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/register', name: 'register', component: { template: '<div />' } },
      { path: '/login', name: 'login', component: { template: '<div />' } },
    ],
  })
  await router.push('/register')
  await router.isReady()
  return mount(RegisterView, {
    global: { plugins: [ElementPlus, router], stubs: { RouterLink: { template: '<a v-bind="$attrs"><slot /></a>', props: ['to'] } } },
  })
}

describe('RegisterView', () => {
  const resendBodies: string[] = []

  beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
  beforeEach(() => {
    resendBodies.length = 0
    vi.useFakeTimers()
    server.use(
      http.get('/api/v1/auth/csrf', () => new HttpResponse(null, { status: 204 })),
      http.post('/api/v1/auth/register', () => new HttpResponse(null, { status: 204 })),
      http.post('/api/v1/auth/resend-verification', async ({ request }) => {
        resendBodies.push(await request.text())
        return new HttpResponse(null, { status: 204 })
      }),
    )
  })
  afterEach(() => {
    server.resetHandlers()
    vi.useRealTimers()
  })
  afterAll(() => server.close())

  async function register(wrapper: Awaited<ReturnType<typeof mountView>>) {
    await wrapper.get('input[name="email"]').setValue('candidate@example.com')
    await wrapper.get('input[name="password"]').setValue('Password123')
    await wrapper.get('input[name="displayName"]').setValue('候选人')
    await wrapper.get('form').trigger('submit.prevent')
    await vi.waitFor(() => expect(wrapper.text()).toContain('验证邮件已发送'))
  }

  it('enforces the 60 second cooldown before allowing a resend', async () => {
    const wrapper = await mountView()
    await register(wrapper)

    const resendButton = wrapper.get('button[data-action="resend-verification"]')
    expect(resendButton.attributes('disabled')).toBeDefined()

    await resendButton.trigger('click')
    expect(resendBodies).toHaveLength(0)

    await vi.advanceTimersByTimeAsync(61_000)

    expect(wrapper.get('button[data-action="resend-verification"]').attributes('disabled')).toBeUndefined()
    await wrapper.get('button[data-action="resend-verification"]').trigger('click')

    await vi.waitFor(() => expect(resendBodies).toHaveLength(1))
    expect(JSON.parse(resendBodies[0]!)).toEqual({ email: 'candidate@example.com' })
    expect(wrapper.text()).toContain('验证邮件已重新发送')
  })

  it('surfaces resend failures with a retryable message', async () => {
    server.use(
      http.post('/api/v1/auth/resend-verification', () => HttpResponse.json({ code: 'RATE_LIMITED', message: '发送过于频繁，请稍后再试' }, { status: 429 })),
    )
    const wrapper = await mountView()
    await register(wrapper)

    await vi.advanceTimersByTimeAsync(61_000)
    await wrapper.get('button[data-action="resend-verification"]').trigger('click')

    await vi.waitFor(() => expect(wrapper.text()).toContain('发送过于频繁，请稍后再试'))
  })
})
