/** 审查任务/记录前端展示辅助（空值友好，不抛错）。 */

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

export function emptyDash(value) {
  if (value == null || value === '') return '—'
  return value
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

/** 基于 Top3 重点问题按严重度分级统计（非全量问题）。 */
export function countFocusIssuesBySeverity(topIssuesJsonOrArray) {
  const empty = { critical: 0, high: 0, medium: 0, low: 0, total: 0 }
  let issues = topIssuesJsonOrArray
  if (typeof issues === 'string') {
    issues = safeParseJson(issues)
  }
  if (!Array.isArray(issues) || !issues.length) return empty
  const counts = { ...empty }
  issues.forEach(issue => {
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

/** 由仓库坐标与 PR 编号生成 GitHub PR 链接。 */
export function buildGithubPrUrl(task) {
  if (!task?.prNumber) return ''
  const owner = task.repositoryOwner
  const name = task.repositoryName
  if (owner && name) {
    return `https://github.com/${owner}/${name}/pull/${task.prNumber}`
  }
  const url = (task.repositoryUrl || '').replace(/\.git$/, '').replace(/\/$/, '')
  if (url.includes('github.com/')) {
    return `${url}/pull/${task.prNumber}`
  }
  return ''
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
