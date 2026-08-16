import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import InterviewRoundForm from './InterviewRoundForm.vue'

function mountForm() {
  return mount(InterviewRoundForm, { global: { plugins: [ElementPlus] } })
}

describe('InterviewRoundForm', () => {
  it('requires round name, interview type and a positive round number', async () => {
    const wrapper = mountForm()
    await wrapper.get('input[name="roundNumber"]').setValue(0)

    await wrapper.get('form').trigger('submit.prevent')

    expect(wrapper.get('[data-field-error="roundName"]').text()).toBe('轮次名称不能为空')
    expect(wrapper.get('[data-field-error="roundNumber"]').text()).toContain('大于 0')
    expect(wrapper.get('[data-field-error="interviewType"]').text()).toBe('请选择面试类型')
    expect(wrapper.emitted('submitted')).toBeUndefined()
  })

  it('supports adding and removing question entries', async () => {
    const wrapper = mountForm()
    expect(wrapper.find('textarea[name="question-0"]').exists()).toBe(false)

    await wrapper.get('button[data-action="add-question"]').trigger('click')
    await wrapper.get('button[data-action="add-question"]').trigger('click')
    expect(wrapper.find('textarea[name="question-0"]').exists()).toBe(true)
    expect(wrapper.find('textarea[name="question-1"]').exists()).toBe(true)

    await wrapper.get('button[data-action="remove-question-0"]').trigger('click')
    expect(wrapper.find('textarea[name="question-0"]').exists()).toBe(true)
    expect(wrapper.find('textarea[name="question-1"]').exists()).toBe(false)
  })

  it('blocks submission when a question entry is empty', async () => {
    const wrapper = mountForm()
    await wrapper.get('input[name="roundName"]').setValue('一面')
    const selects = wrapper.findAllComponents({ name: 'ElSelect' })
    selects[0]!.vm.$emit('update:modelValue', 'VIDEO')
    await wrapper.get('button[data-action="add-question"]').trigger('click')

    await wrapper.get('form').trigger('submit.prevent')

    expect(wrapper.get('[data-field-error="question-0"]').text()).toBe('问题内容不能为空')
    expect(wrapper.emitted('submitted')).toBeUndefined()
  })

  it('emits the round payload with questions sorted by input order', async () => {
    const wrapper = mountForm()
    await wrapper.get('input[name="roundName"]').setValue('一面')
    const selects = wrapper.findAllComponents({ name: 'ElSelect' })
    selects[0]!.vm.$emit('update:modelValue', 'VIDEO')
    await wrapper.get('button[data-action="add-question"]').trigger('click')
    await wrapper.get('textarea[name="question-0"]').setValue('介绍一个项目')
    await wrapper.get('textarea[name="answer-0"]').setValue('回答要点')
    await wrapper.get('input[name="category-0"]').setValue('项目')

    await wrapper.get('form').trigger('submit.prevent')

    const payload = wrapper.emitted('submitted')?.[0]?.[0] as Record<string, unknown>
    expect(payload).toMatchObject({
      roundName: '一面',
      roundNumber: 1,
      interviewType: 'VIDEO',
      result: 'UPCOMING',
      version: null,
    })
    expect(payload.questions).toEqual([
      { question: '介绍一个项目', answer: '回答要点', category: '项目' },
    ])
  })
})
