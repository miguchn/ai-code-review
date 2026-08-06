/** 问题详情生命线：由 actions + issue 状态纯前端推导。 */

const TERMINAL_STATUSES = ['CLOSED', 'IGNORED', 'FALSE_POSITIVE']
const TERMINAL_LABELS = {
  CLOSED: '已关闭',
  IGNORED: '已忽略',
  FALSE_POSITIVE: '误报'
}

function formatLifecycleTime(value, formatDateTime) {
  if (!value) return ''
  const text = formatDateTime(value)
  if (!text || text === '—') return ''
  const m = String(text).match(/(\d{4})-(\d{2})-(\d{2})/)
  return m ? `${m[2]}-${m[3]}` : text
}

function findAction(actions, predicate) {
  if (!Array.isArray(actions)) return null
  for (let i = 0; i < actions.length; i++) {
    if (predicate(actions[i])) return actions[i]
  }
  return null
}

function findLastAction(actions, predicate) {
  if (!Array.isArray(actions)) return null
  for (let i = actions.length - 1; i >= 0; i--) {
    if (predicate(actions[i])) return actions[i]
  }
  return null
}

function isTerminalStatus(status) {
  return TERMINAL_STATUSES.includes(status)
}

/**
 * @param {object|null} issue
 * @param {array} actions
 * @param {(v:any)=>string} formatDateTime
 */
export function buildLifecycleNodes(issue, actions, formatDateTime) {
  const list = Array.isArray(actions) ? actions : []
  const detected = findAction(list, a => a.actionType === 'DETECTED')
  const confirm = findAction(list, a => a.actionType === 'CONFIRM')
  const toFix = findAction(list, a => a.toStatus === 'AWAITING_FIX' && a.fromStatus !== a.toStatus)
  const recheck = findAction(list, a => a.actionType === 'AUTO_RECHECK')
  const terminal = findLastAction(list, a => a.actionType === 'CLOSE' || a.actionType === 'DISMISS')

  const status = issue?.status
  const terminalLabel = TERMINAL_LABELS[status]
    || TERMINAL_LABELS[terminal?.toStatus]
    || '关闭'

  const currentKey = status === 'AWAITING_CONFIRM' ? 'confirm'
    : status === 'AWAITING_FIX' ? 'awaitingFix'
      : status === 'RECHECKING' ? 'rechecking'
        : isTerminalStatus(status) ? 'terminal'
          : null

  return [
    {
      key: 'discover',
      label: '发现',
      reached: !!(detected || issue?.createTime),
      current: currentKey === 'discover',
      timeText: formatLifecycleTime(detected?.createTime || issue?.createTime, formatDateTime),
      roundText: issue?.firstTaskId ? `#${issue.firstTaskId}` : ''
    },
    {
      key: 'confirm',
      label: '确认',
      reached: !!confirm,
      current: currentKey === 'confirm',
      timeText: formatLifecycleTime(confirm?.createTime, formatDateTime),
      roundText: ''
    },
    {
      key: 'awaitingFix',
      label: '待修复',
      reached: !!(toFix || confirm),
      current: currentKey === 'awaitingFix',
      timeText: formatLifecycleTime(toFix?.createTime || confirm?.createTime, formatDateTime),
      roundText: ''
    },
    {
      key: 'rechecking',
      label: '待复核',
      reached: !!recheck,
      current: currentKey === 'rechecking',
      timeText: formatLifecycleTime(recheck?.createTime, formatDateTime),
      roundText: ''
    },
    {
      key: 'terminal',
      label: terminalLabel,
      reached: !!(terminal || isTerminalStatus(status)),
      current: currentKey === 'terminal',
      timeText: formatLifecycleTime(terminal?.createTime || issue?.closedTime, formatDateTime),
      roundText: ''
    }
  ]
}
