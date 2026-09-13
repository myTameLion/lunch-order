import { describe, expect, it } from 'vitest'
import { isNearBottom } from '../scroll'

// 视口 300px，内容 1000px：距底 = 1000 - scrollTop - 300
describe('isNearBottom 聊天底部跟随策略', () => {
  it('在底部 → 跟随', () => {
    expect(isNearBottom(700, 1000, 300)).toBe(true)
  })

  it('接近底部（距底 ≤ 40px）→ 仍视为跟随（灵活容忍）', () => {
    expect(isNearBottom(661, 1000, 300)).toBe(true)
    expect(isNearBottom(660, 1000, 300)).toBe(true)
  })

  it('向上翻看历史 → 不跟随', () => {
    expect(isNearBottom(400, 1000, 300)).toBe(false)
    expect(isNearBottom(0, 1000, 300)).toBe(false)
  })

  it('空内容/零尺寸 → 跟随（默认滚到底）', () => {
    expect(isNearBottom(0, 0, 0)).toBe(true)
  })

  it('自定义阈值', () => {
    expect(isNearBottom(600, 1000, 300, 100)).toBe(true)
    expect(isNearBottom(590, 1000, 300, 100)).toBe(false)
  })
})
