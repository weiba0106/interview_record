import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import { http, HttpResponse } from 'msw'
import DashboardView from './DashboardView.vue'
import { server } from '@/test/msw-server'

const position = {
  id: '101',
  title: '后端开发工程师',
  companyId: '11',
  companyName: '示例科技',
  jobTypeId: '21',
  jobTypeName: '秋招',
  status: { id: '31', name: '投递中', color: '#46a758', statisticsCategory: 'ACTIVE' },
  applyUrl: null,
  appliedAt: '2026-08-01T00:00:00Z',
  deadlineAt: null,
  workLocation: null,
  description: null,
  archived: false,
  interviewRoundCount: 1,
  scheduleCount: 1,
  nextSchedule: { id: '41', title: '一面', eventType: 'INTERVIEW', time: '2026-08-14T09:00:00Z' },
  version: 0,
  createdAt: '2026-08-01T00:00:00Z',
  updatedAt: '2026-08-10T00:00:00Z',
}

const schedule = {
  id: '41',
  title: '一面',
  eventType: 'INTERVIEW',
  startsAt: '2026-08-14T09:00:00Z',
  endsAt: null,
  positionId: '101',
  positionTitle: '后端开发工程师',
  interviewRoundId: null,
  location: null,
  notes: null,
  status: 'PENDING',
  urgency: 'URGENT',
  overdue: true,
  manualUrgency: null,
  referenceTime: '2026-08-12T00:00:00Z',
  version: 0,
  updatedAt: '2026-08-12T00:00:00Z',
}

function dashboardBody(overrides: Record<string, unknown> = {}) {
  return {
    metrics: { totalPositions: 1, activePositions: 1, upcomingScheduleCount: 1, offerCount: 0 },
    positions: [position],
    schedules: [schedule],
    ...overrides,
  }
}

describe('DashboardView', () => {
  beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
  beforeEach(() => server.use(
    http.get('/api/v1/dashboard', () => HttpResponse.json(dashboardBody())),
  ))
  afterEach(() => server.resetHandlers())
  afterAll(() => server.close())

  it('shows the metric cards, position table and urgency-colored schedules', async () => {
    const wrapper = mount(DashboardView, { global: { plugins: [createPinia(), ElementPlus] } })

    await vi.waitFor(() => expect(wrapper.get('[data-testid="metric-total-positions"]').text()).toBe('1'))
    expect(wrapper.get('[data-testid="metric-active-positions"]').text()).toBe('1')
    expect(wrapper.get('[data-testid="metric-upcoming-schedules"]').text()).toBe('1')
    expect(wrapper.get('[data-testid="metric-offers"]').text()).toBe('0')
    expect(wrapper.get('[data-testid="schedule-41"]').classes()).toContain('urgency-urgent')
    expect(wrapper.get('[data-testid="schedule-41"]').text()).toContain('紧急')
    expect(wrapper.get('.dashboard-main-grid')).toBeTruthy()
    expect(wrapper.get('[data-urgency-icon="URGENT"]').text()).toContain('⚠')
    expect(wrapper.get('[data-testid="dashboard-page-header"]').text()).toContain('概览')
    expect(wrapper.get('[data-testid="dashboard-urgency-summary"]').text()).toContain('今天有 1 项紧急日程')
    expect(wrapper.text()).toContain('示例科技')
    expect(wrapper.text()).toContain('后端开发工程师')
  })

  it('completes a schedule directly from the dashboard', async () => {
    server.use(
      http.get('/api/v1/auth/csrf', () => new HttpResponse(null, { status: 204 })),
      http.patch('/api/v1/schedules/41/status', () => HttpResponse.json({ ...schedule, status: 'COMPLETED', urgency: 'HANDLED' })),
    )
    const wrapper = mount(DashboardView, { global: { plugins: [createPinia(), ElementPlus] } })
    await vi.waitFor(() => expect(wrapper.find('[data-testid="schedule-41"]').exists()).toBe(true))

    await wrapper.get('button[data-action="complete-41"]').trigger('click')
    await vi.waitFor(() => expect(wrapper.text()).toContain('日程已完成'))
  })

  it('shows onboarding guidance when the user has no data yet', async () => {
    server.use(
      http.get('/api/v1/dashboard', () => HttpResponse.json(dashboardBody({
        metrics: { totalPositions: 0, activePositions: 0, upcomingScheduleCount: 0, offerCount: 0 },
        positions: [],
        schedules: [],
      }))),
    )
    const wrapper = mount(DashboardView, { global: { plugins: [createPinia(), ElementPlus] } })

    await vi.waitFor(() => expect(wrapper.find('[data-testid="dashboard-empty"]').exists()).toBe(true))
    expect(wrapper.text()).toContain('还没有面试记录')
    expect(wrapper.text()).toContain('先创建你的第一家公司和岗位')
    expect(wrapper.find('button[data-action="empty-create-position"]').exists()).toBe(true)
  })

  it('filters dashboard positions by job type chip and applied date range', async () => {
    const second = {
      ...position,
      id: '102',
      title: '前端开发工程师',
      jobTypeId: '22',
      jobTypeName: '日常实习',
      appliedAt: '2026-06-01T00:00:00Z',
      status: { id: '32', name: 'Offer', color: '#2f81f7', statisticsCategory: 'SUCCESS' },
      nextSchedule: null,
    }
    server.use(
      http.get('/api/v1/dashboard', () => HttpResponse.json(dashboardBody({
        positions: [position, second],
        schedules: [],
      }))),
    )
    const wrapper = mount(DashboardView, { global: { plugins: [createPinia(), ElementPlus] } })
    await vi.waitFor(() => expect(wrapper.text()).toContain('前端开发工程师'))

    const jobTypeChip = wrapper.findAll('button.chip').find((item) => item.text() === '日常实习')
    await jobTypeChip!.trigger('click')

    expect(wrapper.text()).toContain('前端开发工程师')
    expect(wrapper.text()).not.toContain('后端开发工程师')

    await wrapper.get('input[name="dashboardAppliedFrom"]').setValue('2026-08-01')

    expect(wrapper.text()).not.toContain('前端开发工程师')
    expect(wrapper.text()).toContain('没有匹配的岗位')
  })
})
