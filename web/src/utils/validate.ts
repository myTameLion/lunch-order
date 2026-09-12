/** 表单校验纯函数（与服务端契约一致的口径） */

export function validateDisplayName(s: string): boolean {
  const t = s.trim()
  return t.length >= 1 && t.length <= 20
}

export function validatePassword(p: string): boolean {
  return p.length >= 6 && p.length <= 64
}

export function validateConfirm(p: string, confirm: string): boolean {
  return p.length > 0 && p === confirm
}
