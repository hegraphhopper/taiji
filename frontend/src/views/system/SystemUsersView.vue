<template>
  <div class="page users-page">
    <div class="page-heading">
      <div>
        <h1>用户管理</h1>
        <p>维护用户、分配角色、启用/禁用与重置密码；禁用后旧登录立即失效</p>
      </div>
      <div class="heading-actions">
        <el-button type="primary" @click="openCreate">新增用户</el-button>
      </div>
    </div>

    <section class="card filter-bar">
      <el-input v-model="filters.keyword" clearable placeholder="用户名或姓名" class="w-200" @keyup.enter="search" />
      <el-select v-model="filters.role" clearable placeholder="全部角色" class="w-150">
        <el-option v-for="role in roles" :key="role" :label="roleLabel(role)" :value="role" />
      </el-select>
      <el-select v-model="filters.status" clearable placeholder="全部状态" class="w-130">
        <el-option label="启用" value="ENABLED" />
        <el-option label="禁用" value="DISABLED" />
      </el-select>
      <el-select v-model="filters.groupId" clearable placeholder="全部项目组" class="w-180">
        <el-option v-for="group in groups" :key="group.id" :label="group.name" :value="group.id" />
      </el-select>
      <el-button type="primary" plain :loading="loading" @click="search">查询</el-button>
      <el-button text @click="resetFilters">重置</el-button>
    </section>

    <section class="card table-card">
      <el-table :data="users" size="small" border>
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="groupName" label="项目组" width="150" />
        <el-table-column label="角色" min-width="180">
          <template #default="{ row }">
            <el-tag v-for="role in row.roles" :key="role" size="small" class="role-tag">
              {{ roleLabel(role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'ENABLED' ? 'success' : 'info'">
              {{ row.status === 'ENABLED' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginAt" label="最近登录" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="resetPassword(row)">重置密码</el-button>
            <el-button link :type="row.status === 'ENABLED' ? 'danger' : 'success'"
                       @click="toggleStatus(row)">
              {{ row.status === 'ENABLED' ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="load"
          @size-change="load"
        />
      </div>
    </section>

    <el-drawer v-model="drawerVisible" :title="editing ? '编辑用户' : '新增用户'" size="520px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="editing" maxlength="50" />
        </el-form-item>
        <el-form-item :label="editing ? '新密码（留空不修改）' : '初始密码'" :prop="editing ? undefined : 'password'">
          <el-input v-model="form.password" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.realName" maxlength="50" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" maxlength="100" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="form.phone" maxlength="20" />
        </el-form-item>
        <el-form-item label="项目组">
          <el-select v-model="form.groupId" clearable class="full">
            <el-option v-for="group in groups" :key="group.id" :label="group.name" :value="group.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色" prop="roles">
          <el-select v-model="form.roles" multiple class="full">
            <el-option v-for="role in roles" :key="role" :label="roleLabel(role)" :value="role" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="ENABLED">启用</el-radio>
            <el-radio value="DISABLED">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  createUser,
  getAllGroups,
  getUsers,
  resetUserPassword,
  updateUser
} from '@/api/governance'
import type { GroupItem, SystemUser } from '@/types'

const roles = ['ADMIN', 'PMO', 'GROUP_USER', 'EXPERT']
const roleNames: Record<string, string> = {
  ADMIN: '系统管理员',
  PMO: '基金办',
  GROUP_USER: '项目组用户',
  EXPERT: '评审专家'
}

const filters = reactive<{ keyword: string; role: string | null; status: string | null; groupId: number | null }>({
  keyword: '',
  role: null,
  status: null,
  groupId: null
})
const users = ref<SystemUser[]>([])
const groups = ref<GroupItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const drawerVisible = ref(false)
const editing = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)
const form = reactive({
  username: '',
  password: '',
  realName: '',
  email: '',
  phone: '',
  groupId: null as number | null,
  roles: [] as string[],
  status: 'ENABLED'
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  roles: [{ required: true, type: 'array', min: 1, message: '至少选择一个角色', trigger: 'change' }]
}

function roleLabel(role: string): string {
  return roleNames[role] || role
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const data = await getUsers({ ...filters, page: page.value, size: size.value })
    users.value = data.records
    total.value = data.total
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '用户加载失败')
  } finally {
    loading.value = false
  }
}

function search(): void {
  page.value = 1
  void load()
}

function resetFilters(): void {
  filters.keyword = ''
  filters.role = null
  filters.status = null
  filters.groupId = null
  search()
}

function openCreate(): void {
  editing.value = false
  editingId.value = null
  Object.assign(form, {
    username: '', password: '', realName: '', email: '', phone: '',
    groupId: null, roles: [], status: 'ENABLED'
  })
  drawerVisible.value = true
}

function openEdit(row: SystemUser): void {
  editing.value = true
  editingId.value = row.id
  Object.assign(form, {
    username: row.username,
    password: '',
    realName: row.realName || '',
    email: row.email || '',
    phone: row.phone || '',
    groupId: row.groupId,
    roles: [...row.roles],
    status: row.status
  })
  drawerVisible.value = true
}

async function submit(): Promise<void> {
  if (!formRef.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  if (!editing.value && form.password.trim().length < 8) {
    ElMessage.warning('初始密码至少 8 位，且需包含字母和数字')
    return
  }
  submitting.value = true
  try {
    const payload = {
      username: form.username,
      password: form.password || undefined,
      realName: form.realName || null,
      email: form.email || null,
      phone: form.phone || null,
      groupId: form.groupId,
      status: form.status,
      roles: form.roles
    }
    if (editing.value && editingId.value) {
      await updateUser(editingId.value, payload)
    } else {
      await createUser(payload)
    }
    ElMessage.success('保存成功')
    drawerVisible.value = false
    await load()
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '保存失败')
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row: SystemUser): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定${row.status === 'ENABLED' ? '禁用' : '启用'}用户「${row.username}」吗？`,
      '状态变更', { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await updateUser(row.id, {
      username: row.username,
      realName: row.realName,
      email: row.email,
      phone: row.phone,
      groupId: row.groupId,
      status: row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED',
      roles: row.roles
    })
    ElMessage.success('已更新')
    await load()
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '更新失败')
  }
}

async function resetPassword(row: SystemUser): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定重置「${row.username}」的密码吗？`, '重置密码', { type: 'warning' })
  } catch {
    return
  }
  try {
    const result = await resetUserPassword(row.id)
    await ElMessageBox.alert(
      `临时密码：${result.temporaryPassword}\n请立即转告用户并提醒修改。`,
      '重置成功',
      { confirmButtonText: '知道了' }
    )
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '重置失败')
  }
}

onMounted(async () => {
  try {
    groups.value = await getAllGroups()
  } catch {
    groups.value = []
  }
  await load()
})
</script>

<style scoped>
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  padding: 14px 16px;
  margin-bottom: 16px;
}

.w-200 { width: 200px; }
.w-150 { width: 150px; }
.w-130 { width: 130px; }
.w-180 { width: 180px; }

.table-card {
  padding: 10px 12px 16px;
}

.role-tag {
  margin-right: 4px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
}

.full {
  width: 100%;
}
</style>
