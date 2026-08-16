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
  reminderDisabled: Array.isArray(props.initial?.reminderOffsets) && props.initial.reminderOffsets.length === 0,
  reminderOffsets: props.initial?.reminderOffsets?.length ? props.initial.reminderOffsets.join(', ') : '',
})

/** 返回 undefined=不改动（创建时即默认规则），[]=关闭，否则为自定义分钟列表；校验失败返回 null。 */
function resolveReminderOffsets(): number[] | undefined | null {
  if (form.reminderDisabled) return []
  const raw = form.reminderOffsets.trim()
  if (!raw) return undefined
  const offsets = raw.split(',').map((part) => Number(part.trim()))
  if (offsets.some((offset) => !Number.isInteger(offset) || offset < 0 || offset > 10080)) {
    fieldErrors.value.reminderOffsets = '提醒时间必须是 0 到 10080 之间的整数分钟'
    return null
  }
  const unique = [...new Set(offsets)]
  if (unique.length > 5) {
    fieldErrors.value.reminderOffsets = '单条日程最多设置 5 个提醒时间'
    return null
  }
  return unique
}

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
  const reminderOffsets = resolveReminderOffsets()
  if (reminderOffsets === null || Object.keys(fieldErrors.value).length > 0) return
  emit('submitted', {
    title,
    eventType: form.eventType,
    startsAt,
    endsAt,
    positionId: form.positionId || null,
    location: form.location.trim() || null,
    notes: form.notes.trim() || null,
    version: props.initial?.version ?? null,
    // 仅在用户显式关闭或填写自定义时间时携带，避免编辑其他字段时覆盖现有提醒配置
    ...(reminderOffsets !== undefined ? { reminderOffsets } : {}),
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

    <label class="reminder-toggle"><input v-model="form.reminderDisabled" name="reminderDisabled" type="checkbox" /> 关闭这条日程的邮件提醒</label>
    <label for="schedule-reminder-offsets">提醒时间（可选，逗号分隔的分钟数）</label>
    <ElInput
      id="schedule-reminder-offsets"
      v-model="form.reminderOffsets"
      name="reminderOffsets"
      placeholder="例如 1440, 30 表示提前 1 天和 30 分钟提醒"
      :disabled="form.reminderDisabled"
      :aria-describedby="fieldErrors.reminderOffsets ? 'schedule-reminder-offsets-error' : undefined"
    />
    <p v-if="fieldErrors.reminderOffsets" id="schedule-reminder-offsets-error" data-field-error="reminderOffsets" role="alert">{{ fieldErrors.reminderOffsets }}</p>
    <p class="reminder-hint">留空使用偏好默认规则；编辑已有日程时留空会保留当前提醒设置。</p>

    <ElButton native-type="submit" type="primary" data-action="submit-schedule">{{ submitLabel ?? '保存日程' }}</ElButton>
  </form>
</template>

<style scoped>
.schedule-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.reminder-toggle {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
  cursor: pointer;
}
.reminder-hint {
  margin: -2px 0 0;
  color: var(--ir-muted);
  font-size: 12px;
  line-height: 1.6;
}
</style>
