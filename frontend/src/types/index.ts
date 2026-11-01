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
  enabled?: number
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
  enabled?: number
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
  reviewRound: number | null
  reviewPassCount: number | null
  requiredPassCount: number | null
  goals: ProjectGoalItem[]
  contents: ProjectContentItem[]
  keyTechs: ProjectKeyTechItem[]
  milestones: ProjectMilestoneItem[]
  members: ProjectMemberItem[]
  approvalLogs: ProjectApprovalLog[]
  reviews: ProjectReview[]
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

export interface RunReportPair {
  pairId: number
  project: AnalysisProjectBrief
  peer: AnalysisProjectBrief
  scores: DimensionScores
  localScore: number
  llmScore: number | null
  totalScore: number
  level: DuplicateLevel
  relation: string
  direction: 'SELF_GROUP' | 'SIMULTANEOUS' | 'A_EARLIER' | 'B_EARLIER'
  earlierProjectId: number | null
  matchedPoints: MatchPoint[]
  diffPoints: MatchPoint[]
  llmReason: string | null
  llmOverlaps: string[] | null
  llmDifferences: string[] | null
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
  llmOverlaps?: string[] | null
  llmDifferences?: string[] | null
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
  llmOverlaps?: string[] | null
  llmDifferences?: string[] | null
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
  llmUsed?: boolean
  topPeers: PeerMatch[]
}

export interface LlmTestResult {
  success: boolean
  status: string
  latencyMs: number
  model: string
  message: string
}

export interface LlmCallLog {
  id: number
  runId: number | null
  projectId: number | null
  peerProjectId: number | null
  model: string | null
  status: string
  latencyMs: number | null
  promptTokens: number | null
  completionTokens: number | null
  error: string | null
  createdAt: string | null
}

export interface LlmLogPage {
  records: LlmCallLog[]
  total: number
  page: number
  size: number
  successCount: number
  failureCount: number
  avgLatencyMs: number
  promptTokens: number
  completionTokens: number
}



export type TrendGranularity = 'MONTH' | 'QUARTER' | 'YEAR'

export interface TrendQueryParams {
  startMonth?: string
  endMonth?: string
  granularity?: TrendGranularity
  groupId?: number | null
  techDirectionId?: number | null
  fundType?: string | null
}

export interface TrendAttributionCounts {
  selfGroup: number
  peerEarlier: number
  ourEarlier: number
  simultaneous: number
}

export interface TrendAttributionRatios {
  selfGroup: number
  peerEarlier: number
  ourEarlier: number
  simultaneous: number
}

export interface TrendMom {
  avgMaxScoreDelta: number
  avgMaxScoreRatio: number | null
}

export interface TrendPoint {
  period: string
  analyzedProjectCount: number
  avgMaxScore: number
  avgTopScore: number
  highRiskCount: number
  pairCount: number
  uniquePairCount: number
  highPairCount: number
  attribution: TrendAttributionCounts
  ratios: TrendAttributionRatios
  mom: TrendMom | null
}

export interface TrendResponse {
  hasData: boolean
  granularity: string
  startMonth: string
  endMonth: string
  points: TrendPoint[]
  summary: string[]
  thresholds: Record<string, number>
  note: string
}

export interface GroupAttribution {
  groupId: number | null
  groupName: string
  counts: TrendAttributionCounts
  total: number
}

export interface TrendAttributionResponse {
  hasData: boolean
  total: number
  counts: TrendAttributionCounts
  ratios: TrendAttributionRatios
  groups: GroupAttribution[]
  note: string
}

export interface HeatmapRow {
  key: string
  label: string
}

export interface HeatmapCell {
  rowKey: string
  column: string
  value: number | null
  sampleCount: number
}

export interface HeatmapResponse {
  hasData: boolean
  dimension: string
  metric: string
  columns: string[]
  rows: HeatmapRow[]
  cells: HeatmapCell[]
  note: string
}

export interface DrillProject {
  projectId: number
  code: string | null
  name: string | null
  groupId: number | null
  groupName: string | null
  techDirectionName: string | null
  fundType: string | null
  maxScore: number
  riskLevel: string
  attributionTotal: number
  analysisDate: string
}

export interface InvestmentQueryParams {
  startYear?: number | null
  endYear?: number | null
  fundType?: string | null
  groupId?: number | null
  techDirectionId?: number | null
  includeCompleted?: boolean
}

export interface InvestmentOverview {
  hasData: boolean
  totalBudget: number
  projectCount: number
  inProgressCount: number
  completedCount: number
  avgBudget: number
  analyzedCount: number
  unanalyzedCount: number
  avgMaxScore: number
  highRiskCount: number
  exposure: number
  highExposure: number
  yoyBudgetRatio: number | null
  yoyCountRatio: number | null
  thresholds: Record<string, number>
  note: string
}

