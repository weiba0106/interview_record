import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { http, HttpResponse } from 'msw'
import QuestionBankView from './QuestionBankView.vue'
import { server } from '@/test/msw-server'

function item(id: string, question: string, category: string) {
  return {
    id, question, answer: '参考答案', category, roundId: 'r1', roundNumber: 1, roundName: '一面',
    positionId: 'p1', positionTitle: '后端开发工程师', companyName: '示例科技',
    createdAt: '2026-08-13T00:00:00Z',
  }
}

describe('QuestionBankView', () => {
  const listUrls: string[] = []
  const randomUrls: string[] = []

  beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
  beforeEach(() => {
    listUrls.length = 0
    randomUrls.length = 0
    server.use(
      http.get('/api/v1/interview-rounds/question-categories', () => HttpResponse.json(['算法', '项目'])),
      http.get('/api/v1/interview-rounds/questions', ({ request }) => {
        listUrls.push(request.url)
        return HttpResponse.json({
          items: [item('q1', '动态规划是什么', '算法'), item('q2', '介绍一个项目', '项目')],
          page: 0, size: 20, totalItems: 2, totalPages: 1,
        })
      }),
      http.get('/api/v1/interview-rounds/questions/random', ({ request }) => {
        randomUrls.push(request.url)
        return HttpResponse.json([item('q2', '介绍一个项目', '项目')])
      }),
    )
  })
  afterEach(() => server.resetHandlers())
  afterAll(() => server.close())

  it('lists questions with company, position, round and category context', async () => {
    const wrapper = mount(QuestionBankView, { global: { plugins: [ElementPlus] } })

    await vi.waitFor(() => expect(wrapper.find('[data-testid="question-q1"]').exists()).toBe(true))
    expect(wrapper.text()).toContain('动态规划是什么')
    expect(wrapper.text()).toContain('示例科技 · 后端开发工程师 · 第 1 轮 · 一面')
    expect(wrapper.text()).toContain('算法')
  })

  it('reveals and hides the answer on demand', async () => {
    const wrapper = mount(QuestionBankView, { global: { plugins: [ElementPlus] } })
    await vi.waitFor(() => expect(wrapper.find('[data-testid="question-q1"]').exists()).toBe(true))

    expect(wrapper.find('[data-testid="answer-q1"]').exists()).toBe(false)
    await wrapper.get('button[data-action="reveal-q1"]').trigger('click')
    expect(wrapper.get('[data-testid="answer-q1"]').text()).toBe('参考答案')

    await wrapper.get('button[data-action="reveal-q1"]').trigger('click')
    expect(wrapper.find('[data-testid="answer-q1"]').exists()).toBe(false)
  })

  it('forwards keyword and category filters to the search endpoint', async () => {
    const wrapper = mount(QuestionBankView, { global: { plugins: [ElementPlus] } })
    await vi.waitFor(() => expect(wrapper.find('[data-testid="question-q1"]').exists()).toBe(true))

    await wrapper.get('input[name="questionKeyword"]').setValue('动态规划')
    wrapper.findAllComponents({ name: 'ElSelect' })[0]!.vm.$emit('update:modelValue', '算法')
    await wrapper.get('form').trigger('submit.prevent')

    await vi.waitFor(() => expect(listUrls.length).toBeGreaterThanOrEqual(2))
    const filtered = new URL(listUrls[listUrls.length - 1]!)
    expect(filtered.searchParams.get('keyword')).toBe('动态规划')
    expect(filtered.searchParams.get('category')).toBe('算法')
  })

  it('draws random questions and returns to the list', async () => {
    const wrapper = mount(QuestionBankView, { global: { plugins: [ElementPlus] } })
    await vi.waitFor(() => expect(wrapper.find('[data-testid="question-q1"]').exists()).toBe(true))

    await wrapper.get('button[data-action="draw-random"]').trigger('click')

    await vi.waitFor(() => expect(randomUrls).toHaveLength(1))
    expect(wrapper.find('[data-testid="question-q1"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="question-q2"]').exists()).toBe(true)

    await wrapper.get('button[data-action="exit-random"]').trigger('click')

    await vi.waitFor(() => expect(wrapper.find('[data-testid="question-q1"]').exists()).toBe(true))
  })
})
