<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ElButton } from 'element-plus'
import { useAuthStore } from '@/shared/auth/auth.store'

const auth = useAuthStore()
const router = useRouter()

async function signOut() {
  await auth.logout()
  await router.push({ name: 'login' })
}
</script>

<template>
  <header class="app-header">
    <nav aria-label="主导航" class="app-nav">
      <RouterLink :to="{ name: 'app' }">概览</RouterLink>
      <RouterLink :to="{ name: 'positions' }">岗位</RouterLink>
      <RouterLink :to="{ name: 'companies' }">公司</RouterLink>
      <RouterLink :to="{ name: 'schedules' }">日程</RouterLink>
      <RouterLink :to="{ name: 'settings' }">设置</RouterLink>
    </nav>
    <div class="app-header-user">
      <span v-if="auth.user">{{ auth.user.displayName }}</span>
      <ElButton size="small" data-action="sign-out" @click="signOut">退出登录</ElButton>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 20px;
  background: var(--ir-surface);
  border-bottom: 1px solid var(--el-border-color-lighter, #ebeef5);
  flex-wrap: wrap;
}
.app-nav {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}
.app-nav a {
  color: var(--ir-muted);
  text-decoration: none;
  padding: 4px 8px;
  border-radius: 6px;
}
.app-nav a.router-link-active {
  color: var(--ir-primary);
  font-weight: 600;
}
.app-header-user {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
