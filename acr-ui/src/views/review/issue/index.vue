<template>
  <div class="app-container">
    <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch" label-width="84px">
      <el-form-item label="所属项目" prop="projectId">
        <el-select v-model="queryParams.projectId" clearable filterable placeholder="请选择项目" style="width: 200px">
          <el-option v-for="item in projectOptions" :key="item.projectId" :label="item.projectName" :value="item.projectId" />
        </el-select>
      </el-form-item>
      <el-form-item label="审查记录" prop="reviewTaskId">
        <el-input v-model="queryParams.reviewTaskId" placeholder="记录编号" clearable style="width: 130px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="合并请求" prop="prNumber">
        <el-input v-model="queryParams.prNumber" placeholder="编号" clearable style="width: 120px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="发起人" prop="prAuthor">
        <el-input v-model="queryParams.prAuthor" placeholder="Git 用户名" clearable style="width: 140px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="分支" prop="branchKeyword">
        <el-input v-model="queryParams.branchKeyword" placeholder="源/目标分支" clearable style="width: 160px" @keyup.enter="handleQuery" />
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
      <el-form-item label="发现时间">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <section v-if="inRecordContext" class="record-context-card">
      <div class="record-context-main">
        <div class="record-context-title">
          <el-tag type="primary" effect="plain">审查记录 #{{ recordContext.record.taskId }}</el-tag>
          <strong>{{ emptyDash(recordContext.record.projectName) }}</strong>
          <span v-if="recordContext.record.businessSystemName" class="context-muted">
            {{ recordContext.record.businessSystemName }}
          </span>
        </div>
        <div class="record-context-meta">
          <span>{{ recordChangeLabel }}</span>
          <span>发起人：{{ emptyDash(recordContext.record.prAuthor) }}</span>
          <span>分支：{{ recordBranchLabel }}</span>
          <span>提交：<code>{{ shortSha(recordContext.record.headSha) }}</code></span>
          <el-tag :type="recordConclusionTagType(recordContext.record)" size="small">
            {{ recordConclusionLabel(recordContext.record) }}
          </el-tag>
          <span>完成：{{ formatDateTime(recordContext.record.finishedTime) }}</span>
          <span>结果问题 {{ recordContext.resultIssueCount || 0 }} 个</span>
        </div>
      </div>
      <div class="record-context-actions">
        <el-button link type="primary" @click="goRecord(recordContext.record.taskId)">查看审查记录</el-button>
        <el-button v-if="recordMergeRequestUrl" link type="primary" @click="openRecordMergeRequest">打开合并请求</el-button>
        <el-button link @click="exitRecordContext">退出记录视图</el-button>
      </div>
    </section>
    <el-alert
      v-if="inRecordContext && recordContext.untrackedIssues?.length"
      class="mb12"
      type="warning"
      :closable="false"
      show-icon
      :title="`该历史记录有 ${recordContext.untrackedIssues.length} 个结果问题尚未进入台账，当前仅展示可持续跟踪的问题。`"
    />

    <el-row class="mb8 issue-toolbar-row">
      <el-radio-group v-if="!recordModeRequested" v-model="viewMode" @change="handleQuery">
        <el-radio-button value="active">当前活跃</el-radio-button>
        <el-radio-button value="all">全部</el-radio-button>
      </el-radio-group>
      <div v-if="!recordModeRequested" class="issue-stats-bar" aria-label="问题状态总览">
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
          <el-tooltip content="待确认 + 待复核，需要人工判断或处置" placement="top">
            <span class="stats-label">待人工处置</span>
          </el-tooltip>
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
      <el-table-column label="项目 / 业务系统" min-width="170">
        <template #default="scope">
          <div class="stacked-cell">
            <span class="primary-text">{{ emptyDash(scope.row.projectName) }}</span>
            <span class="secondary-text">{{ emptyDash(scope.row.businessSystemName) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="变更来源" min-width="210">
        <template #default="scope">
          <div class="stacked-cell">
            <a
              v-if="mergeRequestUrl(scope.row)"
              :href="mergeRequestUrl(scope.row)"
              target="_blank"
              rel="noopener noreferrer"
              class="source-link"
            >{{ mergeRequestLabel(scope.row.provider) }} #{{ scope.row.prNumber }} · {{ emptyDash(scope.row.prTitle) }}</a>
            <span v-else class="primary-text">
              {{ isPushIssue(scope.row) ? formatPushRefDisplay(scope.row) : (mergeRequestLabel(scope.row.provider) + ' #' + scope.row.prNumber) }}
            </span>
            <span class="secondary-text">{{ branchLabel(scope.row) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="问题" min-width="220">
        <template #default="scope">
          <div class="stacked-cell">
            <span class="issue-number">问题 #{{ scope.row.issueId }}</span>
            <span class="primary-text ellipsis-text" :title="scope.row.title || ''">{{ emptyDash(scope.row.title) }}</span>
          </div>
        </template>
      </el-table-column>
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
      <el-table-column label="审查轨迹" min-width="210">
        <template #default="scope">
          <span v-if="!hasRoundTrail(scope.row)" class="empty-tip">—</span>
          <span v-else class="round-trail">
            <template v-if="canOpenRecord">
              <el-button
                v-if="scope.row.firstTaskId"
                link
                type="primary"
                @click="goRecord(scope.row.firstTaskId)"
              >首次 #{{ scope.row.firstTaskId }}</el-button>
              <template v-if="showLastMatchedTask(scope.row)">
                <span class="round-arrow">→</span>
                <el-button link type="primary" @click="goRecord(scope.row.lastTaskId)">
                  最近命中 #{{ scope.row.lastTaskId }}
                </el-button>
              </template>
              <template v-if="showRecheckTask(scope.row)">
                <span v-if="scope.row.firstTaskId || scope.row.lastTaskId" class="round-arrow">→</span>
                <el-button link type="primary" @click="goRecord(scope.row.recheckTaskId)">
                  复核依据 #{{ scope.row.recheckTaskId }}
                </el-button>
              </template>
            </template>
            <template v-else>
              <span v-if="scope.row.firstTaskId">首次 #{{ scope.row.firstTaskId }}</span>
              <template v-if="showLastMatchedTask(scope.row)">
                <span class="round-arrow">→</span>
                <span>最近命中 #{{ scope.row.lastTaskId }}</span>
              </template>
              <template v-if="showRecheckTask(scope.row)">
                <span v-if="scope.row.firstTaskId || scope.row.lastTaskId" class="round-arrow">→</span>
                <span>复核依据 #{{ scope.row.recheckTaskId }}</span>
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
      <el-table-column label="时间" width="185">
        <template #default="scope">
          <div class="stacked-cell time-cell">
            <span>发现：{{ formatDateTime(scope.row.createTime) }}</span>
            <span>变更：{{ formatDateTime(scope.row.updateTime) }}</span>
          </div>
        </template>
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
      v-show="total > 0 && !recordModeRequested"
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
import { listIssue, getIssueStats, getIssueRecordContext } from '@/api/review/issue'
import { listReviewProject } from '@/api/review/project'
import auth from '@/plugins/auth'
import {
  emptyDash, formatDateTime, formatStageDuration, severityLabel, severityTagType, isPushTask,
  shortSha, formatPushRefDisplay, buildMergeRequestUrl, mergeRequestLabel,
  recordConclusionLabel, recordConclusionTagType
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
const dateRange = ref([])
const recordContext = ref(null)

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
  reviewTaskId: undefined,
  prNumber: undefined,
  prAuthor: undefined,
  branchKeyword: undefined,
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
const recordModeRequested = computed(() => !!queryParams.value.reviewTaskId)
const inRecordContext = computed(() => !!recordContext.value?.record)
const recordMergeRequestUrl = computed(() => buildMergeRequestUrl(recordContext.value?.record || {}))
const recordChangeLabel = computed(() => {
  const record = recordContext.value?.record
  if (!record) return '—'
  if (isPushTask(record)) return `Push ${formatPushRefDisplay(record)}`
  return `${mergeRequestLabel(record.provider)} #${record.prNumber} · ${emptyDash(record.prTitle)}`
})
const recordBranchLabel = computed(() => branchLabel(recordContext.value?.record))

function isPushIssue(row) {
  return Number(row?.prNumber) === 0 || isPushTask(row)
}

function branchLabel(row) {
  if (!row) return '—'
  if (isPushIssue(row)) return row.targetBranch || row.refBranch || row.sourceBranch || '—'
  return `${emptyDash(row.sourceBranch)} → ${emptyDash(row.targetBranch)}`
}

function mergeRequestUrl(row) {
  return buildMergeRequestUrl(row)
}

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
  return !!(row?.firstTaskId || row?.lastTaskId || row?.recheckTaskId || (row?.missedStreak > 0))
}

function showLastMatchedTask(row) {
  return row?.firstTaskId && row?.lastTaskId && row.firstTaskId !== row.lastTaskId
}

function showRecheckTask(row) {
  return row?.recheckTaskId
    && row.recheckTaskId !== row.firstTaskId
    && row.recheckTaskId !== row.lastTaskId
}

function goRecord(taskId) {
  if (!taskId) return
  proxy.$router.push('/review/record-detail/index/' + taskId)
}

function buildListQuery() {
  const q = proxy.addDateRange({ ...queryParams.value }, dateRange.value)
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
  } else if (viewMode.value === 'active' && !recordModeRequested.value) {
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
  const query = buildListQuery()
  const taskId = Number(queryParams.value.reviewTaskId)
  if (recordModeRequested.value && (!Number.isFinite(taskId) || taskId <= 0)) {
    recordContext.value = null
    issueList.value = []
    total.value = 0
    loading.value = false
    proxy.$modal.msgWarning('审查记录编号应为正整数')
    return
  }
  if (recordModeRequested.value && Number.isFinite(taskId) && taskId > 0) {
    getIssueRecordContext(taskId, query).then(response => {
      recordContext.value = response.data || null
      issueList.value = recordContext.value?.issues || []
      total.value = issueList.value.length
    }).finally(() => { loading.value = false })
    return
  }
  recordContext.value = null
  const listReq = listIssue(query).then(response => {
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
  const directFields = ['reviewTaskId', 'projectId', 'prNumber', 'prAuthor', 'branchKeyword', 'severity', 'origin', 'keyword']
  directFields.forEach(field => {
    const value = queryParams.value[field]
    if (value !== undefined && value !== null && value !== '') next[field] = String(value)
    else delete next[field]
  })
  if (dateRange.value?.length === 2) {
    next.beginTime = dateRange.value[0]
    next.endTime = dateRange.value[1]
  } else {
    delete next.beginTime
    delete next.endTime
  }
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
  dateRange.value = []
  recordContext.value = null
  queryParams.value.pendingOnly = undefined
  queryParams.value.closedFlag = undefined
  viewMode.value = 'active'
  handleQuery()
}

function exitRecordContext() {
  queryParams.value.reviewTaskId = undefined
  recordContext.value = null
  handleQuery()
}

function openRecordMergeRequest() {
  if (recordMergeRequestUrl.value) {
    window.open(recordMergeRequestUrl.value, '_blank', 'noopener,noreferrer')
  }
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
  queryParams.value.projectId = undefined
  queryParams.value.reviewTaskId = undefined
  queryParams.value.prNumber = undefined
  queryParams.value.prAuthor = undefined
  queryParams.value.branchKeyword = undefined
  queryParams.value.status = undefined
  queryParams.value.pendingOnly = undefined
  queryParams.value.closedFlag = undefined
  queryParams.value.severity = undefined
  queryParams.value.origin = undefined
  queryParams.value.keyword = undefined
  dateRange.value = []
  recordContext.value = null
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
  if (q.reviewTaskId) queryParams.value.reviewTaskId = Number(q.reviewTaskId) || q.reviewTaskId
  if (q.prAuthor) queryParams.value.prAuthor = String(q.prAuthor)
  if (q.branchKeyword) queryParams.value.branchKeyword = String(q.branchKeyword)
  if (q.keyword) queryParams.value.keyword = String(q.keyword)
  if (q.prNumber != null && q.prNumber !== '') {
    const n = Number(q.prNumber)
    queryParams.value.prNumber = Number.isFinite(n) ? n : String(q.prNumber)
  }
  if (q.beginTime || q.endTime) {
    dateRange.value = [q.beginTime ? String(q.beginTime) : '', q.endTime ? String(q.endTime) : '']
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
.mb12 { margin-bottom: 12px; }
.empty-tip { color: var(--el-text-color-placeholder); }

.record-context-card {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
  padding: 14px 16px;
  border: 1px solid var(--el-color-primary-light-7);
  border-left: 4px solid var(--el-color-primary);
  border-radius: 8px;
  background: var(--el-color-primary-light-9);
}
.record-context-main { min-width: 0; }
.record-context-title,
.record-context-meta,
.record-context-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 14px;
}
.record-context-meta {
  margin-top: 8px;
  font-size: 13px;
  color: var(--el-text-color-regular);
}
.record-context-meta code { font-size: 12px; }
.record-context-actions { flex-shrink: 0; }
.context-muted { color: var(--el-text-color-secondary); font-size: 13px; }

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
.stacked-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
  line-height: 1.4;
}
.primary-text { color: var(--el-text-color-primary); }
.secondary-text,
.time-cell { font-size: 12px; color: var(--el-text-color-secondary); }
.issue-number { font-size: 12px; font-weight: 600; color: var(--el-color-primary); }
.ellipsis-text { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.source-link { color: var(--el-color-primary); text-decoration: none; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.source-link:hover { text-decoration: underline; }
@media (max-width: 900px) {
  .record-context-card { flex-direction: column; }
  .record-context-actions { align-self: flex-start; }
}
</style>
