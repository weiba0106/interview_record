<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElButton, ElDialog, ElIcon, ElInput, ElOption, ElPagination, ElSelect, ElTag } from 'element-plus'
import { ArrowDown, ArrowUp, Search, Setting } from '@element-plus/icons-vue'
import { listCompanies } from '@/features/tracking/api/companies.api'
import { listJobTypes } from '@/features/tracking/api/job-types.api'
import { listStatuses } from '@/features/tracking/api/statuses.api'
import { changePositionStatus, deletePosition, searchPositions, setPositionArchived } from '@/features/tracking/api/positions.api'
import type { Company, JobType, Position, PositionPage, PositionSearchParams, PositionStatus } from '@/features/tracking/api/tracking.types'
import { isApiRequestError } from '@/shared/api/error'
import { formatDate, formatDateTime } from '@/shared/format/datetime'

const loading = ref(true)
const error = ref('')
const message = ref('')
const companies = ref<Company[]>([])
const jobTypes = ref<JobType[]>([])
const statuses = ref<PositionStatus[]>([])
const result = ref<PositionPage | null>(null)

const filters = reactive<PositionSearchParams>({
  companyId: '',
  jobTypeId: '',
  statusId: '',
  archived: false,
  keyword: '',
  appliedFrom: '',
  appliedTo: '',
  page: 0,
  size: 20,
  sortBy: 'updatedAt',
  sortDir: 'desc',
})

/** 会话内保留筛选条件（PRD §7.3.2），不包含分页状态。 */
const FILTER_STORAGE_KEY = 'interview-record.positions.filters.v1'
const persistedFilters = ['companyId', 'jobTypeId', 'statusId', 'archived', 'keyword', 'appliedFrom', 'appliedTo', 'sortBy', 'sortDir'] as const

function restorePersistedFilters() {
  try {
    const raw = sessionStorage.getItem(FILTER_STORAGE_KEY)
    if (!raw) return
    const saved = JSON.parse(raw) as Record<string, unknown>
    for (const key of persistedFilters) {
      if (key in saved) {
        ;(filters as unknown as Record<string, unknown>)[key] = saved[key]
      }
    }
  } catch { /* 会话存储不可用或数据损坏时忽略，退回默认筛选 */ }
}

function persistFilters() {
  try {
    const snapshot: Record<string, unknown> = {}
    for (const key of persistedFilters) snapshot[key] = (filters as unknown as Record<string, unknown>)[key]
    sessionStorage.setItem(FILTER_STORAGE_KEY, JSON.stringify(snapshot))
  } catch { /* 会话存储不可用时静默降级 */ }
}

const deleteTarget = ref<Position | null>(null)
const pendingDelete = ref(false)
const viewMode = ref<'table' | 'board'>('table')
const advancedOpen = ref(false)

const totalPages = computed(() => result.value?.totalPages ?? 0)
const boardColumns = computed(() => statuses.value.map((status) => ({
  status,
  items: result.value?.items.filter((position) => position.status.id === status.id) ?? [],
})))
const activeFilterCount = computed(() =>
  [filters.companyId, filters.jobTypeId, filters.statusId].filter(Boolean).length + (filters.archived ? 1 : 0))

async function loadOptions() {
  try {
    ;[companies.value, jobTypes.value, statuses.value] = await Promise.all([listCompanies(), listJobTypes(), listStatuses()])
  } catch (caught) {
    error.value = isApiRequestError(caught) ? caught.apiError.message : '加载筛选选项失败'
  }
}

async function search(resetPage = false) {
  if (resetPage) filters.page = 0
  loading.value = true
  error.value = ''
  persistFilters()
  try {
    result.value = await searchPositions(filters)
  } catch (caught) {
    error.value = isApiRequestError(caught) ? caught.apiError.message : '加载岗位失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  restorePersistedFilters()
  await loadOptions()
  await search()
})

function applyFilters() { void search(true) }

function resetFilters() {
  filters.companyId = ''
  filters.jobTypeId = ''
  filters.statusId = ''
  filters.archived = false
  filters.keyword = ''
  filters.appliedFrom = ''
  filters.appliedTo = ''
  try { sessionStorage.removeItem(FILTER_STORAGE_KEY) } catch { /* 忽略存储不可用 */ }
  void search(true)
}

function toggleSortDirection() {
  filters.sortDir = filters.sortDir === 'desc' ? 'asc' : 'desc'
  void search(true)
}

function onPageChange(page: number) {
  filters.page = page - 1
  void search()
}

