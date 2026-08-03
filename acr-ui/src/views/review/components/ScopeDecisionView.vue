<template>
  <div v-if="decision" class="sd">
    <el-alert
      v-if="decision.degraded"
      type="warning"
      :closable="false"
      show-icon
      :title="degradedTitle"
    />

    <div v-else class="sd-panel">
      <div class="sd-summary">
        <el-tag v-if="isOcr" size="small" type="info" effect="plain">OCR 引擎路径</el-tag>
        <span v-if="summaryText" class="sd-summary-text">{{ summaryText }}</span>
        <span class="sd-summary-meta">
          <span v-if="finalDiffChars">注入 Diff {{ finalDiffChars }}</span>
          <span v-if="decision.appliedExcludeGlobs != null">--exclude {{ decision.appliedExcludeGlobs }} 条</span>
        </span>
      </div>

      <div v-if="configItems.length" class="sd-config">
        <span class="sd-config-label">生效配置</span>
        <span class="sd-config-text">{{ configItems.join(' · ') }}</span>
      </div>

      <el-alert
        v-if="decision.note"
        type="info"
        :closable="false"
        show-icon
        class="sd-note"
        :title="decision.note"
      />

      <div v-if="sections.length" class="sd-sections">
        <div v-for="sec in sections" :key="sec.key" class="sd-section">
          <button type="button" class="sd-section-head" :aria-expanded="isOpen(sec.key)" @click="toggle(sec.key)">
            <el-icon class="sd-chevron" :class="{ open: isOpen(sec.key) }"><ArrowRight /></el-icon>
            <span class="sd-section-title">{{ sec.title }}</span>
            <span class="sd-section-count">{{ sec.rows.length }}</span>
          </button>
          <div v-show="isOpen(sec.key)" class="sd-section-body">
            <div v-for="(row, i) in sec.rows" :key="row.path || row.text || i" class="sd-row">
              <span v-if="row.text != null" class="sd-warn-text">{{ row.text }}</span>
              <template v-else>
                <span class="sd-path" :class="{ 'sd-path-warn': row.warn }">{{ row.path }}</span>
                <span v-if="(row.tags && row.tags.length) || row.reason" class="sd-row-meta">
                  <el-tag v-for="tag in row.tags" :key="tag.label" size="small" :type="tag.type" effect="plain">
                    {{ tag.label }}
                  </el-tag>
                  <span v-if="row.reason" class="sd-reason">{{ row.reason }}</span>
                </span>
              </template>
            </div>
            <p v-if="sec.hint" class="sd-hint">{{ sec.hint }}</p>
          </div>
        </div>
      </div>
      <p v-else class="sd-empty">全部文件均纳入审查，无排除/扩展/截断。</p>
    </div>
  </div>
  <span v-else class="sd-empty">—</span>
</template>

<script setup name="ScopeDecisionView">
import {
  parseScopeDecision, scopeDecisionSummary,
  scopeExcludeReasonLabel, scopeRecordReasonLabel,
  scopeExpandRuleLabel, scopeExpandStatusLabel, scopeExpandStatusTagType
} from '@/utils/reviewDisplay'

const props = defineProps({
  run: { type: Object, default: null }
})

const decision = computed(() => parseScopeDecision(props.run))
const isOcr = computed(() => decision.value?.pathMode === 'OCR_ENGINE')
const summaryText = computed(() => scopeDecisionSummary(decision.value))

const degradedTitle = computed(() => {
  const reason = decision.value?.reason
  return '范围决策未生效，按全量变更审查' + (reason ? `（${reason}）` : '')
})

const finalDiffChars = computed(() => {
  const chars = decision.value?.finalDiffChars
  return chars ? `${Number(chars).toLocaleString()} 字符` : ''
})

function list(value) {
  return Array.isArray(value) ? value : []
}

const configItems = computed(() => {
  const config = decision.value?.config
  if (!config) return []
  const items = []
  const patterns = Array.isArray(config.excludePatterns) ? config.excludePatterns.length : 0
  items.push(`项目排除 ${patterns} 条`)
  items.push(config.includeTests ? '审查测试文件' : '排除测试文件')
  items.push(config.reportExisting ? '上报存量问题' : '不上报存量问题')
  items.push(config.expandEnabled === false ? '高影响扩展关' : '高影响扩展开')
  return items
})

