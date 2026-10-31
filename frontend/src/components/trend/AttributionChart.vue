<template>
  <div ref="el" class="attribution-chart"></div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { EChartsCoreOption } from 'echarts/core'
import type { TrendPoint } from '@/types'
import { ATTRIBUTION_LABELS, TREND_COLORS, useChart } from './chart'

const props = defineProps<{ points: TrendPoint[] }>()

const option = computed<EChartsCoreOption>(() => {
  const periods = props.points.map((point) => point.period)
  const keys = ['selfGroup', 'peerEarlier', 'ourEarlier', 'simultaneous'] as const
  return {
    grid: { left: 44, right: 16, top: 30, bottom: 28 },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: unknown) => {
        const rows = params as Array<{ dataIndex: number; seriesName: string; value: number }>
        if (!rows.length) {
          return ''
        }
        const point = props.points[rows[0].dataIndex]
        const lines = [`<strong>${point.period}</strong>`]
        keys.forEach((key) => {
          lines.push(`${ATTRIBUTION_LABELS[key]}：${point.attribution[key]} 次（${(
            point.ratios[key] * 100
          ).toFixed(1)}%）`)
        })
        lines.push(`合计：${point.pairCount} 次（项目视角）`)
        return lines.join('<br/>')
      }
    },
    legend: { top: 0, left: 0, itemWidth: 12, itemHeight: 8 },
    xAxis: { type: 'category', data: periods },
    yAxis: {
      type: 'value',
      max: 100,
      axisLabel: { formatter: '{value}%' },
      splitLine: { lineStyle: { color: '#eef1f5' } }
    },
    series: keys.map((key) => ({
      name: ATTRIBUTION_LABELS[key],
      type: 'bar',
      stack: 'attribution',
      barMaxWidth: 36,
      data: props.points.map((point) => Number((point.ratios[key] * 100).toFixed(2))),
      itemStyle: { color: TREND_COLORS[key] }
    }))
  }
})

const { el } = useChart(option)
</script>

<style scoped>
.attribution-chart {
  width: 100%;
  height: 300px;
}
</style>
