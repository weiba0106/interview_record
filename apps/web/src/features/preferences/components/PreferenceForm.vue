<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElButton, ElInput, ElOption, ElSelect } from 'element-plus'
import { applyTheme, themeOptions, type ThemeName } from '@/app/theme'
import type { Preferences } from '../api/preferences.api'

const props = defineProps<{ preferences: Preferences }>()
const emit = defineEmits<{ submitted: [preferences: Preferences] }>()
const fieldErrors = ref<Record<string, string>>({})
const form = reactive({
  displayName: props.preferences.displayName,
  timeZone: props.preferences.timeZone,
  theme: props.preferences.theme,
  interviewReminderOffsets: props.preferences.interviewReminderOffsets.join(', '),
  deadlineReminderOffsets: props.preferences.deadlineReminderOffsets.join(', '),
})

watch(() => props.preferences, (preferences) => {
  form.displayName = preferences.displayName
  form.timeZone = preferences.timeZone
  form.theme = preferences.theme
  form.interviewReminderOffsets = preferences.interviewReminderOffsets.join(', ')
  form.deadlineReminderOffsets = preferences.deadlineReminderOffsets.join(', ')
}, { deep: true })

function selectTheme(theme: ThemeName) {
  form.theme = theme
  // 点击预览卡立即生效，保存后由后端持久化
  applyTheme(theme)
}

function parseOffsets(value: string, field: string): number[] | undefined {
  if (!value.trim()) return []
  const offsets = value.split(',').map((part) => Number(part.trim()))
  if (offsets.some((offset) => !Number.isInteger(offset) || offset < 0 || offset > 10080)) {
    fieldErrors.value[field] = '请输入 0 到 10080 之间的整数分钟数'
    return undefined
  }
  return [...new Set(offsets)].sort((left, right) => right - left)
}

function submit() {
  fieldErrors.value = {}
  const interviewReminderOffsets = parseOffsets(form.interviewReminderOffsets, 'interviewReminderOffsets')
  const deadlineReminderOffsets = parseOffsets(form.deadlineReminderOffsets, 'deadlineReminderOffsets')
  if (!form.displayName.trim()) fieldErrors.value.displayName = '显示名称不能为空'
  if (!form.timeZone.trim()) fieldErrors.value.timeZone = '时区不能为空'
  if (!interviewReminderOffsets || !deadlineReminderOffsets || Object.keys(fieldErrors.value).length > 0) return
  emit('submitted', {
    displayName: form.displayName.trim(), timeZone: form.timeZone.trim(), theme: form.theme,
    interviewReminderOffsets, deadlineReminderOffsets,
  })
}
</script>

<template>
  <form class="preference-form" novalidate @submit.prevent="submit">
    <div class="ir-panel">
      <div class="ir-panel-head"><div><span class="panel-kicker">个人偏好</span><h2>偏好设置</h2></div></div>
      <div class="pref-body">
        <section class="pref-section" aria-labelledby="pref-profile-heading">
          <h3 id="pref-profile-heading">个人信息</h3>
          <label for="preference-display-name">显示名称</label>
          <ElInput id="preference-display-name" v-model="form.displayName" name="displayName" autocomplete="name" :aria-describedby="fieldErrors.displayName ? 'preference-display-name-error' : undefined" />
          <p v-if="fieldErrors.displayName" id="preference-display-name-error" data-field-error="displayName" role="alert">{{ fieldErrors.displayName }}</p>

          <label for="preference-time-zone">时区（IANA，例如 Asia/Shanghai）</label>
          <ElInput id="preference-time-zone" v-model="form.timeZone" name="timeZone" :aria-describedby="fieldErrors.timeZone ? 'preference-time-zone-error' : undefined" />
          <p v-if="fieldErrors.timeZone" id="preference-time-zone-error" data-field-error="timeZone" role="alert">{{ fieldErrors.timeZone }}</p>
        </section>

        <section class="pref-section" aria-labelledby="pref-theme-heading">
          <h3 id="pref-theme-heading">主题</h3>
          <p class="pref-hint">主题会保存到账号，换设备登录后保持一致；紧急程度的语义色不受影响。</p>
          <div class="theme-card-grid" role="radiogroup" aria-label="界面主题">
            <button
              v-for="option in themeOptions"
              :key="option.value"
              type="button"
              role="radio"
              class="theme-card"
              :class="{ active: form.theme === option.value }"
              :aria-checked="form.theme === option.value"
              :data-theme-card="option.value"
              @click="selectTheme(option.value)"
            >
              <span
                class="theme-card-preview"
                aria-hidden="true"
                :style="{
                  '--preview-sidebar': option.palette.sidebar,
                  '--preview-primary': option.palette.primaryStrong,
                  '--preview-bg': option.palette.background,
                  '--preview-card': option.palette.card,
                  '--preview-border': option.palette.border,
                }"
              >
                <i class="preview-sidebar" />
                <span class="preview-body">
                  <i class="preview-button" />
                  <i class="preview-card-block" />
                  <i class="preview-row row-a" />
                  <i class="preview-row row-b" />
                </span>
              </span>
              <span class="theme-card-label"><strong>{{ option.label }}</strong><small>{{ option.description }}</small></span>
              <span v-if="form.theme === option.value" class="theme-card-check" aria-label="当前主题">✓</span>
            </button>
          </div>
          <label for="preference-theme">主题</label>
          <ElSelect id="preference-theme" v-model="form.theme" name="theme" class="theme-select">
            <ElOption label="原始靛蓝" value="INDIGO" />
            <ElOption label="森林青绿" value="FOREST_TEAL" />
            <ElOption label="暖杏棕" value="WARM_APRICOT" />
            <ElOption label="石墨珊瑚" value="GRAPHITE_CORAL" />
          </ElSelect>
        </section>

        <section class="pref-section" aria-labelledby="pref-reminder-heading">
          <h3 id="pref-reminder-heading">提醒设置</h3>
          <p class="pref-hint">提前多少分钟发送邮件提醒，多个时间用逗号分隔，单位是分钟。</p>
          <label for="interview-reminder-offsets">面试默认提醒（分钟，逗号分隔）</label>
          <ElInput id="interview-reminder-offsets" v-model="form.interviewReminderOffsets" name="interviewReminderOffsets" :aria-describedby="fieldErrors.interviewReminderOffsets ? 'preference-interview-reminder-offsets-error' : undefined" />
          <p v-if="fieldErrors.interviewReminderOffsets" id="preference-interview-reminder-offsets-error" data-field-error="interviewReminderOffsets" role="alert">{{ fieldErrors.interviewReminderOffsets }}</p>

          <label for="deadline-reminder-offsets">截止日期默认提醒（分钟，逗号分隔）</label>
          <ElInput id="deadline-reminder-offsets" v-model="form.deadlineReminderOffsets" name="deadlineReminderOffsets" :aria-describedby="fieldErrors.deadlineReminderOffsets ? 'preference-deadline-reminder-offsets-error' : undefined" />
          <p v-if="fieldErrors.deadlineReminderOffsets" id="preference-deadline-reminder-offsets-error" data-field-error="deadlineReminderOffsets" role="alert">{{ fieldErrors.deadlineReminderOffsets }}</p>
        </section>

        <ElButton native-type="submit" type="primary" class="pref-submit">保存偏好</ElButton>
      </div>
    </div>
  </form>
