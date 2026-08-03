# M5 IM 三渠道通知与投递记录

> **状态（2026-08-03）：Review 已通过（含飞书同行多链接、渠道不可用落真实渠道类型、投递编排测试补齐三项修正），待真实环境验收。** 前置：`docs/planning/review-comment-writeback-m4.md`、`docs/planning/review-pipeline-m3.md`、`docs/planning/review-scoring-result-protocol.md`、`docs/planning/review-record-experience-m3.1.md`。本设计明确审查结束后的 IM 结论摘要投递（钉钉/企微/飞书群机器人）、平台级渠道资产、项目单渠道绑定，以及投递记录统一查询与失败补发。

## 1. 目标与成功指标

- 审查结束后，相关群内即可看到**审查结论摘要**（非泛化「系统通知」）：SUCCESS 发摘要卡，FAILED 发简讯；开发者无需打开后台即可获知结论与入口链接；
- IM 与 GitHub 总结评论共用同一内容模型（结论/总分/PR 信息/变更规模/Top3/范围统计），各自适配格式；不含代码片段；
- 渠道为平台级资产（`review_notify_channel`），项目单渠道下拉引用；Webhook URL 与加签 Secret 加密存储、不回显、不进日志；
- 投递事实继续落在 `review_delivery_record`（扩 IM 三渠道值），与审查任务状态分离；投递失败不回滚任务结论；
- 后台「通知管理」提供渠道配置（含测试发送）与投递记录（含 GitHub 评论回写行）统一运维面。

## 2. 当前基线与缺口（2026-08-03 核查）

| 链路 | 现状 | 缺口 |
|---|---|---|
| 审查执行 | SUCCESS / FAILED 落库后无 IM 副作用；M4 仅在 SUCCESS 后回写 GitHub 总结评论 | SUCCESS/FAILED 后按项目绑定发送 IM |
| 内容渲染 | `ReviewCommentBodyRenderer` 直接产出 GitHub 表格 Markdown；Top3 描述截断 ≤500 | 缺共享内容模型与 IM 行式排版；企微 4096 字节约束更严 |
| 渠道资产 | 仅有 `review_git_credential`（平台资产）+ `review_project.credential_id` 引用范式 | 无通知渠道表；项目无 `notify_*` 绑定 |
| 投递事实 | `review_delivery_record` 已落地；`channel=GITHUB_PR_SUMMARY_COMMENT`；幂等键 PR 级 | 缺 `DINGTALK_ROBOT` / `WECOM_ROBOT` / `FEISHU_BOT`；IM 需任务级幂等键；`external_id` 对机器人可空 |
| 重试语义 | `POST /review/delivery/{taskId}/retry`：以 PR 最近 SUCCESS 结论渲染后回写评论 | 投递记录页需「按记录定位 → 原渠道补发」；须与 M4 语义并存并写清边界 |
| 前端/菜单 | 任务/记录详情展示 GitHub 投递摘要；无通知一级菜单 | 需「通知管理」下渠道管理 + 投递记录两页；项目表单绑定渠道 |
| 后台详情链接 | 无统一 `sys_config` 前台基址 | IM 消息需「详情」链接指向任务/记录详情 |

可复用存量资产：`CredentialCryptoService`（独立 AAD 加解密）、`review_delivery_record` 与 `review:delivery:retry`、任务上的 `pr_title` / `pr_author` / `changed_files` / `additions` / `deletions`、字典与菜单幂等 SQL 模式、M4 失败隔离（投递异常不改 `task_status`）。M5 在审查落库之后叠加 IM 投递，不改写 M1–M4 审查与 GitHub 回写主行为（除共享内容模型抽取对渲染路径的等价重构）。

## 3. 本次范围 / 非范围

### 3.1 范围

