<template>
  <div class="app-container insight-page">
    <el-alert
      type="warning"
      :closable="false"
      show-icon
      class="mb16"
      title="用于团队管理与辅导，不作为绩效评价直接输入"
    />

    <div class="toolbar">
      <el-radio-group v-model="viewMode" class="mr16">
        <el-radio-button value="mine">本人视图</el-radio-button>
        <el-radio-button v-hasPermi="['insight:team:view']" value="team">团队视图</el-radio-button>
      </el-radio-group>
      <el-radio-group v-model="rangePreset" @change="reload">
        <el-radio-button :value="7">近 7 天</el-radio-button>
        <el-radio-button :value="30">近 30 天</el-radio-button>
      </el-radio-group>
    </div>

    <div v-if="loading"><el-skeleton :rows="6" animated /></div>
    <div v-else-if="error" class="insight-state">
      <span>加载失败</span>
      <el-button type="primary" link @click="reload">重试</el-button>
    </div>

    <template v-else-if="viewMode === 'mine'">
      <template v-if="!mine?.claimed">
        <el-empty description="尚未认领提交身份">
          <template #description>
            <p>请从下方候选身份中认领，认领后可查看本人提交与被审情况。</p>
          </template>
        </el-empty>
        <el-row :gutter="12">
          <el-col v-for="item in mine?.candidates || []" :key="item.authorKey" :xs="24" :sm="12" :md="8">
            <div class="candidate-card">
              <div class="name">{{ item.authorName || item.authorEmail || item.authorKey }}</div>
              <div class="email">{{ item.authorEmail || '--' }}</div>
              <el-button type="primary" size="small" @click="claim(item)">确认认领</el-button>
            </div>
          </el-col>
        </el-row>
        <el-empty v-if="!(mine?.candidates || []).length" description="暂无匹配候选；请确认 push 审查已产生提交事实" />
      </template>
      <template v-else>
        <div class="meta mb16">
          已认领：
          <span v-for="(c, idx) in mine.claimedIdentities" :key="c.authorKey">
            {{ c.authorName || c.authorEmail }}<span v-if="idx < mine.claimedIdentities.length - 1">、</span>
          </span>
          <span v-if="mine.dataSince"> · 数据自 {{ mine.dataSince }} 起积累</span>
        </div>
        <el-row :gutter="16">
          <el-col :xs="24" :sm="8"><div class="kpi-card"><div class="kpi-name">被审任务</div><div class="kpi-value">{{ mine.tasksReviewed ?? 0 }}</div></div></el-col>
          <el-col :xs="24" :sm="8"><div class="kpi-card"><div class="kpi-name">关联新增问题</div><div class="kpi-value">{{ mine.issuesNew ?? 0 }}</div></div></el-col>
          <el-col :xs="24" :sm="8"><div class="kpi-card"><div class="kpi-name">未关闭问题</div><div class="kpi-value">{{ mine.issuesOpen ?? 0 }}</div></div></el-col>
        </el-row>
        <section class="chart-panel mt16">
          <header class="panel-head"><h3>我的提交趋势</h3></header>
          <div ref="mineChartRef" class="chart-box" />
        </section>
        <section class="chart-panel mt16">
          <header class="panel-head"><h3>我的未关闭问题</h3></header>
          <el-table :data="mine.openIssueTitles || []" size="small" empty-text="暂无未关闭问题">
            <el-table-column label="标题" prop="name" min-width="220" :show-overflow-tooltip="true" />
            <el-table-column label="数量" prop="count" width="80" />
          </el-table>
        </section>
      </template>
    </template>

    <template v-else>
      <section class="chart-panel">
        <header class="panel-head">
          <h3>成员提交趋势</h3>
          <div class="legend-toggles">
            <el-checkbox
              v-for="m in team?.members || []"
              :key="m.authorKey"
              v-model="visibleAuthors[m.authorKey]"
              @change="renderTeamChart"
            >{{ m.authorName || m.authorKey }}</el-checkbox>
          </div>
        </header>
        <div ref="teamChartRef" class="chart-box" />
      </section>
      <el-table class="mt16" :data="team?.members || []" empty-text="暂无团队成员数据">
        <el-table-column label="提交身份" min-width="160" :show-overflow-tooltip="true">
          <template #default="scope">{{ scope.row.authorName || scope.row.authorKey }}</template>
        </el-table-column>
        <el-table-column label="提交次数" prop="commitCount" width="100" sortable />
        <el-table-column label="被审变更数" prop="tasksReviewed" width="120" sortable />
        <el-table-column label="关联问题数" prop="issuesNew" width="120" sortable />
        <el-table-column label="未关闭问题数" prop="issuesOpen" width="130" sortable />
      </el-table>
    </template>
  </div>
