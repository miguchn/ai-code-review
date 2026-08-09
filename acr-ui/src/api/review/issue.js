import request from '@/utils/request'

export function listIssue(query) {
  return request({ url: '/review/issue/list', method: 'get', params: query })
}

export function getIssueStats(query) {
  return request({ url: '/review/issue/stats', method: 'get', params: query })
}

export function getIssueRecordContext(taskId, query) {
  return request({ url: '/review/issue/record/' + taskId, method: 'get', params: query })
}

export function getIssue(issueId) {
  return request({ url: '/review/issue/' + issueId, method: 'get' })
}

export function confirmIssue(issueId) {
  return request({ url: '/review/issue/' + issueId + '/confirm', method: 'put' })
}

export function closeIssue(issueId, data) {
  return request({ url: '/review/issue/' + issueId + '/close', method: 'put', data })
}

export function dismissIssue(issueId, data) {
  return request({ url: '/review/issue/' + issueId + '/dismiss', method: 'put', data })
}

export function reopenIssue(issueId) {
  return request({ url: '/review/issue/' + issueId + '/reopen', method: 'put' })
}

export function batchDisposeIssue(data) {
  return request({ url: '/review/issue/batch', method: 'post', data })
}
