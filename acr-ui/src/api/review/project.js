import request from '@/utils/request'

export function listReviewProject(query) {
  return request({ url: '/review/project/list', method: 'get', params: query })
}

export function getReviewProject(projectId) {
  return request({ url: '/review/project/' + projectId, method: 'get' })
}

export function getReviewProjectOptions() {
  return request({ url: '/review/project/options', method: 'get' })
}

export function readReviewProjectRepositoryInfo(data) {
  return request({ url: '/review/project/repository-info', method: 'post', data })
}

export function addReviewProject(data) {
  return request({ url: '/review/project', method: 'post', data })
}

export function updateReviewProject(data) {
  return request({ url: '/review/project', method: 'put', data })
}

export function delReviewProject(projectId) {
  return request({ url: '/review/project/' + projectId, method: 'delete' })
}

export function changeReviewProjectStatus(projectId, status) {
  return request({ url: '/review/project/' + projectId + '/status', method: 'put', params: { status } })
}

export function testReviewProject(projectId) {
  return request({ url: '/review/project/' + projectId + '/test', method: 'post' })
}
