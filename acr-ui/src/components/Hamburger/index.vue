<template>
  <div class="hamburger-trigger" @click="toggleClick">
    <el-tooltip :content="tooltipText" placement="bottom" :show-after="300">
      <div
        class="hamburger-inner"
        role="button"
        tabindex="0"
        :aria-expanded="isActive"
        :aria-label="tooltipText"
        @keydown.enter.prevent="toggleClick"
        @keydown.space.prevent="toggleClick"
      >
        <el-icon class="hamburger-chevron">
          <DArrowLeft v-if="isActive" />
          <DArrowRight v-else />
        </el-icon>
      </div>
    </el-tooltip>
  </div>
</template>

<script setup>
import { DArrowLeft, DArrowRight } from '@element-plus/icons-vue'

const props = defineProps({
  isActive: {
    type: Boolean,
    default: false
  }
})

const tooltipText = computed(() => (props.isActive ? '收起侧边栏' : '展开侧边栏'))

const emit = defineEmits(['toggleClick'])
const toggleClick = () => {
  emit('toggleClick')
}
</script>

<style scoped>
.hamburger-trigger {
  padding: 0 15px;
  height: 100%;
  display: flex;
  align-items: center;
}

.hamburger-inner {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  outline: none;
  border-radius: var(--radius-control);
  color: var(--navbar-text);
  transition: color 0.15s ease-out, background-color 0.15s ease-out;
}

.hamburger-inner:hover {
  color: var(--brand-600);
  background: var(--bg-hover);
}

.hamburger-inner:focus-visible {
  box-shadow: 0 0 0 3px rgba(22, 163, 74, 0.16);
}

.hamburger-chevron {
  font-size: 18px;
  transition: transform 0.18s ease-out;
}
</style>
