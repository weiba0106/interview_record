import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import { http, HttpResponse } from 'msw'
import PositionsView from './PositionsView.vue'
import { server } from '@/test/msw-server'

const companies = [{ id: '11', name: '示例科技', website: null, notes: null, positionCount: 1, createdAt: '2026-08-01T00:00:00Z', updatedAt: '2026-08-01T00:00:00Z' }]
const jobTypes = [{ id: '21', name: '秋招', active: true }]
const statuses = [{ id: '31', name: '投递中', color: '#46a758', statisticsCategory: 'ACTIVE', sortOrder: 0, active: true, positionCount: 1 }]

function positionRow(id: string, title: string) {
  return {
    id,
    title,
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
    interviewRoundCount: 0,
    scheduleCount: 0,
    nextSchedule: null,
    version: 0,
    createdAt: '2026-08-01T00:00:00Z',
    updatedAt: '2026-08-10T00:00:00Z',
  }
}

function pageBody(items: unknown[]) {
  return { items, page: 0, size: 20, totalItems: items.length, totalPages: items.length > 0 ? 1 : 0 }
}

describe('PositionsView', () => {
  const capturedUrls: string[] = []

  beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
  beforeEach(() => {
    capturedUrls.length = 0
    server.use(
      http.get('/api/v1/companies', () => HttpResponse.json(companies)),
      http.get('/api/v1/job-types', () => HttpResponse.json(jobTypes)),
      http.get('/api/v1/statuses', () => HttpResponse.json(statuses)),
      http.get('/api/v1/positions', ({ request }) => {
        capturedUrls.push(request.url)
        return HttpResponse.json(pageBody([positionRow('101', '后端开发工程师')]))
      }),
    )
  })
  afterEach(() => server.resetHandlers())
  afterAll(() => server.close())

  it('lists positions with company, job type and status columns', async () => {
    const wrapper = mount(PositionsView, { global: { plugins: [createPinia(), ElementPlus] } })

    await vi.waitFor(() => expect(wrapper.find('[data-testid="position-101"]').exists()).toBe(true))
    const row = wrapper.get('[data-testid="position-101"]')
    expect(row.text()).toContain('示例科技')
    expect(row.text()).toContain('后端开发工程师')
    expect(row.text()).toContain('秋招')
    expect(row.text()).toContain('投递中')
  })

  it('sends keyword, company and job type filters to the search endpoint', async () => {
    const wrapper = mount(PositionsView, { global: { plugins: [createPinia(), ElementPlus] } })
    await vi.waitFor(() => expect(wrapper.find('[data-testid="position-101"]').exists()).toBe(true))

    await wrapper.get('input[name="filterKeyword"]').setValue('后端')
    const selects = wrapper.findAllComponents({ name: 'ElSelect' })
    selects[0]!.vm.$emit('update:modelValue', '11')
    selects[1]!.vm.$emit('update:modelValue', '21')
    await wrapper.get('form.filter-bar').trigger('submit.prevent')

    await vi.waitFor(() => expect(capturedUrls.length).toBeGreaterThanOrEqual(2))
    const filtered = new URL(capturedUrls[capturedUrls.length - 1]!)
    expect(filtered.searchParams.get('keyword')).toBe('后端')
    expect(filtered.searchParams.get('companyId')).toBe('11')
    expect(filtered.searchParams.get('jobTypeId')).toBe('21')
    expect(filtered.searchParams.get('sortBy')).toBe('updatedAt')
  })

  it('shows an empty state when no positions match the filters', async () => {
    server.use(
      http.get('/api/v1/positions', () => HttpResponse.json(pageBody([]))),
    )
    const wrapper = mount(PositionsView, { global: { plugins: [createPinia(), ElementPlus] } })

    await vi.waitFor(() => expect(wrapper.find('[data-testid="no-positions"]').exists()).toBe(true))
    expect(wrapper.text()).toContain('没有符合条件的岗位')
  })

  it('switches between the default table and a status board without reloading data', async () => {
    const wrapper = mount(PositionsView, { global: { plugins: [createPinia(), ElementPlus] } })

    await vi.waitFor(() => expect(wrapper.find('[data-testid="position-101"]').exists()).toBe(true))
    expect(wrapper.get('[data-action="view-table"]').attributes('aria-pressed')).toBe('true')
    expect(wrapper.find('.positions-board').exists()).toBe(false)

    await wrapper.get('[data-action="view-board"]').trigger('click')

    expect(wrapper.get('[data-action="view-board"]').attributes('aria-pressed')).toBe('true')
    expect(wrapper.get('.positions-board').text()).toContain('后端开发工程师')
    expect(wrapper.get('[data-status-column="31"]').text()).toContain('投递中')
  })
})
