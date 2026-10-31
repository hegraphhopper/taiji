<template>
  <el-container class="app-shell">
    <el-aside :width="collapsed ? '72px' : '232px'" class="sidebar">
      <div class="brand" :class="{ compact: collapsed }">
        <span class="brand-mark">TJ</span>
        <span v-if="!collapsed" class="brand-text">
          <strong>太极 · 项目治理</strong>
          <small>自投研发重复投入</small>
        </span>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed"
        :collapse-transition="false"
        background-color="transparent"
        text-color="#c7d2e5"
        active-text-color="#ffffff"
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title>工作台</template>
        </el-menu-item>
        <el-menu-item index="/projects">
          <el-icon><Folder /></el-icon>
          <template #title>项目管理 <span class="phase-tag">A2</span></template>
        </el-menu-item>
        <el-sub-menu v-if="canAnalyze" index="/analysis">
          <template #title>
            <el-icon><DataAnalysis /></el-icon>
            <span>分析中心 <span class="phase-tag">A3</span></span>
          </template>
          <el-menu-item index="/analysis/runs">分析运行</el-menu-item>
          <el-menu-item v-if="isAdmin" index="/analysis/config">分析配置</el-menu-item>
        </el-sub-menu>
        <el-menu-item v-if="canViewTrend" index="/trend">
          <el-icon><TrendCharts /></el-icon>
          <template #title>趋势与归因 <span class="phase-tag">A4</span></template>
        </el-menu-item>
      </el-menu>

      <div v-if="!collapsed" class="sidebar-foot">
        <span class="dot"></span> A4 趋势与归因 · 只读聚合
      </div>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div class="topbar-left">
          <el-button
            text
            circle
            class="collapse-btn"
            aria-label="切换侧边栏"
            title="切换侧边栏"
            @click="collapsed = !collapsed"
          >
            <el-icon :size="18"><Expand v-if="collapsed" /><Fold v-else /></el-icon>
          </el-button>
          <span class="crumb">自投项目治理 / {{ pageTitle }}</span>
        </div>
        <div class="topbar-right">
          <el-tag size="small" effect="plain" class="role-tag">{{ auth.roleLabel }}</el-tag>
          <el-dropdown trigger="click" @command="handleCommand">
            <span class="user-trigger">
              <el-avatar :size="30" class="user-avatar">{{ initial }}</el-avatar>
              <span class="user-name">{{ auth.displayName }}</span>
              <el-icon :size="14"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>

  <el-dialog
    v-model="passwordVisible"
    title="修改密码"
    width="440px"
    :close-on-click-modal="false"
    @closed="resetPasswordForm"
  >
    <el-form
      ref="passwordFormRef"
      :model="passwordForm"
      :rules="passwordRules"
      label-width="96px"
      label-position="right"
      @submit.prevent="submitPassword"
    >
      <el-form-item label="当前密码" prop="oldPassword">
        <el-input v-model="passwordForm.oldPassword" type="password" show-password autocomplete="current-password" />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="passwordForm.newPassword" type="password" show-password autocomplete="new-password" />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input v-model="passwordForm.confirmPassword" type="password" show-password autocomplete="new-password" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="passwordVisible = false">取消</el-button>
      <el-button type="primary" :loading="passwordSubmitting" @click="submitPassword">保存并重新登录</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  ArrowDown,
  DataAnalysis,
  Expand,
  Fold,
  Folder,
  Odometer,
  TrendCharts
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useDictStore } from '@/stores/dict'

const collapsed = ref(false)
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const dict = useDictStore()

const pageTitle = computed(() => (route.meta.title as string) || '工作台')
const activeMenu = computed(() => route.path)
const initial = computed(() => auth.displayName.slice(0, 1))
const canAnalyze = computed(() => auth.hasRole('ADMIN', 'PMO'))
const canViewTrend = computed(() => auth.hasRole('ADMIN', 'PMO', 'GROUP_USER'))
const isAdmin = computed(() => auth.hasRole('ADMIN'))

