<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElButton, ElIcon, ElInput } from 'element-plus'
import { Briefcase, Calendar, ChatLineSquare, Plus, Search, Trophy } from '@element-plus/icons-vue'
import { getDashboard, type DashboardData } from '@/features/dashboard/api/dashboard.api'
import { changeScheduleStatus, scheduleEventTypeLabel, type Schedule } from '@/features/scheduling/api/schedules.api'
import { dateGroupLabel, sortByUrgency, urgencyCountdown, urgencyDisplay } from '@/features/scheduling/urgency'
import { isApiRequestError } from '@/shared/api/error'
import { formatDateTime } from '@/shared/format/datetime'

const loading = ref(true)
const error = ref('')
const message = ref('')
const dashboard = ref<DashboardData | null>(null)

const positionKeyword = ref('')
const positionStatusFilter = ref('')

const isEmpty = computed(() =>
  dashboard.value !== null
  && dashboard.value.metrics.totalPositions === 0
  && dashboard.value.schedules.length === 0)

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 5) return '夜深了'
  if (hour < 11) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const upcomingSchedules = computed<Schedule[]>(() => {
  if (!dashboard.value) return []
  return sortByUrgency(dashboard.value.schedules.filter((item) => item.status === 'PENDING')).slice(0, 5)
})
const urgentScheduleCount = computed(() =>
  dashboard.value?.schedules.filter((item) => item.status === 'PENDING' && item.urgency === 'URGENT').length ?? 0)
const urgencySummary = computed(() =>
  urgentScheduleCount.value > 0
    ? `今天有 ${urgentScheduleCount.value} 项紧急日程，请及时处理`
    : '今天暂无紧急日程，保持自己的节奏')

/** 全部岗位卡片：关键词 + 状态胶囊筛选（客户端过滤工作台快照） */
const statusOptions = computed(() => {
  const seen = new Map<string, { id: string; name: string; color: string }>()
  for (const position of dashboard.value?.positions ?? []) {
    if (!seen.has(position.status.id)) seen.set(position.status.id, position.status)
  }
  return [...seen.values()]
})
const filteredPositions = computed(() => {
  const keyword = positionKeyword.value.trim().toLowerCase()
  return (dashboard.value?.positions ?? []).filter((position) =>
    (!keyword || `${position.companyName} ${position.title}`.toLowerCase().includes(keyword))
    && (!positionStatusFilter.value || position.status.id === positionStatusFilter.value))
})

/** 即将到来：按日期分组 */
const groupedUpcoming = computed(() => {
  const groups: Array<{ label: string; items: Schedule[] }> = []
  for (const schedule of upcomingSchedules.value) {
    const label = dateGroupLabel(schedule.startsAt ?? schedule.endsAt ?? '')
    const group = groups.find((item) => item.label === label)
    if (group) group.items.push(schedule)
    else groups.push({ label, items: [schedule] })
  }
  return groups
})

const LEGEND = [
  { className: 'urgency-urgent', label: '紧急 · 24 小时内' },
  { className: 'urgency-approaching', label: '临近 · 72 小时内' },
  { className: 'urgency-normal', label: '普通 · 72 小时以上' },
  { className: 'urgency-handled', label: '已处理 · 完成/取消' },
]

