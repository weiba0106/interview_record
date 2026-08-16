import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import { createMemoryHistory, createRouter } from 'vue-router'
import { http, HttpResponse } from 'msw'
import SchedulesView from './SchedulesView.vue'
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
    interviewRoundId: null,
    location: '线上',
    notes: null,
    status: 'PENDING',
    urgency: 'URGENT',
    overdue: true,
    manualUrgency: null,
    referenceTime: inHours(5),
    version: 0,
    updatedAt: inHours(5),
    reminderOffsets: null,
    reminders: [],
    ...overrides,
  }
}

async function mountView() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/app/schedules', name: 'schedules', component: { template: '<div />' } },
      { path: '/app/schedules/:id', name: 'schedule-detail', component: { template: '<div />' } },
    ],
  })
  await router.push('/app/schedules')
  await router.isReady()
  return mount(SchedulesView, {
    global: {
      plugins: [createPinia(), ElementPlus, router],
      stubs: { RouterLink: { template: '<a v-bind="$attrs"><slot /></a>', props: ['to'] } },
    },
  })
}

describe('SchedulesView', () => {
  const capturedBodies: string[] = []

  beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
  beforeEach(() => {
    capturedBodies.length = 0
    server.use(
      http.get('/api/v1/auth/csrf', () => new HttpResponse(null, { status: 204 })),
      http.get('/api/v1/schedules', () => HttpResponse.json({
        items: [
          scheduleFixture({ reminders: [{ scheduledAt: inHours(6), status: 'FAILED', sentAt: null }] }),
          scheduleFixture({
            id: 's2', title: '笔试测评', status: 'COMPLETED', urgency: 'HANDLED',
            reminderOffsets: [], reminders: [],
          }),
        ],
      })),
      http.get('/api/v1/positions', () => HttpResponse.json({ items: [], page: 0, size: 20, totalItems: 0, totalPages: 0 })),
      http.put('/api/v1/schedules/s1/reminders', async ({ request }) => {
        capturedBodies.push(await request.text())
        return HttpResponse.json(scheduleFixture({ reminderOffsets: [1440, 30] }))
      }),
    )
  })
  afterEach(() => server.resetHandlers())
  afterAll(() => server.close())

  it('shows urgency, reminder configuration and a final delivery failure warning', async () => {
    const wrapper = await mountView()

    await vi.waitFor(() => expect(wrapper.find('[data-testid="schedule-s1"]').exists()).toBe(true))
    expect(wrapper.get('[data-testid="reminder-failed-s1"]').text()).toContain('邮件提醒发送失败')
    expect(wrapper.get('[data-testid="reminder-config-s1"]').text()).toContain('默认规则')
    expect(wrapper.get('[data-testid="reminder-config-s2"]').text()).toContain('已关闭')
    expect(wrapper.text()).toContain('紧急')
    expect(wrapper.text()).toContain('还剩')
  })

  it('saves custom reminder offsets from the per-schedule reminder dialog', async () => {
    const wrapper = await mountView()
    await vi.waitFor(() => expect(wrapper.find('[data-testid="schedule-s1"]').exists()).toBe(true))

    await wrapper.get('button[data-action="reminders-s1"]').trigger('click')
    expect(wrapper.find('[data-testid="schedule-s1"]').exists()).toBe(true)
    await wrapper.get('input[value="CUSTOM"]').setValue()
    await wrapper.get('input[name="reminderOffsets"]').setValue('1440, 30')
    await wrapper.get('button[data-action="save-reminders"]').trigger('click')

    await vi.waitFor(() => expect(wrapper.text()).toContain('提醒设置已更新'))
    expect(JSON.parse(capturedBodies.at(-1) ?? '{}')).toEqual({ offsets: [1440, 30] })
    expect(wrapper.get('[data-testid="reminder-config-s1"]').text()).toContain('提前 1440、30 分钟')
  })

  it('rejects invalid custom offsets before calling the API', async () => {
    const wrapper = await mountView()
    await vi.waitFor(() => expect(wrapper.find('[data-testid="schedule-s1"]').exists()).toBe(true))

    await wrapper.get('button[data-action="reminders-s1"]').trigger('click')
    await wrapper.get('input[value="CUSTOM"]').setValue()
    await wrapper.get('input[name="reminderOffsets"]').setValue('20000')
    await wrapper.get('button[data-action="save-reminders"]').trigger('click')

    expect(wrapper.get('p[role="alert"]').text()).toContain('0 到 10080')
    expect(capturedBodies).toHaveLength(0)
  })
})