1. 从 `ReviewCommentBodyRenderer` 抽取共享内容模型；GitHub 评论与 IM 各自适配格式（GitHub 保持表格 Markdown + 固定标记语义不变）；
2. 新建平台级 `review_notify_channel`（钉钉/企微/飞书群机器人）；Webhook URL 与 Secret 经 `CredentialCryptoService` 加密（URL 本身敏感，钉钉 URL 含 `access_token`）；
3. `review_project` 增加 `notify_enabled`、`notify_channel_id`、`notify_on_failure`；项目表单单渠道下拉（仅已启用渠道）；**渠道绑定不做任务快照冻结**（投递时读项目当前配置）；
4. SUCCESS → IM 摘要卡；FAILED → IM 简讯（`notify_on_failure` 可关，默认开）；FAILED **仍不发** GitHub 评论（沿用 M4）；
5. `review_delivery_record.channel` 扩展三值；IM 幂等键任务级；投递失败不影响任务状态；
6. 一级菜单「通知管理」：子菜单「通知渠道」「投递记录」；投递记录复用表，含 GitHub 行，支持筛选/失败原因/失败重试；
7. 单测覆盖内容模型、IM 渲染截断、渠道加解密脱敏、幂等 upsert、失败隔离；`mvn test` + `npm run build:prod`；CHANGELOG 补 M5。

### 3.2 非范围

- 通知策略、频控、聚合、按严重度路由、IM 账号映射与强 @ 人；
- 一项目多渠道并发投递；
- 飞书互动卡片、钉钉 ActionCard / 独立跳转卡片（本期行式 markdown/text）；
- 邮件（见第 10 节依赖理由）；
- `review_notify_channel` 之外的新投递/策略表；预建未使用字段；
- inline 评论、Status Check、第二 Git Provider、新 Maven 模块/框架；
- 改写 M1–M3.2 审查范围、评分协议或 M4「一 PR 一条总结评论」语义。

## 4. 依赖与待决策项（设计定稿）

| 项 | 定稿 |
|---|---|
| 产品定位 | IM 消息 = **审查结论摘要**；与 GitHub 总结评论同源内容模型，非独立「通知文案系统」 |
| 渠道形态 | 群机器人 Webhook 三适配器：`DINGTALK_ROBOT` / `WECOM_ROBOT` / `FEISHU_BOT` |
| 资产范式 | 对齐 Git 凭据：平台表 + 项目外键引用；密钥落在渠道行密文字段，**不**另建凭据表 |
| 项目绑定 | 单渠道；`notify_enabled` 总开关；`notify_on_failure` 控制 FAILED 简讯（默认开） |
| 快照 | 渠道**不**写入任务/run 快照；投递时读项目当前 `notify_*` 与渠道当前密文/启停 |
| 触发 | 任务落库为 SUCCESS 或 FAILED 之后调用 IM 投递；与 GitHub 投递相互独立、均可失败隔离 |
| GitHub × FAILED | FAILED 不发 GitHub 评论（M4 不变） |
| IM 幂等 | `{channelType}:{taskId}:REVIEW_DONE`，同行 upsert；同一任务同渠道不刷多条记录 |
| `external_id` | IM 允许为空（机器人无稳定消息 id 或不依赖更新） |
| 重试双语义 | 见 §5.6：GitHub 仍按「PR 最近 SUCCESS」；IM 按「原 delivery 记录对应 task 结论重渲染」 |
| 详情链接 | `sys_config` 键 `review.ui.base-url`（如 `https://acr.example.com`）；未配置则消息省略「详情」链或仅保留 PR 链 |
| 邮件 | 明确非范围（缺 SMTP + 按人策略层） |

## 5. 数据、接口、权限和流程

### 5.1 共享内容模型与消息模板

从现有渲染逻辑抽取只读内容对象（名称实现期确定，语义如下）：

| 字段 | 来源 | 说明 |
|---|---|---|
| conclusion / conclusionLabel | `review_task.review_conclusion` | `PASS→通过`、`WARN→建议修改`、`BLOCK→高风险`；缺省 `--` |
| totalScore | task/run | 缺省显示 `--` |
| prNumber / prTitle / prAuthor | task | |
| repositoryOwner / repositoryName | project | 展示 `owner/name` |
| sourceBranch / targetBranch | task | |
| changedFiles / additions / deletions | task | 变更规模一行 |
| topIssues | run | 最多 3 条；含 severity/origin/title/path/lines/description |
| scopeStats | run.resultJson | 纳入/排除/扩展/新增/存量 |
| prUrl | 由 provider+仓库+prNumber 拼装 | GitHub：`{html_base}/pull/{n}` |
| detailUrl | `review.ui.base-url` + 任务或记录详情路径 | 未配置则不输出详情链 |
| taskStatus | task | 区分 SUCCESS 摘要 / FAILED 简讯 |

