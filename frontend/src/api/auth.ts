import http from '@/api/http'
import type { ApiResult, LoginResponse, UserInfo } from '@/types'

export function loginApi(username: string, password: string): Promise<LoginResponse> {
  return http
    .post<ApiResult<LoginResponse>>('/auth/login', { username, password })
    .then((response) => response.data.data)
}

export function logoutApi(): Promise<void> {
  return http.post<ApiResult<void>>('/auth/logout').then(() => undefined)
}

export function meApi(): Promise<UserInfo> {
  return http.get<ApiResult<UserInfo>>('/auth/me').then((response) => response.data.data)
}

export function changePasswordApi(oldPassword: string, newPassword: string): Promise<void> {
  return http
    .put<ApiResult<void>>('/auth/password', { oldPassword, newPassword })
    .then(() => undefined)
}
