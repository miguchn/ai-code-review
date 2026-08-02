import request from '@/utils/request'

export function listReviewRecord(query) {
  return request({ url: '/review/record/list', method: 'get', params: query })
}

export function getReviewRecord(taskId) {
  return request({ url: '/review/record/' + taskId, method: 'get' })
}