**GitHub 适配**：继续表格 Markdown + `<!-- acr-review-summary -->`；Top3 描述截断维持现有 ≤500；行为与 M4 验收兼容（允许等价重构，不改对外评论语义）。

**IM 适配（SUCCESS 摘要卡）**：行式排版（钉钉/企微 markdown **均不支持表格**，飞书本期亦用文本/markdown，不走卡片）。企微 markdown **4096 字节**为最严上限：整卡需可控；Top3 **描述截断 ≤150 字**；**不含代码片段**。

消息样例：

```text
### ⚠️ AI Code Review · 建议修改
总分 72/100 · PR #8 重构用户登录校验
acme/demo · zhangsan · dev → main · 12 文件 +120/−30

Top 3 重点问题
1. [高·新增] 密码明文传输风险 — UserController L42-48
2. …
3. …

范围统计：纳入 10 · 排除 2 · 扩展 1 · 新增 2 · 存量 1
PR：<链接>  详情：<链接>
```

结论文案前缀图标约定（实现常量）：`通过`→✅、`建议修改`→⚠️、`高风险`→🚨、未知→ℹ️。

**IM 适配（FAILED 简讯）**（示意）：

```text
### ❌ AI Code Review · 执行失败
PR #8 重构用户登录校验 · acme/demo
失败类型：<中文失败类型> · 任务 #<taskId>
详情：<链接>
```

不包含 Top3、范围统计与评分。失败类型取任务已落库的中文/字典标签，**不得**附带密钥或堆栈。

各适配器包装：

| 渠道 | 发送形态 | 备注 |
|---|---|---|
| 钉钉 | `msgtype=markdown` | 若配置了 Secret，按钉钉加签（timestamp + HMAC-SHA256）附加 query |
| 企微 | `msgtype=markdown` | 正文按 UTF-8 字节校验 ≤4096；超限先截断 Top3 描述再必要时缩短标题 |
| 飞书 | `msg_type=text` 或 `post` 富文本（post 保证详情链接可点击） | 飞书自定义机器人无 markdown 类型；**不**使用互动卡片；加签若配置 Secret 则按飞书机器人签名规范 |

### 5.2 表 `review_notify_channel`（新建）

平台级渠道资产，对齐 `review_git_credential` 的治理字段风格：

| 列 | 说明 |
|---|---|
| `channel_id` | PK |
| `channel_name` | 显示名；同类型下唯一 |
| `channel_type` | `DINGTALK_ROBOT` / `WECOM_ROBOT` / `FEISHU_BOT` |
| `webhook_url_ciphertext` | AES-GCM 加密后的完整 Webhook URL（**含**钉钉 `access_token` 等敏感 query） |
| `secret_ciphertext` | 加签 Secret，可空；空表示不加签 |
| `status` | `0` 启用 / `1` 停用 |
| `last_check_status` | `UNTESTED` / `SUCCESS` / `FAILED`（测试发送结果） |
| `last_check_message` | ≤255，已脱敏 |
| `last_check_time` | 最近测试时间 |
| `remark` | 备注 |
| `create_time` / `update_time` / `create_by` / `update_by` | 审计字段 |

约束与索引：

- `UNIQUE (channel_type, channel_name)`；
- `KEY (status)`、`KEY (channel_type)`；
- 列表/详情 API：**永不**返回密文或明文 URL/Secret；编辑回显仅「已配置」布尔或脱敏占位；未改密文字段提交时保留原密文（对齐凭据编辑）。

加密：`CredentialCryptoService` 新增独立 AAD，例如：

