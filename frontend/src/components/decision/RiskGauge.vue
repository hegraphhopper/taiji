<template>
  <div ref="el" class="gauge"></div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { EChartsCoreOption } from 'echarts/core'
import { useChart } from '@/components/trend/chart'

const props = defineProps<{
  score: number
  level: string
}>()

const color = computed(() => {
  if (props.level === 'HIGH') return '#e5484d'
  if (props.level === 'MEDIUM') return '#f5a524'
  if (props.level === 'LOW') return '#1f5eff'
  return '#8b93a7'
})

const option = computed<EChartsCoreOption>(() => ({
  series: [
    {
      type: 'gauge',
      startAngle: 210,
      endAngle: -30,
      min: 0,
      max: 100,
      radius: '92%',
      center: ['50%', '58%'],
      progress: { show: true, width: 12, itemStyle: { color: color.value } },
      axisLine: { lineStyle: { width: 12, color: [[1, '#eef1f5']] } },
      axisTick: { show: false },
      splitLine: { length: 8, lineStyle: { width: 1, color: '#c9d1dc' } },
      axisLabel: { distance: 14, fontSize: 10, color: '#8b93a7' },
      pointer: { width: 4, length: '60%', itemStyle: { color: color.value } },
      anchor: { show: true, size: 8, itemStyle: { color: color.value } },
      detail: {
        valueAnimation: true,
        offsetCenter: [0, '35%'],
        fontSize: 26,
        fontWeight: 600,
        color: color.value,
        formatter: (value: number) => value.toFixed(1)
      },
      title: { offsetCenter: [0, '62%'], fontSize: 12, color: '#8b93a7' },
      data: [{ value: Number(props.score || 0), name: '综合风险分' }]
    }
  ]
}))

const { el } = useChart(option)
</script>

<style scoped>
.gauge {
  width: 100%;
  height: 220px;
}
</style>
