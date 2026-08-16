<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AuthLayout from '@/features/auth/components/AuthLayout.vue'
import LoginForm from '@/features/auth/components/LoginForm.vue'

const router = useRouter()
const route = useRoute()
const sessionExpired = computed(() => route.query.expired === '1')
</script>

<template>
  <AuthLayout
    heading-id="login-heading"
    eyebrow="欢迎回来"
    title="继续你的求职进度"
    description="登录后查看岗位进展、面试记录和即将到来的日程。"
  >
    <p v-if="sessionExpired" class="auth-alert" role="alert" tabindex="-1">会话已过期，请重新登录。</p>
    <LoginForm @submitted="router.push({ name: 'app' })" />
    <template #footer>
      <div class="auth-links">
        <RouterLink to="/forgot-password">忘记密码？</RouterLink>
        <RouterLink to="/register">还没有账号？立即注册</RouterLink>
      </div>
    </template>
  </AuthLayout>
</template>
