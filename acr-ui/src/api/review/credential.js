import request from '@/utils/request'

export function listGitCredential(query) {
  return request({ url: '/review/credential/list', method: 'get', params: query })
}

export function getGitCredential(credentialId) {
  return request({ url: '/review/credential/' + credentialId, method: 'get' })
}

export function addGitCredential(data) {
  return request({ url: '/review/credential', method: 'post', data })
}

export function updateGitCredential(data) {
  return request({ url: '/review/credential', method: 'put', data })
}

export function delGitCredential(credentialId) {
  return request({ url: '/review/credential/' + credentialId, method: 'delete' })
}

export function testGitCredential(credentialId) {
  return request({ url: '/review/credential/' + credentialId + '/test', method: 'post' })
}
