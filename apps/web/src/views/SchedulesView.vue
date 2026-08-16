<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElButton, ElDialog, ElInput, ElOption, ElSelect } from 'element-plus'
import ScheduleForm from '@/features/scheduling/components/ScheduleForm.vue'
import {
  changeScheduleStatus,
  createSchedule,
  deleteSchedule,
  listSchedules,
  overrideScheduleUrgency,
  scheduleEventTypeLabel,
  updateSchedule,
  updateScheduleReminders,
  type Schedule,
  type ScheduleRequest,
} from '@/features/scheduling/api/schedules.api'
import { dateGroupLabel, sortByUrgency, urgencyCountdown, urgencyDisplay } from '@/features/scheduling/urgency'
import { searchPositions } from '@/features/tracking/api/positions.api'
import type { PositionSummary } from '@/features/tracking/api/tracking.types'
import { isApiRequestError } from '@/shared/api/error'
import { formatDateTime } from '@/shared/format/datetime'

const route = useRoute()
const loading = ref(true)
const error = ref('')
const message = ref('')
const statusFilter = ref<'ALL' | 'PENDING' | 'COMPLETED' | 'CANCELLED'>('ALL')
const schedules = ref<Schedule[]>([])
const positions = ref<PositionSummary[]>([])

const formDialogOpen = ref(false)
const editing = ref<Schedule | null>(null)
const deleteTarget = ref<Schedule | null>(null)
const pendingRequest = ref(false)

const reminderDialogOpen = ref(false)
const reminderTarget = ref<Schedule | null>(null)
const reminderMode = ref<'AUTO' | 'DISABLED' | 'CUSTOM'>('AUTO')
const reminderInput = ref('')
const reminderError = ref('')
const reminderSaving = ref(false)

const sortedSchedules = computed(() => sortByUrgency(schedules.value))
const groupedSchedules = computed(() => {
  const groups: Array<{ label: string; items: Schedule[] }> = []
  for (const schedule of sortedSchedules.value) {
    const label = dateGroupLabel(schedule.startsAt ?? schedule.endsAt ?? '')
    const group = groups.find((item) => item.label === label)
    if (group) group.items.push(schedule)
    else groups.push({ label, items: [schedule] })
  }
  return groups
})

function failedReminderCount(schedule: Schedule): number {
  return (schedule.reminders ?? []).filter((item) => item.status === 'FAILED').length
}

function reminderSummary(schedule: Schedule): string {
  const offsets = schedule.reminderOffsets
  if (offsets === null || offsets === undefined) return '默认规则'
  if (offsets.length === 0) return '已关闭'
  return `提前 ${offsets.join('、')} 分钟`
}

function openReminderDialog(schedule: Schedule) {
  reminderTarget.value = schedule
  reminderError.value = ''
  const offsets = schedule.reminderOffsets
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
  reminderDialogOpen.value = true
}

async function saveReminders() {
  if (!reminderTarget.value || reminderSaving.value) return
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
  error.value = ''; message.value = ''
  try {
    const updated = await updateScheduleReminders(reminderTarget.value.id, offsets)
    schedules.value = schedules.value.map((item) => (item.id === updated.id ? updated : item))
    message.value = '提醒设置已更新'
    reminderDialogOpen.value = false
  } catch (caught) {
    reminderError.value = isApiRequestError(caught) ? caught.apiError.message : '保存提醒设置失败，请稍后重试'
  } finally {
    reminderSaving.value = false
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    schedules.value = await listSchedules(statusFilter.value === 'ALL' ? undefined : statusFilter.value)
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
  } catch { /* 岗位选项加载失败不阻塞日程页面 */ }
}

onMounted(async () => {
  await Promise.all([load(), loadPositions()])
  if (route.query.create === '1') openCreate()
})

function setFilter(next: typeof statusFilter.value) {
  statusFilter.value = next
  void load()
}

function openCreate() {
  editing.value = null
  formDialogOpen.value = true
}

function openEdit(schedule: Schedule) {
  editing.value = schedule
  formDialogOpen.value = true
}

async function submitSchedule(payload: ScheduleRequest) {
  error.value = ''; message.value = ''
  pendingRequest.value = true
  try {
    if (editing.value) {
      await updateSchedule(editing.value.id, payload)
      message.value = '日程已更新'
    } else {
      await createSchedule(payload)
      message.value = '日程已创建'
    }
    formDialogOpen.value = false
    await load()
  } catch (caught) {
    error.value = isApiRequestError(caught) ? caught.apiError.message : '保存日程失败'
  } finally {
    pendingRequest.value = false
  }
}