- URL：`acr-review:notify-webhook-url:v1`
- Secret：`acr-review:notify-webhook-secret:v1`

主密钥仍为 `ACR_CREDENTIAL_MASTER_KEY`。

SQL 文件：`sql/24_notification_management_m5.sql`（已落地）。

### 5.3 项目绑定列（`review_project` 增量）

| 列 | 说明 |
|---|---|
| `notify_enabled` | `char(1)`，默认 `'N'`（新建项目默认关；显式开启后才发 IM；取值口径对齐 M3.2 `scope_*` 的 `'Y'/'N'`） |
| `notify_channel_id` | `bigint` 可空；指向 `review_notify_channel.channel_id` |
| `notify_on_failure` | `char(1)`，默认 `'Y'`；仅当 `notify_enabled='Y'` 且本列为 `'Y'` 时，FAILED 才发简讯 |

校验：

- `notify_enabled='N'` 时忽略渠道，不发 IM；
- `notify_enabled` 由 `'N'` 改为 `'Y'` 时，`notify_channel_id` 必填且渠道存在、`status=0`、类型为三机器人之一；
- 渠道被停用或删除后：已绑定项目保存校验失败；运行时投递记 `FAILED`（原因「通知渠道不可用」），**不**改任务状态；
- **不**把 `notify_*` 写入 `review_task` / `review_task_run` 快照。

### 5.4 投递记录扩展（复用 `review_delivery_record`）

表结构**不新建**；扩展 `channel` 取值与幂等键约定。现有列语义保持：

| 列 | M5 补充说明 |
|---|---|
| `provider` | 仍为代码平台（本期 `GITHUB`），不把 IM 厂商写入 provider |
| `channel` | 新增 `DINGTALK_ROBOT` / `WECOM_ROBOT` / `FEISHU_BOT`（保留 `GITHUB_PR_SUMMARY_COMMENT`） |
| `pr_number` | IM 行同样填写任务 PR 号，便于筛选 |
| `idempotency_key` | IM：`{channelType}:{taskId}:REVIEW_DONE` |
| `external_id` | IM 允许 NULL；GitHub 成功后仍必填 comment id（M4） |
| `delivery_status` | 仍仅 `SUCCESS` / `FAILED`；无中间态 |
| `task_id` / `run_id` | 最近一次发起/成功投递关联的任务与 run |

幂等键对比：

```text
# M4 GitHub（PR 级，同行随「最新成功任务」更新）
GITHUB:{projectId}:{prNumber}:SUMMARY_COMMENT

# M5 IM（任务级，同行仅服务该 task 的重试/补发）
DINGTALK_ROBOT:{taskId}:REVIEW_DONE
WECOM_ROBOT:{taskId}:REVIEW_DONE
FEISHU_BOT:{taskId}:REVIEW_DONE
```

状态语义（与 M4 一致）：

| 状态 | 含义 |
|---|---|
| `SUCCESS` | 该渠道最近一次投递成功 |
| `FAILED` | 最近一次尝试失败，可按记录重试 |

投递仍为同步调用：结束后一次性 upsert，不预插僵尸行。新建字典 `review_delivery_channel`（渠道类型中文名：GitHub 总结评论 / 钉钉机器人 / 企微机器人 / 飞书机器人），供投递记录筛选项；`review_delivery_status` 复用。

### 5.5 模块边界

