<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElButton, ElCheckbox, ElInput, ElOption, ElSelect } from 'element-plus'
import RichTextEditor from '@/shared/components/RichTextEditor.vue'
import { useFormDraft } from '@/shared/forms/useFormDraft'
import type { Company, JobType, Position, PositionRequest, PositionStatus } from '../api/tracking.types'
import { fromDateInput, fromDatetimeInput, toDateInput, toDatetimeInput } from '@/shared/format/datetime'

const props = defineProps<{
  companies: Company[]
  jobTypes: JobType[]
  statuses: PositionStatus[]
  position?: Position | null
  submitLabel?: string
  /** 从公司详情页“新增岗位”进入时预选的公司 */
  initialCompanyId?: string
}>()
const emit = defineEmits<{ submitted: [payload: PositionRequest] }>()
const fieldErrors = ref<Record<string, string>>({})

const firstStatusId = props.statuses.find((status) => status.active)?.id ?? ''
const form = reactive({
  companyId: props.position?.companyId ?? props.initialCompanyId ?? '',
  newCompanyName: '',
  title: props.position?.title ?? '',
  jobTypeId: props.position?.jobTypeId ?? '',
  statusId: props.position?.status.id ?? firstStatusId,
  applyUrl: props.position?.applyUrl ?? '',
  appliedAt: toDateInput(props.position?.appliedAt),
  deadlineAt: toDatetimeInput(props.position?.deadlineAt),
  workLocation: props.position?.workLocation ?? '',
  description: props.position?.description ?? '',
  createDeadlineSchedule: false,
})
const creatingNewCompany = ref(props.companies.length === 0 && !form.companyId)

/** PRD §12：未提交的表单内容保留到当前会话，断网刷新后可恢复并重试。 */
const draft = useFormDraft('position-form')
const savedDraft = draft.restore()
for (const field of ['title', 'companyId', 'newCompanyName', 'jobTypeId', 'statusId', 'applyUrl', 'appliedAt', 'deadlineAt', 'workLocation', 'description'] as const) {
  const value = savedDraft[field]
  // 草稿只填空位，真正的编辑数据优先
  if (typeof value === 'string' && !(form as unknown as Record<string, string>)[field]) {
    ;(form as unknown as Record<string, string>)[field] = value
  }
}
draft.startWatching(() => ({
  title: form.title, companyId: form.companyId, newCompanyName: form.newCompanyName,
  jobTypeId: form.jobTypeId, statusId: form.statusId, applyUrl: form.applyUrl,
  appliedAt: form.appliedAt, deadlineAt: form.deadlineAt, workLocation: form.workLocation,
  description: form.description,
}))
defineExpose({ clearDraft: draft.clear })

function toggleCompanyMode() {
  creatingNewCompany.value = !creatingNewCompany.value
  delete fieldErrors.value.companyId
  delete fieldErrors.value.newCompanyName
}

