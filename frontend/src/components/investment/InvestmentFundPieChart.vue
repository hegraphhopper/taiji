<template>
  <div ref="el" class="chart"></div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { EChartsCoreOption } from 'echarts/core'
import type { DistributionItem } from '@/types'
import { useChart } from '@/components/trend/chart'

const props = defineProps<{ items: DistributionItem[] }>()

const option = computed<EChartsCoreOption>(() => ({
  color: ['#1f5eff', '#00c2b8', '#f5a524', '#8b93a7'],
  tooltip: {
    trigger: 'item',
    formatter: (params: unknown) => {
      const item = params as { name: string; value: number; dataIndex: number }
      const row = props.items[item.dataIndex]
      return `${item.name}<br/>投入：${item.value} 万元<br/>项目数：${row?.projectCount ?? 0}`
    }
  },
  legend: { bottom: 0, left: 'center', itemWidth: 12, itemHeight: 8 },
  series: [
    {
      type: 'pie',
      radius: ['42%', '68%'],
      avoidLabelOverlap: true,
      label: { formatter: '{b}\n{d}%' },
      data: props.items.map((item) => ({
        name: item.label,
        value: Number((item.budget / 10000).toFixed(2))
      }))
    }
  ]
}))

const { el } = useChart(option)
</script>

<style scoped>
.chart {
  width: 100%;
  height: 300px;
}
</style>
