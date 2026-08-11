<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElButton, ElDialog, ElInput } from 'element-plus'
import PreferenceForm from '@/features/preferences/components/PreferenceForm.vue'
import { deleteAccount, updatePreferences, type Preferences } from '@/features/preferences/api/preferences.api'
import { isApiRequestError } from '@/shared/api/error'
import { useAuthStore } from '@/shared/auth/auth.store'

const auth = useAuthStore()
const router = useRouter()
const deleteDialogOpen = ref(false)
const deletePassword = ref('')
const pendingDeletion = ref(false)
const message = ref('')
const error = ref('')
const preferences = computed<Preferences>(() => ({
  displayName: auth.user?.displayName ?? '',
  timeZone: auth.user?.timeZone ?? 'Asia/Shanghai',
  theme: auth.user?.theme ?? 'GRAPHITE_CORAL',
  interviewReminderOffsets: [1440, 30],
  deadlineReminderOffsets: [1440],
}))

async function savePreferences(next: Preferences) {
  error.value = ''; message.value = ''
  try {
    await updatePreferences(next)
    await auth.loadCurrentUser()
    message.value = '偏好已保存'
  } catch (caught) {
    error.value = isApiRequestError(caught) ? caught.apiError.message : '保存失败，请稍后重试'
  }
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
  <main aria-labelledby="settings-heading">
    <h1 id="settings-heading">账号设置</h1>
    <p v-if="message" role="status">{{ message }}</p>
    <p v-if="error" role="alert">{{ error }}</p>
    <PreferenceForm :preferences="preferences" @submitted="savePreferences" />

    <section aria-labelledby="delete-account-heading">
      <h2 id="delete-account-heading">删除账号</h2>
      <p>删除后无法恢复，请先导出需要保留的数据。</p>
      <ElButton data-action="open-delete-dialog" type="danger" @click="openDeletionDialog">删除账号</ElButton>
    </section>
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
