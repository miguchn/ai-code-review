# 多平台 Git Provider 接入设计（GitLab / Gitee / Gitea）

> **状态（2026-08-03）：设计待审。**  
> 范围：在现有 GitHub MVP 闭环上扩展 GitLab、Gitee、Gitea 真实接入；不重构审查引擎、任务状态、评分协议、通知、投递记录骨架与问题台账。  
> 前置：GitHub M1–M4 链路已落地；契约见 `acr-review/.../git/*` 与 `git/github/*`。  
> 分支：`feature/multi-git-provider-access`。  
> 决策锁定：方案 1（契约注册表 + 平台适配包）；`GitAccessContext`；按平台拆投递渠道；全平台 `repository_full_path`；凭据仅存 `server_url`。

## 1. 目标与成功标准

### 1.1 目标

按「统一契约 → GitLab → Gitee → Gitea」顺序，使每个平台独立跑通：

```text
项目与凭据配置
  → 仓库连接检测
  → 仓库及分支同步
  → Webhook 可信接收
  → 合并请求事件标准化
  → 审查任务创建
  → Diff / 元数据获取
  → 复用现有审查执行
  → 总结评论幂等回写
  → 投递状态及失败重试
```

### 1.2 成功标准

1. GitHub 现有自动测试与行为全部回归通过；存量投递幂等键与渠道编码不变。
2. 三平台均可通过 Provider / Webhook / 合并请求读取 / 评论创建与更新的契约测试。
3. 覆盖：错误签名、重复投递、非代码更新、目标分支不匹配、Token 失效、权限不足、限流、超时、评论回写失败。
4. 外部平台回写失败不改变审查任务状态与审查结论。
5. `mvn test` 与 `cd acr-ui && npm run build:prod` 通过。
6. 有真实环境时分平台做一次仓库闭环；无环境时不伪造验收，提供人工步骤与示例配置。
7. 同步更新产品路线图、架构文档、README、CHANGELOG、部署说明。

### 1.3 非范围

- OAuth / App 授权、自动创建 Webhook、组织仓库批量导入；
- 行级评论、Status Check / Checks、合并门禁；
- 新 Maven 模块、消息队列、通用“未来平台”空实现；
- 重构审查引擎、任务状态机、评分协议、IM 通知与问题台账主流程；
- GitHub Enterprise Server（本期 GitHub / Gitee 固定官方地址）。

## 2. 当前基线与缺口

### 2.1 已有可复用资产

| 资产 | 现状 |
|---|---|
| `GitProvider` | 仓库解析、凭据/仓库检测、读仓库信息；仅 `GitHubProvider` |
| `GitWebhookAdapter` | 验签 + 解析仓库坐标 + 标准化 `GitPullRequestEvent`；仅 GitHub |
| Diff / Metadata / FileContent / Workspace | 接口已按 `providerCode` 声明；仅 GitHub 实现 |
| `GitPullRequestCommentClient` | 按标记查找 / 创建 / 更新 Issue 级评论；仅 GitHub |
| 用例 | `ReviewWebhookServiceImpl.handleGitHubWebhook`、`ReviewProjectServiceImpl`、`GitCredentialServiceImpl`、`ReviewTaskExecutionServiceImpl`、`ReviewDeliveryServiceImpl` |
| 表 | `review_git_credential`、`review_project`、`review_webhook_event`、`review_delivery_record` 等 |
| 前端 | 项目管理、访问凭据页已有，平台选项硬编码 GitHub |
| 评论标记 | `<!-- acr-review-summary -->`（跨平台复用） |
| 幂等键 | `GITHUB:{projectId}:{prNumber}:SUMMARY_COMMENT` |

### 2.2 主要缺口

1. 业务服务与 Spring 注入硬编码单个 GitHub Bean（`GitProvider`、`GitWebhookAdapter`、`GitPullRequestCommentClient` 等）。
2. `GitProvider` 等方法只收 `token`，无法携带自建实例地址。
3. `review_git_credential` 无服务地址字段；`review_project` 以 `(provider, owner, name)` 为唯一身份，无法表达 GitLab 嵌套命名空间。
4. Webhook 入口与用例名为 GitHub 专属；缺少平台路由。
5. 投递渠道与 URL 拼装仅识别 GitHub。
6. 前端无平台动态表单（服务地址、Token 提示、Webhook 说明、PR/MR 标签）。