function submit() {
  fieldErrors.value = {}
  const useNewCompany = !props.position && creatingNewCompany.value
  const newCompanyName = form.newCompanyName.trim()
  if (useNewCompany) {
    if (!newCompanyName) fieldErrors.value.newCompanyName = '请输入新公司名称'
    else if (newCompanyName.length > 120) fieldErrors.value.newCompanyName = '公司名称不能超过 120 个字符'
  } else if (!form.companyId) {
    fieldErrors.value.companyId = '请选择所属公司'
  }
  const title = form.title.trim()
  if (!title) fieldErrors.value.title = '职位名称不能为空'
  else if (title.length > 100) fieldErrors.value.title = '职位名称不能超过 100 个字符'
  if (!form.jobTypeId) fieldErrors.value.jobTypeId = '请选择招聘类型'
  if (!form.statusId) fieldErrors.value.statusId = '请选择当前状态'
  const applyUrl = form.applyUrl.trim()
  if (applyUrl && !/^https?:\/\//.test(applyUrl)) fieldErrors.value.applyUrl = '投递链接必须以 http:// 或 https:// 开头'
  else if (applyUrl.length > 2048) fieldErrors.value.applyUrl = '投递链接不能超过 2048 个字符'
  if (Object.keys(fieldErrors.value).length > 0) return
  emit('submitted', {
    companyId: useNewCompany ? null : form.companyId,
    newCompanyName: useNewCompany ? newCompanyName : null,
    jobTypeId: form.jobTypeId,
    statusId: form.statusId,
    title,
    applyUrl: applyUrl || null,
    appliedAt: fromDateInput(form.appliedAt),
    deadlineAt: fromDatetimeInput(form.deadlineAt),
    workLocation: form.workLocation.trim() || null,
    description: form.description.trim() || null,
    createDeadlineSchedule: form.createDeadlineSchedule,
    version: props.position?.version ?? null,
  })
}
</script>

<template>
  <form novalidate @submit.prevent="submit" class="position-form ir-panel">
    <div class="ir-panel-head"><div><span class="panel-kicker">基础信息</span><h2>岗位信息</h2></div></div>
    <div class="position-form-body">
      <label for="position-company">所属公司 *</label>
    <template v-if="!position && creatingNewCompany">
      <ElInput id="position-new-company" v-model="form.newCompanyName" name="newCompanyName" maxlength="120" placeholder="输入新公司名称，保存时将自动创建" :aria-describedby="fieldErrors.newCompanyName ? 'position-new-company-error' : undefined" />
      <p v-if="fieldErrors.newCompanyName" id="position-new-company-error" data-field-error="newCompanyName" role="alert">{{ fieldErrors.newCompanyName }}</p>
    </template>
    <template v-else>
      <ElSelect id="position-company" v-model="form.companyId" name="companyId" filterable placeholder="选择公司" :aria-describedby="fieldErrors.companyId ? 'position-company-error' : undefined">
        <ElOption v-for="company in companies" :key="company.id" :label="company.name" :value="company.id" />
      </ElSelect>
      <p v-if="fieldErrors.companyId" id="position-company-error" data-field-error="companyId" role="alert">{{ fieldErrors.companyId }}</p>
    </template>
    <button v-if="!position" type="button" class="company-mode-toggle" data-action="toggle-new-company" @click="toggleCompanyMode">
      {{ creatingNewCompany ? '改为选择已有公司' : '没有公司？输入名称快速新建' }}
    </button>

    <label for="position-title">职位名称 *</label>
    <ElInput id="position-title" v-model="form.title" name="title" maxlength="100" :aria-describedby="fieldErrors.title ? 'position-title-error' : undefined" />
    <p v-if="fieldErrors.title" id="position-title-error" data-field-error="title" role="alert">{{ fieldErrors.title }}</p>

    <label for="position-job-type">招聘类型 *</label>
    <ElSelect id="position-job-type" v-model="form.jobTypeId" name="jobTypeId" placeholder="选择招聘类型" :aria-describedby="fieldErrors.jobTypeId ? 'position-job-type-error' : undefined">
      <ElOption v-for="jobType in jobTypes" :key="jobType.id" :label="jobType.active ? jobType.name : `${jobType.name}（已停用）`" :value="jobType.id" :disabled="!jobType.active && jobType.id !== form.jobTypeId" />
    </ElSelect>
    <p v-if="fieldErrors.jobTypeId" id="position-job-type-error" data-field-error="jobTypeId" role="alert">{{ fieldErrors.jobTypeId }}</p>

    <label for="position-status">当前状态 *</label>
    <ElSelect id="position-status" v-model="form.statusId" name="statusId" placeholder="选择状态" :aria-describedby="fieldErrors.statusId ? 'position-status-error' : undefined">
      <ElOption v-for="status in statuses" :key="status.id" :label="status.name" :value="status.id" :disabled="!status.active && status.id !== form.statusId" />
    </ElSelect>
    <p v-if="fieldErrors.statusId" id="position-status-error" data-field-error="statusId" role="alert">{{ fieldErrors.statusId }}</p>

    <label for="position-apply-url">投递链接（可选）</label>
    <ElInput id="position-apply-url" v-model="form.applyUrl" name="applyUrl" placeholder="https://..." :aria-describedby="fieldErrors.applyUrl ? 'position-apply-url-error' : undefined" />
    <p v-if="fieldErrors.applyUrl" id="position-apply-url-error" data-field-error="applyUrl" role="alert">{{ fieldErrors.applyUrl }}</p>

    <label for="position-applied-at">投递日期（可选）</label>
    <input id="position-applied-at" v-model="form.appliedAt" name="appliedAt" type="date" />

    <label for="position-deadline-at">截止日期（可选）</label>
    <input id="position-deadline-at" v-model="form.deadlineAt" name="deadlineAt" type="datetime-local" />
    <ElCheckbox v-if="!position" v-model="form.createDeadlineSchedule" name="createDeadlineSchedule">同时创建截止日程</ElCheckbox>

    <label for="position-work-location">工作地点（可选）</label>
    <ElInput id="position-work-location" v-model="form.workLocation" name="workLocation" maxlength="120" />

      <label for="position-description">岗位描述或备注（可选，支持富文本）</label>
      <RichTextEditor id="position-description" :model-value="form.description" placeholder="记录岗位要求、个人备注…" @update:model-value="form.description = $event ?? ''" />

      <ElButton native-type="submit" type="primary" data-action="submit-position">{{ submitLabel ?? '保存岗位' }}</ElButton>
    </div>
  </form>
</template>

<style scoped>
.position-form {
  display: flex;
  flex-direction: column;
  max-width: 100%;
}
.position-form-body {
  display: flex;
  flex-direction: column;
  gap: 9px;
  padding: 14px 18px 20px;
}
.position-form label { margin-top: 7px; color: var(--ir-text); font-size: 13px; font-weight: 700; }
.position-form p[role="alert"] { margin: -4px 0 0; color: var(--ir-urgent); font-size: 12px; }
.position-form input[type="date"], .position-form input[type="datetime-local"] {
  min-height: 32px;
  padding: 0 12px;
  border: 1px solid var(--ir-border);
  border-radius: var(--ir-radius-sm);
  color: var(--ir-text);
  background: var(--ir-surface);
  font: inherit;
}
.position-form > .el-button { width: fit-content; min-width: 126px; margin-top: 12px; }
.company-mode-toggle {
  align-self: flex-start;
  background: none;
  border: none;
  padding: 0;
  color: var(--ir-primary-strong, var(--el-color-primary));
  cursor: pointer;
  font-size: 13px;
}
.company-mode-toggle:hover {
  text-decoration: underline;
}
</style>
