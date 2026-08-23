# 问题自动指派闭环（M13）— 设计文档

- 阶段：核心版（V0.2）体验与闭环切片
- 日期：2026-08-22
- 状态：待评审
- 前置：`docs/planning/product-roadmap.md`、`issue-lifecycle-m8.md`、`identity-binding-design.md`、`notification-management-m5.md`、`production-readiness-governance.md`

## 0. 目标与成功指标

### 业务目标

问题台账当前"发现即入账"，但无人对问题负责：北极星配套指标「问题按期关闭率」缺少责任主体这一前提。本切片建立**以提交事实为依据的自动指派**：问题物化时系统自动推断责任人并落库，审查总结消息在 IM 群内 @ 到责任人；配合转派救济与逾期标记，形成"发现 → 定责 → 触达 → 逾期可见"的最小闭环。

设计原则（与产品原则一致）：

- **确定性流程托底**：责任人是证据推导（提交作者 / PR 发起人 / 项目负责人），不是人工派单；
- **不增加管理动作**：无指派界面、无指派权限串、无认领；人工动作只保留被指派人自助「转派」这一救济阀；
- **低噪声优先**：@ 与逾期提醒均受项目通知策略治理，不新增消息风暴；
- **复用优先**：身份关联表、对账物化入口、投递意图状态机、工作台卡片全部复用现有资产，不新建框架。

### 成功指标

1. 试点项目新物化的 NEW 问题自动指派率 ≥ 80%（分母：新物化 NEW 问题；分子：assignee_user_id 非空）；
2. 被指派人在项目群总结消息中被真实 @ 到（至少一个渠道实测）；
3. 转派动作全程留痕，误指派可被当事人纠正，不依赖管理员；
4. 逾期问题在台账可见（红色标记 + 筛选），项目群每日至多一条聚合提醒；
5. 零新增框架、组件、Maven 模块、前端依赖。

### 明确不含（防过度设计清单）

- 管理者侧指派选人器、批量指派、认领竞争；
- 独立「指派通知」投递类型——指派信息搭车既有审查总结消息，不单独发消息；
- PR 线提交作者级归属（PR 线不持久化 commit author，统一按 PR 发起人推断）；
- 逾期升级单独 @ 项目负责人——聚合提醒发在项目群、工作台待办卡可见，负责人自然覆盖，不做二级升级链；
- 管理员代维护他人 IM 身份（IM 身份仅本人自助登记）；
- 历史问题回填指派人（与 Token 采集同策略：能力上线前不回填）；
- 指派统计报表（企业版报告范围）。

## 1. 当前基线与可复用资产

| 资产 | 位置 | 本切片用法 |
|---|---|---|
| 身份关联表 `sys_user_identity`（sql/42） | `acr-system` `SysUserIdentity` / `SysUserIdentityMapper` | GIT_COMMIT 身份做邮箱/名称 → user_id 解析；IM 身份类型枚举（`IM_WECOM/IM_DINGTALK/IM_FEISHU`）本期激活消费 |
| 个人设置「我的提交邮箱」 | `SysUserProfileIdentityController`（登录即可） | 同款模式新增「我的 IM 账号」区块 |
| 对账物化入口 | `ReviewIssueServiceImpl.reconcileAfterSuccess`（约 line 102） | 新物化/补空指派时调用指派解析，写 `ASSIGN_AUTO` 动作 |
| 完成链路时序 | `ReviewTaskCompletionService`：先 `reconcileAfterSuccess`（line 42）后 `enqueueTerminalNotification`（line 69） | 总结消息渲染时指派已落库，@ 段直接搭车，无需改时序 |
| 提交作者事实 | push 线 `review_commit_fact.author_email`（sql/38）；PR 线 `task.prAuthor` | push 线按提交作者集合解析；PR 线按 PR 发起人解析 |
| 投递意图状态机 | `ReviewDeliveryIntentService`（策略门槛/冷却/幂等键/`SKIPPED` 留痕）+ `ReviewDeliveryServiceImpl.sendIm` | 逾期聚合提醒复用同一入队与执行路径，仅新增触发来源 |
| 群机器人客户端 | `NotifyRobotClient` 接口 + 钉钉/企微/飞书三实现 | `send` 增加 @ 目标参数，厂商差异收敛在实现内 |
| 总结消息渲染 | `ReviewSummaryContentFactory.build` + `ReviewNotifyMessageRenderer.renderSuccess` | 内容对象携带责任人 @ 列表，渲染追加「责任人」行 |
| 工作台待办卡 | `WorkbenchCard`（type/title/subtitle/count/link/query） | 新增「指派给我」卡片 |
| 动作流水 | `review_issue_action` + `ReviewIssueConstants`（已有 `DETECTED/ROUND_HIT/…`） | 新增 `ASSIGN_AUTO/ASSIGN_TRANSFER` |
| 定时任务种子 | `sql/41_sys_job_seed.sql` 幂等注册模式 | 逾期扫描任务同款注册 |
| 项目通知治理 | `notifyResultPolicy` / `notifyCooldownMinutes` / `notify_enabled` | 逾期提醒仅对 `notify_enabled='Y'` 项目发送；@ 不新建策略开关 |

