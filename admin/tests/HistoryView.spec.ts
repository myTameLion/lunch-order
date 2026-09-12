import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import HistoryView from '@/views/HistoryView.vue'

vi.mock('@/api', () => ({
  getSummary: vi.fn().mockResolvedValue({
    from: '2026-09-01',
    to: '2026-09-07',
    totalOrders: 12,
    totalSpicy: 7,
    totalNonSpicy: 5,
    daily: [
      { date: '2026-09-01', count: 5, spicy: 3, nonSpicy: 2 },
      { date: '2026-09-02', count: 0, spicy: 0, nonSpicy: 0 },
    ],
    perUser: [
      { loginName: 'zhangsan', displayName: '张三', days: 5, spicyDays: 3, nonSpicyDays: 2 },
      { loginName: 'lisi', displayName: '李四', days: 2, spicyDays: 1, nonSpicyDays: 1 },
    ],
  }),
  exportSummary: vi.fn(),
}))

vi.mock('@/components/BaseChart.vue', () => ({
  default: { name: 'BaseChart', props: ['option', 'height'], template: '<div class="base-chart-stub" />' },
}))

describe('HistoryView 历史统计', () => {
  it('加载区间统计后渲染统计牌、图表桩与人员汇总表', async () => {
    const wrapper = mount(HistoryView, { global: { plugins: [ElementPlus] } })
    await flushPromises()

    const text = wrapper.text()
    expect(text).toContain('区间点餐总数')
    expect(text).toContain('张三')
    expect(text).toContain('李四')
    expect(text).toContain('共 7 天')

    const rows = wrapper.findAll('.el-table__row')
    expect(rows.length).toBe(2)
    expect(wrapper.findAll('.base-chart-stub').length).toBe(2)
    wrapper.unmount()
  })
})
