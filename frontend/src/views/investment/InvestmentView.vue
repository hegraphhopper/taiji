<template>
  <div class="page investment-page">
    <div class="page-heading">
      <div>
        <h1>技术创新投入态势</h1>
        <p>钱投到哪里、是否集中、重复投入风险有多大</p>
      </div>
      <div class="heading-actions">
        <el-button :loading="exporting" @click="onExport">导出 Excel</el-button>
        <el-button :loading="exportingCsv" @click="onExportCsv">导出 CSV</el-button>
        <el-button text @click="drillVisible = true">项目明细</el-button>
      </div>
    </div>

    <section class="card filter-bar">
      <el-select v-model="startYear" class="w-120" placeholder="开始年度" clearable>
        <el-option v-for="year in yearOptions" :key="year" :label="`${year} 年`" :value="year" />
      </el-select>
      <span class="dash">—</span>
      <el-select v-model="endYear" class="w-120" placeholder="结束年度" clearable>
        <el-option v-for="year in yearOptions" :key="year" :label="`${year} 年`" :value="year" />
      </el-select>
      <el-select v-model="groupId" class="w-180" placeholder="全部项目组" clearable :disabled="groupLocked">
        <el-option v-for="group in groups" :key="group.id" :label="group.name" :value="group.id" />
      </el-select>
      <el-select v-model="techDirectionId" class="w-160" placeholder="全部技术方向" clearable>
        <el-option v-for="item in dict.techDirections" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-select v-model="fundType" class="w-140" placeholder="全部基金" clearable>
        <el-option v-for="item in dict.fundTypes" :key="item.code" :label="item.name" :value="item.code" />
      </el-select>
      <el-switch v-model="includeCompleted" active-text="含结项" />
      <el-button :loading="loading" type="primary" plain @click="loadAll">刷新</el-button>
    </section>

    <el-empty
      v-if="!loading && overview && !overview.hasData"
      description="所选条件下暂无纳入统计的项目（仅统计已立项、在研、结项项目）"
      :image-size="96"
    />

    <template v-else-if="overview">
      <section class="kpi-grid">
        <article class="card kpi-card">
          <span class="kpi-label">投入总额</span>
          <strong class="kpi-value">{{ wan(overview.totalBudget) }}</strong>
          <span class="kpi-hint">同比 {{ ratio(overview.yoyBudgetRatio) }}</span>
        </article>
        <article class="card kpi-card">
          <span class="kpi-label">项目数</span>
          <strong class="kpi-value">{{ overview.projectCount }}</strong>
          <span class="kpi-hint">在研 {{ overview.inProgressCount }} · 结项 {{ overview.completedCount }} · 同比 {{ ratio(overview.yoyCountRatio) }}</span>
        </article>
        <article class="card kpi-card">
          <span class="kpi-label">平均预算</span>
          <strong class="kpi-value">{{ wan(overview.avgBudget) }}</strong>
          <span class="kpi-hint">按纳入项目计算</span>
        </article>
        <article class="card kpi-card">
          <span class="kpi-label">平均最高重复度</span>
          <strong class="kpi-value">{{ score(overview.avgMaxScore) }}</strong>
          <span class="kpi-hint">已分析 {{ overview.analyzedCount }} · 未分析 {{ overview.unanalyzedCount }}（不计入分母）</span>
        </article>
        <article class="card kpi-card">
          <span class="kpi-label">高风险项目</span>
          <strong class="kpi-value">{{ overview.highRiskCount }}</strong>
          <span class="kpi-hint">最高分 ≥ {{ overview.thresholds.high ?? 80 }}</span>
        </article>
        <article class="card kpi-card">
          <span class="kpi-label">重复投入风险敞口</span>
          <strong class="kpi-value">{{ wan(overview.exposure) }}</strong>
          <span class="kpi-hint">高风险敞口 {{ wan(overview.highExposure) }}</span>
        </article>
      </section>

      <section class="card section-card">
        <header class="section-head">
          <div>
            <h2>年度投入与重复度</h2>
            <p>柱：投入额（万元）；线：平均最高重复度</p>
          </div>
        </header>
        <InvestmentYearChart :items="byYear" />
      </section>

      <div class="two-column">
        <section class="card section-card">
          <header class="section-head">
            <div>
              <h2>基金类型分布</h2>
              <p>按投入额占比</p>
            </div>
          </header>
          <InvestmentFundPieChart :items="byFundType" />
        </section>

        <section class="card section-card">
          <header class="section-head">
            <div>
              <h2>项目组投入对比</h2>
              <p>按投入额倒序，单位万元</p>
            </div>
          </header>
          <InvestmentBarChart :items="byGroup" metric="budget" />
        </section>
      </div>

      <div class="two-column">
        <section class="card section-card">
          <header class="section-head">
            <div>
              <h2>技术方向 Top 10</h2>
              <p>按投入额倒序</p>
            </div>
          </header>
          <InvestmentBarChart :items="byDirection" metric="budget" color="#00c2b8" />
        </section>

        <section class="card section-card">
          <header class="section-head">
            <div>
              <h2>预算 - 重复度散点</h2>
              <p>气泡大小按预算；颜色为风险等级</p>
            </div>
          </header>
          <InvestmentScatterChart :items="projects" />
        </section>
      </div>

      <section class="card section-card">
        <header class="section-head">
          <div>
            <h2>Top 10 风险敞口项目</h2>
            <p>敞口 = 预算 × 最高重复度 / 100</p>
          </div>
        </header>
        <el-table :data="topRisk" size="small" border @row-click="openProject">
          <el-table-column prop="name" label="项目名称" min-width="220" />
          <el-table-column prop="groupName" label="项目组" width="150" />
          <el-table-column label="预算（万元）" width="120" align="right">
            <template #default="{ row }">{{ wan(row.budget) }}</template>
          </el-table-column>
          <el-table-column prop="maxScore" label="最高分" width="90" align="right" />
          <el-table-column label="等级" width="100">
            <template #default="{ row }">
              <el-tag size="small" effect="light" :type="levelType(row.riskLevel)">{{ row.riskLevel }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="敞口（万元）" width="130" align="right">
            <template #default="{ row }">{{ wan(row.exposure) }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="110" />
        </el-table>
      </section>

      <p class="metric-note">{{ overview.note }}</p>
    </template>

    <el-drawer v-model="drillVisible" title="投入项目明细" size="min(920px, 94vw)">
      <el-table :data="projects" size="small" border max-height="640" @row-click="openProject">
        <el-table-column prop="name" label="项目名称" min-width="220" />
        <el-table-column prop="groupName" label="项目组" width="150" />
        <el-table-column prop="techDirectionName" label="技术方向" width="120" />
        <el-table-column prop="year" label="年度" width="80" align="right" />
        <el-table-column label="预算（万元）" width="120" align="right">
          <template #default="{ row }">{{ wan(row.budget) }}</template>
        </el-table-column>
        <el-table-column label="最高分" width="90" align="right">
          <template #default="{ row }">{{ row.maxScore === null ? '未分析' : row.maxScore }}</template>
        </el-table-column>
        <el-table-column label="敞口（万元）" width="120" align="right">
          <template #default="{ row }">{{ wan(row.exposure) }}</template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { onBeforeRouteUpdate, useRoute, useRouter, type LocationQuery } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  exportInvestmentCsv,
  exportInvestmentExcel,
  getInvestmentByDirection,
  getInvestmentByFundType,
  getInvestmentByGroup,
  getInvestmentByYear,
  getInvestmentOverview,
  getInvestmentProjects,
  getInvestmentTopRisk
} from '@/api/investment'
import { getGroupsApi } from '@/api/system'
import { useAuthStore } from '@/stores/auth'
import { useDictStore } from '@/stores/dict'
import type {
  DistributionItem,
  GroupItem,
  InvestmentOverview,
  InvestmentProject,
  InvestmentQueryParams
} from '@/types'
import InvestmentYearChart from '@/components/investment/InvestmentYearChart.vue'
import InvestmentFundPieChart from '@/components/investment/InvestmentFundPieChart.vue'
import InvestmentBarChart from '@/components/investment/InvestmentBarChart.vue'
import InvestmentScatterChart from '@/components/investment/InvestmentScatterChart.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const dict = useDictStore()

