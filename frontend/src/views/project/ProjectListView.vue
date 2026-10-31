<template>
  <div class="page">
    <div class="page-heading">
      <div>
        <h1>项目管理</h1>
        <p>申报书结构化信息、节点计划与审批状态</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建项目</el-button>
    </div>

    <section class="card filters">
      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" placeholder="名称 / 编码 / 负责人" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="项目组">
          <el-select v-model="filters.groupId" placeholder="全部" clearable class="w-160">
            <el-option v-for="group in groups" :key="group.id" :label="group.name" :value="group.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="基金类型">
          <el-select v-model="filters.fundType" placeholder="全部" clearable class="w-140">
            <el-option v-for="item in dict.fundTypes" :key="item.code" :label="item.name" :value="item.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="技术方向">
          <el-select v-model="filters.techDirectionId" placeholder="全部" clearable filterable class="w-160">
            <el-option v-for="item in dict.techDirections" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" multiple collapse-tags placeholder="全部" clearable class="w-220">
            <el-option v-for="item in dict.projectStatuses" :key="item.code" :label="item.name" :value="item.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="预算">
          <div class="budget-range">
            <el-input-number v-model="filters.budgetMin" :min="0" :controls="false" placeholder="最小" />
            <span>—</span>
            <el-input-number v-model="filters.budgetMax" :min="0" :controls="false" placeholder="最大" />
          </div>
        </el-form-item>
        <el-form-item label="重复度">
          <div class="budget-range">
            <el-input-number v-model="filters.duplicateScoreMin" :min="0" :max="100" :controls="false" placeholder="最低" />
            <span>—</span>
            <el-input-number v-model="filters.duplicateScoreMax" :min="0" :max="100" :controls="false" placeholder="最高" />
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="card table-card">
      <el-table
        v-loading="loading"
        :data="rows"
        row-key="id"
        class="project-table"
        :empty-text="loading ? '加载中' : '暂无项目'"
        @row-dblclick="openDetail"
      >
        <el-table-column label="项目" min-width="260">
          <template #default="{ row }">
            <div class="project-cell">
              <button class="project-name" @click="openDetail(row)">{{ row.name }}</button>
              <span class="project-code">{{ row.code }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="groupName" label="项目组" width="140" />
        <el-table-column prop="techDirectionName" label="技术方向" width="130" />
        <el-table-column label="预算（元）" width="140" align="right">
          <template #default="{ row }">{{ formatMoney(row.budget) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTagType(row.status)" effect="light">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="重复度" width="96" align="center">
          <template #default="{ row }">
            <span v-if="row.duplicateScore !== null">{{ row.duplicateScore }}</span>
            <span v-else class="faint">—</span>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
            <el-button
              v-if="canEdit(row)"
              link
              type="primary"
              @click="openEdit(row)"
            >编辑</el-button>
            <el-button
              v-if="canSubmit(row)"
              link
              type="success"
              @click="doAction(row, 'SUBMIT', '提交')"
            >提交</el-button>
            <el-button
              v-if="canWithdraw(row)"
              link
              type="warning"
              @click="doAction(row, 'WITHDRAW', '撤回')"
            >撤回</el-button>
            <el-button
              v-if="canDelete(row)"
              link
              type="danger"
              @click="remove(row)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="load"
          @size-change="handleSizeChange"
        />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { deleteProject, getProjects, projectAction } from '@/api/project'
import { getGroupsApi } from '@/api/system'
import { useAuthStore } from '@/stores/auth'
import { useDictStore } from '@/stores/dict'
import type { GroupItem, ProjectListItem } from '@/types'
import { formatDateTime, formatMoney, statusLabel, statusTagType } from '@/utils/project'

const router = useRouter()
const auth = useAuthStore()
const dict = useDictStore()

const groups = ref<GroupItem[]>([])
const rows = ref<ProjectListItem[]>([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const size = ref(10)

const filters = reactive<{
  keyword: string
  groupId: number | null
  fundType: string
  techDirectionId: number | null
  status: string[]
  budgetMin: number | null
  budgetMax: number | null
  duplicateScoreMin: number | null
  duplicateScoreMax: number | null
}>({
  keyword: '',
  groupId: null,
  fundType: '',
  techDirectionId: null,
  status: [],
  budgetMin: null,
  budgetMax: null,
  duplicateScoreMin: null,
  duplicateScoreMax: null
})

const canCreate = computed(() => auth.hasRole('ADMIN', 'PMO', 'GROUP_USER'))

onMounted(async () => {
  await dict.load()
  groups.value = await getGroupsApi()
  await load()
})

async function load(): Promise<void> {
  loading.value = true
  try {
    const result = await getProjects({
      keyword: filters.keyword || undefined,
      groupId: filters.groupId || undefined,
      fundType: filters.fundType || undefined,
      techDirectionId: filters.techDirectionId || undefined,
      status: filters.status.length ? filters.status : undefined,
      budgetMin: filters.budgetMin ?? undefined,
      budgetMax: filters.budgetMax ?? undefined,
      duplicateScoreMin: filters.duplicateScoreMin ?? undefined,
      duplicateScoreMax: filters.duplicateScoreMax ?? undefined,
      page: page.value,
      size: size.value
    })
    rows.value = result.records
    total.value = result.total
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '项目加载失败')
  } finally {
    loading.value = false
  }
}

function search(): void {
  page.value = 1
  load()
}

function reset(): void {
  filters.keyword = ''
  filters.groupId = null
  filters.fundType = ''
  filters.techDirectionId = null
  filters.status = []
  filters.budgetMin = null
  filters.budgetMax = null
  filters.duplicateScoreMin = null
  filters.duplicateScoreMax = null
  search()
}

function handleSizeChange(): void {
  page.value = 1
  load()
}

function openCreate(): void {
  if (!canCreate.value) {
    ElMessage.warning('当前角色不能创建项目')
    return
  }
  router.push('/projects/new')
}

function openDetail(row: ProjectListItem): void {
  router.push(`/projects/${row.id}`)
}

function openEdit(row: ProjectListItem): void {
  router.push(`/projects/${row.id}/edit`)
}

function canEdit(row: ProjectListItem): boolean {
  return canCreate.value && ['DRAFT', 'REJECTED'].includes(row.status)
}

function canSubmit(row: ProjectListItem): boolean {
  return canCreate.value && ['DRAFT', 'REJECTED'].includes(row.status)
}

function canWithdraw(row: ProjectListItem): boolean {
  return canCreate.value && row.status === 'SUBMITTED'
}

function canDelete(row: ProjectListItem): boolean {
  return canCreate.value && row.status === 'DRAFT'
}

async function doAction(row: ProjectListItem, action: string, label: string): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认${label}项目“${row.name}”吗？`, '确认操作', {
      type: 'warning',
      confirmButtonText: label,
      cancelButtonText: '取消'
    })
    await projectAction(row.id, action, undefined, row.version)
    ElMessage.success(`${label}成功`)
    await load()
  } catch (exception) {
    if (exception === 'cancel' || exception === 'close') {
      return
    }
    ElMessage.error(exception instanceof Error ? exception.message : `${label}失败`)
  }
}

async function remove(row: ProjectListItem): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除草稿“${row.name}”吗？删除后不可恢复。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteProject(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (exception) {
    if (exception === 'cancel' || exception === 'close') {
      return
    }
    ElMessage.error(exception instanceof Error ? exception.message : '删除失败')
  }
}
</script>

<style scoped>
.filters {
  padding: 16px 18px 0;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 0 4px;
}

.filter-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.w-160 {
  width: 160px;
}

.w-140 {
  width: 140px;
}

.w-220 {
  width: 220px;
}

.budget-range {
  display: flex;
  align-items: center;
  gap: 8px;
}

.budget-range :deep(.el-input-number) {
  width: 96px;
}

.table-card {
  margin-top: 16px;
  padding: 8px 18px 18px;
}

.project-table {
  width: 100%;
}

.project-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.project-name {
  border: 0;
  padding: 0;
  background: transparent;
  color: var(--brand-primary);
  font-size: 13px;
  font-weight: 600;
  text-align: left;
  cursor: pointer;
}

.project-name:hover {
  text-decoration: underline;
}

.project-code {
  font-family: "JetBrains Mono", Consolas, monospace;
  font-size: 11px;
  color: var(--ink-faint);
}

.pagination {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

@media (max-width: 900px) {
  .filter-form {
    flex-direction: column;
  }

  .filter-form :deep(.el-form-item) {
    width: 100%;
  }

  .filter-form :deep(.el-input),
  .filter-form :deep(.el-select) {
    width: 100%;
  }
}
</style>
