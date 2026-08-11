<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElButton, ElInput } from 'element-plus'
import { resetPassword } from '@/features/auth/api/auth.api'
import { isApiRequestError } from '@/shared/api/error'

const props = defineProps<{ token: string }>()
const emit = defineEmits<{ submitted: [] }>()
const form = reactive({ newPassword: '', confirmPassword: '' })
const pending = ref(false)
const message = ref('')

async function submit() {
  if (form.newPassword !== form.confirmPassword) { message.value = '两次输入的密码不一致'; return }
  pending.value = true; message.value = ''
  try {
    await resetPassword(props.token, form.newPassword)
    form.newPassword = ''; form.confirmPassword = ''
    emit('submitted')
  } catch (error) { message.value = isApiRequestError(error) ? error.apiError.message : '重置失败，请稍后重试' }
  finally { pending.value = false }
}
</script>

<template>
  <form novalidate @submit.prevent="submit">
    <p v-if="message" role="alert" tabindex="-1">{{ message }}</p>
    <label for="reset-password">新密码</label>
    <ElInput id="reset-password" v-model="form.newPassword" name="newPassword" type="password" autocomplete="new-password" show-password />
    <label for="reset-password-confirm">确认新密码</label>
    <ElInput id="reset-password-confirm" v-model="form.confirmPassword" name="confirmPassword" type="password" autocomplete="new-password" show-password />
    <ElButton native-type="submit" type="primary" :loading="pending" :disabled="pending">重置密码</ElButton>
  </form>
</template>
