<script setup lang="ts">
import { computed, onMounted, ref, useTemplateRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElButton, ElDialog, ElTag } from 'element-plus'
import InterviewRoundForm from '@/features/interviews/components/InterviewRoundForm.vue'
import SharePositionDialog from '@/features/sharing/components/SharePositionDialog.vue'
import { createRound, deleteRound, interviewResultLabel, interviewTypeLabel, listRounds, updateRound, type InterviewRound, type RoundRequest } from '@/features/interviews/api/interviews.api'
import { deletePosition, getPosition, setPositionArchived } from '@/features/tracking/api/positions.api'
import type { Position } from '@/features/tracking/api/tracking.types'
import { isApiRequestError } from '@/shared/api/error'
import { formatDateTime } from '@/shared/format/datetime'

const RESULT_TAG_TYPES: Record<string, 'info' | 'warning' | 'success' | 'danger' | 'primary'> = {
  UPCOMING: 'info',
  AWAITING_RESULT: 'warning',
  PASSED: 'success',
  FAILED: 'danger',
  CANCELLED: 'info',
}

const route = useRoute()
const router = useRouter()
const positionId = typeof route.params.id === 'string' ? route.params.id : ''

const loading = ref(true)
const error = ref('')
const message = ref('')
const position = ref<Position | null>(null)
const rounds = ref<InterviewRound[]>([])

const roundDialogOpen = ref(false)
const editingRound = ref<InterviewRound | null>(null)
const deleteRoundTarget = ref<InterviewRound | null>(null)
const deletePositionDialogOpen = ref(false)
const pendingDelete = ref(false)
const shareDialogOpen = ref(false)
const roundFormRef = useTemplateRef<InstanceType<typeof InterviewRoundForm>>('round-form')

