/**
 * 点餐窗口状态纯函数（契约 D-003：[start, end] 闭区间）。
 * 以"校准后的当前时刻毫秒"输入，输出窗口阶段与剩余秒数，便于单测。
 */
export type WindowPhase = 'before' | 'open' | 'after'

export interface WindowState {
  phase: WindowPhase
  remainSeconds: number
}

function atTimeOfDay(nowMs: number, hhmm: string): number {
  const [h, m] = hhmm.split(':').map(Number)
  const d = new Date(nowMs)
  return new Date(d.getFullYear(), d.getMonth(), d.getDate(), h, m, 0, 0).getTime()
}

export function windowState(start: string, end: string, nowMs: number): WindowState {
  const startMs = atTimeOfDay(nowMs, start)
  const endMs = atTimeOfDay(nowMs, end)
  if (nowMs < startMs) return { phase: 'before', remainSeconds: Math.floor((startMs - nowMs) / 1000) }
  if (nowMs > endMs) return { phase: 'after', remainSeconds: 0 }
  return { phase: 'open', remainSeconds: Math.floor((endMs - nowMs) / 1000) }
}

/** 剩余秒数 → mm:ss */
export function formatRemain(seconds: number): string {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

/** ISO 时间串 → 当地 "HH:mm:ss" 展示 */
export function formatOrderedAt(iso: string): string {
  const d = new Date(iso)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}
