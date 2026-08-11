import request from '@/utils/request'

export function listMyIdentities() {
  return request({ url: '/system/userprofile/identities', method: 'get' })
}

export function addMyIdentity(data) {
  return request({ url: '/system/userprofile/identities', method: 'post', data })
}

export function removeMyIdentity(id) {
  return request({ url: '/system/userprofile/identities/' + id, method: 'delete' })
}

export function listIdentityCandidates() {
  return request({ url: '/insight/identity/candidates', method: 'get' })
}

export function listTeamIdentities() {
  return request({ url: '/insight/team/identities', method: 'get' })
}

export function listIdentityUserOptions(keyword) {
  return request({
    url: '/insight/team/identities/userOptions',
    method: 'get',
    params: { keyword: keyword || undefined }
  })
}

export function bindTeamIdentity(data) {
  return request({ url: '/insight/team/identities/bind', method: 'post', data })
}

export function unbindTeamIdentity(id) {
  return request({ url: '/insight/team/identities/' + id, method: 'delete' })
}