const sortedRounds = computed(() => [...rounds.value].sort((a, b) => a.roundNumber - b.roundNumber))
const nextRoundNumber = computed(() => {
  const last = sortedRounds.value[sortedRounds.value.length - 1]
  return last ? last.roundNumber + 1 : 1
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [loadedPosition, loadedRounds] = await Promise.all([getPosition(positionId), listRounds(positionId)])
    position.value = loadedPosition
    rounds.value = loadedRounds
  } catch (caught) {
    error.value = isApiRequestError(caught) ? caught.apiError.message : '加载岗位失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onMounted(() => { void load() })

function openCreateRound() {
  editingRound.value = null
  roundDialogOpen.value = true
}

function openEditRound(round: InterviewRound) {
  editingRound.value = round
  roundDialogOpen.value = true
}

async function submitRound(payload: RoundRequest) {
  error.value = ''; message.value = ''
  try {
    if (editingRound.value) {
      const updated = await updateRound(editingRound.value.id, payload)
      rounds.value = rounds.value.map((item) => (item.id === updated.id ? updated : item))
      message.value = '面试轮次已更新'
    } else {
      const created = await createRound(positionId, payload)
      rounds.value = [...rounds.value, created]
      message.value = '面试轮次已创建'
    }
    roundFormRef.value?.clearDraft()
    roundDialogOpen.value = false
    await refreshPosition()
  } catch (caught) {
    error.value = isApiRequestError(caught) ? caught.apiError.message : '保存面试轮次失败'
  }
}

async function confirmDeleteRound() {
  if (!deleteRoundTarget.value) return
  pendingDelete.value = true
  error.value = ''
  try {
    await deleteRound(deleteRoundTarget.value.id)
    rounds.value = rounds.value.filter((item) => item.id !== deleteRoundTarget.value?.id)
    message.value = '面试轮次已删除'
    deleteRoundTarget.value = null
    await refreshPosition()
  } catch (caught) {
    error.value = isApiRequestError(caught) ? caught.apiError.message : '删除失败'
  } finally {
    pendingDelete.value = false
  }
}

async function refreshPosition() {
  try {
    position.value = await getPosition(positionId)
  } catch { /* 岗位信息刷新失败不影响轮次操作结果 */ }
}

async function toggleArchived() {
  if (!position.value) return
  error.value = ''
  try {
    position.value = await setPositionArchived(positionId, !position.value.archived)
    message.value = position.value.archived ? '岗位已归档' : '岗位已恢复'
  } catch (caught) {
    error.value = isApiRequestError(caught) ? caught.apiError.message : '操作失败'
  }
}

async function confirmDeletePosition() {
  pendingDelete.value = true
  error.value = ''
  try {
    await deletePosition(positionId, true)
    await router.push({ name: 'positions' })
  } catch (caught) {
    error.value = isApiRequestError(caught) ? caught.apiError.message : '删除失败，请稍后重试'
    pendingDelete.value = false
  }
}
</script>

<template>
  <main class="position-detail page-center" aria-labelledby="position-detail-heading">
    <p v-if="error" role="alert">{{ error }}</p>
    <p v-if="message" role="status">{{ message }}</p>
    <p v-if="loading" role="status">加载中…</p>

    <template v-if="!loading && position">
      <div class="page-head">
        <div>
          <span class="eyebrow">{{ position.companyName }}</span>
          <h1 id="position-detail-heading">{{ position.title }}</h1>
          <p class="page-desc">面试轮次、过程记录与复盘都沉淀在这个岗位下。</p>
        </div>
        <div class="page-head-actions">
          <RouterLink :to="{ name: 'edit-position', params: { id: position.id } }"><ElButton size="small" data-action="edit-position">编辑</ElButton></RouterLink>
          <ElButton size="small" data-action="toggle-archive" @click="toggleArchived">{{ position.archived ? '恢复' : '归档' }}</ElButton>
          <ElButton size="small" data-action="share-position" @click="shareDialogOpen = true">分享</ElButton>
          <ElButton size="small" text type="danger" data-action="delete-position" @click="deletePositionDialogOpen = true">删除</ElButton>
        </div>
      </div>

      <section class="ir-panel position-info" aria-labelledby="position-info-heading">
        <div class="ir-panel-head"><div><span class="panel-kicker">基础信息</span><h2 id="position-info-heading">岗位信息</h2></div></div>
        <div class="info-grid">
          <div class="info-item"><span class="info-label">招聘类型</span><span>{{ position.jobTypeName }}</span></div>
          <div class="info-item"><span class="info-label">当前状态</span><span class="status-pill" :style="{ '--pill': position.status.color }">{{ position.status.name }}</span></div>
          <div class="info-item"><span class="info-label">投递日期</span><span>{{ formatDateTime(position.appliedAt) }}</span></div>
          <div class="info-item"><span class="info-label">截止日期</span><span>{{ formatDateTime(position.deadlineAt) }}</span></div>
          <div class="info-item"><span class="info-label">工作地点</span><span>{{ position.workLocation ?? '—' }}</span></div>
          <div class="info-item">
            <span class="info-label">投递链接</span>
            <a v-if="position.applyUrl" :href="position.applyUrl" target="_blank" rel="noopener noreferrer">{{ position.applyUrl }}</a>
            <span v-else>—</span>
          </div>
          <div class="info-item info-item-wide"><span class="info-label">岗位描述</span><div class="rich-content description-text" v-html="position.description ?? '<p>—</p>'" /></div>
          <div v-if="position.nextSchedule" class="info-item info-item-wide">
            <span class="info-label">下一场日程</span>
            <span>{{ position.nextSchedule.title }} · {{ formatDateTime(position.nextSchedule.time) }}</span>
          </div>
        </div>
      </section>

      <section class="ir-panel" aria-labelledby="rounds-heading">
        <div class="ir-panel-head">
          <div><span class="panel-kicker">面试记录</span><h2 id="rounds-heading">面试轮次</h2></div>
          <ElButton size="small" type="primary" data-action="add-round" @click="openCreateRound">新增面试轮次</ElButton>
        </div>
        <p v-if="sortedRounds.length === 0" class="rounds-empty" data-testid="no-rounds">还没有面试记录，点击右上角「新增面试轮次」记录你的第一场面试。</p>
        <div v-else class="round-list">
          <article v-for="round in sortedRounds" :key="round.id" class="round-card" :data-testid="`round-${round.id}`">
            <header class="round-card-header">
              <h3>第 {{ round.roundNumber }} 轮 · {{ round.roundName }}</h3>
              <div class="round-card-actions">
                <ElButton size="small" text :data-action="`edit-round-${round.id}`" @click="openEditRound(round)">编辑</ElButton>
                <ElButton size="small" text type="danger" :data-action="`delete-round-${round.id}`" @click="deleteRoundTarget = round">删除</ElButton>
              </div>
            </header>
            <p class="round-meta">
              {{ interviewTypeLabel(round.interviewType) }}
              <template v-if="round.startsAt"> · {{ formatDateTime(round.startsAt) }}<template v-if="round.endsAt"> ~ {{ formatDateTime(round.endsAt) }}</template></template>
              <template v-if="round.location"> · {{ round.location }}</template>
            </p>
            <p class="round-result">结果：<ElTag :type="RESULT_TAG_TYPES[round.result] ?? 'info'" size="small">{{ interviewResultLabel(round.result) }}</ElTag></p>
            <template v-if="round.processNotes">
              <h4>过程记录</h4>
              <!-- 内容已经服务端白名单清洗，可安全渲染 -->
              <div class="rich-content round-text" v-html="round.processNotes" />
            </template>
            <template v-if="round.reviewSummary">
              <h4>整体复盘</h4>
              <div class="rich-content round-text" v-html="round.reviewSummary" />
            </template>
            <template v-if="round.questions.length > 0">
              <h4>问题与回答（{{ round.questions.length }}）</h4>
              <ol class="question-list">
                <li v-for="(question, index) in round.questions" :key="index">
                  <p class="question-text">{{ question.question }}<ElTag v-if="question.category" size="small" type="info">{{ question.category }}</ElTag></p>
                  <p v-if="question.answer" class="answer-text">{{ question.answer }}</p>
                </li>
              </ol>
            </template>
          </article>
        </div>
      </section>

      <ElDialog v-model="roundDialogOpen" :title="editingRound ? '编辑面试轮次' : `新增面试轮次（默认第 ${nextRoundNumber} 轮）`" width="min(94vw, 640px)" top="6vh" :teleported="false">
        <InterviewRoundForm
          ref="round-form"
          :initial="editingRound"
          :default-round-number="nextRoundNumber"
          @submitted="submitRound"
        />
      </ElDialog>

      <ElDialog :model-value="deleteRoundTarget !== null" title="确认删除面试轮次" width="min(92vw, 420px)" :teleported="false" @close="deleteRoundTarget = null">
        <p>删除「{{ deleteRoundTarget?.roundName }}」后无法恢复，其问题记录和关联日程也会一并删除。确定吗？</p>
        <template #footer>
          <ElButton @click="deleteRoundTarget = null">取消</ElButton>
          <ElButton type="danger" data-action="confirm-delete-round" :loading="pendingDelete" @click="confirmDeleteRound">删除</ElButton>
        </template>
      </ElDialog>

      <ElDialog v-model="deletePositionDialogOpen" title="确认删除岗位" width="min(92vw, 460px)" :teleported="false">
        <p>删除岗位后无法恢复，将同时删除 {{ position.interviewRoundCount }} 条面试轮次记录和 {{ position.scheduleCount }} 条关联日程。确定吗？</p>
        <template #footer>
          <ElButton @click="deletePositionDialogOpen = false">取消</ElButton>
          <ElButton type="danger" data-action="confirm-delete-position" :loading="pendingDelete" @click="confirmDeletePosition">永久删除</ElButton>
        </template>
      </ElDialog>
      <SharePositionDialog v-model="shareDialogOpen" :position-id="position.id" :rounds="rounds" />
    </template>
  </main>
</template>

<style scoped>
.position-detail {
  display: flex;
  flex-direction: column;
  gap: 18px;
  max-width: 1000px;
}
.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px 18px;
  padding: 14px 16px 16px;
}
.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13.5px;
}
.info-item-wide {
  grid-column: 1 / -1;
}
.info-label {
  color: var(--ir-muted);
  font-size: 12px;
}
.description-text {
  margin: 0;
}
.rounds-empty { margin: 0; padding: 14px 16px 16px; color: var(--ir-muted); font-size: 13px; }
.round-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px 16px 16px;
}
.round-card {
  background: var(--ir-surface-muted);
  border: 1px solid color-mix(in srgb, var(--ir-border), transparent 35%);
  border-radius: var(--ir-radius-md);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.round-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.round-card-header h3 {
  margin: 0;
  font-size: 15px;
}
.round-card-actions {
  display: flex;
  gap: 8px;
}
.round-meta,
.round-result {
  margin: 0;
  color: var(--ir-muted);
  font-size: 13px;
}
.round-card h4 {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--ir-text);
}
.round-text {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.7;
}
.question-list {
  margin: 0;
  padding-left: 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.question-text {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  font-weight: 600;
}
.answer-text {
  margin: 4px 0 0;
  color: var(--ir-muted);
  white-space: pre-wrap;
}
</style>