const groups = ref<GroupItem[]>([])
const groupLocked = computed(() => auth.hasRole('GROUP_USER') && !auth.hasRole('ADMIN', 'PMO'))
const currentYear = new Date().getFullYear()
const yearOptions = Array.from({ length: 8 }, (_, index) => currentYear + 1 - index)

const startYear = ref<number | null>(null)
const endYear = ref<number | null>(null)
const groupId = ref<number | null>(null)
const techDirectionId = ref<number | null>(null)
const fundType = ref<string | null>(null)
const includeCompleted = ref(true)

const overview = ref<InvestmentOverview | null>(null)
const byYear = ref<DistributionItem[]>([])
const byFundType = ref<DistributionItem[]>([])
const byGroup = ref<DistributionItem[]>([])
const byDirection = ref<DistributionItem[]>([])
const topRisk = ref<InvestmentProject[]>([])
const projects = ref<InvestmentProject[]>([])
const loading = ref(false)
const exporting = ref(false)
const exportingCsv = ref(false)
const drillVisible = ref(false)
const ready = ref(false)

const queryParams = computed<InvestmentQueryParams>(() => ({
  startYear: startYear.value ?? undefined,
  endYear: endYear.value ?? undefined,
  groupId: groupId.value ?? undefined,
  techDirectionId: techDirectionId.value ?? undefined,
  fundType: fundType.value ?? undefined,
  includeCompleted: includeCompleted.value
}))

