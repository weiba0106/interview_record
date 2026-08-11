import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import SettingsView from './SettingsView.vue'

describe('SettingsView', () => {
  it('requires password re-entry before exposing destructive account deletion', async () => {
    const wrapper = mount(SettingsView, { global: { plugins: [createPinia(), ElementPlus] } })

    await wrapper.get('button[data-action="open-delete-dialog"]').trigger('click')
    expect(wrapper.get('button[data-action="delete-account"]').attributes('disabled')).toBeDefined()
    await wrapper.get('input[name="deletePassword"]').setValue('Password123')
    expect(wrapper.get('button[data-action="delete-account"]').attributes('disabled')).toBeUndefined()
  })
})
