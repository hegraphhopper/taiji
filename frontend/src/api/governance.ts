import http from '@/api/http'
import type {
  ApiResult,
  AuditLogItem,
  AuditLogQueryParams,
  GroupItem,
  GroupSavePayload,
  PageResult,
  PasswordResetResult,
  ProjectReview,
  RolePermission,
  SystemConfigItem,
  SystemUser,
  SystemUserPayload,
  TechDirection,
  TechDirectionSavePayload,
  TodoSummary
} from '@/types'

export function getUsers(params: {
  keyword?: string
  groupId?: number | null
  status?: string | null
  role?: string | null
  page: number
  size: number
}): Promise<PageResult<SystemUser>> {
  const search = new URLSearchParams()
  if (params.keyword) search.set('keyword', params.keyword)
  if (params.groupId) search.set('groupId', String(params.groupId))
  if (params.status) search.set('status', params.status)
  if (params.role) search.set('role', params.role)
  search.set('page', String(params.page))
  search.set('size', String(params.size))
  return http
    .get<ApiResult<PageResult<SystemUser>>>(`/users?${search.toString()}`)
    .then((response) => response.data.data)
}

export function createUser(payload: SystemUserPayload): Promise<SystemUser> {
  return http
    .post<ApiResult<SystemUser>>('/users', payload)
    .then((response) => response.data.data)
}

export function updateUser(id: number, payload: SystemUserPayload): Promise<SystemUser> {
  return http
    .put<ApiResult<SystemUser>>(`/users/${id}`, payload)
    .then((response) => response.data.data)
}

export function resetUserPassword(id: number): Promise<PasswordResetResult> {
  return http
    .post<ApiResult<PasswordResetResult>>(`/users/${id}/reset-password`)
    .then((response) => response.data.data)
}

export function updateUserRoles(id: number, roles: string[]): Promise<SystemUser> {
  return http
    .put<ApiResult<SystemUser>>(`/users/${id}/roles`, { roles })
    .then((response) => response.data.data)
}

export function getAllGroups(): Promise<GroupItem[]> {
  return http.get<ApiResult<GroupItem[]>>('/groups/all').then((response) => response.data.data)
}

export function createGroup(payload: GroupSavePayload): Promise<GroupItem> {
  return http
    .post<ApiResult<GroupItem>>('/groups', payload)
    .then((response) => response.data.data)
}

export function updateGroup(id: number, payload: GroupSavePayload): Promise<GroupItem> {
  return http
    .put<ApiResult<GroupItem>>(`/groups/${id}`, payload)
    .then((response) => response.data.data)
}

export function deleteGroup(id: number): Promise<void> {
  return http.delete<ApiResult<void>>(`/groups/${id}`).then(() => undefined)
}

export function getAllTechDirections(): Promise<TechDirection[]> {
  return http
    .get<ApiResult<TechDirection[]>>('/tech-directions/all')
    .then((response) => response.data.data)
}

export function createTechDirection(
  payload: TechDirectionSavePayload
): Promise<TechDirection> {
  return http
    .post<ApiResult<TechDirection>>('/tech-directions', payload)
    .then((response) => response.data.data)
}

export function updateTechDirection(
  id: number,
  payload: TechDirectionSavePayload
): Promise<TechDirection> {
  return http
    .put<ApiResult<TechDirection>>(`/tech-directions/${id}`, payload)
    .then((response) => response.data.data)
}

export function deleteTechDirection(id: number): Promise<void> {
  return http.delete<ApiResult<void>>(`/tech-directions/${id}`).then(() => undefined)
}

export function getRoles(): Promise<RolePermission[]> {
  return http.get<ApiResult<RolePermission[]>>('/roles').then((response) => response.data.data)
}

export function getSystemConfigs(): Promise<SystemConfigItem[]> {
  return http
    .get<ApiResult<SystemConfigItem[]>>('/system/configs')
    .then((response) => response.data.data)
}

export function updateSystemConfig(
  key: string,
  value: string,
  description?: string | null
): Promise<SystemConfigItem> {
  return http
    .put<ApiResult<SystemConfigItem>>(`/system/configs/${key}`, { value, description })
    .then((response) => response.data.data)
}

export function getAuditLogs(params: AuditLogQueryParams): Promise<PageResult<AuditLogItem>> {
  const search = new URLSearchParams()
  if (params.from) search.set('from', params.from)
  if (params.to) search.set('to', params.to)
  if (params.username) search.set('username', params.username)
  if (params.action) search.set('action', params.action)
  if (params.resourceType) search.set('resourceType', params.resourceType)
  if (params.result) search.set('result', params.result)
  search.set('page', String(params.page))
  search.set('size', String(params.size))
  return http
    .get<ApiResult<PageResult<AuditLogItem>>>(`/audit-logs?${search.toString()}`)
    .then((response) => response.data.data)
}

export async function downloadAuditLogs(params: AuditLogQueryParams): Promise<void> {
  const search = new URLSearchParams()
  if (params.from) search.set('from', params.from)
  if (params.to) search.set('to', params.to)
  if (params.username) search.set('username', params.username)
  if (params.action) search.set('action', params.action)
  if (params.resourceType) search.set('resourceType', params.resourceType)
  if (params.result) search.set('result', params.result)
  const response = await http.get<Blob>(`/audit-logs/export?${search.toString()}`, {
    responseType: 'blob'
  })
  const url = window.URL.createObjectURL(response.data)
  const link = document.createElement('a')
  link.href = url
  link.download = 'audit-logs.csv'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

export function getTodo(): Promise<TodoSummary> {
  return http.get<ApiResult<TodoSummary>>('/tasks/todo').then((response) => response.data.data)
}

export function getProjectReviews(projectId: number): Promise<ProjectReview[]> {
  return http
    .get<ApiResult<ProjectReview[]>>(`/projects/${projectId}/reviews`)
    .then((response) => response.data.data)
}

export function submitProjectReview(
  projectId: number,
  payload: { score: number; opinion: string; result: string }
): Promise<ProjectReview> {
  return http
    .post<ApiResult<ProjectReview>>(`/projects/${projectId}/reviews`, payload)
    .then((response) => response.data.data)
}
