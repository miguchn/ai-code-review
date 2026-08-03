import request from '@/utils/request'

/** 首页工作台汇总（登录即可，无新权限串） */
export function getWorkbenchSummary() {
  return request({
    url: '/workbench/summary',
    method: 'get'
  })
}
