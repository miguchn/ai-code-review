<template>
  <div class="app-container insight-page">
    <div class="detail-head">
      <div>
        <el-button link type="primary" icon="ArrowLeft" @click="goBack">返回列表</el-button>
        <h2 class="title">{{ data?.projectName || '项目分析' }}</h2>
        <div class="meta">
          <span>{{ data?.businessSystemName || '--' }}</span>
          <span>·</span>
          <span>负责人 {{ data?.ownerName || '--' }}</span>
          <span v-if="data?.metricsVersion">· 口径 {{ data.metricsVersion }}</span>
        </div>
      </div>
      <el-form :inline="true">
        <el-form-item label="时间范围">
          <el-radio-group v-model="rangePreset" @change="loadData">
            <el-radio-button :value="7">近 7 天</el-radio-button>
            <el-radio-button :value="30">近 30 天</el-radio-button>
            <el-radio-button v-if="customRange.length === 2" value="custom" disabled>自定义区间</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
    </div>

    <div v-if="loading"><el-skeleton :rows="5" animated /></div>
    <div v-else-if="error" class="insight-state">
      <span>详情加载失败</span>
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

      <el-row :gutter="16">
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
              <h3>问题趋势</h3>
              <el-button link type="primary" @click="exportChart('issue')">导出 PNG</el-button>
            </header>
            <div ref="issueChartRef" class="chart-box" />
          </section>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="mt16">
        <el-col :span="24">
          <section class="chart-panel">
            <header class="panel-head">
              <h3>项目提交趋势</h3>
              <el-button link type="primary" @click="exportChart('commit')">导出 PNG</el-button>
            </header>
            <div ref="commitChartRef" class="chart-box" />
          </section>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="mt16">
        <el-col :xs="24" :lg="8">
          <section class="chart-panel">
            <header class="panel-head"><h3>严重度分布</h3></header>
            <el-table :data="data?.severityDistribution || []" size="small">
              <el-table-column label="严重度">
                <template #default="scope">{{ severityLabel(scope.row.name) }}</template>
              </el-table-column>
              <el-table-column label="数量" prop="count" width="80" />
            </el-table>
          </section>
        </el-col>
        <el-col :xs="24" :lg="8">
          <section class="chart-panel">
            <header class="panel-head"><h3>类别分布</h3></header>
            <el-table :data="data?.categoryDistribution || []" size="small" empty-text="暂无">
              <el-table-column label="类别" :show-overflow-tooltip="true">
                <template #default="scope">{{ issueCategoryLabel(scope.row.name) }}</template>
              </el-table-column>
              <el-table-column label="数量" prop="count" width="80" />
            </el-table>
          </section>
        </el-col>
        <el-col :xs="24" :lg="8">
          <section class="chart-panel">
            <header class="panel-head"><h3>处置漏斗</h3></header>
            <div class="funnel">
              <div>新增 {{ data?.dispositionFunnel?.issueNew ?? 0 }}</div>
              <div>确认 {{ data?.dispositionFunnel?.confirmed ?? 0 }}</div>
              <div>关闭 {{ data?.dispositionFunnel?.closed ?? 0 }}</div>
            </div>
          </section>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup name="InsightProjectDetail">
import echarts from '@/utils/echarts'
import { getInsightProjectDetail } from '@/api/insight'
import { issueCategoryLabel, severityLabel } from '@/utils/reviewDisplay'
import { formatChange, formatKpiValue, toRangePreset } from '../components/insightFilter'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => route.params.projectId)

const loading = ref(false)
const error = ref(false)
const data = ref(null)
const DATE_QUERY_PATTERN = /^\d{4}-\d{2}-\d{2}$/
function isValidDateParam(value) {
  return typeof value === 'string' && DATE_QUERY_PATTERN.test(value)
}
// 列表页自定义区间下钻：query 带合法 beginDate/endDate 时优先沿用（原实现被 rangePreset 恒真吞掉）
const customRange = isValidDateParam(route.query.beginDate) && isValidDateParam(route.query.endDate)
  ? [route.query.beginDate, route.query.endDate]
  : []
const rangePreset = ref(route.query.days
  ? toRangePreset(Number(route.query.days))
  : (customRange.length === 2 ? 'custom' : 7))
const taskChartRef = ref(null)
const issueChartRef = ref(null)
const commitChartRef = ref(null)
const charts = { task: null, issue: null, commit: null }

const emptyHint = computed(() => {
  const since = data.value?.dataSince
  return since
    ? `数据自 ${since} 起积累。可前往代码项目配置页检查接入与触发开关。`
    : '请确认项目已产生审查任务，并已执行聚合刷新任务。'
})

function goBack() {
  router.push('/insight/project')
}

function buildParams() {
  if (rangePreset.value === 'custom' && customRange.length === 2) {
    return { beginDate: customRange[0], endDate: customRange[1] }
  }
  return { days: rangePreset.value === 30 ? 30 : 7 }
}

async function loadData() {
  loading.value = true
  error.value = false
  try {
    const res = await getInsightProjectDetail(projectId.value, buildParams())
    data.value = res.data
    // 先翻转 loading 再渲染：骨架 v-if 会移除图表容器，须等容器挂载后渲染
    loading.value = false
    await nextTick()
    renderCharts()
  } catch (e) {
    error.value = true
    loading.value = false
  }
}

