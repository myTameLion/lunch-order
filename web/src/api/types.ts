/** 与 shared/contracts/api.yaml 一致的响应类型 */

export interface WindowInfo {
  start: string
  end: string
  open: boolean
  serverTime: string
}

export interface MyOrder {
  spicy: boolean
  orderedAt: string
}

export interface TodayStatus {
  date: string
  window: WindowInfo
  myOrder: MyOrder | null
}

export interface LoginResult {
  token: string
  loginName: string
  displayName: string
  role: string
}

export interface UserView {
  loginName: string
  displayName: string
  role: string
  createdAt: string
}

export interface ApiError {
  code: number
  message: string
}

/** 当天全部点餐记录项（契约 D-008：所有登录用户可查） */
export interface TodayOrderItem {
  loginName: string
  displayName: string
  spicy: boolean
  orderedAt: string
}

export interface TodayAllResponse {
  date: string
  total: number
  spicy: number
  nonSpicy: number
  orders: TodayOrderItem[]
}

/** 公共聊天频道消息（D-013；客户端仅当天） */
export interface ChatMessage {
  loginName: string
  displayName: string
  content: string
  sentAt: string
}

export interface ChatResponse {
  date: string
  count: number
  messages: ChatMessage[]
}

/** 我的历史点餐（D-011） */
export interface MyOrdersResponse {
  from: string
  to: string
  records: { date: string; spicy: boolean; orderedAt: string }[]
  totalDays: number
  spicyDays: number
  nonSpicyDays: number
}
