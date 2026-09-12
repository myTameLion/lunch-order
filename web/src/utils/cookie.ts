/**
 * token cookie 工具（契约 D-002）：
 * 与 admin 中台同主机共享 cookie `lunch_token`（path=/，7 天，SameSite=Lax），实现单点登录。
 */
const KEY = 'lunch_token'

export function setToken(token: string, days = 7): void {
  const expires = new Date(Date.now() + days * 86_400_000).toUTCString()
  document.cookie = `${KEY}=${encodeURIComponent(token)}; expires=${expires}; path=/; SameSite=Lax`
}

export function getToken(): string | null {
  const m = document.cookie.match(/(?:^|; )lunch_token=([^;]*)/)
  return m ? decodeURIComponent(m[1]) : null
}

export function clearToken(): void {
  document.cookie = `${KEY}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/; SameSite=Lax`
}
