<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { applyDark } from '@/app/theme'

withDefaults(defineProps<{
  headingId?: string
  eyebrow: string
  title: string
  description: string
}>(), {
  headingId: 'auth-heading',
})

const DARK_STORAGE_KEY = 'interview-record.dark'
const darkMode = ref(false)

function rememberDark(dark: boolean) {
  try { localStorage.setItem(DARK_STORAGE_KEY, dark ? '1' : '0') } catch { /* 存储不可用时降级 */ }
}

/** 未登录页面同样遵循暗色偏好：读取本地镜像立即生效，切换即写回。 */
onMounted(() => {
  try {
    darkMode.value = localStorage.getItem(DARK_STORAGE_KEY) === '1'
    applyDark(darkMode.value)
  } catch { /* 存储不可用时保持浅色 */ }
})

function toggleDark() {
  darkMode.value = !darkMode.value
  applyDark(darkMode.value)
  rememberDark(darkMode.value)
}
</script>

<template>
  <main class="auth-shell" :aria-labelledby="headingId">
    <button
      type="button"
      class="auth-dark-toggle"
      :data-action="'toggle-dark-auth'"
      :aria-label="darkMode ? '切换到浅色模式' : '切换到暗色模式'"
      :title="darkMode ? '浅色模式' : '暗色模式'"
      @click="toggleDark"
    >{{ darkMode ? '☀' : '☾' }}</button>
    <section class="auth-showcase" aria-label="面试记录产品介绍">
      <RouterLink to="/login" class="auth-brand" aria-label="面试记录首页">
        <span class="auth-brand-mark" aria-hidden="true">IR</span>
        <span><strong>面试记录</strong><small>Interview Log</small></span>
      </RouterLink>

      <div class="auth-showcase-copy">
        <p class="auth-showcase-kicker">YOUR CAREER WORKSPACE</p>
        <h2>把每一次投递与面试，沉淀成清晰的求职进度。</h2>
        <p>集中管理公司、岗位、多轮面试与日程提醒，让秋招和日常实习不再靠零散表格和聊天记录。</p>
        <ul>
          <li><span aria-hidden="true">✓</span> 公司与多个岗位统一管理</li>
          <li><span aria-hidden="true">✓</span> 多轮面试经验持续沉淀</li>
          <li><span aria-hidden="true">✓</span> 紧急日程按优先级清晰提醒</li>
        </ul>
      </div>

      <div class="auth-theme-strip" aria-label="支持四套界面主题">
        <span style="--theme-dot: #4f5bd5" title="原始靛蓝" />
        <span style="--theme-dot: #2f8578" title="森林青绿" />
        <span style="--theme-dot: #c86e3e" title="暖杏棕" />
        <span style="--theme-dot: #ef5e58" title="石墨珊瑚" />
        <small>四套主题 · 登录后自动保存</small>
      </div>
    </section>

    <section class="auth-stage">
      <div class="auth-card">
        <header class="auth-card-header">
          <p class="auth-eyebrow">{{ eyebrow }}</p>
          <h1 :id="headingId">{{ title }}</h1>
          <p>{{ description }}</p>
        </header>
        <slot />
        <footer v-if="$slots.footer" class="auth-card-footer"><slot name="footer" /></footer>
      </div>
      <p class="auth-privacy">你的求职数据默认仅自己可见</p>
    </section>
  </main>
</template>

