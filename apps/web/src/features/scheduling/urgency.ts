import type { Schedule } from './api/schedules.api'

export type UrgencyLevel = 'URGENT' | 'APPROACHING' | 'NORMAL' | 'HANDLED'

export interface UrgencyDisplay {
  label: string
  icon: string
  /** CSS 类,对应 theme.css 中的语义状态色 */
  className: string
  /** Element Plus tag 类型,用于标签组件着色 */
  tagType: 'danger' | 'warning' | 'primary' | 'info'
}

const URGENCY_DISPLAY: Record<UrgencyLevel, UrgencyDisplay> = {
  URGENT: { label: '紧急', icon: '⚠', className: 'urgency-urgent', tagType: 'danger' },
  APPROACHING: { label: '临近', icon: '◷', className: 'urgency-approaching', tagType: 'warning' },
  NORMAL: { label: '普通', icon: '●', className: 'urgency-normal', tagType: 'primary' },
  HANDLED: { label: '已处理', icon: '✓', className: 'urgency-handled', tagType: 'info' },
}

export function urgencyDisplay(urgency: string): UrgencyDisplay {
  return URGENCY_DISPLAY[urgency as UrgencyLevel] ?? URGENCY_DISPLAY.NORMAL
}

/** 剩余时间文字,例如 "已逾期 3 小时"、"还剩 2 天" */
export function urgencyCountdown(schedule: Schedule, now: Date = new Date()): string {
  const reference = schedule.startsAt ?? schedule.endsAt
  if (!reference) return ''
  const diffMs = new Date(reference).getTime() - now.getTime()
  const absMs = Math.abs(diffMs)
  const hours = Math.floor(absMs / 3_600_000)
  const text =
    hours < 1
      ? `${Math.max(1, Math.floor(absMs / 60_000))} 分钟`
      : hours < 48
        ? `${hours} 小时`
        : `${Math.floor(hours / 24)} 天`
  return diffMs < 0 ? `已逾期 ${text}` : `还剩 ${text}`
}

/** 日程按日期分组的标题：今天 / 明天 / 周几 / 具体日期 / 已逾期 */
export function dateGroupLabel(iso: string, now: Date = new Date()): string {
  if (!iso) return '时间未定'
  const target = new Date(iso)
  const today = new Date(now); today.setHours(0, 0, 0, 0)
  const day = new Date(target); day.setHours(0, 0, 0, 0)
  const diffDays = Math.round((day.getTime() - today.getTime()) / 86_400_000)
  if (diffDays < 0) return '已逾期'
  if (diffDays === 0) return '今天'
  if (diffDays === 1) return '明天'
  const weekdays = '日一二三四五六'
  const label = `${day.getMonth() + 1}月${day.getDate()}日`
  return diffDays < 7 ? `${label} · 周${weekdays[day.getDay()]}` : label
}

/** Dashboard/列表排序:逾期置顶,其次按紧急程度、参考时间升序 */
export function sortByUrgency(schedules: Schedule[]): Schedule[] {
  const rank: Record<string, number> = { URGENT: 0, APPROACHING: 1, NORMAL: 2, HANDLED: 3 }
  return [...schedules].sort((a, b) => {
    const aOverdue = a.overdue && a.status === 'PENDING' ? 0 : 1
    const bOverdue = b.overdue && b.status === 'PENDING' ? 0 : 1
    if (aOverdue !== bOverdue) return aOverdue - bOverdue
    const urgencyDiff = (rank[a.urgency] ?? 9) - (rank[b.urgency] ?? 9)
    if (urgencyDiff !== 0) return urgencyDiff
    const aTime = a.startsAt ?? a.endsAt ?? ''
    const bTime = b.startsAt ?? b.endsAt ?? ''
    return aTime.localeCompare(bTime)
  })
}
