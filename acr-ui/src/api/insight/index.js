import request from '@/utils/request'

export function getInsightOverview(query) {
  return request({ url: '/insight/overview', method: 'get', params: query })
}

export function listInsightProjects(query) {
  return request({ url: '/insight/projects', method: 'get', params: query })
}

export function getInsightProjectDetail(projectId, query) {
  return request({ url: '/insight/project/' + projectId, method: 'get', params: query })
}

export function getInsightMetricsDict() {
  return request({ url: '/insight/metrics-dict', method: 'get' })
}

export function getInsightMemberMine(query) {
  return request({ url: '/insight/member/mine', method: 'get', params: query })
}

export function claimInsightMemberIdentity(data) {
  return request({ url: '/insight/member/claim', method: 'post', data })
}

export function getInsightTeamMembers(query) {
  return request({ url: '/insight/team/members', method: 'get', params: query })
}
