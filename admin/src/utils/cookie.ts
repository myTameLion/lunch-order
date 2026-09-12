/**
 * token Cookie 工具（契约 CHANGELOG D-002）。
 * - cookie 名固定 `lunch_token`，path=/，有效期 7 天，SameSite=Lax；
 * - 与员工 web 端同主机共享，实现"员工已登录 → 打开中台即为已登录"的单点登录。
 * 手写实现，不引入额外依赖。
 */

export const TOKEN_KEY = 'lunch_token'

/** token 有效期（天），契约约定 7 天 */
export const TOKEN_MAX_AGE_DAYS = 7

export function setToken(token: string, days: number = TOKEN_MAX_AGE_DAYS): void {
  const maxAge = Math.max(1, Math.floor(days)) * 24 * 60 * 60
  document.cookie = `${TOKEN_KEY}=${encodeURIComponent(token)}; path=/; max-age=${maxAge}; SameSite=Lax`
}

export function getToken(): string | null {
  const prefix = `${TOKEN_KEY}=`
  const found = document.cookie
    .split(';')
    .map((item) => item.trim())
    .find((item) => item.startsWith(prefix))
  if (!found) return null
  const raw = found.slice(prefix.length)
  if (!raw) return null
  try {
    return decodeURIComponent(raw)
  } catch {
    return raw
  }
}

export function clearToken(): void {
  document.cookie = `${TOKEN_KEY}=; path=/; max-age=0; SameSite=Lax`
}
