<template>
  <div class="page trend-page">
    <div class="page-heading">
      <div>
        <h1>重复度趋势与归因</h1>
        <p>按月观察重复度变化，判断问题来自本组内部、他组在先、我方在先还是同期并行</p>
      </div>
      <div class="heading-actions">
        <el-button :loading="exporting" @click="onExport">导出 CSV</el-button>
        <el-button text @click="router.push('/analysis/runs')">分析运行</el-button>
      </div>
    </div>

    <section class="card filter-bar">
      <el-date-picker
        v-model="monthRange"
        type="monthrange"
        value-format="YYYY-MM"
        start-placeholder="开始月份"
        end-placeholder="结束月份"
        :clearable="false"
        class="range-picker"
      />
      <el-select v-model="granularity" class="w-110">
        <el-option label="按月" value="MONTH" />
        <el-option label="按季度" value="QUARTER" />
        <el-option label="按年" value="YEAR" />
      </el-select>
      <el-select
        v-model="groupId"
        class="w-180"
        placeholder="全部项目组"
        clearable
        :disabled="groupLocked"
      >
        <el-option v-for="group in groups" :key="group.id" :label="group.name" :value="group.id" />
      </el-select>
      <el-select v-model="techDirectionId" class="w-160" placeholder="全部技术方向" clearable>
        <el-option v-for="item in dict.techDirections" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-select v-model="fundType" class="w-140" placeholder="全部基金" clearable>
        <el-option v-for="item in dict.fundTypes" :key="item.code" :label="item.name" :value="item.code" />
      </el-select>
      <el-button :loading="loading" type="primary" plain @click="loadAll">刷新</el-button>
    </section>

    <el-empty
      v-if="!loading && trend && !trend.hasData"
      description="所选范围内暂无可分析数据，请先在「分析运行」触发一次全量分析"
      :image-size="96"
    >
      <el-button type="primary" plain @click="router.push('/analysis/runs')">去运行分析</el-button>
    </el-empty>

    <template v-else-if="trend">
      <section class="kpi-grid">
        <article class="card kpi-card">
          <span class="kpi-label">分析项目数</span>
          <strong class="kpi-value">{{ lastPoint?.analyzedProjectCount ?? 0 }}</strong>
          <span class="kpi-hint">最新时间点 {{ lastPoint?.period || '—' }}</span>
        </article>
        <article class="card kpi-card">
          <span class="kpi-label">平均最高重复度</span>
          <strong class="kpi-value">{{ score(lastPoint?.avgMaxScore) }}</strong>
          <span class="kpi-hint">
            环比 {{ momText }}{{ thresholds.high ? ` · 高风险阈值 ≥ ${thresholds.high}` : '' }}
          </span>
        </article>
        <article class="card kpi-card">
          <span class="kpi-label">高风险项目</span>
          <strong class="kpi-value">{{ lastPoint?.highRiskCount ?? 0 }}</strong>
          <span class="kpi-hint">按分析当时阈值判定</span>
        </article>
        <article class="card kpi-card">
          <span class="kpi-label">项目对数</span>
          <strong class="kpi-value">{{ lastPoint?.uniquePairCount ?? 0 }}</strong>
          <span class="kpi-hint">唯一对数；项目视角 {{ lastPoint?.pairCount ?? 0 }} 次</span>
        </article>
      </section>

      <section class="card section-card">
        <header class="section-head">
          <div>
            <h2>重复度趋势</h2>
            <p>点击折线或柱子的时间点可下钻到项目清单</p>
          </div>
        </header>
        <TrendChart :points="trend.points" @drill="openDrill" />
      </section>

      <div class="two-column">
        <section class="card section-card">
          <header class="section-head">
            <div>
              <h2>归因构成</h2>
              <p>项目视角：跨组对在双方项目下各计一次</p>
            </div>
          </header>
          <AttributionChart :points="trend.points" />
        </section>

        <section class="card section-card">
          <header class="section-head">
            <div>
              <h2>结论摘要</h2>
              <p>数字与图表同源，仅给方向</p>
            </div>
          </header>
          <ul class="summary-list">
            <li v-for="(item, index) in trend.summary" :key="index">{{ item }}</li>
          </ul>
        </section>
      </div>

      <section class="card section-card">
        <header class="section-head">
          <div>
            <h2>热力图</h2>
            <p>无数据单元格显示为空白，tooltip 可查看样本数</p>
          </div>
          <div class="heat-controls">
            <el-select v-model="heatmapDimension" class="w-180" @change="loadHeatmap">
              <el-option label="项目组 × 月份" value="GROUP_MONTH" />
              <el-option label="技术方向 × 项目组" value="DIRECTION_GROUP" />
            </el-select>
            <el-select v-model="heatmapMetric" class="w-140" @change="loadHeatmap">
              <el-option label="平均最高分" value="avg" />
              <el-option label="高风险项目数" value="highRisk" />
            </el-select>
          </div>
        </header>
        <HeatmapChart v-if="heatmap && heatmap.hasData" :data="heatmap" />
        <el-empty v-else description="当前筛选下没有热力图数据" :image-size="72" />
      </section>

      <section v-if="attribution && attribution.groups.length" class="card section-card">
        <header class="section-head">
          <div>
            <h2>项目组归因矩阵</h2>
            <p>区间内按项目组汇总的四类归因次数，用于识别跨组撞车最突出的组</p>
          </div>
        </header>
        <el-table :data="attribution.groups" size="small" border>
          <el-table-column prop="groupName" label="项目组" min-width="160" />
          <el-table-column label="本组内部" width="110" align="right">
            <template #default="{ row }">{{ row.counts.selfGroup }}</template>
          </el-table-column>
          <el-table-column label="他组在先" width="110" align="right">
            <template #default="{ row }">{{ row.counts.peerEarlier }}</template>
          </el-table-column>
          <el-table-column label="我方在先" width="110" align="right">
            <template #default="{ row }">{{ row.counts.ourEarlier }}</template>
          </el-table-column>
          <el-table-column label="同期并行" width="110" align="right">
            <template #default="{ row }">{{ row.counts.simultaneous }}</template>
          </el-table-column>
          <el-table-column prop="total" label="合计" width="90" align="right" />
        </el-table>
      </section>

      <p class="metric-note">{{ trend.note }}</p>
    </template>

    <DrillDrawer
      v-model="drillVisible"
      :period="drillPeriod"
      :query="queryParams"
      @open-project="openProject"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { onBeforeRouteUpdate, useRoute, useRouter, type LocationQuery } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  exportTrendCsv,
  getAttribution,
  getHeatmap,
  getTrend
} from '@/api/insight'
import { getGroupsApi } from '@/api/system'
import { useAuthStore } from '@/stores/auth'
import { useDictStore } from '@/stores/dict'
import type {
  GroupItem,
  TrendAttributionResponse,
  TrendGranularity,
  TrendQueryParams,
  TrendResponse,
  HeatmapResponse
} from '@/types'
import TrendChart from '@/components/trend/TrendChart.vue'
import AttributionChart from '@/components/trend/AttributionChart.vue'
import HeatmapChart from '@/components/trend/HeatmapChart.vue'
import DrillDrawer from '@/components/trend/DrillDrawer.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const dict = useDictStore()