</template>

<script setup name="InsightMember">
import echarts from '@/utils/echarts'
import {
  claimInsightMemberIdentity,
  getInsightMemberMine,
  getInsightTeamMembers
} from '@/api/insight'

const loading = ref(false)
const error = ref(false)
const viewMode = ref('mine')
const rangePreset = ref(7)
const mine = ref(null)
const team = ref(null)
const visibleAuthors = reactive({})
const mineChartRef = ref(null)
const teamChartRef = ref(null)
let mineChart = null
let teamChart = null

function params() {
  return { days: Number(rangePreset.value) || 7 }
}

async function reload() {
  loading.value = true
  error.value = false
  try {
    if (viewMode.value === 'mine') {
      const res = await getInsightMemberMine(params())
      mine.value = res.data
      await nextTick()
      renderMineChart()
    } else {
      const res = await getInsightTeamMembers(params())
      team.value = res.data
      ;(team.value?.members || []).forEach(m => {
        if (visibleAuthors[m.authorKey] === undefined) visibleAuthors[m.authorKey] = true
      })
      await nextTick()
      renderTeamChart()
    }
  } catch (e) {
    error.value = true
  } finally {
    loading.value = false
  }
}

async function claim(item) {
  await claimInsightMemberIdentity({
    authorEmail: item.authorEmail || item.authorKey,
    authorName: item.authorName
  })
  await reload()
}

function renderMineChart() {
  if (!mineChartRef.value) return
  if (!mineChart) mineChart = echarts.init(mineChartRef.value)
  const pts = mine.value?.commitTrend || []
  mineChart.setOption({
    grid: { top: 24, left: 8, right: 12, bottom: 0, containLabel: true },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: pts.map(p => String(p.date).slice(5)) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{ type: 'line', data: pts.map(p => p.commitCount), smooth: true, itemStyle: { color: '#15803D' } }]
  }, true)
}

function renderTeamChart() {
  if (!teamChartRef.value) return
  if (!teamChart) teamChart = echarts.init(teamChartRef.value)
  const members = (team.value?.members || []).filter(m => visibleAuthors[m.authorKey] !== false)
  const dates = new Set()
  members.forEach(m => (m.commitTrend || []).forEach(p => dates.add(p.date)))
  const sortedDates = [...dates].sort()
  teamChart.setOption({
    legend: { top: 0, type: 'scroll' },
    grid: { top: 40, left: 8, right: 12, bottom: 0, containLabel: true },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: sortedDates.map(d => String(d).slice(5)) },
    yAxis: { type: 'value', minInterval: 1 },
    series: members.map(m => {
      const map = Object.fromEntries((m.commitTrend || []).map(p => [p.date, p.commitCount]))
      return {
        name: m.authorName || m.authorKey,
        type: 'line',
        smooth: true,
        data: sortedDates.map(d => map[d] || 0)
      }
    })
  }, true)
}

watch(viewMode, reload)
onMounted(reload)
onBeforeUnmount(() => {
  mineChart?.dispose()
  teamChart?.dispose()
})
</script>

<style scoped lang="scss">
.toolbar { display: flex; flex-wrap: wrap; gap: 12px; align-items: center; margin-bottom: 16px; }
.mr16 { margin-right: 16px; }
.mb16 { margin-bottom: 16px; }
.mt16 { margin-top: 16px; }
.candidate-card {
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 8px;
  padding: 14px;
  margin-bottom: 12px;
  .name { font-weight: 600; margin-bottom: 4px; }
  .email { color: var(--text-secondary); font-size: 12px; margin-bottom: 10px; }
}
.kpi-card {
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 8px;
  padding: 14px 16px;
  margin-bottom: 12px;
}
.kpi-name { color: var(--text-secondary); font-size: 13px; }
.kpi-value { margin-top: 6px; font-size: 24px; font-weight: 600; }
.chart-panel {
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 8px;
  padding: 12px 14px;
}
.panel-head {
  display: flex; justify-content: space-between; gap: 12px; flex-wrap: wrap; align-items: center;
  h3 { margin: 0; font-size: 14px; }
}
.legend-toggles { display: flex; flex-wrap: wrap; gap: 8px; }
.chart-box { width: 100%; height: 280px; }
.insight-state { display: flex; gap: 12px; padding: 40px 0; color: var(--text-secondary); }
.meta { color: var(--text-secondary); font-size: 13px; }
</style>
