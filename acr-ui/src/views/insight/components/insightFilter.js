const STORAGE_KEY = 'acr.insight.filters'

export function loadInsightFilters(namespace) {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const all = JSON.parse(raw)
    return all?.[namespace] || null
  } catch (e) {
    return null
  }
}

export function saveInsightFilters(namespace, filters) {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    const all = raw ? JSON.parse(raw) : {}
    all[namespace] = filters
    localStorage.setItem(STORAGE_KEY, JSON.stringify(all))
  } catch (e) {
    /* ignore quota */
  }
}

export function formatRatio(value) {
  if (value == null || Number.isNaN(value)) return '--'
  return (Number(value) * 100).toFixed(1) + '%'
}

export function formatChange(ratio) {
  if (ratio == null || Number.isNaN(ratio)) return '较上期 —'
  const pct = (Math.abs(ratio) * 100).toFixed(1)
  if (ratio > 0) return `较上期 ↑ ${pct}%`
  if (ratio < 0) return `较上期 ↓ ${pct}%`
  return '较上期 持平'
}

export function formatDuration(ms) {
  if (ms == null || Number.isNaN(ms)) return '--'
  if (ms < 1000) return Math.round(ms) + ' ms'
  return (ms / 1000).toFixed(1) + ' s'
}

export function formatKpiValue(card) {
  if (!card) return '--'
  if (card.unit === 'ratio') return formatRatio(card.value)
  if (card.unit === 'ms') return formatDuration(card.value)
  return card.value == null ? '--' : String(Math.round(card.value))
}