## 3. 产品决策（已锁定）

| # | 决策 | 选择 |
|---|---|---|
| 1 | 总体架构 | 契约注册表 + `git/{provider}` 适配包；不复制三套业务 Service |
| 2 | 访问上下文 | 引入 `GitAccessContext`；凭据存 Web 根 `server_url`，适配器推导 API 路径 |
| 3 | 仓库身份 | 全平台增加并唯一约束 `repository_full_path`；owner/name 保留为辅助字段 |
| 4 | 投递渠道 | 按平台拆分；保留 `GITHUB_PR_SUMMARY_COMMENT`；新增三平台渠道 |
| 5 | 幂等键 | `{provider}:{projectId}:{prNumber}:SUMMARY_COMMENT`；GitHub 旧键不变 |
| 6 | 合并请求编号 | GitLab MR IID、Gitee/Gitea PR number 统一映射现有 `pr_number` 字段，不重命名表列 |
| 7 | UI 文案 | 公共页面统一「合并请求」；具体记录可按平台展示 PR / MR 标签 |
| 8 | 实施顺序 | 统一契约与数据迁移 → GitLab → Gitee → Gitea → 文档与验收 |

## 4. 架构设计

### 4.1 模块归属

不变：

- `acr-admin`：Webhook / REST 协议接入；
- `acr-review`：用例、Mapper、Git 适配；
- 不新增 Maven 模块。

包结构（增量）：

```text
acr-review/.../git/
  GitAccessContext.java
  GitProviderRegistry.java          # 及同类小型注册表或统一 GitAdapterRegistry
  github/                           # 现有，改为消费 GitAccessContext
  gitlab/
  gitee/
  gitea/
```

### 4.2 注册与路由

```text
Controller(provider path)
  → IReviewWebhookService.handleWebhook(providerCode, headers, payload)
  → GitWebhookAdapter(registry)
  → 项目匹配(repository_full_path) / 验签 / 动作白名单 / 建单

Project / Credential / Execution / Delivery
  → Registry.resolve*(providerCode)
  → 适配器(GitAccessContext)
```

规则：

- 业务用例禁止 `switch(provider)` 解释平台 HTTP 细节；
- 允许用例按 `providerCode` 选择适配器、拼展示标签、选择投递渠道常量；
- 平台 API 地址、认证头、Webhook 头、签名、载荷、分页、状态码解释只存在于各自适配目录。

### 4.3 `GitAccessContext`

```text
GitAccessContext
  token: String          # 明文，仅调用链路短暂持有
  serverUrl: String      # Web 根；GitHub/Gitee 为官方默认；GitLab/Gitea 来自凭据
```

约定：

- GitHub 默认 `https://github.com`，API 由适配器映射为 `https://api.github.com`（保持现有可配置覆盖能力，如 `review.github.api-url`，但不进凭据表）。
- Gitee 默认 `https://gitee.com`，API 映射官方地址。
- GitLab：`serverUrl` + `/api/v4`。
- Gitea：`serverUrl` + `/api/v1`。
- `serverUrl` 规范化：去尾斜杠、强制 https（本地测试可另议）、拒绝含 userinfo/query/fragment。
- Diff / Metadata / Comment / Workspace / FileContent 方法签名由 `String token` 改为 `GitAccessContext access`（或等价重载后删除旧签名，避免双轨）。

### 4.4 契约演进（最小必要）

现有接口保留 `providerCode()`，方法入参统一带上 `GitAccessContext`：

| 契约 | 变更要点 |
|---|---|
| `GitProvider` | `testCredential/testRepository/readRepository/parseRepository` 使用 context 或 serverUrl 感知解析 |
| `GitWebhookAdapter` | `verifySignature` 改为接收平台相关头集合（见 §6）；解析产出含 `repositoryFullPath` |
| `GitPullRequestDiffFetcher` 等 | `token` → `GitAccessContext` |
| `GitPullRequestCommentClient` | 同上；GitLab 使用 MR Notes API，对业务仍表现为“总结评论” |

