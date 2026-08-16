import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import RichTextEditor from './RichTextEditor.vue'

describe('RichTextEditor', () => {
  const execCommand = vi.fn<(commandId: string, showUI?: boolean, value?: string) => boolean>()

  beforeEach(() => {
    // jsdom 未实现 execCommand；这里只断言命令与参数
    Object.defineProperty(document, 'execCommand', { value: execCommand, configurable: true, writable: true })
  })

  afterEach(() => {
    execCommand.mockReset()
    vi.unstubAllGlobals()
  })

  function setContent(wrapper: ReturnType<typeof mount>, html: string) {
    const element = wrapper.get('.rich-content').element
    element.innerHTML = html
    element.dispatchEvent(new Event('input'))
  }

  it('renders the initial HTML content', () => {
    const wrapper = mount(RichTextEditor, { props: { modelValue: '<p>已有<b>内容</b></p>' } })

    expect(wrapper.get('.rich-content').html()).toContain('<p>已有<b>内容</b></p>')
  })

  it('emits null when the content becomes empty text', async () => {
    const wrapper = mount(RichTextEditor, { props: { modelValue: '<p>旧内容</p>' } })

    setContent(wrapper, '')
    await wrapper.vm.$nextTick()

    expect(wrapper.emitted('update:modelValue')?.at(-1)).toEqual([null])
  })

  it('emits the edited HTML on input', async () => {
    const wrapper = mount(RichTextEditor)

    setContent(wrapper, '<p>新内容</p>')
    await wrapper.vm.$nextTick()

    expect(wrapper.emitted('update:modelValue')?.at(-1)).toEqual(['<p>新内容</p>'])
  })

  it('runs formatting commands from the toolbar', async () => {
    const wrapper = mount(RichTextEditor)

    await wrapper.get('button[data-action="rich-bold"]').trigger('click')
    await wrapper.get('button[data-action="rich-ul"]').trigger('click')

    expect(execCommand).toHaveBeenCalledWith('bold', false, undefined)
    expect(execCommand).toHaveBeenCalledWith('insertUnorderedList', false, undefined)
  })

  it('inserts pasted content as plain text instead of HTML', async () => {
    const wrapper = mount(RichTextEditor)
    const event = new Event('paste')
    Object.defineProperty(event, 'clipboardData', {
      value: { getData: () => '<script>alert(1)</script><p>粘贴内容</p>' },
    })

    wrapper.get('.rich-content').element.dispatchEvent(event)
    await wrapper.vm.$nextTick()

    expect(execCommand).toHaveBeenCalledWith('insertText', false, '<script>alert(1)</script><p>粘贴内容</p>')
  })

  it('rejects unsafe link protocols without creating a link', async () => {
    vi.stubGlobal('prompt', vi.fn(() => 'javascript:alert(1)'))
    const wrapper = mount(RichTextEditor)

    await wrapper.get('button[data-action="rich-link"]').trigger('click')

    expect(execCommand).not.toHaveBeenCalledWith('createLink', expect.anything(), expect.anything())
  })

  it('creates a link for http urls', async () => {
    vi.stubGlobal('prompt', vi.fn(() => 'https://example.com'))
    const wrapper = mount(RichTextEditor)

    await wrapper.get('button[data-action="rich-link"]').trigger('click')

    expect(execCommand).toHaveBeenCalledWith('createLink', false, 'https://example.com')
  })
})
