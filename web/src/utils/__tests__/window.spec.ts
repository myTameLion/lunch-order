import { describe, expect, it } from 'vitest'
import { formatOrderedAt, formatRemain, windowState } from '../window'

// 2026-09-07（周一）本地时区
const at = (h: number, m = 0, s = 0) => new Date(2026, 8, 7, h, m, s).getTime()

describe('windowState（契约 D-003：[start, end] 闭区间）', () => {
  it('13:59:59 → before，剩 1 秒开窗', () => {
    const st = windowState('14:00', '18:00', at(13, 59, 59))
    expect(st.phase).toBe('before')
    expect(st.remainSeconds).toBe(1)
  })

  it('14:00:00 → open（含开始边界）', () => {
    expect(windowState('14:00', '18:00', at(14)).phase).toBe('open')
  })

  it('17:59:59 → open 剩 1 秒', () => {
    const st = windowState('14:00', '18:00', at(17, 59, 59))
    expect(st.phase).toBe('open')
    expect(st.remainSeconds).toBe(1)
  })

  it('18:00:00 → open（含截止边界）', () => {
    expect(windowState('14:00', '18:00', at(18)).phase).toBe('open')
  })

  it('18:00:01 → after', () => {
    expect(windowState('14:00', '18:00', at(18, 0, 1)).phase).toBe('after')
  })

  it('15:00 距截止 3 小时', () => {
    expect(windowState('14:00', '18:00', at(15)).remainSeconds).toBe(3 * 3600)
  })

  it('跨日判断按"当天"的窗口', () => {
    expect(windowState('09:00', '10:00', at(9, 30)).phase).toBe('open')
    expect(windowState('09:00', '10:00', at(11)).phase).toBe('after')
  })
})

describe('formatRemain', () => {
  it('59 秒 → 00:59', () => expect(formatRemain(59)).toBe('00:59'))
  it('3665 秒 → 61:05', () => expect(formatRemain(3600 + 65)).toBe('61:05'))
})

describe('formatOrderedAt', () => {
  it('ISO 时间展示为 HH:mm:ss', () => {
    expect(formatOrderedAt('2026-09-07T15:01:09+08:00')).toMatch(/^\d{2}:\d{2}:\d{2}$/)
  })
})
