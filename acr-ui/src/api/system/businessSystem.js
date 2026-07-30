import request from '@/utils/request'

// 查询业务系统列表
export function listBusinessSystem(query) {
  return request({
    url: '/system/businesssystem/list',
    method: 'get',
    params: query
  })
}

// 查询业务系统详细
export function getBusinessSystem(systemId) {
  return request({
    url: '/system/businesssystem/' + systemId,
    method: 'get'
  })
}

// 新增业务系统
export function addBusinessSystem(data) {
  return request({
    url: '/system/businesssystem',
    method: 'post',
    data: data
  })
}

// 修改业务系统
export function updateBusinessSystem(data) {
  return request({
    url: '/system/businesssystem',
    method: 'put',
    data: data
  })
}

// 删除业务系统
export function delBusinessSystem(systemId) {
  return request({
    url: '/system/businesssystem/' + systemId,
    method: 'delete'
  })
}