const passwordVisible = ref(false)
const passwordSubmitting = ref(false)
const passwordFormRef = ref<FormInstance>()
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 64, message: '长度需为 8-64 位', trigger: 'blur' },
    {
      pattern: /^(?=.*[A-Za-z])(?=.*\d).+$/,
      message: '需同时包含字母和数字',
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

onMounted(() => {
  dict.load().catch(() => undefined)
})

function handleCommand(command: string): void {
  if (command === 'password') {
    passwordVisible.value = true
  } else if (command === 'logout') {
    handleLogout()
  }
}

async function handleLogout(): Promise<void> {
  try {
    await ElMessageBox.confirm('确定退出登录吗？', '提示', {
      type: 'warning',
      confirmButtonText: '退出',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await auth.logout()
  ElMessage.success('已退出登录')
  router.replace('/login')
}

async function submitPassword(): Promise<void> {
  if (passwordSubmitting.value || !passwordFormRef.value) {
    return
  }
  passwordSubmitting.value = true
  try {
    const valid = await passwordFormRef.value.validate().catch(() => false)
    if (!valid) {
      return
    }
    await auth.changePassword(passwordForm.oldPassword, passwordForm.newPassword)
    passwordVisible.value = false
    ElMessage.success('密码已修改，请重新登录')
    await auth.logout()
    router.replace('/login')
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '修改密码失败')
  } finally {
    passwordSubmitting.value = false
  }
}

function resetPasswordForm(): void {
  passwordFormRef.value?.resetFields()
}
</script>

<style scoped>
.app-shell {
  height: 100vh;
  overflow: hidden;
}

.sidebar {
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, var(--sidebar) 0%, var(--sidebar-strong) 100%);
  color: var(--sidebar-text);
  transition: width 0.2s ease;
  overflow: hidden;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 22px 20px 20px;
  min-height: 86px;
}

.brand.compact {
  justify-content: center;
  padding: 22px 0 20px;
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--brand-primary), var(--brand-accent));
  color: #fff;
  font-weight: 700;
  font-size: 13px;
  letter-spacing: 0.5px;
  flex: 0 0 auto;
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
  white-space: nowrap;
}

.brand-text strong {
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

.brand-text small {
  color: #8093b3;
  font-size: 11px;
  margin-top: 2px;
}

.sidebar :deep(.el-menu) {
  border-right: 0;
  padding: 6px 10px;
}

.sidebar :deep(.el-menu-item) {
  height: 44px;
  border-radius: 8px;
  margin-bottom: 4px;
  font-size: 13px;
}

.sidebar :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(31, 94, 255, 0.9), rgba(0, 194, 184, 0.55));
  color: #fff;
}

.sidebar :deep(.el-menu-item:hover:not(.is-disabled)) {
  background: rgba(255, 255, 255, 0.06);
}

.sidebar :deep(.el-menu-item.is-disabled) {
  opacity: 0.45;
}

.phase-tag {
  margin-left: 8px;
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.1);
  color: #9fb0cc;
}

.sidebar-foot {
  margin-top: auto;
  padding: 16px 22px 20px;
  font-size: 11px;
  color: #6f82a3;
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--brand-accent);
  box-shadow: 0 0 8px rgba(0, 194, 184, 0.8);
}

.topbar {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid var(--border);
}

.topbar-left,
.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.collapse-btn {
  color: var(--ink-muted);
}

.crumb {
  font-size: 13px;
  color: var(--ink-muted);
}

.role-tag {
  border-color: var(--border-strong);
  color: var(--ink-muted);
}

.user-trigger {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: var(--ink);
  font-size: 13px;
}

.user-avatar {
  background: var(--brand-primary-soft);
  color: var(--brand-primary);
  font-weight: 600;
}

.user-name {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-main {
  padding: 0;
  overflow-y: auto;
  background: var(--surface-page);
}

@media (max-width: 900px) {
  .crumb {
    display: none;
  }

  .user-name {
    display: none;
  }
}
</style>
