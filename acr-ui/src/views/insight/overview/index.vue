<template>
  <div class="app-container insight-page">
    <div class="insight-toolbar">
      <el-form :inline="true" :model="query" class="insight-filters">
        <el-form-item label="时间范围">
          <el-radio-group v-model="rangePreset" @change="onPresetChange">
            <el-radio-button :value="7">近 7 天</el-radio-button>
            <el-radio-button :value="30">近 30 天</el-radio-button>
            <el-radio-button value="custom">自定义</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="rangePreset === 'custom'" label="自定义">
          <el-date-picker
            v-model="customRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始"
            end-placeholder="结束"
            :clearable="false"
          />
        </el-form-item>
        <el-form-item label="业务系统">
          <el-select v-model="query.businessSystemId" clearable filterable placeholder="全部" style="width: 200px">
            <el-option v-for="item in businessSystems" :key="item.id" :label="item.label" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="loadData">查询</el-button>
        </el-form-item>
      </el-form>
      <div class="insight-toolbar-right">
        <span v-if="data?.metricsVersion" class="version">口径 {{ data.metricsVersion }}</span>
        <el-button link type="primary" @click="dictVisible = true">指标说明</el-button>
      </div>
    </div>

    <div v-if="loading" class="insight-skeleton">
      <el-skeleton :rows="4" animated />
    </div>
    <div v-else-if="error" class="insight-state">
      <span>看板加载失败</span>
      <el-button type="primary" link @click="loadData">重试</el-button>
    </div>
    <template v-else>
      <el-alert
        v-if="data?.empty"
        type="info"
        :closable="false"
        show-icon
        class="mb16"
        :title="data.emptyReason || '暂无数据'"
        :description="emptyHint"
      />

      <el-row :gutter="16" class="kpi-row">
        <el-col v-for="card in data?.kpis || []" :key="card.code" :xs="24" :sm="12" :lg="6">
          <div class="kpi-card">
            <div class="kpi-name">{{ card.name }}</div>
            <div class="kpi-value">{{ formatKpiValue(card) }}</div>
            <div class="kpi-change">{{ formatChange(card.changeRatio) }}</div>
          </div>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="mt16">
        <el-col :xs="24" :lg="12">
          <section class="chart-panel">
            <header class="panel-head">
              <h3>审查任务趋势</h3>
              <el-button link type="primary" @click="exportChart('task')">导出 PNG</el-button>
            </header>
            <div ref="taskChartRef" class="chart-box" />
          </section>
        </el-col>
        <el-col :xs="24" :lg="12">
          <section class="chart-panel">
            <header class="panel-head">
              <h3>新增问题趋势</h3>
              <el-button link type="primary" @click="exportChart('issue')">导出 PNG</el-button>
            </header>
            <div ref="issueChartRef" class="chart-box" />
          </section>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="mt16">
        <el-col :xs="24" :lg="12">
          <section class="chart-panel">
            <header class="panel-head">
              <h3>问题类别分布</h3>
              <el-button link type="primary" @click="exportChart('category')">导出 PNG</el-button>
            </header>
            <div ref="categoryChartRef" class="chart-box" />
          </section>
        </el-col>
        <el-col :xs="24" :lg="12">
          <section class="chart-panel">
            <header class="panel-head"><h3>交付渠道健康</h3></header>
            <el-table :data="data?.deliveryHealth || []" size="small" empty-text="暂无投递数据">
              <el-table-column label="渠道" prop="channel" min-width="160" :show-overflow-tooltip="true" />
              <el-table-column label="尝试" prop="total" width="80" />
              <el-table-column label="成功" prop="success" width="80" />
              <el-table-column label="成功率" width="100">
                <template #default="scope">{{ formatRatio(scope.row.successRate) }}</template>
              </el-table-column>
            </el-table>
          </section>
        </el-col>
      </el-row>
    </template>

    <el-drawer v-model="dictVisible" title="指标说明" size="420px">
      <p class="dict-version">口径版本：{{ metricsDict?.version || data?.metricsVersion || '--' }}</p>
      <div v-for="item in metricsDict?.metrics || []" :key="item.code" class="dict-item">
        <div class="dict-name">{{ item.name }}</div>
        <div class="dict-def">{{ item.definition }}</div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="InsightOverview">
