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
/* 顶栏环境胶囊：毛玻璃质感 + 环境色编码（逻辑仍由 tagType / label 驱动） */
.env-tag.env-tag-badge {
  cursor: default !important;
  display: inline-flex !important;
  align-items: center;
  justify-content: center;
  height: 26px !important;
  padding: 0 12px 0 10px !important;
  margin: 0 !important;
  border-radius: 999px !important;
  font-size: 11px !important;
  font-weight: 700 !important;
  letter-spacing: 0.1em !important;
  line-height: 1 !important;
  border-width: 1px !important;
  border-style: solid !important;
  vertical-align: middle;
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.55) inset,
    0 4px 14px rgba(15, 23, 42, 0.07);
  transition:
    box-shadow 0.22s ease,
    border-color 0.22s ease,
    background 0.22s ease;

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
    box-shadow: 0 0 0 3px color-mix(in srgb, currentColor 22%, transparent);
  }
}

@supports ((backdrop-filter: blur(8px)) or (-webkit-backdrop-filter: blur(8px))) {
  .env-tag.env-tag-badge {
    -webkit-backdrop-filter: blur(10px) saturate(150%);
    backdrop-filter: blur(10px) saturate(150%);
  }
}

/* DEV — 琥珀 */
.env-tag.env-tag-badge.el-tag--warning {
  color: #b45309 !important;
  border-color: rgba(251, 191, 36, 0.42) !important;
  background: linear-gradient(
    155deg,
    rgba(255, 255, 255, 0.82) 0%,
    rgba(254, 243, 199, 0.65) 55%,
    rgba(253, 230, 138, 0.38) 100%
  ) !important;
}

/* UAT / 默认 — 靛蓝 */
.env-tag.env-tag-badge.el-tag--info {
  color: #1d4ed8 !important;
  border-color: rgba(147, 197, 253, 0.55) !important;
  background: linear-gradient(
    155deg,
    rgba(255, 255, 255, 0.85) 0%,
    rgba(219, 234, 254, 0.62) 55%,
    rgba(191, 219, 254, 0.38) 100%
  ) !important;
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
