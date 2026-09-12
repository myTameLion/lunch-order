import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AxiosError, AxiosHeaders } from 'axios'
import { ElMessage } from 'element-plus'
import { clearToken, getToken, setToken } from '../../utils/cookie'
import { http, navigation } from '../http'

vi.mock('element-plus', () => ({
  ElMessage: { error: vi.fn(), warning: vi.fn(), success: vi.fn(), info: vi.fn() },
}))

const originalAdapter = http.defaults.adapter

/** 让 http 实例下一次请求以指定响应失败 */
function failWith(status: number, data: unknown) {
  http.defaults.adapter = async (config) => {
    const headers = new AxiosHeaders()
    throw new AxiosError('request failed', 'ERR_BAD_RESPONSE', config, {}, {
      status,
      data,
      headers,
      config,
    } as never)
  }
}

beforeEach(() => {
  clearToken()
  vi.clearAllMocks()
})

afterEach(() => {
  http.defaults.adapter = originalAdapter
})

describe('http 拦截器', () => {
  it('请求头自动携带 Bearer token', async () => {
    setToken('tok123')
    let seen: AxiosHeaders | undefined
    http.defaults.adapter = async (config) => {
      seen = config.headers as AxiosHeaders
      return { status: 200, statusText: 'OK', data: {}, headers: new AxiosHeaders(), config }
    }
    await http.get('/anything')
    expect(seen?.get('Authorization')).toBe('Bearer tok123')
  })

  it('401 → 清 token 并跳登录页', async () => {
    setToken('will-be-cleared')
    const toLogin = vi.spyOn(navigation, 'toLogin').mockImplementation(() => {})
    failWith(401, { code: 1002, message: '登录状态已失效' })

    await expect(http.get('/api/me')).rejects.toBeTruthy()
    expect(getToken()).toBeNull()
    expect(toLogin).toHaveBeenCalledTimes(1)
    expect(ElMessage.error).not.toHaveBeenCalled()
  })

  it('业务错误 → 弹后端 message', async () => {
    failWith(409, { code: 2001, message: '当前不在点餐时间' })
    await expect(http.put('/api/orders/today', {})).rejects.toBeTruthy()
    expect(ElMessage.error).toHaveBeenCalledWith('当前不在点餐时间')
  })

  it('无响应的网络错误 → 弹网络异常', async () => {
    http.defaults.adapter = async () => {
      throw new Error('connection refused')
    }
    await expect(http.get('/api/me')).rejects.toBeTruthy()
    expect(ElMessage.error).toHaveBeenCalledWith('网络异常，请稍后重试')
  })
})

describe('axios 实例基础能力', () => {
  it('能构造出请求方法（依赖健康）', () => {
    expect(typeof http.get).toBe('function')
    expect(typeof http.post).toBe('function')
    expect(typeof http.put).toBe('function')
    expect(typeof http.delete).toBe('function')
  })
})