export interface DistributionItem {
  key: string
  label: string
  projectCount: number
  budget: number
  budgetShare: number
  avgMaxScore: number
  highRiskCount: number
  exposure: number
}

export interface InvestmentProject {
  projectId: number
  code: string | null
  name: string | null
  groupId: number | null
  groupName: string | null
  techDirectionName: string | null
  fundType: string | null
  status: string
  budget: number
  maxScore: number | null
  riskLevel: string
  exposure: number | null
  year: number | null
}

export type DecisionMode = 'PROJECT' | 'TEXT'

export type DecisionAction = 'APPROVE' | 'EXPLAIN' | 'MERGE' | 'TERMINATE'

export interface DecisionPrecheckPayload {
  mode: DecisionMode
  projectId?: number | null
  goalText?: string
  contentText?: string
  techText?: string
  milestoneText?: string
  techDirectionId?: number | null
  budget?: number | null
}

export interface SuggestedExpert {
  id: number
  realName: string
  groupName: string | null
  ongoingReviews: number
  sameDirectionReviews: number
}

export interface DecisionPrecheckResult {
  mode: DecisionMode
  runId: number
  projectId: number | null
  projectCode: string | null
  projectName: string | null
  groupName: string | null
  techDirectionName: string | null
  budget: number | null
  riskScore: number
  level: DuplicateLevel
  topPeers: PeerMatch[]
  recommendation: DecisionAction
  ruleId: number | null
  ruleDescription: string | null
  reason: string
  savingsEstimate: number | null
  suggestedExperts: SuggestedExpert[]
  llmUsed: boolean
  inputHash: string | null
}

export interface DecisionRecord {
  id: number
  projectId: number | null
  projectCode: string | null
  projectName: string | null
  groupName: string | null
  runId: number | null
  riskScore: number
  level: DuplicateLevel
  recommendation: DecisionAction
  reason: string | null
  similarProjects: PeerMatch[]
  savingsEstimate: number | null
  inputHash: string | null
  operatorId: number | null
  operatorName: string | null
  createdAt: string | null
}

export interface DecisionSavePayload {
  projectId?: number | null
  runId?: number | null
  riskScore: number
  level: string
  recommendation: string
  reason?: string | null
  similarProjects: string
  savingsEstimate?: number | null
  inputHash?: string | null
}

export interface DecisionQueryParams {
  projectId?: number | null
  groupId?: number | null
  level?: string | null
  recommendation?: string | null
  from?: string | null
  to?: string | null
  page: number
  size: number
}

export interface DecisionRuleItem {
  id?: number
  priority: number
  minScore: number
  maxScore: number
  relation: string | null
  sameGroup: number | null
  action: DecisionAction
  description: string | null
  enabled: number
}

export interface ProjectReview {
  id: number
  projectId: number
  reviewerId: number
  reviewerName: string | null
  round: number
  score: number
  opinion: string
  result: 'PASS' | 'REJECT' | 'REVISE'
  createdAt: string | null
}

export interface SystemUser {
  id: number
  username: string
  realName: string | null
  email: string | null
  phone: string | null
  groupId: number | null
  groupName: string | null
  status: string
  roles: string[]
  lastLoginAt: string | null
  createdAt: string | null
}

export interface SystemUserPayload {
  username: string
  password?: string
  realName?: string | null
  email?: string | null
  phone?: string | null
  groupId?: number | null
  status: string
  roles: string[]
}

export interface PasswordResetResult {
  userId: number
  username: string
  temporaryPassword: string
}

export interface RolePermissionItem {
  code: string
  label: string
  roles: string[]
}

export interface RolePermission {
  id: number
  code: string
  name: string
  description: string | null
  permissions: RolePermissionItem[]
}

export interface SystemConfigItem {
  key: string
  value: string
  description: string | null
  updatedBy: number | null
  updatedAt: string | null
}

export interface AuditLogItem {
  id: number
  userId: number | null
  username: string | null
  action: string
  resourceType: string | null
  resourceId: string | null
  detail: string | null
  ip: string | null
  result: string | null
  costMs: number | null
  createdAt: string | null
}

export interface AuditLogQueryParams {
  from?: string | null
  to?: string | null
  username?: string | null
  action?: string | null
  resourceType?: string | null
  result?: string | null
  page: number
  size: number
}

export interface TodoItem {
  projectId: number
  code: string
  name: string
  groupId: number | null
  groupName: string | null
  status: string
  action: string
  actionLabel: string
}

export interface TodoSummary {
  total: number
  counts: Record<string, number>
  items: TodoItem[]
}

export interface GroupSavePayload {
  name: string
  code: string
  parentId?: number | null
  leader?: string | null
  sort?: number
  enabled?: number
}

export interface TechDirectionSavePayload {
  name: string
  code: string
  parentId?: number | null
  keywords?: string[]
  sort?: number
  enabled?: number
}
