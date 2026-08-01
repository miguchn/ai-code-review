/**
 * 平台级统一时间格式化工具
 *
 * 规范：
 * - 仅日期：yyyy-MM-dd
 * - 仅时间：HH:mm:ss
 * - 日期时间：yyyy-MM-dd HH:mm:ss
 * - 年月：yyyy-MM
 * - 到分钟：yyyy-MM-dd HH:mm
 * - 不展示 ISO 的 T、毫秒、微秒、多余时区
 * - 空值/无效值统一展示为 '--'
 *
 * 展示场景一律使用本模块具名函数，禁止各页面自行定义格式。
 */

const PLACEHOLDER = '--'

/**
 * 将任意时间输入归一化为 Date 对象
 * 支持：Date 实例、时间戳（10/13 位）、ISO 字符串、带 T/微秒的字符串
 * @returns {Date|null} 无效输入返回 null
 */
function normalize(value) {
  if (value === null || value === undefined || value === '') {
    return null
  }
  // 已经是 Date
  if (value instanceof Date) {
    return isNaN(value.getTime()) ? null : value
  }
  let input = value
  // 数字时间戳
  if (typeof input === 'number') {
    // 10 位秒级时间戳补齐为毫秒
    if (input.toString().length === 10) {
      input = input * 1000
    }
  } else if (typeof input === 'string') {
    // 纯数字字符串按时间戳处理
    if (/^\d+$/.test(input)) {
      let num = parseInt(input, 10)
      if (input.length === 10) {
        num = num * 1000
      }
      input = num
    } else {
      // 字符串预处理：去掉 T、截掉所有小数秒（毫秒/微秒），兼容 - 与 /
      input = input.replace(/T/g, ' ').replace(/\.\d+/g, '').replace(/-/g, '/')
    }
  }
  const date = new Date(input)
  return isNaN(date.getTime()) ? null : date
}

/**
 * 按模板格式化，占位符：y m d h i s
 */
function build(date, template) {
  const map = {
    y: String(date.getFullYear()),
    m: String(date.getMonth() + 1).padStart(2, '0'),
    d: String(date.getDate()).padStart(2, '0'),
    h: String(date.getHours()).padStart(2, '0'),
    i: String(date.getMinutes()).padStart(2, '0'),
    s: String(date.getSeconds()).padStart(2, '0')
  }
  return template.replace(/{(y|m|d|h|i|s)}/g, (_, key) => map[key])
}

/** 日期时间：yyyy-MM-dd HH:mm:ss */
export function formatDateTime(value) {
  const date = normalize(value)
  return date ? build(date, '{y}-{m}-{d} {h}:{i}:{s}') : PLACEHOLDER
}

/** 仅日期：yyyy-MM-dd */
export function formatDate(value) {
  const date = normalize(value)
  return date ? build(date, '{y}-{m}-{d}') : PLACEHOLDER
}

/** 仅时间：HH:mm:ss */
export function formatTime(value) {
  const date = normalize(value)
  return date ? build(date, '{h}:{i}:{s}') : PLACEHOLDER
}

/** 年月：yyyy-MM */
export function formatYearMonth(value) {
  const date = normalize(value)
  return date ? build(date, '{y}-{m}') : PLACEHOLDER
}

/** 日期时间到分钟：yyyy-MM-dd HH:mm */
export function formatDateTimeMinute(value) {
  const date = normalize(value)
  return date ? build(date, '{y}-{m}-{d} {h}:{i}') : PLACEHOLDER
}

export default {
  formatDateTime,
  formatDate,
  formatTime,
  formatYearMonth,
  formatDateTimeMinute
}
