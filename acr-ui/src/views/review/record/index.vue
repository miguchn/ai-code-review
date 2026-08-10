<template>
  <div class="app-container">
    <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch" label-width="92px">
      <el-form-item label="所属项目" prop="projectId">
        <el-select v-model="queryParams.projectId" clearable filterable placeholder="请选择项目" style="width: 220px">
          <el-option v-for="item in projectOptions" :key="item.projectId" :label="item.projectName" :value="item.projectId" />
        </el-select>
      </el-form-item>
      <el-form-item label="变更来源" prop="eventSource">
        <el-select v-model="queryParams.eventSource" clearable placeholder="请选择变更来源" style="width: 130px">
          <el-option v-for="dict in sourceOptions" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="审查结论" prop="reviewConclusion">
        <el-select v-model="queryParams.reviewConclusion" clearable placeholder="请选择结论" style="width: 140px">
          <el-option v-for="item in conclusionOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <template v-if="showAdvancedSearch">
        <el-form-item label="变更编号" prop="prNumber">
          <el-input v-model="queryParams.prNumber" placeholder="请输入变更编号" clearable style="width: 140px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="发起人" prop="prAuthor">
          <el-input v-model="queryParams.prAuthor" placeholder="Git 账号" clearable style="width: 150px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="完成时间">
          <el-date-picker v-model="dateRange" type="daterange" range-separator="-" start-placeholder="开始日期"
            end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 240px" />
        </el-form-item>
      </template>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button link type="primary" @click="showAdvancedSearch = !showAdvancedSearch">
          {{ showAdvancedSearch ? '收起筛选' : '更多筛选' }}
        </el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table class="record-table" v-loading="loading" :data="recordList" empty-text="暂无审查记录">
      <el-table-column label="项目名称" min-width="130">
        <template #default="scope">
          <div class="project-cell">
            <span class="project-name" :title="scope.row.projectName || ''">{{ recordDisplayValue(scope.row.projectName) }}</span>
            <span class="system-name" :title="scope.row.businessSystemName || ''">{{ recordDisplayValue(scope.row.businessSystemName) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="变更来源" min-width="220">
        <template #default="scope">
          <template v-if="isPushTask(scope.row)">
            <div class="source-cell">
              <div class="source-head">
                <el-tag size="small" type="success">{{ changeSourceLabel(scope.row) }}</el-tag>
                <span class="source-ref">{{ pushRefDisplay(scope.row) }}</span>
              </div>
              <span class="source-title" :title="scope.row.prTitle || ''">{{ recordDisplayValue(scope.row.prTitle) }}</span>
            </div>
          </template>
          <a v-else-if="mergeRequestUrl(scope.row)" class="source-link" :href="mergeRequestUrl(scope.row)" target="_blank" rel="noopener noreferrer"
            :title="'打开 ' + changeSourceLabel(scope.row) + ' #' + scope.row.prNumber">
            <div class="source-head">
              <el-tag size="small" type="info">{{ changeSourceLabel(scope.row) }}</el-tag>
              <span class="source-ref">#{{ scope.row.prNumber }}</span>
            </div>
            <span class="source-title" :title="scope.row.prTitle || ''">{{ recordDisplayValue(scope.row.prTitle) }}</span>
          </a>
          <div v-else class="source-cell">
            <div class="source-head">
              <el-tag size="small" type="info">{{ changeSourceLabel(scope.row) }}</el-tag>
              <span class="source-ref">#{{ scope.row.prNumber }}</span>
            </div>
              <span class="source-title" :title="scope.row.prTitle || ''">{{ recordDisplayValue(scope.row.prTitle) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="代码变更" width="150" align="center">
        <template #default="scope">
          <div v-if="hasChangeMetrics(scope.row)" class="change-summary" :aria-label="changeSummaryLabel(scope.row)">
            <span class="change-metric change-metric-files">
              <span class="change-label">文件</span>
              <strong class="change-value">{{ changeMetricValue(scope.row.changedFiles, '', scope.row) }}</strong>
            </span>
            <span class="change-separator">·</span>
            <span class="change-metric change-metric-additions">
              <span class="change-label">新增</span>
              <strong class="change-value">{{ changeMetricValue(scope.row.additions, '+', scope.row) }}</strong>
            </span>
            <span class="change-separator">·</span>
            <span class="change-metric change-metric-deletions">
              <span class="change-label">删除</span>
              <strong class="change-value">{{ changeMetricValue(scope.row.deletions, '-', scope.row) }}</strong>
            </span>
          </div>
          <span v-else :class="['metric-state', metricStateClass(scope.row)]">{{ metricUnavailableLabel(scope.row) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="审查结论" width="100">
        <template #default="scope">
          <el-tooltip v-if="scope.row.taskStatus === 'FAILED'" :content="readableFailureMessage(scope.row.failureMessage)" placement="top">
            <el-tag :type="recordConclusionTagType(scope.row)" size="small">{{ recordConclusionLabel(scope.row) }}</el-tag>
          </el-tooltip>
          <el-tag v-else :type="recordConclusionTagType(scope.row)" size="small">{{ recordConclusionDisplay(scope.row) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="评分" width="70" align="center">
        <template #default="scope">
          <span :class="['metric-state', metricStateClass(scope.row)]">{{ scoreDisplay(scope.row) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="重点问题" min-width="180">
        <template #default="scope">
          <template v-if="focusIssueMetrics[scope.row.taskId]?.hasBreakdown">
            <div class="focus-counts" title="基于结构化 Top 3 重点问题分级统计，非全量问题" aria-label="重点问题分级统计">
              <span class="focus-count focus-count-critical">
                <span>严重</span><strong>{{ focusIssueMetrics[scope.row.taskId].critical }}</strong>
              </span>
              <span class="focus-count-divider">·</span>
              <span class="focus-count focus-count-high">
                <span>高</span><strong>{{ focusIssueMetrics[scope.row.taskId].high }}</strong>
              </span>
              <span class="focus-count-divider">·</span>
              <span class="focus-count focus-count-medium">
                <span>中</span><strong>{{ focusIssueMetrics[scope.row.taskId].medium }}</strong>
              </span>
              <span class="focus-count-divider">·</span>
              <span class="focus-count focus-count-low">
                <span>低</span><strong>{{ focusIssueMetrics[scope.row.taskId].low }}</strong>
              </span>
            </div>
          </template>
          <span v-else-if="focusIssueMetrics[scope.row.taskId]?.total > 0" class="focus-total">
            {{ focusIssueMetrics[scope.row.taskId].total }} 个重点问题
          </span>
          <span v-else-if="hasIssueData(scope.row)" class="metric-state metric-state-empty">暂无问题</span>
          <span v-else :class="['metric-state', metricStateClass(scope.row)]">{{ metricUnavailableLabel(scope.row) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="完成时间" width="125">
        <template #default="scope">{{ recordDateTime(scope.row.finishedTime) }}</template>
      </el-table-column>
      <el-table-column label="发起人" width="100" :show-overflow-tooltip="true">
        <template #default="scope">{{ recordDisplayValue(scope.row.prAuthor) }}</template>
      </el-table-column>
      <el-table-column label="分支" min-width="160">
        <template #default="scope">
          <span class="branch-flow" :title="`${recordDisplayValue(scope.row.sourceBranch)} → ${recordDisplayValue(scope.row.targetBranch)}`">
            {{ recordDisplayValue(scope.row.sourceBranch) }} → {{ recordDisplayValue(scope.row.targetBranch) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="190" fixed="right">
        <template #default="scope">
          <div class="record-actions">
            <el-button link type="primary" v-hasPermi="['review:record:query']" @click="handleDetail(scope.row)">查看详情</el-button>
            <el-button
              v-if="hasReviewIssues(scope.row)"
              link
              type="primary"
              v-hasPermi="['review:record:query']"
              @click="handleViewIssues(scope.row)"
            >查看问题</el-button>
            <el-button
              v-if="!isPushTask(scope.row)"
              link type="primary"
              :disabled="!mergeRequestUrl(scope.row)"
              @click="openMergeRequest(scope.row)"
            >打开 {{ mergeRequestLabel(scope.row.provider) }}</el-button>
            <el-button
              v-if="scope.row.taskStatus === 'FAILED'"
              link type="primary"
              v-hasPermi="['review:task:retry']"
              @click="handleRetry(scope.row)"
            >重新执行</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup name="ReviewRecord">
import { listReviewRecord } from '@/api/review/record'
import { listReviewProject } from '@/api/review/project'
import { retryReviewTask } from '@/api/review/task'
import {
  formatScore, countFocusIssuesBySeverity,
  recordConclusionLabel, recordConclusionTagType, buildMergeRequestUrl, mergeRequestLabel, changeSourceLabel, formatDateTime,
  isPushTask, formatPushRefDisplay, readableFailureMessage
} from '@/utils/reviewDisplay'

const route = useRoute()
const { proxy } = getCurrentInstance()
const { review_event_source } = proxy.useDict('review_event_source')

const conclusionOptions = [
  { label: '通过', value: 'PASS' },
  { label: '建议修改', value: 'WARN' },
  { label: '高风险', value: 'BLOCK' },
  { label: '执行失败', value: 'FAILED' }
]

const recordList = ref([])
const projectOptions = ref([])
const loading = ref(true)
const showSearch = ref(true)
const showAdvancedSearch = ref(false)
const total = ref(0)
const dateRange = ref([])

const sourceOptions = computed(() => review_event_source.value || [])

const focusIssueMetrics = computed(() => {
  const metrics = {}
  recordList.value.forEach(row => {
    if (!row || row.taskStatus === 'FAILED') return
    const counts = countFocusIssuesBySeverity(row.topIssuesJson)
    if (counts.total > 0) {
      metrics[row.taskId] = { ...counts, hasBreakdown: true }
    } else if (row.focusIssueCount != null) {
      metrics[row.taskId] = { total: row.focusIssueCount, hasBreakdown: false }
    }
  })
  return metrics
})

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    projectId: undefined,
    prNumber: undefined,
    eventSource: undefined,
    prAuthor: undefined,
    reviewConclusion: undefined
  }
})
const { queryParams } = toRefs(data)

function getList() {
  loading.value = true
  listReviewRecord(proxy.addDateRange(queryParams.value, dateRange.value)).then(response => {
    recordList.value = response.rows || []
    total.value = response.total || 0
  }).finally(() => { loading.value = false })
}

function loadProjects() {
  listReviewProject({ pageNum: 1, pageSize: 100 }).then(response => {
    projectOptions.value = response.rows || []
  })
}

function handleDetail(row) {
  proxy.$router.push('/review/record-detail/index/' + row.taskId)
}

function handleViewIssues(row) {
  proxy.$router.push({ path: '/review/record-detail/index/' + row.taskId, query: { focus: 'issues' } })
}

function mergeRequestUrl(row) {
  return buildMergeRequestUrl(row)
}

function openMergeRequest(row) {
  const url = buildMergeRequestUrl(row)
  if (!url) {
    proxy.$modal.msgWarning('暂无法生成来源链接')
    return
  }
  window.open(url, '_blank', 'noopener,noreferrer')
}

function handleRetry(row) {
  proxy.$modal.confirm('确认重新执行该失败任务？历史执行记录将保留。').then(() => {
    return retryReviewTask(row.taskId)
  }).then(() => {
    proxy.$modal.msgSuccess('已提交重新执行')
    getList()
  }).catch(() => {})
}

function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); dateRange.value = []; handleQuery() }

function changeMetricValue(value, prefix = '', row) {
  return value == null ? metricUnavailableLabel(row) : `${prefix}${value}`
}

function recordDisplayValue(value) {
  return value == null || value === '' || value === '—' || value === '--' ? '暂无数据' : value
}

function recordDateTime(value) {
  return recordDisplayValue(formatDateTime(value))
}

function pushRefDisplay(row) {
  return recordDisplayValue(formatPushRefDisplay(row)).replaceAll('—', '暂无数据')
}

function hasChangeMetrics(row) {
  return row?.changedFiles != null || row?.additions != null || row?.deletions != null
}

function changeSummaryLabel(row) {
  return `文件数 ${changeMetricValue(row?.changedFiles, '', row)}，新增行 ${changeMetricValue(row?.additions, '+', row)}，删除行 ${changeMetricValue(row?.deletions, '-', row)}`
}

function metricUnavailableLabel(row) {
  return row?.taskStatus === 'FAILED' ? '未生成' : '暂无数据'
}

function metricStateClass(row) {
  return row?.taskStatus === 'FAILED' ? 'metric-state-failed' : 'metric-state-empty'
}

function scoreDisplay(row) {
  if (row?.taskStatus === 'FAILED') return '未生成'
  return row?.totalScore == null ? '暂无数据' : formatScore(row.totalScore)
}

function recordConclusionDisplay(row) {
  const label = recordConclusionLabel(row)
  return label === '--' ? metricUnavailableLabel(row) : label
}

function parseTopIssues(row) {
  if (Array.isArray(row?.topIssuesJson)) return row.topIssuesJson
  if (typeof row?.topIssuesJson !== 'string' || !row.topIssuesJson.trim()) return []
  try {
    const parsed = JSON.parse(row.topIssuesJson)
    return Array.isArray(parsed) ? parsed : []
  } catch (e) {
    return []
  }
}

function hasReviewIssues(row) {
  if (!row || row.taskStatus === 'FAILED') return false
  return parseTopIssues(row).length > 0 || Number(row.focusIssueCount) > 0
}

function hasIssueData(row) {
  return row?.taskStatus !== 'FAILED' && (row?.focusIssueCount != null || row?.topIssuesJson != null)
}

function applyRouteQuery() {
  const q = route.query || {}
  if (q.reviewConclusion) queryParams.value.reviewConclusion = String(q.reviewConclusion)
  if (q.projectId) queryParams.value.projectId = Number(q.projectId) || q.projectId
  if (q.prNumber) queryParams.value.prNumber = Number(q.prNumber) || q.prNumber
  if (q.eventSource) queryParams.value.eventSource = String(q.eventSource)
  if (q.prAuthor) queryParams.value.prAuthor = String(q.prAuthor)
  if (q.beginTime || q.endTime) {
    dateRange.value = [q.beginTime ? String(q.beginTime) : '', q.endTime ? String(q.endTime) : '']
  }
}

loadProjects()
// 首次进入：TagsView 写入 cachedViews 可能晚于页面首次渲染，未被 keep-alive 缓存的组件
// 不触发 onActivated，首次加载必须在 onMounted 完成；
// 重入 tab：keep-alive 不重跑 onMounted，工作台卡片 query 回填依赖 onActivated
onMounted(() => {
  applyRouteQuery()
  getList()
})
let firstActivated = true
onActivated(() => {
  // 首次挂载紧随的激活已在 onMounted 加载，跳过避免重复请求
  if (firstActivated) { firstActivated = false; return }
  applyRouteQuery()
  getList()
})
</script>

<style scoped>
.project-cell { display: flex; flex-direction: column; gap: 2px; line-height: 1.4; }
.project-name { font-weight: 500; color: var(--el-text-color-primary); }
.system-name { font-size: 12px; color: var(--el-text-color-secondary); }
.source-cell, .source-link {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 3px;
  min-width: 0;
}
.source-link {
  color: var(--el-color-primary);
  text-decoration: none;
}
.source-link:hover .source-title { text-decoration: underline; }
.source-head { display: flex; align-items: center; gap: 5px; max-width: 100%; min-width: 0; }
.source-ref { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--el-text-color-secondary); font-size: 12px; }
.source-title { display: block; max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.branch-flow { color: var(--el-text-color-regular); }
.record-actions { display: flex; flex-wrap: wrap; align-items: center; gap: 0 6px; }
.change-summary {
  display: inline-flex;
  align-items: baseline;
  justify-content: center;
  gap: 4px;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}
.change-metric {
  display: inline-flex;
  align-items: baseline;
  gap: 2px;
}
.change-value {
  font-size: 12px;
  font-weight: 600;
}
.change-label,
.change-separator {
  font-family: var(--el-font-family);
  font-size: 10px;
  color: var(--el-text-color-secondary);
}
.change-separator { color: var(--el-border-color); }
.change-metric-files .change-value { color: var(--el-text-color-regular); }
.change-metric-additions .change-value { color: var(--el-color-success); }
.change-metric-deletions .change-value { color: var(--el-color-danger); }
.focus-counts {
  display: inline-flex;
  align-items: baseline;
  gap: 3px;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}
.focus-count {
  display: inline-flex;
  align-items: baseline;
  gap: 2px;
  font-size: 10px;
  color: var(--el-text-color-secondary);
}
.focus-count strong {
  font-size: 11px;
  font-weight: 600;
  color: var(--focus-color);
}
.focus-count-critical { --focus-color: var(--el-color-danger); }
.focus-count-high { --focus-color: var(--el-color-warning); }
.focus-count-medium { --focus-color: var(--el-color-primary); }
.focus-count-low { --focus-color: var(--el-color-info); }
.focus-count-divider {
  color: var(--el-border-color);
  font-size: 10px;
}
.focus-total {
  color: var(--el-text-color-regular);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}
.metric-state { font-size: 12px; white-space: nowrap; }
.metric-state-empty { color: var(--el-text-color-placeholder); }
.metric-state-failed { color: var(--el-color-danger); }
</style>
