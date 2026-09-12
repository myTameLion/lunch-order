import axios, { AxiosError, type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { clearToken, getToken } from '@/utils/cookie'
import type { ErrorResponse } from './types'

/** 登录页路由路径（路由 base 为 /admin/，实际浏览器地址为 /admin/login） */
export const LOGIN_PATH = '/login'

function isErrorResponse(body: unknown): body is ErrorResponse {
  return typeof body === 'object' && body !== null && 'message' in body && typeof (body as ErrorResponse).message === 'string'
}

/** 请求拦截器：附加 Authorization: Bearer <token> */
export function requestInterceptor(config: InternalAxiosRequestConfig): InternalAxiosRequestConfig {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
}

/**
 * 响应错误拦截器：
 * - HTTP 401 → 清 cookie、跳 /admin/login（当前已在登录页则只提示）；
 * - 业务错误 {code,message} → ElMessage.error(message)；
 * - 网络异常 → 统一中文提示。
 */
export async function responseErrorInterceptor(error: unknown): Promise<never> {
  const err = error as AxiosError<ErrorResponse>
  const status = err?.response?.status
  const body = err?.response?.data

  if (status === 401) {
    if (isErrorResponse(body) && body.message) {
      ElMessage.error(body.message)
    }
    clearToken()
    if (router.currentRoute.value.path !== LOGIN_PATH) {
      router.push(LOGIN_PATH)
    }
    throw err
  }

  if (isErrorResponse(body) && body.message) {
    ElMessage.error(body.message)
  } else if (!err?.response) {
    ElMessage.error('网络异常，请稍后重试')
  }
  throw err
}

export function createHttp(): AxiosInstance {
  const instance = axios.create({
    baseURL: '/api',
    timeout: 15_000,
  })
  instance.interceptors.request.use(requestInterceptor)
  instance.interceptors.response.use((response) => response, responseErrorInterceptor)
  return instance
}

export const http = createHttp()