import echarts from '@/utils/echarts'
import { getReviewProjectOptions } from '@/api/review/project'
import { getInsightMetricsDict, getInsightOverview } from '@/api/insight'
import {
  formatChange,
  formatKpiValue,
  formatRatio,
  loadInsightFilters,
  saveInsightFilters,
  toIdParam
} from '../components/insightFilter'

const loading = ref(false)
const error = ref(false)
const data = ref(null)
const businessSystems = ref([])
const dictVisible = ref(false)
const metricsDict = ref(null)
const rangePreset = ref(7)
const customRange = ref([])
const query = reactive({ businessSystemId: undefined, days: 7 })

const taskChartRef = ref(null)
const issueChartRef = ref(null)
const categoryChartRef = ref(null)
const charts = { task: null, issue: null, category: null }

const emptyHint = computed(() => {
  const since = data.value?.dataSince
  return since
    ? `数据自 ${since} 起积累。可前往「项目接入 → 代码项目」检查 Webhook 与审查开关。`
    : '聚合尚未产生数据。配置定时任务 insightStatsJobTask.refreshRecent() 后重试。'
})

function onPresetChange() {
  if (rangePreset.value !== 'custom') {
    query.days = rangePreset.value
  }
}

function buildParams() {
  if (rangePreset.value === 'custom' && customRange.value?.length === 2) {
    return {
      beginDate: customRange.value[0],
      endDate: customRange.value[1],
      businessSystemId: query.businessSystemId
    }
  }
  return { days: Number(rangePreset.value) || 7, businessSystemId: query.businessSystemId }
}

async function loadOptions() {
  try {
    const res = await getReviewProjectOptions()
    businessSystems.value = res.data?.businessSystems || []
  } catch (e) {
    businessSystems.value = []
  }
}

async function loadData() {
  loading.value = true
  error.value = false
  saveInsightFilters('overview', {
    rangePreset: rangePreset.value,
    customRange: customRange.value,
    businessSystemId: query.businessSystemId
  })
  try {
    const res = await getInsightOverview(buildParams())
    data.value = res.data
    await nextTick()
    renderCharts()
  } catch (e) {
    error.value = true
  } finally {
    loading.value = false
  }
}

async function loadDict() {
  try {
    const res = await getInsightMetricsDict()
    metricsDict.value = res.data
  } catch (e) {
    /* drawer 打开时再试 */
  }
}

function cssVar(name) {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim()
}

