/** 本地镜像（后端字典 review_git_provider 为权威来源，此处仅作离线兜底） */
export const GIT_PROVIDER_FALLBACK = [
  { label: 'GitHub', value: 'GITHUB' },
  { label: 'GitLab', value: 'GITLAB' },
  { label: 'Gitee（码云）', value: 'GITEE' },
  { label: 'Gitea', value: 'GITEA' }
]

/** GitLab/Gitea 依赖自建实例，必须配置服务地址；GitHub/Gitee 为固定公网服务，服务地址必须留空。 */
export function requiresServerUrl(provider) {
  const code = (provider || '').toUpperCase()
  return code === 'GITLAB' || code === 'GITEA'
}
