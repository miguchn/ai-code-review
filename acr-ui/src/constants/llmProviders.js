export const LLM_PROVIDER_ICON_BASE = '/assets/llm-icons'

/** 本地镜像（后端 /providers 为权威来源，此处仅作离线兜底） */
export const LLM_PROVIDER_FALLBACK = [
  { code: 'deepseek', label: 'DeepSeek', domestic: true },
  { code: 'kimi', label: 'Kimi', domestic: true },
  { code: 'qwen', label: '通义千问', domestic: true },
  { code: 'bailian', label: '百炼', domestic: true },
  { code: 'doubao', label: '豆包', domestic: true },
  { code: 'openai', label: 'OpenAI', domestic: false },
  { code: 'claude', label: 'Claude', domestic: false },
  { code: 'custom', label: '其他/自定义', domestic: false }
]

/**
 * 厂商推荐默认值。
 * 切换服务厂商时整体覆盖 API 地址与模型标识，避免残留上一厂商数据。
 * Claude 使用 Anthropic 原生基址，审查引擎会映射为 /v1/messages。
 */
export const LLM_PROVIDER_DEFAULTS = {
  deepseek: {
    apiUrl: 'https://api.deepseek.com/v1/chat/completions',
    model: 'deepseek-chat'
  },
  kimi: {
    apiUrl: 'https://api.moonshot.cn/v1/chat/completions',
    model: 'moonshot-v1-8k'
  },
  qwen: {
    apiUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions',
    model: 'qwen-plus'
  },
  bailian: {
    apiUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions',
    model: 'qwen-plus'
  },
  doubao: {
    apiUrl: 'https://ark.cn-beijing.volces.com/api/v3/chat/completions',
    model: ''
  },
  openai: {
    apiUrl: 'https://api.openai.com/v1/chat/completions',
    model: 'gpt-4o-mini'
  },
  claude: {
    apiUrl: 'https://api.anthropic.com',
    model: 'claude-3-5-sonnet-latest'
  },
  custom: {
    apiUrl: '',
    model: ''
  }
}

export const LLM_ADVANCED_DEFAULTS = {
  timeout: 60000,
  maxTokens: 8000,
  temperature: 0.7,
  contextLength: 128000,
  sortOrder: 0,
  enabled: '1',
  isDefault: '0'
}

export function providerIconUrl(code) {
  if (!code) return ''
  return `${LLM_PROVIDER_ICON_BASE}/${code}.png`
}

export function providerLabel(providers, code, customProviderName) {
  if (code === 'custom' && customProviderName) {
    return customProviderName
  }
  const item = providers.find(p => p.code === code)
  return item ? item.label : code
}

/**
 * 按所选厂商同步 API 地址与模型标识（始终覆盖）。
 * 选择「其他/自定义」时清空为可手填状态，不保留上一厂商残留。
 */
export function syncProviderFields(form, providerCode) {
  if (!form) {
    return
  }
  const defaults = LLM_PROVIDER_DEFAULTS[providerCode] || { apiUrl: '', model: '' }
  form.apiUrl = defaults.apiUrl ? defaults.apiUrl : undefined
  form.model = defaults.model ? defaults.model : undefined
  // 切换厂商后自定义名称一律清空，避免展示/保存与当前厂商不一致
  form.customProviderName = undefined
}
