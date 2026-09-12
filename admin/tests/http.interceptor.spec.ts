import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ElMessage } from 'element-plus'
import { requestInterceptor, responseErrorInterceptor } from '@/api/http'
import { clearToken, getToken, setToken } from '@/utils/cookie'

const mocks = vi.hoisted(() => ({
  push: vi.fn(),
  routePath: { value: { path: '/history' } },
}))

vi.mock('@/router', () => ({
  default: {
    currentRoute: mocks.routePath,
    push: mocks.push,
  },
}))

vi.mock('element-plus', () => ({
  ElMessage: { error: vi.fn(), success: vi.fn(), warning: vi.fn(), info: vi.fn() },
}))

beforeEach(() => {
  clearToken()
  vi.clearAllMocks()
  mocks.routePath.value = { path: '/history' }
})

describe('requestInterceptor', () => {
  it('有 token 时附加 Bearer', () => {
    setToken('tok')
    const headers = new Headers() as unknown as Record<string, string>
    const config = { headers } as never
    requestInterceptor(config as never)
    expect(headers.Authorization).toBe('Bearer tok')
  })

  it('无 token 不附加', () => {
    const headers = new Headers() as unknown as Record<string, string>
    requestInterceptor({ headers } as never)
    expect(headers.Authorization).toBeUndefined()
  })
})

describe('responseErrorInterceptor', () => {
  it('401 → 清 token、跳登录页、提示后端 message', async () => {
    setToken('expired')
    const err = { response: { status: 401, data: { code: 1002, message: '登录状态已失效' } } }
    await expect(responseErrorInterceptor(err)).rejects.toBe(err)
    expect(getToken()).toBeNull()
    expect(mocks.push).toHaveBeenCalledWith('/login')
    expect(ElMessage.error).toHaveBeenCalledWith('登录状态已失效')
  })

  it('已在登录页时不重复跳转', async () => {
    mocks.routePath.value = { path: '/login' }
    const err = { response: { status: 401, data: { code: 1002, message: '失效' } } }
    await expect(responseErrorInterceptor(err)).rejects.toBe(err)
    expect(mocks.push).not.toHaveBeenCalled()
  })

  it('非 401 业务错误 → 仅弹 message', async () => {
    const err = { response: { status: 409, data: { code: 2001, message: '非点餐时间' } } }
    await expect(responseErrorInterceptor(err)).rejects.toBe(err)
    expect(ElMessage.error).toHaveBeenCalledWith('非点餐时间')
    expect(mocks.push).not.toHaveBeenCalled()
  })

  it('网络异常 → 统一提示', async () => {
    const err = { response: undefined }
    await expect(responseErrorInterceptor(err)).rejects.toBe(err)
    expect(ElMessage.error).toHaveBeenCalledWith('网络异常，请稍后重试')
  })
})