async function changeStatus(position: Position, statusId: string) {
  error.value = ''; message.value = ''
  try {
    const updated = await changePositionStatus(position.id, statusId)
    replaceItem(updated)
    message.value = '状态已更新'
  } catch (caught) {
    error.value = isApiRequestError(caught) ? caught.apiError.message : '更新状态失败'
    await search()
  }
}

async function toggleArchived(position: Position) {
  error.value = ''; message.value = ''
  try {
    const updated = await setPositionArchived(position.id, !position.archived)
    replaceItem(updated)
    message.value = updated.archived ? '岗位已归档' : '岗位已恢复'
  } catch (caught) {
    error.value = isApiRequestError(caught) ? caught.apiError.message : '操作失败'
  }
}

function replaceItem(updated: Position) {
  if (!result.value) return
  result.value = { ...result.value, items: result.value.items.map((item) => (item.id === updated.id ? updated : item)) }
}

function openDelete(position: Position) {
  deleteTarget.value = position
}

async function confirmDelete() {
  if (!deleteTarget.value) return
  pendingDelete.value = true
  error.value = ''
  try {
    await deletePosition(deleteTarget.value.id, true)
    message.value = '岗位已删除'
    deleteTarget.value = null
    await search()
  } catch (caught) {
    error.value = isApiRequestError(caught) ? caught.apiError.message : '删除失败，请稍后重试'
  } finally {
    pendingDelete.value = false
  }
}
</script>

