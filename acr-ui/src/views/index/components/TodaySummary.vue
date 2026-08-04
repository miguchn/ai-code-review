<template>
  <section class="wb-panel">
    <header class="wb-panel-head">
      <h3 class="wb-panel-title">审查任务状态</h3>
      <span class="wb-panel-extra">今日</span>
    </header>
    <div v-if="error" class="wb-block-state">
      <span>加载失败</span>
      <el-button link type="primary" size="small" @click="$emit('retry')">重试</el-button>
    </div>
    <div v-else class="wb-today-grid">
      <div class="wb-today-item">
        <div class="wb-today-label">新增任务</div>
        <div class="wb-today-value">{{ display(today.newTasks) }}</div>
      </div>
      <div class="wb-today-item">
        <div class="wb-today-label">成功任务</div>
        <div class="wb-today-value">{{ display(today.successTasks) }}</div>
      </div>
      <div class="wb-today-item">
        <div class="wb-today-label">失败任务</div>
        <div class="wb-today-value" :class="{ 'is-danger': today.failedTasks > 0 }">{{ display(today.failedTasks) }}</div>
      </div>
      <div class="wb-today-item">
        <div class="wb-today-label">关闭问题</div>
        <div class="wb-today-value">{{ display(today.closedIssues) }}</div>
      </div>
    </div>
  </section>
</template>

<script setup>
defineProps({
  today: { type: Object, default: () => ({}) },
  error: { type: Boolean, default: false }
})

defineEmits(['retry'])

function display(value) {
  return value == null ? '—' : value
}
</script>

<style scoped lang="scss">
.wb-today-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.wb-today-item {
  padding: 12px 14px;
  background: var(--neutral-content);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-control, 8px);
}

.wb-today-label {
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 18px;
}

.wb-today-value {
  margin-top: 4px;
  color: var(--text-primary);
  font-size: 22px;
  line-height: 28px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;

  &.is-danger {
    color: var(--status-danger-text);
  }
}
</style>
