<script setup lang="ts">
import { useRouter } from 'vue-router'
import LoginForm from '@/features/auth/components/LoginForm.vue'

const router = useRouter()
</script>

<template>
  <main aria-labelledby="login-heading">
    <h1 id="login-heading">登录</h1>
    <LoginForm @submitted="router.push({ name: 'app' })" />
    <RouterLink to="/forgot-password">忘记密码？</RouterLink>
    <RouterLink to="/register">还没有账号？注册</RouterLink>
  </main>
</template>
