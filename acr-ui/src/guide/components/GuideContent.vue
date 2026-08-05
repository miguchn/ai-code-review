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
  line-height: 1.65;
}

.guide-content__state {
  padding: 40px 0;
  color: var(--text-secondary);
  font-size: 13px;
  text-align: center;
}

/* 标题/段落/列表/行内代码/引用块样式照搬 MessageContentView.vue 的 .markdown-body 规则，选择器改为 .guide-content__body :deep(...) */
.guide-content__body :deep(h1),
.guide-content__body :deep(h2),
.guide-content__body :deep(h3),
.guide-content__body :deep(h4) {
  margin: 1em 0 0.5em;
  color: var(--text-primary);
  font-weight: 600;
  line-height: 1.35;
}

.guide-content__body :deep(h1) { font-size: 1.35em; }
.guide-content__body :deep(h2) { font-size: 1.2em; }
.guide-content__body :deep(h3) { font-size: 1.1em; }
.guide-content__body :deep(h4) { font-size: 1em; }

.guide-content__body :deep(p) {
  margin: 0.55em 0;
}

.guide-content__body :deep(ul),
.guide-content__body :deep(ol) {
  margin: 0.55em 0;
  padding-left: 1.4em;
}

.guide-content__body :deep(li) {
  margin: 0.25em 0;
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
  font-size: 0.92em;
}

.guide-content__body :deep(pre) {
  margin: 0.75em 0;
  padding: 12px 14px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--neutral-code);
  overflow-x: auto;
}

.guide-content__body :deep(pre code) {
  padding: 0;
  background: transparent;
}

.guide-content__body :deep(hr) {
  margin: 1em 0;
  border: none;
  border-top: 1px solid var(--divider);
}

.guide-content__body :deep(blockquote) {
  margin: 0.75em 0;
  padding: 0.2em 0 0.2em 0.9em;
  border-left: 3px solid var(--brand-border);
  color: var(--text-secondary);
}

/* 追加：表格 / 代码块复制按钮 / 图片（均为运行时 DOM，需 :deep 命中） */
.guide-content__body :deep(table) {
  width: 100%;
  margin: 0.75em 0;
  border-collapse: collapse;
  font-size: 0.95em;
}
.guide-content__body :deep(th),
.guide-content__body :deep(td) {
  padding: 6px 10px;
  border: 1px solid var(--border-light);
  text-align: left;
}
.guide-content__body :deep(thead th) {
  background: var(--neutral-card);
  color: var(--text-primary);
  font-weight: 600;
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
