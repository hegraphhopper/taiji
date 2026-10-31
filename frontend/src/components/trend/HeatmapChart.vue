<template>
  <div ref="el" class="heatmap-chart"></div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { EChartsCoreOption } from 'echarts/core'
import type { HeatmapResponse } from '@/types'
import { useChart } from './chart'

const props = defineProps<{ data: HeatmapResponse }>()

const option = computed<EChartsCoreOption>(() => {
  const columns = props.data.columns
  const rows = props.data.rows
  const cells = props.data.cells
  const values = cells
    .map((cell) => cell.value)
    .filter((value): value is number => value !== null && value !== undefined)
  const max = values.length ? Math.max(...values) : 100
  const data: Array<[number, number, number | string]> = []
  rows.forEach((row, rowIndex) => {
    columns.forEach((column, columnIndex) => {
      const cell = cells.find((item) => item.rowKey === row.key && item.column === column)
      data.push([columnIndex, rowIndex, cell?.value === null || cell?.value === undefined ? '-' : cell.value])
    })
  })
  return {
    grid: { left: 120, right: 24, top: 16, bottom: 48 },
    tooltip: {
      formatter: (params: unknown) => {
        const item = params as { value: [number, number, number | string] }
        const column = columns[item.value[0]]
        const row = rows[item.value[1]]
        const cell = cells.find((entry) => entry.rowKey === row?.key && entry.column === column)
        if (!row || !column) {
          return ''
        }
        if (!cell || cell.value === null) {
          return `${row.label} · ${column}<br/>无数据`
        }
        return `${row.label} · ${column}<br/>${
          props.data.metric === 'highRisk' ? '高风险项目数' : '平均最高重复度'
        }：${cell.value}${props.data.metric === 'highRisk' ? ' 个' : ' 分'}<br/>样本：${cell.sampleCount} 个项目`
      }
    },
    xAxis: {
      type: 'category',
      data: columns,
      splitArea: { show: true },
      axisLabel: { color: '#5b6472' }
    },
    yAxis: {
      type: 'category',
      data: rows.map((row) => row.label),
      splitArea: { show: true },
      axisLabel: { color: '#5b6472' }
    },
    visualMap: {
      min: 0,
      max: Math.max(1, Math.ceil(max)),
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: 0,
      inRange: { color: ['#eaf1ff', '#9dbcff', '#4f86ff', '#123ea8'] }
    },
    series: [
      {
        type: 'heatmap',
        data,
        label: { show: true, fontSize: 11 },
        emphasis: { itemStyle: { shadowBlur: 8, shadowColor: 'rgba(0,0,0,0.2)' } }
      }
    ]
  }
})

const { el } = useChart(option)
</script>

<style scoped>
.heatmap-chart {
  width: 100%;
  height: 320px;
}
</style>
