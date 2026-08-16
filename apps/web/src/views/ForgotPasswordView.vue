<script setup lang="ts">
import AuthLayout from '@/features/auth/components/AuthLayout.vue'
import ForgotPasswordForm from '@/features/auth/components/ForgotPasswordForm.vue'
</script>

<template>
  <AuthLayout heading-id="forgot-heading" eyebrow="找回账号" title="重置密码" description="输入注册邮箱，我们会发送一封重置邮件。">
    <ForgotPasswordForm />
    <template #footer><div class="auth-links"><RouterLink to="/login">返回登录</RouterLink></div></template>
  </AuthLayout>
</template>
