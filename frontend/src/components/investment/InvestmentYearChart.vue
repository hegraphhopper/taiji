<template>
  <div ref="el" class="chart"></div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { EChartsCoreOption } from 'echarts/core'
import type { DistributionItem } from '@/types'
import { useChart } from '@/components/trend/chart'

const props = defineProps<{ items: DistributionItem[] }>()

const option = computed<EChartsCoreOption>(() => {
  const years = props.items.map((item) => item.label)
  const budgets = props.items.map((item) => Number((item.budget / 10000).toFixed(2)))
  const scores = props.items.map((item) => Number(item.avgMaxScore))
  return {
    color: ['#1f5eff', '#f5a524'],
    grid: { left: 56, right: 48, top: 36, bottom: 28 },
    tooltip: {
      trigger: 'axis',
      formatter: (params: unknown) => {
        const rows = params as Array<{ dataIndex: number }>
        const item = props.items[rows[0]?.dataIndex ?? 0]
        if (!item) return ''
        return `${item.label}<br/>投入：${(item.budget / 10000).toFixed(2)} 万元<br/>`
          + `项目数：${item.projectCount}<br/>平均最高分：${item.avgMaxScore}<br/>`
          + `高风险项目：${item.highRiskCount}<br/>敞口：${(item.exposure / 10000).toFixed(2)} 万元`
      }
    },
    legend: { right: 8, top: 0 },
    xAxis: { type: 'category', data: years, axisLabel: { color: '#5b6472' } },
    yAxis: [
      { type: 'value', name: '万元', splitLine: { lineStyle: { color: '#eef1f5' } } },
      { type: 'value', name: '分', min: 0, max: 100, splitLine: { show: false } }
    ],
    series: [
      {
        name: '投入额（万元）',
        type: 'bar',
        barMaxWidth: 30,
        data: budgets,
        itemStyle: { borderRadius: [4, 4, 0, 0] }
      },
      {
        name: '平均最高重复度',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbolSize: 7,
        data: scores
      }
    ]
  }
})

const { el } = useChart(option)
</script>

<style scoped>
.chart {
  width: 100%;
  height: 300px;
}
</style>
