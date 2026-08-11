<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import ResetPasswordForm from '@/features/auth/components/ResetPasswordForm.vue'

const route = useRoute()
const token = computed(() => typeof route.query.token === 'string' ? route.query.token : '')
const reset = ref(false)
</script>

<template>
  <main aria-labelledby="reset-heading">
    <h1 id="reset-heading">设置新密码</h1>
    <p v-if="!token" role="alert">重置链接无效或已失效</p>
    <template v-else-if="reset"><p role="status">密码已重置，请使用新密码登录。</p><RouterLink to="/login">前往登录</RouterLink></template>
    <ResetPasswordForm v-else :token="token" @submitted="reset = true" />
  </main>
</template>
