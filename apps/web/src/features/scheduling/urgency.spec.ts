import { describe, expect, it } from 'vitest'
import { sortByUrgency, urgencyCountdown, urgencyDisplay } from './urgency'
import type { Schedule } from './api/schedules.api'

function schedule(partial: Partial<Schedule>): Schedule {
  return {
    id: '1',
    title: '日程',
    eventType: 'INTERVIEW',
    startsAt: null,
    endsAt: null,
    positionId: null,
    positionTitle: null,
    interviewRoundId: null,
    location: null,
    notes: null,
    status: 'PENDING',
    urgency: 'NORMAL',
    overdue: false,
    manualUrgency: null,
    referenceTime: '2026-08-12T00:00:00Z',
    version: 0,
    updatedAt: '2026-08-12T00:00:00Z',
    ...partial,
  }
}

describe('urgencyDisplay', () => {
  it('maps urgency levels to Chinese labels and semantic CSS classes', () => {
    expect(urgencyDisplay('URGENT')).toMatchObject({ label: '紧急', icon: '⚠', className: 'urgency-urgent', tagType: 'danger' })
    expect(urgencyDisplay('APPROACHING')).toMatchObject({ label: '临近', icon: '◷', className: 'urgency-approaching', tagType: 'warning' })
    expect(urgencyDisplay('NORMAL')).toMatchObject({ label: '普通', icon: '●', className: 'urgency-normal', tagType: 'primary' })
    expect(urgencyDisplay('HANDLED')).toMatchObject({ label: '已处理', icon: '✓', className: 'urgency-handled', tagType: 'info' })
  })

  it('falls back to the normal display for unknown urgency values', () => {
    expect(urgencyDisplay('MYSTERY').className).toBe('urgency-normal')
  })
})

describe('urgencyCountdown', () => {
  const now = new Date('2026-08-12T12:00:00Z')

  it('shows remaining time for future schedules', () => {
    expect(urgencyCountdown(schedule({ startsAt: '2026-08-12T18:00:00Z' }), now)).toBe('还剩 6 小时')
    expect(urgencyCountdown(schedule({ startsAt: '2026-08-15T12:00:00Z' }), now)).toBe('还剩 3 天')
  })

  it('shows overdue time for past schedules', () => {
    expect(urgencyCountdown(schedule({ endsAt: '2026-08-10T12:00:00Z' }), now)).toBe('已逾期 2 天')
  })
})

describe('sortByUrgency', () => {
  it('pins overdue pending schedules first, then sorts by urgency and time', () => {
    const overdue = schedule({ id: 'overdue', urgency: 'URGENT', overdue: true, startsAt: '2026-08-11T09:00:00Z' })
    const urgent = schedule({ id: 'urgent', urgency: 'URGENT', startsAt: '2026-08-12T14:00:00Z' })
    const approaching = schedule({ id: 'approaching', urgency: 'APPROACHING', startsAt: '2026-08-13T14:00:00Z' })
    const normal = schedule({ id: 'normal', urgency: 'NORMAL', startsAt: '2026-08-20T14:00:00Z' })

    const sorted = sortByUrgency([normal, approaching, urgent, overdue])
    expect(sorted.map((item) => item.id)).toEqual(['overdue', 'urgent', 'approaching', 'normal'])
  })
})
