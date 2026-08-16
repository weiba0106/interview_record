<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElButton, ElOption, ElSelect } from 'element-plus'
import { listJobTypes } from '@/features/tracking/api/job-types.api'
import { getInsights, type InsightsResponse } from '@/features/insights/api/insights.api'
import { isApiRequestError } from '@/shared/api/error'

const loading = ref(true)
const error = ref('')
const jobTypes = ref<{ id: string; name: string }[]>([])
const selectedJobType = ref('')
const appliedFrom = ref('')
const appliedTo = ref('')
const insights = ref<InsightsResponse | null>(null)

async function load() {
  loading.value = true; error.value = ''
  try {
    const [result, types] = await Promise.all([
      getInsights({ jobTypeId: selectedJobType.value || undefined, appliedFrom: appliedFrom.value || undefined, appliedTo: appliedTo.value || undefined }),
      listJobTypes(),
    ])
    insights.value = result
    jobTypes.value = types.map((item) => ({ id: item.id, name: item.name }))
  } catch (caught) {
    error.value = isApiRequestError(caught) ? caught.apiError.message : '统计加载失败，请稍后重试'
  } finally { loading.value = false }
}

function percent(rate: { available: boolean; percentage: number | null }) {
  return rate.available && rate.percentage !== null ? `${rate.percentage.toFixed(1)}%` : '暂无数据'
}
onMounted(() => { void load() })
</script>

