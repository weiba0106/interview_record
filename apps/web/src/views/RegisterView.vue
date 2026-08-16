<script setup lang="ts">
import { ref } from 'vue'
import AuthLayout from '@/features/auth/components/AuthLayout.vue'
import RegisterForm from '@/features/auth/components/RegisterForm.vue'

const registered = ref(false)
</script>

<template>
  <AuthLayout
    heading-id="register-heading"
    :eyebrow="registered ? '注册成功' : '开始记录'"
    :title="registered ? '验证邮件已发送' : '创建你的求职记录空间'"
    :description="registered ? '完成邮箱验证后，就可以登录并开始管理岗位与面试。' : '只需要邮箱即可注册，你的所有求职数据默认保持私密。'"
  >
    <div v-if="registered" class="auth-success" role="status">
      <span class="auth-success-icon" aria-hidden="true">✓</span>
      <p>请打开注册邮箱中的验证链接激活账号。验证完成后返回这里登录。</p>
      <RouterLink to="/login">返回登录</RouterLink>
    </div>
    <template v-else>
      <RegisterForm @submitted="registered = true" />
    </template>
    <template #footer>
      <div v-if="!registered" class="auth-links"><span>已经有账号？</span><RouterLink to="/login">返回登录</RouterLink></div>
    </template>
  </AuthLayout>
</template>
