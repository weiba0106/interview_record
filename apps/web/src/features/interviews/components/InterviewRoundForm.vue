<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElButton, ElCheckbox, ElInput, ElOption, ElSelect } from 'element-plus'
import RichTextEditor from '@/shared/components/RichTextEditor.vue'
import { INTERVIEW_RESULTS, INTERVIEW_TYPES, type InterviewRound, type RoundRequest } from '../api/interviews.api'
import { fromDatetimeInput, toDatetimeInput } from '@/shared/format/datetime'

const props = defineProps<{ initial?: InterviewRound | null; defaultRoundNumber?: number; submitLabel?: string }>()
const emit = defineEmits<{ submitted: [payload: RoundRequest] }>()
const fieldErrors = ref<Record<string, string>>({})

interface QuestionDraft {
  question: string
  answer: string
  category: string
}

const form = reactive({
  roundName: props.initial?.roundName ?? '',
  roundNumber: props.initial?.roundNumber ?? props.defaultRoundNumber ?? 1,
  interviewType: props.initial?.interviewType ?? '',
  startsAt: toDatetimeInput(props.initial?.startsAt),
  endsAt: toDatetimeInput(props.initial?.endsAt),
  location: props.initial?.location ?? '',
  result: props.initial?.result ?? 'UPCOMING',
  processNotes: props.initial?.processNotes ?? '',
  reviewSummary: props.initial?.reviewSummary ?? '',
  createSchedule: false,
})
const questions = ref<QuestionDraft[]>(
  (props.initial?.questions ?? []).map((item) => ({
    question: item.question,
    answer: item.answer ?? '',
    category: item.category ?? '',
  })),
)

function addQuestion() {
  questions.value.push({ question: '', answer: '', category: '' })
}

function removeQuestion(index: number) {
  questions.value.splice(index, 1)
}

function submit() {
  fieldErrors.value = {}
  if (!form.roundName.trim()) fieldErrors.value.roundName = '轮次名称不能为空'
  if (!Number.isInteger(form.roundNumber) || form.roundNumber < 1) fieldErrors.value.roundNumber = '轮次序号必须是大于 0 的整数'
  if (!form.interviewType) fieldErrors.value.interviewType = '请选择面试类型'
  if (!form.result) fieldErrors.value.result = '请选择面试结果'
  const startsAt = fromDatetimeInput(form.startsAt)
  const endsAt = fromDatetimeInput(form.endsAt)
  if (startsAt && endsAt && endsAt <= startsAt) fieldErrors.value.endsAt = '结束时间必须晚于开始时间'
  questions.value.forEach((item, index) => {
    if (!item.question.trim()) fieldErrors.value[`question-${index}`] = '问题内容不能为空'
  })
  if (Object.keys(fieldErrors.value).length > 0) return
  emit('submitted', {
    roundName: form.roundName.trim(),
    roundNumber: form.roundNumber,
    interviewType: form.interviewType,
    startsAt,
    endsAt,
    location: form.location.trim() || null,
    result: form.result,
    processNotes: form.processNotes.trim() || null,
    reviewSummary: form.reviewSummary.trim() || null,
    questions: questions.value
      .filter((item) => item.question.trim() || item.answer.trim())
      .map((item) => ({
        question: item.question.trim(),
        answer: item.answer.trim() || null,
        category: item.category.trim() || null,
      })),
    createSchedule: form.createSchedule,
    version: props.initial?.version ?? null,
  })
}
</script>

