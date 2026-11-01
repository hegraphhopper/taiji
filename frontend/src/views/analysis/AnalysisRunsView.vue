<template>
  <div class="page analysis-runs">
    <div class="page-heading">
      <div>
        <h1>分析运行</h1>
        <p>触发全量/单项目重复度分析，查看运行历史与失败原因。</p>
      </div>
      <div class="heading-actions">
        <el-tag effect="plain" class="env-tag">本地算法 · 可选大模型融合</el-tag>
      </div>
    </div>

    <section class="card card-pad trigger-card">
      <header class="section-head">
        <h2>触发分析</h2>
        <p>全量分析覆盖全部非草稿项目；单项目分析只更新目标项目的结果与快照。</p>
      </header>
      <div class="trigger-row">
        <el-radio-group v-model="triggerType">
          <el-radio-button label="FULL">全量运行</el-radio-button>
          <el-radio-button label="PROJECT">单项目</el-radio-button>
        </el-radio-group>
        <el-select
          v-if="triggerType === 'PROJECT'"
          v-model="projectId"
          filterable
          clearable
          placeholder="选择项目"
          class="project-select"
        >
          <el-option
            v-for="item in projectOptions"
            :key="item.id"
            :label="`${item.name}（${item.code}）`"
            :value="item.id"
          />
        </el-select>
        <el-date-picker
          v-if="isAdmin"
          v-model="analysisDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="分析日期（可选，仅管理员）"
          class="date-picker"
        />
        <el-button type="primary" :loading="triggering" :icon="VideoPlay" @click="trigger">
          开始分析
        </el-button>
      </div>
      <p v-if="isAdmin" class="form-hint">
        指定分析日期用于历史回填：仅统计该日期前已提交的项目，快照日期取该日期。
      </p>
    </section>

    <section class="card card-pad list-card">
      <header class="section-head list-head">
        <div>
          <h2>运行历史</h2>
          <p>共 {{ total }} 条记录</p>
        </div>
        <div class="list-actions">
          <el-select v-model="filterType" clearable placeholder="类型" class="filter-select" @change="reload">
            <el-option label="全量" value="FULL" />
            <el-option label="单项目" value="PROJECT" />
            <el-option label="预检" value="PRECHECK" />
          </el-select>
          <el-select v-model="filterStatus" clearable placeholder="状态" class="filter-select" @change="reload">
            <el-option label="进行中" value="RUNNING" />
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAILED" />
          </el-select>
          <el-button :icon="Refresh" :loading="loading" @click="reload">刷新</el-button>
        </div>
      </header>

      <el-table :data="runs" size="small" border>
        <el-table-column prop="id" label="#" width="70" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">{{ triggerLabel(row.triggerType) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTagType(row.status)" effect="light">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="项目" width="200">
          <template #default="{ row }">
            <span v-if="row.triggerType === 'PROJECT'">{{ projectLabel(row.projectId) }}</span>
            <span v-else class="faint">全部非草稿项目</span>
          </template>
        </el-table-column>
        <el-table-column prop="projectCount" label="项目数" width="90" align="center" />
        <el-table-column prop="pairCount" label="对数" width="90" align="center" />
        <el-table-column label="耗时" width="100" align="right">
          <template #default="{ row }">{{ formatDuration(row.durationMs) }}</template>
        </el-table-column>
        <el-table-column label="大模型" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.llmUsed ? 'success' : 'info'" effect="plain">
              {{ row.llmUsed ? '已用' : '未用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="configVersion" label="配置版本" width="100" align="center" />
        <el-table-column prop="createdAt" label="触发时间" width="170" />
        <el-table-column label="备注" min-width="180">
          <template #default="{ row }">
            <span v-if="row.message" class="error-text">{{ row.message }}</span>
            <span v-else class="faint">—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canReport(row)" link type="primary" @click="openReport(row)">
              查看报告
            </el-button>
            <el-button
              v-if="isAdmin && row.status === 'FAILED'"
              link
              type="danger"
              @click="remove(row)"
            >
              删除
            </el-button>
            <span v-if="!canReport(row) && !(isAdmin && row.status === 'FAILED')" class="faint">—</span>
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
          @size-change="reload"
          @current-change="reload"
        />
      </div>
    </section>

    <RunReportDialog v-model="reportVisible" :run="reportRun" :pairs="reportPairs" :loading="reportLoading" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, VideoPlay } from '@element-plus/icons-vue'
import {
  deleteAnalysisRun,
  getAnalysisRuns,
  getAnalysisRunPairs,
  triggerAnalysisRun
} from '@/api/analysis'
import { getProjects } from '@/api/project'
import RunReportDialog from '@/components/analysis/RunReportDialog.vue'
import { useAuthStore } from '@/stores/auth'
import type { AnalysisRun, AnalysisTriggerType, ProjectListItem, RunReportPair } from '@/types'

const auth = useAuthStore()
const isAdmin = computed(() => auth.hasRole('ADMIN'))

const runs = ref<AnalysisRun[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)

const triggerType = ref<AnalysisTriggerType>('FULL')
const projectId = ref<number | null>(null)
const analysisDate = ref<string | null>(null)
const triggering = ref(false)

const filterType = ref('')
const filterStatus = ref('')

const projectOptions = ref<ProjectListItem[]>([])

const reportVisible = ref(false)
const reportRun = ref<AnalysisRun | null>(null)
const reportPairs = ref<RunReportPair[]>([])
const reportLoading = ref(false)

onMounted(() => {
  loadProjects()
  reload()
})

async function loadProjects(): Promise<void> {
  try {
    const result = await getProjects({ page: 1, size: 100, sortBy: 'updatedAt', sortOrder: 'desc' })
    projectOptions.value = result.records
  } catch {
    projectOptions.value = []
  }
}

async function reload(): Promise<void> {
  loading.value = true
  try {
    const result = await getAnalysisRuns({
      page: page.value,
      size: size.value,
      triggerType: filterType.value || undefined,
      status: filterStatus.value || undefined
    })
    runs.value = result.records
    total.value = result.total
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '运行历史加载失败')
  } finally {
    loading.value = false
  }
}

