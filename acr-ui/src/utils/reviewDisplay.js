/** 审查任务/记录前端展示辅助（空值友好，不抛错）。 */

import { parseTime } from '@/utils/common'

export const SCORE_DIMENSION_DEFS = [
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

/** 问题归属（协议 v1.1）：新增=本次变更引入，存量=历史遗留。v1.0 结果无 origin，按新增展示。 */
export const ISSUE_ORIGIN_LABELS = {
  NEW: '新增',
  EXISTING: '存量'
}

/** 范围决策快照的枚举中文映射（与后端 ReviewScopeRules / ReviewScopePromptAssembler 常量对齐）。 */
export const SCOPE_EXCLUDE_REASON_LABELS = {
  DEFAULT_EXCLUDE: '平台默认排除',
  PROJECT_EXCLUDE: '项目排除规则',
  TEST_FILE: '测试文件'
}

export const SCOPE_RECORD_REASON_LABELS = {
  DELETED: '删除文件',
  BINARY: '二进制文件',
  RENAME_ONLY: '纯改名',
  GITLINK: '子模块指针',
  MODE_ONLY: '仅权限变更',
  EMPTY: '空文件'
}

export const SCOPE_EXPAND_RULE_LABELS = {
  SECURITY: '权限/安全逻辑',
  DEPENDENCY: '依赖声明',
  DB_SCRIPT: '数据库脚本',
  CONFIG: '配置文件',
  SIGNATURE: '公共签名变更',
  NEW_FILE: '新增文件'
}

export const SCOPE_EXPAND_STATUS_LABELS = {
  IN_DIFF: '内容已在 Diff 中',
  FULL: '已纳入完整内容',
  BUDGET_SKIPPED: '预算不足跳过',
  DEGRADED: '拉取失败降级',
  FETCH_LIMIT_SKIPPED: '超拉取上限跳过'
}

export function emptyDash(value) {
  if (value == null || value === '') return '—'
  return value
}

/** 日期时间展示：空值统一「—」。 */
export function formatDateTime(value) {
  return value ? parseTime(value) : '—'
}

/**
 * 阶段滞留时长：当前时间 − stageEnteredTime。
 * <24h 显示「n小时」，≥24h 显示「n天」；无效时间返回空串。
 */
export function formatStageDuration(stageEnteredTime) {
  if (stageEnteredTime == null || stageEnteredTime === '') return ''
  let start
  if (stageEnteredTime instanceof Date) {
    start = stageEnteredTime
  } else if (typeof stageEnteredTime === 'number') {
    start = new Date(stageEnteredTime)
  } else {
    // 兼容后端 yyyy-MM-dd HH:mm:ss（Safari 需替换空格）
    start = new Date(String(stageEnteredTime).replace(/-/g, '/'))
  }
  if (Number.isNaN(start.getTime())) return ''
  const ms = Date.now() - start.getTime()
  if (ms < 0) return '不到1小时'
  const hours = Math.floor(ms / (1000 * 60 * 60))
  if (hours < 1) return '不到1小时'
  if (hours < 24) return hours + '小时'
  return Math.floor(hours / 24) + '天'
}

export function formatDuration(ms) {
  if (ms == null) return '—'
  if (ms < 1000) return ms + ' ms'
  const seconds = Math.round(ms / 1000)
  if (seconds < 60) return seconds + ' s'
  return Math.floor(seconds / 60) + ' m ' + (seconds % 60) + ' s'
}

export function shortSha(sha) {
  return sha ? String(sha).substring(0, 7) : '—'
}

export function formatScore(value) {
  return value == null ? '--' : value
}

/** 代码变更：文件数 / +新增 / -删除 */
export function formatCodeChange(changedFiles, additions, deletions) {
  const files = changedFiles == null ? '--' : String(changedFiles)
  const add = additions == null ? '--' : `+${additions}`
  const del = deletions == null ? '--' : `-${deletions}`
  if (changedFiles == null && additions == null && deletions == null) return '--'
  return `${files} / ${add} / ${del}`
}

/** 审查记录结论展示：复用 taskStatus + reviewConclusion，不新增状态字典。 */
export function recordConclusionLabel(task) {
  if (!task) return '--'
  if (task.taskStatus === 'FAILED') return '执行失败'
  const c = task.reviewConclusion
  if (c === 'PASS') return '通过'
  if (c === 'WARN') return '建议修改'
  if (c === 'BLOCK') return '高风险'
  return '--'
}

export function recordConclusionTagType(task) {
  if (!task) return 'info'
  if (task.taskStatus === 'FAILED') return 'danger'
  if (task.reviewConclusion === 'PASS') return 'success'
  if (task.reviewConclusion === 'WARN') return 'warning'
  if (task.reviewConclusion === 'BLOCK') return 'danger'
  return 'info'
}

/** 基于 Top3 重点问题按严重度分级统计（非全量问题；v1.1 起存量问题不计入）。 */
export function countFocusIssuesBySeverity(topIssuesJsonOrArray) {
  const empty = { critical: 0, high: 0, medium: 0, low: 0, total: 0 }
  let issues = topIssuesJsonOrArray
  if (typeof issues === 'string') {
    issues = safeParseJson(issues)
  }
  if (!Array.isArray(issues) || !issues.length) return empty
  const counts = { ...empty }
  issues.forEach(issue => {
    if ((issue?.origin || '').toUpperCase() === 'EXISTING') return
    const key = (issue?.severity || '').toUpperCase()
    if (key === 'CRITICAL') counts.critical += 1
    else if (key === 'HIGH') counts.high += 1
    else if (key === 'MEDIUM') counts.medium += 1
    else if (key === 'LOW' || key === 'INFO') counts.low += 1
  })
  counts.total = counts.critical + counts.high + counts.medium + counts.low
  return counts
}

export function formatFocusIssueCounts(task) {
  if (!task || task.taskStatus === 'FAILED') return '--'
  const counts = countFocusIssuesBySeverity(task.topIssuesJson)
  if (counts.total === 0 && task.focusIssueCount == null) return '--'
  if (counts.total === 0) {
    return task.focusIssueCount == null ? '--' : String(task.focusIssueCount)
  }
  return `严重 ${counts.critical} / 高 ${counts.high} / 中 ${counts.medium} / 低 ${counts.low}`
}

const DEFAULT_REPO_HOSTS = {
  GITHUB: 'https://github.com',
  GITEE: 'https://gitee.com'
}

function cleanRepoUrl(url) {
  return (url || '').replace(/(\.git|\/)+$/, '')
}

function mergeRequestPathSegment(provider, prNumber) {
  const code = (provider || 'GITHUB').toUpperCase()
  if (code === 'GITLAB') return `/-/merge_requests/${prNumber}`
  // Gitee/Gitea 的 Web 路径为 /pulls/{n}；GitHub 为 /pull/{n}
  if (code === 'GITEE' || code === 'GITEA') return `/pulls/${prNumber}`
  return `/pull/${prNumber}`
}

function resolveRepositoryBase(task) {
  const provider = (task?.provider || 'GITHUB').toUpperCase()
  const cleanedUrl = cleanRepoUrl(task?.repositoryUrl)
  if (cleanedUrl) return cleanedUrl

  const owner = task?.repositoryOwner
  const name = task?.repositoryName
  const defaultHost = DEFAULT_REPO_HOSTS[provider]
  if (owner && name && defaultHost) {
    return `${defaultHost}/${owner}/${name}`
  }
  return ''
}

/** 合并请求外链标签：GitLab 为 MR，其余为 PR。 */
export function mergeRequestLabel(provider) {
  return (provider || '').toUpperCase() === 'GITLAB' ? 'MR' : 'PR'
}

/** 由平台、仓库坐标与合并请求编号生成 Web 链接。 */
export function buildMergeRequestUrl(task) {
  if (!task?.prNumber) return ''
  const base = resolveRepositoryBase(task)
  if (!base) return ''
  return `${base}${mergeRequestPathSegment(task.provider, task.prNumber)}`
}

/** @deprecated 兼容别名，请使用 buildMergeRequestUrl。 */
export function buildGithubPrUrl(task) {
  return buildMergeRequestUrl(task)
}

export function normalizeMode(mode) {
  return mode === 'OCR_PR_DIFF' ? 'OCR_ENGINE' : mode
}

export function isOcrMode(mode) {
  return normalizeMode(mode) === 'OCR_ENGINE'
}

export function isLlmMode(mode) {
  return normalizeMode(mode) === 'LLM_DIRECT'
}

export function safeParseJson(value) {
  if (value == null || value === '') return null
  if (typeof value === 'object') return value
  try {
    return JSON.parse(value)
  } catch (e) {
    return null
  }
}

export function getParsedResultJson(run) {
  return safeParseJson(run?.resultJson)
}

export function hasScoringFields(run) {
  if (!run) return false
  if (run.totalScore != null) return true
  return SCORE_DIMENSION_DEFS.some(def => run[def.column] != null)
    || (Array.isArray(getParsedResultJson(run)?.scores) && getParsedResultJson(run).scores.length > 0)
}

export function showStructuredResult(run) {
  return (isLlmMode(run?.snapshotReviewMode) && run?.runStatus === 'SUCCESS') || hasScoringFields(run)
}

export function getScoreDimensions(run) {
  const parsed = getParsedResultJson(run)
  const scoreMap = new Map()
  if (Array.isArray(parsed?.scores)) {
    parsed.scores.forEach(item => {
      if (item?.dimension) scoreMap.set(item.dimension, item)
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

export function getReviewSummary(run) {
  const parsed = getParsedResultJson(run)
  return parsed?.summary || run?.resultSummary || ''
}

export function getTopIssues(run) {
  const fromColumn = safeParseJson(run?.topIssuesJson)
  if (Array.isArray(fromColumn) && fromColumn.length) return fromColumn
  const parsed = getParsedResultJson(run)
  return Array.isArray(parsed?.topIssues) ? parsed.topIssues : []
}

export function severityLabel(value) {
  const key = (value || '').toUpperCase()
  return SEVERITY_LABELS[key] || value || '—'
}

export function severityTagType(value) {
  const key = (value || '').toUpperCase()
  if (key === 'CRITICAL' || key === 'HIGH') return 'danger'
  if (key === 'MEDIUM') return 'warning'
  if (key === 'LOW' || key === 'INFO') return 'info'
  return 'info'
}

export function formatIssueLines(issue) {
  if (issue?.startLine == null && issue?.endLine == null) return ''
  if (issue.startLine != null && issue.endLine != null && issue.startLine !== issue.endLine) {
    return `L${issue.startLine}-${issue.endLine}`
  }
  const line = issue.startLine ?? issue.endLine
  return line != null ? `L${line}` : ''
}

export function pickLatestSuccessRun(runs) {
  if (!Array.isArray(runs) || !runs.length) return null
  const success = runs.filter(r => r?.runStatus === 'SUCCESS')
  if (!success.length) return runs[0] || null
  return success.reduce((best, cur) => {
    const bestNo = best?.attemptNo ?? 0
    const curNo = cur?.attemptNo ?? 0
    return curNo >= bestNo ? cur : best
  }, success[0])
}

export function engineOrModelLabel(run) {
  if (!run) return '—'
  if (isLlmMode(run.snapshotReviewMode)) {
    const name = run.snapshotModelName || run.snapshotModel || '—'
    return run.snapshotModel && run.snapshotModelName
      ? `${run.snapshotModelName}（${run.snapshotModel}）`
      : name
  }
  if (isOcrMode(run.snapshotReviewMode)) {
    return run.snapshotEngineName || run.snapshotEngineCode || '—'
  }
  return run.snapshotModelName || run.snapshotEngineName || '—'
}

export function templateLabel(run) {
  if (!run || !isLlmMode(run.snapshotReviewMode)) return '—'
  if (!run.snapshotTemplateName && !run.snapshotTemplateCode) return '—'
  let text = run.snapshotTemplateName || run.snapshotTemplateCode
  if (run.snapshotTemplateVersion != null) text += ` · v${run.snapshotTemplateVersion}`
  return text
}

/** 问题归属展示：v1.0 结果无 origin 时不显示标签（返回空串由调用方判断）。 */
export function issueOriginLabel(origin) {
  const key = (origin || '').toUpperCase()
  return ISSUE_ORIGIN_LABELS[key] || ''
}

export function issueOriginTagType(origin) {
  return (origin || '').toUpperCase() === 'EXISTING' ? 'info' : 'success'
}

/** 范围决策快照（M3.2）：解析 run.scopeDecisionJson，无快照或解析失败返回 null。 */
export function parseScopeDecision(run) {
  return safeParseJson(run?.scopeDecisionJson)
}

/** 范围决策概要行：纳入/排除/扩展/记录类计数 + 截断与降级标记。 */
export function scopeDecisionSummary(decision) {
  if (!decision) return ''
  if (decision.degraded) return ''
  const parts = []
  const included = Array.isArray(decision.includedFiles) ? decision.includedFiles.length : null
  if (included != null) parts.push(`纳入 ${included}`)
  const excluded = Array.isArray(decision.excludedFiles) ? decision.excludedFiles.length : 0
  if (excluded > 0) parts.push(`排除 ${excluded}`)
  const expanded = Array.isArray(decision.expandedFiles) ? decision.expandedFiles.length : 0
  if (expanded > 0) parts.push(`扩展 ${expanded}`)
  const recordOnly = Array.isArray(decision.recordOnlyFiles) ? decision.recordOnlyFiles.length : 0
  if (recordOnly > 0) parts.push(`记录类 ${recordOnly}`)
  if (decision.truncated) parts.push('已截断')
  return parts.join(' · ')
}

export function scopeExcludeReasonLabel(reason) {
  return SCOPE_EXCLUDE_REASON_LABELS[reason] || reason || '—'
}

export function scopeRecordReasonLabel(reason) {
  return SCOPE_RECORD_REASON_LABELS[reason] || reason || '—'
}

export function scopeExpandRuleLabel(rule) {
  return SCOPE_EXPAND_RULE_LABELS[rule] || rule || '—'
}

export function scopeExpandStatusLabel(status) {
  return SCOPE_EXPAND_STATUS_LABELS[status] || status || '—'
}

export function scopeExpandStatusTagType(status) {
  if (status === 'FULL' || status === 'IN_DIFF') return 'success'
  if (status === 'DEGRADED') return 'warning'
  return 'info'
}
