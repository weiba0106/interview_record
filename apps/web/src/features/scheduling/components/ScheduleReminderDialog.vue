<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElButton, ElDialog, ElInput } from 'element-plus'
import { updateScheduleReminders, type Schedule } from '../api/schedules.api'
import { isApiRequestError } from '@/shared/api/error'

const props = defineProps<{
  modelValue: boolean
  schedule: Schedule | null
}>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; updated: [schedule: Schedule] }>()

const reminderMode = ref<'AUTO' | 'DISABLED' | 'CUSTOM'>('AUTO')
const reminderInput = ref('')
const reminderError = ref('')
const reminderSaving = ref(false)

watch(() => props.schedule, (schedule) => {
  reminderError.value = ''
  const offsets = schedule?.reminderOffsets
  if (offsets === null || offsets === undefined) {
    reminderMode.value = 'AUTO'
    reminderInput.value = ''
  } else if (offsets.length === 0) {
    reminderMode.value = 'DISABLED'
    reminderInput.value = ''
  } else {
    reminderMode.value = 'CUSTOM'
    reminderInput.value = offsets.join(', ')
  }
}, { immediate: true })

function close() {
  reminderError.value = ''
  emit('update:modelValue', false)
}

async function save() {
  if (!props.schedule || reminderSaving.value) return
  reminderError.value = ''
  let offsets: number[] | null
  if (reminderMode.value === 'AUTO') {
    offsets = null
  } else if (reminderMode.value === 'DISABLED') {
    offsets = []
  } else {
    const parsed = reminderInput.value.split(',').map((part) => Number(part.trim()))
    if (parsed.some((offset) => !Number.isInteger(offset) || offset < 0 || offset > 10080)) {
      reminderError.value = '提醒时间必须是 0 到 10080 之间的整数分钟'
      return
    }
    if (new Set(parsed).size > 5) {
      reminderError.value = '单条日程最多设置 5 个提醒时间'
      return
    }
    offsets = [...new Set(parsed)]
  }
  reminderSaving.value = true
  try {
    const updated = await updateScheduleReminders(props.schedule.id, offsets)
    emit('updated', updated)
    emit('update:modelValue', false)
  } catch (caught) {
    reminderError.value = isApiRequestError(caught) ? caught.apiError.message : '保存提醒设置失败，请稍后重试'
  } finally {
    reminderSaving.value = false
  }
}
</script>

<template>
  <ElDialog :model-value="modelValue" :title="`提醒设置 · ${schedule?.title ?? ''}`" width="min(94vw, 460px)" :teleported="false" @close="close" @update:model-value="(value: boolean) => !value && close()">
    <div class="reminder-dialog" role="radiogroup" aria-label="提醒方式">
      <label class="reminder-option">
        <input v-model="reminderMode" type="radio" name="reminderMode" value="AUTO" />
        <span><strong>使用默认规则</strong><small>按偏好设置自动提醒</small></span>
      </label>
      <label class="reminder-option">
        <input v-model="reminderMode" type="radio" name="reminderMode" value="CUSTOM" />
        <span><strong>自定义提醒时间</strong><small>提前多少分钟提醒，多个时间用逗号分隔</small></span>
      </label>
      <ElInput
        v-if="reminderMode === 'CUSTOM'"
        v-model="reminderInput"
        name="reminderOffsets"
        placeholder="例如 1440, 30"
        aria-label="自定义提醒时间（分钟，逗号分隔）"
      />
      <label class="reminder-option">
        <input v-model="reminderMode" type="radio" name="reminderMode" value="DISABLED" />
        <span><strong>关闭提醒</strong><small>这条日程不再发送邮件</small></span>
      </label>
    </div>
    <p v-if="reminderError" role="alert">{{ reminderError }}</p>
    <template #footer>
      <ElButton @click="close">取消</ElButton>
      <ElButton type="primary" data-action="save-reminders" :loading="reminderSaving" @click="save">保存提醒设置</ElButton>
    </template>
  </ElDialog>
</template>

<style scoped>
.reminder-dialog { display: grid; gap: 10px; }
.reminder-option {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid var(--ir-border);
  border-radius: var(--ir-radius-sm);
  cursor: pointer;
  transition: border-color var(--ir-transition), background-color var(--ir-transition);
}
.reminder-option:hover { border-color: var(--ir-border-strong); }
.reminder-option:has(input:checked) {
  border-color: var(--ir-primary-strong);
  background: var(--ir-primary-soft);
}
.reminder-option input { margin-top: 3px; }
.reminder-option strong, .reminder-option small { display: block; }
.reminder-option strong { font-size: 13px; }
.reminder-option small { color: var(--ir-muted); font-size: 11.5px; margin-top: 2px; }
</style>
