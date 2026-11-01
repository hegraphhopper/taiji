import http from '@/api/http'
import type {
  AnalysisConfig,
  AnalysisConfigPayload,
  AnalysisPrecheckPayload,
  AnalysisPrecheckResult,
  AnalysisRun,
  AnalysisRunPayload,
  AnalysisRunQueryParams,
  ApiResult,
  PageResult,
  PairDetail,
  ProjectAnalysisReport,
  LlmTestResult,
  LlmLogPage
} from '@/types'

export function getAnalysisConfig(): Promise<AnalysisConfig> {
  return http
    .get<ApiResult<AnalysisConfig>>('/analysis/config')
    .then((response) => response.data.data)
}

export function getAnalysisConfigHistory(): Promise<AnalysisConfig[]> {
  return http
    .get<ApiResult<AnalysisConfig[]>>('/analysis/config/history')
    .then((response) => response.data.data)
}

export function saveAnalysisConfig(payload: AnalysisConfigPayload): Promise<AnalysisConfig> {
  return http
    .put<ApiResult<AnalysisConfig>>('/analysis/config', payload)
    .then((response) => response.data.data)
}

export function triggerAnalysisRun(payload: AnalysisRunPayload): Promise<AnalysisRun> {
  return http
    .post<ApiResult<AnalysisRun>>('/analysis/runs', payload)
    .then((response) => response.data.data)
}

export function getAnalysisRuns(params: AnalysisRunQueryParams): Promise<PageResult<AnalysisRun>> {
  const search = new URLSearchParams()
  if (params.triggerType) search.set('triggerType', params.triggerType)
  if (params.status) search.set('status', params.status)
  if (params.from) search.set('from', params.from)
  if (params.to) search.set('to', params.to)
  search.set('page', String(params.page))
  search.set('size', String(params.size))
  return http
    .get<ApiResult<PageResult<AnalysisRun>>>(`/analysis/runs?${search.toString()}`)
    .then((response) => response.data.data)
}

export function getAnalysisRun(id: number): Promise<AnalysisRun> {
  return http
    .get<ApiResult<AnalysisRun>>(`/analysis/runs/${id}`)
    .then((response) => response.data.data)
}

export function deleteAnalysisRun(id: number): Promise<void> {
  return http.delete<ApiResult<void>>(`/analysis/runs/${id}`).then(() => undefined)
}

export function getProjectAnalysis(projectId: number): Promise<ProjectAnalysisReport> {
  return http
    .get<ApiResult<ProjectAnalysisReport>>(`/analysis/projects/${projectId}`)
    .then((response) => response.data.data)
}

export function getAnalysisPair(pairId: number): Promise<PairDetail> {
  return http
    .get<ApiResult<PairDetail>>(`/analysis/pairs/${pairId}`)
    .then((response) => response.data.data)
}

export function precheckAnalysis(payload: AnalysisPrecheckPayload): Promise<AnalysisPrecheckResult> {
  return http
    .post<ApiResult<AnalysisPrecheckResult>>('/analysis/precheck', payload)
    .then((response) => response.data.data)
}

export function testLlmConnection(): Promise<LlmTestResult> {
  return http
    .post<ApiResult<LlmTestResult>>('/system/llm/test')
    .then((response) => response.data.data)
}

export function getLlmLogs(params: {
  page: number
  size: number
  runId?: number | null
  status?: string
  model?: string
}): Promise<LlmLogPage> {
  const search = new URLSearchParams()
  search.set('page', String(params.page))
  search.set('size', String(params.size))
  if (params.runId) search.set('runId', String(params.runId))
  if (params.status) search.set('status', params.status)
  if (params.model) search.set('model', params.model)
  return http
    .get<ApiResult<LlmLogPage>>(`/analysis/llm-logs?${search.toString()}`)
    .then((response) => response.data.data)
}
