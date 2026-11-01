<template>
  <el-dialog v-model="visible" class="run-report-dialog" width="1120px" top="5vh" destroy-on-close>
    <template #header>
      <div class="report-head">
        <div>
          <h3>分析报告 <span class="faint">运行 #{{ run?.id ?? '—' }}</span></h3>
          <p class="faint">{{ runMeta }}</p>
        </div>
      </div>
    </template>

    <div v-loading="loading" class="report-content">
    <div class="report-toolbar">
      <el-checkbox v-model="onlyHigh">只看高风险</el-checkbox>
      <span class="faint">共 {{ filteredPairs.length }} 对{{ onlyHigh ? '（高风险）' : '' }}</span>
    </div>

    <el-table
      v-if="filteredPairs.length"
      :data="pagedPairs"
      size="small"
      border
      max-height="300"
      highlight-current-row
      row-key="pairId"
      @row-click="selectPair"
    >
      <el-table-column label="项目 A" min-width="200">
        <template #default="{ row }">
          <div class="project-name">{{ row.project.name }}</div>
          <div class="faint">{{ row.project.code }} · {{ row.project.groupName || '未归组' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="项目 B" min-width="200">
        <template #default="{ row }">
          <div class="project-name">{{ row.peer.name }}</div>
          <div class="faint">{{ row.peer.code }} · {{ row.peer.groupName || '未归组' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="总分" width="82" align="right">
        <template #default="{ row }">
          <strong>{{ formatScore(row.totalScore) }}</strong>
        </template>
      </el-table-column>
      <el-table-column label="等级" width="82" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="levelTagType(row.level)" effect="light">
            {{ levelLabel(row.level) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="归因" width="96" align="center">
        <template #default="{ row }">{{ directionLabel(row.direction) }}</template>
      </el-table-column>
      <el-table-column label="重复/差异点" width="104" align="center">
        <template #default="{ row }">
          <span class="point-count">{{ row.matchedPoints.length }}/{{ row.diffPoints.length }}</span>
        </template>
      </el-table-column>
    </el-table>

    <el-empty
      v-else
      description="本次运行没有可展示的重复项目对"
      :image-size="64"
    />

    <div v-if="filteredPairs.length > pageSize" class="report-pager">
      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        :total="filteredPairs.length"
        layout="total, prev, pager, next"
        size="small"
      />
    </div>

    <section v-if="selected" class="pair-detail">
      <header class="pair-head">
        <div class="brief">
          <strong>{{ selected.project.name }}</strong>
          <span class="faint">{{ selected.project.code }} · {{ selected.project.groupName || '未归组' }}</span>
        </div>
        <div class="pair-score">
          <strong>{{ formatScore(selected.totalScore) }}</strong>
          <el-tag size="small" :type="levelTagType(selected.level)" effect="light">
            {{ levelLabel(selected.level) }}
          </el-tag>
          <span class="faint">{{ directionLabel(selected.direction) }}</span>
        </div>
        <div class="brief align-right">
          <strong>{{ selected.peer.name }}</strong>
          <span class="faint">{{ selected.peer.code }} · {{ selected.peer.groupName || '未归组' }}</span>
        </div>
      </header>

      <div class="score-grid">
        <div v-for="item in scoreItems" :key="item.label" class="score-item">
          <span>{{ item.label }}</span>
          <el-progress
            :percentage="Math.min(100, Math.max(0, item.value))"
            :stroke-width="6"
            :show-text="false"
            :color="progressColor(item.value)"
          />
          <strong>{{ formatScore(item.value) }}</strong>
        </div>
      </div>

      <div class="point-grid">
        <div class="point-block">
          <h4>重复点</h4>
          <el-empty v-if="selected.matchedPoints.length === 0" description="无匹配片段" :image-size="42" />
          <div v-for="(point, index) in selected.matchedPoints" :key="`m-${index}`" class="point">
            <strong>{{ dimensionLabel(point.dimension) }}</strong>
            <span class="terms">{{ point.terms.join('、') }}</span>
            <p>{{ point.snippet }}</p>
          </div>
        </div>
        <div class="point-block">
          <h4>差异点</h4>
          <el-empty v-if="selected.diffPoints.length === 0" description="无差异片段" :image-size="42" />
          <div v-for="(point, index) in selected.diffPoints" :key="`d-${index}`" class="point">
            <strong>{{ dimensionLabel(point.dimension) }}</strong>
            <span class="terms">{{ point.terms.join('、') }}</span>
            <p>{{ point.snippet }}</p>
          </div>
        </div>
      </div>

      <div
        v-if="selected.llmOverlaps?.length || selected.llmDifferences?.length || selected.llmReason"
        class="llm-points"
      >
        <div v-if="selected.llmOverlaps?.length" class="point-block">
          <h4>大模型分析重复点</h4>
          <ul>
            <li v-for="(item, index) in selected.llmOverlaps" :key="`lo-${index}`">{{ item }}</li>
          </ul>
        </div>
        <div v-if="selected.llmDifferences?.length" class="point-block">
          <h4>大模型分析差异点</h4>
          <ul>
            <li v-for="(item, index) in selected.llmDifferences" :key="`ld-${index}`">{{ item }}</li>
          </ul>
        </div>
        <el-alert
          v-if="selected.llmReason"
          :title="`大模型补充理由：${selected.llmReason}`"
          type="info"
          :closable="false"
          show-icon
        />
      </div>

      <div class="detail-actions">
        <el-button link type="primary" @click="openProject(selected.project.id)">查看 A 项目详情</el-button>
        <el-button link type="primary" @click="openProject(selected.peer.id)">查看 B 项目详情</el-button>
      </div>
    </section>

    <div v-else-if="filteredPairs.length" class="detail-hint">点击上方任意一行查看具体重复内容</div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import type { AnalysisRun, RunReportPair } from '@/types'

const props = defineProps<{
  modelValue: boolean
  run: AnalysisRun | null
  pairs: RunReportPair[]
  loading?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const router = useRouter()
const pageSize = 5
const page = ref(1)
const onlyHigh = ref(false)
const selected = ref<RunReportPair | null>(null)

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const filteredPairs = computed(() =>
  onlyHigh.value ? props.pairs.filter((pair) => pair.level === 'HIGH') : props.pairs
)

const pagedPairs = computed(() => {
  const start = (page.value - 1) * pageSize
  return filteredPairs.value.slice(start, start + pageSize)
})

const runMeta = computed(() => {
  if (!props.run) {
    return '—'
  }
  const type = props.run.triggerType === 'FULL' ? '全量分析' : '单项目分析'
  const date = props.run.analysisDate ? props.run.analysisDate.slice(0, 10) : '—'
  return `${type} · 分析日期 ${date} · 项目 ${props.run.projectCount} 个 · 项目对 ${props.pairs.length} 对 · 耗时 ${formatDuration(props.run.durationMs)}`
})

const scoreItems = computed(() => {
  if (!selected.value) {
    return []
  }
  return [
    { label: '目标', value: selected.value.scores.goal },
    { label: '内容', value: selected.value.scores.content },
    { label: '技术', value: selected.value.scores.tech },
    { label: '节点', value: selected.value.scores.milestone },
    { label: '标签', value: selected.value.scores.tag },
    { label: '总分', value: selected.value.totalScore }
  ]
})

watch(onlyHigh, () => {
  page.value = 1
  selected.value = null
})

watch(
  () => props.modelValue,
  (value) => {
    if (value) {
      page.value = 1
      onlyHigh.value = false
      selected.value = null
    }
  }
)

function selectPair(row: RunReportPair): void {
  selected.value = row
}

function openProject(id: number): void {
  visible.value = false
  router.push(`/projects/${id}`)
}

function formatScore(value: number | null | undefined): string {
  return value === null || value === undefined ? '—' : value.toFixed(1)
}

function formatDuration(value: number | null): string {
  if (value === null || value === undefined) {
    return '—'
  }
  return value < 1000 ? `${value} ms` : `${(value / 1000).toFixed(2)} s`
}

function levelLabel(value: string): string {
  const labels: Record<string, string> = { HIGH: '高', MEDIUM: '中', LOW: '低', NONE: '无明显' }
  return labels[value] || value
}

function levelTagType(value: string): 'danger' | 'warning' | 'info' | 'success' {
  if (value === 'HIGH') return 'danger'
  if (value === 'MEDIUM') return 'warning'
  if (value === 'LOW') return 'info'
  return 'success'
}

function directionLabel(value: string): string {
  const labels: Record<string, string> = {
    SELF_GROUP: '同组',
    SIMULTANEOUS: '同期',
    A_EARLIER: 'A 在先',
    B_EARLIER: 'B 在先'
  }
  return labels[value] || value
}

function dimensionLabel(value: string): string {
  const labels: Record<string, string> = {
    goal: '目标',
    content: '研制内容',
    tech: '关键技术',
    milestone: '节点计划',
    tag: '技术标签'
  }
  return labels[value] || value
}

function progressColor(value: number): string {
  if (value >= 80) return '#c0392b'
  if (value >= 60) return '#d68910'
  return '#2e86c1'
}
</script>

<style scoped>
:global(.run-report-dialog) {
  max-width: calc(100vw - 32px);
}

:global(.run-report-dialog .el-dialog__body) {
  max-height: 72vh;
  overflow-y: auto;
}

.report-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.report-head h3 {
  font-size: 17px;
  color: var(--ink-strong);
}

.report-head p {
  margin-top: 4px;
  font-size: 12px;
}

.report-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.project-name {
  font-weight: 600;
  color: var(--ink-strong);
}

.point-count {
  font-size: 12px;
  color: var(--ink-muted);
}

.report-pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.pair-detail {
  margin-top: 14px;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--surface-muted, #fafafa);
}

.pair-head {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 14px;
  align-items: center;
}

.brief {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.brief strong {
  color: var(--ink-strong);
}

.brief.align-right {
  text-align: right;
  align-items: flex-end;
}

.pair-score {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pair-score strong {
  font-size: 22px;
  color: var(--ink-strong);
}

.score-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
  margin: 14px 0;
}

.score-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: var(--ink-muted);
}

.score-item strong {
  color: var(--ink-strong);
  font-size: 13px;
}

.point-grid,
.llm-points {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.point-block h4 {
  margin-bottom: 6px;
  font-size: 13px;
  color: var(--ink-strong);
}

.point {
  margin-bottom: 8px;
  font-size: 12px;
}

.point .terms {
  margin-left: 6px;
  color: var(--brand-primary);
}

.point p {
  margin-top: 3px;
  color: var(--ink-muted);
  line-height: 1.5;
}

.llm-points {
  margin-top: 12px;
}

.llm-points ul {
  margin: 0;
  padding-left: 18px;
  font-size: 12px;
  color: var(--ink-muted);
  line-height: 1.6;
}

.detail-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 10px;
}

.detail-hint {
  margin-top: 18px;
  text-align: center;
  font-size: 12px;
  color: var(--ink-faint);
}

@media (max-width: 900px) {
  .score-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
  .pair-head {
    grid-template-columns: 1fr;
  }
  .brief.align-right {
    text-align: left;
    align-items: flex-start;
  }
}
</style>
