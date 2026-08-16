import { request } from '@/shared/api/http'
import type { PositionStatus, StatusRequest } from './tracking.types'

export async function listStatuses(): Promise<PositionStatus[]> {
  return (await request<PositionStatus[]>({ method: 'get', url: '/statuses' })).data
}

export async function createStatus(payload: StatusRequest): Promise<PositionStatus> {
  return (await request<PositionStatus>({ method: 'post', url: '/statuses', data: payload })).data
}

export async function updateStatus(id: string, payload: StatusRequest): Promise<PositionStatus> {
  return (await request<PositionStatus>({ method: 'put', url: `/statuses/${id}`, data: payload })).data
}

export async function reorderStatuses(orderedIds: string[]): Promise<PositionStatus[]> {
  return (await request<PositionStatus[]>({ method: 'put', url: '/statuses/order', data: { orderedIds } })).data
}

export async function deleteStatus(id: string, migrateToId: string): Promise<void> {
  await request({ method: 'delete', url: `/statuses/${id}`, params: { migrateToId } })
}
