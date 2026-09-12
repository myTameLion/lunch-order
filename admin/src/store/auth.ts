import { reactive } from 'vue'
import { http } from '@/api/http'
import type { UserView } from '@/api/types'

export interface AuthState {
  loaded: boolean
  loginName: string
  displayName: string
  role: '' | UserView['role']
}

export const auth = reactive<AuthState>({
  loaded: false,
  loginName: '',
  displayName: '',
  role: '',
})

/** 用登录响应直接水合身份（避免额外 /me 请求） */
export function setAuthFromLogin(user: { loginName: string; displayName: string; role: UserView['role'] }): void {
  auth.loginName = user.loginName
  auth.displayName = user.displayName
  auth.role = user.role
  auth.loaded = true
}

/** GET /api/me 水合当前用户身份（路由守卫在有 token 时调用） */
export async function hydrateAuth(): Promise<UserView> {
  const { data } = await http.get<UserView>('/me')
  setAuthFromLogin(data)
  return data
}

export function resetAuth(): void {
  auth.loaded = false
  auth.loginName = ''
  auth.displayName = ''
  auth.role = ''
}

export function isAdmin(): boolean {
  return auth.role === 'ADMIN'
}
