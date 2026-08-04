/**
 * 工作台共享常量：审查结论元数据与趋势图色板。
 * 色板为浅/暗双套，已经可访问性对比验证（CVD ΔE 与对比度），是全页唯一允许写死的颜色。
 */
export const CONCLUSION_META = {
  PASS: { label: '通过', colorVar: '--status-success-icon' },
  WARN: { label: '建议修改', colorVar: '--status-warning-icon' },
  BLOCK: { label: '高风险', colorVar: '--status-danger-text' },
  FAILED: { label: '执行失败', colorVar: '--text-placeholder' }
}

export const CONCLUSION_ORDER = ['PASS', 'WARN', 'BLOCK']

export const CHART_PALETTE = {
  light: { PASS: '#3D8B61', WARN: '#CA8A04', BLOCK: '#B91C1C', surface: '#FCFDFC' },
  dark: { PASS: '#2F8F5F', WARN: '#B98A1F', BLOCK: '#C73E33', surface: '#1B1F26' }
}
