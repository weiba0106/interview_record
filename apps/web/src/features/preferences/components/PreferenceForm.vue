<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElButton, ElInput, ElOption, ElSelect } from 'element-plus'
import type { Preferences } from '../api/preferences.api'

const props = defineProps<{ preferences: Preferences }>()
const emit = defineEmits<{ submitted: [preferences: Preferences] }>()
const fieldErrors = ref<Record<string, string>>({})
const form = reactive({
  displayName: props.preferences.displayName,
  timeZone: props.preferences.timeZone,
  theme: props.preferences.theme,
  interviewReminderOffsets: props.preferences.interviewReminderOffsets.join(', '),
  deadlineReminderOffsets: props.preferences.deadlineReminderOffsets.join(', '),
})

watch(() => props.preferences, (preferences) => {
  form.displayName = preferences.displayName
  form.timeZone = preferences.timeZone
  form.theme = preferences.theme
  form.interviewReminderOffsets = preferences.interviewReminderOffsets.join(', ')
  form.deadlineReminderOffsets = preferences.deadlineReminderOffsets.join(', ')
}, { deep: true })

function parseOffsets(value: string, field: string): number[] | undefined {
  if (!value.trim()) return []
  const offsets = value.split(',').map((part) => Number(part.trim()))
  if (offsets.some((offset) => !Number.isInteger(offset) || offset < 0 || offset > 10080)) {
    fieldErrors.value[field] = '请输入 0 到 10080 之间的整数分钟数'
    return undefined
  }
  return [...new Set(offsets)].sort((left, right) => right - left)
}

function submit() {
  fieldErrors.value = {}
  const interviewReminderOffsets = parseOffsets(form.interviewReminderOffsets, 'interviewReminderOffsets')
  const deadlineReminderOffsets = parseOffsets(form.deadlineReminderOffsets, 'deadlineReminderOffsets')
  if (!form.displayName.trim()) fieldErrors.value.displayName = '显示名称不能为空'
  if (!form.timeZone.trim()) fieldErrors.value.timeZone = '时区不能为空'
  if (!interviewReminderOffsets || !deadlineReminderOffsets || Object.keys(fieldErrors.value).length > 0) return
  emit('submitted', {
    displayName: form.displayName.trim(), timeZone: form.timeZone.trim(), theme: form.theme,
    interviewReminderOffsets, deadlineReminderOffsets,
  })
}
</script>

<template>
  <form novalidate @submit.prevent="submit">
    <label for="preference-display-name">显示名称</label>
    <ElInput id="preference-display-name" v-model="form.displayName" name="displayName" autocomplete="name" :aria-describedby="fieldErrors.displayName ? 'preference-display-name-error' : undefined" />
    <p v-if="fieldErrors.displayName" id="preference-display-name-error" data-field-error="displayName" role="alert">{{ fieldErrors.displayName }}</p>

    <label for="preference-time-zone">时区（IANA，例如 Asia/Shanghai）</label>
    <ElInput id="preference-time-zone" v-model="form.timeZone" name="timeZone" :aria-describedby="fieldErrors.timeZone ? 'preference-time-zone-error' : undefined" />
    <p v-if="fieldErrors.timeZone" id="preference-time-zone-error" data-field-error="timeZone" role="alert">{{ fieldErrors.timeZone }}</p>

    <label for="preference-theme">主题</label>
    <ElSelect id="preference-theme" v-model="form.theme" name="theme">
      <ElOption label="原始靛蓝" value="INDIGO" />
      <ElOption label="森林青绿" value="FOREST_TEAL" />
      <ElOption label="暖杏棕" value="WARM_APRICOT" />
      <ElOption label="石墨珊瑚" value="GRAPHITE_CORAL" />
    </ElSelect>

    <label for="interview-reminder-offsets">面试默认提醒（分钟，逗号分隔）</label>
    <ElInput id="interview-reminder-offsets" v-model="form.interviewReminderOffsets" name="interviewReminderOffsets" :aria-describedby="fieldErrors.interviewReminderOffsets ? 'preference-interview-reminder-offsets-error' : undefined" />
    <p v-if="fieldErrors.interviewReminderOffsets" id="preference-interview-reminder-offsets-error" data-field-error="interviewReminderOffsets" role="alert">{{ fieldErrors.interviewReminderOffsets }}</p>

    <label for="deadline-reminder-offsets">截止日期默认提醒（分钟，逗号分隔）</label>
    <ElInput id="deadline-reminder-offsets" v-model="form.deadlineReminderOffsets" name="deadlineReminderOffsets" :aria-describedby="fieldErrors.deadlineReminderOffsets ? 'preference-deadline-reminder-offsets-error' : undefined" />
    <p v-if="fieldErrors.deadlineReminderOffsets" id="preference-deadline-reminder-offsets-error" data-field-error="deadlineReminderOffsets" role="alert">{{ fieldErrors.deadlineReminderOffsets }}</p>

    <ElButton native-type="submit" type="primary">保存偏好</ElButton>
  </form>
</template>
