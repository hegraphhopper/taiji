export const STATUS_LABELS: Record<string, string> = {
  DRAFT: '草稿',
  SUBMITTED: '已提交',
  REVIEWING: '评审中',
  APPROVED: '已立项',
  REJECTED: '已驳回',
  IN_PROGRESS: '在研',
  COMPLETED: '已结项',
  TERMINATED: '已终止'
}

export const STATUS_TAG_TYPES: Record<string, 'info' | 'warning' | 'success' | 'danger' | 'primary'> = {
  DRAFT: 'info',
  SUBMITTED: 'warning',
  REVIEWING: 'primary',
  APPROVED: 'success',
  REJECTED: 'danger',
  IN_PROGRESS: 'primary',
  COMPLETED: 'success',
  TERMINATED: 'info'
}

export const FUND_TYPE_LABELS: Record<string, string> = {
  PLATFORM: '平台基金',
  INNOVATION: '创新基金',
  OTHER: '其他'
}

export const DIFFICULTY_LABELS: Record<string, string> = {
  HIGH: '高',
  MEDIUM: '中',
  LOW: '低'
}

export const MEMBER_ROLE_LABELS: Record<string, string> = {
  LEADER: '负责人',
  CORE: '核心成员',
  PARTICIPANT: '参与人'
}

export function statusLabel(status: string): string {
  return STATUS_LABELS[status] || status
}

export function statusTagType(status: string): 'info' | 'warning' | 'success' | 'danger' | 'primary' {
  return STATUS_TAG_TYPES[status] || 'info'
}

export function formatMoney(value: number | null | undefined): string {
  if (value === null || value === undefined) {
    return '—'
  }
  return value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export function formatDateTime(value: string | null | undefined): string {
  return value || '—'
}
