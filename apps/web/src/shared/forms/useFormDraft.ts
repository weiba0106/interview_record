import { onBeforeUnmount, watch } from 'vue'

/**
 * PRD §12：网络中断时表单未提交内容保留到当前浏览器会话，失败后可直接重试。
 * 用法：
 *   const draft = useFormDraft('position-form')
 *   Object.assign(form, draft.restore())          // 挂载时恢复
 *   draft.startWatching(() => ({ 字段们 }))        // 防抖保存
 *   defineExpose({ clearDraft: draft.clear })      // 提交成功后由父组件清除
 */
export function useFormDraft(key: string) {
  const storageKey = `interview-record.draft.${key}`
  let timer: ReturnType<typeof setTimeout> | undefined
  let getter: (() => Record<string, unknown>) | undefined
  let cleared = false

  function restore(): Record<string, unknown> {
    try {
      const raw = sessionStorage.getItem(storageKey)
      if (!raw) return {}
      return JSON.parse(raw) as Record<string, unknown>
    } catch { return {} }
  }

  function write(value: Record<string, unknown>) {
    if (cleared) return
    try { sessionStorage.setItem(storageKey, JSON.stringify(value)) } catch { /* 会话存储不可用时降级 */ }
  }

  function startWatching(value: () => Record<string, unknown>) {
    getter = value
    watch(value, (next) => {
      cleared = false
      if (timer) clearTimeout(timer)
      timer = setTimeout(() => {
        timer = undefined
        write(next)
      }, 300)
    }, { deep: true })
  }

  /** 提交成功后清除草稿；此后组件卸载时的兜底写入会被跳过。 */
  function clear() {
    cleared = true
    if (timer) { clearTimeout(timer); timer = undefined }
    try { sessionStorage.removeItem(storageKey) } catch { /* 会话存储不可用时降级 */ }
  }

  /** 立即把当前输入落盘（组件卸载兜底与测试使用）。 */
  function flush() {
    if (timer) {
      clearTimeout(timer)
      timer = undefined
      if (getter) write(getter())
    }
  }

  // 卸载时把防抖窗口内尚未落盘的输入立即保存，避免关闭弹窗丢失最后几笔输入
  onBeforeUnmount(flush)

  return { restore, startWatching, clear, flush }
}
