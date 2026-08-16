<script setup lang="ts">
import { onMounted, ref, useTemplateRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElButton, ElDialog } from 'element-plus'
import CompanyForm from '@/features/tracking/components/CompanyForm.vue'
import { getCompany, updateCompany } from '@/features/tracking/api/companies.api'
import type { CompanyDetail, CompanyRequest } from '@/features/tracking/api/tracking.types'
import { isApiRequestError } from '@/shared/api/error'
import { formatDate, formatDateTime } from '@/shared/format/datetime'

const route = useRoute()
const router = useRouter()
const companyId = typeof route.params.id === 'string' ? route.params.id : ''

const loading = ref(true)
const error = ref('')
const message = ref('')
const company = ref<CompanyDetail | null>(null)
const editDialogOpen = ref(false)
const editing = ref(false)
const companyFormRef = useTemplateRef<InstanceType<typeof CompanyForm>>('company-form')

async function load() {
  loading.value = true
  error.value = ''
  try {
    company.value = await getCompany(companyId)
  } catch (caught) {
    error.value = isApiRequestError(caught) ? caught.apiError.message : '加载公司详情失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onMounted(() => { void load() })

function openEdit() {
  if (!company.value) return
  editing.value = false
  editDialogOpen.value = true
}

async function submitCompany(payload: CompanyRequest) {
  if (!company.value) return
  error.value = ''; message.value = ''
  editing.value = true
  try {
    const updated = await updateCompany(company.value.id, payload)
    company.value = { ...company.value, name: updated.name, website: updated.website, notes: updated.notes }
    companyFormRef.value?.clearDraft()
    editDialogOpen.value = false
    message.value = '公司已更新'
  } catch (caught) {
    error.value = isApiRequestError(caught) ? caught.apiError.message : '保存失败，请稍后重试'
  } finally {
    editing.value = false
  }
}
</script>

<template>
  <main class="company-detail page-center" aria-labelledby="company-detail-heading">
    <div class="page-head">
      <div>
        <span class="eyebrow">基础数据</span>
        <h1 id="company-detail-heading">{{ company?.name ?? '公司详情' }}</h1>
        <p v-if="company?.website" class="page-desc">
          <a :href="company.website" target="_blank" rel="noopener noreferrer">{{ company.website }}</a>
        </p>
      </div>
      <div class="page-head-actions">
        <ElButton data-action="edit-company" :disabled="!company" @click="openEdit">编辑公司</ElButton>
        <RouterLink :to="{ name: 'new-position', query: { company: companyId } }"><ElButton type="primary" data-action="create-position-for-company">+ 新增岗位</ElButton></RouterLink>
      </div>
    </div>
    <p v-if="message" role="status">{{ message }}</p>
    <p v-if="error" role="alert">{{ error }}</p>
    <p v-if="loading" role="status">加载中…</p>

    <template v-if="!loading && company">
      <section class="ir-panel" aria-label="公司信息">
        <div class="ir-panel-head"><div><span class="panel-kicker">公司信息</span><h2>概览</h2></div></div>
        <div class="info-grid">
          <div class="info-item"><span class="info-label">公司名称</span><span class="cell-strong">{{ company.name }}</span></div>
          <div class="info-item">
            <span class="info-label">官网</span>
            <a v-if="company.website" :href="company.website" target="_blank" rel="noopener noreferrer">{{ company.website }}</a>
            <span v-else>—</span>
          </div>
          <div class="info-item"><span class="info-label">创建时间</span><span>{{ formatDateTime(company.createdAt) }}</span></div>
          <div class="info-item"><span class="info-label">岗位数量</span><span>{{ company.positionCount }}</span></div>
          <div class="info-item"><span class="info-label">面试轮次</span><span>{{ company.interviewRoundCount }}</span></div>
          <div class="info-item"><span class="info-label">关联日程</span><span>{{ company.scheduleCount }}</span></div>
          <div class="info-item"><span class="info-label">分享链接</span><span>{{ company.shareLinkCount }}</span></div>
          <div class="info-item info-item-wide"><span class="info-label">备注</span><span>{{ company.notes ?? '—' }}</span></div>
        </div>
      </section>

      <section class="ir-panel" aria-labelledby="company-positions-heading">
        <div class="ir-panel-head"><div><span class="panel-kicker">全部岗位</span><h2 id="company-positions-heading">岗位列表</h2></div></div>
        <div v-if="company.positions.length > 0" class="table-scroll">
          <table class="ir-table">
            <thead>
              <tr><th scope="col">职位</th><th scope="col">招聘类型</th><th scope="col">当前状态</th><th scope="col">投递日期</th><th scope="col">截止日期</th><th scope="col">操作</th></tr>
            </thead>
            <tbody>
              <tr v-for="position in company.positions" :key="position.id">
                <td class="cell-strong">
                  <RouterLink :to="{ name: 'position-detail', params: { id: position.id } }">{{ position.title }}</RouterLink>
                  <span v-if="position.archived" class="archived-mark">已归档</span>
                </td>
                <td>{{ position.jobTypeName }}</td>
                <td><span class="status-pill" :style="{ '--pill': position.status.color }">{{ position.status.name }}</span></td>
                <td>{{ formatDate(position.appliedAt) }}</td>
                <td>{{ formatDate(position.deadlineAt) }}</td>
                <td class="actions-cell">
                  <RouterLink class="row-action" :to="{ name: 'position-detail', params: { id: position.id } }">详情</RouterLink>
                  <RouterLink class="row-action" :to="{ name: 'edit-position', params: { id: position.id } }">编辑</RouterLink>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="ir-empty">
          <span class="ir-empty-icon" aria-hidden="true">⌂</span>
          <strong>该公司还没有岗位</strong>
          <p>点击右上角「新增岗位」开始记录投递。</p>
        </div>
      </section>

      <ElDialog v-model="editDialogOpen" title="编辑公司" width="min(92vw, 520px)" :teleported="false">
        <CompanyForm
          v-if="company"
          ref="company-form"
          :initial="{ name: company.name, website: company.website, notes: company.notes }"
          :submit-label="editing ? '保存中…' : '保存公司'"
          @submitted="submitCompany"
        />
      </ElDialog>
    </template>
  </main>
</template>

<style scoped>
.company-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 1000px;
}
.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 14px 18px;
  padding: 14px 16px 16px;
}
.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13.5px;
}
.info-item-wide { grid-column: 1 / -1; }
.info-label { color: var(--ir-muted); font-size: 12px; }
.archived-mark {
  margin-left: 6px;
  color: var(--ir-faint);
  font-size: 11px;
  font-weight: 600;
}
</style>
