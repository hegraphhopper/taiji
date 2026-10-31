import type { RolePermissionItem } from '@/types'

export const PERMISSIONS: Record<string, { label: string; roles: string[] }> = {
  SYSTEM_USER_MANAGE: { label: '用户管理', roles: ['ADMIN'] },
  SYSTEM_GROUP_MANAGE: { label: '项目组管理', roles: ['ADMIN'] },
  SYSTEM_DICT_MANAGE: { label: '技术方向管理', roles: ['ADMIN'] },
  SYSTEM_CONFIG_MANAGE: { label: '系统参数管理', roles: ['ADMIN'] },
  ROLE_VIEW: { label: '角色与权限点查看', roles: ['ADMIN'] },
  AUDIT_VIEW: { label: '审计日志', roles: ['ADMIN'] },
  ANALYSIS_RUN: { label: '分析运行', roles: ['ADMIN', 'PMO'] },
  ANALYSIS_CONFIG: { label: '分析配置', roles: ['ADMIN'] },
  INSIGHT_TREND: { label: '趋势与归因', roles: ['ADMIN', 'PMO', 'GROUP_USER'] },
  INSIGHT_INVESTMENT: { label: '投入态势', roles: ['ADMIN', 'PMO', 'GROUP_USER'] },
  DECISION_PRECHECK: { label: '辅助决策预检', roles: ['ADMIN', 'PMO', 'GROUP_USER'] },
  DECISION_RECORD_VIEW: { label: '决策记录查看', roles: ['ADMIN', 'PMO', 'GROUP_USER', 'EXPERT'] },
  DECISION_RULE_MANAGE: { label: '决策规则配置', roles: ['ADMIN'] },
  PROJECT_VIEW_ALL: { label: '全所项目查看', roles: ['ADMIN', 'PMO'] },
  PROJECT_VIEW_GROUP: { label: '本组项目查看', roles: ['GROUP_USER'] },
  PROJECT_EDIT: { label: '项目维护', roles: ['ADMIN', 'GROUP_USER'] },
  PROJECT_SUBMIT: { label: '项目提交与撤回', roles: ['ADMIN', 'GROUP_USER'] },
  PROJECT_REVIEW: { label: '形式审查与立项', roles: ['ADMIN', 'PMO'] },
  PROJECT_EXECUTE: { label: '执行与结项', roles: ['ADMIN', 'GROUP_USER'] },
  PROJECT_REVIEW_SCORE: { label: '专家评审', roles: ['ADMIN', 'EXPERT'] },
  TODO_VIEW: { label: '待办查看', roles: ['ADMIN', 'PMO', 'GROUP_USER', 'EXPERT'] },
  REPORT_EXPORT: { label: '报告导出', roles: ['ADMIN', 'PMO', 'GROUP_USER', 'EXPERT'] }
}

export function hasPermission(roles: string[], code: string): boolean {
  const permission = PERMISSIONS[code]
  if (!permission) {
    return false
  }
  return roles.some((role) => permission.roles.includes(role))
}

export function permissionsOfRole(role: string): RolePermissionItem[] {
  return Object.entries(PERMISSIONS)
    .filter(([, value]) => value.roles.includes(role))
    .map(([code, value]) => ({ code, label: value.label, roles: value.roles }))
}
