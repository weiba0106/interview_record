import { request } from '@/shared/api/http'
import type { Position, PositionPage, PositionRequest, PositionSearchParams } from './tracking.types'

export async function searchPositions(params: PositionSearchParams = {}): Promise<PositionPage> {
  const query: Record<string, string | number | boolean> = {}
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') query[key] = value
  }
  return (await request<PositionPage>({ method: 'get', url: '/positions', params: query })).data
}

export async function getPosition(id: string): Promise<Position> {
  return (await request<Position>({ method: 'get', url: `/positions/${id}` })).data
}

export async function createPosition(payload: PositionRequest): Promise<Position> {
  return (await request<Position>({ method: 'post', url: '/positions', data: payload })).data
}

export async function updatePosition(id: string, payload: PositionRequest): Promise<Position> {
  return (await request<Position>({ method: 'put', url: `/positions/${id}`, data: payload })).data
}

export async function changePositionStatus(id: string, statusId: string): Promise<Position> {
  return (await request<Position>({ method: 'patch', url: `/positions/${id}/status`, data: { statusId } })).data
}

export async function setPositionArchived(id: string, archived: boolean): Promise<Position> {
  return (await request<Position>({ method: 'patch', url: `/positions/${id}/archive`, params: { archived } })).data
}

export async function deletePosition(id: string, confirmed: boolean): Promise<void> {
  await request({ method: 'delete', url: `/positions/${id}`, params: { confirmed } })
}
