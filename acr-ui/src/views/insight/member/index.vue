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
      <el-table class="mt16" :data="team?.members || []" empty-text="暂无已关联成员数据">
        <el-table-column label="成员" min-width="160" :show-overflow-tooltip="true">
          <template #default="scope">
            <el-tooltip
              v-if="(scope.row.identities || []).length"
              :content="(scope.row.identities || []).join('、')"
              placement="top"
            >
              <span>{{ scope.row.authorName || scope.row.authorKey }}</span>
            </el-tooltip>
            <span v-else>{{ scope.row.authorName || scope.row.authorKey }}</span>
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
        <el-table :data="team?.unbound || []" empty-text="暂无未关联成员" size="small">
          <el-table-column label="提交身份" min-width="180" :show-overflow-tooltip="true">
            <template #default="scope">{{ scope.row.authorName || scope.row.authorKey }}</template>
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

const router = useRouter()
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

const canManageIdentity = computed(() => checkPermi(['insight:identity:manage']))
const bindVisible = ref(false)
const bindTarget = ref(null)
const bindUserId = ref(null)
const userOptions = ref([])
const userLoading = ref(false)

function params() {
  return { days: Number(rangePreset.value) || 7 }
}

function goBindEmail() {
  router.push({ name: 'Profile', params: { activeTab: 'commitEmail' } })
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
  // 图例仍只切换已关联成员；未关联成员始终入图，与 stackedTrend 全量口径一致
  const bound = (team.value?.members || []).filter(m => visibleAuthors[m.authorKey] !== false)
  const unbound = team.value?.unbound || []
  const chartMembers = [...bound, ...unbound]
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
