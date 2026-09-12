import { describe, expect, it } from 'vitest'
import { parseBulkUsers } from '@/utils/bulk'

describe('parseBulkUsers 批量创建文本解析（D-010）', () => {
  it('标准逗号格式，密码缺省', () => {
    const r = parseBulkUsers('student01,张三\nstudent02,李四', '123456')
    expect(r.errors).toHaveLength(0)
    expect(r.users).toEqual([
      { loginName: 'student01', displayName: '张三', password: undefined },
      { loginName: 'student02', displayName: '李四', password: undefined },
    ])
  })

  it('支持中文逗号与自定义密码', () => {
    const r = parseBulkUsers('a01，张三，abc12345', '123456')
    expect(r.errors).toHaveLength(0)
    expect(r.users[0]).toEqual({ loginName: 'a01', displayName: '张三', password: 'abc12345' })
  })

  it('无逗号时支持空白分隔', () => {
    const r = parseBulkUsers('a02 王五', '123456')
    expect(r.users[0]?.loginName).toBe('a02')
    expect(r.users[0]?.displayName).toBe('王五')
  })

  it('空行自动跳过', () => {
    const r = parseBulkUsers('\na03,赵六\n\n', '123456')
    expect(r.users).toHaveLength(1)
    expect(r.errors).toHaveLength(0)
  })

  it('批内重复 → 报错且不影响其他行', () => {
    const r = parseBulkUsers('a04,甲\na04,乙\na05,丙', '123456')
    expect(r.users.map((u) => u.loginName)).toEqual(['a04', 'a05'])
    expect(r.errors).toHaveLength(1)
    expect(r.errors[0]).toContain('批内重复')
  })

  it('非法登录名 / 缺姓名 / 坏密码 → 逐行报错', () => {
    const r = parseBulkUsers('bad name!,甲\na06,\na07,乙,123', '123456')
    expect(r.users).toHaveLength(0)
    expect(r.errors).toHaveLength(3)
    expect(r.errors[0]).toContain('3~20 位')
    expect(r.errors[1]).toContain('缺少姓名')
    expect(r.errors[2]).toContain('6~64 位')
  })

  it('登录名空白行优先报缺登录名', () => {
    const r = parseBulkUsers(',无名', '123456')
    expect(r.errors[0]).toContain('缺少登录名')
  })
})
