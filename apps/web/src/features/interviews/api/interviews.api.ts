import { request } from '@/shared/api/http'

export interface InterviewQuestion {
  question: string
  answer?: string | null
  category?: string | null
}

export interface InterviewQuestionItem extends InterviewQuestion {
  sortOrder: number
}

export interface RoundRequest {
  roundName: string
  roundNumber: number
  interviewType: string
  startsAt?: string | null
  endsAt?: string | null
  location?: string | null
  result: string
  processNotes?: string | null
  reviewSummary?: string | null
  questions?: InterviewQuestion[]
  createSchedule?: boolean
  version?: number | null
}

export interface InterviewRound {
  id: string
  positionId: string
  positionTitle: string
  companyName: string
  roundName: string
  roundNumber: number
  interviewType: string
  startsAt: string | null
  endsAt: string | null
  location: string | null
  result: string
  processNotes: string | null
  reviewSummary: string | null
  questions: InterviewQuestionItem[]
  scheduleIds: string[]
  version: number
  createdAt: string
  updatedAt: string
}

export const INTERVIEW_TYPES = [
  { value: 'PHONE', label: '电话面试' },
  { value: 'VIDEO', label: '视频面试' },
  { value: 'ONSITE', label: '现场面试' },
  { value: 'WRITTEN_TEST', label: '笔试' },
  { value: 'OTHER', label: '其他' },
] as const

export const INTERVIEW_RESULTS = [
  { value: 'UPCOMING', label: '待面试' },
  { value: 'AWAITING_RESULT', label: '等待结果' },
  { value: 'PASSED', label: '通过' },
  { value: 'FAILED', label: '未通过' },
  { value: 'CANCELLED', label: '已取消' },
] as const

export function interviewTypeLabel(value: string): string {
  return INTERVIEW_TYPES.find((item) => item.value === value)?.label ?? value
}

export function interviewResultLabel(value: string): string {
  return INTERVIEW_RESULTS.find((item) => item.value === value)?.label ?? value
}

export async function listRounds(positionId: string): Promise<InterviewRound[]> {
  const response = await request<{ items: InterviewRound[] }>({
    method: 'get',
    url: `/positions/${positionId}/interview-rounds`,
  })
  return response.data.items
}

export async function createRound(positionId: string, payload: RoundRequest): Promise<InterviewRound> {
  return (
    await request<InterviewRound>({
      method: 'post',
      url: `/positions/${positionId}/interview-rounds`,
      data: payload,
    })
  ).data
}

export async function getRound(id: string): Promise<InterviewRound> {
  return (await request<InterviewRound>({ method: 'get', url: `/interview-rounds/${id}` })).data
}

export async function updateRound(id: string, payload: RoundRequest): Promise<InterviewRound> {
  return (
    await request<InterviewRound>({ method: 'put', url: `/interview-rounds/${id}`, data: payload })
  ).data
}

export interface QuestionBankItem {
  id: string
  question: string
  answer: string | null
  category: string | null
  roundId: string | null
  roundNumber: number | null
  roundName: string | null
  positionId: string | null
  positionTitle: string | null
  companyName: string | null
  createdAt: string
}

export interface QuestionBankPage {
  items: QuestionBankItem[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export async function searchQuestionBank(params: {
  category?: string
  keyword?: string
  page?: number
  size?: number
}): Promise<QuestionBankPage> {
  return (
    await request<QuestionBankPage>({ method: 'get', url: '/interview-rounds/questions', params })
  ).data
}

export async function randomQuestions(limit = 10): Promise<QuestionBankItem[]> {
  return (
    await request<QuestionBankItem[]>({
      method: 'get',
      url: '/interview-rounds/questions/random',
      params: { limit },
    })
  ).data
}

export async function questionCategories(): Promise<string[]> {
  return (await request<string[]>({ method: 'get', url: '/interview-rounds/question-categories' })).data
}

export async function deleteRound(id: string): Promise<void> {
  await request({ method: 'delete', url: `/interview-rounds/${id}` })
}
