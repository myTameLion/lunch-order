/**
 * 与 shared/contracts/api.yaml（契约版本 1.0.0）一一对应的 TS 类型。
 * 字段名不得私改（见 glossary.md 命名约定）。
 */

export type Role = 'USER' | 'ADMIN'

export interface LoginRequest {
  loginName: string
  password: string
}

export interface LoginResponse {
  token: string
  loginName: string
  displayName: string
  role: Role
}

export interface UserView {
  loginName: string
  displayName: string
  role: Role
  createdAt: string
}

export interface WindowInfo {
  /** HH:mm，如 14:00 */
  start: string
  end: string
  open: boolean
  serverTime: string
}

export interface OrderView {
  loginName: string
  displayName: string
  spicy: boolean
  /** ISO 8601 带时区，如 2026-09-05T14:23:05+08:00 */
  orderedAt: string
  /** 是否为修改既有记录（false=首次登记） */
  updated: boolean
}

export interface TodayAllResponse {
  date: string
  window: WindowInfo
  /** 去重 loginName 数 */
  total: number
  spicy: number
  nonSpicy: number
  orders: OrderView[]
}

export interface DailySummary {
  date: string
  /** 当日点餐人数 */
  count: number
  spicy: number
  nonSpicy: number
}

export interface UserSummary {
  loginName: string
  displayName: string
  /** 区间内点餐天数 */
  days: number
  spicyDays: number
  nonSpicyDays: number
}

export interface RangeSummary {
  from: string
  to: string
  totalOrders: number
  totalSpicy: number
  totalNonSpicy: number
  /** 区间内每一天一条（含无点餐的 0 值天），按日期升序 */
  daily: DailySummary[]
  /** 全部用户（含 0 单用户），按 days 降序、loginName 升序 */
  perUser: UserSummary[]
}

export interface CreateUserRequest {
  loginName: string
  displayName: string
  password: string
  role: Role
}

export interface ErrorResponse {
  code: number
  message: string
}

// ── v1.2.0 ──

export interface WindowSettings {
  orderWindowStart: string
  orderWindowEnd: string
}

export interface BulkUserItem {
  loginName: string
  displayName: string
  /** 缺省 123456 */
  password?: string
}

export interface BulkCreateResponse {
  created: number
  failed: { loginName: string; reason: string }[]
}

export interface EvaluationRecord {
  loginName: string
  displayName: string
  rating: number
  comment: string
  ratedAt: string
}

export interface EvaluationsResponse {
  date: string
  count: number
  avgRating: number
  evaluations: EvaluationRecord[]
}
