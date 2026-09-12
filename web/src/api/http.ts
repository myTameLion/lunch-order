import axios from 'axios'
import { ElMessage } from 'element-plus'
import { clearToken, getToken } from '../utils/cookie'

export const http = axios.create({ baseURL: '', timeout: 15_000 })

http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

/** 401 → 清 cookie 回登录页（带回跳地址）；navigation 抽出来便于测试注入 */
export const navigation = {
  toLogin(): void {
    const redirect = encodeURIComponent(location.pathname + location.search)
    window.location.assign(`/login?redirect=${redirect}`)
  },
}

http.interceptors.response.use(
  (resp) => resp,
  (error) => {
    const status = error?.response?.status
    if (status === 401) {
      clearToken()
      navigation.toLogin()
    } else {
      const msg: string = error?.response?.data?.message ?? '网络异常，请稍后重试'
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  },
)