```text
acr-admin
  ReviewNotifyChannelController   # 渠道 CRUD / 启停 / 测试发送
  ReviewDeliveryController        # 扩展：投递记录列表 + 按 deliveryId 重试；保留 M4 taskId 重试
acr-review
  ├─ delivery/
  │    ReviewSummaryContent           # 共享内容模型（从 task/run/project 装配）
  │    ReviewSummaryContentFactory    # 装配（可与 renderer 同包）
  │    ReviewCommentBodyRenderer      # GitHub：Content → Markdown（标记不变）
  │    ReviewNotifyMessageRenderer    # IM：Content → 行式文本（SUCCESS/FAILED）
  │    ReviewDeliveryConstants        # 渠道常量、IM 截断、幂等键工厂
  │    domain/mapper/ReviewDeliveryRecord*  # 列表查询条件扩展
  ├─ notify/
  │    ReviewNotifyChannel*           # 实体/Mapper
  │    NotifyRobotClient              # 能力接口 send(webhook, secret, markdown/text)
  │    dingtalk/DingTalkRobotClient
  │    wecom/WeComRobotClient
  │    feishu/FeishuBotClient
  ├─ security/CredentialCryptoService # 新增 notify URL/Secret AAD 方法
  └─ service/
       IReviewNotifyChannelService
       IReviewNotifyDeliveryService   # 或并入 IReviewDeliveryService 的 IM 分支
       impl/*                         # SUCCESS/FAILED 后投递；失败隔离
       ReviewTaskExecutionServiceImpl # 落库后挂钩 GitHub（仅 SUCCESS）+ IM（SUCCESS/FAILED）
```

- Controller 不访问 Mapper；
- 不新建 Maven 模块；机器人差异只在 `notify/{vendor}`；
- **不**建设通用「通知中心框架」或策略引擎；仅三机器人适配 + 投递用例。

### 5.6 主流程

```text
审查执行落库 SUCCESS
  →（M4）GitHub 总结评论投递（不变）
  →（M5）若 project.notify_enabled='Y' 且渠道可用
       → 装配 ReviewSummaryContent
       → ReviewNotifyMessageRenderer.renderSuccess
       → 解密 URL/Secret → 适配器发送
       → upsert review_delivery_record（IM 幂等键，SUCCESS/FAILED）
  → 任一投递异常 catch 落失败记录，任务保持 SUCCESS

审查执行落库 FAILED
  → 不发 GitHub 评论
  →（M5）若 notify_enabled='Y' 且 notify_on_failure='Y' 且渠道可用
       → 渲染 FAILED 简讯 → 发送 → upsert 投递记录
  → 投递失败不改 task_status=FAILED
```

**重试双语义（定稿）**：

| 入口 | 定位 | 渲染数据源 | 外部行为 | 为何无「旧结论覆盖新结论」问题 |
|---|---|---|---|---|
| 任务/记录详情「重试投递」（M4） | `taskId` → 项目+PR | **该 PR 最近一次 SUCCESS 任务**结论 | 查找标记后 update/create **同一条** PR 评论 | PR 级持久评论必须跟最新成功结论；故故意忽略锚点旧任务 |
| 投递记录页「失败重试」（M5） | `deliveryId` → 原 `channel` + 原 `task_id` | **该 task 自身**结论（SUCCESS 摘要或 FAILED 简讯）重渲染 | IM：再次 POST 机器人（群内可能多一条消息；DB 同行 upsert）；GitHub 行：复用 M4 写评论逻辑但数据源仍走「PR 最近 SUCCESS」（见下） | IM 幂等键绑定 taskId，结论在任务终态后**不可变**，重试只是补发同一结论，不存在跨任务覆盖；GitHub 行在记录页重试时**仍执行 M4 PR 级最新结论规则**，避免用历史 task 覆盖新评论 |

记录页点到 `GITHUB_PR_SUMMARY_COMMENT` 失败行时：以该行的 `project_id`+`pr_number` 走 M4「最近 SUCCESS」路径，并 upsert **同一 PR 级幂等键**行（可能更新 `task_id` 至最新成功任务）。记录页点到 IM 失败行时：严格使用行内 `task_id` 重渲染并 upsert **该任务级幂等键**行。

### 5.7 接口与权限

