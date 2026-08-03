import request from '@/utils/request'

export function listNotifyChannel(query) {
  return request({ url: '/review/notify/channel/list', method: 'get', params: query })
}

export function getNotifyChannel(channelId) {
  return request({ url: '/review/notify/channel/' + channelId, method: 'get' })
}

export function addNotifyChannel(data) {
  return request({ url: '/review/notify/channel', method: 'post', data })
}

export function updateNotifyChannel(data) {
  return request({ url: '/review/notify/channel', method: 'put', data })
}

export function delNotifyChannel(channelId) {
  return request({ url: '/review/notify/channel/' + channelId, method: 'delete' })
}

export function changeNotifyChannelStatus(data) {
  return request({ url: '/review/notify/channel/changeStatus', method: 'put', data })
}

export function testNotifyChannel(channelId) {
  return request({ url: '/review/notify/channel/' + channelId + '/test', method: 'post' })
}
