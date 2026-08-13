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

export function getInsightTeamMembers(query) {
  return request({ url: '/insight/team/members', method: 'get', params: query })
}

export function getTokenOverview(query) {
  return request({ url: '/insight/token/overview', method: 'get', params: query })
}

export function getTokenTrend(query) {
  return request({ url: '/insight/token/trend', method: 'get', params: query })
}

export function listTokenModels(query) {
  return request({ url: '/insight/token/models', method: 'get', params: query })
}

export function listTokenProjects(query) {
  return request({ url: '/insight/token/projects', method: 'get', params: query })
}

export function listTokenRuns(query) {
  return request({ url: '/insight/token/runs', method: 'get', params: query })
}
