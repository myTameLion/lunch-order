/**
 * 日期工具（全部基于本地时区做 yyyy-MM-dd 处理；展示"登记时间"时按契约统一转为 Asia/Shanghai）。
 * 命名约定（glossary.md）：日期 yyyy-MM-dd；时间字符串 ISO 8601 带时区。
 */

const DATE_RE = /^\d{4}-\d{2}-\d{2}$/

/** Date → yyyy-MM-dd（本地时区） */
export function formatDate(date: Date): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

/** 返回新 Date，不修改入参 */
export function addDays(date: Date, days: number): Date {
  const next = new Date(date)
  next.setDate(next.getDate() + days)
  return next
}

/** yyyy-MM-dd → 本地时区当日 0 点的 Date；非法输入返回 null */
export function parseDate(value: string): Date | null {
  if (!DATE_RE.test(value)) return null
  const [y, m, d] = value.split('-').map(Number)
  const date = new Date(y, m - 1, d)
  return Number.isNaN(date.getTime()) ? null : date
}

/** 区间自然日数，含 from 与 to 两天（glossary.md「参与率」口径）；非法输入返回 0 */
export function daysBetweenInclusive(from: string, to: string): number {
  const a = parseDate(from)
  const b = parseDate(to)
  if (!a || !b) return 0
  const diff = Date.UTC(b.getFullYear(), b.getMonth(), b.getDate()) - Date.UTC(a.getFullYear(), a.getMonth(), a.getDate())
  const days = Math.round(diff / 86_400_000) + 1
  return days > 0 ? days : 0
}

/** 以 end 为止（含）往前取 n 天的 [from, to]，默认截止今天 */
export function lastNDays(n: number, end: Date = new Date()): [string, string] {
  const to = formatDate(end)
  const from = formatDate(addDays(end, -(Math.max(1, n) - 1)))
  return [from, to]
}

const SHANGHAI_DATE_TIME_FORMATTER = new Intl.DateTimeFormat('en-CA', {
  timeZone: 'Asia/Shanghai',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
})

/** ISO 8601 时间字符串 → 上海时区 "yyyy-MM-dd HH:mm"；非法输入返回 "–" */
export function formatDateTime(iso: string): string {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return '–'
  const parts = SHANGHAI_DATE_TIME_FORMATTER.formatToParts(date)
  const get = (type: Intl.DateTimeFormatPartTypes) => parts.find((p) => p.type === type)?.value ?? ''
  return `${get('year')}-${get('month')}-${get('day')} ${get('hour')}:${get('minute')}`
}
