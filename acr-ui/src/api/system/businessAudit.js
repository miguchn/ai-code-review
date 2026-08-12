import request from '@/utils/request'

// 查询业务审计事实
export function list(query) {
  return request({
    url: '/system/business-audit/list',
    method: 'get',
    params: query
  })
}
