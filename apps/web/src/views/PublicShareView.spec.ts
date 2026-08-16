import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { http, HttpResponse } from 'msw'
import PublicShareView from './PublicShareView.vue'
import { server } from '@/test/msw-server'

function shareBody() {
  return {
    position: { positionTitle: '后端开发工程师', companyName: '示例科技', jobType: '秋招', status: '面试中' },
    rounds: [{ id: 'r1', content: { basicInfo: { roundNumber: 1, roundName: '一面', interviewType: '视频面试', startsAt: null }, processNotes: '<p>过程</p>', reviewSummary: null, result: null, questions: [] } }],
    robots: 'noindex, nofollow',
  }
}

async function mountView(token = 'valid-token') {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/share/:token', name: 'public-share', component: { template: '<div />' } }],
  })
  await router.push(`/share/${token}`)
  await router.isReady()
  return mount(PublicShareView, { global: { plugins: [router] } })
}

describe('PublicShareView', () => {
  beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
  beforeEach(() => {
    server.use(http.get('/api/v1/shares/valid-token', () => HttpResponse.json(shareBody())))
  })
  afterEach(() => {
    server.resetHandlers()
    document.head.querySelector('meta[name="robots"]')?.remove()
  })
  afterAll(() => server.close())

  it('renders the whitelisted share content', async () => {
    const wrapper = await mountView()

    await vi.waitFor(() => expect(wrapper.text()).toContain('后端开发工程师'))
    expect(wrapper.text()).toContain('示例科技')
    expect(wrapper.text()).toContain('过程')
  })

  it('applies the backend robots directive as a meta tag and removes it on unmount', async () => {
    const wrapper = await mountView()
    await vi.waitFor(() => expect(wrapper.text()).toContain('后端开发工程师'))

    const meta = document.head.querySelector('meta[name="robots"]')
    expect(meta).not.toBeNull()
    expect(meta!.getAttribute('content')).toBe('noindex, nofollow')

    wrapper.unmount()

    expect(document.head.querySelector('meta[name="robots"]')).toBeNull()
  })

  it('shows a uniform expired message for invalid tokens without exposing details', async () => {
    server.use(http.get('/api/v1/shares/expired-token', () => HttpResponse.json({ code: 'SHARE_INVALID', message: '链接已失效' }, { status: 404 })))

    const wrapper = await mountView('expired-token')

    await vi.waitFor(() => expect(wrapper.text()).toContain('分享链接不存在、已过期或已撤销'))
    expect(document.head.querySelector('meta[name="robots"]')).toBeNull()
  })
})
