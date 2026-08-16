<script setup lang="ts">
import { onMounted, ref, useTemplateRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElButton, ElDialog } from 'element-plus'
import ScheduleForm from '@/features/scheduling/components/ScheduleForm.vue'
import ScheduleReminderDialog from '@/features/scheduling/components/ScheduleReminderDialog.vue'
import {
  changeScheduleStatus,
  deleteSchedule,
  getSchedule,
  scheduleEventTypeLabel,
  scheduleStatusLabel,
  updateSchedule,
  type Schedule,
  type ScheduleRequest,
} from '@/features/scheduling/api/schedules.api'
import { urgencyCountdown, urgencyDisplay } from '@/features/scheduling/urgency'
import { searchPositions } from '@/features/tracking/api/positions.api'
import type { PositionSummary } from '@/features/tracking/api/tracking.types'
import { isApiRequestError } from '@/shared/api/error'
import { formatDateTime } from '@/shared/format/datetime'

const route = useRoute()
const router = useRouter()
const scheduleId = typeof route.params.id === 'string' ? route.params.id : ''

const loading = ref(true)
const error = ref('')
const message = ref('')
const schedule = ref<Schedule | null>(null)
const positions = ref<PositionSummary[]>([])
const pendingRequest = ref(false)

const editDialogOpen = ref(false)
const deleteDialogOpen = ref(false)
const reminderDialogOpen = ref(false)
const scheduleFormRef = useTemplateRef<InstanceType<typeof ScheduleForm>>('schedule-form')

