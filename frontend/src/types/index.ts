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

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export interface ProjectListItem {
  id: number
  code: string
  name: string
  fundType: string
  groupId: number
  groupName: string | null
  ownerId: number
  ownerName: string | null
  techDirectionId: number | null
  techDirectionName: string | null
  budget: number
  startDate: string | null
  endDate: string | null
  status: string
  duplicateScore: number | null
  riskLevel: string | null
  submitTime: string | null
  updatedAt: string | null
  version: number
}

export interface ProjectGoalItem {
  id?: number
  content: string
  sort: number
}

export interface ProjectContentItem {
  id?: number
  content: string
  sort: number
}

export interface ProjectKeyTechItem {
  id?: number
  name: string
  description?: string | null
  difficulty?: string | null
  sort: number
}

export interface ProjectMilestoneItem {
  id?: number
  name: string
  planStart?: string | null
  planEnd?: string | null
  deliverable?: string | null
  weight: number
  sort: number
}

export interface ProjectMemberItem {
  id?: number
  userId: number
  memberRole: string
  workload: number
}

export interface ProjectApprovalLog {
  id: number
  fromStatus: string | null
  toStatus: string
  action: string
  operatorId: number
  comment: string | null
  createdAt: string | null
}

export interface ProjectDocumentItem {
  id: number
  projectId: number
  fileName: string
  fileType: string
  fileSize: number
  parseStatus: string
  parseMessage: string | null
  uploadedBy: number | null
  createdAt: string | null
}

export interface ProjectDetail extends ProjectListItem {
  summary: string | null
  expectedOutcome: string | null
  createdAt: string | null
  goals: ProjectGoalItem[]
  contents: ProjectContentItem[]
  keyTechs: ProjectKeyTechItem[]
  milestones: ProjectMilestoneItem[]
  members: ProjectMemberItem[]
  approvalLogs: ProjectApprovalLog[]
  documents: ProjectDocumentItem[]
}

export interface ProjectSavePayload {
  name: string
  code?: string
  fundType: string
  groupId: number
  ownerId: number
  techDirectionId?: number | null
  budget: number
  startDate?: string | null
  endDate?: string | null
  summary?: string | null
  expectedOutcome?: string | null
  version?: number
}

export interface SectionSavePayload {
  goals: Array<{ content: string; sort: number }>
  contents: Array<{ content: string; sort: number }>
  keyTechs: Array<{ name: string; description?: string; difficulty?: string; sort: number }>
  milestones: Array<{
    name: string
    planStart?: string
    planEnd?: string
    deliverable?: string
    weight: number
    sort: number
  }>
  members: Array<{ userId: number; memberRole: string; workload: number }>
}

export interface ProjectQueryParams {
  keyword?: string
  groupId?: number
  fundType?: string
  techDirectionId?: number
  status?: string[]
  budgetMin?: number
  budgetMax?: number
  duplicateScoreMin?: number
  duplicateScoreMax?: number
  page: number
  size: number
  sortBy?: string
  sortOrder?: string
}

export interface SectionPreview {
  parsed: boolean
  message: string | null
  goals: string[]
  contents: string[]
  keyTechs: string[]
  milestones: string[]
  indicators: string[]
}

