<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ElButton } from 'element-plus'
import { useAuthStore } from '@/shared/auth/auth.store'

const auth = useAuthStore()
const router = useRouter()

async function signOut() { await auth.logout(); await router.push({ name: 'login' }) }
</script>

<template>
  <main aria-labelledby="dashboard-heading"><h1 id="dashboard-heading">面试记录</h1><p v-if="auth.user">欢迎回来，{{ auth.user.displayName }}</p><ElButton @click="signOut">退出登录</ElButton></main>
</template>