<template>
  <main class="positions-page" aria-labelledby="positions-heading">
    <div class="page-head">
      <div>
        <span class="eyebrow">记录总览</span>
        <h1 id="positions-heading">岗位</h1>
        <p class="page-desc">按公司、招聘类型和状态筛选，表格与看板随时切换。</p>
      </div>
      <div class="page-head-actions">
        <div class="segmented" role="tablist" aria-label="岗位展示方式">
          <button type="button" role="tab" data-action="view-table" :aria-selected="viewMode === 'table'" :aria-pressed="viewMode === 'table'" @click="viewMode = 'table'">表格</button>
          <button type="button" role="tab" data-action="view-board" :aria-selected="viewMode === 'board'" :aria-pressed="viewMode === 'board'" @click="viewMode = 'board'">看板</button>
        </div>
        <RouterLink :to="{ name: 'new-position' }"><ElButton type="primary" data-action="create-position">+ 新增岗位</ElButton></RouterLink>
      </div>
    </div>
    <p v-if="message" role="status">{{ message }}</p>
    <p v-if="error" role="alert">{{ error }}</p>

    <div class="ir-panel">
      <form class="filter-bar" aria-label="岗位筛选" novalidate @submit.prevent="applyFilters">
        <div class="filter-row">
          <ElInput v-model="filters.keyword" name="filterKeyword" placeholder="搜索公司或职位名称" clearable class="keyword-input">
            <template #prefix><ElIcon><Search /></ElIcon></template>
          </ElInput>
          <ElSelect v-model="filters.companyId" name="filterCompanyId" clearable placeholder="全部公司">
            <ElOption v-for="company in companies" :key="company.id" :label="company.name" :value="company.id" />
          </ElSelect>
          <ElSelect v-model="filters.jobTypeId" name="filterJobTypeId" clearable placeholder="全部招聘类型">
            <ElOption v-for="jobType in jobTypes" :key="jobType.id" :label="jobType.name" :value="jobType.id" />
          </ElSelect>
          <ElSelect v-model="filters.statusId" name="filterStatusId" clearable placeholder="全部状态">
            <ElOption v-for="status in statuses" :key="status.id" :label="status.name" :value="status.id" />
          </ElSelect>
          <ElButton class="filter-toggle" :data-action="'toggle-advanced-filters'" :aria-expanded="advancedOpen" @click="advancedOpen = !advancedOpen">
            <ElIcon><Setting /></ElIcon>
            <span>更多筛选</span>
            <span v-if="activeFilterCount > 0" class="filter-badge">{{ activeFilterCount }}</span>
          </ElButton>
          <ElButton native-type="submit" type="primary" data-action="apply-filters">筛选</ElButton>
          <ElButton data-action="reset-filters" @click="resetFilters">重置</ElButton>
        </div>
        <div v-if="advancedOpen" class="filter-advanced">
          <label class="archived-filter"><input v-model="filters.archived" name="filterArchived" type="checkbox" /> 只看已归档</label>
          <span class="advanced-divider" aria-hidden="true" />
          <label class="date-filter">投递开始<input v-model="filters.appliedFrom" name="filterAppliedFrom" type="date" /></label>
          <label class="date-filter">投递结束<input v-model="filters.appliedTo" name="filterAppliedTo" type="date" /></label>
          <span class="advanced-divider" aria-hidden="true" />
          <ElSelect v-model="filters.sortBy" name="sortBy" class="sort-select" aria-label="排序字段">
            <ElOption label="按最近更新" value="updatedAt" />
            <ElOption label="按投递日期" value="appliedAt" />
            <ElOption label="按截止日期" value="deadlineAt" />
            <ElOption label="按下次日程" value="nextSchedule" />
          </ElSelect>
          <ElButton :data-action="'toggle-sort-dir'" @click="toggleSortDirection">
            <ElIcon><component :is="filters.sortDir === 'desc' ? ArrowDown : ArrowUp" /></ElIcon>
            {{ filters.sortDir === 'desc' ? '降序' : '升序' }}
          </ElButton>
        </div>
      </form>

      <p v-if="loading" role="status" class="positions-loading">加载中…</p>
      <template v-else-if="result">
        <div v-if="viewMode === 'table'" class="table-scroll">
          <table class="ir-table positions-table" v-if="result.items.length > 0">
            <thead>
              <tr><th scope="col">公司</th><th scope="col">职位</th><th scope="col">招聘类型</th><th scope="col">当前状态</th><th scope="col">投递日期</th><th scope="col">下一场日程</th><th scope="col">操作</th></tr>
            </thead>
            <tbody>
              <tr v-for="position in result.items" :key="position.id" :data-testid="`position-${position.id}`">
                <td class="cell-strong">{{ position.companyName }}</td>
                <td>
                  <RouterLink :to="{ name: 'position-detail', params: { id: position.id } }">{{ position.title }}</RouterLink>
                  <ElTag v-if="position.archived" size="small" type="info">已归档</ElTag>
                </td>
                <td>{{ position.jobTypeName }}</td>
                <td>
                  <ElSelect :model-value="position.status.id" :name="`status-${position.id}`" size="small" class="row-status-select" :aria-label="`修改 ${position.title} 的状态`" @change="(value: string) => changeStatus(position, value)">
                    <ElOption v-for="status in statuses" :key="status.id" :label="status.name" :value="status.id" :disabled="!status.active && status.id !== position.status.id" />
                  </ElSelect>
                </td>
                <td>{{ formatDate(position.appliedAt) }}</td>
                <td>{{ position.nextSchedule ? `${position.nextSchedule.title} · ${formatDateTime(position.nextSchedule.time)}` : '—' }}</td>
                <td class="actions-cell">
                  <RouterLink class="row-action" :to="{ name: 'position-detail', params: { id: position.id } }">详情</RouterLink>
                  <RouterLink class="row-action" :to="{ name: 'edit-position', params: { id: position.id } }">编辑</RouterLink>
                  <ElButton size="small" text :data-action="`archive-${position.id}`" @click="toggleArchived(position)">{{ position.archived ? '恢复' : '归档' }}</ElButton>
                  <ElButton size="small" text type="danger" :data-action="`delete-${position.id}`" @click="openDelete(position)">删除</ElButton>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-else class="ir-empty" data-testid="no-positions">
            <span class="ir-empty-icon" aria-hidden="true">⌕</span>
            <strong>没有符合条件的岗位</strong>
            <p>调整筛选条件或点击右上角「新增岗位」创建。</p>
          </div>
        </div>
        <div v-else class="positions-board-wrap">
          <div class="positions-board" aria-label="岗位看板">
            <section v-for="column in boardColumns" :key="column.status.id" class="position-column" :data-status-column="column.status.id">
              <header class="position-column-header"><span class="status-pill" :style="{ '--pill': column.status.color }">{{ column.status.name }}</span><span class="column-count">{{ column.items.length }}</span></header>
              <div v-if="column.items.length > 0" class="position-column-items">
                <article v-for="position in column.items" :key="position.id" class="position-card" :data-testid="`board-position-${position.id}`">
                  <RouterLink :to="{ name: 'position-detail', params: { id: position.id } }" class="position-card-title">{{ position.title }}</RouterLink>
                  <p>{{ position.companyName }}</p>
                  <div class="position-card-meta"><span>{{ position.jobTypeName }}</span><span>{{ formatDate(position.appliedAt) }}</span></div>
                  <div class="position-card-actions"><RouterLink :to="{ name: 'position-detail', params: { id: position.id } }">详情</RouterLink><RouterLink :to="{ name: 'edit-position', params: { id: position.id } }">编辑</RouterLink></div>
                </article>
              </div>
              <p v-else class="column-empty">暂无岗位</p>
            </section>
          </div>
        </div>
      </template>
    </div>

    <ElPagination
      v-if="!loading && totalPages > 1"
      class="positions-pagination"
      layout="prev, pager, next"
      :total="result!.totalItems"
      :page-size="result!.size"
      :current-page="result!.page + 1"
      @current-change="onPageChange"
    />

    <ElDialog :model-value="deleteTarget !== null" title="确认删除岗位" width="min(92vw, 460px)" :teleported="false" @close="deleteTarget = null">
      <div v-if="deleteTarget">
        <p>删除岗位「{{ deleteTarget.companyName }} · {{ deleteTarget.title }}」后无法恢复，将同时删除：</p>
        <ul class="cascade-list">
          <li>{{ deleteTarget.interviewRoundCount }} 条面试轮次记录</li>
          <li>{{ deleteTarget.scheduleCount }} 条关联日程</li>
        </ul>
      </div>
      <template #footer>
        <ElButton @click="deleteTarget = null">取消</ElButton>
        <ElButton type="danger" data-action="confirm-delete-position" :loading="pendingDelete" :disabled="!deleteTarget" @click="confirmDelete">永久删除</ElButton>
      </template>
    </ElDialog>
  </main>
