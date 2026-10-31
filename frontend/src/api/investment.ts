import http from '@/api/http'
import type {
  ApiResult,
  DistributionItem,
  InvestmentOverview,
  InvestmentProject,
  InvestmentQueryParams
} from '@/types'

export function getInvestmentOverview(params: InvestmentQueryParams): Promise<InvestmentOverview> {
  return http
    .get<ApiResult<InvestmentOverview>>('/insight/investment/overview', { params })
    .then((response) => response.data.data)
}

export function getInvestmentByYear(params: InvestmentQueryParams): Promise<DistributionItem[]> {
  return http
    .get<ApiResult<DistributionItem[]>>('/insight/investment/by-year', { params })
    .then((response) => response.data.data)
}

export function getInvestmentByFundType(params: InvestmentQueryParams): Promise<DistributionItem[]> {
  return http
    .get<ApiResult<DistributionItem[]>>('/insight/investment/by-fund-type', { params })
    .then((response) => response.data.data)
}

export function getInvestmentByGroup(params: InvestmentQueryParams): Promise<DistributionItem[]> {
  return http
    .get<ApiResult<DistributionItem[]>>('/insight/investment/by-group', { params })
    .then((response) => response.data.data)
}

export function getInvestmentByDirection(params: InvestmentQueryParams): Promise<DistributionItem[]> {
  return http
    .get<ApiResult<DistributionItem[]>>('/insight/investment/by-direction', { params })
    .then((response) => response.data.data)
}

export function getInvestmentTopRisk(params: InvestmentQueryParams): Promise<InvestmentProject[]> {
  return http
    .get<ApiResult<InvestmentProject[]>>('/insight/investment/top-risk', { params })
    .then((response) => response.data.data)
}

export function getInvestmentProjects(params: InvestmentQueryParams): Promise<InvestmentProject[]> {
  return http
    .get<ApiResult<InvestmentProject[]>>('/insight/investment/projects', { params })
    .then((response) => response.data.data)
}

export async function exportInvestmentExcel(params: InvestmentQueryParams): Promise<void> {
  const response = await http.get<Blob>('/insight/investment/export', {
    params,
    responseType: 'blob'
  })
  const url = window.URL.createObjectURL(response.data)
  const link = document.createElement('a')
  link.href = url
  link.download = `investment-${params.startYear || 'all'}_${params.endYear || 'all'}.xlsx`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

export async function exportInvestmentCsv(params: InvestmentQueryParams): Promise<void> {
  const response = await http.get<Blob>('/insight/investment/export/csv', {
    params,
    responseType: 'blob'
  })
  const url = window.URL.createObjectURL(response.data)
  const link = document.createElement('a')
  link.href = url
  link.download = `investment-${params.startYear || 'all'}_${params.endYear || 'all'}.csv`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}
