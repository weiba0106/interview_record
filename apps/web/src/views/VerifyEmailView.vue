<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
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
  <main aria-live="polite"><h1>邮箱验证</h1><p :role="state === 'error' ? 'alert' : 'status'">{{ message }}</p><RouterLink v-if="state !== 'loading'" to="/login">前往登录</RouterLink></main>
</template>