`GitRepositoryCoordinates` 扩展为稳定四元组语义：

```text
owner                # 命名空间前缀：GitHub/Gitee/Gitea = owner；GitLab = path_with_namespace 去掉末段后的前缀（可含 /）
repository           # 末段项目名
fullPath             # 唯一身份：GitHub/Gitee/Gitea = owner/repo；GitLab = path_with_namespace
canonicalUrl         # 规范化 Web 仓库地址（基于 serverUrl）
```

匹配与唯一约束**只使用** `fullPath`；`owner`/`repository` 仅供展示、兼容旧查询与拼装辅助信息。

为减少大面积破坏，可采用：

- 新增 `fullPath` 字段（record 组件）；
- 全仓库编译修复构造点；
- Webhook / 项目匹配只信任 `fullPath`。

### 4.5 Provider 编码

| 编码 | 说明 |
|---|---|
| `GITHUB` | 官方 github.com |
| `GITLAB` | gitlab.com 或自建 |
| `GITEE` | 官方 gitee.com |
| `GITEA` | 自建或官方兼容实例 |

字典：扩展现有/新增 `review_git_provider`（若尚无）供前端下拉；渠道字典扩展见 §7。

## 5. 数据模型

### 5.1 `review_git_credential` 增量

| 列 | 类型 | 说明 |
|---|---|---|
| `server_url` | `varchar(500) NULL` | Web 根。GitLab/Gitea **必填**；GitHub/Gitee **必须为空**（使用官方默认） |

约束与行为：

- 唯一键仍为 `(provider, credential_name)`；
- 列表/详情可回显 `server_url`（非密钥）；Token 继续加密、不回显；
- 连接测试使用 `GitAccessContext(token, resolvedServerUrl)`；
- 项目绑定凭据时校验 `project.provider == credential.provider`；GitLab/Gitea 项目的 API 调用使用凭据 `server_url`。

### 5.2 `review_project` 增量

| 列 | 类型 | 说明 |
|---|---|---|
| `repository_full_path` | `varchar(255) NOT NULL`（迁移后） | 仓库全路径身份 |

迁移策略（增量脚本，建议序号 `29_*`，不改历史脚本）：

1. 增加可空列 `repository_full_path`；
2. 回填：`CONCAT(repository_owner, '/', repository_name)`（当前仅 GitHub 数据）；
3. 确认无空值后改为 `NOT NULL`；
4. 删除旧唯一键 `uk_review_project_repository (provider, repository_owner, repository_name)`；
5. 新建唯一键 `uk_review_project_full_path (provider, repository_full_path)`；
6. 保留 `repository_owner` / `repository_name` 供展示与兼容查询，由 Provider 解析写入，不信任前端。

Webhook 事件表：

- 增加可空 `repository_full_path`（便于审计与排障）；
- 匹配项目改为 `selectByFullPath(provider, fullPath)`；
- `repository_owner` / `repository_name` 继续填充辅助信息。

### 5.3 字段语义：合并请求编号

| 平台 | 外部编号 | 落库字段 |
|---|---|---|
| GitHub | PR number | `pr_number` |
| GitLab | MR IID（项目内） | `pr_number` |
| Gitee | PR number | `pr_number` |
| Gitea | PR number | `pr_number` |

禁止把 GitLab 全局 MR id 写入 `pr_number`。评论 API 必须使用与 Web UI 一致的 IID/number。

### 5.4 安全

- Token、Webhook Secret 继续 AES-GCM；响应/日志/异常脱敏；
- Webhook 必须基于**原始请求体字节**验签后再 JSON 解析；
- 不在日志打印签名头完整值、Token、Secret；
- 载荷大小限制沿用 `review.webhook.max-payload-bytes`。

## 6. Webhook 设计

### 6.1 HTTP 入口

`acr-admin` 提供平台路径（可同 Controller 多映射）：

| 路径 | providerCode |
|---|---|
| `POST /webhook/github` | `GITHUB`（保留，行为兼容） |
| `POST /webhook/gitlab` | `GITLAB` |
| `POST /webhook/gitee` | `GITEE` |
| `POST /webhook/gitea` | `GITEA` |

