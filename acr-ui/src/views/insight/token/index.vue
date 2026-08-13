<template>
  <div class="app-container insight-page">
    <el-form :inline="true" :model="query" class="insight-filters" v-show="showSearch" label-width="84px">
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
      <el-form-item label="模型">
        <el-select v-model="query.modelId" clearable filterable placeholder="全部" style="width: 200px">
          <el-option
            v-for="item in modelOptions"
            :key="item.modelId"
            :label="item.modelName"
            :value="item.modelId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="项目">
        <el-select v-model="query.projectId" clearable filterable placeholder="全部" style="width: 200px">
          <el-option
            v-for="item in projectOptions"
            :key="item.projectId"
            :label="item.projectName"
            :value="item.projectId"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="search">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="loadData" />
    </el-row>

    <div v-if="loading">
      <el-skeleton :rows="8" animated />
    </div>
    <div v-else-if="error" class="insight-state">
      <span>Token 用量加载失败</span>
      <el-button type="primary" link @click="loadData">重试</el-button>
    </div>
    <template v-else>
      <div class="scope-meta">
        <span v-if="overview?.dataSince">数据自 {{ overview.dataSince }} 起积累</span>
        <span v-if="overview?.dataGapRatio > 0">
          <span v-if="overview?.dataSince"> · </span>数据缺口 {{ formatRatio(overview.dataGapRatio) }}
        </span>
      </div>
      <el-alert
        v-if="overview?.empty"
        type="info"
        :closable="false"
        show-icon
        class="mb16"
        :title="overview.emptyReason || '暂无 Token 用量数据'"
        :description="emptyHint"
      />

      <el-row :gutter="16" class="kpi-row">
        <el-col v-for="card in overview?.kpis || []" :key="card.code" :xs="12" :sm="8" :lg="4">
          <div class="kpi-card">
            <div class="kpi-name">{{ card.name }}</div>
            <div class="kpi-value">{{ formatKpiValue(card) }}</div>
            <div class="kpi-change">{{ formatChange(card.changeRatio) }}</div>
          </div>
        </el-col>
      </el-row>

      <section class="chart-panel">
        <header class="panel-head"><h3>Token 趋势</h3></header>
        <div ref="trendChartRef" class="chart-box" />
      </section>

      <el-row :gutter="16" class="mt16">
        <el-col :xs="24" :lg="10">
          <section class="chart-panel">
            <header class="panel-head"><h3>模型占比</h3></header>
            <div ref="pieChartRef" class="chart-box" />
          </section>
        </el-col>
        <el-col :xs="24" :lg="14">
          <section class="chart-panel">
            <header class="panel-head"><h3>模型维度</h3></header>
            <el-table :data="models" empty-text="暂无模型用量" @row-click="onModelRow">
              <el-table-column label="模型" prop="modelName" min-width="140" :show-overflow-tooltip="true">
                <template #default="scope">
                  <el-link type="primary">{{ scope.row.modelName || '未命名模型' }}</el-link>
                </template>
              </el-table-column>
              <el-table-column label="厂商" prop="provider" min-width="100" :show-overflow-tooltip="true" />
              <el-table-column label="调用次数" prop="callCount" width="110" sortable />
              <el-table-column label="输入" prop="inputTokens" width="100" sortable>
                <template #default="scope">{{ formatTokenCount(scope.row.inputTokens) }}</template>
              </el-table-column>
              <el-table-column label="输出" prop="outputTokens" width="100" sortable>
                <template #default="scope">{{ formatTokenCount(scope.row.outputTokens) }}</template>
              </el-table-column>
              <el-table-column label="总 Token" prop="totalTokens" width="110" sortable>
                <template #default="scope">{{ formatTokenCount(scope.row.totalTokens) }}</template>
              </el-table-column>
              <el-table-column label="估算成本" prop="estimatedCost" width="120" sortable>
                <template #default="scope">{{ formatYuan(scope.row.estimatedCost) }}</template>
              </el-table-column>
              <el-table-column label="占比" prop="share" width="90" sortable>
                <template #default="scope">{{ formatRatio(scope.row.share) }}</template>
              </el-table-column>
            </el-table>
          </section>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="mt16">
        <el-col :xs="24" :lg="10">
          <section class="chart-panel">
            <header class="panel-head"><h3>项目排行</h3></header>
            <div ref="barChartRef" class="chart-box" />
          </section>
        </el-col>
        <el-col :xs="24" :lg="14">
          <section class="chart-panel">
            <header class="panel-head"><h3>项目维度</h3></header>
            <el-table :data="projects" empty-text="暂无项目用量" @row-click="onProjectRow">
              <el-table-column label="项目" prop="projectName" min-width="140" :show-overflow-tooltip="true">
                <template #default="scope">
                  <el-link type="primary">{{ scope.row.projectName }}</el-link>
                </template>
              </el-table-column>
              <el-table-column label="业务系统" prop="businessSystemName" min-width="120" :show-overflow-tooltip="true" />
              <el-table-column label="负责人" prop="ownerName" width="100" :show-overflow-tooltip="true" />
              <el-table-column label="调用次数" prop="callCount" width="110" sortable />
              <el-table-column label="总 Token" prop="totalTokens" width="110" sortable>
                <template #default="scope">{{ formatTokenCount(scope.row.totalTokens) }}</template>
              </el-table-column>
              <el-table-column label="估算成本" prop="estimatedCost" width="120" sortable>
                <template #default="scope">{{ formatYuan(scope.row.estimatedCost) }}</template>
              </el-table-column>
            </el-table>
          </section>
        </el-col>
      </el-row>

      <section id="token-runs" class="chart-panel mt16">
        <header class="panel-head"><h3>审查明细</h3></header>
        <el-table :data="runs" empty-text="暂无审查用量明细" @row-click="onRunRow">
          <el-table-column label="记录ID" prop="taskId" width="100">
            <template #default="scope">
              <el-link type="primary">{{ scope.row.taskId }}</el-link>
            </template>
          </el-table-column>
          <el-table-column label="项目" prop="projectName" min-width="140" :show-overflow-tooltip="true" />
          <el-table-column label="模型" prop="modelName" min-width="140" :show-overflow-tooltip="true" />
          <el-table-column label="审查方式" prop="reviewMode" width="120">
            <template #default="scope">
              <dict-tag :options="review_mode" :value="normalizeMode(scope.row.reviewMode)" />
            </template>
          </el-table-column>
          <el-table-column label="触发时间" prop="triggerTime" min-width="170" sortable />
          <el-table-column label="输入" prop="inputTokens" width="90" sortable>
            <template #default="scope">{{ formatTokenCount(scope.row.inputTokens) }}</template>
          </el-table-column>
          <el-table-column label="输出" prop="outputTokens" width="90" sortable>
            <template #default="scope">{{ formatTokenCount(scope.row.outputTokens) }}</template>
          </el-table-column>
          <el-table-column label="总 Token" prop="totalTokens" width="110" sortable>
            <template #default="scope">{{ formatTokenCount(scope.row.totalTokens) }}</template>
          </el-table-column>
          <el-table-column label="估算成本" prop="estimatedCost" width="120" sortable>
            <template #default="scope">{{ formatYuan(scope.row.estimatedCost) }}</template>
          </el-table-column>
        </el-table>
        <pagination
          v-show="runTotal > 0"
          :total="runTotal"
          v-model:page="runQuery.pageNum"
          v-model:limit="runQuery.pageSize"
          @pagination="loadRuns"
        />
      </section>
    </template>
  </div>
