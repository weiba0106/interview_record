<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElButton, ElInput, ElOption, ElSelect } from 'element-plus'
import { SCHEDULE_EVENT_TYPES, type Schedule, type ScheduleRequest } from '../api/schedules.api'
import type { PositionSummary } from '@/features/tracking/api/tracking.types'
import { fromDatetimeInput, toDatetimeInput } from '@/shared/format/datetime'

const props = defineProps<{
  initial?: Schedule | null
  positions: PositionSummary[]
  submitLabel?: string
}>()
const emit = defineEmits<{ submitted: [payload: ScheduleRequest] }>()
const fieldErrors = ref<Record<string, string>>({})
const form = reactive({
  title: props.initial?.title ?? '',
  eventType: props.initial?.eventType ?? '',
  startsAt: toDatetimeInput(props.initial?.startsAt),
  endsAt: toDatetimeInput(props.initial?.endsAt),
  positionId: props.initial?.positionId ?? '',
  location: props.initial?.location ?? '',
  notes: props.initial?.notes ?? '',
})

function submit() {
  fieldErrors.value = {}
  const title = form.title.trim()
  if (!title) fieldErrors.value.title = '标题不能为空'
  else if (title.length > 120) fieldErrors.value.title = '标题不能超过 120 个字符'
  if (!form.eventType) fieldErrors.value.eventType = '请选择日程类型'
  const startsAt = fromDatetimeInput(form.startsAt)
  const endsAt = fromDatetimeInput(form.endsAt)
  if (!startsAt && !endsAt) fieldErrors.value.startsAt = '请至少填写开始时间或截止时间'
  if (startsAt && endsAt && endsAt <= startsAt) fieldErrors.value.endsAt = '结束时间必须晚于开始时间'
  if (form.notes.length > 2000) fieldErrors.value.notes = '备注不能超过 2000 个字符'
  if (Object.keys(fieldErrors.value).length > 0) return
  emit('submitted', {
    title,
    eventType: form.eventType,
    startsAt,
    endsAt,
    positionId: form.positionId || null,
    location: form.location.trim() || null,
    notes: form.notes.trim() || null,
    version: props.initial?.version ?? null,
  })
}
</script>

<template>
  <form novalidate @submit.prevent="submit" class="schedule-form">
    <label for="schedule-title">标题 *</label>
    <ElInput id="schedule-title" v-model="form.title" name="title" maxlength="120" :aria-describedby="fieldErrors.title ? 'schedule-title-error' : undefined" />
    <p v-if="fieldErrors.title" id="schedule-title-error" data-field-error="title" role="alert">{{ fieldErrors.title }}</p>

    <label for="schedule-event-type">日程类型 *</label>
    <ElSelect id="schedule-event-type" v-model="form.eventType" name="eventType" placeholder="选择日程类型" :aria-describedby="fieldErrors.eventType ? 'schedule-event-type-error' : undefined">
      <ElOption v-for="item in SCHEDULE_EVENT_TYPES" :key="item.value" :label="item.label" :value="item.value" />
    </ElSelect>
    <p v-if="fieldErrors.eventType" id="schedule-event-type-error" data-field-error="eventType" role="alert">{{ fieldErrors.eventType }}</p>

    <label for="schedule-starts-at">开始时间</label>
    <input id="schedule-starts-at" v-model="form.startsAt" name="startsAt" type="datetime-local" :aria-describedby="fieldErrors.startsAt ? 'schedule-starts-at-error' : undefined" />
    <p v-if="fieldErrors.startsAt" id="schedule-starts-at-error" data-field-error="startsAt" role="alert">{{ fieldErrors.startsAt }}</p>

    <label for="schedule-ends-at">结束时间或截止时间</label>
    <input id="schedule-ends-at" v-model="form.endsAt" name="endsAt" type="datetime-local" :aria-describedby="fieldErrors.endsAt ? 'schedule-ends-at-error' : undefined" />
    <p v-if="fieldErrors.endsAt" id="schedule-ends-at-error" data-field-error="endsAt" role="alert">{{ fieldErrors.endsAt }}</p>

    <label for="schedule-position">关联岗位（可选）</label>
    <ElSelect id="schedule-position" v-model="form.positionId" name="positionId" filterable clearable placeholder="不关联">
      <ElOption v-for="position in positions" :key="position.id" :label="`${position.companyName} · ${position.title}`" :value="position.id" />
    </ElSelect>

    <label for="schedule-location">地点或链接（可选）</label>
    <ElInput id="schedule-location" v-model="form.location" name="location" maxlength="500" />

    <label for="schedule-notes">备注（可选）</label>
    <ElInput id="schedule-notes" v-model="form.notes" name="notes" type="textarea" :rows="3" maxlength="2000" :aria-describedby="fieldErrors.notes ? 'schedule-notes-error' : undefined" />
    <p v-if="fieldErrors.notes" id="schedule-notes-error" data-field-error="notes" role="alert">{{ fieldErrors.notes }}</p>

    <ElButton native-type="submit" type="primary" data-action="submit-schedule">{{ submitLabel ?? '保存日程' }}</ElButton>
  </form>
</template>

<style scoped>
.schedule-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
</style>