async function setStatus(schedule: Schedule, status: 'PENDING' | 'COMPLETED' | 'CANCELLED') {
  error.value = ''; message.value = ''
  try {
    const updated = await changeScheduleStatus(schedule.id, status)
    schedules.value = schedules.value.map((item) => (item.id === updated.id ? updated : item))
    message.value = status === 'COMPLETED' ? '日程已完成' : status === 'CANCELLED' ? '日程已取消' : '日程已恢复为待处理'
    if (statusFilter.value !== 'ALL' && statusFilter.value !== status) await load()
  } catch (caught) {
    error.value = isApiRequestError(caught) ? caught.apiError.message : '操作失败，请稍后重试'
  }
}

async function setUrgency(schedule: Schedule, urgency: string) {
  error.value = ''
  try {
    const updated = await overrideScheduleUrgency(schedule.id, urgency === 'AUTO' ? null : urgency)
    schedules.value = schedules.value.map((item) => (item.id === updated.id ? updated : item))
    message.value = urgency === 'AUTO' ? '已恢复自动紧急程度' : '已手动设置紧急程度'
  } catch (caught) {
    error.value = isApiRequestError(caught) ? caught.apiError.message : '设置紧急程度失败'
  }
}

async function confirmDelete() {
  if (!deleteTarget.value) return
  pendingRequest.value = true
  error.value = ''
  try {
    await deleteSchedule(deleteTarget.value.id)
    message.value = '日程已删除'
    deleteTarget.value = null
    await load()
  } catch (caught) {
    error.value = isApiRequestError(caught) ? caught.apiError.message : '删除失败，请稍后重试'
  } finally {
    pendingRequest.value = false
  }
}
</script>

