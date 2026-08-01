import request from '@/utils/request'

export function listAiModelConfig(query) {
  return request({
    url: '/system/aimodelconfig/list',
    method: 'get',
    params: query
  })
}

export function getAiModelConfig(modelId) {
  return request({
    url: '/system/aimodelconfig/' + modelId,
    method: 'get'
  })
}

export function listLlmProviders() {
  return request({
    url: '/system/aimodelconfig/providers',
    method: 'get'
  })
}

export function addAiModelConfig(data) {
  return request({
    url: '/system/aimodelconfig',
    method: 'post',
    data: data
  })
}

export function updateAiModelConfig(data) {
  return request({
    url: '/system/aimodelconfig',
    method: 'put',
    data: data
  })
}

export function delAiModelConfig(modelId) {
  return request({
    url: '/system/aimodelconfig/' + modelId,
    method: 'delete'
  })
}

export function enableAiModel(modelId, enabled) {
  return request({
    url: '/system/aimodelconfig/' + modelId + '/enable?enabled=' + enabled,
    method: 'put'
  })
}

export function setDefaultModel(modelId) {
  return request({
    url: '/system/aimodelconfig/' + modelId + '/default',
    method: 'put'
  })
}

export function testAiModelConnection(data) {
  return request({
    url: '/system/aimodelconfig/test',
    method: 'post',
    data: data
  })
}

export function testAiModelCall(data) {
  return request({
    url: '/system/aimodelconfig/test-call',
    method: 'post',
    data: data
  })
}
