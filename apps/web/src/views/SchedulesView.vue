<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElButton, ElDialog, ElOption, ElSelect } from 'element-plus'
import ScheduleForm from '@/features/scheduling/components/ScheduleForm.vue'
import {
  changeScheduleStatus,
  createSchedule,
  deleteSchedule,
  listSchedules,
  overrideScheduleUrgency,
  scheduleEventTypeLabel,
  updateSchedule,
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
@media (max-width: 480px) {
  .event-actions { width: 100%; }
  .urgency-select { flex: 1; }
}
</style>
