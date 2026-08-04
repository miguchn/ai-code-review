<template>
  <section class="wb-panel">
    <header class="wb-panel-head">
      <h3 class="wb-panel-title">模型运行情况</h3>
      <el-button v-if="canManage" link type="primary" size="small" @click="$emit('go-models')">管理</el-button>
    </header>
    <div v-if="error" class="wb-block-state">
      <span>模型状态加载失败</span>
      <el-button link type="primary" size="small" @click="$emit('retry')">重试</el-button>
    </div>
    <div v-else-if="loading && !models" class="wb-block-state">加载中…</div>
    <el-empty v-else-if="!models || models.enabledCount === 0" description="暂无启用的模型服务" :image-size="56">
      <el-button v-if="canManage" type="primary" size="small" @click="$emit('go-models')">去配置</el-button>
    </el-empty>
    <template v-else>
      <div class="wb-models-summary">
        <span class="wb-models-stat"><strong>{{ models.enabledCount }}</strong> 个启用</span>
        <span class="wb-models-stat"><strong :class="{ 'is-danger': models.onlineCount < models.enabledCount }">{{ models.onlineCount }}</strong> 个检测正常</span>
        <span v-if="models.lastCheckTime" class="wb-models-stat">
          最近检测
          <el-tooltip :content="models.lastCheckTime" placement="top">
            <span class="wb-models-time-link">{{ relativeTime(models.lastCheckTime) }}</span>
          </el-tooltip>
        </span>
      </div>
      <ul class="wb-models-list">
        <li v-for="(item, idx) in models.items" :key="idx" class="wb-models-item">
          <span class="wb-models-dot" :class="`is-${tone(item.checkStatus)}`" />
          <div class="wb-models-main">
            <div class="wb-models-name">
              {{ item.modelName }}
              <span v-if="item.isDefault" class="wb-models-tag">默认</span>
            </div>
            <div class="wb-models-meta">{{ item.providerLabel }} · {{ item.model }}</div>
          </div>
          <div class="wb-models-status">
            <span class="wb-models-check" :class="`is-${tone(item.checkStatus)}`">{{ label(item.checkStatus) }}</span>
            <el-tooltip v-if="item.lastCheckResult" :content="item.lastCheckResult" placement="top">
              <span class="wb-models-time">{{ relativeTime(item.lastCheckTime) }}</span>
            </el-tooltip>
          </div>
        </li>
      </ul>
    </template>
  </section>
</template>

<script setup>
import { relativeTime } from '../utils'

defineProps({
  models: { type: Object, default: null },
  loading: { type: Boolean, default: false },
  error: { type: Boolean, default: false },
  canManage: { type: Boolean, default: false }
})

defineEmits(['retry', 'go-models'])

const TONE = { SUCCESS: 'success', FAILED: 'danger', NEVER: 'muted' }
const LABEL = { SUCCESS: '检测正常', FAILED: '检测异常', NEVER: '未检测' }

function tone(status) {
  return TONE[status] || 'muted'
}

function label(status) {
  return LABEL[status] || '未检测'
}
</script>

<style scoped lang="scss">
.wb-models-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--divider);
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 20px;
}

.wb-models-stat strong {
  margin-right: 2px;
  color: var(--text-primary);
  font-weight: 600;
  font-variant-numeric: tabular-nums;

  &.is-danger {
    color: var(--status-danger-text);
  }
}

.wb-models-time-link {
  color: var(--text-regular);
  cursor: default;
}

.wb-models-list {
  margin: 4px 0 0;
  padding: 0;
  list-style: none;
}

.wb-models-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;

  & + & {
    border-top: 1px solid var(--divider);
  }
}

.wb-models-dot {
  flex-shrink: 0;
  width: 8px;
  height: 8px;
  border-radius: 50%;

  &.is-success { background: var(--status-success-icon); }
  &.is-danger { background: var(--status-danger-text); }
  &.is-muted { background: var(--border-default); }
}

.wb-models-main {
  min-width: 0;
  flex: 1;
}

.wb-models-name {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-regular);
  font-size: 14px;
  line-height: 20px;
  font-weight: 500;
}

.wb-models-tag {
  padding: 0 6px;
  background: var(--brand-bg-soft);
  border: 1px solid var(--brand-border);
  border-radius: 4px;
  color: var(--brand-text);
  font-size: 12px;
  line-height: 18px;
  font-weight: 500;
}

.wb-models-meta {
  margin-top: 1px;
  color: var(--text-assist);
  font-size: 12px;
  line-height: 18px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wb-models-status {
  flex-shrink: 0;
  text-align: right;
}

.wb-models-check {
  display: block;
  font-size: 12px;
  line-height: 18px;

  &.is-success { color: var(--status-success-text); }
  &.is-danger { color: var(--status-danger-text); }
  &.is-muted { color: var(--text-assist); }
}

.wb-models-time {
  display: block;
  margin-top: 1px;
  color: var(--text-assist);
  font-size: 12px;
  line-height: 18px;
  cursor: default;
}
</style>
