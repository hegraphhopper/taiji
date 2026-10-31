import http from '@/api/http'
import type { ApiResult, Dicts, GroupItem, TechDirection } from '@/types'

export function getDictsApi(): Promise<Dicts> {
  return http.get<ApiResult<Dicts>>('/dicts').then((response) => response.data.data)
}

export function getGroupsApi(): Promise<GroupItem[]> {
  return http.get<ApiResult<GroupItem[]>>('/groups').then((response) => response.data.data)
}

export function getTechDirectionsApi(): Promise<TechDirection[]> {
  return http
    .get<ApiResult<TechDirection[]>>('/tech-directions')
    .then((response) => response.data.data)
}