</template>

<style scoped>
.positions-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.filter-bar {
  display: flex;
  flex-direction: column;
  border-bottom: 1px solid var(--ir-border);
}
.filter-row {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
  padding: 12px 16px;
}
.keyword-input {
  width: min(240px, 100%);
  flex: 1 1 180px;
}
.filter-row .el-select { flex: 0 1 160px; min-width: 140px; }
.filter-toggle { flex: none; }
.filter-badge {
  display: inline-grid; place-items: center; min-width: 16px; height: 16px; padding: 0 4px;
  border-radius: 999px; background: var(--ir-primary-soft); color: var(--ir-primary-strong);
  font-size: 10px; font-weight: 800;
}
.filter-advanced {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding: 0 16px 12px;
  border-top: 1px dashed color-mix(in srgb, var(--ir-border), transparent 40%);
  padding-top: 12px;
}
.advanced-divider { width: 1px; height: 20px; background: var(--ir-border); }
.archived-filter {
  display: flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
  color: var(--ir-muted);
  font-size: 13px;
}
.date-filter {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--ir-muted);
  font-size: 13px;
  white-space: nowrap;
}
.date-filter input { width: 140px; }
.sort-select { width: 150px; }
.positions-loading {
  margin: 0;
  padding: 16px;
  color: var(--ir-muted);
  font-size: 13px;
}
.positions-table { min-width: 820px; }
.row-status-select { width: 130px; }
.positions-board-wrap { padding: 14px 16px 16px; overflow-x: auto; }
.positions-board {
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: minmax(230px, 1fr);
  gap: 12px;
  align-items: start;
}
.position-column {
  min-width: 230px;
  border-radius: var(--ir-radius-md);
  background: var(--ir-surface-muted);
  padding: 10px;
}
.position-column-header { display: flex; justify-content: space-between; align-items: center; gap: 8px; margin-bottom: 10px; }
.column-count { color: var(--ir-muted); font-size: 12px; font-weight: 700; }
.position-column-items { display: grid; gap: 8px; }
.position-card { border: 1px solid var(--ir-border); border-radius: var(--ir-radius-md); background: var(--ir-surface); padding: 12px; box-shadow: var(--ir-shadow-sm); transition: transform var(--ir-transition), box-shadow var(--ir-transition); }
.position-card:hover { transform: translateY(-2px); box-shadow: var(--ir-shadow-md); }
.position-card-title { color: var(--ir-text); font-weight: 700; text-decoration: none; }
.position-card-title:hover { color: var(--ir-primary-strong); }
.position-card p { margin: 4px 0 9px; color: var(--ir-muted); font-size: 12px; }
.position-card-meta { display: flex; justify-content: space-between; gap: 8px; color: var(--ir-muted); font-size: 11px; }
.position-card-actions { display: flex; gap: 9px; margin-top: 10px; font-size: 11px; }
.column-empty { margin: 0; color: var(--ir-muted); font-size: 12px; }
.positions-pagination { justify-content: center; }
.cascade-list { margin: 8px 0; padding-left: 20px; }
@media (max-width: 760px) {
  .filter-row { padding: 10px 12px; }
  .keyword-input { width: 100%; flex-basis: 100%; }
  .filter-row .el-select { flex: 1 1 44%; min-width: 0; }
  .filter-advanced { padding: 0 12px 12px; padding-top: 12px; }
  .advanced-divider { display: none; }
  .positions-board { grid-auto-flow: row; grid-auto-columns: auto; }
  .position-column { min-width: 0; }
}
</style>