## 2. 本次范围 / 非范围

### 本次范围

1. 指派解析与自动落库（物化钩子 + 三级降级规则 + 动作流水）；
2. 台账列表/详情的责任人展示、筛选与转派接口；
3. 工作台「指派给我」待办卡；
4. 个人设置「我的 IM 账号」激活（三种渠道身份自助登记）；
5. 审查总结消息 @ 责任人（三渠道 @ 语法适配，降级为纯文本姓名）；
6. 逾期标记：每日任务重算 `overdue_flag`，台账红色标记 + 筛选，项目群每日一条聚合提醒；
7. 脚本 `sql/49_issue_auto_assignment.sql` 与 `init-full.sql` 同步。

### 非范围

见 §0「明确不含」。另：不修改既有状态机（问题状态集合不变）、不新增菜单、不新增权限串。

## 3. 依赖与待决策项

| 项 | 状态 | 说明 |
|---|---|---|
| 钉钉自定义机器人 @ 方式 | 待实测 | 使用 `at.atMobiles`：IM 身份登记**手机号**，且 markdown 正文必须包含 `@手机号` 文本才会渲染 @。开发时用测试群验证 |
| 企微群机器人 @ 方式 | 待实测 | markdown 正文内嵌 `<@userid>`；被 @ 人需为机器人所属企业成员，否则仅显示文本。IM 身份登记**企微 userid** |
| 飞书群机器人 @ 方式 | 待实测 | 正文内嵌 `<at user_id="ou_xxx">姓名</at>`；被 @ 人需在群内。IM 身份登记 **open_id**（与机器人同应用可见范围） |
| 身份关联覆盖率 | 上线前盘点 | 用成员分析「未关联成员」视图统计试点团队关联率；低于 50% 时先推登记再灰度 |
| push 线任务发起人字段 | 开发确认 | 确认 push 任务的 `task.prAuthor` 是否已存推送者 login；未存则取 `review_commit_fact` 提交作者集合，多作者直接进入责任人兜底 |
| 动作展示映射 | 开发确认 | 现有动作类型展示走字典还是前端映射，`ASSIGN_AUTO/ASSIGN_TRANSFER` 按现状同款补充 |

三渠道 @ 的降级原则统一：**取不到责任人的渠道身份时，正文显示纯文本姓名（「责任人：张三」），不阻塞发送，不报错**。

## 4. 数据、接口、权限与流程

### 4.1 数据变更（`sql/49_issue_auto_assignment.sql`，幂等）

```sql
ALTER TABLE review_issue
  ADD COLUMN assignee_user_id bigint      DEFAULT NULL COMMENT '责任人平台用户ID(自动推断或转派)',
  ADD COLUMN assign_source    varchar(20) DEFAULT NULL COMMENT '指派来源(AUTO_COMMIT/AUTO_PR_AUTHOR/AUTO_OWNER/TRANSFER)',
  ADD COLUMN assign_time      datetime    DEFAULT NULL COMMENT '最近指派时间',
  ADD COLUMN overdue_flag     char(1)     NOT NULL DEFAULT 'N' COMMENT '逾期标志(逾期任务每日重算)',
  ADD KEY idx_issue_assignee (project_id, assignee_user_id, status),
  ADD KEY idx_issue_overdue (overdue_flag, status);
```

