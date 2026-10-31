import http from '@/api/http'
import type {
  ApiResult,
  PageResult,
  ProjectApprovalLog,
  ProjectDetail,
  ProjectDocumentItem,
  ProjectListItem,
  ProjectQueryParams,
  ProjectSavePayload,
  SectionPreview,
  SectionSavePayload
} from '@/types'

export function getProjects(params: ProjectQueryParams): Promise<PageResult<ProjectListItem>> {
  const search = new URLSearchParams()
  if (params.keyword) search.set('keyword', params.keyword)
  if (params.groupId) search.set('groupId', String(params.groupId))
  if (params.fundType) search.set('fundType', params.fundType)
  if (params.techDirectionId) search.set('techDirectionId', String(params.techDirectionId))
  if (params.status) params.status.forEach((status) => search.append('status', status))
  if (params.budgetMin !== undefined && params.budgetMin !== null) {
    search.set('budgetMin', String(params.budgetMin))
  }
  if (params.budgetMax !== undefined && params.budgetMax !== null) {
    search.set('budgetMax', String(params.budgetMax))
  }
  if (params.duplicateScoreMin !== undefined && params.duplicateScoreMin !== null) {
    search.set('duplicateScoreMin', String(params.duplicateScoreMin))
  }
  if (params.duplicateScoreMax !== undefined && params.duplicateScoreMax !== null) {
    search.set('duplicateScoreMax', String(params.duplicateScoreMax))
  }
  search.set('page', String(params.page))
  search.set('size', String(params.size))
  search.set('sortBy', params.sortBy || 'updatedAt')
  search.set('sortOrder', params.sortOrder || 'desc')
  return http
    .get<ApiResult<PageResult<ProjectListItem>>>(`/projects?${search.toString()}`)
    .then((response) => response.data.data)
}

export function getProject(id: number): Promise<ProjectDetail> {
  return http
    .get<ApiResult<ProjectDetail>>(`/projects/${id}`)
    .then((response) => response.data.data)
}

export function createProject(payload: ProjectSavePayload): Promise<ProjectDetail> {
  return http
    .post<ApiResult<ProjectDetail>>('/projects', payload)
    .then((response) => response.data.data)
}

export function updateProject(id: number, payload: ProjectSavePayload): Promise<ProjectDetail> {
  return http
    .put<ApiResult<ProjectDetail>>(`/projects/${id}`, payload)
    .then((response) => response.data.data)
}

export function saveSections(id: number, payload: SectionSavePayload): Promise<ProjectDetail> {
  return http
    .put<ApiResult<ProjectDetail>>(`/projects/${id}/sections`, payload)
    .then((response) => response.data.data)
}

export function deleteProject(id: number): Promise<void> {
  return http.delete<ApiResult<void>>(`/projects/${id}`).then(() => undefined)
}

export function projectAction(
  id: number,
  action: string,
  comment?: string,
  version?: number
): Promise<ProjectDetail> {
  return http
    .post<ApiResult<ProjectDetail>>(`/projects/${id}/actions`, { action, comment, version })
    .then((response) => response.data.data)
}

export function getProjectLogs(id: number): Promise<ProjectApprovalLog[]> {
  return http
    .get<ApiResult<ProjectApprovalLog[]>>(`/projects/${id}/logs`)
    .then((response) => response.data.data)
}

export function uploadDocument(projectId: number, file: File): Promise<ProjectDocumentItem> {
  const form = new FormData()
  form.append('file', file)
  return http
    .post<ApiResult<ProjectDocumentItem>>(`/projects/${projectId}/documents`, form)
    .then((response) => response.data.data)
}

export function parseDocument(documentId: number): Promise<ProjectDocumentItem> {
  return http
    .post<ApiResult<ProjectDocumentItem>>(`/documents/${documentId}/parse`)
    .then((response) => response.data.data)
}

export function deleteDocument(documentId: number): Promise<void> {
  return http.delete<ApiResult<void>>(`/documents/${documentId}`).then(() => undefined)
}

export function getDocumentPreview(projectId: number, documentId: number): Promise<SectionPreview> {
  return http
    .get<ApiResult<SectionPreview>>(
      `/projects/${projectId}/documents/${documentId}/sections`
    )
    .then((response) => response.data.data)
}

export function downloadDocument(documentId: number, fileName: string): Promise<void> {
  return http
    .get<Blob>(`/documents/${documentId}/download`, { responseType: 'blob' })
    .then((response) => {
      const url = window.URL.createObjectURL(response.data)
      const link = document.createElement('a')
      link.href = url
      link.download = fileName
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    })
}