Controller 只做：读取原始 body、抽取平台相关头、调用

```text
handleWebhook(providerCode, WebhookHeaders, byte[] payload)
```

废除业务层对 `handleGitHubWebhook` 的依赖（可短期委托到新方法，避免双逻辑）。

### 6.2 头与投递 ID（适配器负责解释）

| 平台 | 事件类型头 | 投递 ID | 签名/密钥头 |
|---|---|---|---|
| GitHub | `X-GitHub-Event` | `X-GitHub-Delivery` | `X-Hub-Signature-256` |
| GitLab | `X-Gitlab-Event` | 优先 `X-Gitlab-Event-UUID`；若缺失则用稳定合成策略（见下） | `X-Gitlab-Token`（Secret Token）及/或官方签名头（适配器兼容） |
| Gitee | `X-Gitee-Event` / 文档等价头 | 平台投递头；若仅有时间戳+签名则按官方去重键合成并文档化 | Token / 签名按 Gitee 官方协议 |
| Gitea | `X-Gitea-Event` | `X-Gitea-Delivery` | `X-Gitea-Signature`（HMAC-SHA256） |

GitLab 投递 ID 合成（仅当 UUID 头不存在时）：

```text
sha256(eventType + "|" + project_path + "|" + mr_iid + "|" + action + "|" + head_sha)
```

截断到 `delivery_id` 列长；同一代码推送应稳定；不同 head 不碰撞。合成逻辑只在 `gitlab` 包。

### 6.3 验签契约

将 `GitWebhookAdapter.verifySignature` 演进为：

```text
boolean verify(String secret, byte[] payload, WebhookRequestHeaders headers)
```

`WebhookRequestHeaders` 为平台无关的只读头视图（大小写不敏感取值）。各适配器自行读取所需头。

校验顺序（用例层）：

1. 载荷大小；
2. provider + deliveryId 去重插入；
3. 解析 `fullPath` 并匹配项目；
4. 项目启用与 Secret 存在性；
5. **原始字节验签**；
6. 事件类型是否为合并请求类；
7. 解析标准化事件；
8. 动作白名单与目标分支；
9. 建单。

失败分类保持现有：`unauthorized` / `badRequest` / `payloadTooLarge` / 业务忽略仍 200 快速响应。

### 6.4 动作白名单（统一审查事件）

用例层使用**统一动作**集合（配置键可按平台拆分或共用）：

| 统一动作 | 含义 |
|---|---|
| `opened` | 新建合并请求 |
| `reopened` | 重新打开 |
| `synchronize` | 源分支代码更新 |

适配器负责把平台原始 action 映射到上述三值；无法映射则视为忽略。

平台映射（实现时以官方文档为准，单测钉死）：

| 平台 | 原始事件 | 映射 |
|---|---|---|
| GitHub | `pull_request` + opened/reopened/synchronize | 同名 |
| GitLab | `Merge Request Hook` + open/reopen/update | opened/reopened/synchronize；**仅**当 update 表示代码变更（新 head SHA / oldrev≠newrev）时映射 synchronize；标题/标签/审批等字段变更不建单 |
| Gitee | PR Hook 对应 open/reopen/push_update（或文档等价） | opened/reopened/synchronize |
| Gitea | `pull_request` + opened/reopened/synchronized | opened/reopened/synchronize |

明确不建单：close、merge、approve、unapprove、edit（无代码变更）、assignment、label-only 等。

配置项：

- 保留 `review.github.prEvents`；
- 新增 `review.gitlab.mrEvents`、`review.gitee.prEvents`、`review.gitea.prEvents`，默认均为 `opened,reopened,synchronize`。

### 6.5 标准化事件

继续使用 `GitPullRequestEvent`；补充/确保可承载：

- `repositoryFullPath`（或与 coordinates 一并返回）；
- `prNumber` = MR IID / PR number；
- `action` = 统一动作；
- base/head SHA、源/目标分支、标题、作者、变更规模字段（平台有则填，无则空，执行阶段再补）。