function wan(value: number | null | undefined): string {
  if (value === null || value === undefined) {
    return '—'
  }
  return `${(Number(value) / 10000).toFixed(2)} 万`
}

function score(value: number | null | undefined): string {
  return value === null || value === undefined ? '—' : Number(value).toFixed(2)
}

function ratio(value: number | null | undefined): string {
  if (value === null || value === undefined) {
    return '—'
  }
  const percent = Number(value) * 100
  return `${percent > 0 ? '+' : ''}${percent.toFixed(1)}%`
}

function levelType(level: string): 'danger' | 'warning' | 'info' | 'success' {
  if (level === 'HIGH') return 'danger'
  if (level === 'MEDIUM') return 'warning'
  if (level === 'LOW') return 'info'
  return 'success'
}

function openProject(row: InvestmentProject): void {
  drillVisible.value = false
  void router.push(`/projects/${row.projectId}`)
}

function readQuery(source?: LocationQuery): void {
  const query = source ?? route.query
  startYear.value = query.startYear ? Number(query.startYear) : null
  endYear.value = query.endYear ? Number(query.endYear) : null
  groupId.value = query.groupId ? Number(query.groupId) : null
  techDirectionId.value = query.techDirectionId ? Number(query.techDirectionId) : null
  fundType.value = typeof query.fundType === 'string' && query.fundType ? query.fundType : null
  if (query.includeCompleted === 'false') {
    includeCompleted.value = false
  } else if (query.includeCompleted === 'true') {
    includeCompleted.value = true
  }
}

function syncQuery(): void {
  if (!ready.value) {
    return
  }
  void router.replace({
    query: {
      startYear: startYear.value ? String(startYear.value) : undefined,
      endYear: endYear.value ? String(endYear.value) : undefined,
      groupId: groupId.value ? String(groupId.value) : undefined,
      techDirectionId: techDirectionId.value ? String(techDirectionId.value) : undefined,
      fundType: fundType.value || undefined,
      includeCompleted: String(includeCompleted.value)
    }
  })
}

async function loadAll(): Promise<void> {
  loading.value = true
  try {
    const params = queryParams.value
    const [overviewData, yearData, fundData, groupData, directionData, riskData, projectData] =
      await Promise.all([
        getInvestmentOverview(params),
        getInvestmentByYear(params),
        getInvestmentByFundType(params),
        getInvestmentByGroup(params),
        getInvestmentByDirection(params),
        getInvestmentTopRisk(params),
        getInvestmentProjects(params)
      ])
    overview.value = overviewData
    byYear.value = yearData
    byFundType.value = fundData
    byGroup.value = groupData
    byDirection.value = directionData
    topRisk.value = riskData
    projects.value = projectData
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '投入数据加载失败')
  } finally {
    loading.value = false
  }
}

async function onExport(): Promise<void> {
  exporting.value = true
  try {
    await exportInvestmentExcel(queryParams.value)
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '导出失败')
  } finally {
    exporting.value = false
  }
}

async function onExportCsv(): Promise<void> {
  exportingCsv.value = true
  try {
    await exportInvestmentCsv(queryParams.value)
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '导出失败')
  } finally {
    exportingCsv.value = false
  }
}

watch(
  [startYear, endYear, groupId, techDirectionId, fundType, includeCompleted],
  () => {
    if (!ready.value) {
      return
    }
    syncQuery()
    void loadAll()
  }
)

onBeforeRouteUpdate((to) => {
  if (!ready.value) {
    return
  }
  readQuery(to.query)
})

onMounted(async () => {
  readQuery()
  await Promise.all([
    dict.load(),
    getGroupsApi().then((data) => {
      groups.value = data
    })
  ])
  ready.value = true
  await loadAll()
})
</script>

<style scoped>
.investment-page {
  padding-bottom: 48px;
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  margin-bottom: 16px;
}

.dash {
  color: var(--ink-faint);
}

.w-120 {
  width: 120px;
}

.w-140 {
  width: 140px;
}

.w-160 {
  width: 160px;
}

.w-180 {
  width: 180px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.kpi-card {
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.kpi-label {
  font-size: 12px;
  color: var(--ink-muted);
}

.kpi-value {
  font-size: 22px;
  font-weight: 600;
  color: var(--ink-strong);
  line-height: 1.2;
}

.kpi-hint {
  font-size: 12px;
  color: var(--ink-faint);
}

.section-card {
  padding: 16px 18px 20px;
  margin-bottom: 16px;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.section-head h2 {
  margin: 0;
  font-size: 15px;
  color: var(--ink-strong);
}

.section-head p {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--ink-muted);
}

.two-column {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.metric-note {
  margin: 4px 2px 0;
  font-size: 12px;
  color: var(--ink-faint);
}

@media (max-width: 1400px) {
  .kpi-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1100px) {
  .two-column {
    grid-template-columns: 1fr;
  }

  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