async function load() {
  loading.value = true
  error.value = ''
  try {
    dashboard.value = await getDashboard()
  } catch (caught) {
    error.value = isApiRequestError(caught) ? caught.apiError.message : '加载工作台失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onMounted(() => { void load() })

async function setStatus(schedule: Schedule, status: 'COMPLETED' | 'CANCELLED') {
  error.value = ''; message.value = ''
  try {
    const updated = await changeScheduleStatus(schedule.id, status)
    if (dashboard.value) {
      dashboard.value.schedules = dashboard.value.schedules.map((item) => (item.id === updated.id ? updated : item))
    }
    message.value = status === 'COMPLETED' ? '日程已完成' : '日程已取消'
  } catch (caught) {
    error.value = isApiRequestError(caught) ? caught.apiError.message : '操作失败，请稍后重试'
  }
}
</script>

<template>
  <main class="dashboard" aria-labelledby="dashboard-heading">
    <div class="page-head" data-testid="dashboard-page-header">
      <div>
        <span class="eyebrow">岗位记录</span>
        <h1 id="dashboard-heading">概览</h1>
        <p class="page-desc" data-testid="dashboard-urgency-summary">{{ greeting }}，{{ urgencySummary }}</p>
      </div>
      <div class="page-head-actions">
        <RouterLink :to="{ name: 'new-position' }"><ElButton type="primary" data-action="create-position">+ 新增岗位</ElButton></RouterLink>
        <RouterLink :to="{ name: 'schedules', query: { create: '1' } }"><ElButton data-action="create-schedule">新增日程</ElButton></RouterLink>
      </div>
    </div>
    <p v-if="message" role="status" data-testid="dashboard-message">{{ message }}</p>
    <p v-if="error" role="alert">{{ error }}</p>
    <p v-if="loading" role="status">加载中…</p>

    <template v-if="!loading && dashboard">
      <section v-if="isEmpty" class="ir-panel dashboard-empty" aria-label="空状态引导" data-testid="dashboard-empty">
        <div class="ir-empty">
          <span class="ir-empty-icon" aria-hidden="true">面</span>
          <strong>还没有面试记录</strong>
          <p>先创建你的第一家公司和岗位，面试进度会汇总在这里。</p>
          <div class="dashboard-empty-actions">
            <RouterLink :to="{ name: 'companies' }"><ElButton data-action="empty-create-company">创建公司</ElButton></RouterLink>
            <RouterLink :to="{ name: 'new-position' }"><ElButton type="primary" data-action="empty-create-position">新增岗位</ElButton></RouterLink>
          </div>
        </div>
      </section>

      <template v-else>
        <section class="metric-grid" aria-label="概览指标">
          <div class="metric-card">
            <div class="metric-top">
              <span class="metric-value" data-testid="metric-total-positions">{{ dashboard.metrics.totalPositions }}</span>
              <span class="metric-icon" aria-hidden="true"><ElIcon><Briefcase /></ElIcon></span>
            </div>
            <span class="metric-label">全部岗位</span>
          </div>
          <div class="metric-card">
            <div class="metric-top">
              <span class="metric-value" data-testid="metric-active-positions">{{ dashboard.metrics.activePositions }}</span>
              <span class="metric-icon" aria-hidden="true"><ElIcon><ChatLineSquare /></ElIcon></span>
            </div>
            <span class="metric-label">面试中</span>
          </div>
          <div class="metric-card">
            <div class="metric-top">
              <span class="metric-value" data-testid="metric-upcoming-schedules">{{ dashboard.metrics.upcomingScheduleCount }}</span>
              <span class="metric-icon" aria-hidden="true"><ElIcon><Calendar /></ElIcon></span>
            </div>
            <span class="metric-label">本周日程</span>
          </div>
          <div class="metric-card">
            <div class="metric-top">
              <span class="metric-value" data-testid="metric-offers">{{ dashboard.metrics.offerCount }}</span>
              <span class="metric-icon" aria-hidden="true"><ElIcon><Trophy /></ElIcon></span>
            </div>
            <span class="metric-label">已获 Offer</span>
          </div>
        </section>

        <div class="dashboard-main-grid">
          <section class="ir-panel dashboard-positions" aria-labelledby="dashboard-positions-heading">
            <div class="ir-panel-head">
              <div><span class="panel-kicker">记录总览</span><h2 id="dashboard-positions-heading">全部岗位</h2></div>
              <RouterLink class="ir-panel-link" :to="{ name: 'positions' }">进入岗位列表</RouterLink>
            </div>
            <div class="dashboard-toolbar">
              <ElInput
                v-model="positionKeyword"
                name="dashboardPositionKeyword"
                placeholder="搜索公司或职位"
                clearable
                class="dashboard-search"
                aria-label="搜索岗位"
              >
                <template #prefix><ElIcon><Search /></ElIcon></template>
              </ElInput>
              <div class="chip-row dashboard-chips" role="group" aria-label="按状态筛选">
                <button type="button" class="chip" :aria-pressed="positionStatusFilter === ''" @click="positionStatusFilter = ''">全部</button>
                <button
                  v-for="status in statusOptions"
                  :key="status.id"
                  type="button"
                  class="chip"
                  :aria-pressed="positionStatusFilter === status.id"
                  :style="{ '--pill': status.color }"
                  @click="positionStatusFilter = positionStatusFilter === status.id ? '' : status.id"
                ><i class="chip-dot" aria-hidden="true" />{{ status.name }}</button>
              </div>
            </div>
            <div class="table-scroll">
              <table v-if="filteredPositions.length > 0" class="ir-table dashboard-table">
                <thead>
                  <tr><th scope="col">公司</th><th scope="col">职位</th><th scope="col">状态</th><th scope="col">下次日程</th></tr>
                </thead>
                <tbody>
                  <tr v-for="position in filteredPositions" :key="position.id">
                    <td class="cell-strong">{{ position.companyName }}</td>
                    <td><RouterLink :to="{ name: 'position-detail', params: { id: position.id } }">{{ position.title }}</RouterLink></td>
                    <td><span class="status-pill" :style="{ '--pill': position.status.color }">{{ position.status.name }}</span></td>
                    <td>{{ position.nextSchedule ? `${position.nextSchedule.title} · ${formatDateTime(position.nextSchedule.time)}` : '—' }}</td>
                  </tr>
                </tbody>
              </table>
              <div v-else class="ir-empty">
                <span class="ir-empty-icon" aria-hidden="true">⌕</span>
                <strong>没有匹配的岗位</strong>
                <p>试试其他关键词或状态，或者点击右上角「新增岗位」开始。</p>
              </div>
            </div>
          </section>

          <section class="ir-panel dashboard-agenda" aria-labelledby="dashboard-schedules-heading">
            <div class="ir-panel-head">
              <div><span class="panel-kicker">时间提醒</span><h2 id="dashboard-schedules-heading">即将到来</h2></div>
              <RouterLink class="ir-panel-link" :to="{ name: 'schedules' }">查看日程</RouterLink>
            </div>
            <div v-if="groupedUpcoming.length > 0" class="schedule-list">
              <template v-for="group in groupedUpcoming" :key="group.label">
                <p class="date-group-label">{{ group.label }}</p>
                <article
                  v-for="schedule in group.items"
                  :key="schedule.id"
                  class="urgency-event"
                  :class="urgencyDisplay(schedule.urgency).className"
                  :data-testid="`schedule-${schedule.id}`"
                >
                  <div class="event-top">
                    <p class="event-title">
                      <span class="urgency-icon" :data-urgency-icon="schedule.urgency" :aria-label="urgencyDisplay(schedule.urgency).label">{{ urgencyDisplay(schedule.urgency).icon }}</span>
                      {{ schedule.title }}
                    </p>
                    <span class="level">{{ urgencyDisplay(schedule.urgency).label }}</span>
                  </div>
                  <p class="event-meta">{{ scheduleEventTypeLabel(schedule.eventType) }} · {{ formatDateTime(schedule.startsAt ?? schedule.endsAt) }}</p>
                  <p v-if="schedule.positionTitle" class="event-meta">岗位：{{ schedule.positionTitle }}</p>
                  <p class="event-meta event-countdown">{{ urgencyCountdown(schedule) }}</p>
                  <div class="event-actions">
                    <ElButton size="small" :data-action="`complete-${schedule.id}`" @click="setStatus(schedule, 'COMPLETED')">完成</ElButton>
                    <ElButton size="small" text :data-action="`cancel-${schedule.id}`" @click="setStatus(schedule, 'CANCELLED')">取消</ElButton>
                  </div>
                </article>
              </template>
            </div>
            <div v-else class="ir-empty" data-testid="no-upcoming-schedules">
              <span class="ir-empty-icon" aria-hidden="true">☼</span>
              <strong>未来 7 天没有待处理日程</strong>
              <p>添加日程后，紧急程度会按 24 / 72 小时自动计算。</p>
            </div>
            <RouterLink class="add-schedule-button" :to="{ name: 'schedules', query: { create: '1' } }"><ElIcon><Plus /></ElIcon> 添加日程</RouterLink>
            <ul class="urgency-legend" aria-label="紧急程度说明">
              <li v-for="item in LEGEND" :key="item.label"><i :class="item.className" aria-hidden="true" /><span>{{ item.label }}</span></li>
            </ul>
          </section>
        </div>
      </template>
    </template>
  </main>
</template>

<style scoped>
.dashboard {
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.dashboard-empty {
  width: 100%;
  min-height: 360px;
  display: grid;
  place-items: center;
  padding: 40px 20px;
}
.dashboard-empty-actions { display: flex; gap: 10px; margin-top: 10px; flex-wrap: wrap; justify-content: center; }
.dashboard-main-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 344px;
  gap: 14px;
  align-items: start;
}
.dashboard-table { min-width: 600px; }
.dashboard-toolbar { display: flex; flex-direction: column; gap: 0; }
.dashboard-search { width: min(280px, 100%); margin: 12px 16px 0; }
.dashboard-chips { padding: 10px 16px 12px; border-bottom: 0; }
.chip-dot { width: 7px; height: 7px; border-radius: 50%; background: var(--pill, var(--ir-muted)); flex: none; }
.schedule-list {
  list-style: none;
  margin: 0;
  padding: 8px 10px 4px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
@media (max-width: 960px) {
  .dashboard-main-grid { grid-template-columns: 1fr; }
}
@media (max-width: 760px) {
  .dashboard-main-grid { display: flex; flex-direction: column; }
  /* 移动端先展示紧急日程，再展示岗位数据 */
  .dashboard-agenda { order: -1; }
  /* 空状态随内容自然撑开，不使用固定最小高度 */
  .dashboard-empty { min-height: auto; padding: 32px 16px; }
}
@media (max-width: 480px) {
  .page-head-actions { width: 100%; }
  .page-head-actions > * { flex: 1; }
  .schedule-list { padding: 8px 10px 4px; }
}
</style>
