<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getPublicShare, type PublicShare } from '@/features/sharing/api/sharing.api'

const route = useRoute()
const data = ref<PublicShare | null>(null)
const error = ref('')
onMounted(async () => {
  const token = String(route.params.token ?? '')
  try { data.value = await getPublicShare(token); document.title = '面试经验分享' } catch { error.value = '分享链接不存在、已过期或已撤销' }
})
</script>
<template>
  <main class="public-share">
    <div v-if="error" class="ir-panel">
      <div class="ir-empty">
        <span class="ir-empty-icon" aria-hidden="true">!</span>
        <strong>链接已失效</strong>
        <p role="alert">{{ error }}</p>
      </div>
    </div>
    <template v-else-if="data">
      <header class="share-header">
        <p class="share-brand"><span class="share-brand-mark" aria-hidden="true">IR</span>Interview Record</p>
        <h1>{{ data.position.positionTitle ?? '岗位面试经验' }}</h1>
        <p class="subline">{{ data.position.companyName ?? '' }}<template v-if="data.position.jobType"> · {{ data.position.jobType }}</template><template v-if="data.position.status"> · {{ data.position.status }}</template></p>
      </header>
      <section v-for="round in data.rounds" :key="round.id" class="round-card">
        <template v-if="round.content.basicInfo">
          <h2>第 {{ (round.content.basicInfo as { roundNumber?: number }).roundNumber ?? '' }} 轮 · {{ (round.content.basicInfo as { roundName?: string }).roundName ?? '面试' }}</h2>
          <p class="muted">{{ (round.content.basicInfo as { interviewType?: string }).interviewType }} · {{ (round.content.basicInfo as { startsAt?: string }).startsAt }}</p>
        </template>
        <div v-if="round.content.processNotes" class="rich-content text" v-html="String(round.content.processNotes)" />
        <div v-if="round.content.reviewSummary" class="rich-content text" v-html="String(round.content.reviewSummary)" />
        <p v-if="round.content.result" class="result">结果：{{ round.content.result }}</p>
        <div v-if="round.content.questions" class="questions">
          <h3>面试问题</h3>
          <ul><li v-for="question in (round.content.questions as { question: string; category?: string }[])" :key="question.question">{{ question.question }}<small v-if="question.category"> · {{ question.category }}</small></li></ul>
        </div>
      </section>
      <footer>此页面由用户主动分享，仅展示已选择的内容。</footer>
    </template>
  </main>
</template>
<style scoped>
.public-share { max-width: 780px; margin: 0 auto; padding: 48px 20px 56px; display: grid; gap: 16px; }
.share-header { display: grid; gap: 8px; }
.share-brand { display: flex; align-items: center; gap: 8px; margin: 0; color: var(--ir-muted); font-weight: 700; font-size: 12px; letter-spacing: .06em; }
.share-brand-mark {
  display: inline-grid; place-items: center; width: 26px; height: 26px;
  border-radius: var(--ir-radius-sm); background: var(--ir-primary-strong);
  color: #fff; font-size: 11px; font-weight: 800;
}
.public-share h1 { margin: 0; font-size: clamp(26px, 5vw, 38px); line-height: 1.2; }
.subline { margin: 0; font-size: 14px; color: var(--ir-muted); }
.round-card { padding: 20px 22px; background: var(--ir-surface); border: 1px solid var(--ir-border); border-radius: var(--ir-radius-lg); box-shadow: var(--ir-shadow-sm); display: grid; gap: 10px; }
.round-card h2 { margin: 0; font-size: 16px; }
.muted { margin: 0; font-size: 13px; color: var(--ir-muted); }
.text { margin: 0; white-space: pre-wrap; font: inherit; font-size: 14px; line-height: 1.75; }
.result { margin: 0; font-weight: 700; font-size: 13.5px; }
.questions { display: grid; gap: 6px; }
.questions h3 { margin: 4px 0 0; font-size: 13px; }
.questions ul { margin: 0; padding-left: 20px; }
.questions li { margin: 6px 0; font-size: 14px; }
.questions small { color: var(--ir-muted); }
footer { margin-top: 6px; color: var(--ir-muted); font-size: 12px; text-align: center; }
</style>
