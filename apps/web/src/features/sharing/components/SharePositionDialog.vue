<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElButton, ElCheckbox, ElCheckboxGroup, ElDialog, ElOption, ElSelect, ElTag } from 'element-plus'
import { createShare, listShares, revokeShare, type CreateSharePayload, type PositionShareField, type RoundShareField, type ShareLink } from '@/features/sharing/api/sharing.api'
import type { InterviewRound } from '@/features/interviews/api/interviews.api'

const props = defineProps<{ positionId: string; rounds: InterviewRound[] }>()
const open = defineModel<boolean>({ default: false })
const emit = defineEmits<{ created: [publicPath: string] }>()
const positionFields = ref<PositionShareField[]>(['COMPANY_NAME', 'POSITION_TITLE', 'JOB_TYPE', 'STATUS'])
const selectedRounds = ref<string[]>([])
const roundFields = ref<Record<string, RoundShareField[]>>({})
const expiry = ref<CreateSharePayload['expiry']>('SEVEN_DAYS')
const shares = ref<ShareLink[]>([])
const loading = ref(false)
const error = ref('')
const createdLink = ref('')
const fieldLabels: Record<string, string> = { COMPANY_NAME: '公司', POSITION_TITLE: '岗位', JOB_TYPE: '招聘类型', STATUS: '状态', BASIC_INFO: '基本信息', QUESTIONS: '问题', ANSWERS: '回答', PROCESS: '面试过程', REVIEW: '复盘', RESULT: '结果' }
const shareUrl = computed(() => createdLink.value ? `${window.location.origin}${createdLink.value}` : '')

async function loadShares() { try { shares.value = await listShares(props.positionId) } catch { /* list remains empty */ } }
function ensureRound(id: string) { roundFields.value[id] ??= ['BASIC_INFO', 'QUESTIONS', 'ANSWERS', 'PROCESS', 'REVIEW', 'RESULT'] }
async function submit() {
  if (!positionFields.value.length) { error.value = '至少选择一个岗位字段'; return }
  loading.value = true; error.value = ''
  try {
    const result = await createShare(props.positionId, { positionFields: positionFields.value, rounds: selectedRounds.value.map((roundId) => ({ roundId, visibleFields: roundFields.value[roundId] ?? ['BASIC_INFO'] })), expiry: expiry.value })
    createdLink.value = result.publicPath; emit('created', result.publicPath); await loadShares()
  } catch { error.value = '分享链接创建失败，请稍后重试' } finally { loading.value = false }
}
async function revoke(id: string) { await revokeShare(props.positionId, id); await loadShares() }
async function copy() { if (shareUrl.value) await navigator.clipboard?.writeText(shareUrl.value) }
onMounted(() => { void loadShares() })
</script>
<template>
  <ElDialog v-model="open" title="分享岗位经验" width="min(94vw, 680px)" :teleported="false">
    <div class="share-form">
      <p class="hint">只会公开你勾选的字段，投递链接、备注和账号信息不会出现在分享页。</p>
      <strong>岗位字段</strong><ElCheckboxGroup v-model="positionFields"><ElCheckbox v-for="field in ['COMPANY_NAME','POSITION_TITLE','JOB_TYPE','STATUS']" :key="field" :label="field">{{ fieldLabels[field] }}</ElCheckbox></ElCheckboxGroup>
      <strong>面试轮次</strong><div v-if="rounds.length" class="round-selects"><div v-for="round in rounds" :key="round.id" class="round-row"><label><input v-model="selectedRounds" type="checkbox" :value="round.id" @change="ensureRound(round.id)" /> 第 {{ round.roundNumber }} 轮 · {{ round.roundName }}</label><ElCheckboxGroup v-if="selectedRounds.includes(round.id)" v-model="roundFields[round.id]" class="round-fields"><ElCheckbox v-for="field in ['BASIC_INFO','QUESTIONS','ANSWERS','PROCESS','REVIEW','RESULT']" :key="field" :label="field">{{ fieldLabels[field] }}</ElCheckbox></ElCheckboxGroup></div></div><p v-else class="hint">还没有面试轮次，先记录一轮面试再分享。</p>
      <label>有效期 <ElSelect v-model="expiry" style="width: 160px"><ElOption label="1 天" value="ONE_DAY" /><ElOption label="7 天" value="SEVEN_DAYS" /><ElOption label="30 天" value="THIRTY_DAYS" /><ElOption label="永久" value="PERMANENT" /></ElSelect></label>
      <p v-if="error" role="alert" class="error">{{ error }}</p><p v-if="shareUrl" role="status" class="created">链接已生成：<a :href="shareUrl" target="_blank" rel="noopener">{{ shareUrl }}</a> <ElButton size="small" @click="copy">复制</ElButton></p>
      <div v-if="shares.length" class="existing"><strong>历史链接</strong><div v-for="share in shares" :key="share.id" class="existing-row"><span><ElTag :type="share.revokedAt ? 'info' : 'success'">{{ share.revokedAt ? '已撤销' : '有效' }}</ElTag> {{ share.expiresAt ? `到期 ${new Date(share.expiresAt).toLocaleDateString()}` : '永久' }}</span><ElButton v-if="!share.revokedAt" size="small" type="danger" @click="revoke(share.id)">撤销</ElButton></div></div>
    </div>
    <template #footer><ElButton @click="open = false">关闭</ElButton><ElButton type="primary" :loading="loading" @click="submit">生成分享链接</ElButton></template>
  </ElDialog>
</template>
<style scoped>.share-form{display:grid;gap:12px}.hint{margin:0;color:var(--ir-muted);line-height:1.6}.round-selects,.existing{display:grid;gap:8px}.round-row{padding:10px;border:1px solid var(--ir-border);border-radius:8px}.round-fields{padding:8px 0 0 24px}.error{color:var(--ir-danger)}.created{padding:10px;background:var(--ir-surface-muted);overflow-wrap:anywhere}.existing-row{display:flex;justify-content:space-between;align-items:center;border-top:1px solid var(--ir-border);padding:8px 0}</style>
