import request from '@/utils/request'

export function listReviewTemplate(query) {
  return request({ url: '/review/template/list', method: 'get', params: query })
}

export function getPlatformRules() {
  return request({ url: '/review/template/platform-rules', method: 'get' })
}

export function getReviewTemplate(templateId) {
  return request({ url: '/review/template/' + templateId, method: 'get' })
}

export function addReviewTemplate(data) {
  return request({ url: '/review/template', method: 'post', data })
}

export function updateReviewTemplate(data) {
  return request({ url: '/review/template', method: 'put', data })
}

export function delReviewTemplate(templateId) {
  return request({ url: '/review/template/' + templateId, method: 'delete' })
}