## 7. 评论回写与投递

### 7.1 渠道常量

| provider | channel |
|---|---|
| GITHUB | `GITHUB_PR_SUMMARY_COMMENT`（不变） |
| GITLAB | `GITLAB_MR_SUMMARY_COMMENT` |
| GITEE | `GITEE_PR_SUMMARY_COMMENT` |
| GITEA | `GITEA_PR_SUMMARY_COMMENT` |

字典 `review_delivery_channel` 增量插入上述新值。

### 7.2 幂等键

```text
{provider}:{projectId}:{prNumber}:SUMMARY_COMMENT
```

- GitHub 继续生成 `GITHUB:...`，与历史行兼容；
- `ReviewDeliveryConstants.idempotencyKey` 改为接收 `provider`；
- 固定评论标记仍为 `<!-- acr-review-summary -->`；同一项目同一合并请求仅一条总结评论并更新。

### 7.3 回写行为

- 仅 `task_status=SUCCESS` 发总结评论；FAILED 不发（与 M4 一致）；
- 查找标记 → 更新，否则创建；
- 失败写入 `review_delivery_record`，不修改任务/结论；
- 重试语义保持：按该合并请求最近 SUCCESS 任务渲染；
- PR/MR Web URL 由 `serverUrl + fullPath + 平台路径规则` 生成（GitHub `/pull/{n}`，GitLab `/-/merge_requests/{iid}`，Gitee/Gitea `/pulls/{n}`），禁止写死 github.com 于多平台路径。

### 7.4 GitLab 评论 API 注意

GitLab 使用 Notes（MR 讨论）实现“总结评论”语义：list/create/update note，按正文标记匹配。业务层仍只依赖 `GitPullRequestCommentClient`。

## 8. 前端设计

复用「项目管理」「访问凭据」，不新增平台菜单。

### 8.1 访问凭据

- 平台选择：GitHub / GitLab / Gitee / Gitea；
- GitLab、Gitea 显示「服务地址」必填（placeholder 示例 `https://gitlab.example.com`）；
- GitHub、Gitee 隐藏服务地址；
- Token 提示按平台变化（PAT / Personal Access Token / Private Token 等文案）；
- 编辑时 Token 仍为空不回显。

### 8.2 项目管理

- 平台选择后：过滤同平台凭据；仓库地址校验与解析跟随 Provider；
- 展示 Webhook URL：`{apiBase}/webhook/{provider}` 与 Secret 配置说明（按平台）；
- 分支同步、连接测试走对应 Provider；
- 表单字段「是否启用合并请求审查」「目标分支」等公共文案统一用「合并请求」。

### 8.3 列表与详情标签

- 公共文案：合并请求；
- 行内/详情可根据 `provider` 显示 `PR`（GitHub/Gitee/Gitea）或 `MR`（GitLab）微标签，不改后端字段名。

## 9. 实现切片与验证

| 步骤 | 内容 | 验证 |
|---|---|---|
| S0 | 契约：`GitAccessContext`、Registry、coordinates.fullPath、Webhook 头视图；GitHub 全量迁移编译与测试绿 | `mvn test` 中 GitHub 相关全绿 |
| S1 | SQL `29_*`：`server_url`、`repository_full_path` 回填与唯一键、字典渠道 | 脚本可重复执行；存量项目可查 |
| S2 | 用例去 GitHub 硬编码：凭据/项目/Webhook/执行/投递按 registry | 现有 Webhook/Delivery 单测改造后绿 |
| S3 | GitLab 适配包 + 契约测试 + 前端平台项 | 单测覆盖验签/映射/评论；页面可选 GitLab |
| S4 | Gitee 适配包 + 契约测试 | 同上 |
| S5 | Gitea 适配包 + 契约测试 | 同上 |
| S6 | 文档：路线图、架构、README、CHANGELOG、deployment | 与实现一致 |
| S7 | 前端 `build:prod` + 人工/真实环境清单 | 构建通过；验收清单可执行 |

## 10. 测试矩阵（最低要求）

每平台适配器至少覆盖：

