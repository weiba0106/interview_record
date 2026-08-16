<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import AuthLayout from '@/features/auth/components/AuthLayout.vue'
import { verifyEmail } from '@/features/auth/api/auth.api'
import { isApiRequestError } from '@/shared/api/error'

const route = useRoute()
const state = ref<'loading' | 'success' | 'error'>('loading')
const message = ref('正在验证邮箱…')

onMounted(async () => {
  const token = typeof route.query.token === 'string' ? route.query.token : ''
  if (!token) { state.value = 'error'; message.value = '验证链接无效或已失效'; return }
  window.history.replaceState(null, '', route.path)
  try { await verifyEmail(token); state.value = 'success'; message.value = '邮箱验证成功' }
  catch (error) { state.value = 'error'; message.value = isApiRequestError(error) ? error.apiError.message : '验证失败，请稍后重试' }
})
</script>

<template>
  <AuthLayout heading-id="verify-heading" eyebrow="账号验证" title="邮箱验证" description="验证成功后即可登录并开始记录求职进度。">
    <div class="auth-success" aria-live="polite">
      <span class="auth-success-icon" aria-hidden="true">{{ state === 'loading' ? '…' : state === 'success' ? '✓' : '!' }}</span>
      <p :class="{ 'auth-alert': state === 'error' }" :role="state === 'error' ? 'alert' : 'status'">{{ message }}</p>
      <RouterLink v-if="state !== 'loading'" to="/login">前往登录</RouterLink>
    </div>
  </AuthLayout>
</template>
