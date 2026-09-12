import { reactive } from 'vue'
import { api } from '../api'
import { clearToken } from '../utils/cookie'

/** 极简用户状态（供顶部栏与路由守卫使用） */
export const userStore = reactive({
  loginName: '',
  displayName: '',
  role: '',
  loaded: false,
})

export async function loadUser(): Promise<void> {
  try {
    const u = await api.me()
    userStore.loginName = u.loginName
    userStore.displayName = u.displayName
    userStore.role = u.role
    userStore.loaded = true
  } catch {
    /* 401 已由拦截器处理 */
  }
}

export function logout(): void {
  clearToken()
  userStore.loginName = ''
  userStore.displayName = ''
  userStore.role = ''
  userStore.loaded = false
  window.location.assign('/login')
}
