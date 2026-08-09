<template>
  <section class="wb-panel">
    <header class="wb-panel-head">
      <h3 class="wb-panel-title">项目风险趋势</h3>
      <span v-if="trend" class="wb-panel-extra">近 {{ trend.days }} 天 · 按审查完成日期</span>
    </header>
    <div v-if="error" class="wb-block-state">
      <span>趋势加载失败</span>
      <el-button link type="primary" size="small" @click="$emit('retry')">重试</el-button>
    </div>
    <div v-else class="wb-trend-body">
      <div ref="chartRef" class="wb-trend-chart" />
      <div v-if="isEmpty" class="wb-trend-empty">近 {{ trend?.days || 14 }} 天暂无审查记录</div>
    </div>
  </section>
</template>

<script setup>
import echarts from '@/utils/echarts'
import useSettingsStore from '@/store/modules/settings'
import { CHART_PALETTE, CONCLUSION_META, CONCLUSION_ORDER } from '../constants'

const props = defineProps({
  trend: { type: Object, default: null },
  loading: { type: Boolean, default: false },
  error: { type: Boolean, default: false }
})

defineEmits(['retry'])

const settingsStore = useSettingsStore()
const chartRef = ref(null)
let chart = null
let resizeObserver = null

const isEmpty = computed(() => {
  if (props.loading || props.error || !props.trend) return false
  const pts = props.trend.points || []
  return pts.every(p => !p.pass && !p.warn && !p.block && !p.failed)
})

function cssVar(name) {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim()
}

function buildOption() {
  const palette = settingsStore.isDark ? CHART_PALETTE.dark : CHART_PALETTE.light
  const pts = props.trend?.points || []
  const dates = pts.map(p => String(p.date).slice(5))
  const series = CONCLUSION_ORDER.map(key => ({
    name: CONCLUSION_META[key].label,
    type: 'bar',
    stack: 'conclusion',
    barMaxWidth: 14,
    itemStyle: {
      color: palette[key],
      borderColor: palette.surface,
      borderWidth: 2,
      borderRadius: [3, 3, 0, 0]
    },
    data: pts.map(p => p[key.toLowerCase()])
  }))
  return {
    animationDuration: 300,
    grid: { top: 36, left: 8, right: 8, bottom: 0, containLabel: true },
    legend: {
      top: 0,
      left: 0,
      icon: 'rect',
      itemWidth: 10,
      itemHeight: 10,
      itemGap: 16,
      textStyle: { color: cssVar('--text-secondary'), fontSize: 12 }
    },
    tooltip: {
      trigger: 'axis',
      confine: true,
      position(point, params, dom, rect, size) {
        const [viewWidth, viewHeight] = size.viewSize
        const [contentWidth, contentHeight] = size.contentSize
        const gap = 12
        const preferLeft = point[0] > viewWidth / 2
        let x = preferLeft ? point[0] - contentWidth - gap : point[0] + gap
        let y = point[1] - contentHeight - gap
        if (x < 8) x = point[0] + gap
        if (x + contentWidth > viewWidth - 8) x = point[0] - contentWidth - gap
        if (y < 36) y = point[1] + gap
        return [
          Math.max(8, Math.min(x, viewWidth - contentWidth - 8)),
          Math.max(8, Math.min(y, viewHeight - contentHeight - 8))
        ]
      },
      axisPointer: {
        type: 'line',
        lineStyle: { color: cssVar('--text-assist'), width: 1, type: 'dashed' }
      },
      backgroundColor: cssVar('--neutral-overlay'),
      borderColor: cssVar('--border-light'),
      borderWidth: 1,
      textStyle: { color: cssVar('--text-regular'), fontSize: 12 },
      formatter(params) {
        const idx = params?.[0]?.dataIndex ?? 0
        const point = pts[idx] || {}
        const lines = [String(point.date || '')]
        params.forEach(p => lines.push(`${p.marker} ${p.seriesName}：${p.value}`))
        if (point.failed > 0) lines.push(`执行失败：${point.failed} 条`)
        return lines.join('<br/>')
      }
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: cssVar('--divider') } },
      axisTick: { show: false },
      axisLabel: { color: cssVar('--text-assist'), fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: cssVar('--divider') } },
      axisLabel: { color: cssVar('--text-assist'), fontSize: 11 }
    },
    series
  }
}

function renderChart() {
  if (!chartRef.value || props.error) return
  if (!chart) {
    chart = echarts.init(chartRef.value)
  }
  chart.setOption(buildOption(), true)
}

onMounted(() => {
  renderChart()
  if (chartRef.value) {
    resizeObserver = new ResizeObserver(() => chart && chart.resize())
    resizeObserver.observe(chartRef.value)
  }
})

watch(() => [props.trend, settingsStore.isDark], () => renderChart(), { deep: true })

onActivated(() => {
  if (chart) chart.resize()
})

onBeforeUnmount(() => {
  if (resizeObserver) resizeObserver.disconnect()
  if (chart) {
    chart.dispose()
    chart = null
  }
})
</script>

<style scoped lang="scss">
.wb-trend-body {
  position: relative;
}

.wb-trend-chart {
  width: 100%;
  height: 260px;
}

.wb-trend-empty {
  position: absolute;
  inset: 36px 0 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-placeholder);
  font-size: 13px;
  pointer-events: none;
}
</style>
