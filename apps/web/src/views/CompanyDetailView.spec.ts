import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { createMemoryHistory, createRouter } from 'vue-router'
import { http, HttpResponse } from 'msw'
import CompanyDetailView from './CompanyDetailView.vue'
import { server } from '@/test/msw-server'

function companyDetail() {
  return {
    id: '11',
    name: '示例科技',
    website: 'https://example.com',
    notes: '核心部门',
    positionCount: 2,
    interviewRoundCount: 3,
    scheduleCount: 4,
    shareLinkCount: 1,
    createdAt: '2026-08-01T00:00:00Z',
    updatedAt: '2026-08-10T00:00:00Z',
    positions: [
      { id: '101', title: '后端开发工程师', companyId: '11', companyName: '示例科技', jobTypeId: '21', jobTypeName: '秋招', status: { id: '31', name: '面试中', color: '#46a758', statisticsCategory: 'ACTIVE' }, appliedAt: '2026-08-01', deadlineAt: null, archived: false, updatedAt: '2026-08-10T00:00:00Z' },
      { id: '102', title: '前端开发工程师', companyId: '11', companyName: '示例科技', jobTypeId: '22', jobTypeName: '日常实习', status: { id: '32', name: 'Offer', color: '#2f81f7', statisticsCategory: 'SUCCESS' }, appliedAt: '2026-07-15', deadlineAt: '2026-08-20T12:00:00Z', archived: false, updatedAt: '2026-08-09T00:00:00Z' },
    ],
  }
}

async function mountView() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/app/companies/:id', name: 'company-detail', component: { template: '<div />' } },
      { path: '/app/positions/:id', name: 'position-detail', component: { template: '<div />' } },
      { path: '/app/positions/:id/edit', name: 'edit-position', component: { template: '<div />' } },
    ],
  })
  await router.push('/app/companies/11')
  await router.isReady()
  return mount(CompanyDetailView, {
    global: { plugins: [ElementPlus, router], stubs: { RouterLink: { template: '<a v-bind="$attrs"><slot /></a>', props: ['to'] } } },
  })
}

describe('CompanyDetailView', () => {
  beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
  beforeEach(() => {
    server.use(http.get('/api/v1/companies/11', () => HttpResponse.json(companyDetail())))
  })
  afterEach(() => server.resetHandlers())
  afterAll(() => server.close())

  it('shows company info, cascade counts and every position with its current status', async () => {
    const wrapper = await mountView()

    await vi.waitFor(() => expect(wrapper.text()).toContain('示例科技'))
    expect(wrapper.text()).toContain('https://example.com')
    expect(wrapper.text()).toContain('3')
    expect(wrapper.text()).toContain('4')
    expect(wrapper.text()).toContain('1')
    expect(wrapper.text()).toContain('后端开发工程师')
    expect(wrapper.text()).toContain('前端开发工程师')
    expect(wrapper.text()).toContain('面试中')
    expect(wrapper.text()).toContain('Offer')
    expect(wrapper.find('[data-action="create-position-for-company"]').exists()).toBe(true)
  })

  it('updates company info from the edit dialog', async () => {
    server.use(
      http.get('/api/v1/auth/csrf', () => new HttpResponse(null, { status: 204 })),
      http.put('/api/v1/companies/11', () => HttpResponse.json({ id: '11', name: '新名字科技', website: null, notes: '新备注', positionCount: 2, createdAt: '2026-08-01T00:00:00Z', updatedAt: '2026-08-11T00:00:00Z' })),
    )
    const wrapper = await mountView()
    await vi.waitFor(() => expect(wrapper.text()).toContain('示例科技'))

    await wrapper.get('button[data-action="edit-company"]').trigger('click')
    await wrapper.get('input[name="name"]').setValue('新名字科技')
    await wrapper.get('form').trigger('submit.prevent')

    await vi.waitFor(() => expect(wrapper.text()).toContain('公司已更新'))
    expect(wrapper.text()).toContain('新名字科技')
  })
})