const groups = ref<GroupItem[]>([])
const groupLocked = computed(() => auth.hasRole('GROUP_USER') && !auth.hasRole('ADMIN', 'PMO'))

const now = new Date()
const currentMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
const startDefault = monthBefore(currentMonth, 11)

const monthRange = ref<[string, string]>([startDefault, currentMonth])
const granularity = ref<TrendGranularity>('MONTH')
const groupId = ref<number | null>(null)
const techDirectionId = ref<number | null>(null)
const fundType = ref<string | null>(null)

const trend = ref<TrendResponse | null>(null)
const attribution = ref<TrendAttributionResponse | null>(null)
const heatmap = ref<HeatmapResponse | null>(null)
const heatmapDimension = ref<'GROUP_MONTH' | 'DIRECTION_GROUP'>('GROUP_MONTH')
const heatmapMetric = ref<'avg' | 'highRisk'>('avg')
const loading = ref(false)
const exporting = ref(false)
const drillVisible = ref(false)
const drillPeriod = ref('')
const ready = ref(false)

const queryParams = computed<TrendQueryParams>(() => ({
  startMonth: monthRange.value?.[0],
  endMonth: monthRange.value?.[1],
  granularity: granularity.value,
  groupId: groupId.value ?? undefined,
  techDirectionId: techDirectionId.value ?? undefined,
  fundType: fundType.value ?? undefined
}))

const lastPoint = computed(() => {
  const points = trend.value?.points ?? []
  return points.length ? points[points.length - 1] : null
})

const thresholds = computed(() => trend.value?.thresholds ?? {})

const momText = computed(() => {
  const mom = lastPoint.value?.mom
  if (!mom) {
    return '—'
  }
  const delta = Number(mom.avgMaxScoreDelta)
  if (delta > 0) {
    return `+${delta.toFixed(2)} 分`
  }
  if (delta < 0) {
    return `${delta.toFixed(2)} 分`
  }
  return '持平'
})

