<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElButton, ElDialog, ElInput } from 'element-plus'
import PreferenceForm from '@/features/preferences/components/PreferenceForm.vue'
import { deleteAccount, getPreferences, updatePreferences, type Preferences } from '@/features/preferences/api/preferences.api'
import { isApiRequestError } from '@/shared/api/error'
import { useAuthStore } from '@/shared/auth/auth.store'
import { downloadJsonExport } from '@/features/export/api/export.api'

const auth = useAuthStore()
const router = useRouter()
const deleteDialogOpen = ref(false)
const deletePassword = ref('')
const pendingDeletion = ref(false)
const message = ref('')
const error = ref('')
const exporting = ref(false)
const savedPreferences = ref<Preferences | null>(null)
const preferences = computed<Preferences>(() => savedPreferences.value ?? ({
  displayName: auth.user?.displayName ?? '',
  timeZone: auth.user?.timeZone ?? 'Asia/Shanghai',
  theme: auth.user?.theme ?? 'GRAPHITE_CORAL',
  interviewReminderOffsets: [1440, 30],
  deadlineReminderOffsets: [1440],
}))

function applySavedPreferences(saved: Preferences) {
  savedPreferences.value = saved
  if (auth.user) auth.user = { ...auth.user, displayName: saved.displayName, timeZone: saved.timeZone, theme: saved.theme }
}

async function loadPreferences() {
  try {
    applySavedPreferences(await getPreferences())
  } catch (caught) {
    error.value = isApiRequestError(caught) ? caught.apiError.message : '加载偏好失败，请稍后重试'
  }
}

onMounted(() => { void loadPreferences() })

async function savePreferences(next: Preferences) {
  error.value = ''; message.value = ''
  try {
    applySavedPreferences(await updatePreferences(next))
    message.value = '偏好已保存'
  } catch (caught) {
    error.value = isApiRequestError(caught) ? caught.apiError.message : '保存失败，请稍后重试'
  }
}

async function exportData() {
  exporting.value = true; error.value = ''
  try { await downloadJsonExport(); message.value = '数据导出已开始下载' }
  catch (caught) { error.value = isApiRequestError(caught) ? caught.apiError.message : '导出失败，请稍后重试' }
  finally { exporting.value = false }
}

function openDeletionDialog() {
  deletePassword.value = ''
  error.value = ''
  deleteDialogOpen.value = true
}

async function confirmDeletion() {
  pendingDeletion.value = true; error.value = ''
  try {
    await deleteAccount(deletePassword.value)
    try { await auth.logout() } catch { /* the account deletion endpoint already invalidated this session */ }
    await router.replace({ name: 'login' })
  } catch (caught) {
    error.value = isApiRequestError(caught) ? caught.apiError.message : '删除账号失败，请稍后重试'
  } finally {
    pendingDeletion.value = false
  }
}
</script>

<template>
  <main class="settings-page" aria-labelledby="settings-heading">
    <div class="page-head">
      <div>
        <span class="eyebrow">个人偏好</span>
        <h1 id="settings-heading">设置</h1>
        <p class="page-desc">管理显示名、时区、主题、提醒与账号数据。</p>
      </div>
    </div>
    <p v-if="message" role="status">{{ message }}</p>
    <p v-if="error" role="alert">{{ error }}</p>

    <div class="settings-grid">
      <PreferenceForm :preferences="preferences" @submitted="savePreferences" />

      <div class="settings-side">
        <section class="ir-panel" aria-labelledby="export-heading">
          <div class="ir-panel-head"><div><span class="panel-kicker">数据所有权</span><h2 id="export-heading">数据备份</h2></div></div>
          <div class="settings-card-body">
            <p>下载包含公司、岗位、面试、日程和偏好的 JSON 备份，不包含密码、会话和令牌。</p>
            <ElButton data-action="export-json" :loading="exporting" @click="exportData">导出 JSON</ElButton>
          </div>
        </section>

        <section class="ir-panel danger-card" aria-labelledby="delete-account-heading">
          <div class="ir-panel-head"><div><span class="panel-kicker">危险操作</span><h2 id="delete-account-heading">删除账号</h2></div></div>
          <div class="settings-card-body">
            <p>删除后无法恢复，请先导出需要保留的数据。</p>
            <ElButton data-action="open-delete-dialog" type="danger" plain @click="openDeletionDialog">删除账号</ElButton>
          </div>
        </section>
      </div>
    </div>

    <ElDialog v-model="deleteDialogOpen" title="确认删除账号" width="min(92vw, 460px)" :teleported="false">
      <p>此操作会永久删除你的求职记录和设置。</p>
      <label for="delete-account-password">输入密码确认</label>
      <ElInput id="delete-account-password" v-model="deletePassword" name="deletePassword" type="password" autocomplete="current-password" show-password />
      <template #footer>
        <ElButton @click="deleteDialogOpen = false">取消</ElButton>
        <ElButton data-action="delete-account" type="danger" :disabled="!deletePassword || pendingDeletion" :loading="pendingDeletion" @click="confirmDeletion">永久删除</ElButton>
      </template>
    </ElDialog>
  </main>
</template>

<style scoped>
.settings-page { display: flex; flex-direction: column; gap: 16px; }
.settings-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(0, 1fr);
  gap: 14px;
  align-items: start;
}
.settings-side { display: grid; gap: 14px; }
.settings-card-body { padding: 14px 18px 18px; display: grid; gap: 10px; justify-items: start; }
.settings-card-body p { margin: 0; color: var(--ir-muted); line-height: 1.65; font-size: 13px; }
.danger-card { border-color: color-mix(in srgb, var(--ir-danger), transparent 68%); }
.danger-card .panel-kicker { color: var(--ir-danger); }
@media (max-width: 960px) {
  .settings-grid { grid-template-columns: 1fr; }
}
</style>
