export const LLM_PROVIDER_ICON_BASE = '/assets/llm-icons'

/** 本地镜像（后端 /providers 为权威来源，此处仅作离线兜底） */
export const LLM_PROVIDER_FALLBACK = [
  { code: 'deepseek', label: 'DeepSeek', domestic: true },
  { code: 'kimi', label: 'Kimi', domestic: true },
  { code: 'qwen', label: '通义千问', domestic: true },
  { code: 'bailian', label: '百炼', domestic: true },
  { code: 'doubao', label: '豆包', domestic: true },
  { code: 'openai', label: 'OpenAI', domestic: false },
  { code: 'claude', label: 'Claude', domestic: false }
]

export function providerIconUrl(code) {
  if (!code) return ''
  return `${LLM_PROVIDER_ICON_BASE}/${code}.png`
}

export function providerLabel(providers, code) {
  const item = providers.find(p => p.code === code)
  return item ? item.label : code
}
