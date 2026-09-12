/**
 * 图表数据加工纯函数 + ECharts option 构建纯函数。
 * 全部无副作用、不依赖 DOM，便于 Vitest 单测。
 * 统一配色（Element Plus 色板）：['#409EFF','#67C23A','#E6A23C','#F56C6C','#909399']
 */
import type { EChartsOption } from 'echarts'
import type { DailySummary, OrderView, UserSummary } from '@/api/types'

/** 全站统一配色 */
export const PALETTE = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399'] as const

/** 辣度两色：要辣-红、不要辣-绿 */
export const COLOR_SPICY = PALETTE[3]
export const COLOR_NON_SPICY = PALETTE[1]
export const COLOR_MAIN = PALETTE[0]

/* ------------------------------------------------------------------ */
/* 通用文本样式                                                        */
/* ------------------------------------------------------------------ */

const AXIS_LABEL = { color: '#909399' }
const SPLIT_LINE = { lineStyle: { color: '#EBEEF5' } }
const AXIS_LINE = { lineStyle: { color: '#DCDFE6' } }

/* ------------------------------------------------------------------ */
/* 时区与分时段                                                        */
/* ------------------------------------------------------------------ */

const SHANGHAI_CLOCK_FORMATTER = new Intl.DateTimeFormat('en-GB', {
  timeZone: 'Asia/Shanghai',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
})

/**
 * ISO 8601 时间字符串 → 上海时区的当日分钟数（0~1439）；非法输入返回 -1。
 */
export function shanghaiMinutes(iso: string): number {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return -1
  const parts = SHANGHAI_CLOCK_FORMATTER.formatToParts(date)
  const get = (type: Intl.DateTimeFormatPartTypes) => Number(parts.find((p) => p.type === type)?.value ?? NaN)
  const h = get('hour')
  const m = get('minute')
  if (Number.isNaN(h) || Number.isNaN(m)) return -1
  return h * 60 + m
}

/** 点餐窗口起点（契约 D-003：闭区间 [14:00, 18:00]），单位分钟 */
export const WINDOW_START_MINUTES = 14 * 60
export const WINDOW_END_MINUTES = 18 * 60
/** 分桶粒度：10 分钟/桶 */
export const SLOT_MINUTES = 10
/** 桶数：14:00~18:00 固定 24 桶 */
export const SLOT_COUNT = (WINDOW_END_MINUTES - WINDOW_START_MINUTES) / SLOT_MINUTES

/** 横轴标签：14:00、14:10 …… 17:50 */
export function timeSlotLabels(): string[] {
  return Array.from({ length: SLOT_COUNT }, (_, i) => {
    const total = WINDOW_START_MINUTES + i * SLOT_MINUTES
    const h = String(Math.floor(total / 60)).padStart(2, '0')
    const m = String(total % 60).padStart(2, '0')
    return `${h}:${m}`
  })
}

/**
 * 把点餐记录按登记时间分桶（10 分钟一档，14:00~18:00 共 24 桶）。
 * - 14:00:00 落第 0 桶（窗口闭区间含左端点）；
 * - 18:00:00 落最后一桶（闭区间含右端点）；
 * - 窗口外时刻不计；
 * - 空 orders → 24 个 0 值桶。
 */
export function bucketByTimeSlot(orders: ReadonlyArray<Pick<OrderView, 'orderedAt'>>): number[] {
  const buckets = new Array<number>(SLOT_COUNT).fill(0)
  for (const order of orders) {
    const minutes = shanghaiMinutes(order.orderedAt)
    if (minutes < WINDOW_START_MINUTES || minutes > WINDOW_END_MINUTES) continue
    const index = Math.floor((minutes - WINDOW_START_MINUTES) / SLOT_MINUTES)
    buckets[Math.min(index, SLOT_COUNT - 1)] += 1
  }
  return buckets
}

/* ------------------------------------------------------------------ */
/* 辣度聚合                                                            */
/* ------------------------------------------------------------------ */

export interface PieItem {
  name: string
  value: number
}

/** 辣度分布（环形图数据）：要辣 / 不要辣 */
export function spicyDistribution(orders: ReadonlyArray<Pick<OrderView, 'spicy'>>): PieItem[] {
  let spicy = 0
  let nonSpicy = 0
  for (const order of orders) {
    if (order.spicy) spicy += 1
    else nonSpicy += 1
  }
  return [
    { name: '要辣', value: spicy },
    { name: '不要辣', value: nonSpicy },
  ]
}

/* ------------------------------------------------------------------ */
/* 历史统计聚合                                                        */
/* ------------------------------------------------------------------ */

export interface DailySeries {
  dates: string[]
  spicy: number[]
  nonSpicy: number[]
}

/** 每日堆叠柱状图数据（后端 daily 已按日期升序且含 0 值天，此处仅做映射） */
export function toDailySeries(daily: ReadonlyArray<DailySummary>): DailySeries {
  return {
    dates: daily.map((d) => d.date),
    spicy: daily.map((d) => d.spicy),
    nonSpicy: daily.map((d) => d.nonSpicy),
  }
}

/** 按 days 降序、loginName 升序排序（契约 RangeSummary.perUser 口径），返回新数组 */
export function sortUsersByDays(users: ReadonlyArray<UserSummary>): UserSummary[] {
  return [...users].sort((a, b) => b.days - a.days || a.loginName.localeCompare(b.loginName))
}

