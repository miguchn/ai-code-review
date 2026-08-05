<template>
  <div class="message-content-view">
    <div v-if="displayTitle" class="message-content-view__title">{{ displayTitle }}</div>
    <div
      v-if="safeHtml"
      class="message-content-view__body markdown-body"
      v-html="safeHtml"
    />
    <div v-else class="message-content-view__empty">暂无消息内容</div>
  </div>
</template>

<script setup>
import { renderMarkdown } from '@/utils/markdown'

const props = defineProps({
  /** 消息标题（纯文本） */
  title: { type: String, default: '' },
  /** 消息正文（简化 Markdown） */
  body: { type: String, default: '' }
})

const displayTitle = computed(() => {
  const value = props.title == null ? '' : String(props.title).trim()
  return value
})

const safeHtml = computed(() => {
  return renderMarkdown(props.body)
})
</script>

<style scoped>
.message-content-view {
  color: var(--text-regular);
  font-size: 14px;
  line-height: 1.65;
}

.message-content-view__title {
  margin: 0 0 12px;
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 600;
  line-height: 1.4;
  word-break: break-word;
}

.message-content-view__empty {
  color: var(--text-secondary);
  padding: 24px 0;
  text-align: center;
}

.message-content-view__body :deep(h1),
.message-content-view__body :deep(h2),
.message-content-view__body :deep(h3),
.message-content-view__body :deep(h4) {
  margin: 1em 0 0.5em;
  color: var(--text-primary);
  font-weight: 600;
  line-height: 1.35;
}

.message-content-view__body :deep(h1) { font-size: 1.35em; }
.message-content-view__body :deep(h2) { font-size: 1.2em; }
.message-content-view__body :deep(h3) { font-size: 1.1em; }
.message-content-view__body :deep(h4) { font-size: 1em; }

.message-content-view__body :deep(p) {
  margin: 0.55em 0;
}

.message-content-view__body :deep(ul),
.message-content-view__body :deep(ol) {
  margin: 0.55em 0;
  padding-left: 1.4em;
}

.message-content-view__body :deep(li) {
  margin: 0.25em 0;
}

.message-content-view__body :deep(strong) {
  color: var(--text-primary);
  font-weight: 600;
}

.message-content-view__body :deep(a) {
  color: var(--el-color-primary);
  text-decoration: none;
}

.message-content-view__body :deep(a:hover) {
  color: var(--el-color-primary-light-3);
  text-decoration: underline;
}

.message-content-view__body :deep(code) {
  padding: 0.1em 0.35em;
  border-radius: 4px;
  background: var(--neutral-code);
  color: var(--text-regular);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 0.92em;
}

.message-content-view__body :deep(pre) {
  margin: 0.75em 0;
  padding: 12px 14px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--neutral-code);
  overflow-x: auto;
}

.message-content-view__body :deep(pre code) {
  padding: 0;
  background: transparent;
}

.message-content-view__body :deep(hr) {
  margin: 1em 0;
  border: none;
  border-top: 1px solid var(--divider);
}

.message-content-view__body :deep(blockquote) {
  margin: 0.75em 0;
  padding: 0.2em 0 0.2em 0.9em;
  border-left: 3px solid var(--brand-border);
  color: var(--text-secondary);
}
</style>