**通知渠道**

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/review/notify/channel/list` | `review:notify:list` | 分页列表；无密文 |
| GET | `/review/notify/channel/{id}` | `review:notify:query` | 详情；URL/Secret 仅脱敏标志 |
| POST | `/review/notify/channel` | `review:notify:add` | 新增（明文 URL/Secret 仅请求体入站即加密） |
| PUT | `/review/notify/channel` | `review:notify:edit` | 修改；空 Secret/URL 表示不更新该密文 |
| PUT | `/review/notify/channel/changeStatus` | `review:notify:status` | 启停 |
| DELETE | `/review/notify/channel/{ids}` | `review:notify:remove` | 删除；若仍被项目引用则拒绝 |
| POST | `/review/notify/channel/{channelId}/test` | `review:notify:test` | 测试发送固定短文案；更新 last_check_* |

**投递记录**

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/review/delivery/list` | `review:delivery:list` | 筛选：时间、项目、渠道、状态、PR；数据范围按项目 |
| GET | `/review/delivery/{deliveryId}` | `review:delivery:query` | 详情（含 failureMessage） |
| POST | `/review/delivery/record/{deliveryId}/retry` | `review:delivery:retry` | 按记录重试（语义见 §5.6） |
| POST | `/review/delivery/{taskId}/retry` | `review:delivery:retry` | **保留 M4**：GitHub PR 总结评论重试 |

项目下拉「已启用渠道」：复用 list 接口 `status=0` 精简结果，权限随项目编辑（`review:project:edit`）可读，或提供 `review:notify:list` 只读子集。

**菜单（号段已于 2026-08-03 经本地库核验空闲；SQL 落地前复核一次）**

| menu_id | 名称 | 类型 | 说明 |
|---|---|---|---|
| 5 | 通知管理 | M | 一级目录（4 已被「模型服务」占用）；`order_num` 紧随「代码审查」之后 |
| 129 | 通知渠道 | C | `notify/channel/index`；挂按钮 1151–1157（list/query/add/edit/remove/status/test） |
| 130 | 投递记录 | C | `notify/delivery/index`；挂按钮 1158–1159（list/query）；重试复用 `review:delivery:retry`（1150 已存在，归属审查记录） |

按钮权限建议：`review:notify:list|query|add|edit|remove|status|test`；`review:delivery:list|query`；重试继续 `review:delivery:retry`。角色 2 同步授权（对齐 M4）。

### 5.8 前端

1. **通知渠道**列表/表单：类型、名称、Webhook URL、Secret（密码框）、启停、最近测试、测试发送；样式对齐连接管理/凭据页；遵守 `rules/UI_THEME_RULES.md`。
2. **投递记录**列表：时间、项目、渠道、状态、PR、任务、失败原因、操作（失败重试）；筛选齐全；GitHub 与 IM 同行展示。
3. **项目表单**：在合适分区（如「通知」或「Webhook」旁）增加：启用通知、渠道下拉、失败时通知开关；未启用时渠道控件禁用。
4. **任务/记录详情**：保留 M4「PR 评论投递」区块；可附加「IM 投递」摘要（同 task 下 IM 渠道最新一行），失败且有权限时跳转投递记录或直接调 `deliveryId` 重试——实现期二选一，优先少打扰详情页（可仅展示状态 + 链到投递记录）。

## 6. 失败分类与安全

| 场景 | 处理 |
|---|---|
| Webhook 4xx/5xx / 超时 | 投递 `FAILED`，任务状态不变；文案中文、可提示稍后重试 |
| 加签错误 / URL 失效 | `FAILED`，不回显 Secret/完整 URL |
| 渠道停用或项目未绑定 | 跳过或 `FAILED`（已启用但渠道不可用 → FAILED） |
| 企微超 4096 字节 | 发送前截断；仍失败则记 FAILED |
| 测试发送失败 | 只更新渠道 `last_check_*`，不写 `review_delivery_record` |
| 任务 FAILED | 不发 GitHub；按 `notify_on_failure` 决定 IM 简讯 |
| 记录页重试 IM | 用原 task 结论；群内可能新增一条机器人消息（机器人通常无法幂等编辑） |

安全：

- URL/Secret 仅内存解密；响应与日志禁止明文；失败原因走既有脱敏；
- 测试发送正文使用固定无敏感信息文案；
- 删除渠道前检查项目引用。

## 7. 测试要点

