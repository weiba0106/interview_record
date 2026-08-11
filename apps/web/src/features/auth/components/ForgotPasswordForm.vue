<script setup lang="ts">
import { ref } from 'vue'
import { ElButton, ElInput } from 'element-plus'
import { requestPasswordReset } from '@/features/auth/api/auth.api'
import { isApiRequestError } from '@/shared/api/error'

const email = ref('')
const pending = ref(false)
const message = ref('')
const error = ref('')
const fieldErrors = ref<Record<string, string>>({})

async function submit() {
  pending.value = true; error.value = ''; fieldErrors.value = {}
  try { await requestPasswordReset(email.value); message.value = '如该邮箱已注册，重置邮件将很快发送。' }
  catch (caught) {
    if (isApiRequestError(caught)) { error.value = caught.apiError.message; fieldErrors.value = caught.apiError.fieldErrors }
    else error.value = '请求失败，请稍后重试'
  }
  finally { pending.value = false }
}
</script>

<template>
  <form novalidate @submit.prevent="submit">
    <p v-if="error" role="alert" tabindex="-1">{{ error }}</p>
    <p v-if="message" role="status">{{ message }}</p>
    <label for="forgot-email">邮箱</label>
    <ElInput id="forgot-email" v-model="email" name="email" type="email" autocomplete="email" :aria-describedby="fieldErrors.email ? 'forgot-email-error' : undefined" />
    <p v-if="fieldErrors.email" id="forgot-email-error" data-field-error="email" role="alert">{{ fieldErrors.email }}</p>
    <ElButton native-type="submit" type="primary" :loading="pending" :disabled="pending">发送重置邮件</ElButton>
  </form>
</template>
