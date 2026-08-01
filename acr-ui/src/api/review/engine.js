import request from '@/utils/request'

export function getReviewEngineInfo() {
  return request({ url: '/review/engine/info', method: 'get' })
}

export function detectReviewEngine() {
  return request({ url: '/review/engine/detect', method: 'post' })
}

export function testReviewEngine(data) {
  return request({ url: '/review/engine/test', method: 'post', data: data || {} })
}
