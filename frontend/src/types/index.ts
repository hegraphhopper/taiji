export interface ApiResult<T> {
  code: number
  message: string
  data: T
  timestamp?: number
}

export interface DictItem {
  code: string
  name: string
  description?: string | null
}

export interface TechDirection {
  id: number
  code: string
  name: string
  parentId: number | null
  keywords: string[]
  sort: number
}

export interface Dicts {
  fundTypes: DictItem[]
  projectStatuses: DictItem[]
  roles: DictItem[]
  techDirections: TechDirection[]
}

export interface UserInfo {
  id: number
  username: string
  realName: string
  email: string | null
  phone: string | null
  groupId: number | null
  groupName: string | null
  roles: string[]
  status: string
  lastLoginAt: string | null
}

export interface LoginResponse {
  token: string
  expiresAt: string
  user: UserInfo
  roles: string[]
  permissions: string[]
}

export interface GroupItem {
  id: number
  name: string
  code: string
  parentId: number | null
  leader: string | null
  sort: number
}
