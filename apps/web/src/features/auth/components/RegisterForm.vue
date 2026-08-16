<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElButton, ElInput } from 'element-plus'
import { register } from '@/features/auth/api/auth.api'
import { isApiRequestError } from '@/shared/api/error'

const emit = defineEmits<{ submitted: [] }>()
const defaultTimeZone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Shanghai'
const form = reactive({ email: '', password: '', displayName: '', timeZone: defaultTimeZone })
const pending = ref(false)
const message = ref('')
const fieldErrors = ref<Record<string, string>>({})

async function submit() {
  pending.value = true; message.value = ''; fieldErrors.value = {}
  try { await register(form); emit('submitted') }
  catch (error) {
    if (isApiRequestError(error)) { message.value = error.apiError.message; fieldErrors.value = error.apiError.fieldErrors }
    else message.value = '注册失败，请稍后重试'
  } finally { pending.value = false }
}
</script>

<template>
  <form class="auth-form" novalidate @submit.prevent="submit">
    <p v-if="message" class="auth-alert" role="alert" tabindex="-1">{{ message }}</p>
    <label for="register-email">邮箱</label>
    <ElInput id="register-email" v-model="form.email" name="email" type="email" autocomplete="email" placeholder="用于登录和接收验证邮件" :aria-describedby="fieldErrors.email ? 'register-email-error' : undefined" />
    <p v-if="fieldErrors.email" id="register-email-error" class="auth-field-error" data-field-error="email" role="alert">{{ fieldErrors.email }}</p>
    <label for="register-password">密码</label>
    <ElInput id="register-password" v-model="form.password" name="password" type="password" autocomplete="new-password" placeholder="设置一个安全密码" show-password :aria-describedby="fieldErrors.password ? 'register-password-error' : undefined" />
    <p v-if="fieldErrors.password" id="register-password-error" class="auth-field-error" data-field-error="password" role="alert">{{ fieldErrors.password }}</p>
    <label for="register-display-name">显示名称</label>
    <ElInput id="register-display-name" v-model="form.displayName" name="displayName" autocomplete="name" placeholder="例如：尾巴" :aria-describedby="fieldErrors.displayName ? 'register-display-name-error' : undefined" />
    <p v-if="fieldErrors.displayName" id="register-display-name-error" class="auth-field-error" data-field-error="displayName" role="alert">{{ fieldErrors.displayName }}</p>
    <ElButton class="auth-submit" native-type="submit" type="primary" :loading="pending" :disabled="pending">创建账号</ElButton>
  </form>
</template>
