<template>
  <el-drawer
    :model-value="modelValue"
    :title="`${period || ''} 项目清单`"
    size="min(860px, 92vw)"
    @update:model-value="onUpdate"
  >
    <div class="drawer-toolbar">
      <el-select v-model="level" placeholder="全部等级" clearable class="level-select" @change="load">
        <el-option label="高风险" value="HIGH" />
        <el-option label="中风险" value="MEDIUM" />
        <el-option label="低风险" value="LOW" />
        <el-option label="无明显重复" value="NONE" />
      </el-select>
      <span class="hint">等级按当前阈值折算；趋势图指标按各次分析时阈值判定。</span>
      <el-button :loading="loading" text @click="load">刷新</el-button>
    </div>

    <el-table v-loading="loading" :data="projects" size="small" border>
      <el-table-column prop="name" label="项目名称" min-width="220">
        <template #default="{ row }">
          <el-link type="primary" @click="emit('open-project', row.projectId)">{{ row.name }}</el-link>
        </template>
      </el-table-column>
      <el-table-column prop="groupName" label="项目组" width="150" />
      <el-table-column prop="techDirectionName" label="技术方向" width="120" />
      <el-table-column prop="maxScore" label="最高分" width="90" align="right">
        <template #default="{ row }">{{ Number(row.maxScore ?? 0).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="等级" width="110">
        <template #default="{ row }">
          <el-tag size="small" effect="light" :type="levelType(row.riskLevel)">{{ row.riskLevel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="attributionTotal" label="归因数" width="90" align="right" />
    </el-table>

    <el-empty v-if="!loading && !projects.length" description="该时间点与筛选条件下没有项目" :image-size="80" />
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { getDrillProjects } from '@/api/insight'
import type { DrillProject, TrendQueryParams } from '@/types'

const props = defineProps<{
  modelValue: boolean
  period: string
  query: TrendQueryParams
}>()
const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'open-project', projectId: number): void
}>()

function onUpdate(value: boolean): void {
  emit('update:modelValue', value)
}

const level = ref<string>('')
const loading = ref(false)
const projects = ref<DrillProject[]>([])

async function load(): Promise<void> {
  if (!props.period) {
    return
  }
  loading.value = true
  try {
    projects.value = await getDrillProjects({
      ...props.query,
      period: props.period,
      level: level.value || undefined
    })
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.modelValue, props.period],
  () => {
    if (props.modelValue) {
      void load()
    }
  }
)

function levelType(levelValue: string): 'danger' | 'warning' | 'info' | 'success' {
  if (levelValue === 'HIGH') return 'danger'
  if (levelValue === 'MEDIUM') return 'warning'
  if (levelValue === 'LOW') return 'info'
  return 'success'
}
</script>

<style scoped>
.drawer-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.level-select {
  width: 150px;
}

.hint {
  flex: 1;
  font-size: 12px;
  color: var(--ink-faint);
}
</style>
