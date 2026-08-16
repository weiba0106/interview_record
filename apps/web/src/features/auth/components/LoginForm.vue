<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElButton, ElInput } from 'element-plus'
import { isApiRequestError } from '@/shared/api/error'
import { useAuthStore } from '@/shared/auth/auth.store'

const emit = defineEmits<{ submitted: [] }>()
const auth = useAuthStore()
const form = reactive({ email: '', password: '' })
const pending = ref(false)
const message = ref('')
const fieldErrors = ref<Record<string, string>>({})

async function submit() {
  pending.value = true; message.value = ''; fieldErrors.value = {}
  try { await auth.login(form); emit('submitted') }
  catch (error) {
    if (isApiRequestError(error)) { message.value = error.apiError.message; fieldErrors.value = error.apiError.fieldErrors }
    else message.value = '登录失败，请稍后重试'
  }
  finally { pending.value = false }
}
</script>

<template>
  <form class="auth-form" novalidate @submit.prevent="submit">
    <p v-if="message" class="auth-alert" role="alert" tabindex="-1">{{ message }}</p>
    <label for="login-email">邮箱</label>
    <ElInput id="login-email" v-model="form.email" name="email" type="email" autocomplete="email" placeholder="请输入邮箱地址" :aria-describedby="fieldErrors.email ? 'login-email-error' : undefined" />
    <p v-if="fieldErrors.email" id="login-email-error" class="auth-field-error" data-field-error="email" role="alert">{{ fieldErrors.email }}</p>
    <label for="login-password">密码</label>
    <ElInput id="login-password" v-model="form.password" name="password" type="password" autocomplete="current-password" placeholder="请输入密码" show-password :aria-describedby="fieldErrors.password ? 'login-password-error' : undefined" />
    <p v-if="fieldErrors.password" id="login-password-error" class="auth-field-error" data-field-error="password" role="alert">{{ fieldErrors.password }}</p>
    <ElButton class="auth-submit" native-type="submit" type="primary" :loading="pending" :disabled="pending">登录</ElButton>
  </form>
</template>
