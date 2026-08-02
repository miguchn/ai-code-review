<template>
  <div v-if="decision" class="scope-decision">
    <el-alert
      v-if="decision.degraded"
      type="warning"
      :closable="false"
      show-icon
      class="scope-alert"
      :title="degradedTitle"
    />

    <template v-else>
      <div class="scope-summary">
        <el-tag v-if="isOcr" size="small" type="info" effect="plain">OCR 引擎路径</el-tag>
        <span v-if="summaryText" class="scope-summary-text">{{ summaryText }}</span>
        <span v-if="finalDiffChars" class="scope-summary-meta">注入 Diff {{ finalDiffChars }}</span>
        <span v-if="decision.appliedExcludeGlobs != null" class="scope-summary-meta">
          --exclude {{ decision.appliedExcludeGlobs }} 条
        </span>
      </div>

      <div v-if="configItems.length" class="scope-config">
        <span class="scope-config-label">生效配置</span>
        <span v-for="item in configItems" :key="item" class="scope-config-item">{{ item }}</span>
      </div>

      <el-alert
        v-if="decision.note"
        type="info"
        :closable="false"
        show-icon
        class="scope-alert"
        :title="decision.note"
      />

      <el-collapse v-if="hasDetails" class="scope-collapse">
        <el-collapse-item v-if="includedFiles.length" :title="`纳入审查（${includedFiles.length}）`" name="included">
          <div v-for="path in includedFiles" :key="path" class="scope-path">{{ path }}</div>
        </el-collapse-item>

        <el-collapse-item v-if="excludedFiles.length" :title="`排除（${excludedFiles.length}）`" name="excluded">
          <div v-for="file in excludedFiles" :key="file.path" class="scope-row">
            <span class="scope-path">{{ file.path }}</span>
            <el-tag size="small" type="info" effect="plain">{{ scopeExcludeReasonLabel(file.reason) }}</el-tag>
          </div>
        </el-collapse-item>

        <el-collapse-item v-if="expandedFiles.length" :title="`高影响扩展（${expandedFiles.length}）`" name="expanded">
          <div v-for="file in expandedFiles" :key="file.path" class="scope-row">
            <span class="scope-path">{{ file.path }}</span>
            <el-tag size="small" effect="plain">{{ scopeExpandRuleLabel(file.rule) }}</el-tag>
            <el-tag v-if="file.status" size="small" :type="scopeExpandStatusTagType(file.status)" effect="plain">
              {{ scopeExpandStatusLabel(file.status) }}
            </el-tag>
            <span v-if="file.reason" class="scope-reason">{{ file.reason }}</span>
          </div>
        </el-collapse-item>

        <el-collapse-item v-if="recordOnlyFiles.length" :title="`记录类变更（${recordOnlyFiles.length}）`" name="record">
          <div v-for="file in recordOnlyFiles" :key="file.path" class="scope-row">
            <span class="scope-path">{{ file.path }}</span>
            <el-tag size="small" type="info" effect="plain">{{ scopeRecordReasonLabel(file.reason) }}</el-tag>
          </div>
        </el-collapse-item>

        <el-collapse-item v-if="droppedFiles.length" :title="`预算截断丢弃（${droppedFiles.length}）`" name="dropped">
          <div v-for="path in droppedFiles" :key="path" class="scope-path scope-dropped">{{ path }}</div>
        </el-collapse-item>

        <el-collapse-item v-if="parseWarnings.length" :title="`解析警告（${parseWarnings.length}）`" name="warnings">
          <div v-for="(warning, index) in parseWarnings" :key="index" class="scope-warning">{{ warning }}</div>
        </el-collapse-item>

        <el-collapse-item v-if="skippedPatterns.length" :title="`未传入的排除规则（${skippedPatterns.length}）`" name="skipped">
          <div v-for="pattern in skippedPatterns" :key="pattern" class="scope-path">{{ pattern }}</div>
          <p class="scope-hint">含逗号的 glob 无法经 CLI --exclude 表达，未生效。</p>
        </el-collapse-item>
      </el-collapse>
      <p v-else class="scope-empty">全部文件均纳入审查，无排除/扩展/截断。</p>
    </template>
  </div>
  <span v-else class="scope-empty">—</span>
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

const includedFiles = computed(() => list(decision.value?.includedFiles))
const excludedFiles = computed(() => list(decision.value?.excludedFiles))
const expandedFiles = computed(() => list(decision.value?.expandedFiles))
const recordOnlyFiles = computed(() => list(decision.value?.recordOnlyFiles))
const droppedFiles = computed(() => list(decision.value?.droppedFiles))
const parseWarnings = computed(() => list(decision.value?.parseWarnings))
const skippedPatterns = computed(() => list(decision.value?.skippedExcludePatterns))

const hasDetails = computed(() =>
  includedFiles.value.length || excludedFiles.value.length || expandedFiles.value.length
  || recordOnlyFiles.value.length || droppedFiles.value.length || parseWarnings.value.length
  || skippedPatterns.value.length)

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

function list(value) {
  return Array.isArray(value) ? value : []
}
</script>

<style scoped>
.scope-decision { min-width: 320px; }
.scope-alert { margin-bottom: 8px; }
.scope-summary {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 13px;
}
.scope-summary-text { font-weight: 600; }
.scope-summary-meta { color: var(--el-text-color-secondary); font-size: 12px; }
.scope-config {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px 12px;
  margin-bottom: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.scope-config-label { font-weight: 600; color: var(--el-text-color-regular); }
.scope-collapse { border-top: 1px solid var(--el-border-color-lighter); }
.scope-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 2px 0;
  font-size: 12px;
}
.scope-path {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  word-break: break-all;
}
.scope-reason { color: var(--el-text-color-secondary); font-size: 12px; }
.scope-dropped { color: var(--el-color-warning); }
.scope-warning { color: var(--el-color-warning); font-size: 12px; }
.scope-hint { margin: 6px 0 0; font-size: 12px; color: var(--el-text-color-secondary); }
.scope-empty { margin: 0; font-size: 12px; color: var(--el-text-color-secondary); }
</style>
