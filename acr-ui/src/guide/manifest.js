export const DEFAULT_DOC_ID = 'quick-start-first-repo'

export const GUIDE_GROUPS = [
  { key: 'quick-start', title: '快速上手', docIds: ['quick-start-first-repo'] },
  { key: 'platforms', title: '平台接入指引', docIds: ['platform-github', 'platform-gitlab', 'platform-gitee', 'platform-gitea'] },
  { key: 'model-engine', title: '模型与引擎', docIds: ['model-config', 'engine-config', 'model-check-failed'] },
  { key: 'commit-message', title: '提交注释规范', docIds: ['commit-convention', 'commit-examples'] },
  { key: 'delivery', title: '投递与通知', docIds: ['delivery-channel', 'delivery-failed'] },
  { key: 'faq', title: '常见问题', docIds: ['faq-webhook-not-triggered', 'faq-review-not-run'] }
]

export const GUIDE_DOCS = [
  { id: 'quick-start-first-repo', title: '接入第一个仓库', group: 'quick-start', keywords: ['开始', '上手', '流程', '第一个', '接入'], file: 'quick-start/first-repo.md' },
  { id: 'platform-github', title: 'GitHub 接入', group: 'platforms', keywords: ['github', 'token', 'webhook', '凭据', 'secret'], file: 'platforms/github.md' },
  { id: 'platform-gitlab', title: 'GitLab 接入', group: 'platforms', keywords: ['gitlab', 'token', 'webhook', '凭据', 'merge request'], file: 'platforms/gitlab.md' },
  { id: 'platform-gitee', title: 'Gitee 接入', group: 'platforms', keywords: ['gitee', '码云', 'token', 'webhook', '凭据'], file: 'platforms/gitee.md' },
  { id: 'platform-gitea', title: 'Gitea 接入', group: 'platforms', keywords: ['gitea', 'token', 'webhook', '凭据', '自建'], file: 'platforms/gitea.md' },
  { id: 'model-config', title: '模型服务配置', group: 'model-engine', keywords: ['模型', 'apikey', 'provider', '厂商', '默认模型', '检测'], file: 'model-engine/model-config.md' },
  { id: 'engine-config', title: '审查引擎配置', group: 'model-engine', keywords: ['引擎', 'engine', 'ocr', '超时', '并发'], file: 'model-engine/engine-config.md' },
  { id: 'model-check-failed', title: '模型检测失败排查', group: 'model-engine', keywords: ['检测失败', '排查', '密钥', '额度', '网络'], file: 'model-engine/check-failed.md' },
  { id: 'commit-convention', title: '提交注释规范说明', group: 'commit-message', keywords: ['提交', '注释', 'commit', '规范', '首行'], file: 'commit-message/convention.md' },
  { id: 'commit-examples', title: '注释正误实例对照', group: 'commit-message', keywords: ['实例', '示例', '模板', '对照', 'feat', 'fix'], file: 'commit-message/examples.md' },
  { id: 'delivery-channel', title: '通知渠道配置', group: 'delivery', keywords: ['钉钉', '企微', '飞书', '机器人', '通知', '渠道'], file: 'delivery/channel-config.md' },
  { id: 'delivery-failed', title: '投递失败排查', group: 'delivery', keywords: ['投递失败', '加签', '关键词', '重试', '补发'], file: 'delivery/delivery-failed.md' },
  { id: 'faq-webhook-not-triggered', title: 'Webhook 没触发怎么办', group: 'faq', keywords: ['webhook', '没触发', '事件', '签名', '分支'], file: 'faq/webhook-not-triggered.md' },
  { id: 'faq-review-not-run', title: '审查没执行或结果不符预期', group: 'faq', keywords: ['没执行', '失败', '结果', '预期', '模板'], file: 'faq/review-not-run.md' }
]

export function findDoc(id) {
  return GUIDE_DOCS.find(d => d.id === id) || null
}
