import request from '@/utils/request'

// 查询 AI 大模型配置列表
export function listAiModelConfig(query) {
  return request({
    url: '/system/aimodelconfig/list',
    method: 'get',
    params: query
  })
}

// 查询 AI 大模型配置详细
export function getAiModelConfig(modelId) {
  return request({
    url: '/system/aimodelconfig/' + modelId,
    method: 'get'
  })
}

// 新增 AI 大模型配置
export function addAiModelConfig(data) {
  return request({
    url: '/system/aimodelconfig',
    method: 'post',
    data: data
  })
}

// 修改 AI 大模型配置
export function updateAiModelConfig(data) {
  return request({
    url: '/system/aimodelconfig',
    method: 'put',
    data: data
  })
}

// 删除 AI 大模型配置
export function delAiModelConfig(modelId) {
  return request({
    url: '/system/aimodelconfig/' + modelId,
    method: 'delete'
  })
}

// 启用/禁用模型
export function enableAiModel(modelId, enabled) {
  return request({
    url: '/system/aimodelconfig/' + modelId + '/enable?enabled=' + enabled,
    method: 'put'
  })
}

// 设为默认模型
export function setDefaultModel(modelId) {
  return request({
    url: '/system/aimodelconfig/' + modelId + '/default',
    method: 'put'
  })
}

// 测试大模型连接
export function testAiModel(data) {
  return request({
    url: '/system/aimodelconfig/test',
    method: 'post',
    data: data
  })
}
