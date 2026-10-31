import axios, { type AxiosError, type AxiosResponse } from 'axios'
import type { ApiResult } from '@/types'

export const TOKEN_KEY = 'fund_platform_token'

const http = axios.create({
  baseURL: '/api/v1',
  timeout: 15000
})

let redirectingToLogin = false

function redirectToLogin(): void {
  if (redirectingToLogin) {
    return
  }
  const current = `${window.location.pathname}${window.location.search}`
  if (current.startsWith('/login')) {
    return
  }
  redirectingToLogin = true
  window.location.href = `/login?redirect=${encodeURIComponent(current)}`
}

function resolveErrorMessage(error: AxiosError<ApiResult<unknown>>): string {
  const serverMessage = error.response?.data?.message
  if (serverMessage) {
    return serverMessage
  }
  if (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT') {
    return '请求超时，请稍后重试'
  }
  if (error.response) {
    return `请求失败（HTTP ${error.response.status}）`
  }
  if (error.message) {
    return error.message
  }
  return '网络异常，请稍后重试'
}

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response: AxiosResponse<ApiResult<unknown>>) => {
    if (response.config.responseType === 'blob') {
      return response
    }
    const body = response.data
    if (body && typeof body.code === 'number' && body.code !== 0) {
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return response
  },
  (error: AxiosError<ApiResult<unknown>>) => {
    const status = error.response?.status
    if (status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      redirectToLogin()
    } else if (status === 403 && window.location.pathname !== '/403') {
      window.location.href = '/403'
    }
    return Promise.reject(new Error(resolveErrorMessage(error)))
  }
)

export default http
