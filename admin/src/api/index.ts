import type { AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { http } from './http'
import type {
  BulkCreateResponse,
  BulkUserItem,
  CreateUserRequest,
  EvaluationsResponse,
  LoginRequest,
  LoginResponse,
  RangeSummary,
  TodayAllResponse,
  UserView,
  WindowSettings,
} from './types'

/** POST /api/auth/login 登录 */
export async function login(payload: LoginRequest): Promise<LoginResponse> {
  const { data } = await http.post<LoginResponse>('/auth/login', payload)
  return data
}

/** GET /api/orders/today/all 当天全部点餐名单与统计（ADMIN） */
export async function getTodayAll(): Promise<TodayAllResponse> {
  const { data } = await http.get<TodayAllResponse>('/orders/today/all')
  return data
}

/** GET /api/admin/summary 区间统计（ADMIN） */
export async function getSummary(from: string, to: string): Promise<RangeSummary> {
  const { data } = await http.get<RangeSummary>('/admin/summary', { params: { from, to } })
  return data
}

/**
 * GET /api/admin/export 导出 Excel（blob）。
 * 错误响应为 JSON Blob 时，解析出后端 message 并提示。
 */
export async function exportSummary(from: string, to: string): Promise<AxiosResponse<Blob>> {
  try {
    return await http.get<Blob>('/admin/export', { params: { from, to }, responseType: 'blob' })
  } catch (error) {
    const blob = (error as { response?: { data?: unknown } })?.response?.data
    if (typeof Blob !== 'undefined' && blob instanceof Blob) {
      try {
        const body = JSON.parse(await blob.text()) as { code?: number; message?: string }
        if (body?.message) ElMessage.error(body.message)
      } catch {
        // 非 JSON 响应，忽略
      }
    }
    throw error
  }
}

/** GET /api/admin/users 用户列表（ADMIN，不含密码字段） */
export async function getUsers(): Promise<UserView[]> {
  const { data } = await http.get<{ users: UserView[] }>('/admin/users')
  return data.users
}

/** POST /api/admin/users 创建用户（ADMIN；409/1005 登录名已存在由拦截器展示后端 message） */
export async function createUser(payload: CreateUserRequest): Promise<UserView> {
  const { data } = await http.post<UserView>('/admin/users', payload)
  return data
}

/** GET /api/admin/settings/window 查看点餐窗口（D-009） */
export async function getWindow(): Promise<WindowSettings> {
  const { data } = await http.get<WindowSettings>('/admin/settings/window')
  return data
}

/** PUT /api/admin/settings/window 设置点餐窗口（D-009） */
export async function updateWindow(start: string, end: string): Promise<WindowSettings> {
  const { data } = await http.put<WindowSettings>('/admin/settings/window', { start, end })
  return data
}

/** POST /api/admin/users/bulk 批量创建用户（D-010） */
export async function bulkCreateUsers(users: BulkUserItem[]): Promise<BulkCreateResponse> {
  const { data } = await http.post<BulkCreateResponse>('/admin/users/bulk', { users })
  return data
}

/** GET /api/admin/users/export 导出全部账户与密码 xlsx（D-010；错误响应为 JSON Blob 时提示 message） */
export async function exportUsers(): Promise<AxiosResponse<Blob>> {
  try {
    return await http.get<Blob>('/admin/users/export', { responseType: 'blob' })
  } catch (error) {
    const blob = (error as { response?: { data?: unknown } })?.response?.data
    if (typeof Blob !== 'undefined' && blob instanceof Blob) {
      try {
        const body = JSON.parse(await blob.text()) as { code?: number; message?: string }
        if (body?.message) ElMessage.error(body.message)
      } catch {
        // 非 JSON 响应，忽略
      }
    }
    throw error
  }
}

/** GET /api/admin/evaluations?date= 按天查看全员评价（D-012） */
export async function getEvaluations(date: string): Promise<EvaluationsResponse> {
  const { data } = await http.get<EvaluationsResponse>('/admin/evaluations', { params: { date } })
  return data
}
