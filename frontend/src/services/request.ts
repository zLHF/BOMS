import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { R } from '@/types'

const TOKEN_KEY = 'boms_token'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}
export function setToken(t: string): void {
  localStorage.setItem(TOKEN_KEY, t)
}
export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

const http: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

// 请求拦截：注入 Authorization。租户身份由后端从 token 解析，前端不可信传 tenant_id。
http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getToken()
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }
  return config
})

// 响应拦截：解包 R<T>，非 0 抛错；401 清 token 跳登录。
http.interceptors.response.use(
  (resp) => {
    const body = resp.data as R<unknown>
    if (body && typeof body.code === 'number') {
      if (body.code === 0) {
        return body.data
      }
      const err = new Error(body.message) as Error & { code?: number }
      err.code = body.code
      // 40910=疑似重复客户软提示，交由页面处理，不弹默认错误
      if (body.code !== 40910) ElMessage.error(body.message || '请求失败')
      return Promise.reject(err)
    }
    return resp.data
  },
  (error) => {
    const status = error?.response?.status
    if (status === 401) {
      clearToken()
      if (location.pathname !== '/login') {
        location.href = '/login'
      }
    }
    ElMessage.error(error?.response?.data?.message || error.message || '网络异常')
    return Promise.reject(error)
  },
)

export default http
