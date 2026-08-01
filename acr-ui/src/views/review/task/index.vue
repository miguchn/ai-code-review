<template>
  <div class="app-container">
    <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch" label-width="84px">
      <el-form-item label="所属项目" prop="projectId">
        <el-select v-model="queryParams.projectId" clearable filterable placeholder="请选择项目" style="width: 220px">
          <el-option v-for="item in projectOptions" :key="item.projectId" :label="item.projectName" :value="item.projectId" />
        </el-select>
      </el-form-item>
      <el-form-item label="PR 编号" prop="prNumber">
        <el-input v-model="queryParams.prNumber" placeholder="请输入 PR 编号" clearable style="width: 140px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="任务状态" prop="taskStatus">
        <el-select v-model="queryParams.taskStatus" clearable placeholder="请选择状态" style="width: 130px">
          <el-option v-for="dict in review_task_status" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="触发时间">
        <el-date-picker v-model="dateRange" type="daterange" range-separator="-" start-placeholder="开始日期"
          end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 240px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="taskList" empty-text="暂无审查任务">
      <el-table-column label="任务 ID" prop="taskId" width="90" />
      <el-table-column label="所属项目" prop="projectName" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="Pull Request" min-width="200">
        <template #default="scope">
          <div class="pr-cell">
            <el-tag size="small" type="primary">#{{ scope.row.prNumber }}</el-tag>
            <span class="pr-title" :title="scope.row.prTitle">{{ scope.row.prTitle }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="分支" min-width="180">
        <template #default="scope">
          <span class="branch-flow">{{ scope.row.sourceBranch }} → {{ scope.row.targetBranch }}</span>
        </template>
      </el-table-column>
      <el-table-column label="任务状态" width="100">
        <template #default="scope">
          <dict-tag :options="review_task_status" :value="scope.row.taskStatus" />
        </template>
      </el-table-column>
      <el-table-column label="审查结论" width="100">
        <template #default="scope">
          <dict-tag v-if="scope.row.reviewConclusion" :options="review_conclusion" :value="scope.row.reviewConclusion" />
          <span v-else class="empty-tip">—</span>
        </template>
      </el-table-column>
      <el-table-column label="耗时" width="90">
        <template #default="scope">{{ formatDuration(scope.row.durationMs) }}</template>
      </el-table-column>
      <el-table-column label="触发时间" width="170">
        <template #default="scope">{{ formatDateTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="失败原因" min-width="140">
        <template #default="scope">
          <span v-if="scope.row.failureMessage" class="failure-message" :title="scope.row.failureMessage">{{ scope.row.failureMessage }}</span>
          <span v-else class="empty-tip">—</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="scope">
          <el-tooltip :content="detailActionTip(scope.row)" placement="top" :disabled="canOpenDetail()">
            <span class="action-wrap">
              <el-button link type="primary" :disabled="!canOpenDetail()" @click="handleDetailClick(scope.row)">详情</el-button>
            </span>
          </el-tooltip>
          <el-tooltip :content="retryActionTip(scope.row)" placement="top" :disabled="canRetry(scope.row)">
            <span class="action-wrap">
              <el-button link type="primary" :disabled="!canRetry(scope.row)" @click="handleRetryClick(scope.row)">
                {{ retryActionLabel(scope.row) }}
              </el-button>
            </span>
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer v-model="detailOpen" title="审查任务详情" size="680px" destroy-on-close>
      <div v-loading="detailLoading" class="detail-body">
        <el-alert v-if="detailError" :title="detailError" type="warning" :closable="false" show-icon class="mb12" />
        <template v-if="detailTask">
          <section class="detail-section">
            <h4>任务概览</h4>
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="任务 ID">{{ detailTask.taskId }}</el-descriptions-item>
              <el-descriptions-item label="项目">{{ detailTask.projectName }}</el-descriptions-item>
              <el-descriptions-item label="PR">#{{ detailTask.prNumber }} {{ detailTask.prTitle }}</el-descriptions-item>
              <el-descriptions-item label="分支">{{ detailTask.sourceBranch }} → {{ detailTask.targetBranch }}</el-descriptions-item>
              <el-descriptions-item label="Base SHA"><code>{{ detailTask.baseSha }}</code></el-descriptions-item>
              <el-descriptions-item label="Head SHA"><code>{{ detailTask.headSha }}</code></el-descriptions-item>
              <el-descriptions-item label="执行状态">
                <dict-tag :options="review_task_status" :value="detailTask.taskStatus" />
              </el-descriptions-item>
              <el-descriptions-item label="审查结论">
                <dict-tag v-if="detailTask.reviewConclusion" :options="review_conclusion" :value="detailTask.reviewConclusion" />
                <span v-else>—</span>
              </el-descriptions-item>
              <el-descriptions-item label="审查方式">
                <dict-tag v-if="detailTask.snapshotReviewMode" :options="review_mode" :value="normalizeMode(detailTask.snapshotReviewMode)" />
                <span v-else>—</span>
              </el-descriptions-item>
              <el-descriptions-item label="当前步骤">
                <dict-tag v-if="detailTask.currentStep" :options="review_task_step" :value="detailTask.currentStep" />
                <span v-else>—</span>
              </el-descriptions-item>
              <el-descriptions-item v-if="isLlmMode(detailTask.snapshotReviewMode)" label="审查模板" :span="2">
                {{ detailTask.snapshotTemplateName || '—' }}
                <span v-if="detailTask.snapshotTemplateCode">（{{ detailTask.snapshotTemplateCode }}）</span>
                <span v-if="detailTask.snapshotTemplateVersion != null"> · v{{ detailTask.snapshotTemplateVersion }}</span>
              </el-descriptions-item>
              <el-descriptions-item v-if="isLlmMode(detailTask.snapshotReviewMode)" label="模型配置" :span="2">
                {{ detailTask.snapshotModelName || '—' }}
                <span v-if="detailTask.snapshotModel">（{{ detailTask.snapshotModel }}）</span>
              </el-descriptions-item>
              <el-descriptions-item v-if="isOcrMode(detailTask.snapshotReviewMode)" label="审查引擎" :span="2">
                {{ engineLabel(detailTask.snapshotEngineCode) }}
                <span v-if="detailTask.snapshotEngineName"> · {{ detailTask.snapshotEngineName }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="耗时">{{ formatDuration(detailTask.durationMs) }}</el-descriptions-item>
              <el-descriptions-item label="失败分类" :span="2">
                <dict-tag v-if="detailTask.failureType" :options="review_task_failure_type" :value="detailTask.failureType" />
                <span v-else>—</span>
              </el-descriptions-item>
              <el-descriptions-item label="失败原因" :span="2">{{ detailTask.failureMessage || '—' }}</el-descriptions-item>
            </el-descriptions>
            <div class="detail-actions">
              <el-tooltip :content="retryActionTip(detailTask)" placement="top" :disabled="canRetry(detailTask)">
                <span class="action-wrap">
                  <el-button type="primary" :disabled="!canRetry(detailTask)" @click="handleRetryClick(detailTask)">
                    {{ retryActionLabel(detailTask) }}
                  </el-button>
                </span>
              </el-tooltip>
              <span v-if="!canRetry(detailTask)" class="action-hint">{{ retryActionTip(detailTask) }}</span>
            </div>
          </section>

          <section class="detail-section">
            <h4>执行记录</h4>
            <el-empty v-if="!detailRuns.length" description="暂无执行记录（待执行任务会在开始执行后出现记录）" :image-size="64" />
            <el-timeline v-else>
              <el-timeline-item v-for="run in detailRuns" :key="run.runId"
                :type="runTimelineType(run.runStatus)" :timestamp="formatDateTime(run.startedTime)">
                <div class="run-card">
                  <div class="run-head">
                    <strong>第 {{ run.attemptNo }} 次</strong>
                    <dict-tag :options="review_task_status" :value="run.runStatus" />
                    <span class="muted">{{ formatDuration(run.durationMs) }}</span>
                  </div>
                  <div class="run-meta">
                    <div>步骤：{{ stepLabel(run.currentStep) }}</div>
                    <div>审查方式：{{ modeLabel(normalizeMode(run.snapshotReviewMode)) }}</div>
                    <div v-if="isOcrMode(run.snapshotReviewMode)">
                      审查引擎：{{ engineLabel(run.snapshotEngineCode) }}
                      <span v-if="run.snapshotEngineVersion"> v{{ run.snapshotEngineVersion }}</span>
                    </div>
                    <div>SHA：{{ shortSha(run.snapshotBaseSha) }} → {{ shortSha(run.snapshotHeadSha) }}</div>
                    <div v-if="run.failureMessage" class="failure-message">失败：{{ run.failureMessage }}</div>
                  </div>

                  <template v-if="showStructuredResult(run)">
                    <div class="score-panel">
                      <div class="score-total">
                        <span class="score-total-label">总分</span>
                        <span class="score-total-value">{{ formatScore(run.totalScore) }}</span>
                        <span class="score-total-unit">/ 100</span>
                      </div>

                      <div class="score-dimensions">
                        <div v-for="dim in getScoreDimensions(run)" :key="dim.dimension" class="score-dim-row">
                          <div class="score-dim-head">
                            <span class="score-dim-name">{{ dim.label }}</span>
                            <span class="score-dim-score">{{ formatScore(dim.score) }} / {{ dim.maxScore }}</span>
                          </div>
                          <div v-if="dim.reason" class="score-dim-reason">{{ dim.reason }}</div>
                        </div>
                      </div>

                      <div v-if="getReviewSummary(run)" class="result-block">
                        <div class="result-block-title">审查摘要</div>
                        <p class="result-summary">{{ getReviewSummary(run) }}</p>
                      </div>

                      <div class="result-block">
                        <div class="result-block-title">Top 3 重点问题</div>
                        <el-empty v-if="!getTopIssues(run).length" description="暂无重点问题" :image-size="48" />
                        <div v-else class="issue-list">
                          <div v-for="issue in getTopIssues(run)" :key="issue.rank || issue.title" class="issue-card">
                            <div class="issue-head">
                              <span class="issue-rank">#{{ issue.rank }}</span>
                              <el-tag v-if="issue.severity" :type="severityTagType(issue.severity)" size="small">
                                {{ severityLabel(issue.severity) }}
                              </el-tag>
                              <span v-if="issue.category" class="issue-category">{{ issue.category }}</span>
                              <strong class="issue-title">{{ issue.title || '—' }}</strong>
                            </div>
                            <p v-if="issue.description" class="issue-text">{{ issue.description }}</p>
                            <div v-if="issue.filePath || formatIssueLines(issue)" class="issue-locate">
                              <code v-if="issue.filePath">{{ issue.filePath }}</code>
                              <span v-if="formatIssueLines(issue)" class="issue-lines">{{ formatIssueLines(issue) }}</span>
                            </div>
                            <div v-if="issue.evidence" class="issue-evidence">
                              <span class="issue-field-label">证据</span>
                              <pre class="issue-pre">{{ issue.evidence }}</pre>
                            </div>
                            <div v-if="issue.suggestion" class="issue-suggestion">
                              <span class="issue-field-label">建议</span>
                              <p class="issue-text">{{ issue.suggestion }}</p>
                            </div>
                          </div>
                        </div>
                      </div>

                      <div class="result-block exec-info">
                        <div class="result-block-title">执行信息</div>
                        <el-descriptions :column="1" border size="small">
                          <el-descriptions-item v-if="isLlmMode(run.snapshotReviewMode)" label="审查模板">
                            {{ run.snapshotTemplateName || '—' }}
                            <span v-if="run.snapshotTemplateCode">（{{ run.snapshotTemplateCode }}）</span>
                            <span v-if="run.snapshotTemplateVersion != null"> · v{{ run.snapshotTemplateVersion }}</span>
                          </el-descriptions-item>
                          <el-descriptions-item label="协议版本">{{ run.protocolVersion || '—' }}</el-descriptions-item>
                          <el-descriptions-item label="模型">
                            {{ run.snapshotModelName || '—' }}
                            <span v-if="run.snapshotModel">（{{ run.snapshotModel }}）</span>
                          </el-descriptions-item>
                          <el-descriptions-item label="耗时">{{ formatDuration(run.durationMs) }}</el-descriptions-item>
                        </el-descriptions>
                      </div>
                    </div>
                  </template>

                  <template v-else>
                    <div v-if="run.resultSummary" class="result-block">
                      <div class="result-block-title">审查摘要</div>
                      <p class="result-summary">{{ run.resultSummary }}</p>
                    </div>
                  </template>

                  <el-collapse v-if="runCollapses(run).length" class="run-collapse">
                    <el-collapse-item v-for="item in runCollapses(run)" :key="item.name" :title="item.title" :name="item.name">
                      <pre class="result-json">{{ item.content }}</pre>
                    </el-collapse-item>
                  </el-collapse>
                </div>
              </el-timeline-item>
            </el-timeline>
          </section>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="ReviewTask">
import { listReviewTask, getReviewTask, retryReviewTask } from '@/api/review/task'
import { listReviewProject } from '@/api/review/project'
import auth from '@/plugins/auth'

const { proxy } = getCurrentInstance()
const {
  review_task_status, review_conclusion,
  review_task_step, review_task_failure_type, review_mode, review_engine_code
} = proxy.useDict(
  'review_task_status', 'review_conclusion',
  'review_task_step', 'review_task_failure_type', 'review_mode', 'review_engine_code'
)

const taskList = ref([])
const projectOptions = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const dateRange = ref([])
const detailOpen = ref(false)
const detailLoading = ref(false)
const detailTask = ref(null)
const detailRuns = ref([])
const detailError = ref('')

const data = reactive({
  queryParams: { pageNum: 1, pageSize: 10, projectId: undefined, prNumber: undefined, taskStatus: undefined }
})
const { queryParams } = toRefs(data)

function getList() {
  loading.value = true
  listReviewTask(proxy.addDateRange(queryParams.value, dateRange.value)).then(response => {
    taskList.value = response.rows || []
    total.value = response.total || 0
  }).finally(() => { loading.value = false; schedulePolling() })
}

/** 执行中的任务每 5 秒静默刷新一次，结束后自动停止。 */
const pollTimer = ref(null)

function schedulePolling() {
  clearPolling()
  const hasRunning = taskList.value.some(row => row.taskStatus === 'RUNNING')
    || (detailOpen.value && detailTask.value?.taskStatus === 'RUNNING')
  if (hasRunning) {
    pollTimer.value = setTimeout(() => { refreshRunning().finally(() => schedulePolling()) }, 5000)
  }
}

function clearPolling() {
  if (pollTimer.value) { clearTimeout(pollTimer.value); pollTimer.value = null }
}

function refreshRunning() {
  return listReviewTask(proxy.addDateRange(queryParams.value, dateRange.value)).then(response => {
    taskList.value = response.rows || []
    total.value = response.total || 0
    if (detailOpen.value && detailTask.value?.taskStatus === 'RUNNING') {
      return getReviewTask(detailTask.value.taskId).then(detail => {
        const payload = detail.data || {}
        detailTask.value = payload.task || null
        detailRuns.value = payload.runs || []
      })
    }
  })
}

onBeforeUnmount(() => clearPolling())

function loadProjects() {
  listReviewProject({ pageNum: 1, pageSize: 100 }).then(response => {
    projectOptions.value = response.rows || []
  })
}

function canOpenDetail() {
  return auth.hasPermi('review:task:query')
}

function canRetry(row) {
  return auth.hasPermi('review:task:retry')
    && (row?.taskStatus === 'FAILED' || row?.taskStatus === 'PENDING' || row?.taskStatus === 'RUNNING')
}

function retryActionLabel(row) {
  return row?.taskStatus === 'PENDING' ? '执行' : '重试'
}

function statusLabel(status) {
  return dictLabel(review_task_status.value, status)
}

function detailActionTip() {
  if (!auth.hasPermi('review:task:query')) {
    return '当前账号没有「审查任务详情」权限（review:task:query）。请联系管理员在角色菜单中授权后重新登录。'
  }
  return '查看任务执行配置、过程、结果与失败原因'
}

function retryActionTip(row) {
  if (!auth.hasPermi('review:task:retry')) {
    return '当前账号没有「审查任务执行/重试」权限（review:task:retry）。请联系管理员授权后重新登录。'
  }
  const status = row?.taskStatus
  if (status === 'FAILED') {
    return '任务已失败，可重试；历史执行记录会保留。'
  }
  if (status === 'PENDING') {
    return '任务尚未开始。建单后一般会自动执行；若长时间仍为待执行，可点「执行」手动触发。'
  }
  if (status === 'RUNNING') {
    return `当前任务状态为「${statusLabel(status)}」。请等待本次执行结束，页面会自动刷新。可先打开详情查看当前步骤；若执行已中断超过 30 分钟，可点「重试」回收。`
  }
  if (status === 'SUCCESS') {
    return `当前任务状态为「${statusLabel(status)}」。已完成任务不可再次执行，以免覆盖有效结论。如需重新审查，请等待新的 PR 事件生成新任务。`
  }
  return `仅「待执行」或「已失败」任务可手动触发。当前状态：${statusLabel(status) || status || '未知'}。`
}

function handleDetailClick(row) {
  if (!canOpenDetail()) {
    proxy.$modal.msgWarning(detailActionTip())
    return
  }
  openDetail(row)
}

function handleRetryClick(row) {
  if (!canRetry(row)) {
    proxy.$modal.msgWarning(retryActionTip(row))
    return
  }
  handleRetry(row)
}

function openDetail(row) {
  detailOpen.value = true
  detailLoading.value = true
  detailTask.value = null
  detailRuns.value = []
  detailError.value = ''
  getReviewTask(row.taskId).then(response => {
    const payload = response.data || {}
    detailTask.value = payload.task || null
    detailRuns.value = payload.runs || []
    if (!detailTask.value) {
      detailError.value = '未获取到任务详情。请确认任务仍存在，或联系管理员检查后端接口。'
    }
  }).catch(error => {
    detailError.value = error?.message
      || '详情加载失败。请确认已具备 review:task:query 权限，且后端服务与数据库脚本已就绪。'
  }).finally(() => { detailLoading.value = false })
}

function handleRetry(row) {
  const pending = row?.taskStatus === 'PENDING'
  const running = row?.taskStatus === 'RUNNING'
  const confirmText = pending
    ? '确认立即执行该待执行任务？'
    : running
      ? '仅当本次执行已中断（超过 30 分钟）时才可回收重试，确认继续？执行中的任务会被拒绝。'
      : '确认重新执行该失败任务？历史执行记录将保留。'
  const successText = pending ? '已提交执行' : '已提交重试'
  proxy.$modal.confirm(confirmText).then(() => {
    return retryReviewTask(row.taskId)
  }).then(() => {
    proxy.$modal.msgSuccess(successText)
    getList()
    if (detailOpen.value && detailTask.value?.taskId === row.taskId) {
      openDetail(row)
    }
  }).catch(() => {})
}

function normalizeMode(mode) {
  return mode === 'OCR_PR_DIFF' ? 'OCR_ENGINE' : mode
}

function isOcrMode(mode) {
  const value = normalizeMode(mode)
  return value === 'OCR_ENGINE'
}

function isLlmMode(mode) {
  return normalizeMode(mode) === 'LLM_DIRECT'
}

function shortSha(sha) {
  return sha ? sha.substring(0, 7) : '—'
}

function formatDuration(ms) {
  if (ms == null) return '—'
  if (ms < 1000) return ms + ' ms'
  const seconds = Math.round(ms / 1000)
  if (seconds < 60) return seconds + ' s'
  return Math.floor(seconds / 60) + ' m ' + (seconds % 60) + ' s'
}

function prettyJson(value) {
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch (e) {
    return value
  }
}

const SCORE_DIMENSION_DEFS = [
  { dimension: 'CORRECTNESS', label: '功能实现的正确性与健壮性', maxScore: 40, column: 'scoreCorrectness' },
  { dimension: 'SECURITY', label: '安全性与潜在风险', maxScore: 30, column: 'scoreSecurity' },
  { dimension: 'PRACTICE', label: '最佳实践与可维护性', maxScore: 20, column: 'scorePractice' },
  { dimension: 'PERFORMANCE', label: '性能与资源利用', maxScore: 5, column: 'scorePerformance' },
  { dimension: 'COMMIT_QUALITY', label: '提交信息质量', maxScore: 5, column: 'scoreCommitQuality' }
]

const SEVERITY_LABELS = {
  CRITICAL: '严重',
  HIGH: '高',
  MEDIUM: '中',
  LOW: '低',
  INFO: '信息'
}

function safeParseJson(value) {
  if (value == null || value === '') return null
  if (typeof value === 'object') return value
  try {
    return JSON.parse(value)
  } catch (e) {
    return null
  }
}

function getParsedResultJson(run) {
  return safeParseJson(run?.resultJson)
}

function hasScoringFields(run) {
  if (!run) return false
  if (run.totalScore != null) return true
  return SCORE_DIMENSION_DEFS.some(def => run[def.column] != null)
    || (Array.isArray(getParsedResultJson(run)?.scores) && getParsedResultJson(run).scores.length > 0)
}

function showStructuredResult(run) {
  return (isLlmMode(run?.snapshotReviewMode) && run?.runStatus === 'SUCCESS') || hasScoringFields(run)
}

function formatScore(value) {
  return value == null ? '—' : value
}

function getScoreDimensions(run) {
  const parsed = getParsedResultJson(run)
  const scoreMap = new Map()
  if (Array.isArray(parsed?.scores)) {
    parsed.scores.forEach(item => {
      if (item?.dimension) {
        scoreMap.set(item.dimension, item)
      }
    })
  }
  return SCORE_DIMENSION_DEFS.map(def => {
    const fromJson = scoreMap.get(def.dimension)
    if (fromJson) {
      return {
        dimension: def.dimension,
        label: def.label,
        score: fromJson.score,
        maxScore: fromJson.maxScore ?? def.maxScore,
        reason: fromJson.reason || ''
      }
    }
    return {
      dimension: def.dimension,
      label: def.label,
      score: run?.[def.column],
      maxScore: def.maxScore,
      reason: ''
    }
  })
}

function getReviewSummary(run) {
  const parsed = getParsedResultJson(run)
  return parsed?.summary || run?.resultSummary || ''
}

function getTopIssues(run) {
  const fromColumn = safeParseJson(run?.topIssuesJson)
  if (Array.isArray(fromColumn) && fromColumn.length) {
    return fromColumn
  }
  const parsed = getParsedResultJson(run)
  return Array.isArray(parsed?.topIssues) ? parsed.topIssues : []
}

function severityLabel(value) {
  const key = (value || '').toUpperCase()
  return SEVERITY_LABELS[key] || value || '—'
}

function severityTagType(value) {
  const key = (value || '').toUpperCase()
  if (key === 'CRITICAL' || key === 'HIGH') return 'danger'
  if (key === 'MEDIUM') return 'warning'
  if (key === 'LOW' || key === 'INFO') return 'info'
  return 'info'
}

function formatIssueLines(issue) {
  if (issue?.startLine == null && issue?.endLine == null) return ''
  if (issue.startLine != null && issue.endLine != null && issue.startLine !== issue.endLine) {
    return `L${issue.startLine}-${issue.endLine}`
  }
  const line = issue.startLine ?? issue.endLine
  return line != null ? `L${line}` : ''
}

function buildDiagnosticContent(run) {
  const parts = []
  if (run?.rawResponseExcerpt) {
    parts.push('--- 原始响应摘录 ---\n' + run.rawResponseExcerpt)
  }
  if (run?.resultJson) {
    parts.push('--- 结构化 JSON ---\n' + prettyJson(run.resultJson))
  }
  return parts.join('\n\n')
}

function runCollapses(run) {
  const items = []
  if (run?.snapshotPromptContent) {
    items.push({
      name: 'prompt',
      title: '模板正文快照（后续模板修改不影响本任务）',
      content: run.snapshotPromptContent
    })
  }
  if (showStructuredResult(run)) {
    const diagnostic = buildDiagnosticContent(run)
    if (diagnostic) {
      items.push({ name: 'diagnostic', title: '诊断：原始响应', content: diagnostic })
    }
  } else if (run?.resultJson) {
    items.push({ name: 'result', title: '结构化结果', content: prettyJson(run.resultJson) })
  }
  return items
}

function dictLabel(options, value) {
  const hit = (options || []).find(item => item.value === value)
  return hit ? hit.label : value || '—'
}

function stepLabel(value) { return dictLabel(review_task_step.value, value) }
function modeLabel(value) { return dictLabel(review_mode.value, value) }
function engineLabel(value) { return dictLabel(review_engine_code.value, value) }

function runTimelineType(status) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'RUNNING') return 'primary'
  return 'info'
}

