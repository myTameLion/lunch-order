/**
 * 表单校验纯函数（口径与 shared/contracts/api.yaml 的 CreateUserRequest 及 glossary.md 一致）。
 */

/** 登录名：^[a-zA-Z0-9_]{3,20}$ */
export const LOGIN_NAME_RE = /^[a-zA-Z0-9_]{3,20}$/

/** 姓名：1~20 个非空白字符 */
export const DISPLAY_NAME_RE = /^\S{1,20}$/

/** 密码：6~64 位（api.yaml minLength 6 / maxLength 64） */
export function isValidPassword(password: string): boolean {
  return typeof password === 'string' && password.length >= 6 && password.length <= 64
}

export function isValidLoginName(loginName: string): boolean {
  return LOGIN_NAME_RE.test(loginName)
}

export function isValidDisplayName(displayName: string): boolean {
  return DISPLAY_NAME_RE.test(displayName)
}

export function isValidRole(role: unknown): boolean {
  return role === 'USER' || role === 'ADMIN'
}