<template>
  <main class="schedules-page page-center" aria-labelledby="schedules-heading">
    <div class="page-head">
      <div>
        <span class="eyebrow">时间提醒</span>
        <h1 id="schedules-heading">日程</h1>
        <p class="page-desc">紧急程度按 24 / 72 小时边界自动计算，也可以手动覆盖。</p>
      </div>
      <div class="page-head-actions">
        <ElButton type="primary" data-action="add-schedule" @click="openCreate">+ 新增日程</ElButton>
      </div>
    </div>
    <p v-if="message" role="status">{{ message }}</p>
    <p v-if="error" role="alert">{{ error }}</p>

    <div class="schedules-toolbar">
      <div class="segmented" role="group" aria-label="日程状态筛选">
        <button type="button" data-action="filter-all" :aria-pressed="statusFilter === 'ALL'" @click="setFilter('ALL')">全部</button>
        <button type="button" data-action="filter-pending" :aria-pressed="statusFilter === 'PENDING'" @click="setFilter('PENDING')">待处理</button>
        <button type="button" data-action="filter-completed" :aria-pressed="statusFilter === 'COMPLETED'" @click="setFilter('COMPLETED')">已完成</button>
        <button type="button" data-action="filter-cancelled" :aria-pressed="statusFilter === 'CANCELLED'" @click="setFilter('CANCELLED')">已取消</button>
      </div>
    </div>

    <p v-if="loading" role="status">加载中…</p>
    <template v-else>
      <div v-if="groupedSchedules.length > 0" class="schedule-groups">
        <section v-for="group in groupedSchedules" :key="group.label" class="schedule-group">
          <h2 class="date-group-label">{{ group.label }}</h2>
          <ul class="schedule-list">
            <li
              v-for="schedule in group.items"
              :key="schedule.id"
              class="urgency-event schedule-item"
              :class="urgencyDisplay(schedule.urgency).className"
              :data-testid="`schedule-${schedule.id}`"
            >
              <div class="event-top">
                <p class="event-title">
                  <span class="urgency-icon" :aria-label="urgencyDisplay(schedule.urgency).label">{{ urgencyDisplay(schedule.urgency).icon }}</span>
                  {{ schedule.title }}
                  <span v-if="schedule.manualUrgency" class="manual-badge">手动</span>
                </p>
                <div class="event-badges">
                  <span class="level">{{ urgencyDisplay(schedule.urgency).label }}</span>
                </div>
              </div>
              <p class="event-meta">
                {{ scheduleEventTypeLabel(schedule.eventType) }} · {{ formatDateTime(schedule.startsAt ?? schedule.endsAt) }}
                <template v-if="schedule.endsAt && schedule.startsAt"> ~ {{ formatDateTime(schedule.endsAt) }}</template>
              </p>
              <p v-if="schedule.positionTitle" class="event-meta">岗位：{{ schedule.positionTitle }}<template v-if="schedule.location"> · 形式/地点：{{ schedule.location }}</template></p>
              <p v-if="schedule.status === 'PENDING'" class="event-meta event-countdown">{{ urgencyCountdown(schedule) }}</p>
              <p v-if="failedReminderCount(schedule) > 0" class="reminder-warning" role="status" :data-testid="`reminder-failed-${schedule.id}`">
                ⚠ 邮件提醒发送失败（{{ failedReminderCount(schedule) }} 条），已停止重试，请检查邮箱后编辑日程重新触发。
              </p>
              <p class="reminder-config" :data-testid="`reminder-config-${schedule.id}`">提醒：{{ reminderSummary(schedule) }}</p>
              <div class="event-actions">
                <ElSelect
                  v-if="schedule.status === 'PENDING'"
                  :model-value="schedule.manualUrgency ?? 'AUTO'"
                  size="small"
                  class="urgency-select"
                  :aria-label="`设置 ${schedule.title} 的紧急程度`"
                  @change="(value: string) => setUrgency(schedule, value)"
                >
                  <ElOption label="自动" value="AUTO" />
                  <ElOption label="紧急" value="URGENT" />
                  <ElOption label="临近" value="APPROACHING" />
                  <ElOption label="普通" value="NORMAL" />
                </ElSelect>
                <ElButton v-if="schedule.status === 'PENDING'" size="small" :data-action="`complete-${schedule.id}`" @click="setStatus(schedule, 'COMPLETED')">完成</ElButton>
                <ElButton v-if="schedule.status === 'PENDING'" size="small" text :data-action="`cancel-${schedule.id}`" @click="setStatus(schedule, 'CANCELLED')">取消</ElButton>
                <ElButton v-else size="small" :data-action="`reopen-${schedule.id}`" @click="setStatus(schedule, 'PENDING')">恢复待处理</ElButton>
                <ElButton size="small" text :data-action="`reminders-${schedule.id}`" @click="openReminderDialog(schedule)">提醒</ElButton>
                <ElButton size="small" text :data-action="`edit-${schedule.id}`" @click="openEdit(schedule)">编辑</ElButton>
                <ElButton size="small" text type="danger" :data-action="`delete-${schedule.id}`" @click="deleteTarget = schedule">删除</ElButton>
              </div>
            </li>
          </ul>
        </section>
      </div>
      <div v-else class="ir-panel">
        <div class="ir-empty" data-testid="no-schedules">
          <span class="ir-empty-icon" aria-hidden="true">☼</span>
          <strong>暂无日程</strong>
          <p>点击右上角「新增日程」，让重要节点不再被遗漏。</p>
        </div>
      </div>
    </template>

    <ElDialog v-model="formDialogOpen" :title="editing ? '编辑日程' : '新增日程'" width="min(94vw, 560px)" top="6vh" :teleported="false">
      <ScheduleForm :initial="editing" :positions="positions" @submitted="submitSchedule" />
    </ElDialog>

    <ElDialog :model-value="deleteTarget !== null" title="确认删除日程" width="min(92vw, 420px)" :teleported="false" @close="deleteTarget = null">
      <p>删除日程「{{ deleteTarget?.title }}」后无法恢复。确定吗？</p>
      <template #footer>
        <ElButton @click="deleteTarget = null">取消</ElButton>
        <ElButton type="danger" data-action="confirm-delete-schedule" :loading="pendingRequest" @click="confirmDelete">删除</ElButton>
      </template>
    </ElDialog>

    <ElDialog v-model="reminderDialogOpen" :title="`提醒设置 · ${reminderTarget?.title ?? ''}`" width="min(94vw, 460px)" :teleported="false">
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
        <ElButton @click="reminderDialogOpen = false">取消</ElButton>
        <ElButton type="primary" data-action="save-reminders" :loading="reminderSaving" @click="saveReminders">保存提醒设置</ElButton>
      </template>
    </ElDialog>
  </main>
</template>

<style scoped>
.schedules-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 920px;
}
.schedules-toolbar { display: flex; }
.schedule-groups { display: flex; flex-direction: column; gap: 14px; }
.schedule-group { display: flex; flex-direction: column; gap: 8px; }
.schedule-group .date-group-label { margin: 0 2px; }
.schedule-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.schedule-item { box-shadow: var(--ir-shadow-sm); }
.event-badges { display: flex; align-items: center; gap: 6px; }
.manual-badge {
  display: inline-block; margin-left: 2px; padding: 1px 7px;
  border-radius: 999px; font-size: 10.5px; font-weight: 700;
  background: color-mix(in srgb, var(--urgency-color), transparent 80%);
  color: color-mix(in srgb, var(--urgency-color), black 30%);
  vertical-align: 1px;
}
.urgency-select { width: 96px; }
.reminder-config {
  margin: 0;
  color: var(--ir-faint);
  font-size: 11.5px;
}
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
@media (max-width: 480px) {
  .event-actions { width: 100%; }
  .urgency-select { flex: 1; }
}
</style>