<template>
  <form novalidate @submit.prevent="submit" class="round-form">
    <label for="round-name">轮次名称 *</label>
    <ElInput id="round-name" v-model="form.roundName" name="roundName" placeholder="例如：一面、HR 面" maxlength="80" :aria-describedby="fieldErrors.roundName ? 'round-name-error' : undefined" />
    <p v-if="fieldErrors.roundName" id="round-name-error" data-field-error="roundName" role="alert">{{ fieldErrors.roundName }}</p>

    <label for="round-number">轮次序号 *</label>
    <input id="round-number" v-model.number="form.roundNumber" name="roundNumber" type="number" min="1" :aria-describedby="fieldErrors.roundNumber ? 'round-number-error' : undefined" />
    <p v-if="fieldErrors.roundNumber" id="round-number-error" data-field-error="roundNumber" role="alert">{{ fieldErrors.roundNumber }}</p>

    <label for="round-type">面试类型 *</label>
    <ElSelect id="round-type" v-model="form.interviewType" name="interviewType" placeholder="选择面试类型" :aria-describedby="fieldErrors.interviewType ? 'round-type-error' : undefined">
      <ElOption v-for="item in INTERVIEW_TYPES" :key="item.value" :label="item.label" :value="item.value" />
    </ElSelect>
    <p v-if="fieldErrors.interviewType" id="round-type-error" data-field-error="interviewType" role="alert">{{ fieldErrors.interviewType }}</p>

    <label for="round-starts-at">开始时间（可选）</label>
    <input id="round-starts-at" v-model="form.startsAt" name="startsAt" type="datetime-local" />

    <label for="round-ends-at">结束时间（可选）</label>
    <input id="round-ends-at" v-model="form.endsAt" name="endsAt" type="datetime-local" :aria-describedby="fieldErrors.endsAt ? 'round-ends-at-error' : undefined" />
    <p v-if="fieldErrors.endsAt" id="round-ends-at-error" data-field-error="endsAt" role="alert">{{ fieldErrors.endsAt }}</p>
    <ElCheckbox v-if="!initial" v-model="form.createSchedule" name="createSchedule">同时创建面试日程</ElCheckbox>

    <label for="round-location">面试地点或会议链接（可选）</label>
    <ElInput id="round-location" v-model="form.location" name="location" maxlength="500" />

    <label for="round-result">面试结果 *</label>
    <ElSelect id="round-result" v-model="form.result" name="result" :aria-describedby="fieldErrors.result ? 'round-result-error' : undefined">
      <ElOption v-for="item in INTERVIEW_RESULTS" :key="item.value" :label="item.label" :value="item.value" />
    </ElSelect>
    <p v-if="fieldErrors.result" id="round-result-error" data-field-error="result" role="alert">{{ fieldErrors.result }}</p>

    <label for="round-process-notes">面试过程记录（可选，支持富文本）</label>
    <RichTextEditor id="round-process-notes" :model-value="form.processNotes" placeholder="记录面试流程、提问节奏…" @update:model-value="form.processNotes = $event ?? ''" />

    <label for="round-review">整体复盘（可选，支持富文本）</label>
    <RichTextEditor id="round-review" :model-value="form.reviewSummary" placeholder="记录表现、反馈和改进项…" @update:model-value="form.reviewSummary = $event ?? ''" />

    <fieldset class="round-questions">
      <legend>面试问题与回答</legend>
      <div v-for="(item, index) in questions" :key="index" class="round-question-item">
        <label :for="`round-question-${index}`">问题 {{ index + 1 }} *</label>
        <ElInput :id="`round-question-${index}`" v-model="item.question" :name="`question-${index}`" type="textarea" :rows="2" maxlength="2000" :aria-describedby="fieldErrors[`question-${index}`] ? `round-question-${index}-error` : undefined" />
        <p v-if="fieldErrors[`question-${index}`]" :id="`round-question-${index}-error`" :data-field-error="`question-${index}`" role="alert">{{ fieldErrors[`question-${index}`] }}</p>
        <label :for="`round-answer-${index}`">回答（可选）</label>
        <ElInput :id="`round-answer-${index}`" v-model="item.answer" :name="`answer-${index}`" type="textarea" :rows="2" maxlength="4000" />
        <label :for="`round-category-${index}`">分类（可选，例如 算法、项目）</label>
        <ElInput :id="`round-category-${index}`" v-model="item.category" :name="`category-${index}`" maxlength="40" />
        <ElButton size="small" :data-action="`remove-question-${index}`" @click="removeQuestion(index)">删除该问题</ElButton>
      </div>
      <ElButton size="small" data-action="add-question" @click="addQuestion">添加问题</ElButton>
    </fieldset>

    <ElButton native-type="submit" type="primary" data-action="submit-round">{{ submitLabel ?? '保存面试轮次' }}</ElButton>
  </form>
</template>

<style scoped>
.round-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.round-questions {
  border: 1px solid var(--el-border-color, #dcdfe6);
  border-radius: 6px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.round-question-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-bottom: 8px;
  border-bottom: 1px dashed var(--el-border-color-lighter, #ebeef5);
}
</style>
