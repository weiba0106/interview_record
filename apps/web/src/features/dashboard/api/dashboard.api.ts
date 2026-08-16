import { request } from '@/shared/api/http'
import type { Schedule } from '@/features/scheduling/api/schedules.api'
import type { Position } from '@/features/tracking/api/tracking.types'

export interface DashboardMetrics {
  totalPositions: number
  activePositions: number
  upcomingScheduleCount: number
  offerCount: number
}

export interface DashboardData {
  metrics: DashboardMetrics
  positions: Position[]
  schedules: Schedule[]
}

export async function getDashboard(): Promise<DashboardData> {
  return (await request<DashboardData>({ method: 'get', url: '/dashboard' })).data
}