</template>

<style scoped>
.preference-form { min-width: 0; }
.pref-body { padding: 16px 18px 18px; display: grid; gap: 20px; }
.pref-section { display: grid; gap: 8px; }
.pref-section h3 {
  margin: 0 0 2px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: .04em;
  color: var(--ir-muted);
  text-transform: uppercase;
}
.pref-section label { font-size: 13px; font-weight: 650; color: var(--ir-text); }
.pref-hint { margin: -4px 0 2px; color: var(--ir-muted); font-size: 12px; line-height: 1.6; }
.pref-submit { width: fit-content; min-width: 116px; margin-top: 2px; }

.theme-card-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
.theme-card {
  position: relative;
  display: grid;
  gap: 8px;
  padding: 10px;
  border: 1px solid var(--ir-border);
  border-radius: var(--ir-radius-md);
  background: var(--ir-surface);
  color: var(--ir-text);
  text-align: left;
  font: inherit;
  cursor: pointer;
  transition: border-color var(--ir-transition), box-shadow var(--ir-transition), background-color var(--ir-transition);
}
.theme-card:hover { border-color: var(--ir-border-strong); }
.theme-card.active {
  border-color: var(--ir-primary-strong);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ir-primary-strong), transparent 86%);
  background: var(--ir-primary-soft);
}
.theme-card-check {
  position: absolute;
  top: 8px;
  right: 8px;
  display: grid;
  place-items: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--ir-primary-strong);
  color: #fff;
  font-size: 11px;
  font-weight: 800;
}
.theme-card-label strong { display: block; font-size: 13px; }
.theme-card-label small { display: block; color: var(--ir-muted); font-size: 11px; margin-top: 2px; }

/* 主题缩略预览：侧边栏色 + 页面背景 + 卡片 + 主按钮 */
.theme-card-preview {
  display: flex;
  height: 52px;
  overflow: hidden;
  border: 1px solid var(--preview-border);
  border-radius: var(--ir-radius-sm);
  background: var(--preview-bg);
}
.preview-sidebar { display: block; width: 22%; background: var(--preview-sidebar); flex: none; }
.preview-body { position: relative; flex: 1; padding: 7px; }
.preview-button {
  position: absolute;
  top: 7px;
  right: 7px;
  width: 22%;
  height: 7px;
  border-radius: 3px;
  background: var(--preview-primary);
}
.preview-card-block {
  position: absolute;
  left: 7px;
  top: 7px;
  width: 56%;
  height: 14px;
  border-radius: 3px;
  border: 1px solid var(--preview-border);
  background: var(--preview-card);
}
.preview-row { position: absolute; left: 7px; width: 46%; height: 3px; border-radius: 2px; background: var(--preview-border); }
.preview-row.row-a { top: 28px; }
.preview-row.row-b { top: 35px; }

.theme-select { width: min(240px, 100%); }
@media (max-width: 480px) {
  .theme-card-grid { grid-template-columns: 1fr; }
}
</style>