</template>

<script setup name="InsightTokenUsage">
import echarts from '@/utils/echarts'
import { getTokenOverview, getTokenTrend, listTokenModels, listTokenProjects, listTokenRuns } from '@/api/insight'
import { normalizeMode } from '@/utils/reviewDisplay'
import {
  formatChange,
  formatKpiValue,
  formatRatio,
  formatTokenCount,
  formatYuan,
  loadInsightFilters,
  saveInsightFilters,
  toIdParam,
  toRangePreset,
  toDateRangeParam
} from '../components/insightFilter'

const { proxy } = getCurrentInstance()
const { review_mode } = proxy.useDict('review_mode')
const router = useRouter()

const loading = ref(false)
const error = ref(false)
const showSearch = ref(true)
const rangePreset = ref(7)
const customRange = ref([])
const query = reactive({
  modelId: undefined,
  projectId: undefined
})
const overview = ref(null)
const trend = ref([])
const models = ref([])
const projects = ref([])
const runs = ref([])
const runTotal = ref(0)
const runQuery = reactive({ pageNum: 1, pageSize: 10 })
const modelOptions = ref([])
const projectOptions = ref([])

const trendChartRef = ref(null)
const pieChartRef = ref(null)
const barChartRef = ref(null)
const charts = { trend: null, pie: null, bar: null }

