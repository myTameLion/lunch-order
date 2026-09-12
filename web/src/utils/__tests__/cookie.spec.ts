import { beforeEach, describe, expect, it } from 'vitest'
import { clearToken, getToken, setToken } from '../cookie'

beforeEach(() => clearToken())

describe('cookie 工具（单点登录 token 存储，D-002）', () => {
  it('set 后可 get', () => {
    setToken('abc.def-123')
    expect(getToken()).toBe('abc.def-123')
  })

  it('含特殊字符的 token 编解码一致', () => {
    const token = 'eyJhbGciOiJIUzI1NiJ9.a+b/c=d=='
    setToken(token)
    expect(getToken()).toBe(token)
  })

  it('clear 后为 null', () => {
    setToken('x')
    clearToken()
    expect(getToken()).toBeNull()
  })

  it('从未设置时返回 null', () => {
    expect(getToken()).toBeNull()
  })
})
