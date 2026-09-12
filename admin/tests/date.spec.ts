import { describe, expect, it } from 'vitest'
import { addDays, daysBetweenInclusive, formatDate, formatDateTime, lastNDays, parseDate } from '@/utils/date'

describe('日期工具', () => {
  it('formatDate 输出 yyyy-MM-dd', () => {
    expect(formatDate(new Date(2026, 8, 7))).toBe('2026-09-07')
  })

  it('addDays 跨月进位且不改入参', () => {
    const base = new Date(2026, 8, 30)
    expect(formatDate(addDays(base, 1))).toBe('2026-10-01')
    expect(formatDate(base)).toBe('2026-09-30')
  })

  it('parseDate 严格 yyyy-MM-dd', () => {
    expect(parseDate('2026-09-07')?.getDate()).toBe(7)
    expect(parseDate('2026-9-7')).toBeNull()
    expect(parseDate('abc')).toBeNull()
  })

  it('daysBetweenInclusive 含首尾（glossary 参与率口径）', () => {
    expect(daysBetweenInclusive('2026-09-01', '2026-09-07')).toBe(7)
    expect(daysBetweenInclusive('2026-09-07', '2026-09-07')).toBe(1)
    expect(daysBetweenInclusive('2026-09-07', '2026-09-01')).toBe(0)
    expect(daysBetweenInclusive('bad', '2026-09-01')).toBe(0)
  })

  it('lastNDays 返回 [from, to]', () => {
    const [from, to] = lastNDays(7, new Date(2026, 8, 7))
    expect(from).toBe('2026-09-01')
    expect(to).toBe('2026-09-07')
  })

  it('formatDateTime 固定按 Asia/Shanghai 展示', () => {
    expect(formatDateTime('2026-09-07T15:01:09+08:00')).toBe('2026-09-07 15:01')
    expect(formatDateTime('2026-09-07T07:01:09Z')).toBe('2026-09-07 15:01')
    expect(formatDateTime('not-a-date')).toBe('–')
  })
})
