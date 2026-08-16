import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { createMemoryHistory, createRouter } from 'vue-router'
import { http, HttpResponse } from 'msw'
import ScheduleDetailView from './ScheduleDetailView.vue'
import { server } from '@/test/msw-server'

const now = Date.now()
const inHours = (hours: number) => new Date(now + hours * 3_600_000).toISOString()

function scheduleFixture(overrides: Record<string, unknown> = {}) {
  return {
    id: 's1',
    title: '技术一面',
    eventType: 'INTERVIEW',
    startsAt: inHours(5),
    endsAt: null,
    positionId: '101',
    positionTitle: '后端开发工程师',
    interviewRoundId: 'r1',
    location: '线上 · 视频面试',
    notes: '准备算法题',
    status: 'PENDING',
    urgency: 'URGENT',
    overdue: true,
    manualUrgency: null,
    referenceTime: inHours(5),
    version: 0,
    updatedAt: inHours(5),
    reminderOffsets: [1440, 30],
    reminders: [{ scheduledAt: inHours(6), status: 'FAILED', sentAt: null }],
    ...overrides,
  }
}

async function mountView() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/app/schedules/:id', name: 'schedule-detail', component: { template: '<div />' } },
      { path: '/app/schedules', name: 'schedules', component: { template: '<div />' } },
      { path: '/app/positions/:id', name: 'position-detail', component: { template: '<div />' } },
    ],
  })
  await router.push('/app/schedules/s1')
  await router.isReady()
  return mount(ScheduleDetailView, {
    global: { plugins: [ElementPlus, router], stubs: { RouterLink: { template: '<a v-bind="$attrs"><slot /></a>', props: ['to'] } } },
  })
}

describe('ScheduleDetailView', () => {
  beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
  beforeEach(() => {
    server.use(
      http.get('/api/v1/schedules/s1', () => HttpResponse.json(scheduleFixture())),
      http.get('/api/v1/positions', () => HttpResponse.json({ items: [], page: 0, size: 20, totalItems: 0, totalPages: 0 })),
    )
  })
  afterEach(() => server.resetHandlers())
  afterAll(() => server.close())

  it('shows the four-way urgency, reminder config and the failed delivery warning', async () => {
    const wrapper = await mountView()

    await vi.waitFor(() => expect(wrapper.text()).toContain('技术一面'))
    expect(wrapper.get('[data-urgency-icon="URGENT"]').text()).toContain('⚠')
    expect(wrapper.text()).toContain('紧急')
    expect(wrapper.text()).toContain('还剩')
    expect(wrapper.text()).toContain('提前 1440、30 分钟')
    expect(wrapper.text()).toContain('邮件提醒发送失败')
    expect(wrapper.text()).toContain('已关联（r1）')
  })

  it('completes the schedule from the detail page', async () => {
    server.use(
      http.get('/api/v1/auth/csrf', () => new HttpResponse(null, { status: 204 })),
      http.patch('/api/v1/schedules/s1/status', () => HttpResponse.json(scheduleFixture({ status: 'COMPLETED', urgency: 'HANDLED' }))),
    )
    const wrapper = await mountView()
    await vi.waitFor(() => expect(wrapper.text()).toContain('技术一面'))

    await wrapper.get('button[data-action="complete-schedule"]').trigger('click')

    await vi.waitFor(() => expect(wrapper.text()).toContain('日程已完成'))
  })

  it('saves custom reminders through the shared dialog', async () => {
    const captured: string[] = []
    server.use(
      http.get('/api/v1/auth/csrf', () => new HttpResponse(null, { status: 204 })),
      http.put('/api/v1/schedules/s1/reminders', async ({ request }) => {
        captured.push(await request.text())
        return HttpResponse.json(scheduleFixture({ reminderOffsets: [60] }))
      }),
    )
    const wrapper = await mountView()
    await vi.waitFor(() => expect(wrapper.text()).toContain('技术一面'))

    await wrapper.get('button[data-action="reminders-schedule"]').trigger('click')
    await wrapper.get('input[value="CUSTOM"]').setValue()
    await wrapper.get('input[name="reminderOffsets"]').setValue('60')
    await wrapper.get('button[data-action="save-reminders"]').trigger('click')

    await vi.waitFor(() => expect(wrapper.text()).toContain('提醒设置已更新'))
    expect(JSON.parse(captured[0]!)).toEqual({ offsets: [60] })
  })
})
