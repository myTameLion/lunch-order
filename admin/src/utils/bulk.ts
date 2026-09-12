/**
 * 批量创建用户：文本解析纯函数（D-010）。
 * 每行一条：`登录名,姓名[,密码]`（支持中英文逗号；无逗号时支持空白分隔）；
 * 密码缺省使用 defaultPassword；解析失败的行进入 errors，不影响其他行。
 */
import type { BulkUserItem } from '@/api/types'
import { isValidDisplayName, isValidLoginName, isValidPassword } from '@/utils/validate'

export interface BulkParseResult {
  users: BulkUserItem[]
  errors: string[]
}

export function parseBulkUsers(text: string, defaultPassword: string): BulkParseResult {
  const users: BulkUserItem[] = []
  const errors: string[] = []
  const seen = new Set<string>()

  text.split(/\r?\n/).forEach((rawLine, idx) => {
    const line = rawLine.trim()
    if (!line) return
    const lineNo = idx + 1

    // 逗号分隔保留空字段（用于判定"缺少登录名/姓名"）；无逗号时按空白分隔
    let parts = line.split(/[,，]/).map((s) => s.trim())
    if (parts.length < 2) parts = line.split(/\s+/).map((s) => s.trim()).filter((s) => s.length > 0)
    const [loginName = '', displayName = '', password] = parts

    if (!loginName) {
      errors.push(`第 ${lineNo} 行：缺少登录名`)
      return
    }
    if (seen.has(loginName)) {
      errors.push(`第 ${lineNo} 行：登录名 ${loginName} 批内重复`)
      return
    }
    if (!isValidLoginName(loginName)) {
      errors.push(`第 ${lineNo} 行：登录名 ${loginName} 需为 3~20 位字母/数字/下划线`)
      return
    }
    if (!displayName) {
      errors.push(`第 ${lineNo} 行：缺少姓名`)
      return
    }
    if (!isValidDisplayName(displayName)) {
      errors.push(`第 ${lineNo} 行：姓名需为 1~20 个非空白字符`)
      return
    }
    const pwd = password || defaultPassword
    if (!isValidPassword(pwd)) {
      errors.push(`第 ${lineNo} 行：密码需为 6~64 位`)
      return
    }

    seen.add(loginName)
    users.push({ loginName, displayName, password: password ? pwd : undefined })
  })

  return { users, errors }
}
