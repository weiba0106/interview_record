import { request } from '@/shared/api/http'

export interface Schedule {
  id: string
  title: string
  eventType: string
  startsAt: string | null
  endsAt: string | null
  positionId: string | null
  positionTitle: string | null
  interviewRoundId: string | null
  location: string | null
  notes: string | null
  status: string
  urgency: string
  overdue: boolean
  manualUrgency: string | null
  referenceTime: string
  version: number
  updatedAt: string
}

export interface ScheduleRequest {
  title: string
  eventType: string
  startsAt?: string | null
  endsAt?: string | null
  positionId?: string | null
  interviewRoundId?: string | null
  location?: string | null
  notes?: string | null
  version?: number | null
}

export const SCHEDULE_EVENT_TYPES = [
  { value: 'INTERVIEW', label: '面试' },
  { value: 'WRITTEN_TEST', label: '笔试' },
  { value: 'HR_COMMUNICATION', label: 'HR 沟通' },
  { value: 'APPLY_DEADLINE', label: '投递截止' },
  { value: 'OFFER_DEADLINE', label: 'Offer 截止' },
  { value: 'CUSTOM', label: '自定义' },
] as const

export const SCHEDULE_STATUSES = [
  { value: 'PENDING', label: '待处理' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'CANCELLED', label: '已取消' },
] as const

export function scheduleEventTypeLabel(value: string): string {
  return SCHEDULE_EVENT_TYPES.find((item) => item.value === value)?.label ?? value
}

export function scheduleStatusLabel(value: string): string {
  return SCHEDULE_STATUSES.find((item) => item.value === value)?.label ?? value
}

export async function listSchedules(status?: string): Promise<Schedule[]> {
  const response = await request<{ items: Schedule[] }>({
    method: 'get',
    url: '/schedules',
    params: status ? { status } : undefined,
  })
  return response.data.items
}

export async function createSchedule(payload: ScheduleRequest): Promise<Schedule> {
  return (await request<Schedule>({ method: 'post', url: '/schedules', data: payload })).data
}

export async function getSchedule(id: string): Promise<Schedule> {
  return (await request<Schedule>({ method: 'get', url: `/schedules/${id}` })).data
}

export async function updateSchedule(id: string, payload: ScheduleRequest): Promise<Schedule> {
  return (await request<Schedule>({ method: 'put', url: `/schedules/${id}`, data: payload })).data
}

export async function changeScheduleStatus(id: string, status: string): Promise<Schedule> {
  return (
    await request<Schedule>({ method: 'patch', url: `/schedules/${id}/status`, data: { status } })
  ).data
}

export async function overrideScheduleUrgency(id: string, urgency: string | null): Promise<Schedule> {
  return (
    await request<Schedule>({ method: 'patch', url: `/schedules/${id}/urgency`, data: { urgency } })
  ).data
}

export async function deleteSchedule(id: string): Promise<void> {
  await request({ method: 'delete', url: `/schedules/${id}` })
}
