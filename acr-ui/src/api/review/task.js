import request from '@/utils/request'

export function listReviewTask(query) {
  return request({ url: '/review/task/list', method: 'get', params: query })
}

export function getReviewTask(taskId) {
  return request({ url: '/review/task/' + taskId, method: 'get' })
}

export function retryReviewTask(taskId) {
  return request({ url: '/review/task/' + taskId + '/retry', method: 'post' })
}
