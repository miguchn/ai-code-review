<template>
  <section class="wb-panel">
    <header class="wb-panel-head">
      <h3 class="wb-panel-title">今日待办</h3>
      <span class="wb-panel-extra">点击卡片直达对应列表</span>
    </header>
    <div v-if="error" class="wb-block-state">
      <span>待办加载失败</span>
      <el-button link type="primary" size="small" @click="$emit('retry')">重试</el-button>
    </div>
    <div v-else-if="loading && !cards.length" class="wb-block-state">加载中…</div>
    <el-empty v-else-if="!cards.length" description="暂无待办事项" :image-size="56" />
    <div v-else class="wb-todo-grid">
      <div
        v-for="card in cards"
        :key="card.type"
        class="wb-todo-card"
        :class="[`is-${tone(card.type)}`, { 'is-zero': card.count === 0 }]"
        role="button"
        tabindex="0"
        @click="$emit('open', card)"
        @keyup.enter="$emit('open', card)"
      >
        <div class="wb-todo-head">
          <svg-icon :icon-class="cardIcon(card.type)" class-name="wb-todo-icon" />
          <span class="wb-todo-title">{{ card.title }}</span>
        </div>
        <div class="wb-todo-count">{{ card.count }}</div>
        <div v-if="card.subtitle" class="wb-todo-subtitle">{{ card.subtitle }}</div>
      </div>
    </div>
  </section>
</template>

<script setup>
defineProps({
  cards: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  error: { type: Boolean, default: false }
})

defineEmits(['open', 'retry'])

const CARD_ICON = {
  ISSUE_AWAITING_CONFIRM: 'bug',
  ISSUE_EXISTING_CONFIRM: 'clipboard',
  ISSUE_AWAITING_FIX: 'edit',
  HIGH_RISK_CONCLUSION: 'validCode',
  TASK_FAILED: 'job',
  DELIVERY_FAILED: 'message'
}

const CARD_TONE = {
  ISSUE_AWAITING_CONFIRM: 'info',
  ISSUE_EXISTING_CONFIRM: 'muted',
  ISSUE_AWAITING_FIX: 'warning',
  HIGH_RISK_CONCLUSION: 'danger',
  TASK_FAILED: 'danger',
  DELIVERY_FAILED: 'warning'
}

function cardIcon(type) {
  return CARD_ICON[type] || 'dashboard'
}

function tone(type) {
  return CARD_TONE[type] || 'muted'
}
</script>

<style scoped lang="scss">
.wb-todo-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

@media (max-width: 768px) {
  .wb-todo-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.wb-todo-card {
  position: relative;
  padding: 14px 16px 14px 20px;
  background: var(--neutral-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-card, 12px);
  cursor: pointer;
  transition: border-color 0.15s ease, background 0.15s ease;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 14px;
    bottom: 14px;
    width: 3px;
    border-radius: 0 2px 2px 0;
    background: var(--border-default);
  }

  &.is-info::before { background: var(--status-info-icon); }
  &.is-warning::before { background: var(--status-warning-icon); }
  &.is-danger::before { background: var(--status-danger-text); }

  &:hover,
  &:focus-visible {
    border-color: var(--brand-border);
    outline: none;
  }
}

.wb-todo-head {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-regular);
  font-size: 13px;
  line-height: 20px;
}

.wb-todo-icon {
  font-size: 15px;
  color: var(--text-secondary);

  .is-info & { color: var(--status-info-icon); }
  .is-warning & { color: var(--status-warning-icon); }
  .is-danger & { color: var(--status-danger-text); }
}

.wb-todo-count {
  margin-top: 6px;
  color: var(--text-primary);
  font-size: 26px;
  line-height: 32px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;

  .is-zero & {
    color: var(--text-placeholder);
  }
}

.wb-todo-subtitle {
  margin-top: 2px;
  color: var(--text-assist);
  font-size: 12px;
  line-height: 18px;
}
</style>