function score(value: number | null | undefined): string {
  if (value === null || value === undefined) {
    return '—'
  }
  return Number(value).toFixed(2)
}

function monthBefore(month: string, offset: number): string {
  const [year, value] = month.split('-').map(Number)
  const date = new Date(year, value - 1 - offset, 1)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
}

function readQuery(source?: LocationQuery): void {
  const query = source ?? route.query
  const start = typeof query.startMonth === 'string' ? query.startMonth : undefined
  const end = typeof query.endMonth === 'string' ? query.endMonth : undefined
  if (start && end && (monthRange.value[0] !== start || monthRange.value[1] !== end)) {
    monthRange.value = [start, end]
  }
  if (query.granularity === 'MONTH' || query.granularity === 'QUARTER' || query.granularity === 'YEAR') {
    granularity.value = query.granularity
  }
  const nextGroup = query.groupId ? Number(query.groupId) : null
  if (nextGroup !== groupId.value) {
    groupId.value = nextGroup
  }
  const nextDirection = query.techDirectionId ? Number(query.techDirectionId) : null
  if (nextDirection !== techDirectionId.value) {
    techDirectionId.value = nextDirection
  }
  const nextFund = typeof query.fundType === 'string' && query.fundType ? query.fundType : null
  if (nextFund !== fundType.value) {
    fundType.value = nextFund
  }
  if (query.dimension === 'DIRECTION_GROUP') {
    heatmapDimension.value = 'DIRECTION_GROUP'
  }
  if (typeof query.period === 'string' && query.period) {
    drillPeriod.value = query.period
    drillVisible.value = true
  }
}

function syncQuery(): void {
  if (!ready.value) {
    return
  }
  void router.replace({
    query: {
      startMonth: monthRange.value[0],
      endMonth: monthRange.value[1],
      granularity: granularity.value,
      groupId: groupId.value ? String(groupId.value) : undefined,
      techDirectionId: techDirectionId.value ? String(techDirectionId.value) : undefined,
      fundType: fundType.value || undefined,
      dimension: heatmapDimension.value
    }
  })
}

async function loadAll(): Promise<void> {
  if (!monthRange.value?.[0] || !monthRange.value?.[1]) {
    return
  }
  loading.value = true
  try {
    const params = queryParams.value
    const [trendData, attributionData] = await Promise.all([
      getTrend(params),
      getAttribution(params)
    ])
    trend.value = trendData
    attribution.value = attributionData
    await loadHeatmap()
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '趋势数据加载失败')
  } finally {
    loading.value = false
  }
}

async function loadHeatmap(): Promise<void> {
  try {
    heatmap.value = await getHeatmap({
      ...queryParams.value,
      dimension: heatmapDimension.value,
      metric: heatmapMetric.value
    })
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '热力图加载失败')
  }
}

async function onExport(): Promise<void> {
  exporting.value = true
  try {
    await exportTrendCsv(queryParams.value)
  } catch (exception) {
    ElMessage.error(exception instanceof Error ? exception.message : '导出失败')
  } finally {
    exporting.value = false
  }
}

function openDrill(period: string): void {
  drillPeriod.value = period
  drillVisible.value = true
  void router.replace({ query: { ...route.query, period } })
}

watch(drillVisible, (visible) => {
  if (!visible && route.query.period) {
    const query = { ...route.query }
    delete query.period
    void router.replace({ query })
  }
})

function openProject(projectId: number): void {
  drillVisible.value = false
  void router.push(`/projects/${projectId}`)
}

watch(
  [monthRange, granularity, groupId, techDirectionId, fundType],
  () => {
    if (!ready.value) {
      return
    }
    syncQuery()
    void loadAll()
  },
  { deep: true }
)

watch(heatmapDimension, () => {
  syncQuery()
  void loadHeatmap()
})

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
.trend-page {
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

.range-picker {
  width: 260px;
}

.w-110 {
  width: 110px;
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
  grid-template-columns: repeat(4, minmax(0, 1fr));
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
  font-size: 26px;
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
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr);
  gap: 16px;
}

.summary-list {
  margin: 8px 0 0;
  padding-left: 18px;
  color: var(--ink-body);
  line-height: 1.8;
}

.heat-controls {
  display: flex;
  gap: 8px;
}

.metric-note {
  margin: 4px 2px 0;
  font-size: 12px;
  color: var(--ink-faint);
}

@media (max-width: 1200px) {
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .two-column {
    grid-template-columns: 1fr;
  }
}
</style>
