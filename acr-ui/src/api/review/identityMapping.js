import request from '@/utils/request'

/** 列出自动发现的 Git 平台用户（含已映射/未映射） */
export function listPlatformUsers() {
  return request({ url: '/review/identity-mapping', method: 'get' })
}

/** 管理员将自动发现的身份绑定到系统用户 */
export function bindIdentity(id, userId) {
  return request({ url: '/review/identity-mapping/' + id, method: 'put', data: { userId } })
}
