<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'

const props = withDefaults(defineProps<{
  modelValue?: string | null
  id?: string
  placeholder?: string
}>(), {
  modelValue: null,
  id: undefined,
  placeholder: '输入内容…',
})

const emit = defineEmits<{ 'update:modelValue': [value: string | null] }>()

const editor = ref<HTMLElement | null>(null)

function syncFromModel() {
  const element = editor.value
  if (!element) return
  const next = props.modelValue ?? ''
  // 仅在内容确实不同时写回 DOM：输入过程中 emit 的值会原样回传，避免光标跳动
  if (element.innerHTML !== next) element.innerHTML = next
}

/** 外部值变化时同步到编辑区（输入过程中由 input 事件主导）。 */
watch(() => props.modelValue, () => syncFromModel())
onMounted(() => syncFromModel())

function emitChange() {
  const element = editor.value
  if (!element) return
  emit('update:modelValue', (element.textContent ?? '').trim() === '' ? null : element.innerHTML)
}

function runCommand(command: string, value?: string) {
  const element = editor.value
  if (!element) return
  element.focus()
  document.execCommand(command, false, value)
  emitChange()
}

function addLink() {
  if (typeof window.prompt !== 'function') return
  const url = window.prompt('输入链接地址（仅支持 http / https / mailto）')
  if (!url) return
  const trimmed = url.trim()
  if (!/^(https?:\/\/|mailto:)/i.test(trimmed)) return
  runCommand('createLink', trimmed)
}

/** 粘贴统一按纯文本插入，阻止富文本粘贴带入危险标签；服务端白名单清洗仍是最终防线。 */
function onPaste(event: ClipboardEvent) {
  event.preventDefault()
  const text = event.clipboardData?.getData('text/plain') ?? ''
  if (text) {
    document.execCommand('insertText', false, text)
    emitChange()
  }
}
</script>

<template>
  <div class="rich-editor">
    <div class="rich-toolbar" role="toolbar" aria-label="富文本格式">
      <button type="button" data-action="rich-bold" title="加粗" aria-label="加粗" @mousedown.prevent @click="runCommand('bold')"><strong>B</strong></button>
      <button type="button" data-action="rich-italic" title="斜体" aria-label="斜体" @mousedown.prevent @click="runCommand('italic')"><em>I</em></button>
      <button type="button" data-action="rich-underline" title="下划线" aria-label="下划线" @mousedown.prevent @click="runCommand('underline')"><u>U</u></button>
      <button type="button" data-action="rich-ul" title="无序列表" aria-label="无序列表" @mousedown.prevent @click="runCommand('insertUnorderedList')">•≡</button>
      <button type="button" data-action="rich-ol" title="有序列表" aria-label="有序列表" @mousedown.prevent @click="runCommand('insertOrderedList')">1≡</button>
      <button type="button" data-action="rich-link" title="插入链接" aria-label="插入链接" @mousedown.prevent @click="addLink">🔗</button>
      <button type="button" data-action="rich-clear" title="清除格式" aria-label="清除格式" @mousedown.prevent @click="runCommand('removeFormat')">✕</button>
    </div>
    <div
      :id="id"
      ref="editor"
      class="rich-content"
      contenteditable="true"
      role="textbox"
      aria-multiline="true"
      :aria-label="placeholder"
      :data-placeholder="placeholder"
      @input="emitChange"
      @paste="onPaste"
    />
  </div>
</template>

<style scoped>
.rich-editor {
  border: 1px solid var(--ir-border);
  border-radius: var(--ir-radius-sm);
  background: var(--ir-surface);
  overflow: hidden;
  transition: box-shadow var(--ir-transition), border-color var(--ir-transition);
}
.rich-editor:focus-within {
  border-color: var(--ir-primary-strong);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ir-primary-strong), transparent 86%);
}
.rich-toolbar {
  display: flex;
  gap: 2px;
  padding: 4px 6px;
  border-bottom: 1px solid var(--ir-border);
  background: var(--ir-surface-muted);
}
.rich-toolbar button {
  display: inline-grid;
  place-items: center;
  width: 28px;
  height: 26px;
  border: 0;
  border-radius: var(--ir-radius-xs);
  background: transparent;
  color: var(--ir-muted);
  font-size: 12.5px;
  font-weight: 700;
  transition: background-color var(--ir-transition), color var(--ir-transition);
}
.rich-toolbar button:hover {
  background: var(--ir-surface);
  color: var(--ir-primary-strong);
}
.rich-content {
  min-height: 96px;
  max-height: 340px;
  overflow-y: auto;
  padding: 10px 12px;
  outline: none;
  line-height: 1.7;
  word-break: break-word;
}
.rich-content:empty::before {
  content: attr(data-placeholder);
  color: var(--ir-faint);
  pointer-events: none;
}
</style>
