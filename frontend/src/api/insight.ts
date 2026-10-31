import http from '@/api/http'
import type {
  ApiResult,
  DrillProject,
  HeatmapResponse,
  TrendAttributionResponse,
  TrendQueryParams,
  TrendResponse
} from '@/types'

export function getTrend(params: TrendQueryParams): Promise<TrendResponse> {
  return http
    .get<ApiResult<TrendResponse>>('/insight/trend', { params })
    .then((response) => response.data.data)
}

export function getAttribution(params: TrendQueryParams): Promise<TrendAttributionResponse> {
  return http
    .get<ApiResult<TrendAttributionResponse>>('/insight/attribution', { params })
    .then((response) => response.data.data)
}

export function getHeatmap(
  params: TrendQueryParams & { dimension: string; metric?: string }
): Promise<HeatmapResponse> {
  return http
    .get<ApiResult<HeatmapResponse>>('/insight/heatmap', { params })
    .then((response) => response.data.data)
}

export function getDrillProjects(
  params: TrendQueryParams & { period: string; level?: string }
): Promise<DrillProject[]> {
  return http
    .get<ApiResult<DrillProject[]>>('/insight/trend/projects', { params })
    .then((response) => response.data.data)
}

export async function exportTrendCsv(params: TrendQueryParams): Promise<void> {
  const response = await http.get<Blob>('/insight/trend/export', {
    params,
    responseType: 'blob'
  })
  const blob = response.data
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `trend-${params.startMonth || ''}_${params.endMonth || ''}.csv`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}