1. 合法签名 / Secret Token 通过；错误签名拒绝；
2. 重复 delivery 去重；
3. 非合并请求事件忽略；
4. 非白名单动作忽略（含 GitLab 无代码变更的 update）；
5. 目标分支不匹配忽略；
6. Token 失效 / 401、权限不足 / 403、404、限流、超时的连接与拉取失败分类；
7. 评论：无标记则创建，有标记则更新；回写失败不改 task；
8. GitHub 回归：旧幂等键、旧渠道、旧 Webhook 路径。

## 11. 人工验收清单（无真实环境时使用）

### 11.1 通用准备

1. 执行至最新 SQL（含 `29_*`）。
2. 配置 `ACR_CREDENTIAL_MASTER_KEY`。
3. 在「访问凭据」创建对应平台凭据；GitLab/Gitea 填写可达的 `server_url`。
4. 在「项目管理」创建项目，连接测试成功，同步分支，配置目标分支与 Webhook Secret，启用合并请求审查。

### 11.2 Webhook 配置示例

| 平台 | URL | Secret | 事件 |
|---|---|---|---|
| GitHub | `https://<host>/webhook/github` | 与项目 Secret 一致 | Pull requests |
| GitLab | `https://<host>/webhook/gitlab` | Secret Token = 项目 Secret | Merge request events |
| Gitee | `https://<host>/webhook/gitee` | 按官方密码/签名模式与项目 Secret 对齐 | PR 相关 |
| Gitea | `https://<host>/webhook/gitea` | 与项目 Secret 一致 | Pull Request |

### 11.3 闭环步骤

1. 新建合并请求 → 产生任务 → 审查完成 → 平台出现总结评论。
2. 推送新 commit → 新任务 → 同一评论被更新（非新增一条）。
3. 关闭/合并/仅改标题 → 不建新任务。
4. 错误 Secret → 401/未授权，事件 FAILED。
5. 投递失败后在投递记录重试，任务结论不变。

无真实 Token/实例时：标注「未执行真实闭环」，不得写已验收。

## 12. 文档同步范围

| 文档 | 变更要点 |
|---|---|
| `docs/planning/product-roadmap.md` | 多平台从“规模化候选”调整为当前扩展切片；写明四平台 MVP 闭环与非范围 |
| `docs/planning/architecture-scaffold.md` | Provider 注册表、多适配包、full_path / server_url |
| `README.md` | 支持平台列表与配置入口 |
| `CHANGELOG.md` | 记录多平台接入 |
| `docs/deployment.md` | Webhook URL、自建实例 `server_url`、Secret 配置 |
| `sql/README.md` | 登记 `29_*` |

本设计审定后再写 `docs/superpowers/plans/2026-08-03-multi-git-provider-access.md` 实施计划。

## 13. 风险与缓解

| 风险 | 缓解 |
|---|---|
| GitLab update 误触发 | 适配器比较 head SHA / 代码更新字段；单测钉死 |
| 嵌套命名空间匹配失败 | 唯一键与匹配只认 `full_path`；解析以 API/`path_with_namespace` 为准 |
| 自建实例反代路径特殊 | 本期只支持 Web 根 + 标准 API 后缀；文档声明不支持非常规 API 前缀 |
| Spring 多实现注入冲突 | Registry 收集 `List<接口>`，禁止业务直接注入单实现（测试可用 `@Primary` mock registry） |
| 存量唯一键迁移 | 脚本分步回填、校验、再切唯一键；可重复执行 |
| 平台 API 差异导致评论权限不足 | 失败分类 + 投递记录；文档写明 Token 所需最小 scope |

## 14. 待实施时核对的平台细节（非开放问题）

以下不阻塞本设计审定，实施对应适配包时对照官方文档钉死到测试：

1. Gitee 签名模式（密码 Token vs 签名）与去重头最终字段名；
2. GitLab 旧版仅 Secret Token、新版签名头并存时的兼容顺序；
3. 各平台分页头/链路与评论列表上限（对齐现有 `COMMENT_PAGE_SIZE` / `COMMENT_MAX_PAGES`）；
4. GitLab 仓库 URL 多种 host（含端口）的 `parseRepository` 规则与 `server_url` 一致性校验。
