import { marked } from 'marked'
import DOMPurify from 'dompurify'

marked.setOptions({ gfm: true, breaks: true })

// 链接统一新标签打开，并阻断 opener 引用（模块级注册一次，多处消费共享）
DOMPurify.addHook('afterSanitizeAttributes', (node) => {
  if (node.tagName === 'A') {
    node.setAttribute('target', '_blank')
    node.setAttribute('rel', 'noopener noreferrer')
  }
})

/**
 * 渲染简化 Markdown 为消毒后的 HTML 字符串。
 * @param {string} raw markdown 原文
 * @returns {string} 可安全 v-html 的 HTML；空输入返回 ''
 */
export function renderMarkdown(raw) {
  const text = raw == null ? '' : String(raw)
  if (!text.trim()) return ''
  const html = marked.parse(text, { async: false })
  return DOMPurify.sanitize(typeof html === 'string' ? html : String(html))
}
