import request from '@/utils/request'

export function listDelivery(query) {
  return request({ url: '/review/delivery/list', method: 'get', params: query })
}

export function getDelivery(deliveryId) {
  return request({ url: '/review/delivery/' + deliveryId, method: 'get' })
}

/** 任务最近一条 IM 投递记录（任务/记录详情页用，不要求 delivery:list）。 */
export function getLatestImDelivery(taskId) {
  return request({ url: '/review/delivery/task/' + taskId + '/im-latest', method: 'get' })
}

/** 重试 GitHub PR 总结评论投递（以该 PR 最近 SUCCESS 结论渲染）。 */
export function retryDelivery(taskId) {
  return request({ url: '/review/delivery/' + taskId + '/retry', method: 'post' })
}

/** 按投递记录补发（原渠道、原任务结论）。 */
export function retryDeliveryById(deliveryId) {
  return request({ url: '/review/delivery/record/' + deliveryId + '/retry', method: 'post' })
}

/** 查看投递正文快照（kind/channelType/title/body）。 */
export function getDeliveryContent(deliveryId) {
  return request({ url: '/review/delivery/record/' + deliveryId + '/content', method: 'get' })
}

/** @deprecated 使用 retryDelivery */
export function retryReviewDelivery(taskId) {
  return retryDelivery(taskId)
}
