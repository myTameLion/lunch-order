/**
 * token 会话存储（契约 D-016 变更）：
 * 使用 sessionStorage——**按浏览器标签页隔离**，web 与 admin 各自独立登录、互不顶线
 * （此前共享 cookie 会导致一个端登录把另一端踢下线）。
 */
const KEY = 'lunch_token'

export function setToken(token: string): void {
  sessionStorage.setItem(KEY, token)
}

export function getToken(): string | null {
  return sessionStorage.getItem(KEY)
}

export function clearToken(): void {
  sessionStorage.removeItem(KEY)
}
