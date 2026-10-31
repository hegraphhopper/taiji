<template>
  <div class="page records-page">
    <div class="page-heading">
      <div>
        <h1>决策记录</h1>
        <p>保存的预检快照可回看、可下载报告；记录不可编辑，仅可作废</p>
      </div>
      <div class="heading-actions">
        <el-button v-if="isAdmin" text @click="goRules">规则配置</el-button>
        <el-button text @click="goDecision">发起预检</el-button>
      </div>
    </div>

    <section class="card filter-bar">
      <el-select v-model="filters.level" placeholder="全部等级" clearable class="w-140">
        <el-option label="高风险" value="HIGH" />
        <el-option label="中风险" value="MEDIUM" />
        <el-option label="低风险" value="LOW" />
        <el-option label="无明显重复" value="NONE" />
      </el-select>
      <el-select v-model="filters.recommendation" placeholder="全部建议" clearable class="w-180">
        <el-option label="可立项" value="APPROVE" />
        <el-option label="补充差异说明" value="EXPLAIN" />
        <el-option label="建议合并" value="MERGE" />
        <el-option label="建议终止/转向" value="TERMINATE" />
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        class="w-260"
      />
      <el-button type="primary" plain :loading="loading" @click="search">查询</el-button>
      <el-button text @click="reset">重置</el-button>
    </section>

    <section class="card table-card">
      <el-table :data="records" size="small" border @row-click="openDetail">
        <el-table-column label="项目" min-width="200">
          <template #default="{ row }">{{ row.projectName || '临时文本预检' }}</template>
        </el-table-column>
        <el-table-column label="项目组" width="140">
          <template #default="{ row }">{{ row.groupName || '—' }}</template>
        </el-table-column>
        <el-table-column label="风险分" width="90" align="right">
          <template #default="{ row }">{{ Number(row.riskScore).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="等级" width="90">
          <template #default="{ row }">
            <el-tag size="small" effect="light" :type="levelType(row.level)">{{ levelLabel(row.level) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="建议" width="130">
          <template #default="{ row }">
            <el-tag size="small" :type="actionType(row.recommendation)" effect="plain">
              {{ actionLabel(row.recommendation) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="预计重复投入" width="135" align="right">
          <template #default="{ row }">{{ money(row.savingsEstimate) }}</template>
        </el-table-column>
        <el-table-column label="操作人" width="110">
          <template #default="{ row }">{{ row.operatorName || '—' }}</template>
        </el-table-column>
        <el-table-column label="时间" width="160">
          <template #default="{ row }">{{ row.createdAt || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="openDetail(row)">详情</el-button>
            <el-button link type="primary" @click.stop="download(row.id, 'docx')">Word</el-button>
            <el-button link type="primary" @click.stop="download(row.id, 'xlsx')">Excel</el-button>
            <el-button v-if="canVoid" link type="danger" @click.stop="voidRecord(row)">作废</el-button>
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

    <el-drawer v-model="drawerVisible" title="决策记录详情" size="min(860px, 94vw)">
      <template v-if="detail">
        <div class="detail-head">
          <div>
            <h3>{{ detail.projectName || '临时文本预检' }}</h3>
            <p>{{ detail.projectCode || '—' }} · {{ detail.groupName || '—' }}</p>
          </div>
          <div class="detail-tags">
            <el-tag :type="levelType(detail.level)" effect="dark">{{ levelLabel(detail.level) }}</el-tag>
            <el-tag :type="actionType(detail.recommendation)" effect="plain">{{ actionLabel(detail.recommendation) }}</el-tag>
          </div>
        </div>
        <el-descriptions :column="2" border size="small" class="detail-meta">
          <el-descriptions-item label="风险分">{{ Number(detail.riskScore).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="预计重复投入">{{ money(detail.savingsEstimate) }}</el-descriptions-item>
          <el-descriptions-item label="操作人">{{ detail.operatorName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="保存时间">{{ detail.createdAt || '—' }}</el-descriptions-item>
        </el-descriptions>
        <h4 class="detail-title">决策理由</h4>
        <p class="detail-reason">{{ detail.reason || '—' }}</p>
        <h4 class="detail-title">Top 相似项目</h4>
        <el-table :data="detail.similarProjects" size="small" border>
          <el-table-column label="项目" min-width="200">
            <template #default="{ row }">{{ row.peer?.name }}</template>
          </el-table-column>
          <el-table-column label="项目组" width="140">
            <template #default="{ row }">{{ row.peer?.groupName || '—' }}</template>
          </el-table-column>
          <el-table-column label="总分" width="90" align="right">
            <template #default="{ row }">{{ Number(row.totalScore).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="归因" width="110">
            <template #default="{ row }">{{ directionLabel(row.direction) }}</template>
          </el-table-column>
        </el-table>
        <div class="drawer-actions">
          <el-button type="primary" plain @click="download(detail.id, 'docx')">下载 Word</el-button>
          <el-button plain @click="download(detail.id, 'xlsx')">下载 Excel</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  downloadDecisionExcel,
  downloadDecisionWord,
  getDecisionRecord,
  getDecisionRecords,
  voidDecisionRecord
} from '@/api/decision'
import { useAuthStore } from '@/stores/auth'
import type { DecisionRecord, DuplicateLevel } from '@/types'

const router = useRouter()
const auth = useAuthStore()

const filters = reactive<{ level: string | null; recommendation: string | null }>({
  level: null,
  recommendation: null
})
const dateRange = ref<[string, string] | null>(null)
const records = ref<DecisionRecord[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const drawerVisible = ref(false)
const detail = ref<DecisionRecord | null>(null)

const isAdmin = computed(() => auth.hasRole('ADMIN'))
const canVoid = computed(() => auth.hasRole('ADMIN', 'PMO'))

async function load(): Promise<void> {
  loading.value = true
  try {
    const data = await getDecisionRecords({
      level: filters.level,
      recommendation: filters.recommendation,
      from: dateRange.value?.[0] || null,
      to: dateRange.value?.[1] || null,
      page: page.value,
      size: size.value
    })
    records.value = data.records
    total.value = data.total
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '决策记录加载失败')
  } finally {
    loading.value = false
  }
}

function search(): void {
  page.value = 1
  void load()
}

function reset(): void {
  filters.level = null
  filters.recommendation = null
  dateRange.value = null
  page.value = 1
  void load()
}

async function openDetail(row: DecisionRecord): Promise<void> {
  try {
    detail.value = await getDecisionRecord(row.id)
    drawerVisible.value = true
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '详情加载失败')
  }
}

async function download(id: number, type: 'docx' | 'xlsx'): Promise<void> {
  try {
    if (type === 'docx') {
      await downloadDecisionWord(id)
    } else {
      await downloadDecisionExcel(id)
    }
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '报告下载失败')
  }
}

async function voidRecord(row: DecisionRecord): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定作废「${row.projectName || '临时文本预检'}」的决策记录吗？作废后不可恢复。`,
      '作废确认',
      { type: 'warning', confirmButtonText: '作废', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await voidDecisionRecord(row.id)
    ElMessage.success('已作废')
    await load()
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '作废失败')
  }
}

function levelType(level: DuplicateLevel | string): 'danger' | 'warning' | 'info' | 'success' {
  if (level === 'HIGH') return 'danger'
  if (level === 'MEDIUM') return 'warning'
  if (level === 'LOW') return 'info'
  return 'success'
}

function levelLabel(level: DuplicateLevel | string): string {
  if (level === 'HIGH') return '高风险'
  if (level === 'MEDIUM') return '中风险'
  if (level === 'LOW') return '低风险'
  return '无明显重复'
}

function actionLabel(action: string): string {
  if (action === 'APPROVE') return '可立项'
  if (action === 'EXPLAIN') return '补充差异说明'
  if (action === 'MERGE') return '建议合并'
  if (action === 'TERMINATE') return '建议终止/转向'
  return action
}

function actionType(action: string): 'danger' | 'warning' | 'info' | 'success' {
  if (action === 'TERMINATE') return 'danger'
  if (action === 'MERGE') return 'warning'
  if (action === 'EXPLAIN') return 'info'
  return 'success'
}

function directionLabel(direction: string): string {
  if (direction === 'SELF_GROUP') return '本组内部'
  if (direction === 'PEER_EARLIER') return '他组在先'
  if (direction === 'OUR_EARLIER') return '我方在先'
  if (direction === 'SIMULTANEOUS') return '同期并行'
  return direction
}

function money(value: number | null | undefined): string {
  if (value === null || value === undefined) {
    return '—'
  }
  return `${(Number(value) / 10000).toFixed(2)} 万元`
}

function goRules(): void {
  void router.push('/decision/rules')
}

function goDecision(): void {
  void router.push('/decision')
}

onMounted(load)
</script>

<style scoped>
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  margin-bottom: 16px;
}

.w-140 {
  width: 140px;
}

.w-180 {
  width: 180px;
}

.w-260 {
  width: 260px;
}

.table-card {
  padding: 8px 12px 16px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding: 12px 4px 0;
}

.detail-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 12px;
}

.detail-head h3 {
  margin: 0;
  font-size: 16px;
  color: var(--ink-strong);
}

.detail-head p {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--ink-faint);
}

.detail-tags {
  display: flex;
  gap: 8px;
}

.detail-meta {
  margin-bottom: 16px;
}

.detail-title {
  margin: 16px 0 6px;
  font-size: 13px;
  color: var(--ink-strong);
}

.detail-reason {
  margin: 0 0 4px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ink);
}

.drawer-actions {
  display: flex;
  gap: 8px;
  margin-top: 16px;
}
</style>
