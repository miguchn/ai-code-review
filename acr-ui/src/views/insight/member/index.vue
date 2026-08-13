<template>
  <div class="app-container insight-page">
    <el-form :inline="true" :model="teamQuery" class="insight-filters" v-show="showSearch" label-width="84px">
      <el-form-item label="时间范围">
        <el-radio-group v-model="rangePreset" @change="onRangeChange">
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
          @change="onCustomRangeChange"
        />
      </el-form-item>
      <template v-if="viewMode === 'team'">
        <el-form-item label="业务系统">
          <el-select
            v-model="teamQuery.businessSystemId"
            clearable
            filterable
            placeholder="全部"
            style="width: 180px"
            @change="onTeamSystemChange"
          >
            <el-option
              v-for="item in teamBusinessSystems"
              :key="item.id"
              :label="item.label"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="项目">
          <el-select v-model="teamQuery.projectId" clearable filterable placeholder="全部" style="width: 200px">
            <el-option
              v-for="item in teamProjectOptions"
              :key="item.projectId"
              :label="item.projectName"
              :value="item.projectId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="成员">
          <el-select
            v-model="selectedAuthors"
            multiple
            collapse-tags
            collapse-tags-tooltip
            clearable
            filterable
            placeholder="全部成员"
            style="width: 210px"
          >
            <el-option
              v-for="member in allTeamMembers"
              :key="member.authorKey"
              :label="memberLabel(member)"
              :value="member.authorKey"
            />
          </el-select>
        </el-form-item>
      </template>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="reload">查询</el-button>
        <el-button icon="Refresh" @click="resetTeamFilters">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8 insight-action-row">
      <el-radio-group v-model="viewMode">
        <el-radio-button value="mine">本人视图</el-radio-button>
        <el-radio-button v-hasPermi="['insight:team:view']" value="team">团队视图</el-radio-button>
      </el-radio-group>
      <span class="toolbar-hint">本人视图仅展示已关联的提交身份；团队视图按授权范围统计</span>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="reload" />
    </el-row>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="mb16"
      title="用于团队管理与辅导，不作为绩效评价直接输入"
    />

    <div v-if="loading"><el-skeleton :rows="6" animated /></div>
    <div v-else-if="error" class="insight-state">
      <span>加载失败</span>
      <el-button type="primary" link @click="reload">重试</el-button>
    </div>

    <template v-else-if="viewMode === 'mine'">
      <template v-if="!mine?.claimed">
        <el-empty>
          <template #description>
            <p class="empty-guide">
              关联你的提交邮箱后，这里会展示你的提交趋势、被审查情况和关联问题
            </p>
          </template>
          <el-button type="primary" @click="goBindEmail">去关联</el-button>
        </el-empty>
      </template>
      <template v-else>
        <div class="meta mb16">
          已关联：
          <span v-for="(c, idx) in mine.claimedIdentities" :key="c.authorKey">
            {{ c.authorName || c.authorEmail || c.authorKey }}<span v-if="idx < mine.claimedIdentities.length - 1">、</span>
          </span>
          <span v-if="mine.dataSince"> · 数据自 {{ mine.dataSince }} 起积累</span>
        </div>
        <el-row :gutter="16">
          <el-col :xs="12" :sm="8" :md="4"><div class="kpi-card"><div class="kpi-name">被审任务</div><div class="kpi-value">{{ mine.tasksReviewed ?? 0 }}</div></div></el-col>
          <el-col :xs="12" :sm="8" :md="5"><div class="kpi-card"><div class="kpi-name">新增行数</div><div class="kpi-value kpi-add">{{ mine.additionsSum ?? 0 }}</div></div></el-col>
          <el-col :xs="12" :sm="8" :md="5"><div class="kpi-card"><div class="kpi-name">删减行数</div><div class="kpi-value kpi-del">{{ mine.deletionsSum ?? 0 }}</div></div></el-col>
          <el-col :xs="12" :sm="8" :md="5"><div class="kpi-card"><div class="kpi-name">关联新增问题</div><div class="kpi-value">{{ mine.issuesNew ?? 0 }}</div></div></el-col>
          <el-col :xs="12" :sm="8" :md="5"><div class="kpi-card"><div class="kpi-name">未关闭问题</div><div class="kpi-value">{{ mine.issuesOpen ?? 0 }}</div></div></el-col>
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
      <div class="scope-meta">
        {{ teamScopeLabel }}<span v-if="team?.dataSince"> · 数据自 {{ team.dataSince }} 起积累</span>
      </div>
      <el-row :gutter="16" class="kpi-row">
        <el-col v-for="card in teamKpis" :key="card.key" :xs="12" :sm="8" :lg="4">
          <div class="kpi-card">
            <div class="kpi-name">{{ card.label }}</div>
            <div class="kpi-value" :class="card.className">{{ card.value }}</div>
          </div>
        </el-col>
      </el-row>
      <section class="chart-panel">
        <header class="panel-head">
          <h3>成员提交趋势</h3>
          <div class="legend-toggles">
            <el-checkbox
              v-for="m in visibleTeamMembers"
              :key="m.authorKey"
              v-model="visibleAuthors[m.authorKey]"
              @change="renderTeamChart"
            >{{ memberLabel(m) }}</el-checkbox>
          </div>
        </header>
        <div ref="teamChartRef" class="chart-box" />
      </section>
      <el-table class="mt16" :data="visibleBoundMembers" empty-text="暂无已关联成员数据">
        <el-table-column label="成员" min-width="160" :show-overflow-tooltip="true">
          <template #default="scope">
            <el-tooltip
              v-if="(scope.row.identities || []).length"
              :content="(scope.row.identities || []).join('、')"
              placement="top"
            >
              <el-link type="primary" @click="focusMember(scope.row)">{{ scope.row.authorName || scope.row.authorKey }}</el-link>
            </el-tooltip>
            <el-link v-else type="primary" @click="focusMember(scope.row)">{{ scope.row.authorName || scope.row.authorKey }}</el-link>
          </template>
        </el-table-column>
        <el-table-column label="提交次数" prop="commitCount" width="100" sortable />
        <el-table-column label="被审变更数" prop="tasksReviewed" width="120" sortable />
        <el-table-column label="新增行数" prop="additionsSum" width="110" sortable>
          <template #default="scope">
            <span class="line-add">{{ scope.row.additionsSum ?? 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="删减行数" prop="deletionsSum" width="110" sortable>
          <template #default="scope">
            <span class="line-del">{{ scope.row.deletionsSum ?? 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="关联问题数" prop="issuesNew" width="120" sortable />
        <el-table-column label="未关闭问题数" prop="issuesOpen" width="130" sortable />
      </el-table>

      <section class="unbound-panel mt16">
        <header class="panel-head">
          <h3>未关联成员</h3>
          <span class="panel-desc">以下提交身份尚未关联到平台账号</span>
        </header>
        <el-table :data="visibleUnboundMembers" empty-text="暂无未关联成员" size="small">
          <el-table-column label="提交身份" min-width="180" :show-overflow-tooltip="true">
            <template #default="scope">
              <el-link type="primary" @click="focusMember(scope.row)">{{ scope.row.authorName || scope.row.authorKey }}</el-link>
            </template>
          </el-table-column>
          <el-table-column label="提交次数" prop="commitCount" width="100" />
          <el-table-column label="被审变更数" prop="tasksReviewed" width="120" />
          <el-table-column
            v-if="canManageIdentity"
            label="操作"
            width="100"
            fixed="right"
          >
            <template #default="scope">
              <el-button type="primary" link @click="openBind(scope.row)">指派</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </template>

    <el-dialog v-model="bindVisible" title="指派提交邮箱" width="420px" destroy-on-close>
      <p class="bind-tip">将 <strong>{{ bindTarget?.authorKey }}</strong> 关联到平台用户</p>
      <el-select
        v-model="bindUserId"
        filterable
        remote
        clearable
        placeholder="搜索用户"
        :remote-method="searchUsers"
        :loading="userLoading"
        style="width: 100%"
      >
        <el-option
          v-for="u in userOptions"
          :key="u.userId"
          :label="(u.nickName || u.userName) + (u.userName ? '（' + u.userName + '）' : '')"
          :value="u.userId"
        />
      </el-select>
      <template #footer>
        <el-button @click="bindVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!bindUserId" @click="confirmBind">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="InsightMember">
import echarts from '@/utils/echarts'
import { getInsightMemberMine, getInsightTeamMembers } from '@/api/insight'
import { bindTeamIdentity, listIdentityUserOptions } from '@/api/system/identity'
import { checkPermi } from '@/utils/permission'
import { loadInsightFilters, saveInsightFilters, toIdParam, toRangePreset, toDateRangeParam, toStringArrayParam } from '../components/insightFilter'

const router = useRouter()
const loading = ref(false)
const error = ref(false)
const showSearch = ref(true)
const viewMode = ref('mine')
const rangePreset = ref(7)
const customRange = ref([])
const mine = ref(null)
const team = ref(null)
const teamQuery = reactive({
  businessSystemId: undefined,
  projectId: undefined
})
const selectedAuthors = ref([])
const visibleAuthors = reactive({})
const mineChartRef = ref(null)
const teamChartRef = ref(null)
let mineChart = null
let teamChart = null

const canManageIdentity = computed(() => checkPermi(['insight:identity:manage']))
const bindVisible = ref(false)
const bindTarget = ref(null)
const bindUserId = ref(null)
const userOptions = ref([])
const userLoading = ref(false)

const teamProjectOptions = computed(() => {
  const options = team.value?.projectOptions || []
  if (!teamQuery.businessSystemId) return options
  return options.filter(item => String(item.businessSystemId) === String(teamQuery.businessSystemId))
})
const teamBusinessSystems = computed(() => {
  const seen = new Map()
  ;(team.value?.projectOptions || []).forEach(item => {
    if (item.businessSystemId == null || seen.has(String(item.businessSystemId))) return
    seen.set(String(item.businessSystemId), { id: item.businessSystemId, label: item.businessSystemName || '--' })
  })
  return [...seen.values()].sort((a, b) => a.label.localeCompare(b.label))
})
const allTeamMembers = computed(() => [...(team.value?.members || []), ...(team.value?.unbound || [])])
const visibleTeamMembers = computed(() => {
  if (!selectedAuthors.value.length) return allTeamMembers.value
  return allTeamMembers.value.filter(member => selectedAuthors.value.includes(member.authorKey))
})
const visibleBoundMembers = computed(() => visibleTeamMembers.value.filter(member => member.userId != null))
const visibleUnboundMembers = computed(() => visibleTeamMembers.value.filter(member => member.userId == null))
const teamTotals = computed(() => visibleTeamMembers.value.reduce((total, member) => ({
  commits: total.commits + numberOf(member.commitCount),
  tasks: total.tasks + numberOf(member.tasksReviewed),
  additions: total.additions + numberOf(member.additionsSum),
  deletions: total.deletions + numberOf(member.deletionsSum),
  issuesNew: total.issuesNew + numberOf(member.issuesNew),
  issuesOpen: total.issuesOpen + numberOf(member.issuesOpen)
}), { commits: 0, tasks: 0, additions: 0, deletions: 0, issuesNew: 0, issuesOpen: 0 }))
const teamKpis = computed(() => [
  { key: 'members', label: '成员数', value: visibleTeamMembers.value.length },
  { key: 'commits', label: '提交次数', value: teamTotals.value.commits },
  { key: 'tasks', label: '被审变更数', value: teamTotals.value.tasks },
  { key: 'additions', label: '新增行数', value: teamTotals.value.additions, className: 'kpi-add' },
  { key: 'issuesNew', label: '关联新增问题', value: teamTotals.value.issuesNew },
  { key: 'issuesOpen', label: '未关闭问题', value: teamTotals.value.issuesOpen, className: 'kpi-warn' }
])
const teamScopeLabel = computed(() => {
  const system = teamBusinessSystems.value.find(item => String(item.id) === String(teamQuery.businessSystemId))
  const project = (team.value?.projectOptions || []).find(item => String(item.projectId) === String(teamQuery.projectId))
  const scope = [system?.label, project?.projectName].filter(Boolean)
  const range = rangePreset.value === 'custom' && customRange.value.length === 2
    ? `${customRange.value[0]} 至 ${customRange.value[1]}`
    : `近 ${Number(rangePreset.value) || 7} 天`
  return `当前范围：${scope.length ? scope.join(' / ') : '全部授权项目'} · ${range}`
})

function numberOf(value) {
  return Number(value) || 0
}

function memberLabel(member) {
  const name = member.authorName || member.authorKey
  return member.userId == null ? `${name}（未关联）` : name
}

function params() {
  return rangeParams()
}

function rangeParams() {
  if (rangePreset.value === 'custom' && customRange.value?.length === 2) {
    return { beginDate: customRange.value[0], endDate: customRange.value[1] }
  }
  return { days: Number(rangePreset.value) || 7 }
}

function teamParams() {
  return {
    ...rangeParams(),
    businessSystemId: toIdParam(teamQuery.businessSystemId),
    projectId: toIdParam(teamQuery.projectId)
  }
}

function onRangeChange() {
  if (viewMode.value === 'mine' && rangePreset.value !== 'custom') reload()
}

/** 自定义区间变更：本人视图保持原有的即时刷新行为，团队视图沿用「点查询生效」 */
function onCustomRangeChange() {
  if (viewMode.value === 'mine') reload()
}

function onTeamSystemChange() {
  if (teamQuery.projectId && !teamProjectOptions.value.some(item => String(item.projectId) === String(teamQuery.projectId))) {
    teamQuery.projectId = undefined
  }
}

function resetTeamFilters() {
  rangePreset.value = 7
  customRange.value = []
  teamQuery.businessSystemId = undefined
  teamQuery.projectId = undefined
  selectedAuthors.value = []
  reload()
}

function focusMember(member) {
  selectedAuthors.value = [member.authorKey]
  nextTick(renderTeamChart)
}

function goBindEmail() {
  router.push({ name: 'Profile', params: { activeTab: 'commitEmail' } })
}

// 请求序号守卫：快速切换视图/连续查询时丢弃过期响应，防止旧载荷覆盖新状态
let reloadSeq = 0

async function reload() {
  const seq = ++reloadSeq
  loading.value = true
  error.value = false
  if (viewMode.value === 'team') {
    saveInsightFilters('member', {
      rangePreset: rangePreset.value,
      customRange: customRange.value,
      businessSystemId: teamQuery.businessSystemId,
      projectId: teamQuery.projectId,
      selectedAuthors: selectedAuthors.value
    })
  }
  try {
    if (viewMode.value === 'mine') {
      const res = await getInsightMemberMine(params())
      if (seq !== reloadSeq) return
      mine.value = res.data
    } else {
      const res = await getInsightTeamMembers(teamParams())
      if (seq !== reloadSeq) return
      team.value = res.data
      ;allTeamMembers.value.forEach(m => {
        if (visibleAuthors[m.authorKey] === undefined) visibleAuthors[m.authorKey] = true
      })
      selectedAuthors.value = selectedAuthors.value.filter(key => allTeamMembers.value.some(m => m.authorKey === key))
    }
    // 先翻转 loading 再渲染：骨架 v-if 会移除图表容器，须等容器挂载后渲染
    loading.value = false
    await nextTick()
    if (seq !== reloadSeq) return
    if (viewMode.value === 'mine') renderMineChart()
    else renderTeamChart()
  } catch (e) {
    if (seq !== reloadSeq) return
    error.value = true
    loading.value = false
  }
}

function openBind(row) {
  bindTarget.value = row
  bindUserId.value = null
  userOptions.value = []
  bindVisible.value = true
  searchUsers('')
}

async function searchUsers(query) {
  userLoading.value = true
  try {
    const res = await listIdentityUserOptions(query || undefined)
    userOptions.value = res.data || []
  } finally {
    userLoading.value = false
  }
}

async function confirmBind() {
  if (!bindTarget.value || !bindUserId.value) return
  await bindTeamIdentity({
    userId: bindUserId.value,
    identifier: bindTarget.value.authorKey,
    displayName: bindTarget.value.authorName
  })
  bindVisible.value = false
  await reload()
}

/** loading 骨架会销毁图表容器，旧实例不可复用：DOM 变化或已 dispose 时重建 */
function ensureChart(existing, el) {
  if (existing && !existing.isDisposed() && existing.getDom() === el) return existing
  if (existing && !existing.isDisposed()) existing.dispose()
  return echarts.init(el)
}

function renderMineChart() {
  if (!mineChartRef.value) return
  mineChart = ensureChart(mineChart, mineChartRef.value)
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
  teamChart = ensureChart(teamChart, teamChartRef.value)
  const chartMembers = visibleTeamMembers.value.filter(m => visibleAuthors[m.authorKey] !== false)
  const dates = new Set()
  chartMembers.forEach(m => (m.commitTrend || []).forEach(p => dates.add(p.date)))
  const sortedDates = [...dates].sort()
  teamChart.setOption({
    legend: { top: 0, type: 'scroll' },
    grid: { top: 40, left: 8, right: 12, bottom: 0, containLabel: true },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: sortedDates.map(d => String(d).slice(5)) },
    yAxis: { type: 'value', minInterval: 1 },
    series: chartMembers.map(m => {
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

watch(viewMode, () => {
  // 切换视图即销毁旧图实例，避免僵尸 canvas 跨视图复用
  if (mineChart && !mineChart.isDisposed()) mineChart.dispose()
  if (teamChart && !teamChart.isDisposed()) teamChart.dispose()
  mineChart = null
  teamChart = null
  reload()
})
onMounted(async () => {
  bindResize()
  const remembered = loadInsightFilters('member')
  if (remembered) {
    rangePreset.value = toRangePreset(remembered.rangePreset)
    customRange.value = toDateRangeParam(remembered.customRange)
    teamQuery.businessSystemId = toIdParam(remembered.businessSystemId)
    teamQuery.projectId = toIdParam(remembered.projectId)
    selectedAuthors.value = toStringArrayParam(remembered.selectedAuthors)
  }
  await reload()
})
function resizeCharts() {
  if (mineChart && !mineChart.isDisposed()) mineChart.resize()
  if (teamChart && !teamChart.isDisposed()) teamChart.resize()
}

/** keep-alive 缓存期监听会被容器 detach 打成 0×0：激活时重绑并自愈，停用时摘除 */
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
  if (mineChart && !mineChart.isDisposed()) mineChart.dispose()
  if (teamChart && !teamChart.isDisposed()) teamChart.dispose()
})
</script>

<style scoped lang="scss">
.insight-action-row { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; }
.insight-action-row :deep(.top-right-btn) { margin-left: auto; }
.toolbar-hint { margin-left: 12px; color: var(--text-secondary, #64748b); font-size: 13px; }
.insight-filters { margin-bottom: 16px; }
.scope-meta { color: var(--text-secondary, #64748b); font-size: 13px; margin: 0 0 16px; }
.kpi-row { margin-bottom: 4px; }
.mb16 { margin-bottom: 16px; }
.mt16 { margin-top: 16px; }
.empty-guide {
  max-width: 360px;
  margin: 0 auto 4px;
  color: var(--text-secondary, #64748b);
  line-height: 1.6;
}
.kpi-card {
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 8px;
  padding: 14px 16px;
  margin-bottom: 12px;
}
.kpi-name { color: var(--text-secondary); font-size: 13px; }
.kpi-value { margin-top: 6px; font-size: 24px; font-weight: 600; color: var(--text-primary, #0f172a); }
.kpi-add { color: #2f7650; }
.kpi-del { color: #c2413a; }
.kpi-warn { color: #946200; }
.line-add { color: #2f7650; font-variant-numeric: tabular-nums; }
.line-del { color: #c2413a; font-variant-numeric: tabular-nums; }
.chart-panel, .unbound-panel {
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 8px;
  padding: 12px 14px;
}
.panel-head {
  display: flex; justify-content: space-between; gap: 12px; flex-wrap: wrap; align-items: center;
  margin-bottom: 8px;
  h3 { margin: 0; font-size: 14px; }
}
.panel-desc { color: var(--text-secondary, #64748b); font-size: 12px; }
.legend-toggles { display: flex; flex-wrap: wrap; gap: 8px; }
.chart-box { width: 100%; height: 280px; }
.insight-state { display: flex; gap: 12px; padding: 40px 0; color: var(--text-secondary); }
.meta { color: var(--text-secondary); font-size: 13px; }
.bind-tip { margin: 0 0 12px; color: var(--text-regular, #334155); font-size: 13px; }
</style>