function cssVar(name) {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim()
}

/** loading 骨架会销毁图表容器，旧实例不可复用：DOM 变化或已 dispose 时重建 */
function ensureChart(existing, el) {
  if (existing && !existing.isDisposed() && existing.getDom() === el) return existing
  if (existing && !existing.isDisposed()) existing.dispose()
  return echarts.init(el)
}

function renderCharts() {
  const taskPts = data.value?.taskTrend || []
  if (taskChartRef.value) {
    charts.task = ensureChart(charts.task, taskChartRef.value)
    charts.task.setOption({
      legend: { top: 0, left: 0 },
      grid: { top: 36, left: 8, right: 12, bottom: 0, containLabel: true },
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: taskPts.map(p => String(p.date).slice(5)) },
      yAxis: { type: 'value', minInterval: 1 },
      series: [
        { name: '成功', type: 'bar', stack: 't', data: taskPts.map(p => p.success), itemStyle: { color: '#16A34A' } },
        { name: '失败', type: 'bar', stack: 't', data: taskPts.map(p => p.failed), itemStyle: { color: '#94A3B8' } }
      ]
    }, true)
  }
  const issuePts = data.value?.issueTrend || []
  if (issueChartRef.value) {
    charts.issue = ensureChart(charts.issue, issueChartRef.value)
    charts.issue.setOption({
      legend: { top: 0, left: 0 },
      grid: { top: 36, left: 8, right: 12, bottom: 0, containLabel: true },
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: issuePts.map(p => String(p.date).slice(5)) },
      yAxis: { type: 'value', minInterval: 1 },
      series: [
        { name: severityLabel('CRITICAL'), type: 'bar', stack: 'i', data: issuePts.map(p => p.critical), itemStyle: { color: '#B91C1C' } },
        { name: severityLabel('HIGH'), type: 'bar', stack: 'i', data: issuePts.map(p => p.high), itemStyle: { color: '#C2410C' } },
        { name: severityLabel('MEDIUM'), type: 'bar', stack: 'i', data: issuePts.map(p => p.medium), itemStyle: { color: '#A16207' } },
        { name: severityLabel('LOW'), type: 'bar', stack: 'i', data: issuePts.map(p => p.low), itemStyle: { color: '#64748B' } }
      ]
    }, true)
  }
  const commitPts = data.value?.commitTrend || []
  if (commitChartRef.value) {
    charts.commit = ensureChart(charts.commit, commitChartRef.value)
    charts.commit.setOption({
      grid: { top: 24, left: 8, right: 12, bottom: 0, containLabel: true },
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: commitPts.map(p => String(p.date).slice(5)) },
      yAxis: { type: 'value', minInterval: 1 },
      series: [{
        name: '提交数',
        type: 'line',
        smooth: true,
        data: commitPts.map(p => p.commitCount),
        itemStyle: { color: '#15803D' },
        areaStyle: { color: 'rgba(21, 128, 61, 0.08)' }
      }]
    }, true)
  }
}

function exportChart(key) {
  const chart = charts[key]
  if (!chart) return
  const url = chart.getDataURL({ type: 'png', pixelRatio: 2, backgroundColor: '#fff' })
  const a = document.createElement('a')
  a.href = url
  a.download = `insight-project-${key}.png`
  a.click()
}

onMounted(() => {
  bindResize()
  loadData()
})
watch(projectId, loadData)

function resizeCharts() {
  Object.values(charts).forEach(c => c && !c.isDisposed() && c.resize())
}

/** keep-alive 缓存期容器被 detach，resize 会把画布打成 0×0：激活时重绑并自愈，停用时摘除 */
function bindResize() {
  window.removeEventListener('resize', resizeCharts)
  window.addEventListener('resize', resizeCharts)
}

onActivated(() => {
  bindResize()
  nextTick(resizeCharts)
})
onDeactivated(() => {
  window.removeEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  Object.keys(charts).forEach(k => {
    charts[k]?.dispose()
    charts[k] = null
  })
})
</script>

<style scoped lang="scss">
.detail-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.title {
  margin: 8px 0 4px;
  font-size: 20px;
}
.meta {
  color: var(--text-secondary);
  font-size: 13px;
  display: flex;
  gap: 8px;
}
.kpi-card {
  background: var(--el-bg-color);
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 8px;
  padding: 16px 18px;
  margin-bottom: 12px;
}
.kpi-name { color: var(--text-secondary); font-size: 13px; }
.kpi-value { margin-top: 8px; font-size: 26px; font-weight: 600; }
.kpi-change { margin-top: 6px; font-size: 12px; color: var(--text-assist); }
.chart-panel {
  background: var(--el-bg-color);
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 8px;
  padding: 12px 14px;
  margin-bottom: 12px;
}
.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  h3 { margin: 0; font-size: 14px; }
}
.chart-box { width: 100%; height: 260px; }
.funnel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px 4px;
  color: var(--text-regular);
  font-size: 14px;
}
.insight-state {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 40px 0;
  color: var(--text-secondary);
}
.mt16 { margin-top: 16px; }
.mb16 { margin-bottom: 16px; }
</style>
