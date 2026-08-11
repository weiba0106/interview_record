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

async function submit() {
  pending.value = true; message.value = ''
  try { await auth.login(form); emit('submitted') }
  catch (error) { message.value = isApiRequestError(error) ? error.apiError.message : '登录失败，请稍后重试' }
  finally { pending.value = false }
}
</script>

<template>
  <form novalidate @submit.prevent="submit">
    <p v-if="message" role="alert" tabindex="-1">{{ message }}</p>
    <label for="login-email">邮箱</label>
    <ElInput id="login-email" v-model="form.email" name="email" type="email" autocomplete="email" />
    <label for="login-password">密码</label>
    <ElInput id="login-password" v-model="form.password" name="password" type="password" autocomplete="current-password" show-password />
    <ElButton native-type="submit" type="primary" :loading="pending" :disabled="pending">登录</ElButton>
  </form>
</template>
