<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElButton, ElInput } from 'element-plus'
import type { CompanyRequest } from '../api/tracking.types'

const props = defineProps<{ initial?: CompanyRequest | null; submitLabel?: string }>()
const emit = defineEmits<{ submitted: [payload: CompanyRequest] }>()
const fieldErrors = ref<Record<string, string>>({})
const form = reactive({
  name: props.initial?.name ?? '',
  website: props.initial?.website ?? '',
  notes: props.initial?.notes ?? '',
})

function submit() {
  fieldErrors.value = {}
  const name = form.name.trim()
  if (!name) fieldErrors.value.name = '公司名称不能为空'
  else if (name.length > 120) fieldErrors.value.name = '公司名称不能超过 120 个字符'
  const website = form.website.trim()
  if (website && !/^https?:\/\//.test(website)) fieldErrors.value.website = '官网必须以 http:// 或 https:// 开头'
  if (form.notes.length > 2000) fieldErrors.value.notes = '备注不能超过 2000 个字符'
  if (Object.keys(fieldErrors.value).length > 0) return
  emit('submitted', {
    name,
    website: website || null,
    notes: form.notes.trim() || null,
    confirmDuplicate: props.initial?.confirmDuplicate,
  })
}
</script>

<template>
  <form novalidate @submit.prevent="submit">
    <label for="company-name">公司名称 *</label>
    <ElInput id="company-name" v-model="form.name" name="name" maxlength="120" :aria-describedby="fieldErrors.name ? 'company-name-error' : undefined" />
    <p v-if="fieldErrors.name" id="company-name-error" data-field-error="name" role="alert">{{ fieldErrors.name }}</p>

    <label for="company-website">官网（可选）</label>
    <ElInput id="company-website" v-model="form.website" name="website" placeholder="https://example.com" :aria-describedby="fieldErrors.website ? 'company-website-error' : undefined" />
    <p v-if="fieldErrors.website" id="company-website-error" data-field-error="website" role="alert">{{ fieldErrors.website }}</p>

    <label for="company-notes">备注（可选）</label>
    <ElInput id="company-notes" v-model="form.notes" name="notes" type="textarea" :rows="3" maxlength="2000" :aria-describedby="fieldErrors.notes ? 'company-notes-error' : undefined" />
    <p v-if="fieldErrors.notes" id="company-notes-error" data-field-error="notes" role="alert">{{ fieldErrors.notes }}</p>

    <ElButton native-type="submit" type="primary" data-action="submit-company">{{ submitLabel ?? '保存公司' }}</ElButton>
  </form>
</template>
