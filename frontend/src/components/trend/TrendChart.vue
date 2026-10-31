<template>
  <div ref="el" class="trend-chart"></div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { EChartsCoreOption } from 'echarts/core'
import type { TrendPoint } from '@/types'
import { useChart } from './chart'

const props = defineProps<{ points: TrendPoint[] }>()
const emit = defineEmits<{ (event: 'drill', period: string): void }>()

const option = computed<EChartsCoreOption>(() => {
  const periods = props.points.map((point) => point.period)
  const scores = props.points.map((point) => Number(point.avgMaxScore))
  const highRisk = props.points.map((point) => point.highRiskCount)
  return {
    color: ['#1f5eff', '#f5a524'],
    grid: { left: 44, right: 48, top: 36, bottom: 28 },
    tooltip: { trigger: 'axis' },
    legend: { data: ['平均最高重复度', '高风险项目数'], right: 8, top: 0 },
    xAxis: {
      type: 'category',
      data: periods,
      axisTick: { alignWithLabel: true },
      axisLabel: { color: '#5b6472' }
    },
    yAxis: [
      {
        type: 'value',
        name: '分',
        min: 0,
        max: 100,
        splitLine: { lineStyle: { color: '#eef1f5' } }
      },
      {
        type: 'value',
        name: '个',
        minInterval: 1,
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '平均最高重复度',
        type: 'line',
        smooth: true,
        symbolSize: 7,
        data: scores,
        areaStyle: { opacity: 0.08 },
        lineStyle: { width: 2.5 }
      },
      {
        name: '高风险项目数',
        type: 'bar',
        yAxisIndex: 1,
        barMaxWidth: 22,
        data: highRisk,
        itemStyle: { borderRadius: [4, 4, 0, 0] }
      }
    ]
  }
})

const { el, getChart } = useChart(option, (event) => {
  const chart = getChart()
  if (!chart) {
    return
  }
  if (!chart.containPixel({ gridIndex: 0 }, [event.offsetX, event.offsetY])) {
    return
  }
  const point = chart.convertFromPixel(
    { gridIndex: 0 },
    [event.offsetX, event.offsetY]
  ) as [number, number] | undefined
  if (!point) {
    return
  }
  const index = Math.round(point[0])
  if (index >= 0 && index < props.points.length) {
    emit('drill', props.points[index].period)
  }
})
</script>

<style scoped>
.trend-chart {
  width: 100%;
  height: 320px;
}
</style>
