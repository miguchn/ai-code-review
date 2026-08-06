<template>
  <div class="app-container">
    <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch" label-width="84px">
      <el-form-item label="所属项目" prop="projectId">
        <el-select v-model="queryParams.projectId" clearable filterable placeholder="请选择项目" style="width: 200px">
          <el-option v-for="item in projectOptions" :key="item.projectId" :label="item.projectName" :value="item.projectId" />
        </el-select>
      </el-form-item>
      <el-form-item label="合并请求" prop="prNumber">
        <el-input v-model="queryParams.prNumber" placeholder="编号" clearable style="width: 120px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="问题状态" prop="status">
        <el-select
          v-model="queryParams.status"
          clearable
          placeholder="请选择状态"
          style="width: 140px"
          @change="onStatusChange"
        >
          <el-option v-for="dict in review_issue_status" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="严重度" prop="severity">
        <el-select v-model="queryParams.severity" clearable placeholder="请选择严重度" style="width: 120px">
          <el-option v-for="item in severityOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="归属" prop="origin">
        <el-select v-model="queryParams.origin" clearable placeholder="请选择归属" style="width: 120px">
          <el-option v-for="dict in review_issue_origin" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词" prop="keyword">
        <el-input v-model="queryParams.keyword" placeholder="标题或文件路径" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row class="mb8 issue-toolbar-row">
      <el-radio-group v-model="viewMode" @change="handleQuery">
        <el-radio-button value="active">当前活跃</el-radio-button>
        <el-radio-button value="all">全部</el-radio-button>
      </el-radio-group>
      <div class="issue-stats-bar" aria-label="问题状态总览">
        <button
          type="button"
          class="stats-item"
          :class="{ 'is-active': isStatsActive('status', 'AWAITING_CONFIRM') }"
          @click="toggleStatsFilter('status', 'AWAITING_CONFIRM')"
        >
          <span class="stats-label">待确认</span>
          <span class="stats-value">{{ stats.awaitingConfirm }}</span>
        </button>
        <span class="stats-divider">｜</span>
        <button
          type="button"
          class="stats-item"
          :class="{ 'is-active': isStatsActive('status', 'AWAITING_FIX') }"
          @click="toggleStatsFilter('status', 'AWAITING_FIX')"
        >
          <span class="stats-label">待修复</span>
          <span class="stats-value">{{ stats.awaitingFix }}</span>
        </button>
        <span class="stats-divider">｜</span>
        <button
          type="button"
          class="stats-item"
          :class="{ 'is-active': isStatsActive('status', 'RECHECKING') }"
          @click="toggleStatsFilter('status', 'RECHECKING')"
        >
          <span class="stats-label">待复核</span>
          <span class="stats-value">{{ stats.rechecking }}</span>
        </button>
        <span class="stats-divider stats-divider-strong">║</span>
        <button
          type="button"
          class="stats-item"
          :class="{ 'is-active': isStatsActive('pendingOnly', 'Y') }"
          @click="toggleStatsFilter('pendingOnly', 'Y')"
        >
          <span class="stats-label">需处理</span>
          <span class="stats-value">{{ stats.pending }}</span>
        </button>
        <span class="stats-divider stats-divider-strong">║</span>
        <button
          type="button"
          class="stats-item"
          :class="{ 'is-active': isStatsActive('closed', true) }"
          @click="toggleStatsFilter('closed')"
        >
          <span class="stats-label">累计已关闭</span>
          <span class="stats-value">{{ stats.closed }}</span>
        </button>
      </div>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <IssueBatchBar
      :selected-rows="selectedRows"
      @clear="clearSelection"
      @done="onBatchDone"
    />

    <el-table
      ref="tableRef"
      v-loading="loading"
      :data="issueList"
      empty-text="暂无问题记录"
      @selection-change="onSelectionChange"
    >
      <el-table-column v-if="canBatch" type="selection" width="48" align="center" />
      <el-table-column label="项目名称" prop="projectName" min-width="150" :show-overflow-tooltip="true" />
      <el-table-column label="合并请求" width="90">
        <template #default="scope">#{{ scope.row.prNumber }}</template>
      </el-table-column>
      <el-table-column label="问题标题" prop="title" min-width="200" :show-overflow-tooltip="true" />
      <el-table-column label="严重度" width="90">
        <template #default="scope">
          <el-tag v-if="scope.row.severity" :type="severityTagType(scope.row.severity)" size="small">
            {{ severityLabel(scope.row.severity) }}
          </el-tag>
          <span v-else class="empty-tip">—</span>
        </template>
      </el-table-column>
      <el-table-column label="阶段" min-width="150">
        <template #default="scope">
          <div class="stage-cell">
            <dict-tag :options="review_issue_status" :value="scope.row.status" />
            <span v-if="stageDurationText(scope.row)" class="stage-duration">
              · {{ stageDurationText(scope.row) }}
            </span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="轮次轨迹" min-width="160">
        <template #default="scope">
          <span v-if="!hasRoundTrail(scope.row)" class="empty-tip">—</span>
          <span v-else class="round-trail">
            <template v-if="canOpenRecord">
              <el-button
                v-if="scope.row.firstTaskId"
                link
                type="primary"
                @click="goRecord(scope.row.firstTaskId)"
              >#{{ scope.row.firstTaskId }}</el-button>
              <template v-if="showRoundArrow(scope.row)">
                <span class="round-arrow">→</span>
                <el-button link type="primary" @click="goRecord(scope.row.lastTaskId)">
                  #{{ scope.row.lastTaskId }}
                </el-button>
              </template>
            </template>
            <template v-else>
              <span v-if="scope.row.firstTaskId">#{{ scope.row.firstTaskId }}</span>
              <template v-if="showRoundArrow(scope.row)">
                <span class="round-arrow">→</span>
                <span>#{{ scope.row.lastTaskId }}</span>
              </template>
            </template>
            <span v-if="scope.row.missedStreak > 0" class="round-miss">
              · 未命中{{ scope.row.missedStreak }}
            </span>
          </span>
        </template>
      </el-table-column>
      <el-table-column label="归属" width="90">
        <template #default="scope">
          <dict-tag :options="review_issue_origin" :value="scope.row.origin" />
        </template>
      </el-table-column>
      <el-table-column label="更新时间" width="170">
        <template #default="scope">{{ formatDateTime(scope.row.updateTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" v-hasPermi="['review:issue:query']" @click="openDetail(scope.row.issueId)">详情</el-button>
          <el-button
            v-if="scope.row.status === 'RECHECKING'"
            link
            type="primary"
            v-hasPermi="['review:issue:query']"
            @click="openDetail(scope.row.issueId)"
          >复核</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <IssueDetailDrawer
      v-model="drawerVisible"
      v-model:issue-id="activeIssueId"
      @disposed="onDisposed"
    />
  </div>
