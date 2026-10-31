<template>
  <div ref="el" class="chart"></div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { EChartsCoreOption } from 'echarts/core'
import type { DistributionItem } from '@/types'
import { useChart } from '@/components/trend/chart'

const props = defineProps<{
  items: DistributionItem[]
  metric: 'budget' | 'exposure'
  color?: string
}>()

const option = computed<EChartsCoreOption>(() => {
  const sorted = [...props.items].sort((left, right) =>
    props.metric === 'budget' ? right.budget - left.budget : right.exposure - left.exposure
  )
  const labels = sorted.map((item) => item.label).reverse()
  const values = sorted
    .map((item) => Number(((props.metric === 'budget' ? item.budget : item.exposure) / 10000).toFixed(2)))
    .reverse()
  return {
    grid: { left: 110, right: 32, top: 16, bottom: 24 },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: unknown) => {
        const rows = params as Array<{ name: string; value: number }>
        return `${rows[0]?.name ?? ''}<br/>${props.metric === 'budget' ? '投入' : '风险敞口'}：`
          + `${rows[0]?.value ?? 0} 万元`
      }
    },
    xAxis: { type: 'value', name: '万元', splitLine: { lineStyle: { color: '#eef1f5' } } },
    yAxis: { type: 'category', data: labels, axisLabel: { color: '#5b6472' } },
    series: [
      {
        type: 'bar',
        barMaxWidth: 16,
        data: values,
        itemStyle: { color: props.color || '#1f5eff', borderRadius: [0, 4, 4, 0] },
        label: { show: true, position: 'right', fontSize: 11 }
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