function formatDateTime(value) {
  return value ? proxy.parseTime(value) : '—'
}

function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); dateRange.value = []; handleQuery() }

loadProjects()
getList()
</script>

<style scoped>
.pr-cell { display: flex; align-items: center; gap: 8px; }
.pr-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.branch-flow { color: var(--el-text-color-regular); }
.failure-message { color: var(--el-color-danger); }
.empty-tip { color: var(--el-text-color-placeholder); }
.action-wrap { display: inline-block; }
.action-hint { margin-left: 12px; color: var(--el-text-color-secondary); font-size: 12px; }
.mb12 { margin-bottom: 12px; }
.detail-body { padding: 0 4px 24px; }
.detail-section { margin-bottom: 24px; }
.detail-section h4 { margin: 0 0 12px; font-size: 15px; font-weight: 600; }
.detail-actions { display: flex; align-items: center; margin-top: 12px; }
.run-card { padding: 4px 0; }
.run-head { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.run-meta { font-size: 13px; line-height: 1.7; color: var(--el-text-color-regular); }
.muted { color: var(--el-text-color-secondary); font-size: 12px; }
.result-json {
  margin: 0;
  max-height: 280px;
  overflow: auto;
  padding: 12px;
  background: #f5f7f6;
  border: 1px solid #e5ebe7;
  border-radius: 6px;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-word;
}
.run-collapse { margin-top: 12px; }
.score-panel { margin-top: 12px; }
.score-total {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #f8faf9;
  border: 1px solid #e5ebe7;
  border-radius: 8px;
}
.score-total-label { font-size: 13px; color: var(--el-text-color-secondary); }
.score-total-value { font-size: 28px; font-weight: 600; color: var(--el-text-color-primary); font-variant-numeric: tabular-nums; }
.score-total-unit { font-size: 13px; color: var(--el-text-color-secondary); }
.score-dimensions { display: flex; flex-direction: column; gap: 10px; margin-bottom: 16px; }
.score-dim-row {
  padding: 10px 12px;
  background: #fcfdfc;
  border: 1px solid #e5ebe7;
  border-radius: 6px;
}
.score-dim-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.score-dim-name { font-size: 13px; font-weight: 500; color: var(--el-text-color-regular); }
.score-dim-score { font-size: 13px; font-weight: 600; font-variant-numeric: tabular-nums; white-space: nowrap; }
.score-dim-reason { margin-top: 6px; font-size: 13px; line-height: 1.6; color: var(--el-text-color-secondary); }
.result-block { margin-bottom: 16px; }
.result-block-title { margin-bottom: 8px; font-size: 13px; font-weight: 600; color: var(--el-text-color-primary); }
.result-summary { margin: 0; font-size: 13px; line-height: 1.7; color: var(--el-text-color-regular); white-space: pre-wrap; }
.exec-info :deep(.el-descriptions__label) { width: 88px; }
.issue-list { display: flex; flex-direction: column; gap: 12px; }
.issue-card {
  padding: 12px;
  background: #fcfdfc;
  border: 1px solid #e5ebe7;
  border-left: 3px solid #d8e0dc;
  border-radius: 6px;
}
.issue-head { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-bottom: 6px; }
.issue-rank { font-size: 12px; font-weight: 600; color: var(--el-text-color-secondary); }
.issue-category { font-size: 12px; color: var(--el-text-color-secondary); }
.issue-title { font-size: 14px; font-weight: 600; color: var(--el-text-color-primary); }
.issue-text { margin: 0; font-size: 13px; line-height: 1.7; color: var(--el-text-color-regular); white-space: pre-wrap; }
.issue-locate { margin-top: 6px; font-size: 12px; color: var(--el-text-color-secondary); }
.issue-locate code { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; font-size: 12px; }
.issue-lines { margin-left: 8px; }
.issue-evidence, .issue-suggestion { margin-top: 8px; }
.issue-field-label { display: block; margin-bottom: 4px; font-size: 12px; font-weight: 500; color: var(--el-text-color-secondary); }
.issue-pre {
  margin: 0;
  padding: 8px 10px;
  background: #f6f8f7;
  border: 1px solid #e5ebe7;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