const emptyHint = computed(() => {
  const since = overview.value?.dataSince
  return since
    ? `数据自 ${since} 起积累。采集上线前的审查不会回填 Token。`
    : '尚未采集到 Token 用量。完成一次大模型审查后刷新本页。'
})

function onPresetChange() {
  /* handled in buildParams */
}

function search() {
  runQuery.pageNum = 1
  loadData()
}

function resetQuery() {
  rangePreset.value = 7
  customRange.value = []
  query.modelId = undefined
  query.projectId = undefined
  runQuery.pageNum = 1
  loadData()
}

function buildParams() {
  const params = {
    modelId: toIdParam(query.modelId),
    projectId: toIdParam(query.projectId)
  }
  if (rangePreset.value === 'custom' && customRange.value?.length === 2) {
    params.beginDate = customRange.value[0]
    params.endDate = customRange.value[1]
  } else {
    params.days = Number(rangePreset.value) || 7
  }
  return params
}

function persistFilters() {
  saveInsightFilters('token', {
    rangePreset: rangePreset.value,
    customRange: customRange.value,
    modelId: query.modelId,
    projectId: query.projectId
  })
}

async function loadData() {
  loading.value = true
  error.value = false
  persistFilters()
  try {
    await Promise.all([loadDashboard(), loadRuns()])
    loading.value = false
    await nextTick()
    renderCharts()
  } catch (e) {
    error.value = true
    loading.value = false
  }
}

async function loadDashboard() {
  const params = buildParams()
  const [overviewRes, trendRes, modelsRes, projectsRes] = await Promise.all([
    getTokenOverview(params),
    getTokenTrend(params),
    listTokenModels(params),
    listTokenProjects(params)
  ])
  overview.value = overviewRes.data
  trend.value = trendRes.data || []
  models.value = modelsRes.data || []
  projects.value = projectsRes.data || []
  modelOptions.value = overview.value?.modelOptions || []
  projectOptions.value = overview.value?.projectOptions || []
}

async function loadRuns() {
  const res = await listTokenRuns({
    ...buildParams(),
    pageNum: runQuery.pageNum,
    pageSize: runQuery.pageSize
  })
  runs.value = res.rows || []
  runTotal.value = res.total || 0
}

function onModelRow(row) {
  query.modelId = row.modelId
  runQuery.pageNum = 1
  loadData()
}

function onProjectRow(row) {
  query.projectId = row.projectId
  runQuery.pageNum = 1
  loadData().then(() => {
    nextTick(() => {
      document.getElementById('token-runs')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    })
  })
}

