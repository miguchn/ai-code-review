import request from '@/utils/request'

/** 首页工作台汇总（登录即可，无新权限串） */
export function getWorkbenchSummary() {
  return request({
    url: '/workbench/summary',
    method: 'get'
  })
}

/** 审查结论按天趋势（近 days 天；无 review:record:list 权限时 data 为 null） */
export function getWorkbenchTrend(days = 14) {
  return request({
    url: '/workbench/trend',
    method: 'get',
    params: { days }
  })
}

/** 启用模型健康摘要（登录可调，字段白名单脱敏） */
export function getWorkbenchModels() {
  return request({
    url: '/workbench/models',
    method: 'get'
  })
}
