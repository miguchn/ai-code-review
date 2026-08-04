<template>
  <section class="wb-panel">
    <header class="wb-panel-head">
      <h3 class="wb-panel-title">最近动态</h3>
      <el-button link type="primary" size="small" @click="$emit('view-all')">查看全部</el-button>
    </header>
    <div v-if="error" class="wb-block-state">
      <span>动态加载失败</span>
      <el-button link type="primary" size="small" @click="$emit('retry')">重试</el-button>
    </div>
    <el-empty v-else-if="!recent.length" description="暂无审查动态" :image-size="56" />
    <ul v-else class="wb-recent-list">
      <li
        v-for="(item, idx) in recent"
        :key="idx"
        class="wb-recent-item"
        role="button"
        tabindex="0"
        @click="$emit('open', item)"
        @keyup.enter="$emit('open', item)"
      >
        <span class="wb-recent-dot" :style="{ background: conclusionColor(item.conclusion) }" />
        <span class="wb-recent-title">{{ item.title }}</span>
        <el-tooltip v-if="item.time" :content="item.time" placement="top">
          <span class="wb-rel-time">{{ relativeTime(item.time) }}</span>
        </el-tooltip>
      </li>
    </ul>
  </section>
</template>

<script setup>
import { CONCLUSION_META } from '../constants'
import { relativeTime } from '../utils'

defineProps({
  recent: { type: Array, default: () => [] },
  error: { type: Boolean, default: false }
})

defineEmits(['open', 'retry', 'view-all'])

function conclusionColor(conclusion) {
  const meta = conclusion ? CONCLUSION_META[conclusion] : null
  return meta ? `var(${meta.colorVar})` : 'var(--border-default)'
}
</script>

<style scoped lang="scss">
.wb-recent-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.wb-recent-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: var(--radius-control, 8px);
  cursor: pointer;
  transition: background 0.15s ease;

  &:hover,
  &:focus-visible {
    background: var(--bg-hover);
    outline: none;
  }

  & + & {
    margin-top: 2px;
  }
}

.wb-recent-dot {
  flex-shrink: 0;
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.wb-recent-title {
  min-width: 0;
  flex: 1;
  color: var(--text-regular);
  font-size: 14px;
  line-height: 22px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wb-rel-time {
  flex-shrink: 0;
  color: var(--text-assist);
  font-size: 12px;
  line-height: 18px;
  cursor: default;
}
</style>
