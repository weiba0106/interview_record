<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElButton, ElDialog, ElInput, ElOption, ElSelect, ElTag } from 'element-plus'
import { createStatus, deleteStatus, listStatuses, updateStatus } from '../api/statuses.api'
import type { PositionStatus, StatusRequest } from '../api/tracking.types'
import { isApiRequestError } from '@/shared/api/error'

const STATISTICS_CATEGORY_OPTIONS = [
  { value: 'ACTIVE', label: '进行中' },
  { value: 'SUCCESS', label: '成功（Offer）' },
  { value: 'REJECTED', label: '未通过' },
  { value: 'WITHDRAWN', label: '放弃' },
] as const

const statuses = ref<PositionStatus[]>([])
const loading = ref(true)
const error = ref('')
const dialogOpen = ref(false)
const editing = ref<PositionStatus | null>(null)
const draft = ref<StatusRequest>({ name: '', color: '#e46f61', statisticsCategory: 'ACTIVE', active: true })
const fieldErrors = ref<Record<string, string>>({})
const submitting = ref(false)

const deleteTarget = ref<PositionStatus | null>(null)
const migrateToId = ref('')
const migrateOptions = computed(() => statuses.value.filter((status) => status.id !== deleteTarget.value?.id))

function categoryLabel(value: string): string {
  return STATISTICS_CATEGORY_OPTIONS.find((item) => item.value === value)?.label ?? value
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    statuses.value = await listStatuses()
  } catch (caught) {
    error.value = isApiRequestError(caught) ? caught.apiError.message : '加载状态失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => { void load() })

function openCreate() {
  editing.value = null
  draft.value = { name: '', color: '#e46f61', statisticsCategory: 'ACTIVE', active: true }
  fieldErrors.value = {}
  dialogOpen.value = true
}

function openEdit(status: PositionStatus) {
  editing.value = status
  draft.value = { name: status.name, color: status.color, statisticsCategory: status.statisticsCategory, active: status.active }
  fieldErrors.value = {}
  dialogOpen.value = true
}

async function submit() {
  fieldErrors.value = {}
  const name = draft.value.name.trim()
  if (!name) fieldErrors.value.name = '状态名称不能为空'
  if (!/^#[0-9a-fA-F]{6}$/.test(draft.value.color)) fieldErrors.value.color = '颜色必须是 #RRGGBB 格式'
  if (Object.keys(fieldErrors.value).length > 0) return
  submitting.value = true
  error.value = ''
  try {
    const payload: StatusRequest = { name, color: draft.value.color, statisticsCategory: draft.value.statisticsCategory, active: draft.value.active }
    if (editing.value) await updateStatus(editing.value.id, payload)
    else await createStatus(payload)
    dialogOpen.value = false
    await load()
  } catch (caught) {
    fieldErrors.value.name = isApiRequestError(caught) ? caught.apiError.message : '保存失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}

function openDelete(status: PositionStatus) {
  deleteTarget.value = status
  migrateToId.value = ''
  error.value = ''
}

async function confirmDelete() {
  if (!deleteTarget.value) return
  if (deleteTarget.value.positionCount > 0 && !migrateToId.value) {
    error.value = '请先选择岗位迁移目标状态'
    return
  }
  error.value = ''
  try {
    await deleteStatus(deleteTarget.value.id, migrateToId.value)
    deleteTarget.value = null
    await load()
  } catch (caught) {
    error.value = isApiRequestError(caught) ? caught.apiError.message : '删除失败，请稍后重试'
  }
}
</script>

<template>
  <section class="ir-panel" aria-labelledby="statuses-heading">
    <div class="ir-panel-head">
      <div><span class="panel-kicker">基础数据</span><h2 id="statuses-heading">岗位状态</h2></div>
      <ElButton size="small" data-action="add-status" @click="openCreate">新增状态</ElButton>
    </div>
    <p v-if="error" class="panel-inline-error" role="alert">{{ error }}</p>
    <p v-if="loading" class="panel-inline-hint" role="status">加载中…</p>
    <ul v-else class="status-list">
      <li v-for="status in statuses" :key="status.id">
        <span class="status-pill" :style="{ '--pill': status.color }">{{ status.name }}</span>
        <ElTag size="small" type="info">{{ categoryLabel(status.statisticsCategory) }}</ElTag>
        <ElTag v-if="!status.active" size="small" type="warning">已停用</ElTag>
        <span class="status-count">{{ status.positionCount }} 个岗位</span>
        <span class="item-actions">
          <ElButton size="small" text :data-action="`edit-status-${status.id}`" @click="openEdit(status)">编辑</ElButton>
          <ElButton size="small" text type="danger" :data-action="`delete-status-${status.id}`" @click="openDelete(status)">删除</ElButton>
        </span>
      </li>
    </ul>

    <ElDialog v-model="dialogOpen" :title="editing ? '编辑状态' : '新增状态'" width="min(92vw, 460px)" :teleported="false">
      <form novalidate class="status-form" @submit.prevent="submit">
        <label for="status-name">状态名称 *</label>
        <ElInput id="status-name" v-model="draft.name" name="name" maxlength="40" :aria-describedby="fieldErrors.name ? 'status-name-error' : undefined" />
        <p v-if="fieldErrors.name" id="status-name-error" data-field-error="name" role="alert">{{ fieldErrors.name }}</p>

        <label for="status-color">颜色 *</label>
        <ElInput id="status-color" v-model="draft.color" name="color" placeholder="#RRGGBB" :aria-describedby="fieldErrors.color ? 'status-color-error' : undefined" />
        <p v-if="fieldErrors.color" id="status-color-error" data-field-error="color" role="alert">{{ fieldErrors.color }}</p>

        <label for="status-category">统计分类 *</label>
        <ElSelect id="status-category" v-model="draft.statisticsCategory" name="statisticsCategory">
          <ElOption v-for="item in STATISTICS_CATEGORY_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </ElSelect>

        <label><input v-model="draft.active" name="active" type="checkbox" /> 启用中</label>
        <ElButton native-type="submit" type="primary" data-action="submit-status" :loading="submitting">保存</ElButton>
      </form>
    </ElDialog>

    <ElDialog :model-value="deleteTarget !== null" title="确认删除状态" width="min(92vw, 460px)" :teleported="false" @close="deleteTarget = null">
      <div v-if="deleteTarget">
        <p>确定删除状态「{{ deleteTarget.name }}」吗？</p>
        <div v-if="deleteTarget.positionCount > 0">
          <p>该状态下有 {{ deleteTarget.positionCount }} 个岗位，请先选择迁移目标状态：</p>
          <ElSelect v-model="migrateToId" name="migrateToId" placeholder="选择迁移目标状态">
            <ElOption v-for="status in migrateOptions" :key="status.id" :label="status.name" :value="status.id" />
          </ElSelect>
        </div>
      </div>
      <template #footer>
        <ElButton @click="deleteTarget = null">取消</ElButton>
        <ElButton type="danger" data-action="confirm-delete-status" :disabled="!deleteTarget" @click="confirmDelete">删除</ElButton>
      </template>
    </ElDialog>
  </section>
</template>

<style scoped>
.panel-inline-error { margin: 0; padding: 10px 16px; font-size: 13px; }
.panel-inline-hint { margin: 0; padding: 10px 16px; color: var(--ir-muted); font-size: 13px; }
.status-list {
  list-style: none;
  margin: 0;
  padding: 6px 10px 12px;
  display: flex;
  flex-direction: column;
}
.status-list li {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 9px 6px;
  border-bottom: 1px solid color-mix(in srgb, var(--ir-border), transparent 55%);
}
.status-list li:last-child { border-bottom: 0; }
.status-count {
  color: var(--ir-muted);
  font-size: 12.5px;
}
.item-actions { margin-left: auto; display: flex; gap: 6px; }
.status-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
</style>
