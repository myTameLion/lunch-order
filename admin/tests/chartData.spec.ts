import { describe, expect, it } from 'vitest'
import {
  SLOT_COUNT,
  bucketByTimeSlot,
  buildDailyBarOption,
  buildDonutOption,
  buildTopBarOption,
  buildTrendOption,
  participationRate,
  sortUsersByDays,
  spicyDistribution,
  timeSlotLabels,
  toDailySeries,
  topUsers,
} from '@/utils/chartData'

describe('bucketByTimeSlot（10 分钟/桶，14:00~18:00 共 24 桶，D-003 闭区间）', () => {
  it('空 orders → 24 个 0 值桶', () => {
    const buckets = bucketByTimeSlot([])
    expect(buckets).toHaveLength(SLOT_COUNT)
    expect(SLOT_COUNT).toBe(24)
    expect(buckets.every((v) => v === 0)).toBe(true)
  })

  it('14:00:00 落第 0 桶（含左端点）', () => {
    expect(bucketByTimeSlot([{ orderedAt: '2026-09-07T14:00:00+08:00' }])[0]).toBe(1)
  })

  it('14:09:59 仍在第 0 桶，14:10:00 落第 1 桶', () => {
    const buckets = bucketByTimeSlot([
      { orderedAt: '2026-09-07T14:09:59+08:00' },
      { orderedAt: '2026-09-07T14:10:00+08:00' },
    ])
    expect(buckets[0]).toBe(1)
    expect(buckets[1]).toBe(1)
  })

  it('18:00:00 落最后一桶（含右端点）', () => {
    const buckets = bucketByTimeSlot([{ orderedAt: '2026-09-07T18:00:00+08:00' }])
    expect(buckets[23]).toBe(1)
  })

  it('窗口外时刻不计入（按分钟粒度判定）', () => {
    const buckets = bucketByTimeSlot([
      { orderedAt: '2026-09-07T13:59:00+08:00' },
      { orderedAt: '2026-09-07T18:01:00+08:00' },
    ])
    expect(buckets.every((v) => v === 0)).toBe(true)
  })

  it('按上海时区解析（UTC 写法等价）', () => {
    // 06:00Z = 14:00+08:00
    expect(bucketByTimeSlot([{ orderedAt: '2026-09-07T06:00:00Z' }])[0]).toBe(1)
  })
})

describe('spicyDistribution / toDailySeries', () => {
  it('辣度分布计数', () => {
    expect(spicyDistribution([{ spicy: true }, { spicy: false }, { spicy: true }])).toEqual([
      { name: '要辣', value: 2 },
      { name: '不要辣', value: 1 },
    ])
  })

  it('daily 序列映射', () => {
    const series = toDailySeries([
      { date: '2026-09-01', count: 3, spicy: 2, nonSpicy: 1 },
      { date: '2026-09-02', count: 0, spicy: 0, nonSpicy: 0 },
    ])
    expect(series.dates).toEqual(['2026-09-01', '2026-09-02'])
    expect(series.spicy).toEqual([2, 0])
    expect(series.nonSpicy).toEqual([1, 0])
  })
})

describe('排序 / TOP N / 参与率', () => {
  const users = [
    { loginName: 'c', displayName: '丙', days: 2, spicyDays: 1, nonSpicyDays: 1 },
    { loginName: 'a', displayName: '甲', days: 5, spicyDays: 3, nonSpicyDays: 2 },
    { loginName: 'b', displayName: '乙', days: 5, spicyDays: 1, nonSpicyDays: 4 },
  ]

  it('days 降序、loginName 升序', () => {
    expect(sortUsersByDays(users).map((u) => u.loginName)).toEqual(['a', 'b', 'c'])
  })

  it('topUsers 取前 N；N<=0 返回空', () => {
    expect(topUsers(users, 2)).toHaveLength(2)
    expect(topUsers(users, 0)).toEqual([])
  })

  it('参与率保留 1 位小数，分母 0 保护', () => {
    expect(participationRate(5, 7)).toBe(71.4)
    expect(participationRate(0, 7)).toBe(0)
    expect(participationRate(3, 0)).toBe(0)
    expect(participationRate(7, 7)).toBe(100)
  })
})

describe('timeSlotLabels', () => {
  it('24 个标签，首 14:00 尾 17:50', () => {
    const labels = timeSlotLabels()
    expect(labels).toHaveLength(24)
    expect(labels[0]).toBe('14:00')
    expect(labels[23]).toBe('17:50')
  })
})

describe('ECharts option 构建（烟测）', () => {
  it('环形图空数据带空态文案', () => {
    const empty = buildDonutOption([{ name: '要辣', value: 0 }, { name: '不要辣', value: 0 }])
    expect(empty.title).toBeDefined()
    const nonEmpty = buildDonutOption([{ name: '要辣', value: 2 }, { name: '不要辣', value: 1 }])
    expect(nonEmpty.title).toBeUndefined()
  })

  it('趋势/柱状/TOP 数据长度一致', () => {
    const trend = buildTrendOption(timeSlotLabels(), new Array(SLOT_COUNT).fill(1))
    expect((trend.series as Array<{ data: number[] }>)[0].data).toHaveLength(24)

    const bar = buildDailyBarOption(['2026-09-01'], [1], [2])
    const barSeries = bar.series as Array<{ data: number[] }>
    expect(barSeries[0].data).toEqual([1])
    expect(barSeries[1].data).toEqual([2])

    const top = buildTopBarOption([
      { loginName: 'a', displayName: '甲', days: 5, spicyDays: 3, nonSpicyDays: 2 },
      { loginName: 'b', displayName: '乙', days: 2, spicyDays: 1, nonSpicyDays: 1 },
    ])
    const topSeries = top.series as Array<{ data: number[] }>
    expect(topSeries[0].data).toEqual([2, 5])
  })
})