function onRunRow(row) {
  if (!row?.taskId) return
  router.push('/review/record-detail/index/' + row.taskId)
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
  const pts = trend.value || []
  ensureChart('trend', trendChartRef.value)?.setOption({
    ...baseAxis(),
    legend: { top: 0, left: 0, textStyle: { color: cssVar('--text-secondary'), fontSize: 12 } },
    xAxis: { ...baseAxis().xAxis, data: pts.map(p => String(p.date).slice(5)) },
    series: [
      { name: '输入 Token', type: 'bar', stack: 't', data: pts.map(p => p.inputTokens || 0), itemStyle: { color: cssVar('--el-color-primary') } },
      { name: '输出 Token', type: 'bar', stack: 't', data: pts.map(p => p.outputTokens || 0), itemStyle: { color: cssVar('--el-color-warning') } }
    ]
  }, true)

  const pieColors = [
    cssVar('--el-color-primary'),
    cssVar('--el-color-success'),
    cssVar('--el-color-warning'),
    cssVar('--el-color-info'),
    cssVar('--el-color-danger')
  ]
  const pieData = (models.value || []).map((row, idx) => ({
    name: row.modelName || '未命名模型',
    value: row.totalTokens || 0,
    itemStyle: { color: pieColors[idx % pieColors.length] }
  }))
  ensureChart('pie', pieChartRef.value)?.setOption({
    tooltip: { trigger: 'item' },
    legend: { type: 'scroll', bottom: 0, textStyle: { color: cssVar('--text-secondary'), fontSize: 12 } },
    series: [{
      type: 'pie',
      radius: ['42%', '68%'],
      data: pieData,
      label: { color: cssVar('--text-regular'), fontSize: 11 }
    }]
  }, true)

  const top = (projects.value || []).slice(0, 10).slice().reverse()
  ensureChart('bar', barChartRef.value)?.setOption({
    grid: { top: 8, left: 8, right: 24, bottom: 0, containLabel: true },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: cssVar('--divider') } },
      axisLabel: { color: cssVar('--text-assist'), fontSize: 11 }
    },
    yAxis: {
      type: 'category',
      data: top.map(p => p.projectName),
      axisLabel: { color: cssVar('--text-assist'), fontSize: 11 }
    },
    series: [{
      type: 'bar',
      data: top.map(p => p.totalTokens || 0),
      itemStyle: { color: cssVar('--el-color-primary') },
      barMaxWidth: 16
    }]
  }, true)
}

function ensureChart(key, el) {
  if (!el) return null
  const existing = charts[key]
  if (existing && !existing.isDisposed() && existing.getDom() === el) return existing
  if (existing && !existing.isDisposed()) existing.dispose()
  charts[key] = echarts.init(el)
  return charts[key]
}

function bindResize() {
  window.removeEventListener('resize', resizeCharts)
  window.addEventListener('resize', resizeCharts)
}

function resizeCharts() {
  Object.values(charts).forEach(c => c && !c.isDisposed() && c.resize())
}

onMounted(async () => {
  const remembered = loadInsightFilters('token')
  if (remembered) {
    rangePreset.value = toRangePreset(remembered.rangePreset)
    customRange.value = toDateRangeParam(remembered.customRange)
    query.modelId = toIdParam(remembered.modelId)
    query.projectId = toIdParam(remembered.projectId)
  }
  await loadData()
  bindResize()
})

onActivated(() => {
  bindResize()
  nextTick(resizeCharts)
})
onDeactivated(() => {
  window.removeEventListener('resize', resizeCharts)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  Object.keys(charts).forEach(key => {
    charts[key]?.dispose()
    charts[key] = null
  })
})
</script>

<style scoped lang="scss">
.scope-meta {
  color: var(--text-assist);
  font-size: 12px;
  margin-bottom: 12px;
}
.kpi-row {
  margin-bottom: 8px;
}
.kpi-card {
  background: var(--el-bg-color);
  border: 1px solid var(--border-light, var(--el-border-color));
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
  font-size: 24px;
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
  border: 1px solid var(--border-light, var(--el-border-color));
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
    color: var(--text-regular);
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
</style>
