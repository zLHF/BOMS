import http from './request'
import type { LoginResult, MeResult } from '@/types'

export interface LoginParams {
  tenantCode?: string
  account: string
  password: string
}

// 响应拦截器已解包 R<T>，这里返回的即 data。
export function apiLogin(params: LoginParams): Promise<LoginResult> {
  return http.post('/auth/login', params) as unknown as Promise<LoginResult>
}

export function apiMe(): Promise<MeResult> {
  return http.get('/auth/me') as unknown as Promise<MeResult>
}

export function apiChangePassword(oldPassword: string, newPassword: string): Promise<void> {
  return http.post('/auth/change-password', { oldPassword, newPassword }) as unknown as Promise<void>
}

export function apiLogout(): Promise<void> {
  return http.post('/auth/logout') as unknown as Promise<void>
}

/** 算力平台 SSO 登录（用 code 换 BOMS token）。 */
export function apiSso(code: string): Promise<LoginResult> {
  return http.get('/cpn/sso', { params: { code } }) as unknown as Promise<LoginResult>
}
