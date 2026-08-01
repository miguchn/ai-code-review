import request from '@/utils/request'

export function listReviewTask(query) {
  return request({ url: '/review/task/list', method: 'get', params: query })
}

export function getReviewTask(taskId) {
  return request({ url: '/review/task/' + taskId, method: 'get' })
}
