<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElButton, ElDialog } from 'element-plus'
import CompanyForm from '@/features/tracking/components/CompanyForm.vue'
import JobTypeManager from '@/features/tracking/components/JobTypeManager.vue'
import StatusManager from '@/features/tracking/components/StatusManager.vue'
import { createCompany, deleteCompany, getCompany, listCompanies, updateCompany } from '@/features/tracking/api/companies.api'
import type { Company, CompanyDetail, CompanyRequest } from '@/features/tracking/api/tracking.types'
import { isApiRequestError } from '@/shared/api/error'

const companies = ref<Company[]>([])
const loading = ref(true)
const error = ref('')
const message = ref('')

const formDialogOpen = ref(false)
const editing = ref<Company | null>(null)
const formInitial = ref<CompanyRequest | null>(null)

const duplicateDialogOpen = ref(false)
const duplicatePayload = ref<CompanyRequest | null>(null)

const deleteDialogOpen = ref(false)
const deleteDetail = ref<CompanyDetail | null>(null)
const pendingDelete = ref(false)

async function load() {
  loading.value = true
  error.value = ''
  try {
    companies.value = await listCompanies()
  } catch (caught) {
    error.value = isApiRequestError(caught) ? caught.apiError.message : '加载公司失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onMounted(() => { void load() })

function openCreate() {
  editing.value = null
  formInitial.value = null
  formDialogOpen.value = true
}

function openEdit(company: Company) {
  editing.value = company
  formInitial.value = { name: company.name, website: company.website, notes: company.notes }
  formDialogOpen.value = true
}

async function submitCompany(payload: CompanyRequest) {
  error.value = ''; message.value = ''
  try {
    if (editing.value) {
      await updateCompany(editing.value.id, payload)
      message.value = '公司已更新'
    } else {
      await createCompany(payload)
      message.value = '公司已创建'
    }
    formDialogOpen.value = false
    await load()
  } catch (caught) {
    if (isApiRequestError(caught) && caught.apiError.code === 'COMPANY_DUPLICATE' && !editing.value) {
      duplicatePayload.value = payload
      duplicateDialogOpen.value = true
      return
    }
    error.value = isApiRequestError(caught) ? caught.apiError.message : '保存失败，请稍后重试'
  }
}

async function confirmDuplicate() {
  if (!duplicatePayload.value) return
  duplicateDialogOpen.value = false
  await submitCompany({ ...duplicatePayload.value, confirmDuplicate: true })
  duplicatePayload.value = null
}

async function openDelete(company: Company) {
  error.value = ''
  try {
    deleteDetail.value = await getCompany(company.id)
    deleteDialogOpen.value = true
  } catch (caught) {
    error.value = isApiRequestError(caught) ? caught.apiError.message : '加载公司详情失败'
  }
}

async function confirmDelete() {
  if (!deleteDetail.value) return
  pendingDelete.value = true
  error.value = ''
  try {
    await deleteCompany(deleteDetail.value.id, true)
    message.value = '公司已删除'
    deleteDialogOpen.value = false
    deleteDetail.value = null
    await load()
  } catch (caught) {
    error.value = isApiRequestError(caught) ? caught.apiError.message : '删除失败，请稍后重试'
  } finally {
    pendingDelete.value = false
  }
}
</script>

<template>
  <main class="companies-page" aria-labelledby="companies-heading">
    <div class="page-head">
      <div>
        <span class="eyebrow">基础数据</span>
        <h1 id="companies-heading">公司</h1>
        <p class="page-desc">维护公司信息，并在下方管理招聘类型与岗位状态。</p>
      </div>
      <div class="page-head-actions">
        <ElButton type="primary" data-action="add-company" @click="openCreate">新增公司</ElButton>
      </div>
    </div>
    <p v-if="message" role="status">{{ message }}</p>
    <p v-if="error" role="alert">{{ error }}</p>
    <p v-if="loading" role="status">加载中…</p>

    <section v-else class="ir-panel" aria-label="公司列表">
      <div class="table-scroll">
        <table class="ir-table companies-table" v-if="companies.length > 0">
          <thead>
            <tr><th scope="col">公司名称</th><th scope="col">官网</th><th scope="col">岗位数</th><th scope="col">备注</th><th scope="col">操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="company in companies" :key="company.id" :data-testid="`company-${company.id}`">
              <td class="cell-strong">{{ company.name }}</td>
              <td><a v-if="company.website" :href="company.website" target="_blank" rel="noopener noreferrer">{{ company.website }}</a><span v-else>—</span></td>
              <td>{{ company.positionCount }}</td>
              <td class="notes-cell">{{ company.notes ?? '—' }}</td>
              <td class="actions-cell">
                <ElButton size="small" text :data-action="`edit-company-${company.id}`" @click="openEdit(company)">编辑</ElButton>
                <ElButton size="small" text type="danger" :data-action="`delete-company-${company.id}`" @click="openDelete(company)">删除</ElButton>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-else class="ir-empty" data-testid="no-companies">
          <span class="ir-empty-icon" aria-hidden="true">⌂</span>
          <strong>还没有公司</strong>
          <p>点击右上角「新增公司」创建你的第一家公司。</p>
        </div>
      </div>
    </section>

    <div class="companies-managers">
      <JobTypeManager />
      <StatusManager />
    </div>

    <ElDialog v-model="formDialogOpen" :title="editing ? '编辑公司' : '新增公司'" width="min(92vw, 520px)" :teleported="false">
      <CompanyForm :initial="formInitial" @submitted="submitCompany" />
    </ElDialog>

    <ElDialog v-model="duplicateDialogOpen" title="可能存在重复公司" width="min(92vw, 420px)" :teleported="false">
      <p>已存在同名公司「{{ duplicatePayload?.name }}」，确定要再创建一个吗？</p>
      <template #footer>
        <ElButton @click="duplicateDialogOpen = false">取消</ElButton>
        <ElButton type="primary" data-action="confirm-duplicate" @click="confirmDuplicate">确认创建</ElButton>
      </template>
    </ElDialog>

    <ElDialog :model-value="deleteDialogOpen" title="确认删除公司" width="min(92vw, 460px)" :teleported="false" @close="deleteDialogOpen = false">
      <div v-if="deleteDetail">
        <p>删除公司「{{ deleteDetail.name }}」后无法恢复，将同时删除：</p>
        <ul class="cascade-list">
          <li>{{ deleteDetail.positions.length }} 个岗位</li>
          <li>{{ deleteDetail.interviewRoundCount }} 条面试轮次记录</li>
          <li>{{ deleteDetail.scheduleCount }} 条关联日程</li>
        </ul>
      </div>
      <template #footer>
        <ElButton @click="deleteDialogOpen = false">取消</ElButton>
        <ElButton type="danger" data-action="confirm-delete-company" :loading="pendingDelete" :disabled="!deleteDetail" @click="confirmDelete">永久删除</ElButton>
      </template>
    </ElDialog>
  </main>
</template>

<style scoped>
.companies-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.companies-managers {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.4fr);
  gap: 14px;
  align-items: start;
}
.companies-table { min-width: 640px; }
.companies-table th { padding-top: 11px; padding-bottom: 11px; }
.notes-cell {
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.actions-cell {
  display: flex;
  gap: 8px;
}
.cascade-list {
  margin: 8px 0;
  padding-left: 20px;
}
@media (max-width: 960px) {
  .companies-managers { grid-template-columns: 1fr; }
}
</style>
