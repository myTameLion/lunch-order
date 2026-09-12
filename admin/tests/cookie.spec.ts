import { beforeEach, describe, expect, it } from 'vitest'
import { clearToken, getToken, setToken } from '@/utils/cookie'

beforeEach(() => clearToken())

describe('cookie 工具（与员工端共享 lunch_token，D-002）', () => {
  it('set 后可 get', () => {
    setToken('admin-token')
    expect(getToken()).toBe('admin-token')
  })

  it('特殊字符编码解码一致', () => {
    const token = 'eyJ.abc+1/2=='
    setToken(token)
    expect(getToken()).toBe(token)
  })

  it('clear 后为 null', () => {
    setToken('x')
    clearToken()
    expect(getToken()).toBeNull()
  })

  it('未设置返回 null', () => {
    expect(getToken()).toBeNull()
  })
})
