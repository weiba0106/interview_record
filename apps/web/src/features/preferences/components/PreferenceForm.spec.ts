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

    expect(wrapper.emitted('submitted')?.[0]).toEqual([{ displayName: '小林', timeZone: 'Asia/Shanghai', theme: 'GRAPHITE_CORAL', interviewReminderOffsets: [1440, 30], deadlineReminderOffsets: [60] }])
  })
})
