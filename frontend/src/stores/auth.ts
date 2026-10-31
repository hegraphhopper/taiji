import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { changePasswordApi, loginApi, logoutApi, meApi } from '@/api/auth'
import { TOKEN_KEY } from '@/api/http'
import type { UserInfo } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const user = ref<UserInfo | null>(null)
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])

  const isLoggedIn = computed(() => Boolean(token.value))
  const displayName = computed(() => user.value?.realName || user.value?.username || '未登录')
  const roleLabel = computed(() => {
    const map: Record<string, string> = {
      ADMIN: '系统管理员',
      PMO: '基金办',
      GROUP_USER: '项目组用户',
      EXPERT: '评审专家'
    }
    return roles.value.map((role) => map[role] || role).join(' / ') || '无角色'
  })

  async function login(username: string, password: string): Promise<void> {
    const result = await loginApi(username, password)
    token.value = result.token
    user.value = result.user
    roles.value = result.roles
    permissions.value = result.permissions
    localStorage.setItem(TOKEN_KEY, result.token)
  }

  async function fetchMe(): Promise<void> {
    const info = await meApi()
    user.value = info
    roles.value = info.roles
  }

  function clearSession(): void {
    token.value = ''
    user.value = null
    roles.value = []
    permissions.value = []
    localStorage.removeItem(TOKEN_KEY)
  }

  async function logout(): Promise<void> {
    try {
      if (token.value) {
        await logoutApi()
      }
    } catch {
      // 无状态登出失败不阻断前端清理
    }
    clearSession()
  }

  async function changePassword(oldPassword: string, newPassword: string): Promise<void> {
    await changePasswordApi(oldPassword, newPassword)
  }

  function hasRole(...codes: string[]): boolean {
    return codes.some((code) => roles.value.includes(code))
  }

  function hasPermission(code: string): boolean {
    return permissions.value.includes(code)
  }

  return {
    token,
    user,
    roles,
    permissions,
    isLoggedIn,
    displayName,
    roleLabel,
    login,
    fetchMe,
    logout,
    clearSession,
    changePassword,
    hasRole,
    hasPermission
  }
})