</template>

<script setup name="ReviewIssue">
import { useRoute, useRouter } from 'vue-router'
import { listIssue, getIssueStats } from '@/api/review/issue'
import { listReviewProject } from '@/api/review/project'
import auth from '@/plugins/auth'
import {
  formatDateTime, formatStageDuration, severityLabel, severityTagType
} from '@/utils/reviewDisplay'
import IssueDetailDrawer from './IssueDetailDrawer.vue'
import IssueBatchBar from './IssueBatchBar.vue'

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()
const { review_issue_status, review_issue_origin } = proxy.useDict(
  'review_issue_status',
  'review_issue_origin'
)

const TERMINAL_STATUSES = ['CLOSED', 'IGNORED', 'FALSE_POSITIVE']
const severityOptions = [
  { label: '严重', value: 'CRITICAL' },
  { label: '高', value: 'HIGH' },
  { label: '中', value: 'MEDIUM' },
  { label: '低', value: 'LOW' },
  { label: '信息', value: 'INFO' }
]

const issueList = ref([])
const projectOptions = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const viewMode = ref('active')
const tableRef = ref(null)
const selectedRows = ref([])

const drawerVisible = ref(false)
const activeIssueId = ref(null)

const stats = ref({
  awaitingConfirm: 0,
  awaitingFix: 0,
  rechecking: 0,
  pending: 0,
  closed: 0
})

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  projectId: undefined,
  prNumber: undefined,
  status: undefined,
  pendingOnly: undefined,
  closedFlag: undefined,
  severity: undefined,
  origin: undefined,
  keyword: undefined
})

const canBatch = computed(() =>
  auth.hasPermiOr(['review:issue:confirm', 'review:issue:close'])
)
const canOpenRecord = computed(() => auth.hasPermi('review:record:query'))

const closedFilterActive = computed(() => queryParams.value.closedFlag === 'Y')

function isStatsActive(kind, value) {
  if (kind === 'status') {
    return queryParams.value.status === value
      && !queryParams.value.pendingOnly
      && !queryParams.value.closedFlag
  }
  if (kind === 'pendingOnly') {
    return queryParams.value.pendingOnly === 'Y'
      && !queryParams.value.status
      && !queryParams.value.closedFlag
  }
  if (kind === 'closed') {
    return closedFilterActive.value
  }
  return false
}