参数字典（沿用参数管理/字典管理，值随脚本幂等写入）：

- 参数 `review.issue.overdue.highDays` 默认 `3`：CRITICAL/HIGH 严重度逾期天数；
- 参数 `review.issue.overdue.normalDays` 默认 `7`：其余严重度逾期天数；
- 字典 `review_delivery_trigger_source` 增加值 `OVERDUE_REMIND`「逾期提醒」（若触发来源未走字典则按现状补充展示映射）。

不新建表。历史行四个新列保持 NULL/`N`，不回填。

### 4.2 指派解析规则（核心业务规则）

解析器输入：问题（`origin`）、任务（`prAuthor`）、项目（`owner_user_id`）、push 线提交作者邮箱集合。身份匹配统一为：对 `sys_user_identity` 按 `identity_type='GIT_COMMIT'` 且 `identifier` 与输入做 trim+小写后**精确相等**查询，命中返回 `user_id`；不做模糊匹配。

```text
NEW 问题：
  1. push 线且提交作者集合恰好 1 人且身份匹配命中 → 指派，来源 AUTO_COMMIT
  2. task.prAuthor 身份匹配命中                    → 指派，来源 AUTO_PR_AUTHOR
  3. project.owner_user_id                        → 指派，来源 AUTO_OWNER
EXISTING 问题：
  1. task.prAuthor 身份匹配命中                    → 指派，来源 AUTO_PR_AUTHOR
  2. 否则不指派（责任人留空，可见为「未指派」）
```

要点：

- **指派只发生在新物化或责任人仍为空时**；既有问题被后续轮次再次命中不改责任人（幂等）；
- EXISTING 不兜底到项目负责人——避免首次大批量审查把历史存量全部压给负责人；
- 每次自动指派写动作流水 `ASSIGN_AUTO`，operator=`system`，note 记录依据（如「按提交邮箱 dev@corp.cn 自动指派给张三」「PR 发起人匹配失败，按项目负责人兜底指派」）。

### 4.3 转派（唯一人工动作）

`PUT /review/issue/{issueId}/transfer`，请求体 `{ "assigneeUserId": 123, "note": "该模块已移交李四负责" }`。

- 权限：当前责任人本人，**或**持有 `review:issue:close` 且对该项目有访问权限的用户（与复核重开复用同款权限先例，不新增权限串）；
- 目标用户必须是该项目有效成员（OWNER/ADMIN/REVIEWER），防止转给无关人员；
- 仅活跃态问题（`isActive`）可转派；`note` 必填且 ≤500 字；
- 动作流水 `ASSIGN_TRANSFER`（含 from→to 与原因），`assign_source` 更新为 `TRANSFER`；
- 转派不单独发 IM 消息（避免噪声；下一轮审查总结消息自然 @ 新责任人）。

### 4.4 列表与详情

- 列表 SQL 左联 `sys_user` 输出 `assigneeName`；新增查询参数 `assignFilter`：`MINE`（责任人=当前用户）/ `UNASSIGNED`（责任人为空且活跃态）/ 不传（全部）；
- `overdue_flag='Y'` 在列表以红色「逾期」标签展示，支持 `overdueFlag=Y` 筛选；
- 详情展示责任人、指派来源（中文：按提交作者/按 PR 发起人/按项目负责人/人工转派）、指派时间；动作时间线展示 `ASSIGN_AUTO/ASSIGN_TRANSFER`；
- 前端命名使用「责任人」「转派」「逾期」，不暴露 assignee/assign 等英文字面。

### 4.5 工作台「指派给我」卡片

