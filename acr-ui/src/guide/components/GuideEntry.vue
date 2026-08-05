<template>
  <el-tooltip content="功能助手" effect="dark" placement="bottom">
    <div id="guide-entry" class="right-menu-item hover-effect guide-entry" @click="guideStore.open()">
      <el-icon :size="18"><QuestionFilled /></el-icon>
    </div>
  </el-tooltip>
</template>

<script setup>
import useGuideStore from '@/store/modules/guide'

const guideStore = useGuideStore()
</script>

<style scoped>
/* 根节点为 el-tooltip 时 Navbar 的 scoped 样式与 id 透传均不落底，
   按 brief Step 4 兜底：入口的 padding/hover 等在组件内自带 */
.guide-entry {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 6px;
  height: 100%;
  color: var(--navbar-text);
  cursor: pointer;
  transition: background-color 0.15s ease-out, box-shadow 0.15s ease-out;
}

.guide-entry:hover {
  background: var(--bg-hover);
}
</style>
