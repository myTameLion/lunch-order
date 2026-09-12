import { http } from './http'
import type { EvaluationView, MyOrdersResponse, TodayAllResponse, TodayStatus, UserView } from './types'

export const api = {
  login(loginName: string, password: string) {
    return http.post<LoginResult>('/api/auth/login', { loginName, password }).then((r) => r.data)
  },
  getToday() {
    return http.get<TodayStatus>('/api/orders/today').then((r) => r.data)
  },
  /** 当天全员点餐情况（D-008：所有登录用户可查，30 秒轮询即实时） */
  getTodayAll() {
    return http.get<TodayAllResponse>('/api/orders/today/all').then((r) => r.data)
  },
  putOrder(spicy: boolean) {
    return http.put('/api/orders/today', { spicy }).then((r) => r.data)
  },
  cancelOrder() {
    return http.delete<void>('/api/orders/today').then((r) => r.data)
  },
  /** 我的当天评价（D-012） */
  getMyEvaluation() {
    return http.get<{ evaluation: EvaluationView | null }>('/api/orders/today/evaluation').then((r) => r.data.evaluation)
  },
  putEvaluation(rating: number, comment: string) {
    return http.put<EvaluationView>('/api/orders/today/evaluation', { rating, comment }).then((r) => r.data)
  },
  /** 我的历史点餐（D-011） */
  getMyOrders(from: string, to: string) {
    return http.get<MyOrdersResponse>('/api/me/orders', { params: { from, to } }).then((r) => r.data)
  },
  me() {
    return http.get<UserView>('/api/me').then((r) => r.data)
  },
  updateProfile(displayName: string) {
    return http.put<UserView>('/api/me/profile', { displayName }).then((r) => r.data)
  },
  changePassword(oldPassword: string, newPassword: string) {
    return http.put<{ token: string }>('/api/me/password', { oldPassword, newPassword }).then((r) => r.data)
  },
}