function stageDurationText(row) {
  if (!row || TERMINAL_STATUSES.includes(row.status)) return ''
  return formatStageDuration(row.stageEnteredTime)
}

function hasRoundTrail(row) {
  return !!(row?.firstTaskId || row?.lastTaskId || (row?.missedStreak > 0))
}

function showRoundArrow(row) {
  return row?.firstTaskId && row?.lastTaskId && row.firstTaskId !== row.lastTaskId
}

function goRecord(taskId) {
  if (!taskId) return
  proxy.$router.push('/review/record-detail/index/' + taskId)
}

function buildListQuery() {
  const q = { ...queryParams.value }
  if (q.prNumber !== undefined && q.prNumber !== null && q.prNumber !== '') {
    const n = Number(q.prNumber)
    q.prNumber = Number.isFinite(n) ? n : q.prNumber
  } else {
    delete q.prNumber
  }
  if (q.status) {
    delete q.activeFlag
    delete q.pendingOnly
    delete q.closedFlag
  } else if (q.closedFlag === 'Y') {
    delete q.activeFlag
    delete q.pendingOnly
  } else if (q.pendingOnly === 'Y') {
    delete q.activeFlag
    delete q.closedFlag
  } else if (viewMode.value === 'active') {
    q.activeFlag = 'Y'
    delete q.pendingOnly
    delete q.closedFlag
  } else {
    delete q.activeFlag
    delete q.pendingOnly
    delete q.closedFlag
  }
  if (q.pendingOnly !== 'Y') delete q.pendingOnly
  if (q.closedFlag !== 'Y') delete q.closedFlag
  return q
}

function getList() {
  loading.value = true
  clearSelection()
  const listReq = listIssue(buildListQuery()).then(response => {
    issueList.value = response.rows || []
    total.value = response.total || 0
  })
  const statsReq = getIssueStats({}).then(response => {
    const data = response.data || {}
    stats.value = {
      awaitingConfirm: data.awaitingConfirm || 0,
      awaitingFix: data.awaitingFix || 0,
      rechecking: data.rechecking || 0,
      pending: data.pending || 0,
      closed: data.closed || 0
    }
  }).catch(() => {})
  Promise.all([listReq, statsReq]).finally(() => { loading.value = false })
}

function loadProjects() {
  listReviewProject({ pageNum: 1, pageSize: 200, status: '0' }).then(response => {
    projectOptions.value = response.rows || []
  })
}

function syncFilterQuery() {
  const next = { ...route.query }
  if (queryParams.value.status) {
    next.status = String(queryParams.value.status)
  } else {
    delete next.status
  }
  if (queryParams.value.pendingOnly === 'Y') {
    next.pendingOnly = 'Y'
  } else {
    delete next.pendingOnly
  }
  if (queryParams.value.closedFlag === 'Y') {
    next.closedFlag = 'Y'
  } else {
    delete next.closedFlag
  }
  router.replace({ path: route.path, query: next })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  syncFilterQuery()
  getList()
}

function onStatusChange() {
  if (queryParams.value.status) {
    queryParams.value.pendingOnly = undefined
    queryParams.value.closedFlag = undefined
  }
  handleQuery()
}

function toggleStatsFilter(kind, value) {
  if (kind === 'status') {
    if (queryParams.value.status === value && !queryParams.value.pendingOnly && !queryParams.value.closedFlag) {
      queryParams.value.status = undefined
    } else {
      queryParams.value.status = value
      queryParams.value.pendingOnly = undefined
      queryParams.value.closedFlag = undefined
    }
  } else if (kind === 'pendingOnly') {
    if (queryParams.value.pendingOnly === 'Y' && !queryParams.value.status && !queryParams.value.closedFlag) {
      queryParams.value.pendingOnly = undefined
    } else {
      queryParams.value.pendingOnly = 'Y'
      queryParams.value.status = undefined
      queryParams.value.closedFlag = undefined
    }
  } else if (kind === 'closed') {
    if (queryParams.value.closedFlag === 'Y') {
      queryParams.value.closedFlag = undefined
    } else {
      queryParams.value.closedFlag = 'Y'
      queryParams.value.status = undefined
      queryParams.value.pendingOnly = undefined
    }
  }
  handleQuery()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  queryParams.value.pendingOnly = undefined
  queryParams.value.closedFlag = undefined
  viewMode.value = 'active'
  handleQuery()
}

