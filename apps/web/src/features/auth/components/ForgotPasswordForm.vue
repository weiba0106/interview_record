<script setup lang="ts">
import { ref } from 'vue'
import { ElButton, ElInput } from 'element-plus'
import { requestPasswordReset } from '@/features/auth/api/auth.api'
import { isApiRequestError } from '@/shared/api/error'

const email = ref('')
const pending = ref(false)
const message = ref('')
const error = ref('')

async function submit() {
  pending.value = true; error.value = ''
  try { await requestPasswordReset(email.value); message.value = '如该邮箱已注册，重置邮件将很快发送。' }
  catch (caught) { error.value = isApiRequestError(caught) ? caught.apiError.message : '请求失败，请稍后重试' }
  finally { pending.value = false }
}
</script>

<template>
  <form novalidate @submit.prevent="submit">
    <p v-if="error" role="alert" tabindex="-1">{{ error }}</p>
    <p v-if="message" role="status">{{ message }}</p>
    <label for="forgot-email">邮箱</label>
    <ElInput id="forgot-email" v-model="email" name="email" type="email" autocomplete="email" />
    <ElButton native-type="submit" type="primary" :loading="pending" :disabled="pending">发送重置邮件</ElButton>
  </form>
</template>
