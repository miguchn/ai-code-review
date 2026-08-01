# P0/M2 GitHub PR Webhook 事件接入设计

> **实施状态（2026-08-01）：已实现并通过真实仓库验收。** 本文保留为链路公共语义的事实源；Diff 获取、审查执行、回写、通知等后续切片在此基础上扩展。验收记录：本地模拟七类场景（受理/判重/分支不匹配/白名单外/签名错误/超限/缺头）全部符合预期；真实仓库 miguchn/webhook-test 的 ping/closed/reopened/synchronize 投递 GitHub 侧全部 200，reopened 与 synchronize 分别生成 PENDING 任务。

## 1. 目标与范围

本切片交付可独立验收的 GitHub PR 事件可信接入：GitHub 仓库配置 Webhook 后，PR 的 `opened`、`reopened`、`synchronize` 事件经签名校验、项目匹配、目标分支判断和事件幂等后生成待审查任务，后台可查询任务列表，项目详情可查看 Webhook 配置状态与最近接收结果。

闭环：`GitHub PR 事件 → Webhook 验签 → 项目匹配 → 目标分支判断 → 事件去重 → 生成待审查任务 → 后台可查询`。

本次不实现：PR Diff 获取、OCR 或大模型调用、审查结果生成、PR 评论回写、通知推送、Push 事件、复杂重试和数据看板。

## 2. 当前基线与处理结论

- M1 已具备 `review_project`（含 `pr_review_enabled`、`pr_target_branches`、仓库 owner/name 唯一键、启停状态）和 `review_git_credential`（AES-256-GCM 加密 PAT，`CredentialCryptoService` 提供加解密能力，主密钥来自 `ACR_CREDENTIAL_MASTER_KEY`）。
- M1 补充切片已固定约束：按仓库接收 PR 事件，以项目 `pr_target_branches` 匹配 base 分支；来源分支不预配置；任务必须保存事件中的 PR 编号、base SHA、head SHA，不能用分支当前 HEAD 替代事件版本。
- `sys_config` 已有 `review.github.prEvents = opened,reopened,synchronize` 平台统一事件白名单，本切片直接消费。
- `GitProvider` 是项目接入契约（地址解析、连接测试、仓库读取），与 Webhook 关注点不同。Webhook 的验签与载荷解析建立独立适配契约，不扩充 `GitProvider` 接口，避免接口膨胀和语义混淆。
- 操作日志已排除 `token`/`tokenCiphertext`，本切片追加 `webhookSecret`/`webhookSecretCiphertext`。

## 3. 完整链路设计（事件、任务、状态流转、Provider 边界）

本节固定后续切片的公共语义，Diff 获取、审查执行、回写、通知都在此基础上扩展，不再重新定义。

### 3.1 对象分层

```text
GitHub Webhook POST
  └─ Webhook 事件（review_webhook_event）：一次可信投递的不可变接入事实
       └─ 审查任务（review_task）：一次由事件触发的执行实例（1:1）
            └─ 审查执行 / 问题 / 交付记录（后续切片，分别独立建表）
```

- 事件只回答“平台投递了什么、平台如何处理这次投递”，不承载审查结果；
- 任务只回答“这次审查要审什么、执行到哪一步”，事件 ID 是任务的触发来源；
- 事件与任务 1:1，靠 `review_task.event_id` 唯一键兜底，同一事件绝不重复生成任务。

### 3.2 事件处理状态机

事件 `process_status` 终态互斥，只表达接入侧处理结果：

| 状态 | 含义 | 进入条件 |
|---|---|---|
| RECEIVED | 已接收待处理（初始占位） | 事件首次入库 |
| ACCEPTED | 已受理并生成审查任务 | 验签通过、项目匹配、分支命中、幂等通过 |
| IGNORED | 已忽略（合法但无需处理） | 非 PR 事件、非白名单 action、项目未匹配/停用/未启用 PR 审查、目标分支未命中 |
| DUPLICATE | 重复投递 | `(provider, delivery_id)` 唯一键冲突，累加 `duplicate_count`，不新增行 |
| FAILED | 接入失败 | 载荷超限/非法、未配置 Secret、签名校验失败、内部异常 |

验签失败与忽略区分：签名无效是安全事件，记 FAILED 并返回 401；分支未命中是正常业务过滤，记 IGNORED 并返回 200。

### 3.3 任务状态机

任务 `task_status` 只表达执行进度（路线图 3.2 固定语义）：

