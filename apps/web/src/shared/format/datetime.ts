import dayjs from 'dayjs'

/** 展示用日期时间,例如 2026-08-12 14:30 */
export function formatDateTime(value: string | null | undefined): string {
  if (!value) return '—'
  const parsed = dayjs(value)
  return parsed.isValid() ? parsed.format('YYYY-MM-DD HH:mm') : '—'
}

/** 展示用日期,例如 2026-08-12 */
export function formatDate(value: string | null | undefined): string {
  if (!value) return '—'
  const parsed = dayjs(value)
  return parsed.isValid() ? parsed.format('YYYY-MM-DD') : '—'
}

/** ISO 字符串转 datetime-local 输入值(本地时区) */
export function toDatetimeInput(value: string | null | undefined): string {
  if (!value) return ''
  const parsed = dayjs(value)
  return parsed.isValid() ? parsed.format('YYYY-MM-DDTHH:mm:ss') : ''
}

/** datetime-local 输入值转 ISO 字符串;空输入返回 null */
export function fromDatetimeInput(value: string): string | null {
  if (!value) return null
  const parsed = dayjs(value)
  return parsed.isValid() ? parsed.toISOString() : null
}

/** ISO 字符串转 date 输入值(本地时区) */
export function toDateInput(value: string | null | undefined): string {
  if (!value) return ''
  const parsed = dayjs(value)
  return parsed.isValid() ? parsed.format('YYYY-MM-DD') : ''
}

/** date 输入值转 ISO 日期字符串(当日零点);空输入返回 null */
export function fromDateInput(value: string): string | null {
  if (!value) return null
  const parsed = dayjs(value)
  return parsed.isValid() ? parsed.toISOString() : null
}
