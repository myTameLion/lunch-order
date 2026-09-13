/**
 * token 会话存储（契约 D-016 变更）：
 * 使用 sessionStorage——**按浏览器标签页隔离**，web 与 admin 各自独立登录、互不顶线。
 */
export const TOKEN_KEY = 'lunch_token'

export function setToken(token: string): void {
  sessionStorage.setItem(TOKEN_KEY, token)
}

export function getToken(): string | null {
  return sessionStorage.getItem(TOKEN_KEY)
}

export function clearToken(): void {
  sessionStorage.removeItem(TOKEN_KEY)
}
