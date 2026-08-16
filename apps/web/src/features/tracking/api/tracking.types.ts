export interface Company {
  id: string
  name: string
  website: string | null
  notes: string | null
  positionCount: number
  createdAt: string
  updatedAt: string
}

export interface PositionSummary {
  id: string
  title: string
  companyId: string
  companyName: string
  jobTypeId: string
  jobTypeName: string
  status: StatusRef
  appliedAt: string | null
  deadlineAt: string | null
  archived: boolean
  updatedAt: string
}

export interface CompanyDetail extends Company {
  interviewRoundCount: number
  scheduleCount: number
  shareLinkCount: number
  positions: PositionSummary[]
}

export interface CompanyRequest {
  name: string
  website?: string | null
  notes?: string | null
  confirmDuplicate?: boolean
}

export interface JobType {
  id: string
  name: string
  active: boolean
}

export interface JobTypeRequest {
  name: string
  active?: boolean | null
}

export interface StatusRef {
  id: string
  name: string
  color: string
  statisticsCategory: string
}

export interface PositionStatus extends StatusRef {
  sortOrder: number
  active: boolean
  positionCount: number
}

export interface StatusRequest {
  name: string
  color: string
  statisticsCategory: string
  active?: boolean | null
}

export interface NextScheduleRef {
  id: string
  title: string
  eventType: string
  time: string
}

export interface Position {
  id: string
  title: string
  companyId: string
  companyName: string
  jobTypeId: string
  jobTypeName: string
  status: StatusRef
  applyUrl: string | null
  appliedAt: string | null
  deadlineAt: string | null
  workLocation: string | null
  description: string | null
  archived: boolean
  interviewRoundCount: number
  scheduleCount: number
  nextSchedule: NextScheduleRef | null
  version: number
  createdAt: string
  updatedAt: string
}

export interface PositionRequest {
  companyId?: string | null
  newCompanyName?: string | null
  jobTypeId: string
  statusId: string
  title: string
  applyUrl?: string | null
  appliedAt?: string | null
  deadlineAt?: string | null
  workLocation?: string | null
  description?: string | null
  createDeadlineSchedule?: boolean
  version?: number | null
}

export interface PositionPage {
  items: Position[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export interface PositionSearchParams {
  companyId?: string
  jobTypeId?: string
  statusId?: string
  archived?: boolean
  keyword?: string
  /** 投递日期范围（YYYY-MM-DD） */
  appliedFrom?: string
  appliedTo?: string
  page?: number
  size?: number
  sortBy?: string
  sortDir?: 'asc' | 'desc'
}
