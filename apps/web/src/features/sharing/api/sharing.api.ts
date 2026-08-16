import { request } from '@/shared/api/http'

export type PositionShareField = 'COMPANY_NAME' | 'POSITION_TITLE' | 'JOB_TYPE' | 'STATUS'
export type RoundShareField = 'BASIC_INFO' | 'QUESTIONS' | 'ANSWERS' | 'PROCESS' | 'REVIEW' | 'RESULT'
export interface RoundSelection { roundId: string; visibleFields: RoundShareField[] }
export interface CreatedShare { id: string; token: string; expiresAt: string | null; publicPath: string }
export interface ShareLink { id: string; positionFields: PositionShareField[]; rounds: RoundSelection[]; expiresAt: string | null; revokedAt: string | null; createdAt: string }
export interface CreateSharePayload { positionFields: PositionShareField[]; rounds: RoundSelection[]; expiry: 'ONE_DAY' | 'SEVEN_DAYS' | 'THIRTY_DAYS' | 'PERMANENT' }
export interface PublicShare { position: Record<string, unknown>; rounds: { id: string; content: Record<string, unknown> }[]; robots: string }

export async function createShare(positionId: string, payload: CreateSharePayload): Promise<CreatedShare> {
  return (await request<CreatedShare>({ method: 'post', url: `/positions/${positionId}/shares`, data: payload })).data
}
export async function listShares(positionId: string): Promise<ShareLink[]> {
  return (await request<ShareLink[]>({ method: 'get', url: `/positions/${positionId}/shares` })).data
}
export async function revokeShare(positionId: string, shareId: string): Promise<void> {
  await request({ method: 'delete', url: `/positions/${positionId}/shares/${shareId}` })
}
export async function getPublicShare(token: string): Promise<PublicShare> {
  return (await request<PublicShare>({ method: 'get', url: `/shares/${encodeURIComponent(token)}` })).data
}
