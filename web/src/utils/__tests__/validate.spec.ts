import { describe, expect, it } from 'vitest'
import { validateConfirm, validateDisplayName, validatePassword } from '../validate'

describe('表单校验纯函数', () => {
  it('姓名 1~20 字符', () => {
    expect(validateDisplayName('张三')).toBe(true)
    expect(validateDisplayName(' 张三 ')).toBe(true)
    expect(validateDisplayName('')).toBe(false)
    expect(validateDisplayName('   ')).toBe(false)
    expect(validateDisplayName('一二三四五六七八九十一二三四五六七八九十')).toBe(true)
    expect(validateDisplayName('一二三四五六七八九十一二三四五六七八九十1')).toBe(false)
  })

  it('密码 6~64 位', () => {
    expect(validatePassword('123456')).toBe(true)
    expect(validatePassword('12345')).toBe(false)
    expect(validatePassword('a'.repeat(64))).toBe(true)
    expect(validatePassword('a'.repeat(65))).toBe(false)
  })

  it('确认密码一致', () => {
    expect(validateConfirm('abcdef', 'abcdef')).toBe(true)
    expect(validateConfirm('abcdef', 'abcdeg')).toBe(false)
    expect(validateConfirm('', '')).toBe(false)
  })
})
