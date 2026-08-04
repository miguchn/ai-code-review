<template>
  <section class="wb-panel">
    <header class="wb-panel-head">
      <h3 class="wb-panel-title">审查结果概览</h3>
      <span class="wb-panel-extra">近 7 天</span>
    </header>
    <template v-if="hasData">
      <div class="wb-concl-bar" role="img" :aria-label="barAriaLabel">
        <div
          v-for="seg in segments"
          :key="seg.key"
          class="wb-concl-seg"
          :style="{ width: seg.pct + '%', background: seg.color }"
        />
      </div>
      <ul class="wb-concl-list">
        <li v-for="seg in segments" :key="seg.key" class="wb-concl-item">
          <span class="wb-concl-dot" :style="{ background: seg.color }" />
          <span class="wb-concl-label">{{ seg.label }}</span>
          <span class="wb-concl-count">{{ seg.count }}</span>
          <span class="wb-concl-pct">{{ seg.pctText }}</span>
        </li>
      </ul>
      <div v-if="failedCount > 0" class="wb-concl-failed">另有 {{ failedCount }} 个任务执行失败</div>
    </template>
    <div v-else class="wb-block-state">近 7 天暂无审查结论</div>
  </section>
</template>

<script setup>
import useSettingsStore from '@/store/modules/settings'
import { CHART_PALETTE, CONCLUSION_META, CONCLUSION_ORDER } from '../constants'

const props = defineProps({
  trend: { type: Object, default: null }
})

const settingsStore = useSettingsStore()

const totals = computed(() => {
  const pts = (props.trend?.points || []).slice(-7)
  return pts.reduce(
    (acc, p) => ({
      pass: acc.pass + (p.pass || 0),
      warn: acc.warn + (p.warn || 0),
      block: acc.block + (p.block || 0),
      failed: acc.failed + (p.failed || 0)
    }),
    { pass: 0, warn: 0, block: 0, failed: 0 }
  )
})

const totalConclusions = computed(() => totals.value.pass + totals.value.warn + totals.value.block)
const hasData = computed(() => totalConclusions.value > 0)
const failedCount = computed(() => totals.value.failed)

const segments = computed(() => {
  if (!hasData.value) return []
  const palette = settingsStore.isDark ? CHART_PALETTE.dark : CHART_PALETTE.light
  const total = totalConclusions.value
  return CONCLUSION_ORDER.map(key => {
    const count = totals.value[key.toLowerCase()]
    const pct = (count / total) * 100
    return {
      key,
      label: CONCLUSION_META[key].label,
      count,
      pct,
      pctText: `${Math.round(pct)}%`,
      color: palette[key]
    }
  }).filter(seg => seg.count > 0)
})

const barAriaLabel = computed(() =>
  segments.value.map(s => `${s.label} ${s.count} 条，占 ${s.pctText}`).join('；')
)
</script>

<style scoped lang="scss">
.wb-concl-bar {
  display: flex;
  gap: 2px;
  height: 8px;
  border-radius: 4px;
  overflow: hidden;
}

.wb-concl-seg {
  min-width: 4px;
  border-radius: 2px;
}

.wb-concl-list {
  margin: 14px 0 0;
  padding: 0;
  list-style: none;
}

.wb-concl-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  line-height: 20px;

  & + & {
    margin-top: 8px;
  }
}

.wb-concl-dot {
  flex-shrink: 0;
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.wb-concl-label {
  flex: 1;
  color: var(--text-regular);
}

.wb-concl-count {
  color: var(--text-primary);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.wb-concl-pct {
  width: 44px;
  text-align: right;
  color: var(--text-assist);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.wb-concl-failed {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid var(--divider);
  color: var(--text-assist);
  font-size: 12px;
  line-height: 18px;
}
</style>
