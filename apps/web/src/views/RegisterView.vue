<script setup lang="ts">
import { onBeforeUnmount, ref } from 'vue'
import AuthLayout from '@/features/auth/components/AuthLayout.vue'
import RegisterForm from '@/features/auth/components/RegisterForm.vue'
import { resendVerification } from '@/features/auth/api/auth.api'
import { isApiRequestError } from '@/shared/api/error'

const registered = ref(false)
const registeredEmail = ref('')
const resendCooldown = ref(0)
const resending = ref(false)
const resendMessage = ref('')
const resendError = ref('')
let cooldownTimer: ReturnType<typeof setInterval> | undefined

function onRegistered(email: string) {
  registered.value = true
  registeredEmail.value = email
  startCooldown()
}

function startCooldown() {
  resendCooldown.value = 60
  cooldownTimer = setInterval(() => {
    if (resendCooldown.value > 0) resendCooldown.value -= 1
    else if (cooldownTimer) { clearInterval(cooldownTimer); cooldownTimer = undefined }
  }, 1000)
}

async function resend() {
  if (resending.value || resendCooldown.value > 0) return
  resending.value = true
  resendError.value = ''
  resendMessage.value = ''
  try {
    await resendVerification(registeredEmail.value)
    resendMessage.value = '验证邮件已重新发送，请查收邮箱'
    startCooldown()
  } catch (caught) {
    resendError.value = isApiRequestError(caught) ? caught.apiError.message : '重发失败，请稍后重试'
  } finally {
    resending.value = false
  }
}

onBeforeUnmount(() => {
  if (cooldownTimer) clearInterval(cooldownTimer)
})
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
      <p>请打开注册邮箱中的验证链接激活账号。没有收到邮件？可以在 {{ resendCooldown > 0 ? `${resendCooldown} 秒后` : '现在' }}重新发送。</p>
      <div class="resend-row">
        <button type="button" class="resend-button" data-action="resend-verification" :disabled="resending || resendCooldown > 0" @click="resend">
          {{ resending ? '发送中…' : resendCooldown > 0 ? `重新发送（${resendCooldown}s）` : '重新发送验证邮件' }}
        </button>
        <RouterLink to="/login">返回登录</RouterLink>
      </div>
      <p v-if="resendMessage" role="status" class="resend-status">{{ resendMessage }}</p>
      <p v-if="resendError" role="alert" class="resend-status">{{ resendError }}</p>
    </div>
    <template v-else>
      <RegisterForm @submitted="onRegistered" />
    </template>
    <template #footer>
      <div v-if="!registered" class="auth-links"><span>已经有账号？</span><RouterLink to="/login">返回登录</RouterLink></div>
    </template>
  </AuthLayout>
</template>

<style scoped>
.resend-row { display: flex; align-items: center; gap: 14px; flex-wrap: wrap; }
.resend-button {
  display: inline-flex;
  align-items: center;
  min-height: 42px;
  padding: 0 18px;
  border: 0;
  border-radius: var(--ir-radius-sm);
  color: var(--ir-primary-contrast);
  background: var(--ir-primary-strong);
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}
.resend-button:disabled { opacity: .55; cursor: not-allowed; }
.resend-status { margin: 0; font-size: 13px; }
</style>