- `ReviewSummaryContent` / GitHub renderer：与现有评论单测等价（标记、结论映射、缺字段）；
- `ReviewNotifyMessageRenderer`：行式结构、Top3 ≤150 字、无代码块、企微字节上限、FAILED 简讯字段集；
- 渠道服务：加密落库、编辑保留密文、列表无泄漏、启停与删除引用校验、测试发送更新检测字段；
- IM 投递：幂等键冲突更新同行；`external_id` 可空成功；失败不改 `task_status`；`notify_enabled='N'` 不发送；`notify_on_failure='N'` 时 FAILED 不发；
- 重试：deliveryId→IM 用原 task；deliveryId→GitHub 仍解析为 PR 最近 SUCCESS；taskId 重试保持 M4；
- 适配器：可用 MockWebServer 校验钉钉加签 query、企微/飞书请求体；
- 前端：生产构建通过即可。

## 8. 分步实现计划

| 步 | 内容 | 验证 | 状态 |
|---|---|---|---|
| 1 | 设计文档定稿（本文）+ 路线图 §4.2/§7.3 小改 | 人工确认范围、表结构、双重建试语义 | ✅ |
| 2 | SQL `24_notification_management_m5.sql`（渠道表、项目三列、字典、菜单权限、`review.ui.base-url`）+ `docs/deployment.md` / `sql/README.md` | 脚本幂等、utf8mb4；清单连续编号 | ✅ |
| 3 | `CredentialCryptoService` AAD 方法；渠道领域/Mapper/Service；测试发送 | 加解密与脱敏单测绿 | ✅ |
| 4 | 共享内容模型 + GitHub renderer 等价迁移 + IM renderer + 单测 | 渲染单测绿；GitHub 标记回归 | ✅ |
| 5 | 三机器人客户端 + IM 投递用例 + 执行链挂钩 + deliveryId 重试；列表 API | `mvn test`；失败隔离用例 | ✅ |
| 6 | 前端：通知管理两页、项目绑定、详情/记录联动；CHANGELOG | `npm run build:prod` | ✅ |

每步可独立提交；步 2–3 合入不改变审查主链路行为，步 5 接入后生效。

## 9. 验收标准与风险

**验收**：

- 项目启用通知并绑定渠道后：SUCCESS 任务群内出现摘要卡；FAILED 且 `notify_on_failure` 开启时出现简讯；关闭总开关后无 IM；
- 同 task 重复投递/重试只维护一行 IM 投递记录（幂等键）；GitHub 仍保持一 PR 一条总结评论；
- 人为制造 Webhook 失败 → 任务状态不变，投递 FAILED，投递记录页可补发；
- 渠道 URL/Secret 接口不回显明文；日志无 Secret/完整 Webhook；
- 投递记录可筛到 GitHub 与三 IM 渠道行并展示失败原因；
- `mvn test` 全绿；`cd acr-ui && npm run build:prod` 通过；CHANGELOG 有 M5；

**风险与对策**：

- 机器人无法编辑历史消息：IM 重试可能导致群内重复卡片——接受并在 UI 文案写明「补发」；DB 层仍单行；
- 企微字节上限：发送前校验 + Top3 描述 ≤150 + 必要时降级省略次要行；
- 渠道实时读取导致「审查时启用、投递时被关」：记 FAILED 可重试，不回写任务；
- 与 M4 重试入口并存：文案区分「重试 PR 评论（跟最新成功结论）」与「按投递记录补发」；
- `review.ui.base-url` 未配置：消息仍含 PR 链接，详情链省略，避免错误 URL。

## 10. 非范围（再声明）

通知策略/频控/聚合、IM 账号映射与强 @、一项目多渠道、飞书卡片/钉钉 ActionCard、邮件、通用通知框架、`review_notify_channel` 以外的新业务表、预建未使用字段、inline/Status Check/多 Provider。

**邮件为何非范围**：按人投递依赖 SMTP（或企业邮渠道）接入层，以及「收件人解析 / 通知策略」层（事件匹配、成员映射、订阅偏好）。本期仅具备群 Webhook 广播能力，上述依赖未满足；邮件单独立项，避免在无策略层时落入「全局邮箱刷屏」或半吊子按人路由。