function onSelectionChange(rows) {
  selectedRows.value = rows || []
}

function clearSelection() {
  selectedRows.value = []
  tableRef.value?.clearSelection?.()
}

function onBatchDone() {
  clearSelection()
  getList()
}

function onDisposed() {
  getList()
}

function openDetail(issueId) {
  activeIssueId.value = issueId
  drawerVisible.value = true
  syncIssueQuery(issueId)
}

function syncIssueQuery(issueId) {
  const next = { ...route.query }
  if (issueId) {
    next.issueId = String(issueId)
  } else {
    delete next.issueId
  }
  router.replace({ path: route.path, query: next })
}

watch(activeIssueId, (id) => {
  if (drawerVisible.value) {
    syncIssueQuery(id || undefined)
  }
})

watch(drawerVisible, (visible) => {
  if (!visible) {
    activeIssueId.value = null
    syncIssueQuery(undefined)
  }
})

function openFromRoute() {
  const raw = route.query.issueId
  if (!raw) return
  const issueId = Number(raw)
  if (!issueId) return
  openDetail(issueId)
}

function applyRouteQuery() {
  const q = route.query || {}
  if (q.status) {
    queryParams.value.status = String(q.status)
    queryParams.value.pendingOnly = undefined
    queryParams.value.closedFlag = undefined
  } else if (q.pendingOnly === 'Y' || q.pendingOnly === '1' || q.pendingOnly === 'true') {
    queryParams.value.pendingOnly = 'Y'
    queryParams.value.status = undefined
    queryParams.value.closedFlag = undefined
  } else if (q.closedFlag === 'Y' || q.closedFlag === '1' || q.closedFlag === 'true') {
    queryParams.value.closedFlag = 'Y'
    queryParams.value.status = undefined
    queryParams.value.pendingOnly = undefined
  }
  if (q.origin) queryParams.value.origin = String(q.origin)
  if (q.severity) queryParams.value.severity = String(q.severity)
  if (q.projectId) queryParams.value.projectId = Number(q.projectId) || q.projectId
  if (q.keyword) queryParams.value.keyword = String(q.keyword)
  if (q.prNumber != null && q.prNumber !== '') {
    const n = Number(q.prNumber)
    queryParams.value.prNumber = Number.isFinite(n) ? n : String(q.prNumber)
  }
}

loadProjects()
watch(() => route.query.issueId, () => {
  if (route.query.issueId && !drawerVisible.value) openFromRoute()
})
onMounted(() => {
  openFromRoute()
  applyRouteQuery()
  getList()
})
// 首次进入：TagsView 写入 cachedViews 可能晚于页面首次渲染，未被 keep-alive 缓存的组件
// 不触发 onActivated，首次加载必须在 onMounted 完成；
// 重入 tab：keep-alive 不重跑 onMounted，工作台卡片 query 回填依赖 onActivated
let firstActivated = true
onActivated(() => {
  // 首次挂载紧随的激活已在 onMounted 加载，跳过避免重复请求
  if (firstActivated) { firstActivated = false; return }
  applyRouteQuery()
  getList()
})
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
.empty-tip { color: var(--el-text-color-placeholder); }

.issue-toolbar-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 12px;
}
.issue-stats-bar {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 0;
  min-width: 0;
  margin: 0 0 0 16px;
  padding: 2px 8px;
  background: transparent;
  border: none;
}
.stats-item {
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
  margin: 0;
  padding: 2px 6px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--el-text-color-regular);
  font: inherit;
  cursor: pointer;
  line-height: 1.4;
}
.stats-item:hover {
  background: var(--el-fill-color-light);
}
.stats-item.is-active {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}
.stats-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.stats-item.is-active .stats-label {
  color: var(--el-color-primary);
}
.stats-value {
  font-size: 16px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--el-text-color-primary);
}
.stats-item.is-active .stats-value {
  color: var(--el-color-primary);
}
.stats-divider {
  margin: 0 4px;
  color: var(--el-text-color-placeholder);
  user-select: none;
}
.stats-divider-strong {
  margin: 0 8px;
  color: var(--el-text-color-secondary);
}

.stage-cell {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
}
.stage-duration {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.round-trail {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 2px;
  font-size: 13px;
  color: var(--el-text-color-regular);
}
.round-arrow {
  margin: 0 2px;
  color: var(--el-text-color-secondary);
}
.round-miss {
  margin-left: 2px;
  color: var(--el-text-color-secondary);
}
</style>