async function trigger(): Promise<void> {
  if (triggering.value) {
    return
  }
  if (triggerType.value === 'PROJECT' && !projectId.value) {
    ElMessage.warning('请选择要分析的项目')
    return
  }
  triggering.value = true
  try {
    const run = await triggerAnalysisRun({
      triggerType: triggerType.value,
      projectId: triggerType.value === 'PROJECT' ? projectId.value : null,
      analysisDate: isAdmin.value ? analysisDate.value || null : null
    })
    ElMessage.success(
      `分析完成：项目 ${run.projectCount} 个，项目对 ${run.pairCount} 条，耗时 ${formatDuration(run.durationMs)}`
    )
    page.value = 1
    await reload()
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '分析运行失败')
  } finally {
    triggering.value = false
  }
}

async function remove(run: AnalysisRun): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除失败运行 #${run.id} 及其明细吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await deleteAnalysisRun(run.id)
    ElMessage.success('已删除')
    await reload()
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '删除失败')
  }
}

function canReport(run: AnalysisRun): boolean {
  return run.status === 'SUCCESS' && run.triggerType !== 'PRECHECK'
}

async function openReport(run: AnalysisRun): Promise<void> {
  reportRun.value = run
  reportPairs.value = []
  reportVisible.value = true
  reportLoading.value = true
  try {
    reportPairs.value = await getAnalysisRunPairs(run.id)
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '分析报告加载失败')
  } finally {
    reportLoading.value = false
  }
}

function triggerLabel(value: string): string {
  const labels: Record<string, string> = {
    FULL: '全量',
    PROJECT: '单项目',
    PRECHECK: '预检'
  }
  return labels[value] || value
}

function statusLabel(value: string): string {
  const labels: Record<string, string> = {
    RUNNING: '进行中',
    SUCCESS: '成功',
    FAILED: '失败'
  }
  return labels[value] || value
}

function statusTagType(value: string): 'success' | 'danger' | 'warning' {
  if (value === 'SUCCESS') return 'success'
  if (value === 'FAILED') return 'danger'
  return 'warning'
}

function projectLabel(id: number | null): string {
  if (!id) return '—'
  const project = projectOptions.value.find((item) => item.id === id)
  return project ? project.name : `#${id}`
}

function formatDuration(value: number | null): string {
  if (value === null || value === undefined) {
    return '—'
  }
  if (value < 1000) {
    return `${value} ms`
  }
  return `${(value / 1000).toFixed(2)} s`
}
</script>

<style scoped>
.heading-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.env-tag {
  border-color: var(--border-strong);
  color: var(--ink-muted);
}

.section-head {
  margin-bottom: 14px;
}

.section-head h2 {
  font-size: 15px;
  font-weight: 600;
  color: var(--ink-strong);
}

.section-head p {
  margin-top: 4px;
  font-size: 12px;
  color: var(--ink-muted);
}

.trigger-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.project-select {
  width: 280px;
}

.date-picker {
  width: 220px;
}

.form-hint {
  margin-top: 10px;
  font-size: 12px;
  color: var(--ink-faint);
}

.list-card {
  margin-top: 16px;
}

.list-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.list-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-select {
  width: 120px;
}

.error-text {
  color: var(--brand-danger);
  font-size: 12px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
</style>
