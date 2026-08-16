import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import PreferenceForm from './PreferenceForm.vue'

describe('PreferenceForm', () => {
  it('submits unique reminder offsets in descending order', async () => {
    const wrapper = mount(PreferenceForm, {
      props: { preferences: { displayName: '小林', timeZone: 'Asia/Shanghai', theme: 'GRAPHITE_CORAL', interviewReminderOffsets: [30], deadlineReminderOffsets: [60] } },
      global: { plugins: [ElementPlus] },
    })

    await wrapper.get('input[name="interviewReminderOffsets"]').setValue('30, 1440, 30')
    await wrapper.get('form').trigger('submit.prevent')

    expect(wrapper.emitted('submitted')?.[0]).toEqual([{ displayName: '小林', timeZone: 'Asia/Shanghai', theme: 'GRAPHITE_CORAL', darkMode: false, interviewReminderOffsets: [1440, 30], deadlineReminderOffsets: [60] }])
  })

  it('associates reminder validation errors with their input controls', async () => {
    const wrapper = mount(PreferenceForm, {
      props: { preferences: { displayName: '小林', timeZone: 'Asia/Shanghai', theme: 'GRAPHITE_CORAL', interviewReminderOffsets: [30], deadlineReminderOffsets: [60] } },
      global: { plugins: [ElementPlus] },
    })

    await wrapper.get('input[name="interviewReminderOffsets"]').setValue('10081')
    await wrapper.get('form').trigger('submit.prevent')

    expect(wrapper.get('[data-field-error="interviewReminderOffsets"]').attributes('id')).toBe('preference-interview-reminder-offsets-error')
    expect(wrapper.get('input[name="interviewReminderOffsets"]').attributes('aria-describedby')).toBe('preference-interview-reminder-offsets-error')
  })
})