async function load() {
  loading.value = true
  error.value = ''
  try {
    schedule.value = await getSchedule(scheduleId)
  } catch (caught) {
    error.value = isApiRequestError(caught) ? caught.apiError.message : '加载日程失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function loadPositions() {
  try {
    const page = await searchPositions({ size: 100 })
    positions.value = page.items
  } catch { /* 岗位选项加载失败不阻塞日程详情 */ }
}

onMounted(() => {
  void load()
  void loadPositions()
})

const failedReminderCount = () =>
  (schedule.value?.reminders ?? []).filter((item) => item.status === 'FAILED').length

function reminderSummary(): string {
  const offsets = schedule.value?.reminderOffsets
  if (offsets === null || offsets === undefined) return '默认规则'
  if (offsets.length === 0) return '已关闭'
  return `提前 ${offsets.join('、')} 分钟`
}

async function setStatus(status: 'PENDING' | 'COMPLETED' | 'CANCELLED') {
  if (!schedule.value || pendingRequest.value) return
  pendingRequest.value = true
  error.value = ''; message.value = ''
  try {
    schedule.value = await changeScheduleStatus(schedule.value.id, status)
    message.value = status === 'COMPLETED' ? '日程已完成' : status === 'CANCELLED' ? '日程已取消' : '日程已恢复为待处理'
  } catch (caught) {
    error.value = isApiRequestError(caught) ? caught.apiError.message : '操作失败，请稍后重试'
  } finally {
    pendingRequest.value = false
  }
}

async function submitEdit(payload: ScheduleRequest) {
  if (!schedule.value) return
  pendingRequest.value = true
  error.value = ''; message.value = ''
  try {
    schedule.value = await updateSchedule(schedule.value.id, payload)
    scheduleFormRef.value?.clearDraft()
    editDialogOpen.value = false
    message.value = '日程已更新'
  } catch (caught) {
    error.value = isApiRequestError(caught) ? caught.apiError.message : '保存失败，请稍后重试'
  } finally {
    pendingRequest.value = false
  }
}

async function confirmDelete() {
  if (!schedule.value) return
  pendingRequest.value = true
  error.value = ''
  try {
    await deleteSchedule(schedule.value.id)
    await router.replace({ name: 'schedules' })
  } catch (caught) {
    error.value = isApiRequestError(caught) ? caught.apiError.message : '删除失败，请稍后重试'
    pendingRequest.value = false
  }
}

function onRemindersUpdated(updated: Schedule) {
  schedule.value = updated
  message.value = '提醒设置已更新'
}
</script>

<template>
  <main class="schedule-detail page-center" aria-labelledby="schedule-detail-heading">
    <p v-if="message" role="status">{{ message }}</p>
    <p v-if="error" role="alert">{{ error }}</p>
    <p v-if="loading" role="status">加载中…</p>

    <template v-if="!loading && schedule">
      <div class="page-head">
        <div>
          <span class="eyebrow">时间提醒</span>
          <h1 id="schedule-detail-heading">{{ schedule.title }}</h1>
          <p class="page-desc">{{ scheduleStatusLabel(schedule.status) }} · {{ scheduleEventTypeLabel(schedule.eventType) }}</p>
        </div>
        <div class="page-head-actions">
          <ElButton v-if="schedule.status === 'PENDING'" data-action="complete-schedule" :loading="pendingRequest" @click="setStatus('COMPLETED')">完成</ElButton>
          <ElButton v-if="schedule.status === 'PENDING'" data-action="cancel-schedule" :loading="pendingRequest" @click="setStatus('CANCELLED')">取消</ElButton>
          <ElButton v-else data-action="reopen-schedule" :loading="pendingRequest" @click="setStatus('PENDING')">恢复待处理</ElButton>
          <ElButton data-action="edit-schedule" @click="editDialogOpen = true">编辑</ElButton>
          <ElButton data-action="reminders-schedule" @click="reminderDialogOpen = true">提醒</ElButton>
          <ElButton text type="danger" data-action="delete-schedule" @click="deleteDialogOpen = true">删除</ElButton>
        </div>
      </div>

      <section class="ir-panel" aria-label="紧急程度">
        <div class="urgency-banner" :class="urgencyDisplay(schedule.urgency).className">
          <span class="urgency-icon" :data-urgency-icon="schedule.urgency" :aria-label="urgencyDisplay(schedule.urgency).label">{{ urgencyDisplay(schedule.urgency).icon }}</span>
          <div>
            <strong>{{ urgencyDisplay(schedule.urgency).label }}</strong>
            <span v-if="schedule.status === 'PENDING'">{{ urgencyCountdown(schedule) }}</span>
            <span v-else>{{ scheduleStatusLabel(schedule.status) }}</span>
            <span v-if="schedule.manualUrgency" class="manual-badge">手动设置</span>
          </div>
        </div>
      </section>

      <section class="ir-panel" aria-labelledby="schedule-info-heading">
        <div class="ir-panel-head"><div><span class="panel-kicker">日程信息</span><h2 id="schedule-info-heading">详情</h2></div></div>
        <div class="info-grid">
          <div class="info-item"><span class="info-label">日程类型</span><span>{{ scheduleEventTypeLabel(schedule.eventType) }}</span></div>
          <div class="info-item"><span class="info-label">开始时间</span><span>{{ formatDateTime(schedule.startsAt) }}</span></div>
          <div class="info-item"><span class="info-label">结束/截止时间</span><span>{{ formatDateTime(schedule.endsAt) }}</span></div>
          <div class="info-item">
            <span class="info-label">关联岗位</span>
            <RouterLink v-if="schedule.positionId" :to="{ name: 'position-detail', params: { id: schedule.positionId } }">{{ schedule.positionTitle || schedule.positionId }}</RouterLink>
            <span v-else>—</span>
          </div>
          <div class="info-item"><span class="info-label">面试轮次</span><span>{{ schedule.interviewRoundId ? `已关联（${schedule.interviewRoundId}）` : '—' }}</span></div>
          <div class="info-item"><span class="info-label">地点/链接</span><span>{{ schedule.location ?? '—' }}</span></div>
          <div class="info-item info-item-wide"><span class="info-label">备注</span><span>{{ schedule.notes ?? '—' }}</span></div>
        </div>
      </section>

      <section class="ir-panel" aria-labelledby="schedule-reminders-heading">
        <div class="ir-panel-head">
          <div><span class="panel-kicker">邮件提醒</span><h2 id="schedule-reminders-heading">提醒配置</h2></div>
          <ElButton size="small" data-action="reminders-schedule-panel" @click="reminderDialogOpen = true">修改提醒</ElButton>
        </div>
        <div class="reminder-panel-body">
          <p class="reminder-summary">当前规则：<strong>{{ reminderSummary() }}</strong></p>
          <p v-if="failedReminderCount() > 0" class="reminder-warning" role="status">
            ⚠ 邮件提醒发送失败（{{ failedReminderCount() }} 条），已停止重试，请检查邮箱后编辑日程重新触发。
          </p>
          <ul v-if="schedule.reminders.length > 0" class="reminder-state-list" aria-label="提醒发送记录">
            <li v-for="reminder in schedule.reminders" :key="`${reminder.scheduledAt}-${reminder.status}`">
              <span>{{ formatDateTime(reminder.scheduledAt) }}</span>
              <span class="reminder-state" :class="`state-${reminder.status.toLowerCase()}`">{{ reminder.status }}</span>
              <span v-if="reminder.sentAt" class="reminder-sent">发送于 {{ formatDateTime(reminder.sentAt) }}</span>
            </li>
          </ul>
          <p v-else class="reminder-none">尚无提醒记录，保存日程后按规则自动生成。</p>
        </div>
      </section>

      <ElDialog v-model="editDialogOpen" title="编辑日程" width="min(94vw, 560px)" top="6vh" :teleported="false">
        <ScheduleForm v-if="schedule" ref="schedule-form" :initial="schedule" :positions="positions" @submitted="submitEdit" />
      </ElDialog>

      <ElDialog v-model="deleteDialogOpen" title="确认删除日程" width="min(92vw, 420px)" :teleported="false">
        <p>删除日程「{{ schedule.title }}」后无法恢复，未发送的提醒也会一并取消。确定吗？</p>
        <template #footer>
          <ElButton @click="deleteDialogOpen = false">取消</ElButton>
          <ElButton type="danger" data-action="confirm-delete-schedule" :loading="pendingRequest" @click="confirmDelete">删除</ElButton>
        </template>
      </ElDialog>

      <ScheduleReminderDialog v-model="reminderDialogOpen" :schedule="schedule" @updated="onRemindersUpdated" />
    </template>
  </main>
</template>

<style scoped>
.schedule-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 920px;
}
.urgency-banner {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 14px 16px;
  padding: 12px 14px;
  border: 1px solid color-mix(in srgb, var(--urgency-color), transparent 70%);
  border-left: 3px solid var(--urgency-color);
  border-radius: var(--ir-radius-md);
  background: color-mix(in srgb, var(--urgency-color), var(--ir-surface) 95%);
}
.urgency-banner strong { display: block; font-size: 14px; }
.urgency-banner span { color: var(--ir-muted); font-size: 12.5px; }
.urgency-banner .urgency-icon {
  display: inline-grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: color-mix(in srgb, var(--urgency-color), transparent 85%);
  color: var(--urgency-color);
  font-size: 15px;
  font-weight: 800;
  flex: none;
}
.manual-badge {
  display: inline-block;
  margin-left: 8px;
  padding: 1px 7px;
  border-radius: 999px;
  font-size: 10.5px;
  font-weight: 700;
  background: color-mix(in srgb, var(--urgency-color), transparent 80%);
  color: color-mix(in srgb, var(--urgency-color), black 30%);
}
.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px 18px;
  padding: 14px 16px 16px;
}
.info-item { display: flex; flex-direction: column; gap: 4px; font-size: 13.5px; }
.info-item-wide { grid-column: 1 / -1; }
.info-label { color: var(--ir-muted); font-size: 12px; }
.reminder-panel-body { padding: 14px 16px 16px; display: grid; gap: 10px; }
.reminder-summary { margin: 0; font-size: 13.5px; }
.reminder-warning {
  margin: 0;
  padding: 7px 10px;
  border-radius: var(--ir-radius-sm);
  color: #9f2d35;
  background: color-mix(in srgb, var(--ir-danger), white 92%);
  border: 1px solid color-mix(in srgb, var(--ir-danger), transparent 72%);
  font-size: 12px;
  line-height: 1.5;
}
.reminder-state-list { margin: 0; padding: 0; list-style: none; display: grid; gap: 6px; }
.reminder-state-list li { display: flex; align-items: center; gap: 10px; font-size: 12.5px; color: var(--ir-muted); }
.reminder-state {
  padding: 1px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  background: var(--ir-surface-muted);
  color: var(--ir-muted);
}
.reminder-state.state-sent { background: color-mix(in srgb, var(--ir-success), white 88%); color: var(--ir-success); }
.reminder-state.state-failed { background: color-mix(in srgb, var(--ir-danger), white 88%); color: var(--ir-danger); }
.reminder-state.state-cancelled { opacity: .7; }
.reminder-sent { font-size: 12px; }
.reminder-none { margin: 0; color: var(--ir-faint); font-size: 12.5px; }
</style>
