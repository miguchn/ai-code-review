import { formatTime } from '@/utils/index'

/** 'yyyy-MM-dd HH:mm[:ss]' → 相对时间（刚刚/N分钟前/N小时前/N天前/更早） */
export function relativeTime(timeStr) {
  if (!timeStr) return '—'
  const ms = new Date(String(timeStr).replace(/-/g, '/')).getTime()
  if (Number.isNaN(ms)) return timeStr
  return formatTime(ms)
}
