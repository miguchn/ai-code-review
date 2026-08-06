<template>
  <div class="guide-content">
    <div v-if="loading" class="guide-content__state">加载中…</div>
    <div v-else-if="missing" class="guide-content__state">该文档暂未提供</div>
    <div v-else ref="bodyRef" class="guide-content__body guide-md" v-html="html" @click="onBodyClick" />
  </div>
</template>

<script setup>
import { renderMarkdown } from '@/utils/markdown'
import { loadDocContent, getAssetUrl } from '../loader'

const props = defineProps({
  /** manifest 文档条目 */
  doc: { type: Object, required: true }
})

const html = ref('')
const loading = ref(false)
const missing = ref(false)
const bodyRef = ref(null)

watch(() => props.doc?.id, load, { immediate: true })

async function load() {
  if (!props.doc) return
  loading.value = true
  missing.value = false
  const raw = await loadDocContent(props.doc.file)
  loading.value = false
  if (raw == null) {
    html.value = ''
    missing.value = true
    return
  }
  html.value = renderMarkdown(raw)
  await nextTick()
  enhanceBody()
}

/** 代码块外包一层并追加复制按钮；markdown 图片文件名重写为构建期 URL */
function enhanceBody() {
  const root = bodyRef.value
  if (!root) return
  root.querySelectorAll('pre').forEach(pre => {
    if (pre.parentElement?.classList.contains('code-block')) return
    const wrap = document.createElement('div')
    wrap.className = 'code-block'
    pre.parentNode.insertBefore(wrap, pre)
    wrap.appendChild(pre)
    const btn = document.createElement('button')
    btn.type = 'button'
    btn.className = 'code-block__copy'
    btn.textContent = '复制'
    wrap.appendChild(btn)
  })
  root.querySelectorAll('img').forEach(img => {
    const name = (img.getAttribute('src') || '').split('/').pop()
    const url = name ? getAssetUrl(name) : ''
    if (url) {
      img.src = url
      img.classList.add('guide-md__img')
    }
  })
}

function onBodyClick(e) {
  const btn = e.target.closest('.code-block__copy')
  if (!btn) return
  const pre = btn.parentElement?.querySelector('pre')
  copyText(pre?.innerText || '', btn)
}

/** 复制（含 http 内网非安全上下文降级），成功后按钮短暂反馈 */
function copyText(text, btn) {
  const done = () => {
    btn.textContent = '已复制'
    setTimeout(() => { btn.textContent = '复制' }, 1200)
  }
  if (navigator.clipboard?.writeText) {
    navigator.clipboard.writeText(text).then(done).catch(() => legacyCopy(text, done))
  } else {
    legacyCopy(text, done)
  }
}

function legacyCopy(text, done) {
  const ta = document.createElement('textarea')
  ta.value = text
  ta.style.position = 'fixed'
  ta.style.opacity = '0'
  document.body.appendChild(ta)
  ta.select()
  try { document.execCommand('copy'); done() } catch { /* 忽略：无剪切板权限 */ }
  document.body.removeChild(ta)
}
</script>

<style lang="scss" scoped>
.guide-content {
  color: var(--text-regular);
  font-size: 14px;
  line-height: 1.75;
}

.guide-content__state {
  padding: 40px 0;
  color: var(--text-secondary);
  font-size: 13px;
  text-align: center;
}

/* 篇名 H1 已上移到抽屉常驻篇目栏，正文内隐藏（仅当它是正文第一个元素时） */
.guide-content__body :deep(h1:first-child) {
  display: none;
}

/* 篇名后的「这篇帮你…」首段渲染为浅底引言卡 */
.guide-content__body :deep(h1:first-child + p) {
  margin: 0 0 20px;
  padding: 10px 14px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--neutral-content);
  color: var(--text-secondary);
}

/* 标题层级：绝对字号 + 段前距 + h2 细分隔线，与正文明确分区 */
.guide-content__body :deep(h2),
.guide-content__body :deep(h3),
.guide-content__body :deep(h4) {
  color: var(--text-primary);
  font-weight: 600;
  line-height: 1.4;
}

.guide-content__body :deep(h2) {
  margin: 28px 0 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-light);
  font-size: 16px;
}

.guide-content__body :deep(h3) {
  margin: 22px 0 8px;
  font-size: 15px;
}

.guide-content__body :deep(h4) {
  margin: 16px 0 6px;
  font-size: 14px;
}

.guide-content__body :deep(p) {
  margin: 0.7em 0;
}

.guide-content__body :deep(ul),
.guide-content__body :deep(ol) {
  margin: 0.7em 0;
  padding-left: 1.4em;
}

.guide-content__body :deep(li) {
  margin: 0.4em 0;
}

.guide-content__body :deep(li::marker) {
  color: var(--text-assist);
}

.guide-content__body :deep(strong) {
  color: var(--text-primary);
  font-weight: 600;
}

.guide-content__body :deep(a) {
  color: var(--el-color-primary);
  text-decoration: none;
}

.guide-content__body :deep(a:hover) {
  color: var(--el-color-primary-light-3);
  text-decoration: underline;
}

.guide-content__body :deep(code) {
  padding: 0.1em 0.35em;
  border-radius: 4px;
  background: var(--neutral-code);
  color: var(--text-regular);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 0.9em;
}

.guide-content__body :deep(pre) {
  margin: 0.85em 0;
  padding: 12px 14px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--neutral-code);
  font-size: 13px;
  line-height: 1.6;
  overflow-x: auto;
}

.guide-content__body :deep(pre code) {
  padding: 0;
  background: transparent;
  font-size: inherit;
}

.guide-content__body :deep(hr) {
  margin: 1.5em 0;
  border: none;
  border-top: 1px solid var(--divider);
}

.guide-content__body :deep(blockquote) {
  margin: 0.85em 0;
  padding: 4px 0 4px 12px;
  border-left: 3px solid var(--brand-border);
  color: var(--text-secondary);
}

/* 表格 / 代码块复制按钮 / 图片（均为运行时 DOM，需 :deep 命中） */
.guide-content__body :deep(table) {
  width: 100%;
  margin: 0.85em 0;
  border-collapse: collapse;
  font-size: 13px;
}
.guide-content__body :deep(th),
.guide-content__body :deep(td) {
  padding: 8px 12px;
  border: 1px solid var(--border-light);
  text-align: left;
}
.guide-content__body :deep(thead th) {
  background: var(--neutral-content);
  color: var(--text-primary);
  font-weight: 600;
  white-space: nowrap;
}
.guide-content__body :deep(.code-block) {
  position: relative;
}
.guide-content__body :deep(.code-block__copy) {
  position: absolute;
  top: 6px;
  right: 6px;
  padding: 2px 8px;
  border: 1px solid var(--border-light);
  border-radius: 4px;
  background: var(--neutral-card);
  color: var(--text-secondary);
  font-size: 12px;
  cursor: pointer;
}
.guide-content__body :deep(.code-block__copy:hover) {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary);
}
.guide-content__body :deep(.guide-md__img) {
  max-width: 100%;
  border: 1px solid var(--border-light);
  border-radius: 8px;
}
</style>
