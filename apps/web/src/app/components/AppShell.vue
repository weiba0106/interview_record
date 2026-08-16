<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElButton, ElIcon } from 'element-plus'
import { Calendar, DataAnalysis, Grid, OfficeBuilding, Setting, Tickets, UserFilled } from '@element-plus/icons-vue'
import { useAuthStore } from '@/shared/auth/auth.store'
import { getPreferences, updatePreferences } from '@/features/preferences/api/preferences.api'
import { applyTheme, themeLabel, themeOptions, type ThemeName } from '@/app/theme'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const mobileOpen = ref(false)
const themePickerOpen = ref(false)
const themeSaving = ref(false)
const themeError = ref('')
const themePickerRef = ref<HTMLElement | null>(null)
const links = [
  { name: 'app', label: '概览', icon: Grid },
  { name: 'positions', label: '岗位', icon: Tickets },
  { name: 'companies', label: '公司', icon: OfficeBuilding },
  { name: 'schedules', label: '日程', icon: Calendar },
  { name: 'insights', label: '统计', icon: DataAnalysis },
  { name: 'settings', label: '设置', icon: Setting },
]
const routeLabels: Record<string, string> = {
  'new-position': '新增岗位',
  'position-detail': '岗位详情',
  'edit-position': '编辑岗位',
  'company-detail': '公司详情',
  'schedule-detail': '日程详情',
}
const currentLabel = computed(() => {
  const name = typeof route.name === 'string' ? route.name : ''
  return routeLabels[name] ?? links.find((link) => link.name === route.name)?.label ?? '概览'
})
function closeMobile() { mobileOpen.value = false }
async function signOut() { await auth.logout(); await router.push({ name: 'login' }) }
function closeThemePicker() { themePickerOpen.value = false; themeError.value = '' }
function toggleThemePicker() { themePickerOpen.value = !themePickerOpen.value; themeError.value = '' }
function handleThemeKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') closeThemePicker()
}
function handleOutsideThemeClick(event: PointerEvent) {
  if (themePickerOpen.value && themePickerRef.value && !themePickerRef.value.contains(event.target as Node)) {
    closeThemePicker()
  }
}
async function selectTheme(theme: ThemeName) {
  if (!auth.user || themeSaving.value) return
  const previousTheme = auth.user.theme
  if (previousTheme === theme) { closeThemePicker(); return }

  themeError.value = ''
  themeSaving.value = true
  auth.user = { ...auth.user, theme }
  applyTheme(theme)
  try {
    const current = await getPreferences()
    const saved = await updatePreferences({ ...current, theme })
    if (auth.user) auth.user = { ...auth.user, theme: saved.theme }
    applyTheme(saved.theme)
    closeThemePicker()
  } catch {
    if (auth.user) auth.user = { ...auth.user, theme: previousTheme }
    applyTheme(previousTheme)
    themeError.value = '主题保存失败，请稍后重试'
  } finally {
    themeSaving.value = false
  }
}
watch(() => auth.user?.theme, (theme) => applyTheme(theme), { immediate: true })
onMounted(() => document.addEventListener('pointerdown', handleOutsideThemeClick))
onBeforeUnmount(() => document.removeEventListener('pointerdown', handleOutsideThemeClick))
</script>

<template>
  <div class="app-shell">
    <button v-if="mobileOpen" class="shell-backdrop" type="button" aria-label="关闭导航" @click="closeMobile" />
    <aside class="shell-sidebar" data-layout="compact" :class="{ 'is-open': mobileOpen }" aria-label="应用导航">
      <div class="shell-brand"><span class="brand-mark" aria-hidden="true">IR</span><div><strong>面试记录</strong><small>Interview Log</small></div></div>
      <nav class="shell-nav"><RouterLink v-for="link in links" :key="link.name" :to="{ name: link.name }" class="shell-nav-link" :aria-label="link.label" :title="link.label" @click="closeMobile"><ElIcon><component :is="link.icon" /></ElIcon><span>{{ link.label }}</span></RouterLink></nav>
      <div class="shell-sidebar-footer"><span class="privacy-note">数据仅对你可见</span><RouterLink :to="{ name: 'settings' }" class="profile-link" :aria-label="auth.user?.displayName || '我的账号'" :title="auth.user?.displayName || '我的账号'" @click="closeMobile"><ElIcon><UserFilled /></ElIcon><span>{{ auth.user?.displayName || '我的账号' }}</span></RouterLink></div>
    </aside>
    <div class="shell-main">
      <header class="shell-topbar">
        <div class="shell-topbar-left"><ElButton class="mobile-menu-button" text aria-label="打开导航" @click="mobileOpen = true">☰</ElButton><div><span class="eyebrow">求职进度</span><h1>{{ currentLabel }}</h1></div></div>
        <div class="shell-topbar-actions">
          <div ref="themePickerRef" class="theme-picker">
            <button
              class="theme-picker-trigger"
              type="button"
              data-action="open-theme-picker"
              aria-haspopup="listbox"
              :aria-expanded="themePickerOpen"
              :aria-label="`当前主题：${themeLabel(auth.user?.theme)}`"
              @click.stop="toggleThemePicker"
              @keydown.esc="handleThemeKeydown"
            >
              <span class="theme-trigger-swatch" aria-hidden="true" />
              <span class="theme-trigger-label">主题</span>
            </button>
            <div v-if="themePickerOpen" class="theme-picker-popover" role="listbox" aria-label="界面主题">
              <div class="theme-picker-heading"><strong>界面主题</strong><span v-if="themeSaving">保存中…</span><span v-else>自动保存</span></div>
              <button
                v-for="option in themeOptions"
                :key="option.value"
                class="theme-option"
                :class="{ active: option.value === auth.user?.theme }"
                type="button"
                role="option"
                :aria-selected="option.value === auth.user?.theme"
                :data-theme-option="option.value"
                @click="selectTheme(option.value)"
              >
                <span
                  class="theme-mini"
                  aria-hidden="true"
                  :style="{ '--mini-primary': option.palette.primary, '--mini-bg': option.palette.background, '--mini-card': option.palette.card }"
                ><i class="mini-sidebar" :style="{ background: option.palette.sidebar }" /><i class="mini-body" /></span>
                <span><strong>{{ option.label }}</strong><small>{{ option.description }}</small></span>
                <span v-if="option.value === auth.user?.theme" class="theme-check" aria-label="当前主题">✓</span>
              </button>
              <p v-if="themeError" class="theme-picker-error" role="alert">{{ themeError }}</p>
            </div>
          </div>
          <span class="user-greeting">{{ auth.user?.displayName }}</span><ElButton text @click="signOut">退出登录</ElButton>
        </div>
      </header>
      <main class="shell-content"><slot /></main>
    </div>
  </div>
</template>
