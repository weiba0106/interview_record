<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import AuthLayout from '@/features/auth/components/AuthLayout.vue'
import ResetPasswordForm from '@/features/auth/components/ResetPasswordForm.vue'

const route = useRoute()
const token = ref(typeof route.query.token === 'string' ? route.query.token : '')
const reset = ref(false)
onMounted(() => { if (token.value) window.history.replaceState(null, '', route.path) })
</script>

<template>
  <AuthLayout heading-id="reset-heading" eyebrow="账号安全" title="设置新密码" description="设置完成后，其他已登录会话将失效。">
    <p v-if="!token" class="auth-alert" role="alert">重置链接无效或已失效</p>
    <div v-else-if="reset" class="auth-success" role="status"><span class="auth-success-icon" aria-hidden="true">✓</span><p>密码已重置，请使用新密码登录。</p><RouterLink to="/login">前往登录</RouterLink></div>
    <ResetPasswordForm v-else :token="token" @submitted="reset = true" />
    <template #footer><div class="auth-links"><RouterLink to="/login">返回登录</RouterLink></div></template>
  </AuthLayout>
</template>
