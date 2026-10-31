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

export type AnalysisTriggerType = 'FULL' | 'PROJECT' | 'PRECHECK'

export type AnalysisRunStatus = 'RUNNING' | 'SUCCESS' | 'FAILED'

export type DuplicateLevel = 'HIGH' | 'MEDIUM' | 'LOW' | 'NONE'

export type AnalysisDirection =
  | 'SELF_GROUP'
  | 'SIMULTANEOUS'
  | 'OUR_EARLIER'
  | 'PEER_EARLIER'

export interface AnalysisConfig {
  id: number
  version: number
  fieldWeights: Record<string, number>
  thresholds: Record<string, number>
  topK: number
  simultaneousDays: number
  llmEnabled: boolean
  localWeight: number
  llmWeight: number
  llmTopN: number
  llmTimeoutMs: number
  active: boolean
  createdBy: number | null
  createdAt: string | null
}

export interface AnalysisConfigPayload {
  fieldWeights: Record<string, number>
  thresholds: Record<string, number>
  topK: number
  simultaneousDays: number
  llmEnabled: boolean
  localWeight: number
  llmWeight: number
  llmTopN: number
  llmTimeoutMs: number
}

export interface AnalysisRun {
  id: number
  triggerType: AnalysisTriggerType
  projectId: number | null
  configVersion: number
  analysisDate: string | null
  status: AnalysisRunStatus
  projectCount: number
  pairCount: number
  llmUsed: boolean
  durationMs: number | null
  message: string | null
  operatorId: number | null
  createdAt: string | null
}

export interface AnalysisRunPayload {
  triggerType: AnalysisTriggerType
  projectId?: number | null
  analysisDate?: string | null
}

export interface AnalysisRunQueryParams {
  triggerType?: string
  status?: string
  from?: string
  to?: string
  page: number
  size: number
}

export interface AnalysisProjectBrief {
  id: number
  code: string
  name: string
  groupId: number | null
  groupName: string | null
  ownerId: number | null
  ownerName: string | null
  techDirectionId: number | null
  techDirectionName: string | null
  status: string
  budget: number | null
  submitTime: string | null
}

export interface DimensionScores {
  goal: number
  content: number
  tech: number
  milestone: number
  tag: number
}

export interface MatchPoint {
  dimension: string
  terms: string[]
  snippet: string
}

export interface PeerMatch {
  pairId: number | null
  runId: number
  peer: AnalysisProjectBrief
  scores: DimensionScores
  localScore: number
  llmScore: number | null
  totalScore: number
  level: DuplicateLevel
  relation: string
  direction: AnalysisDirection
  earlierProjectId: number | null
  matchedPoints: MatchPoint[]
  diffPoints: MatchPoint[]
  llmReason: string | null
  suggestion: string | null
}

export interface DuplicateSnapshot {
  id: number
  runId: number
  projectId: number
  groupId: number | null
  analysisDate: string
  maxScore: number
  avgTopScore: number
  highPairCount: number
  crossGroupCount: number
  selfGroupCount: number
  ourEarlierCount: number
  peerEarlierCount: number
  simultaneousCount: number
  riskExposure: number | null
  createdAt: string | null
}

export interface ProjectAnalysisReport {
  project: AnalysisProjectBrief
  snapshot: DuplicateSnapshot | null
  topPeers: PeerMatch[]
  riskLevel: DuplicateLevel | null
  thresholds: Record<string, number>
  analyzed: boolean
}

export interface PairDetail {
  pairId: number
  runId: number
  analysisDate: string | null
  project: AnalysisProjectBrief
  peer: AnalysisProjectBrief
  scores: DimensionScores
  localScore: number
  llmScore: number | null
  totalScore: number
  level: DuplicateLevel
  relation: string
  direction: AnalysisDirection
  earlierProjectId: number | null
  matchedPoints: MatchPoint[]
  diffPoints: MatchPoint[]
  llmReason: string | null
  createdAt: string | null
}

export interface AnalysisPrecheckPayload {
  projectId?: number | null
  goalText?: string
  contentText?: string
  techText?: string
  milestoneText?: string
  techDirectionId?: number | null
}

export interface AnalysisPrecheckResult {
  runId: number
  projectId: number | null
  peerCount: number
  configVersion: number
  topPeers: PeerMatch[]
}


