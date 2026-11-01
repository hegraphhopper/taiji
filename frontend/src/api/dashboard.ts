import http from '@/api/http'
import type { ApiResult, DashboardSummary } from '@/types'

export function getDashboardSummary(): Promise<DashboardSummary> {
  return http
    .get<ApiResult<DashboardSummary>>('/dashboard/summary')
    .then((response) => response.data.data)
}
