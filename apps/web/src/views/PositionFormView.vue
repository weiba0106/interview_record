<script setup lang="ts">
import { computed, onMounted, ref, useTemplateRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElButton } from 'element-plus'
import PositionForm from '@/features/tracking/components/PositionForm.vue'
import { listCompanies } from '@/features/tracking/api/companies.api'
import { listJobTypes } from '@/features/tracking/api/job-types.api'
import { listStatuses } from '@/features/tracking/api/statuses.api'
import { createPosition, getPosition, updatePosition } from '@/features/tracking/api/positions.api'
import type { Company, JobType, Position, PositionRequest, PositionStatus } from '@/features/tracking/api/tracking.types'
import { isApiRequestError } from '@/shared/api/error'

const route = useRoute()
const router = useRouter()
const positionId = computed(() => (typeof route.params.id === 'string' ? route.params.id : ''))
const isEdit = computed(() => positionId.value !== '')

const loading = ref(true)
const error = ref('')
const companies = ref<Company[]>([])
const jobTypes = ref<JobType[]>([])
const statuses = ref<PositionStatus[]>([])
const position = ref<Position | null>(null)

onMounted(async () => {
  try {
    const [loadedCompanies, loadedJobTypes, loadedStatuses] = await Promise.all([
      listCompanies(), listJobTypes(), listStatuses(),
    ])
    companies.value = loadedCompanies
    jobTypes.value = loadedJobTypes
    statuses.value = loadedStatuses
    if (isEdit.value) position.value = await getPosition(positionId.value)
  } catch (caught) {
    error.value = isApiRequestError(caught) ? caught.apiError.message : '加载表单数据失败，请稍后重试'
  } finally {
    loading.value = false
  }
})

async function submit(payload: PositionRequest) {
  error.value = ''
  try {
    const saved = isEdit.value
      ? await updatePosition(positionId.value, payload)
      : await createPosition(payload)
    positionFormRef.value?.clearDraft()
    await router.push({ name: 'position-detail', params: { id: saved.id } })
  } catch (caught) {
    if (isApiRequestError(caught) && caught.status === 409) {
      error.value = caught.apiError.message
      return
    }
    error.value = isApiRequestError(caught) ? caught.apiError.message : '保存失败，请稍后重试'
  }
}

const positionFormRef = useTemplateRef<InstanceType<typeof PositionForm>>('position-form')
</script>

<template>
  <main class="position-form-page page-center" :aria-labelledby="isEdit ? 'edit-position-heading' : 'new-position-heading'">
    <div class="page-head">
      <div>
        <span class="eyebrow">记录总览</span>
        <h1 :id="isEdit ? 'edit-position-heading' : 'new-position-heading'">{{ isEdit ? '编辑岗位' : '新增岗位' }}</h1>
        <p class="page-desc">公司、职位、状态和投递信息一次填齐，也可以稍后补充。</p>
      </div>
      <div class="page-head-actions">
        <ElButton @click="router.back()">返回</ElButton>
      </div>
    </div>
    <p v-if="error" role="alert">{{ error }}</p>
    <p v-if="loading" role="status">加载中…</p>
    <PositionForm
      v-else
      ref="position-form"
      :companies="companies"
      :job-types="jobTypes"
      :statuses="statuses"
      :position="position"
      :initial-company-id="typeof route.query.company === 'string' ? route.query.company : undefined"
      :submit-label="isEdit ? '保存修改' : '创建岗位'"
      @submitted="submit"
    />
    <p v-if="!loading && companies.length === 0" class="form-hint">还没有公司？没关系，直接在上方输入新公司名称即可快速创建；也可以先到<RouterLink :to="{ name: 'companies' }">公司</RouterLink>补充官网和备注。</p>
  </main>
</template>

<style scoped>
.position-form-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
  max-width: 720px;
}
.form-hint { margin: 0; color: var(--ir-muted); font-size: 13px; line-height: 1.7; }
</style>