/** 分组统一归一为 { key, title, hint?, rows: [{ path?, text?, warn?, tags?, reason? }] }，模板一套渲染。 */
const sections = computed(() => {
  const d = decision.value
  if (!d) return []
  const secs = []
  const included = list(d.includedFiles)
  if (included.length) {
    secs.push({ key: 'included', title: '纳入审查', rows: included.map(path => ({ path })) })
  }
  const excluded = list(d.excludedFiles)
  if (excluded.length) {
    secs.push({
      key: 'excluded', title: '排除',
      rows: excluded.map(f => ({ path: f.path, tags: [{ label: scopeExcludeReasonLabel(f.reason), type: 'info' }] }))
    })
  }
  const expanded = list(d.expandedFiles)
  if (expanded.length) {
    secs.push({
      key: 'expanded', title: '高影响扩展',
      rows: expanded.map(f => ({
        path: f.path,
        tags: [
          { label: scopeExpandRuleLabel(f.rule), type: 'info' },
          f.status ? { label: scopeExpandStatusLabel(f.status), type: scopeExpandStatusTagType(f.status) } : null
        ].filter(Boolean),
        reason: f.reason
      }))
    })
  }
  const recordOnly = list(d.recordOnlyFiles)
  if (recordOnly.length) {
    secs.push({
      key: 'record', title: '记录类变更',
      rows: recordOnly.map(f => ({ path: f.path, tags: [{ label: scopeRecordReasonLabel(f.reason), type: 'info' }] }))
    })
  }
  const dropped = list(d.droppedFiles)
  if (dropped.length) {
    secs.push({ key: 'dropped', title: '预算截断丢弃', rows: dropped.map(path => ({ path, warn: true })) })
  }
  const warnings = list(d.parseWarnings)
  if (warnings.length) {
    secs.push({ key: 'warnings', title: '解析警告', rows: warnings.map(text => ({ text })) })
  }
  const skipped = list(d.skippedExcludePatterns)
  if (skipped.length) {
    secs.push({
      key: 'skipped', title: '未传入的排除规则',
      hint: '含逗号的 glob 无法经 CLI --exclude 表达，未生效。',
      rows: skipped.map(path => ({ path }))
    })
  }
  return secs
})

const openKeys = ref(new Set())
watch(sections, secs => {
  openKeys.value = new Set(secs.length ? [secs[0].key] : [])
}, { immediate: true })

function isOpen(key) {
  return openKeys.value.has(key)
}

function toggle(key) {
  const next = new Set(openKeys.value)
  if (next.has(key)) {
    next.delete(key)
  } else {
    next.add(key)
  }
  openKeys.value = next
}
</script>

<style scoped>
.sd { min-width: 320px; }

.sd-panel {
  padding: 12px 16px;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.sd-summary {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 4px 10px;
}
.sd-summary-text { font-size: 13px; font-weight: 600; color: var(--el-text-color-primary); }
.sd-summary-meta {
  display: inline-flex;
  gap: 10px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.sd-config {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-top: 6px;
  font-size: 12px;
}
.sd-config-label { flex: none; font-weight: 600; color: var(--el-text-color-regular); }
.sd-config-text { color: var(--el-text-color-secondary); }

.sd-note { margin-top: 8px; }

.sd-sections { margin-top: 8px; border-top: 1px solid var(--el-border-color-lighter); }
.sd-section + .sd-section { border-top: 1px solid var(--el-border-color-extra-light); }

.sd-section-head {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  padding: 7px 2px;
  background: none;
  border: 0;
  cursor: pointer;
}
.sd-section-head:hover .sd-section-title { color: var(--el-text-color-primary); }
.sd-section-head:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 1px;
  border-radius: 4px;
}
.sd-chevron {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  transition: transform 0.16s ease-out;
}
.sd-chevron.open { transform: rotate(90deg); }
.sd-section-title { font-size: 13px; font-weight: 600; color: var(--el-text-color-regular); }
.sd-section-count { font-size: 12px; color: var(--el-text-color-secondary); }

.sd-section-body {
  max-height: 168px;
  padding: 0 2px 10px 20px;
  overflow-y: auto;
}
.sd-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  padding: 2px 0;
}
.sd-path {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  color: var(--el-text-color-regular);
  word-break: break-all;
}
.sd-path-warn { color: var(--el-color-warning); }
.sd-row-meta { display: inline-flex; align-items: center; flex: none; gap: 6px; }
.sd-reason { font-size: 12px; color: var(--el-text-color-secondary); }
.sd-warn-text { font-size: 12px; color: var(--el-color-warning); }

.sd-hint { margin: 6px 0 0; font-size: 12px; color: var(--el-text-color-secondary); }
.sd-empty { margin: 0; font-size: 12px; color: var(--el-text-color-secondary); }
</style>
