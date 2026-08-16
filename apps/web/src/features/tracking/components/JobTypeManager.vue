<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElButton, ElDialog, ElInput, ElTag } from 'element-plus'
import { createJobType, listJobTypes, updateJobType } from '../api/job-types.api'
import type { JobType } from '../api/tracking.types'
import { isApiRequestError } from '@/shared/api/error'

const jobTypes = ref<JobType[]>([])
const loading = ref(true)
const error = ref('')
const dialogOpen = ref(false)
const editing = ref<JobType | null>(null)
const draftName = ref('')
const draftActive = ref(true)
const nameError = ref('')
const submitting = ref(false)

async function load() {
  loading.value = true
  error.value = ''
  try {
    jobTypes.value = await listJobTypes()
  } catch (caught) {
    error.value = isApiRequestError(caught) ? caught.apiError.message : '加载招聘类型失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => { void load() })

function openCreate() {
  editing.value = null
  draftName.value = ''
  draftActive.value = true
  nameError.value = ''
  dialogOpen.value = true
}

function openEdit(jobType: JobType) {
  editing.value = jobType
  draftName.value = jobType.name
  draftActive.value = jobType.active
  nameError.value = ''
  dialogOpen.value = true
}

async function submit() {
  nameError.value = ''
  const name = draftName.value.trim()
  if (!name) { nameError.value = '名称不能为空'; return }
  submitting.value = true
  error.value = ''
  try {
    if (editing.value) await updateJobType(editing.value.id, { name, active: draftActive.value })
    else await createJobType({ name, active: draftActive.value })
    dialogOpen.value = false
    await load()
  } catch (caught) {
    nameError.value = isApiRequestError(caught) ? caught.apiError.message : '保存失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}

async function toggleActive(jobType: JobType) {
  error.value = ''
  try {
    await updateJobType(jobType.id, { name: jobType.name, active: !jobType.active })
    await load()
  } catch (caught) {
    error.value = isApiRequestError(caught) ? caught.apiError.message : '操作失败，请稍后重试'
  }
}
</script>

<template>
  <section class="ir-panel" aria-labelledby="job-types-heading">
    <div class="ir-panel-head">
      <div><span class="panel-kicker">基础数据</span><h2 id="job-types-heading">招聘类型</h2></div>
      <ElButton size="small" data-action="add-job-type" @click="openCreate">新增招聘类型</ElButton>
    </div>
    <p v-if="error" class="panel-inline-error" role="alert">{{ error }}</p>
    <p v-if="loading" class="panel-inline-hint" role="status">加载中…</p>
    <ul v-else class="job-type-list">
      <li v-for="jobType in jobTypes" :key="jobType.id">
        <span class="item-name">{{ jobType.name }}</span>
        <ElTag v-if="!jobType.active" type="info" size="small">已停用</ElTag>
        <span class="item-actions">
          <ElButton size="small" text :data-action="`edit-job-type-${jobType.id}`" @click="openEdit(jobType)">编辑</ElButton>
          <ElButton size="small" text :data-action="`toggle-job-type-${jobType.id}`" @click="toggleActive(jobType)">{{ jobType.active ? '停用' : '启用' }}</ElButton>
        </span>
      </li>
    </ul>

    <ElDialog v-model="dialogOpen" :title="editing ? '编辑招聘类型' : '新增招聘类型'" width="min(92vw, 420px)" :teleported="false">
      <form novalidate @submit.prevent="submit">
        <label for="job-type-name">名称 *</label>
        <ElInput id="job-type-name" v-model="draftName" name="name" maxlength="40" :aria-describedby="nameError ? 'job-type-name-error' : undefined" />
        <p v-if="nameError" id="job-type-name-error" data-field-error="name" role="alert">{{ nameError }}</p>
        <template v-if="editing">
          <label><input v-model="draftActive" name="active" type="checkbox" /> 启用中</label>
        </template>
        <ElButton native-type="submit" type="primary" data-action="submit-job-type" :loading="submitting">保存</ElButton>
      </form>
    </ElDialog>
  </section>
</template>

<style scoped>
.panel-inline-error { margin: 0; padding: 10px 16px; font-size: 13px; }
.panel-inline-hint { margin: 0; padding: 10px 16px; color: var(--ir-muted); font-size: 13px; }
.job-type-list {
  list-style: none;
  margin: 0;
  padding: 6px 10px 12px;
  display: flex;
  flex-direction: column;
}
.job-type-list li {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 9px 6px;
  border-bottom: 1px solid color-mix(in srgb, var(--ir-border), transparent 55%);
}
.job-type-list li:last-child { border-bottom: 0; }
.item-name { font-weight: 600; font-size: 13.5px; }
.item-actions { margin-left: auto; display: flex; gap: 6px; }
form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
</style>
