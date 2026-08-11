<script setup lang="ts">
import ForgotPasswordForm from '@/features/auth/components/ForgotPasswordForm.vue'
</script>

<template>
  <main aria-labelledby="forgot-heading"><h1 id="forgot-heading">重置密码</h1><ForgotPasswordForm /><RouterLink to="/login">返回登录</RouterLink></main>
</template>