- 类型 `ASSIGNED_TO_ME`，count = `assignee_user_id=当前用户` 的活跃问题数（与现有卡片同款 DataScope × 项目成员交集口径）；
- `link` 指向问题台账，`query={assignFilter:'MINE'}`，列表页按既有 query 回填能力落筛选；
- 登录即可见（计数只含本人，无新权限串）；位置紧随「待修复问题」卡。

### 4.6 IM 身份激活（个人设置）

- 新增 `GET/POST/DELETE /system/userprofile/imIdentities`（登录即可，模式与 `/system/userprofile/identities` 完全一致）：`identity_type` 限 `IM_WECOM/IM_DINGTALK/IM_FEISHU`，`identifier` ≤320 字符，全局唯一冲突（`uk_identity`）转人话提示；
- 前端个人设置在「我的提交邮箱」旁新增「我的 IM 账号」区块，三行固定渠道，占位文案说明标识用途：钉钉=手机号（用于群内 @）、企微=企业微信 userid、飞书=open_id；
- IM 身份仅用于 @ 渲染，**不参与**提交归属匹配与洞察聚合。

### 4.7 总结消息 @ 责任人

- `ReviewSummaryContentFactory.build` 增补：收集本次任务物化问题（含既有命中问题）的责任人去重集合，解析姓名 + 与当前渠道类型匹配的 IM 身份，写入 `ReviewSummaryContent` 的责任人列表；
- `ReviewNotifyMessageRenderer.renderSuccess` 在正文追加一行：`责任人：@13800000000 @李四`（有渠道身份者按渠道 @ 语法，无身份者纯文本姓名；最多 5 人，超出追加「…等 N 人」）；责任人集合为空则不渲染该行；
- `NotifyRobotClient.send` 签名扩展为 `send(webhookUrl, secret, title, body, List<String> atIds)`（三实现与测试同步修改，不保留旧签名）：
  - 钉钉：请求体加 `at: { atMobiles: [...], isAtAll: false }`，`atIds` 为手机号，正文已含 `@手机号` 文本；
  - 企微：正文尾部已由渲染器内嵌 `<@userid>`，`atIds` 仅用于日志与测试断言；
  - 飞书：正文已由渲染器内嵌 `<at user_id="…">姓名</at>`，`atIds` 同上；
- 失败通知（renderFailed）不带责任人段；
- @ 渲染失败（身份解析异常）静默降级为不带 @ 的原消息，不影响投递主流程。

### 4.8 逾期标记与聚合提醒

- `issueOverdueJobTask.scan`（Quartz 触发，业务在 `acr-review`），每日 08:30，`sys_job` 随 `sql/49` 幂等注册（仿 `sql/41`）；
- 扫描全部活跃问题：`stage_entered_time` 早于 `当前时间 - 严重度对应天数` 则置 `overdue_flag='Y'`，否则置 `N`（状态流转后自然恢复）；
- 本次扫描**新转为逾期**的问题按项目分组：项目 `notify_enabled='Y'` 且渠道可解析时，入队一条聚合提醒意图——
  - 幂等键 `overdue-remind:{projectId}:{yyyyMMdd}`（当日重跑不重复发）；
  - 触发来源 `OVERDUE_REMIND`；
  - 渲染：标题「AI Code Review · 逾期问题提醒」，正文列逾期问题（严重度降序，最多 5 条：标题 + 责任人姓名）+「共 N 个逾期问题，请前往问题台账处理」，@ 段复用 §4.7 机制（@ 逾期问题责任人）；
  - 不受结论策略与冷却门槛约束（逾期提醒本身即频控：每项目每日至多一条），但渠道不可用时按现有 `MANUAL` 语义留痕不强发；
- 项目未启用通知时仅更新标志位，不发任何消息。

## 5. 分步实现计划

