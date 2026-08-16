import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import PositionForm from './PositionForm.vue'
import type { Company, JobType, PositionStatus } from '../api/tracking.types'

const companies: Company[] = [
  { id: '11', name: '示例科技', website: null, notes: null, positionCount: 0, createdAt: '2026-08-01T00:00:00Z', updatedAt: '2026-08-01T00:00:00Z' },
]
const jobTypes: JobType[] = [{ id: '21', name: '秋招', active: true }]
const statuses: PositionStatus[] = [
  { id: '31', name: '投递中', color: '#46a758', statisticsCategory: 'ACTIVE', sortOrder: 0, active: true, positionCount: 0 },
]

function mountForm() {
  return mount(PositionForm, {
    props: { companies, jobTypes, statuses },
    global: { plugins: [ElementPlus] },
  })
}

describe('PositionForm', () => {
  it('shows field errors next to the required fields when submitting empty', async () => {
    const wrapper = mountForm()

    await wrapper.get('form').trigger('submit.prevent')

    expect(wrapper.get('[data-field-error="companyId"]').text()).toBe('请选择所属公司')
    expect(wrapper.get('[data-field-error="title"]').text()).toBe('职位名称不能为空')
    expect(wrapper.get('[data-field-error="jobTypeId"]').text()).toBe('请选择招聘类型')
    expect(wrapper.emitted('submitted')).toBeUndefined()
  })

  it('rejects apply links without a safe protocol', async () => {
    const wrapper = mountForm()
    const selects = wrapper.findAllComponents({ name: 'ElSelect' })
    selects[0]!.vm.$emit('update:modelValue', '11')
    selects[1]!.vm.$emit('update:modelValue', '21')
    await wrapper.get('input[name="title"]').setValue('后端开发工程师')
    await wrapper.get('input[name="applyUrl"]').setValue('javascript:alert(1)')

    await wrapper.get('form').trigger('submit.prevent')

    expect(wrapper.get('[data-field-error="applyUrl"]').text()).toContain('http')
    expect(wrapper.emitted('submitted')).toBeUndefined()
  })

  it('emits the position payload with the default active status and null version', async () => {
    const wrapper = mountForm()
    const selects = wrapper.findAllComponents({ name: 'ElSelect' })
    selects[0]!.vm.$emit('update:modelValue', '11')
    selects[1]!.vm.$emit('update:modelValue', '21')
    await wrapper.get('input[name="title"]').setValue('后端开发工程师')
    await wrapper.get('input[name="appliedAt"]').setValue('2026-08-01')

    await wrapper.get('form').trigger('submit.prevent')

    const payload = wrapper.emitted('submitted')?.[0]?.[0] as Record<string, unknown>
    expect(payload).toMatchObject({
      companyId: '11',
      jobTypeId: '21',
      statusId: '31',
      title: '后端开发工程师',
      version: null,
    })
    expect(payload.appliedAt).toBe(new Date('2026-08-01T00:00:00').toISOString())
  })

  it('switches to quick-create company mode and emits newCompanyName instead of companyId', async () => {
    const wrapper = mount(PositionForm, {
      props: { companies: [], jobTypes, statuses },
      global: { plugins: [ElementPlus] },
    })
    // 无公司时默认直接进入快速新建模式
    await wrapper.get('input[name="newCompanyName"]').setValue('新力科技')
    const selects = wrapper.findAllComponents({ name: 'ElSelect' })
    selects[0]!.vm.$emit('update:modelValue', '21')

    await wrapper.get('input[name="title"]').setValue('后端开发工程师')
    await wrapper.get('form').trigger('submit.prevent')

    const payload = wrapper.emitted('submitted')?.[0]?.[0] as Record<string, unknown>
    expect(payload.companyId).toBeNull()
    expect(payload.newCompanyName).toBe('新力科技')
  })

  it('shows a field error when the quick-create company name is empty', async () => {
    const wrapper = mount(PositionForm, {
      props: { companies: [], jobTypes, statuses },
      global: { plugins: [ElementPlus] },
    })

    await wrapper.get('form').trigger('submit.prevent')

    expect(wrapper.get('[data-field-error="newCompanyName"]').text()).toBe('请输入新公司名称')
    expect(wrapper.emitted('submitted')).toBeUndefined()
  })

  it('toggles back to selecting an existing company', async () => {
    const wrapper = mountForm()
    await wrapper.get('button[data-action="toggle-new-company"]').trigger('click')
    expect(wrapper.find('input[name="newCompanyName"]').exists()).toBe(true)

    await wrapper.get('button[data-action="toggle-new-company"]').trigger('click')
    expect(wrapper.find('input[name="newCompanyName"]').exists()).toBe(false)
    expect(wrapper.findAllComponents({ name: 'ElSelect' }).length).toBeGreaterThanOrEqual(3)
  })
})