/** 点餐天数 TOP N（先排序后截取） */
export function topUsers(users: ReadonlyArray<UserSummary>, n: number): UserSummary[] {
  if (n <= 0) return []
  return sortUsersByDays(users).slice(0, n)
}

/**
 * 参与率（glossary.md）：点餐天数 ÷ 区间自然日数（含首尾），百分制保留 1 位小数；分母为 0 时取 0。
 */
export function participationRate(days: number, totalDays: number): number {
  if (!Number.isFinite(days) || !Number.isFinite(totalDays) || totalDays <= 0) return 0
  return Math.round((days / totalDays) * 1000) / 10
}

/* ------------------------------------------------------------------ */
/* ECharts option 构建                                                 */
/* ------------------------------------------------------------------ */

/** 环形图：辣度分布；全部为 0 时在环心显示空态文案 */
export function buildDonutOption(dist: ReadonlyArray<PieItem>, emptyText = '今日暂无点餐'): EChartsOption {
  const total = dist.reduce((sum, item) => sum + item.value, 0)
  return {
    color: [COLOR_SPICY, COLOR_NON_SPICY],
    tooltip: { trigger: 'item', formatter: '{b}：{c} 份（{d}%）' },
    legend: { bottom: 0, icon: 'circle', itemWidth: 8, itemHeight: 8, textStyle: { color: '#606266' } },
    title:
      total > 0
        ? undefined
        : {
            text: emptyText,
            left: 'center',
            top: '42%',
            textStyle: { color: '#909399', fontSize: 14, fontWeight: 'normal' },
          },
    series: [
      {
        name: '辣度分布',
        type: 'pie',
        radius: ['48%', '72%'],
        center: ['50%', '46%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: total > 0, formatter: '{b}\n{c} 份', color: '#606266' },
        labelLine: { length: 12, length2: 8 },
        data: [...dist],
      },
    ],
  }
}

/** 折线图：分时段登记趋势 */
export function buildTrendOption(labels: ReadonlyArray<string>, values: ReadonlyArray<number>): EChartsOption {
  return {
    color: [COLOR_MAIN],
    tooltip: { trigger: 'axis', axisPointer: { type: 'line' }, valueFormatter: (value) => `${value} 人` },
    grid: { left: 8, right: 24, top: 40, bottom: 0, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: [...labels],
      axisLabel: { ...AXIS_LABEL, interval: 1 },
      axisLine: AXIS_LINE,
      axisTick: { show: false },
    },
    yAxis: { type: 'value', minInterval: 1, axisLabel: AXIS_LABEL, splitLine: SPLIT_LINE },
    series: [
      {
        name: '登记人数',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        showSymbol: false,
        data: [...values],
        itemStyle: { color: COLOR_MAIN },
        lineStyle: { width: 2 },
        areaStyle: { color: 'rgba(64, 158, 255, 0.15)' },
      },
    ],
  }
}

/** 柱状图：每日点餐人数（要辣/不要辣堆叠） */
export function buildDailyBarOption(dates: ReadonlyArray<string>, spicy: ReadonlyArray<number>, nonSpicy: ReadonlyArray<number>): EChartsOption {
  return {
    color: [COLOR_SPICY, COLOR_NON_SPICY],
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, valueFormatter: (value) => `${value} 人` },
    legend: { top: 0, right: 0, icon: 'roundRect', itemWidth: 12, itemHeight: 6, textStyle: { color: '#606266' } },
    grid: { left: 8, right: 16, top: 40, bottom: 0, containLabel: true },
    xAxis: {
      type: 'category',
      data: [...dates],
      axisLabel: { ...AXIS_LABEL, formatter: (value: string) => value.slice(5) },
      axisLine: AXIS_LINE,
      axisTick: { show: false },
    },
    yAxis: { type: 'value', minInterval: 1, axisLabel: AXIS_LABEL, splitLine: SPLIT_LINE },
    series: [
      { name: '要辣', type: 'bar', stack: 'total', barMaxWidth: 28, data: [...spicy] },
      { name: '不要辣', type: 'bar', stack: 'total', barMaxWidth: 28, data: [...nonSpicy], itemStyle: { borderRadius: [4, 4, 0, 0] } },
    ],
  }
}

/** 横向条形图：点餐天数 TOP N（第一名在最上方） */
export function buildTopBarOption(users: ReadonlyArray<UserSummary>, n = 10): EChartsOption {
  const top = topUsers(users, n).slice().reverse()
  return {
    color: [COLOR_MAIN],
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, valueFormatter: (value) => `${value} 天` },
    grid: { left: 8, right: 40, top: 16, bottom: 0, containLabel: true },
    xAxis: { type: 'value', minInterval: 1, axisLabel: AXIS_LABEL, splitLine: SPLIT_LINE },
    yAxis: {
      type: 'category',
      data: top.map((u) => u.displayName),
      axisLabel: { color: '#606266' },
      axisTick: { show: false },
      axisLine: { show: false },
    },
    series: [
      {
        name: '点餐天数',
        type: 'bar',
        barMaxWidth: 18,
        data: top.map((u) => u.days),
        itemStyle: { borderRadius: [0, 9, 9, 0] },
        label: { show: true, position: 'right', color: '#606266' },
      },
    ],
  }
}
