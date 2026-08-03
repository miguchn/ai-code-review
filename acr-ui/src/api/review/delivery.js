import request from '@/utils/request'

/** 重试 GitHub PR 总结评论投递（以该 PR 最近 SUCCESS 结论渲染）。 */
export function retryReviewDelivery(taskId) {
  return request({ url: '/review/delivery/' + taskId + '/retry', method: 'post' })
}
