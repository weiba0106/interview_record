import { request } from '@/shared/api/http'

export interface InsightFilter {
  jobTypeId?: string
  appliedFrom?: string
  appliedTo?: string
}
export interface StatusDistributionItem { statusId: string; statusName: string; statisticsCategory: string; count: number; percentage: number }
export interface JobTypeBreakdownItem { jobTypeId: string; jobTypeName: string; applicationCount: number; interviewedPositionCount: number; offerCount: number }
export interface ApplicationTrendItem { date: string; applicationCount: number; interviewRoundCount: number }
export interface ConversionRate { available: boolean; percentage: number | null }
export interface InsightsResponse {
  statusDistribution: StatusDistributionItem[]
  jobTypeBreakdown: JobTypeBreakdownItem[]
  applicationTrend: ApplicationTrendItem[]
  conversions: { interviewReachRate: ConversionRate; offerConversionRate: ConversionRate; interviewPassRate: ConversionRate }
}

export async function getInsights(params: InsightFilter = {}): Promise<InsightsResponse> {
  const query: Record<string, string> = {}
  if (params.jobTypeId) query.jobTypeId = params.jobTypeId
  if (params.appliedFrom) query.appliedFrom = params.appliedFrom
  if (params.appliedTo) query.appliedTo = params.appliedTo
  return (await request<InsightsResponse>({ method: 'get', url: '/insights', params: query })).data
}
