import * as echarts from 'echarts/core'
import { BarChart, HeatmapChart, LineChart } from 'echarts/charts'
import {
  GridComponent,
  LegendComponent,
  TooltipComponent,
  VisualMapComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { onBeforeUnmount, onMounted, ref, watch, type Ref } from 'vue'
import type { EChartsCoreOption } from 'echarts/core'

echarts.use([
  LineChart,
  BarChart,
  HeatmapChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  VisualMapComponent,
  CanvasRenderer
])

export const TREND_COLORS = {
  selfGroup: '#1f5eff',
  peerEarlier: '#f5a524',
  ourEarlier: '#00c2b8',
  simultaneous: '#8b93a7'
}

export const ATTRIBUTION_LABELS = {
  selfGroup: '本组内部',
  peerEarlier: '他组在先',
  ourEarlier: '我方在先',
  simultaneous: '同期并行'
}

export interface ChartClickEvent {
  offsetX: number
  offsetY: number
}

export function useChart(
  option: Ref<EChartsCoreOption>,
  onClick?: (event: ChartClickEvent) => void
) {
  const el = ref<HTMLDivElement | null>(null)
  let chart: echarts.ECharts | null = null
  let observer: ResizeObserver | null = null

  const render = (): void => {
    if (chart) {
      chart.setOption(option.value, true)
    }
  }

  onMounted(() => {
    if (!el.value) {
      return
    }
    chart = echarts.init(el.value)
    render()
    if (onClick) {
      chart.getZr().on('click', (event) => {
        onClick({ offsetX: event.offsetX, offsetY: event.offsetY })
      })
    }
    observer = new ResizeObserver(() => chart?.resize())
    observer.observe(el.value)
  })

  onBeforeUnmount(() => {
    observer?.disconnect()
    observer = null
    chart?.dispose()
    chart = null
  })

  watch(option, render, { deep: true })

  return { el, getChart: () => chart }
}