```text
PENDING（待执行）→ RUNNING（执行中）→ SUCCESS（已完成）/ FAILED（已失败）/ CANCELLED（已取消）
```

本切片只创建 `PENDING` 任务；后续执行切片负责任务领取和状态推进，执行结论（通过/警告/阻断）届时独立成列，不与执行状态混用。`failure_message` 列本切片保留为空，供执行失败时填写；任务列表同步展示。

### 3.4 Git Provider Webhook 公共边界

```text
acr-admin  GitHubWebhookController（协议、载荷限制、匿名放行、快速响应）
  └─ acr-review  ReviewWebhookService（项目匹配、白名单、分支判断、幂等、建单）
       └─ GitWebhookAdapter（平台契约：验签 + 载荷解析 → 平台无关事件对象）
            └─ github/GitHubWebhookAdapter（HMAC-SHA256、X-Hub-Signature-256、payload 字段）
```

- `GitWebhookAdapter.providerCode()` 与 `GitProvider.providerCode()` 使用同一选择键 `GITHUB`，后续第二平台两个契约各自新增实现，互不干扰；
- 适配器只做两件纯函数式工作：`verifySignature(secret, payloadBytes, signatureHeader)` 和 `parsePullRequestEvent(eventType, deliveryId, payload)`，不发起 HTTP 调用，不含业务规则；
- 平台无关事件对象 `GitPullRequestEvent` 承载业务事实：deliveryId、action、仓库 owner/name、PR 编号、标题、来源分支、目标分支、base SHA、head SHA；GitHub 事件头、签名字段和 payload 结构差异不越出 `git/github` 包；
- action 白名单（`sys_config` 平台参数）、目标分支匹配（项目配置）、项目启停判断属于业务规则，全部在用例层，不进适配器；
- 后续 Diff 获取、评论回写沿用同一边界：业务事实进平台无关对象，平台 API 差异留在适配器。

## 4. 数据对象

### 4.1 Webhook 事件 `review_webhook_event`

| 列 | 说明 |
|---|---|
| event_id | 主键 |
| provider | `GITHUB` |
| delivery_id | GitHub `X-GitHub-Delivery`，幂等键 |
| event_type | `X-GitHub-Event`，如 `pull_request`、`ping` |
| action | PR 动作（`opened` 等），ping 等事件为空 |
| repository_owner / repository_name | 从载荷解析，用于项目匹配 |
| project_id | 匹配到的项目，未匹配为空 |
| pr_number / pr_title / source_branch / target_branch / base_sha / head_sha | PR 业务事实 |
| process_status / process_message | 处理状态与原因说明 |
| duplicate_count | 重复投递次数（首次为 0，重复时累加并更新 `process_time`） |
| task_id | 生成的审查任务，未生成为空 |
| payload_size | 载荷字节数；不保存原始载荷（Diff 走 API 拉取，载荷无审查价值且含代码资产，保留即风险） |
| receive_time / process_time | 接收与处理完成时间 |

唯一键 `uk_webhook_delivery (provider, delivery_id)`；索引：project_id、process_status、receive_time。

### 4.2 审查任务 `review_task`

| 列 | 说明 |
|---|---|
| task_id | 主键 |
| project_id / event_id | 所属项目、触发事件（event_id 唯一） |
| provider | `GITHUB` |
| pr_number / pr_title / source_branch / target_branch | PR 事实（事件快照，不随分支移动） |
| base_sha / head_sha | 审查基线版本（事件版本，不用分支当前 HEAD 替代） |
| trigger_type | `WEBHOOK`，预留 `MANUAL`/`SCHEDULE` |
| task_status | `PENDING`（状态机见 3.3） |
| failure_message | 执行失败原因，本切片为空 |
| 审计字段 | create_by/time 等 |

索引：project_id、task_status、(project_id, pr_number)。

### 4.3 项目表增量列 `review_project`

| 列 | 说明 |
|---|---|
| webhook_secret_ciphertext | Webhook Secret 密文（AES-256-GCM，与 PAT 不同 AAD） |
| last_webhook_time | 最近一次成功接收事件的时间 |
| last_webhook_result | 最近一次接收结果摘要（如“已受理 PR #12”“已忽略：非审查目标分支”） |

Secret 只写不回显：详情响应只返回 `webhookSecretConfigured` 布尔状态；编辑留空保留原值，与 PAT 同一模式。

## 5. 安全设计

