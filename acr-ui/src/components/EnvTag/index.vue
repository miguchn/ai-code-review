<template>
  <el-tag :type="tagType" size="small" effect="plain" class="env-tag env-tag-badge">
    <span class="env-tag-badge__inner">{{ label }}</span>
  </el-tag>
</template>

<script setup>
const props = defineProps({
  env: {
    type: String,
    default: 'development'
  }
})

const envMap = {
  development: { label: 'DEV', type: 'warning' },
  uat: { label: 'UAT', type: 'info' }
}

const tagType = envMap[props.env]?.type || 'info'
const label = envMap[props.env]?.label || props.env.toUpperCase()
</script>

<style scoped lang="scss">
/* 顶栏环境标签：短环境代码允许紧凑徽标形态 */
.env-tag.env-tag-badge {
  cursor: default !important;
  display: inline-flex !important;
  align-items: center;
  justify-content: center;
  height: 24px !important;
  padding: 0 8px !important;
  margin: 0 !important;
  border-radius: var(--radius-sm) !important;
  font-size: 11px !important;
  font-weight: 600 !important;
  letter-spacing: 0.04em !important;
  line-height: 1 !important;
  border-width: 1px !important;
  border-style: solid !important;
  vertical-align: middle;
  box-shadow: none;
  transition:
    border-color 0.15s ease-out,
    background-color 0.15s ease-out;

  .env-tag-badge__inner {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-variant-numeric: tabular-nums;
  }

  .env-tag-badge__inner::before {
    content: '';
    flex-shrink: 0;
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: currentColor;
    opacity: 0.92;
    box-shadow: none;
  }
}

@supports ((backdrop-filter: blur(8px)) or (-webkit-backdrop-filter: blur(8px))) {
  .env-tag.env-tag-badge {
    -webkit-backdrop-filter: none;
    backdrop-filter: none;
  }
}

/* DEV — 琥珀 */
.env-tag.env-tag-badge.el-tag--warning {
  color: var(--status-warning-text) !important;
  border-color: var(--status-warning-border) !important;
  background: var(--status-warning-bg) !important;
}

/* UAT / 默认 — 靛蓝 */
.env-tag.env-tag-badge.el-tag--info {
  color: var(--status-info-text) !important;
  border-color: var(--status-info-border) !important;
  background: var(--status-info-bg) !important;
}

html.dark .env-tag.env-tag-badge {
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.06) inset,
    0 8px 22px rgba(0, 0, 0, 0.35);
}

html.dark .env-tag.env-tag-badge.el-tag--warning {
  color: #fcd34d !important;
  border-color: rgba(251, 191, 36, 0.35) !important;
  background: linear-gradient(
    155deg,
    rgba(69, 26, 3, 0.72) 0%,
    rgba(120, 53, 15, 0.48) 50%,
    rgba(146, 64, 14, 0.28) 100%
  ) !important;
}

html.dark .env-tag.env-tag-badge.el-tag--info {
  color: #93c5fd !important;
  border-color: rgba(96, 165, 250, 0.35) !important;
  background: linear-gradient(
    155deg,
    rgba(23, 37, 84, 0.72) 0%,
    rgba(30, 58, 138, 0.48) 50%,
    rgba(37, 99, 235, 0.22) 100%
  ) !important;
}

@media (prefers-reduced-motion: reduce) {
  .env-tag.env-tag-badge {
    transition: none;
  }
}
</style>