<template>
  <main class="insights-page" aria-labelledby="insights-heading">
    <div class="page-head">
      <div>
        <span class="eyebrow">复盘与决策</span>
        <h1 id="insights-heading">统计</h1>
        <p class="page-desc">用真实记录看清投递、面试和 Offer 转化。</p>
      </div>
    </div>
    <p v-if="error" role="alert">{{ error }}</p>
    <p v-if="loading" role="status">加载中…</p>
    <template v-else-if="insights">
      <section class="metric-grid" aria-label="转化指标">
        <article class="metric-card">
          <div class="metric-top">
            <span class="metric-value">{{ percent(insights.conversions.interviewReachRate) }}</span>
            <span class="metric-icon" aria-hidden="true">⇢</span>
          </div>
          <span class="metric-label">面试触达率</span>
          <small class="metric-hint">有面试轮次的岗位 / 已投递岗位</small>
        </article>
        <article class="metric-card">
          <div class="metric-top">
            <span class="metric-value">{{ percent(insights.conversions.offerConversionRate) }}</span>
            <span class="metric-icon" aria-hidden="true">✓</span>
          </div>
          <span class="metric-label">Offer 转化率</span>
          <small class="metric-hint">成功状态 / 已投递岗位</small>
        </article>
        <article class="metric-card">
          <div class="metric-top">
            <span class="metric-value">{{ percent(insights.conversions.interviewPassRate) }}</span>
            <span class="metric-icon" aria-hidden="true">◈</span>
          </div>
          <span class="metric-label">面试通过率</span>
          <small class="metric-hint">通过轮次 / 已判定轮次</small>
        </article>
      </section>

      <div class="ir-panel">
        <form class="insights-filters" @submit.prevent="load">
          <label class="filter-field">招聘类型
            <ElSelect v-model="selectedJobType" name="insightsJobType" placeholder="全部类型" clearable>
              <ElOption v-for="type in jobTypes" :key="type.id" :label="type.name" :value="type.id" />
            </ElSelect>
          </label>
          <label class="filter-field">投递开始<input v-model="appliedFrom" name="appliedFrom" type="date" /></label>
          <label class="filter-field">投递结束<input v-model="appliedTo" name="appliedTo" type="date" /></label>
          <ElButton native-type="submit" type="primary" data-action="apply-insight-filters">应用筛选</ElButton>
        </form>
      </div>

      <div class="insight-columns">
        <section class="ir-panel" aria-label="状态分布">
          <div class="ir-panel-head"><div><span class="panel-kicker">岗位状态</span><h2>状态分布</h2></div></div>
          <div v-if="insights.statusDistribution.length" class="bar-list">
            <div v-for="item in insights.statusDistribution" :key="item.statusId" class="bar-row">
              <span>{{ item.statusName }}</span>
              <div class="bar-track"><i :style="{ width: `${Math.min(item.percentage, 100)}%` }" /></div>
              <b>{{ item.count }}</b>
            </div>
          </div>
          <div v-else class="ir-empty">
            <span class="ir-empty-icon" aria-hidden="true">▤</span>
            <strong>暂无岗位数据</strong>
            <p>创建岗位后，状态分布会显示在这里。</p>
          </div>
        </section>
        <section class="ir-panel" aria-label="招聘类型分布">
          <div class="ir-panel-head"><div><span class="panel-kicker">招聘类型</span><h2>类型对比</h2></div></div>
          <div v-if="insights.jobTypeBreakdown.length" class="table-scroll">
            <table class="ir-table">
              <thead><tr><th scope="col">类型</th><th scope="col">投递</th><th scope="col">面试岗位</th><th scope="col">Offer</th></tr></thead>
              <tbody>
                <tr v-for="item in insights.jobTypeBreakdown" :key="item.jobTypeId">
                  <td class="cell-strong">{{ item.jobTypeName }}</td><td>{{ item.applicationCount }}</td><td>{{ item.interviewedPositionCount }}</td><td>{{ item.offerCount }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="ir-empty">
            <span class="ir-empty-icon" aria-hidden="true">▤</span>
            <strong>暂无数据</strong>
            <p>选择筛选范围后查看各类型的投递与面试情况。</p>
          </div>
        </section>
      </div>

      <section class="ir-panel" aria-label="投递趋势">
        <div class="ir-panel-head"><div><span class="panel-kicker">时间线</span><h2>投递趋势</h2></div></div>
        <div v-if="insights.applicationTrend.length" class="table-scroll">
          <table class="ir-table">
            <thead><tr><th scope="col">日期</th><th scope="col">投递岗位</th><th scope="col">面试轮次</th></tr></thead>
            <tbody>
              <tr v-for="item in insights.applicationTrend" :key="item.date">
                <td class="cell-strong">{{ item.date }}</td><td>{{ item.applicationCount }}</td><td>{{ item.interviewRoundCount }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="ir-empty">
          <span class="ir-empty-icon" aria-hidden="true">▦</span>
          <strong>筛选范围内暂无投递记录</strong>
          <p>填写投递日期后，趋势会按时间汇总。</p>
        </div>
      </section>
    </template>
  </main>
</template>

<style scoped>
.insights-page { display: grid; gap: 16px; }
.metric-hint { color: var(--ir-faint); font-size: 11px; }
.insights-filters { display: flex; flex-wrap: wrap; gap: 14px; align-items: end; padding: 12px 16px; }
.filter-field { display: grid; gap: 5px; color: var(--ir-muted); font-size: 12.5px; font-weight: 650; }
.filter-field .el-select { width: 180px; }
.filter-field input { width: 158px; }
.insight-columns { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; align-items: start; }
.bar-list { display: grid; gap: 12px; padding: 14px 16px 16px; }
.bar-row { display: grid; grid-template-columns: 110px 1fr 34px; align-items: center; gap: 10px; font-size: 13px; }
.bar-row span { color: var(--ir-text); }
.bar-row b { color: var(--ir-muted); font-size: 12px; text-align: right; }
.bar-track { height: 10px; background: var(--ir-surface-muted); border-radius: 8px; overflow: hidden; }
.bar-track i { display: block; height: 100%; background: var(--ir-primary-strong); border-radius: inherit; }
@media (max-width: 760px) {
  .insight-columns { grid-template-columns: 1fr; }
  .bar-row { grid-template-columns: 92px 1fr 30px; }
  .filter-field, .filter-field .el-select, .filter-field input { width: 100%; }
}
</style>
