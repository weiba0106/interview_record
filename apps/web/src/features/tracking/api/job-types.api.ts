import { request } from '@/shared/api/http'
import type { JobType, JobTypeRequest } from './tracking.types'

export async function listJobTypes(): Promise<JobType[]> {
  return (await request<JobType[]>({ method: 'get', url: '/job-types' })).data
}

export async function createJobType(payload: JobTypeRequest): Promise<JobType> {
  return (await request<JobType>({ method: 'post', url: '/job-types', data: payload })).data
}

export async function updateJobType(id: string, payload: JobTypeRequest): Promise<JobType> {
  return (await request<JobType>({ method: 'put', url: `/job-types/${id}`, data: payload })).data
}
