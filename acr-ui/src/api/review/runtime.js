import request from '@/utils/request'

export function getRuntimeOverview() {
  return request({ url: '/review/runtime/overview', method: 'get' })
}

export function listOverduePendingTasks(limit) {
  return request({ url: '/review/runtime/backlog/overdue-pending', method: 'get', params: { limit } })
}

export function listLeaseExpiredTasks(limit) {
  return request({ url: '/review/runtime/backlog/lease-expired', method: 'get', params: { limit } })
}

export function listStuckDeliveries(limit) {
  return request({ url: '/review/runtime/backlog/stuck-deliveries', method: 'get', params: { limit } })
}
