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
  border-radius: 4px;
  color: var(--navbar-text, #5a5e66);
  transition: color 0.2s ease, transform 0.2s ease;
}

.hamburger-inner:hover {
  color: var(--current-color, #409eff);
}

.hamburger-inner:focus-visible {
  box-shadow: 0 0 0 2px var(--current-color-light, rgba(64, 158, 255, 0.35));
}

.hamburger-chevron {
  font-size: 18px;
  transition: transform 0.22s cubic-bezier(0.34, 1.4, 0.64, 1);
}
</style>
