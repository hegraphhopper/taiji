import http from '@/api/http'
import type {
  ApiResult,
  DecisionPrecheckPayload,
  DecisionPrecheckResult,
  DecisionQueryParams,
  DecisionRecord,
  DecisionRuleItem,
  DecisionSavePayload,
  PageResult
} from '@/types'

export function runDecisionPrecheck(
  payload: DecisionPrecheckPayload
): Promise<DecisionPrecheckResult> {
  return http
    .post<ApiResult<DecisionPrecheckResult>>('/decision/precheck', payload)
    .then((response) => response.data.data)
}

export function saveDecision(payload: DecisionSavePayload): Promise<DecisionRecord> {
  return http
    .post<ApiResult<DecisionRecord>>('/decision/records', payload)
    .then((response) => response.data.data)
}

export function getDecisionRecords(
  params: DecisionQueryParams
): Promise<PageResult<DecisionRecord>> {
  const search = new URLSearchParams()
  if (params.projectId) search.set('projectId', String(params.projectId))
  if (params.groupId) search.set('groupId', String(params.groupId))
  if (params.level) search.set('level', params.level)
  if (params.recommendation) search.set('recommendation', params.recommendation)
  if (params.from) search.set('from', params.from)
  if (params.to) search.set('to', params.to)
  search.set('page', String(params.page))
  search.set('size', String(params.size))
  return http
    .get<ApiResult<PageResult<DecisionRecord>>>(`/decision/records?${search.toString()}`)
    .then((response) => response.data.data)
}

export function getDecisionRecord(id: number): Promise<DecisionRecord> {
  return http
    .get<ApiResult<DecisionRecord>>(`/decision/records/${id}`)
    .then((response) => response.data.data)
}

export function voidDecisionRecord(id: number): Promise<void> {
  return http.delete<ApiResult<void>>(`/decision/records/${id}`).then(() => undefined)
}

export function getDecisionRules(): Promise<DecisionRuleItem[]> {
  return http.get<ApiResult<DecisionRuleItem[]>>('/decision/rules').then((response) => response.data.data)
}

export function saveDecisionRules(rules: DecisionRuleItem[]): Promise<DecisionRuleItem[]> {
  return http
    .put<ApiResult<DecisionRuleItem[]>>('/decision/rules', rules)
    .then((response) => response.data.data)
}

async function downloadReport(id: number, type: 'docx' | 'xlsx'): Promise<void> {
  const response = await http.get<Blob>(`/decision/records/${id}/report.${type}`, {
    responseType: 'blob'
  })
  const disposition = response.headers['content-disposition'] as string | undefined
  let fileName = `decision-${id}.${type}`
  const match = disposition?.match(/filename\*=UTF-8''([^;]+)/i)
  if (match) {
    fileName = decodeURIComponent(match[1])
  }
  const url = window.URL.createObjectURL(response.data)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

export function downloadDecisionWord(id: number): Promise<void> {
  return downloadReport(id, 'docx')
}

export function downloadDecisionExcel(id: number): Promise<void> {
  return downloadReport(id, 'xlsx')
}
