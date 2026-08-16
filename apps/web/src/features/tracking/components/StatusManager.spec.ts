import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { http, HttpResponse } from 'msw'
import StatusManager from './StatusManager.vue'
import { server } from '@/test/msw-server'

const statuses = [
  { id: '31', name: '待投递', sortOrder: 0, color: '#8a8f8c', statisticsCategory: 'ACTIVE', active: true, positionCount: 0 },
  { id: '32', name: '投递中', sortOrder: 1, color: '#46a758', statisticsCategory: 'ACTIVE', active: true, positionCount: 1 },
  { id: '33', name: 'Offer', sortOrder: 2, color: '#2f81f7', statisticsCategory: 'SUCCESS', active: true, positionCount: 0 },
]

describe('StatusManager', () => {
  const orderRequests: string[] = []

  beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
  beforeEach(() => {
    orderRequests.length = 0
    server.use(
      http.get('/api/v1/auth/csrf', () => new HttpResponse(null, { status: 204 })),
      http.get('/api/v1/statuses', () => HttpResponse.json(statuses)),
      http.put('/api/v1/statuses/order', async ({ request }) => {
        orderRequests.push(await request.text())
        return HttpResponse.json(statuses)
      }),
    )
  })
  afterEach(() => server.resetHandlers())
  afterAll(() => server.close())

  async function mountManager() {
    const wrapper = mount(StatusManager, { global: { plugins: [ElementPlus] } })
    await vi.waitFor(() => expect(wrapper.findAll('[data-action^="edit-status-"]').length).toBe(3))
    return wrapper
  }

  it('moves a status down by submitting the full reordered id list', async () => {
    const wrapper = await mountManager()

    await wrapper.get('button[data-action="move-status-down-31"]').trigger('click')

    await vi.waitFor(() => expect(orderRequests).toHaveLength(1))
    expect(JSON.parse(orderRequests[0]!)).toEqual({ orderedIds: ['32', '31', '33'] })
  })

  it('moves the last status up and disables out-of-range buttons', async () => {
    const wrapper = await mountManager()

    expect(wrapper.get('button[data-action="move-status-up-31"]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('button[data-action="move-status-down-33"]').attributes('disabled')).toBeDefined()

    await wrapper.get('button[data-action="move-status-up-33"]').trigger('click')

    await vi.waitFor(() => expect(orderRequests).toHaveLength(1))
    expect(JSON.parse(orderRequests[0]!)).toEqual({ orderedIds: ['31', '33', '32'] })
  })
})
