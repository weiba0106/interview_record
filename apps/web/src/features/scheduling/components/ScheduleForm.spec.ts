import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { http, HttpResponse } from 'msw'
import ScheduleForm from './ScheduleForm.vue'
import { server } from '@/test/msw-server'

const positions = [{
  id: '101', title: '后端开发工程师', companyName: '示例科技', companyId: '11',
  jobTypeId: '21', jobTypeName: '秋招', status: { id: '31', name: '投递中', color: '#46a758', statisticsCategory: 'ACTIVE' },
  appliedAt: null, deadlineAt: null, archived: false, updatedAt: '2026-08-01T00:00:00Z',
}]
const rounds = [
  { id: 'r1', positionId: '101', positionTitle: '后端开发工程师', companyName: '示例科技', roundName: '一面', roundNumber: 1, interviewType: 'VIDEO', startsAt: null, endsAt: null, location: null, result: 'UPCOMING', processNotes: null, reviewSummary: null, questions: [], scheduleIds: [], version: 0, createdAt: '2026-08-01T00:00:00Z', updatedAt: '2026-08-01T00:00:00Z' },
  { id: 'r2', positionId: '101', positionTitle: '后端开发工程师', companyName: '示例科技', roundName: '二面', roundNumber: 2, interviewType: 'VIDEO', startsAt: null, endsAt: null, location: null, result: 'UPCOMING', processNotes: null, reviewSummary: null, questions: [], scheduleIds: [], version: 0, createdAt: '2026-08-02T00:00:00Z', updatedAt: '2026-08-02T00:00:00Z' },
]

describe('ScheduleForm', () => {
  beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
  beforeEach(() => {
    server.use(
      http.get('/api/v1/positions/101/interview-rounds', () => HttpResponse.json({ items: rounds })),
    )
  })
  afterEach(() => server.resetHandlers())
  afterAll(() => server.close())

  async function fillRequired(wrapper: Awaited<ReturnType<typeof mountForm>>) {
    await wrapper.get('input[name="title"]').setValue('技术一面')
    const selects = wrapper.findAllComponents({ name: 'ElSelect' })
    selects[0]!.vm.$emit('update:modelValue', 'INTERVIEW')
    await wrapper.get('input[name="startsAt"]').setValue('2026-09-01T10:00')
  }

  function mountForm() {
    return mount(ScheduleForm, { props: { positions }, global: { plugins: [ElementPlus] } })
  }

  it('loads rounds for the selected position and emits the linked round id', async () => {
    const wrapper = mountForm()
    const selects = wrapper.findAllComponents({ name: 'ElSelect' })
    selects[1]!.vm.$emit('update:modelValue', '101')
    await vi.waitFor(() => expect(wrapper.get('input[name="interviewRoundId"]').attributes('disabled')).toBeUndefined())

    await fillRequired(wrapper)
    selects[2]!.vm.$emit('update:modelValue', 'r2')
    await wrapper.get('form').trigger('submit.prevent')

    const payload = wrapper.emitted('submitted')?.[0]?.[0] as Record<string, unknown>
    expect(payload.interviewRoundId).toBe('r2')
    expect(payload.positionId).toBe('101')
  })

  it('clears the round selection when the position changes', async () => {
    const wrapper = mountForm()
    const selects = wrapper.findAllComponents({ name: 'ElSelect' })
    selects[1]!.vm.$emit('update:modelValue', '101')
    await vi.waitFor(() => expect(wrapper.get('input[name="interviewRoundId"]').attributes('disabled')).toBeUndefined())
    wrapper.findAllComponents({ name: 'ElSelect' })[2]!.vm.$emit('update:modelValue', 'r1')
    await wrapper.vm.$nextTick()

    wrapper.findAllComponents({ name: 'ElSelect' })[1]!.vm.$emit('update:modelValue', '')
    await wrapper.vm.$nextTick()

    const roundSelect = wrapper.findAllComponents({ name: 'ElSelect' })[2]!
    expect((roundSelect.vm as { modelValue?: unknown }).modelValue).toBe('')
  })
})
