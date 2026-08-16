import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { effectScope, reactive } from 'vue'
import { useFormDraft } from './useFormDraft'

describe('useFormDraft', () => {
  beforeEach(() => {
    sessionStorage.clear()
    vi.useFakeTimers()
  })
  afterEach(() => {
    vi.useRealTimers()
  })

  function setup(key: string, initial: Record<string, string> = {}) {
    const scope = effectScope()
    const form = reactive({ title: initial.title ?? '', notes: initial.notes ?? '' })
    const draft = scope.run(() => useFormDraft(key))!
    const saved = draft.restore()
    if (typeof saved.title === 'string') form.title = saved.title
    if (typeof saved.notes === 'string') form.notes = saved.notes
    draft.startWatching(() => ({ title: form.title, notes: form.notes }))
    return { scope, form, draft, saved }
  }

  it('saves edits after the debounce window and restores them on a new instance', async () => {
    const first = setup('test-form')
    first.form.title = '未提交的岗位'
    first.form.notes = '备注草稿'
    await vi.advanceTimersByTimeAsync(350)

    expect(JSON.parse(sessionStorage.getItem('interview-record.draft.test-form')!)).toMatchObject({
      title: '未提交的岗位',
      notes: '备注草稿',
    })

    first.scope.stop()
    const second = setup('test-form')
    expect(second.saved).toMatchObject({ title: '未提交的岗位', notes: '备注草稿' })
    expect(second.form.title).toBe('未提交的岗位')
    second.scope.stop()
  })

  it('keeps the final keystroke when flushed before unmount', async () => {
    const first = setup('test-form')
    first.form.title = '最后几个字'
    // 先让 Vue 调度器执行 watcher 建立防抖定时器，再在窗口内 flush（组件卸载兜底同路径）
    await vi.advanceTimersByTimeAsync(0)
    first.draft.flush()

    expect(JSON.parse(sessionStorage.getItem('interview-record.draft.test-form')!)).toMatchObject({
      title: '最后几个字',
    })
    first.scope.stop()
  })

  it('clear removes the draft and suppresses later fallback writes', async () => {
    const first = setup('test-form')
    first.form.title = '已提交的内容'
    await vi.advanceTimersByTimeAsync(350)
    expect(sessionStorage.getItem('interview-record.draft.test-form')).not.toBeNull()

    first.draft.clear()
    expect(sessionStorage.getItem('interview-record.draft.test-form')).toBeNull()

    first.form.title = '提交后不再保存'
    first.draft.flush()
    expect(sessionStorage.getItem('interview-record.draft.test-form')).toBeNull()
    first.scope.stop()
  })

  it('ignores corrupted session data instead of throwing', () => {
    sessionStorage.setItem('interview-record.draft.test-form', '{broken json')
    const { scope, saved } = setup('test-form')
    expect(saved).toEqual({})
    scope.stop()
  })

  it('keeps multiple drafts isolated by key', async () => {
    const a = setup('form-a')
    const b = setup('form-b')
    a.form.title = '表单 A'
    b.form.title = '表单 B'
    await vi.advanceTimersByTimeAsync(350)

    expect(JSON.parse(sessionStorage.getItem('interview-record.draft.form-a')!)).toMatchObject({ title: '表单 A' })
    expect(JSON.parse(sessionStorage.getItem('interview-record.draft.form-b')!)).toMatchObject({ title: '表单 B' })
    a.scope.stop()
    b.scope.stop()
  })

  it('does not write storage while the form stays empty', async () => {
    const first = setup('test-form')
    await vi.advanceTimersByTimeAsync(1000)
    expect(sessionStorage.getItem('interview-record.draft.test-form')).toBeNull()
    first.scope.stop()
  })
})