| 步 | 内容 | 验证 |
|---|---|---|
| 1 | `sql/49`：表列、索引、参数、字典、job 种子；`init-full.sql` 同步 | 临时空库执行两遍均成功（幂等）；`init-full` 全量装载通过 |
| 2 | 指派解析器 + 物化钩子：`ReviewIssue` 新字段、Mapper 读写、`reconcileAfterSuccess` 接入、`ASSIGN_AUTO` 流水 | 单测覆盖：NEW 三级降级各路径、EXISTING 两路径、多作者不指派、身份未命中、重复命中不改责任人、流水依据文本 |
| 3 | 转派接口 + 权限校验 + `ASSIGN_TRANSFER` 流水 | 单测：本人/关闭权限者通过、无关用户拒绝、目标非项目成员拒绝、终态拒绝、原因必填；接口返回含新责任人 |
| 4 | 列表/详情/工作台卡：`assignFilter`、`assigneeName`、逾期标签、「指派给我」卡片 + 前端台账列/筛选/详情/转派交互、工作台卡 | 后端单测口径；前端走查：筛选回填、逾期标红、转派弹窗（目标成员选择器 + 必填原因）、卡片跳转带筛选 |
| 5 | IM 身份：`/system/userprofile/imIdentities` 三接口 + 校验 + 个人设置区块 | 单测：类型白名单、全局唯一冲突人话提示、仅可删本人；前端三渠道占位文案 |
| 6 | @ 路由：`send` 签名扩展、三客户端 @ 载荷、内容工厂责任人收集、渲染器责任人段 | 单测：钉钉载荷含 `at.atMobiles`、企微正文含 `<@userid>`、飞书正文含 `<at>`、无身份降级纯文本、超 5 人截断、异常降级不抛出 |
| 7 | 逾期任务：标志位重算、聚合提醒入队与渲染 | 单测：阈值按严重度、N→Y 当日幂等键不重发、项目未启用通知不发消息、状态流转后标志复位；`mvn test` 全量 + `npm run build:prod` |

步 2–4 可先于步 5–7 交付验收（指派与可见性先行，@ 与逾期随后），但同属一个切片，不拆版本。

## 6. 验收标准

1. 新物化 NEW 问题按 §4.2 规则自动落责任人，动作流水含依据；重复审查命中不改责任人；
2. 存量问题（历史行）责任人保持空，列表显示「未指派」，可筛选；
3. 责任人或关闭权限者可转派给项目有效成员，流水完整，转派后下一轮总结消息 @ 新责任人；
4. 三渠道总结消息在责任人登记对应 IM 身份时真实 @（至少钉钉实测一次），未登记时纯文本姓名不报错；
5. 逾期任务运行后：到期问题标红可筛，项目群收到一条聚合提醒且当日不重复；未启用通知的项目无任何外发；
6. 工作台「指派给我」计数与台账 `assignFilter=MINE` 结果一致；
7. 后端全量测试与前端生产构建通过；脚本幂等；无新依赖、无新菜单、无新权限串；
8. `CHANGELOG` 与路线图 §7.4 进度注记同步（含「问题指派闭环由此落地，人工指派入口明确不做」）。

## 7. 风险与对策

| 风险 | 影响 | 对策 |
|---|---|---|
| 三渠道 @ 生效条件（在群/在企业/手机号）不满足 | @ 不渲染，触达打折 | 降级纯文本姓名；上线说明中注明登记要求；实测记录写入验收 |
| 身份关联覆盖不足 | 自动指派率低，退回「未指派」 | 上线前盘点关联率；未指派可筛可见，不静默 |
| 误指派（squash/结对/格式化提交） | 责任人异议 | 转派阀自助纠正；动作留痕 |
| 逾期提醒与冷却语义不一致 | 口径疑问 | 文档与字典注明：逾期提醒按「每项目每日一条」自治频控，不走项目冷却 |
| 负责人兜底造成负责人积压 | 负责人待办膨胀 | 仅 NEW 兜底；工作台卡与台账筛选暴露分布，后续按数据决定是否调整规则 |

## 附：与既有决策的一致性说明

- 与路线图 §4.3「忽略高风险…二次确认并填写原因」一致：转派必填原因，但不二次确认（非高风险动作）；
- 与成员分析合规边界一致：指派与 @ 不构成个人绩效数据，仅为问题责任归属；
- 与 `production-readiness-governance.md` 一致：本切片不声称企业版能力，逾期提醒不替代业务告警。
