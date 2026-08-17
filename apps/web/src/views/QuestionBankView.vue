<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElButton, ElInput, ElOption, ElPagination, ElSelect, ElTag } from 'element-plus'
import {
  questionCategories,
  randomQuestions,
  searchQuestionBank,
  type QuestionBankItem,
} from '@/features/interviews/api/interviews.api'
import { isApiRequestError } from '@/shared/api/error'
import { formatDate } from '@/shared/format/datetime'

const loading = ref(true)
const error = ref('')
const items = ref<QuestionBankItem[]>([])
const totalItems = ref(0)
const totalPages = ref(0)
const page = ref(1)
const keyword = ref('')
const category = ref('')
const categories = ref<string[]>([])

const randomMode = ref(false)
const randomLoading = ref(false)
const revealed = ref<Set<string>>(new Set())

const PAGE_SIZE = 20

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await searchQuestionBank({
      category: category.value || undefined,
      keyword: keyword.value.trim() || undefined,
      page: page.value - 1,
      size: PAGE_SIZE,
    })
    items.value = result.items
    totalItems.value = result.totalItems
    totalPages.value = result.totalPages
  } catch (caught) {
    error.value = isApiRequestError(caught) ? caught.apiError.message : '加载题库失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    categories.value = await questionCategories()
  } catch { /* 分类加载失败不阻塞题库 */ }
}

onMounted(() => {
  void load()
  void loadCategories()
})

function applyFilters() {
  page.value = 1
  void load()
}

function onPageChange(next: number) {
  page.value = next
  void load()
}

async function drawRandom() {
  randomLoading.value = true
  error.value = ''
  try {
    items.value = await randomQuestions(10)
    randomMode.value = true
    totalPages.value = 0
    revealed.value = new Set()
  } catch (caught) {
    error.value = isApiRequestError(caught) ? caught.apiError.message : '随机抽题失败，请稍后重试'
  } finally {
    randomLoading.value = false
  }
}

function exitRandom() {
  randomMode.value = false
  page.value = 1
  void load()
}

function toggleReveal(id: string) {
  const next = new Set(revealed.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  revealed.value = next
}

function metaLine(item: QuestionBankItem): string {
  const parts = [
    item.companyName,
    item.positionTitle,
    item.roundNumber !== null && item.roundName ? `第 ${item.roundNumber} 轮 · ${item.roundName}` : null,
  ].filter(Boolean)
  return parts.join(' · ')
}
</script>

<template>
  <main class="question-bank page-center" aria-labelledby="question-bank-heading">
    <div class="page-head">
      <div>
        <span class="eyebrow">面试题库</span>
        <h1 id="question-bank-heading">题库复习</h1>
        <p class="page-desc">汇总你记录过的面试题，按分类检索或随机抽题复习。</p>
      </div>
      <div class="page-head-actions">
        <ElButton v-if="randomMode" data-action="exit-random" @click="exitRandom">返回列表</ElButton>
        <ElButton type="primary" data-action="draw-random" :loading="randomLoading" @click="drawRandom">{{ randomMode ? '换一批' : '随机抽题' }}</ElButton>
      </div>
    </div>
    <p v-if="error" role="alert">{{ error }}</p>

    <div class="ir-panel">
      <form v-if="!randomMode" class="question-filter" aria-label="题库筛选" @submit.prevent="applyFilters">
        <ElInput v-model="keyword" name="questionKeyword" placeholder="搜索题目内容" clearable class="keyword-input" />
        <ElSelect v-model="category" name="questionCategory" clearable placeholder="全部分类">
          <ElOption v-for="item in categories" :key="item" :label="item" :value="item" />
        </ElSelect>
        <ElButton native-type="submit" type="primary" data-action="apply-question-filters">筛选</ElButton>
      </form>
      <p v-if="loading" role="status" class="question-loading">加载中…</p>
      <template v-else>
        <ul v-if="items.length > 0" class="question-list">
          <li v-for="item in items" :key="item.id" class="question-card" :data-testid="`question-${item.id}`">
            <div class="question-head">
              <p class="question-text">{{ item.question }}</p>
              <ElTag v-if="item.category" size="small">{{ item.category }}</ElTag>
            </div>
            <p class="question-meta">{{ metaLine(item) }}<template v-if="item.createdAt"> · {{ formatDate(item.createdAt) }}</template></p>
            <template v-if="item.answer">
              <p v-if="revealed.has(item.id)" class="question-answer" :data-testid="`answer-${item.id}`">{{ item.answer }}</p>
              <ElButton size="small" text :data-action="`reveal-${item.id}`" @click="toggleReveal(item.id)">
                {{ revealed.has(item.id) ? '收起回答' : '显示回答' }}
              </ElButton>
            </template>
          </li>
        </ul>
        <div v-else class="ir-empty">
          <span class="ir-empty-icon" aria-hidden="true">✎</span>
          <strong>{{ randomMode ? '题库还是空的' : '没有匹配的题目' }}</strong>
          <p>{{ randomMode ? '先在岗位详情里记录几道面试题，再来这里复习。' : '换个关键词或分类试试。' }}</p>
        </div>
      </template>
    </div>

    <ElPagination
      v-if="!randomMode && !loading && totalPages > 1"
      class="question-pagination"
      layout="prev, pager, next"
      :total="totalItems"
      :page-size="PAGE_SIZE"
      :current-page="page"
      @current-change="onPageChange"
    />
  </main>
</template>

<style scoped>
.question-bank {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 900px;
}
.question-filter {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
  padding: 12px 16px;
  border-bottom: 1px solid var(--ir-border);
}
.keyword-input { width: min(260px, 100%); }
.question-filter .el-select { width: 170px; }
.question-loading { margin: 0; padding: 16px; color: var(--ir-muted); font-size: 13px; }
.question-list {
  list-style: none;
  margin: 0;
  padding: 12px 16px 16px;
  display: grid;
  gap: 10px;
}
.question-card {
  border: 1px solid var(--ir-border);
  border-radius: var(--ir-radius-md);
  background: var(--ir-surface);
  padding: 12px 14px;
  display: grid;
  gap: 6px;
  box-shadow: var(--ir-shadow-sm);
}
.question-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 10px; }
.question-text { margin: 0; font-size: 14px; font-weight: 650; line-height: 1.6; }
.question-meta { margin: 0; color: var(--ir-muted); font-size: 12px; }
.question-answer {
  margin: 2px 0 0;
  padding: 10px 12px;
  border-radius: var(--ir-radius-sm);
  background: var(--ir-surface-muted);
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
}
.question-pagination { justify-content: center; }
@media (max-width: 480px) {
  .keyword-input { width: 100%; }
  .question-filter .el-select { flex: 1; }
}
</style>
