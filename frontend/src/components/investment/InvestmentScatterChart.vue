<template>
  <div ref="el" class="chart"></div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { EChartsCoreOption } from 'echarts/core'
import type { InvestmentProject } from '@/types'
import { useChart } from '@/components/trend/chart'

const props = defineProps<{ items: InvestmentProject[] }>()

const RISK_COLORS: Record<string, string> = {
  HIGH: '#e34d59',
  MEDIUM: '#f5a524',
  LOW: '#00c2b8',
  NONE: '#8b93a7',
  UNANALYZED: '#c5cad3'
}

const option = computed<EChartsCoreOption>(() => {
  const groups = ['HIGH', 'MEDIUM', 'LOW', 'NONE', 'UNANALYZED']
  const series = groups.map((level) => ({
    name: level,
    type: 'scatter',
    symbolSize: (value: number[]) => Math.max(8, Math.min(30, Math.sqrt(value[0]) * 1.6)),
    itemStyle: { color: RISK_COLORS[level], opacity: 0.8 },
    data: props.items
      .filter((item) => item.riskLevel === level && item.maxScore !== null)
      .map((item) => [Number((item.budget / 10000).toFixed(2)), Number(item.maxScore)])
  }))
  return {
    grid: { left: 56, right: 24, top: 36, bottom: 40 },
    tooltip: {
      trigger: 'item',
      formatter: (params: unknown) => {
        const point = params as { value: [number, number]; seriesName: string }
        const match = props.items.find(
          (item) => Number((item.budget / 10000).toFixed(2)) === point.value[0]
            && Number(item.maxScore) === point.value[1]
        )
        if (!match) return ''
        return `${match.name ?? ''}<br/>${match.groupName ?? ''} · ${match.techDirectionName ?? '未选方向'}<br/>`
          + `预算：${point.value[0]} 万元<br/>最高重复度：${point.value[1]}<br/>`
          + `敞口：${match.exposure === null ? '—' : (match.exposure / 10000).toFixed(2) + ' 万元'}<br/>`
          + `等级：${point.seriesName}`
      }
    },
    legend: { top: 0, left: 0, itemWidth: 10, itemHeight: 8 },
    xAxis: { type: 'value', name: '预算（万元）', splitLine: { lineStyle: { color: '#eef1f5' } } },
    yAxis: { type: 'value', name: '最高重复度', min: 0, max: 100, splitLine: { lineStyle: { color: '#eef1f5' } } },
    series
  }
})

const { el } = useChart(option)
</script>

<style scoped>
.chart {
  width: 100%;
  height: 320px;
}
</style>