<style scoped>
.auth-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(360px, 0.88fr) minmax(520px, 1.12fr);
  background: var(--ir-bg);
  position: relative;
}
.auth-dark-toggle {
  position: absolute;
  z-index: 10;
  top: 14px;
  right: 14px;
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border: 1px solid var(--ir-border);
  border-radius: var(--ir-radius-sm);
  background: var(--ir-surface);
  color: var(--ir-text);
  font-size: 17px;
  cursor: pointer;
  transition: border-color var(--ir-transition), color var(--ir-transition);
}
.auth-dark-toggle:hover { border-color: var(--ir-primary-strong); color: var(--ir-primary-strong); }
:root[data-dark="true"] .auth-dark-toggle { border-color: var(--ir-border-strong); }
.auth-showcase {
  position: relative;
  isolation: isolate;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  padding: clamp(28px, 4vw, 58px);
  color: var(--ir-sidebar-text);
  background: var(--ir-sidebar);
}
.auth-showcase::before {
  content: "";
  position: absolute;
  z-index: -1;
  top: -140px;
  right: -110px;
  width: 320px;
  height: 320px;
  border-radius: 50%;
  background: color-mix(in srgb, var(--ir-primary), transparent 82%);
  pointer-events: none;
}
.auth-brand {
  width: fit-content;
  display: flex;
  align-items: center;
  gap: 12px;
  color: #fff;
  text-decoration: none;
}
.auth-brand-mark {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: var(--ir-radius-md);
  background: var(--ir-primary-strong);
  color: var(--ir-primary-contrast);
  font-size: 17px;
  font-weight: 800;
  letter-spacing: .02em;
}
.auth-brand strong,
.auth-brand small { display: block; }
.auth-brand strong { font-size: 18px; letter-spacing: .04em; }
.auth-brand small { margin-top: 2px; color: var(--ir-sidebar-text); font-size: 10px; }
.auth-showcase-copy {
  width: min(520px, 100%);
  margin: auto 0;
  padding: 54px 0;
}
.auth-showcase-kicker {
  margin: 0 0 16px;
  color: color-mix(in srgb, var(--ir-primary), white 26%);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: .18em;
}
.auth-showcase h2 {
  margin: 0;
  max-width: 510px;
  color: #fff;
  font-size: clamp(32px, 4vw, 54px);
  line-height: 1.18;
  letter-spacing: -.035em;
}
.auth-showcase-copy > p:not(.auth-showcase-kicker) {
  max-width: 480px;
  margin: 22px 0 0;
  color: color-mix(in srgb, var(--ir-sidebar-text), white 8%);
  font-size: 15px;
  line-height: 1.8;
}
.auth-showcase ul {
  display: grid;
  gap: 13px;
  margin: 28px 0 0;
  padding: 0;
  list-style: none;
}
.auth-showcase li { display: flex; align-items: center; gap: 10px; font-size: 14px; }
.auth-showcase li span {
  display: grid;
  place-items: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  color: #fff;
  background: color-mix(in srgb, var(--ir-primary-strong), transparent 16%);
  font-size: 12px;
}
.auth-theme-strip { display: flex; align-items: center; gap: 7px; }
.auth-theme-strip > span {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255,255,255,.7);
  border-radius: 50%;
  background: var(--theme-dot);
  box-shadow: 0 0 0 1px rgba(0,0,0,.12);
}
.auth-theme-strip small { margin-left: 5px; color: var(--ir-sidebar-text); }
.auth-stage {
  display: grid;
  place-items: center;
  align-content: center;
  gap: 18px;
  min-width: 0;
  padding: clamp(28px, 6vw, 84px);
  background: var(--ir-bg);
}
.auth-card {
  width: min(480px, 100%);
  padding: clamp(28px, 4vw, 44px);
  border: 1px solid var(--ir-border);
  border-radius: var(--ir-radius-xl);
  background: var(--ir-surface);
  box-shadow: var(--ir-shadow-md);
}
.auth-card-header { margin-bottom: 28px; }
.auth-eyebrow {
  margin: 0 0 10px;
  color: var(--ir-primary-strong);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: .12em;
}
.auth-card-header h1 { margin: 0; color: var(--ir-text); font-size: clamp(28px, 4vw, 36px); letter-spacing: -.03em; }
.auth-card-header > p:last-child { margin: 12px 0 0; color: var(--ir-muted); line-height: 1.7; }
.auth-card-footer { margin-top: 24px; padding-top: 20px; border-top: 1px solid var(--ir-border); }
.auth-card-footer:empty { display: none; }
.auth-privacy { margin: 0; color: var(--ir-muted); font-size: 12px; }
:deep(.auth-form) { display: grid; gap: 10px; }
:deep(.auth-form label) { margin-top: 6px; color: var(--ir-text); font-size: 13px; font-weight: 700; }
:deep(.auth-form .el-input__wrapper) { min-height: 44px; padding: 1px 14px; }
:deep(.auth-form .auth-submit) { width: 100%; min-height: 44px; margin-top: 12px; font-size: 15px; }
:deep(.auth-alert) { margin: 0 0 4px; padding: 11px 12px; border-radius: var(--ir-radius-sm); color: #9f2d35; background: color-mix(in srgb, var(--ir-danger), white 92%); border: 1px solid color-mix(in srgb, var(--ir-danger), transparent 72%); font-size: 13px; }
:deep(.auth-form [role="status"]) { margin: 0 0 4px; padding: 11px 12px; border-radius: var(--ir-radius-sm); color: #246447; background: color-mix(in srgb, var(--ir-success), white 93%); border: 1px solid color-mix(in srgb, var(--ir-success), transparent 70%); font-size: 13px; }
:deep(.auth-field-error) { margin: -4px 0 0; color: var(--ir-urgent); font-size: 12px; }
:deep(.auth-links) { display: flex; justify-content: space-between; gap: 12px; flex-wrap: wrap; font-size: 13px; }
:deep(.auth-links a) { font-weight: 700; text-decoration: none; }
:deep(.auth-links a:hover) { text-decoration: underline; }
:deep(.auth-success) { display: grid; justify-items: start; gap: 13px; }
:deep(.auth-success-icon) { display: grid; place-items: center; width: 54px; height: 54px; border-radius: var(--ir-radius-lg); background: var(--ir-primary-soft); color: var(--ir-primary-strong); font-size: 26px; font-weight: 800; }
:deep(.auth-success p) { margin: 0; color: var(--ir-muted); line-height: 1.7; }
:deep(.auth-success a) { display: inline-flex; align-items: center; min-height: 42px; padding: 0 18px; border-radius: var(--ir-radius-sm); color: var(--ir-primary-contrast); background: var(--ir-primary-strong); font-weight: 700; text-decoration: none; }
@media (max-width: 860px) {
  .auth-shell { grid-template-columns: 1fr; }
  .auth-showcase { min-height: auto; padding: 24px 22px 72px; }
  .auth-showcase-copy { margin: 44px 0 0; padding: 0; }
  .auth-showcase h2 { font-size: clamp(28px, 8vw, 42px); }
  .auth-showcase-copy > p:not(.auth-showcase-kicker), .auth-showcase ul { display: none; }
  .auth-theme-strip { display: none; }
  .auth-stage { margin-top: -48px; padding: 0 16px 32px; background: transparent; }
  .auth-card { padding: 26px 22px; border-radius: var(--ir-radius-lg); }
  .auth-privacy { color: var(--ir-muted); }
}
</style>