- Secret 使用 AES-256-GCM 加密，`CredentialCryptoService` 增加独立 AAD（`acr-review:github-webhook-secret:v1`）的加解密方法，主密钥复用 `ACR_CREDENTIAL_MASTER_KEY`；
- 验签使用 HMAC-SHA256（`X-Hub-Signature-256`，`sha256=` 前缀 hex），比较使用恒定时间比较，防时序攻击；验签基于请求原始字节，不经 JSON 反序列化重排；
- Secret 与签名不进入任何响应、日志和异常消息；操作日志排除 `webhookSecret`/`webhookSecretCiphertext`；
- 载荷限制 256KB（可配置），超限记 FAILED 返回 413；
- Webhook 端点匿名放行（无会话），安全完全依赖签名校验；验签失败返回 401，不泄露项目是否存在；
- 不保存原始载荷，只保存解析后的业务字段与载荷大小。

## 6. 处理流程（请求线程内，毫秒级，无外部调用）

```text
1. 载荷大小检查            → 超限：FAILED + 413
2. 插入事件占位(RECEIVED)   → 唯一键冲突：duplicate_count+1，返回 200（重复）
3. 解析载荷                → 非法：FAILED + 200（记录后不再投递）
4. 按仓库匹配项目(启用中)   → 未匹配：IGNORED + 200
5. 读取项目 Secret 验签     → 未配置/签名无效：FAILED + 401
6. event_type 白名单        → 非 pull_request：IGNORED + 200
7. action 白名单            → 非 opened/reopened/synchronize：IGNORED + 200
8. 项目 PR 审查启用 + 目标分支匹配 → 未命中：IGNORED + 200
9. 同事务：插入任务(PENDING) + 事件置 ACCEPTED 关联 task_id
10. 更新项目 last_webhook_time/result，返回 200
```

- 步骤 2 先落占位：所有到达事件（含后续失败的）都有审计记录；处理异常中断遗留的 RECEIVED 可由后续对账清理；
- 步骤 9 同事务保证事件与任务一致；全程无 GitHub API 调用、无审查执行，满足快速响应；
- 重复投递不产生新事件行、不生成任务，只累加计数，GitHub 侧收到 200 停止重试。

## 7. API 与权限

| 资源 | API | 权限 |
|---|---|---|
| GitHub Webhook | `POST /webhook/github` | 匿名（签名校验代替会话） |
| 审查任务 | `GET /review/task/list`、`GET /review/task/{taskId}` | `review:task:list/query` |
| 项目 Webhook 配置 | 复用 `PUT /review/project`（secret 字段）与 `GET /review/project/{id}`（配置状态/最近接收） | `review:project:edit/query` |

任务列表通过 join `review_project` 复用部门/负责人数据范围，跨项目不可见。Webhook 回调地址由配置项 `review.webhook.callback-base-url` 拼接 `/webhook/github` 生成，随项目详情返回展示。

## 8. 页面

- 一级目录“代码审查”下新增“审查任务”：筛选（项目、PR 编号、任务状态、时间范围）+ 表格（任务 ID、项目、PR 编号与标题、来源→目标分支、head SHA 短码、任务状态、触发时间、失败原因）+ 分页；本切片只读，不提供操作按钮。
- 项目表单新增“Webhook 配置”分区：回调地址（只读+复制）、Secret 输入（密码框，编辑留空保留）、配置状态标签、最近接收时间与结果（只读）。

## 9. 验收标准

1. 增量 SQL 可重复执行：两张新表、项目表三列增量、任务菜单与 2 项权限完整；
2. 验签正确/错误 Secret、伪造签名、缺失签名头的行为符合预期，Secret 不明文出现在响应、日志、异常中；
3. 同一 delivery_id 重复投递只生成一条任务，事件记录重复计数；
4. 非白名单 action、非目标分支、项目停用/未匹配分别按 IGNORED 记录；
5. 后端测试、根 Maven 构建、前端生产构建通过；
6. 测试仓库真实 PR 触发事件，后台可见一条 PENDING 任务，项目详情展示最近接收结果。

## 10. 风险与后续衔接

- 事件与任务 1:1 是 MVP 简化；人工重跑场景（同一事件再次触发）由后续切片以新任务+原事件关联方式处理，不破坏本切片唯一键；
- RECEIVED 残留事件的清理策略随执行切片一并设计；
- Secret 轮换与主密钥轮换流程属运维手册，不在本切片。
