<template>
  <div class="app-container">
    <div class="page-toolbar">
      <el-page-header @back="goBack" content="审查记录详情" />
      <div class="toolbar-actions">
        <el-button v-if="mergeRequestLink" @click="openMergeRequest">打开 {{ mergeRequestLinkLabel }}</el-button>
        <el-button
          v-if="detailTask?.taskStatus === 'FAILED'"
          type="primary"
          v-hasPermi="['review:task:retry']"
          @click="handleRetry"
        >重新执行</el-button>
      </div>
    </div>

    <el-alert v-if="detailError" :title="detailError" type="warning" :closable="false" show-icon class="mb12" />

    <div v-loading="detailLoading">
      <template v-if="detailTask">
        <section class="detail-section">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="项目">
              {{ recordDisplayValue(detailTask.projectName) }}
              <span v-if="detailTask.businessSystemName" class="system-inline">（{{ detailTask.businessSystemName }}）</span>
            </el-descriptions-item>
            <el-descriptions-item label="变更来源">
              <el-tag :type="isPushTask(detailTask) ? 'success' : 'info'" size="small">{{ changeSourceLabel(detailTask) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="发起人">{{ recordDisplayValue(detailTask.prAuthor) }}</el-descriptions-item>
            <el-descriptions-item v-if="!isPushTask(detailTask)" label="来源详情">
              <a v-if="mergeRequestLink" :href="mergeRequestLink" target="_blank" rel="noopener noreferrer" class="pr-link">
                <el-tag size="small" type="info" class="mr-tag">{{ mergeRequestLinkLabel }}</el-tag>
                #{{ detailTask.prNumber }} {{ recordDisplayValue(detailTask.prTitle) }}
              </a>
              <span v-else>
                <el-tag size="small" type="info" class="mr-tag">{{ mergeRequestLinkLabel }}</el-tag>
                #{{ detailTask.prNumber }} {{ recordDisplayValue(detailTask.prTitle) }}
              </span>
            </el-descriptions-item>
            <el-descriptions-item v-else label="来源详情">
              {{ pushRefDisplay(detailTask) }}
              <span v-if="detailTask.prTitle" class="push-title"> · {{ detailTask.prTitle }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="分支">{{ recordDisplayValue(detailTask.sourceBranch) }} → {{ recordDisplayValue(detailTask.targetBranch) }}</el-descriptions-item>
            <el-descriptions-item label="代码变更">
              <div v-if="hasChangeMetrics(detailTask)" class="change-summary" :aria-label="changeSummaryLabel(detailTask)">
                <span class="change-metric change-metric-files">
                  <span class="change-label">文件</span>
                  <strong class="change-value">{{ changeMetricValue(detailTask.changedFiles, '', detailTask) }}</strong>
                </span>
                <span class="change-separator">·</span>
                <span class="change-metric change-metric-additions">
                  <span class="change-label">新增</span>
                  <strong class="change-value">{{ changeMetricValue(detailTask.additions, '+', detailTask) }}</strong>
                </span>
                <span class="change-separator">·</span>
                <span class="change-metric change-metric-deletions">
                  <span class="change-label">删除</span>
                  <strong class="change-value">{{ changeMetricValue(detailTask.deletions, '-', detailTask) }}</strong>
                </span>
              </div>
              <span v-else :class="['metric-state', metricStateClass(detailTask)]">{{ metricUnavailableLabel(detailTask) }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="审查结论">
              <div class="conclusion-cell">
                <el-tag :type="recordConclusionTagType(detailTask)" size="small">{{ recordConclusionDisplay(detailTask) }}</el-tag>
                <span v-if="isPushTask(detailTask)" class="push-scope-note">{{ PUSH_SCOPE_NOTE }}</span>
              </div>
            </el-descriptions-item>
            <el-descriptions-item label="完成时间">{{ recordDateTime(detailTask.finishedTime) }}</el-descriptions-item>
          </el-descriptions>
        </section>

        <DeliveryStatusView
          :delivery="detailDelivery"
          :task-id="detailTask.taskId"
          :task-status="detailTask.taskStatus"
          :event-source="detailTask.eventSource"
          :inline-deliveries="detailInlineDeliveries"
          @retried="loadDetail"
        />

        <ImDeliveryStatusView :task-id="detailTask.taskId" />

        <el-tabs v-model="activeTab" class="record-tabs">
          <el-tab-pane label="审查结果" name="result">
            <el-alert v-if="detailTask.taskStatus === 'FAILED'" class="mb12" type="error" :closable="false" show-icon
              :title="'执行失败：' + readableFailureMessage(detailTask.failureMessage)" />
            <div v-if="resultRun && detailTask.taskStatus === 'SUCCESS'" class="result-pane">
              <div class="score-panel">
                <div class="score-total">
                  <span class="score-total-label">总分</span>
                  <span class="score-total-value">{{ scoreDisplay(detailTask, resultRun) }}</span>
                  <span v-if="hasReviewScore(detailTask, resultRun)" class="score-total-unit">/ 100</span>
                  <el-tag :type="recordConclusionTagType(detailTask)" size="small" class="conclusion-tag">
                    {{ recordConclusionDisplay(detailTask) }}
                  </el-tag>
                </div>

                <div v-if="showStructuredResult(resultRun)" class="score-dimensions">
                  <div v-for="dim in getScoreDimensions(resultRun)" :key="dim.dimension" class="score-dim-row">
                    <div class="score-dim-head">
                      <span class="score-dim-name">{{ dim.label }}</span>
                      <span class="score-dim-score">{{ formatScore(dim.score) }} / {{ dim.maxScore }}</span>
                    </div>
                    <div v-if="dim.reason" class="score-dim-reason">{{ dim.reason }}</div>
                  </div>
                </div>

                <div class="result-block">
                  <div class="result-block-title">审查摘要</div>
                  <p class="result-summary">{{ getReviewSummary(resultRun) || '暂无审查摘要' }}</p>
                </div>

                <div id="focus-issues" class="result-block">
                  <div class="result-block-title">重点问题</div>
                  <el-empty v-if="!newIssues.length" description="暂无重点问题" :image-size="48" />
                  <div v-else class="issue-list">
                    <div v-for="issue in displayedNewIssues" :key="issue.rank || issue.title" class="issue-card">
                      <div class="issue-head">
                        <span class="issue-rank">#{{ issue.rank || '未编号' }}</span>
                        <el-tag v-if="issue.severity" :type="severityTagType(issue.severity)" size="small">
                          {{ severityLabel(issue.severity) }}
                        </el-tag>
                        <el-tag v-if="issueOriginLabel(issue.origin)" :type="issueOriginTagType(issue.origin)" size="small" effect="plain">
                          {{ issueOriginLabel(issue.origin) }}
                        </el-tag>
                        <dict-tag v-if="issue.dispositionStatus" :options="review_issue_status" :value="issue.dispositionStatus" />
                        <span v-if="issue.category" class="issue-category">{{ issue.category }}</span>
                        <strong class="issue-title">{{ recordDisplayValue(issue.title) }}</strong>
                        <el-button v-if="issue.issueId" link type="primary" size="small" class="issue-ledger-link"
                          @click="goIssueLedger(issue.issueId)">台账</el-button>
                      </div>
                      <p v-if="issue.description" class="issue-text">{{ issue.description }}</p>
                      <div v-if="issue.filePath || formatIssueLines(issue)" class="issue-locate">
                        <code v-if="issue.filePath">{{ issue.filePath }}</code>
                        <span v-if="formatIssueLines(issue)" class="issue-lines">{{ formatIssueLines(issue) }}</span>
                      </div>
                      <div v-if="issue.suggestion" class="issue-suggestion">
                        <span class="issue-field-label">建议</span>
                        <p class="issue-text">{{ issue.suggestion }}</p>
                      </div>
                    </div>
                    <el-button
                      v-if="newIssues.length > 3"
                      link
                      type="primary"
                      class="issue-more-link"
                      @click="goIssueLedgerByRecord"
                    >共 {{ newIssues.length }} 个问题，其余见问题台账</el-button>
                  </div>
                </div>

                <div v-if="existingIssues.length" class="result-block">
                  <div class="result-block-title">
                    存量问题（{{ existingIssues.length }}，不影响评分与结论，仅参考）
                  </div>
                  <div class="issue-list">
                    <div v-for="issue in existingIssues" :key="'existing-' + (issue.rank || issue.title)" class="issue-card issue-card-existing">
                      <div class="issue-head">
                        <el-tag v-if="issue.severity" :type="severityTagType(issue.severity)" size="small">
                          {{ severityLabel(issue.severity) }}
                        </el-tag>
                        <el-tag type="info" size="small" effect="plain">存量</el-tag>
                        <dict-tag v-if="issue.dispositionStatus" :options="review_issue_status" :value="issue.dispositionStatus" />
                        <span v-if="issue.category" class="issue-category">{{ issue.category }}</span>
                        <strong class="issue-title">{{ recordDisplayValue(issue.title) }}</strong>
                        <el-button v-if="issue.issueId" link type="primary" size="small" class="issue-ledger-link"
                          @click="goIssueLedger(issue.issueId)">台账</el-button>
                      </div>
                      <p v-if="issue.description" class="issue-text">{{ issue.description }}</p>
                      <div v-if="issue.filePath || formatIssueLines(issue)" class="issue-locate">
                        <code v-if="issue.filePath">{{ issue.filePath }}</code>
                        <span v-if="formatIssueLines(issue)" class="issue-lines">{{ formatIssueLines(issue) }}</span>
                      </div>
                      <div v-if="issue.suggestion" class="issue-suggestion">
                        <span class="issue-field-label">建议</span>
                        <p class="issue-text">{{ issue.suggestion }}</p>
                      </div>
                    </div>
                  </div>
                </div>

                <div class="result-block">
                  <div class="result-block-title">提交说明</div>
                  <pre class="commit-pre">{{ resultRun.commitMessages || '暂无提交说明' }}</pre>
                </div>
              </div>
            </div>
            <el-empty v-else-if="detailTask.taskStatus !== 'FAILED'" description="暂无可用的审查结果" :image-size="64" />
            <div v-else id="focus-issues" class="result-block">
              <div class="result-block-title">重点问题</div>
              <p class="result-summary">执行失败，评分与重点问题未生成。</p>
            </div>
          </el-tab-pane>

          <el-tab-pane label="执行记录" name="runs">
            <p class="section-hint">技术排障信息：状态、模型/引擎、模板、SHA、耗时、范围决策与失败原因。</p>
            <el-empty v-if="!detailRuns.length" description="暂无执行记录" :image-size="64" />
            <el-table v-else :data="detailRuns" border>
              <el-table-column type="expand">
                <template #default="scope">
                  <div class="run-expand">
                    <div class="run-expand-title">范围决策快照</div>
                    <ScopeDecisionView :run="scope.row" />
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="次数" prop="attemptNo" width="70" align="center" />
              <el-table-column label="状态" width="100">
                <template #default="scope">
                  <dict-tag :options="review_task_status" :value="scope.row.runStatus" />
                </template>
              </el-table-column>
              <el-table-column label="模型 / 引擎" min-width="160">
                <template #default="scope">{{ readableValue(engineOrModelLabel(scope.row)) }}</template>
              </el-table-column>
              <el-table-column label="模板" min-width="140">
                <template #default="scope">{{ readableValue(templateLabel(scope.row)) }}</template>
              </el-table-column>
              <el-table-column label="SHA" min-width="150">
                <template #default="scope">{{ readableValue(shortSha(scope.row.snapshotBaseSha)) }} → {{ readableValue(shortSha(scope.row.snapshotHeadSha)) }}</template>
              </el-table-column>
              <el-table-column label="耗时" width="100">
                <template #default="scope">{{ readableValue(formatDuration(scope.row.durationMs)) }}</template>
              </el-table-column>
              <el-table-column label="失败原因" min-width="180">
                <template #default="scope">
                  <span v-if="scope.row.runStatus === 'FAILED'" class="failure-message">
                    {{ readableFailureMessage(scope.row.failureMessage) }}
                  </span>
                  <span v-else class="empty-tip">无</span>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </template>
    </div>
  </div>
</template>

<script setup name="ReviewRecordDetail">
import { getReviewRecord } from '@/api/review/record'
import { retryReviewTask } from '@/api/review/task'
import { listInlineDeliveriesByTask } from '@/api/review/delivery'
import DeliveryStatusView from '@/views/review/components/DeliveryStatusView.vue'
import ImDeliveryStatusView from '@/views/review/components/ImDeliveryStatusView.vue'
import ScopeDecisionView from '@/views/review/components/ScopeDecisionView.vue'
import {
  formatScore, formatDuration, shortSha,
  showStructuredResult, getScoreDimensions, getReviewSummary, getTopIssues,
  severityLabel, severityTagType, formatIssueLines, pickLatestSuccessRun,
  engineOrModelLabel, templateLabel, buildMergeRequestUrl, mergeRequestLabel, changeSourceLabel,
  recordConclusionLabel, recordConclusionTagType,
  issueOriginLabel, issueOriginTagType, formatDateTime,
  isPushTask, formatPushRefDisplay, readableFailureMessage, PUSH_SCOPE_NOTE
} from '@/utils/reviewDisplay'

const { proxy } = getCurrentInstance()
const route = useRoute()
const { review_task_status, review_issue_status } = proxy.useDict('review_task_status', 'review_issue_status')

const detailLoading = ref(false)
const detailTask = ref(null)
const detailRuns = ref([])
const detailDelivery = ref(null)
const detailInlineDeliveries = ref([])
const detailError = ref('')
const activeTab = ref('result')

const taskId = computed(() => route.params.taskId)
const resultRun = computed(() => pickLatestSuccessRun(detailRuns.value))
const topIssues = computed(() => getTopIssues(resultRun.value))
const newIssues = computed(() => topIssues.value.filter(issue => (issue?.origin || '').toUpperCase() !== 'EXISTING'))
const displayedNewIssues = computed(() => newIssues.value.slice(0, 3))
const existingIssues = computed(() => topIssues.value.filter(issue => (issue?.origin || '').toUpperCase() === 'EXISTING'))
const mergeRequestLink = computed(() => buildMergeRequestUrl(detailTask.value || {}))
const mergeRequestLinkLabel = computed(() => mergeRequestLabel(detailTask.value?.provider))

function metricUnavailableLabel(row) {
  return row?.taskStatus === 'FAILED' ? '未生成' : '暂无数据'
}

function metricStateClass(row) {
  return row?.taskStatus === 'FAILED' ? 'metric-state-failed' : 'metric-state-empty'
}

function recordConclusionDisplay(row) {
  const label = recordConclusionLabel(row)
  return label === '--' ? metricUnavailableLabel(row) : label
}

function scoreDisplay(row, run) {
  if (row?.taskStatus === 'FAILED') return '未生成'
  const value = run?.totalScore ?? row?.totalScore
  return value == null ? '未评分' : formatScore(value)
}

function hasReviewScore(row, run) {
  return (run?.totalScore ?? row?.totalScore) != null
}

function changeMetricValue(value, prefix = '', row) {
  return value == null ? metricUnavailableLabel(row) : `${prefix}${value}`
}

function hasChangeMetrics(row) {
  return row?.changedFiles != null || row?.additions != null || row?.deletions != null
}

function changeSummaryLabel(row) {
  return `文件数 ${changeMetricValue(row?.changedFiles, '', row)}，新增行 ${changeMetricValue(row?.additions, '+', row)}，删除行 ${changeMetricValue(row?.deletions, '-', row)}`
}

function readableValue(value) {
  return value == null || value === '' || value === '—' ? '暂无数据' : value
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

function loadDetail() {
  if (!taskId.value) {
    detailError.value = '缺少记录 ID'
    return
  }
  detailLoading.value = true
  detailError.value = ''
  getReviewRecord(taskId.value).then(response => {
    const payload = response.data || {}
    detailTask.value = payload.task || null
    detailRuns.value = payload.runs || []
    detailDelivery.value = payload.delivery || null
    if (!detailTask.value) detailError.value = '未获取到审查记录'
    loadInlineDeliveries()
    nextTick(() => focusIssuesIfNeeded())
  }).catch(error => {
    detailError.value = error?.message || '详情加载失败'
  }).finally(() => { detailLoading.value = false })
}

function loadInlineDeliveries() {
  const id = detailTask.value?.taskId || taskId.value
  if (!id || isPushTask(detailTask.value)) {
    detailInlineDeliveries.value = []
    return
  }
  listInlineDeliveriesByTask(id).then(response => {
    detailInlineDeliveries.value = response.data || []
  }).catch(() => {
    detailInlineDeliveries.value = []
  })
}

function focusIssuesIfNeeded() {
  if (route.query.focus !== 'issues') return
  activeTab.value = 'result'
  nextTick(() => {
    const el = document.getElementById('focus-issues')
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
}

function goBack() {
  proxy.$router.push('/review/record')
}

function openMergeRequest() {
  if (mergeRequestLink.value) window.open(mergeRequestLink.value, '_blank', 'noopener,noreferrer')
}

function goIssueLedger(issueId) {
  proxy.$router.push({
    path: '/review/issue',
    query: { reviewTaskId: String(taskId.value), issueId: String(issueId) }
  })
}

function goIssueLedgerByRecord() {
  proxy.$router.push({
    path: '/review/issue',
    query: { reviewTaskId: String(taskId.value) }
  })
}

function handleRetry() {
  proxy.$modal.confirm('确认重新执行该失败任务？历史执行记录将保留。').then(() => {
    return retryReviewTask(detailTask.value.taskId)
  }).then(() => {
    proxy.$modal.msgSuccess('已提交重新执行')
    proxy.$router.push('/review/task')
  }).catch(() => {})
}

watch(taskId, () => loadDetail(), { immediate: true })
watch(() => route.query.focus, () => focusIssuesIfNeeded())
</script>

<style scoped>
.page-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.toolbar-actions { display: flex; align-items: center; gap: 8px; }
.system-inline { color: var(--el-text-color-secondary); font-size: 13px; }
.pr-link { color: var(--el-color-primary); text-decoration: none; }
.pr-link:hover { text-decoration: underline; }
.mr-tag { margin-right: 6px; vertical-align: middle; }
.push-title { color: var(--el-text-color-secondary); font-size: 13px; }
.change-summary {
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}
.change-metric { display: inline-flex; align-items: baseline; gap: 3px; }
.change-value { font-size: 13px; font-weight: 600; }
.change-label,
.change-separator { font-family: var(--el-font-family); font-size: 11px; color: var(--el-text-color-secondary); }
.change-separator { color: var(--el-border-color); }
.change-metric-files .change-value { color: var(--el-text-color-regular); }
.change-metric-additions .change-value { color: var(--el-color-success); }
.change-metric-deletions .change-value { color: var(--el-color-danger); }
.mb12 { margin-bottom: 12px; }
.detail-section { margin-bottom: 20px; }
.section-hint { margin: 0 0 12px; font-size: 13px; color: var(--el-text-color-secondary); }
.record-tabs { margin-top: 4px; }
.score-panel { max-width: 960px; }
.score-total {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}
.score-total-label { font-size: 13px; color: var(--el-text-color-secondary); }
.score-total-value {
  font-size: 28px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  font-variant-numeric: tabular-nums;
}
.score-total-unit { font-size: 13px; color: var(--el-text-color-secondary); }
.conclusion-tag { margin-left: 8px; }
.conclusion-cell {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.push-scope-note {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}
.score-dimensions { display: flex; flex-direction: column; gap: 10px; margin-bottom: 16px; }
.score-dim-row {
  padding: 10px 12px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
}
.score-dim-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.score-dim-name { font-size: 13px; font-weight: 500; }
.score-dim-score { font-size: 13px; font-weight: 600; font-variant-numeric: tabular-nums; white-space: nowrap; }
.score-dim-reason { margin-top: 6px; font-size: 13px; line-height: 1.6; color: var(--el-text-color-secondary); }
.result-block { margin-bottom: 16px; }
.result-block-title { margin-bottom: 8px; font-size: 13px; font-weight: 600; }
.result-summary { margin: 0; font-size: 13px; line-height: 1.7; white-space: pre-wrap; }
.issue-list { display: flex; flex-direction: column; gap: 12px; }
.issue-card {
  padding: 12px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-left: 3px solid var(--el-color-primary-light-5);
  border-radius: 6px;
}
.issue-card-existing { border-left-color: var(--el-border-color); background: var(--el-fill-color-light); }
.run-expand { padding: 8px 12px; }
.run-expand-title { margin-bottom: 8px; font-size: 13px; font-weight: 600; }
.issue-head { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-bottom: 6px; }
.issue-rank { font-size: 12px; font-weight: 600; color: var(--el-text-color-secondary); }
.issue-category { font-size: 12px; color: var(--el-text-color-secondary); }
.issue-title { font-size: 14px; font-weight: 600; }
.issue-ledger-link { margin-left: auto; }
.issue-more-link { align-self: flex-start; margin-top: 4px; padding: 0; }
.issue-text { margin: 0; font-size: 13px; line-height: 1.7; white-space: pre-wrap; }
.issue-locate { margin-top: 6px; font-size: 12px; color: var(--el-text-color-secondary); }
.issue-locate code { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; font-size: 12px; }
.issue-lines { margin-left: 8px; }
.issue-suggestion { margin-top: 8px; }
.issue-field-label { display: block; margin-bottom: 4px; font-size: 12px; font-weight: 500; color: var(--el-text-color-secondary); }
.commit-pre {
  margin: 0;
  padding: 10px 12px;
  max-height: 220px;
  overflow: auto;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
.failure-message { color: var(--el-color-danger); }
.empty-tip { color: var(--el-text-color-placeholder); }
.metric-state { font-size: 13px; white-space: nowrap; }
.metric-state-empty { color: var(--el-text-color-placeholder); }
.metric-state-failed { color: var(--el-color-danger); }
</style>