function baseAxis() {
  return {
    xAxis: {
      type: 'category',
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
    grid: { top: 36, left: 8, right: 12, bottom: 0, containLabel: true },
    tooltip: { trigger: 'axis' }
  }
}

function renderCharts() {
  const taskPts = data.value?.taskTrend || []
  ensureChart('task', taskChartRef.value)?.setOption({
    ...baseAxis(),
    legend: { top: 0, left: 0, textStyle: { color: cssVar('--text-secondary'), fontSize: 12 } },
    xAxis: { ...baseAxis().xAxis, data: taskPts.map(p => String(p.date).slice(5)) },
    series: [
      { name: '成功', type: 'bar', stack: 't', data: taskPts.map(p => p.success), itemStyle: { color: '#16A34A' } },
      { name: '失败', type: 'bar', stack: 't', data: taskPts.map(p => p.failed), itemStyle: { color: '#94A3B8' } }
    ]
  }, true)

  const issuePts = data.value?.issueTrend || []
  ensureChart('issue', issueChartRef.value)?.setOption({
    ...baseAxis(),
    legend: { top: 0, left: 0, textStyle: { color: cssVar('--text-secondary'), fontSize: 12 } },
    xAxis: { ...baseAxis().xAxis, data: issuePts.map(p => String(p.date).slice(5)) },
    series: [
      { name: 'CRITICAL', type: 'bar', stack: 'i', data: issuePts.map(p => p.critical), itemStyle: { color: '#B91C1C' } },
      { name: 'HIGH', type: 'bar', stack: 'i', data: issuePts.map(p => p.high), itemStyle: { color: '#C2410C' } },
      { name: 'MEDIUM', type: 'bar', stack: 'i', data: issuePts.map(p => p.medium), itemStyle: { color: '#A16207' } },
      { name: 'LOW', type: 'bar', stack: 'i', data: issuePts.map(p => p.low), itemStyle: { color: '#64748B' } }
    ]
  }, true)

  const cats = data.value?.categoryDistribution || []
  ensureChart('category', categoryChartRef.value)?.setOption({
    grid: { top: 8, left: 8, right: 24, bottom: 0, containLabel: true },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: cssVar('--divider') } } },
    yAxis: {
      type: 'category',
      data: cats.map(c => c.name).reverse(),
      axisLabel: { color: cssVar('--text-assist'), fontSize: 11 }
    },
    series: [{
      type: 'bar',
      data: cats.map(c => c.count).reverse(),
      itemStyle: { color: '#15803D' },
      barMaxWidth: 16
    }]
  }, true)
}

function ensureChart(key, el) {
  if (!el) return null
  if (!charts[key]) charts[key] = echarts.init(el)
  return charts[key]
}

function exportChart(key) {
  const chart = charts[key]
  if (!chart) return
  const url = chart.getDataURL({ type: 'png', pixelRatio: 2, backgroundColor: '#fff' })
  const a = document.createElement('a')
  a.href = url
  a.download = `insight-${key}.png`
  a.click()
}

onMounted(async () => {
  const remembered = loadInsightFilters('overview')
  if (remembered) {
    rangePreset.value = remembered.rangePreset ?? 7
    customRange.value = remembered.customRange || []
    query.businessSystemId = toIdParam(remembered.businessSystemId)
  }
  await loadOptions()
  await loadDict()
  await loadData()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  Object.keys(charts).forEach(key => {
    charts[key]?.dispose()
    charts[key] = null
  })
})

function resizeCharts() {
  Object.values(charts).forEach(c => c && c.resize())
}
</script>

<style scoped lang="scss">
.insight-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.insight-toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.version {
  color: var(--text-assist);
  font-size: 12px;
}
.kpi-row {
  margin-bottom: 8px;
}
.kpi-card {
  background: var(--el-bg-color);
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 8px;
  padding: 16px 18px;
  margin-bottom: 12px;
}
.kpi-name {
  color: var(--text-secondary);
  font-size: 13px;
}
.kpi-value {
  margin-top: 8px;
  font-size: 28px;
  font-weight: 600;
  color: var(--text-regular);
}
.kpi-change {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-assist);
}
.chart-panel {
  background: var(--el-bg-color);
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 8px;
  padding: 12px 14px 8px;
  margin-bottom: 12px;
}
.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
  h3 {
    margin: 0;
    font-size: 14px;
    font-weight: 600;
  }
}
.chart-box {
  width: 100%;
  height: 280px;
}
.mt16 { margin-top: 16px; }
.mb16 { margin-bottom: 16px; }
.insight-state {
  display: flex;
  gap: 12px;
  align-items: center;
  color: var(--text-secondary);
  padding: 40px 0;
}
.dict-version {
  color: var(--text-assist);
  font-size: 12px;
  margin-top: 0;
}
.dict-item {
  padding: 12px 0;
  border-bottom: 1px solid var(--border-light, #e5e7eb);
}
.dict-name {
  font-weight: 600;
  margin-bottom: 4px;
}
.dict-def {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.5;
}
</style>
