<script setup lang="ts">
import { ref } from 'vue'
import RegisterForm from '@/features/auth/components/RegisterForm.vue'

const registered = ref(false)
</script>

<template>
  <main aria-labelledby="register-heading">
    <template v-if="registered">
      <h1 id="register-heading">请查收邮箱</h1>
      <p role="status">注册成功。请打开验证邮件以激活账号。</p>
      <RouterLink to="/login">返回登录</RouterLink>
    </template>
    <template v-else>
      <h1 id="register-heading">注册</h1>
      <RegisterForm @submitted="registered = true" />
      <RouterLink to="/login">已有账号？登录</RouterLink>
    </template>
  </main>
</template>
