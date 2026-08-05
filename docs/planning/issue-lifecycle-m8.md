# M8 问题生命周期管理 — 设计文档

- 阶段：核心版（V0.2）首个切片
- 日期：2026-08-05
- 状态：待评审
- 基线：main ebd9a55（sql 01–30）
- 回归原型：webhook-test PR#3 两轮联调（task#6 查出 SQL 注入/命令注入/硬编码密码 → 修复提交 → task#7 复审未命中原问题、新发现修复代码 3 个质量问题）

## 1. 背景与问题

M6 问题台账是「单轮物化 + PR 级指纹去重」的扁平模型，多轮审查下存在四个结构性缺口（2026-08-04 联调实证）：

1. **无轮次概念**：PR#3 两轮审查产生 6 条记录，语义上仅 3 个活跃问题，清单膨胀、统计失真；
2. **无修复信号**：漏洞修复后复审 27→78 分，台账对「问题可能已修复」零表达，产品核心叙事「发现→修复→确认」断在最后一环；
3. **指纹绑定模型措辞**：指纹 = hash(category + filePath + 归一化 title)，title 措辞漂移即产生新指纹，旧问题被误判「未命中」；
4. **只物化 Top3**：被挤出 Top3 的问题不进台账，整改闭环只覆盖最重要的 3 条，其余问题无人追踪。

M6 已预留的锚点（本期全部启用）：`RECHECKING` 状态字典、`close_source=auto_recheck` 字典、`review_issue.first/last_task_id`、`review_issue_action` 动作流水。

## 2. 范围

### 2.1 本期范围

| # | 工作流 | 说明 |
|---|---|---|
| W1 | 协议 v1.2（全量问题清单） | 模型输出全部问题（上限 20），Top3 降级为展示层概念；解析兼容 1.0/1.1/1.2 |
| W2 | 物化扩围 | 全部 NEW 问题入账（不再限 Top3）；EXISTING 维持现状（仅 `scope_report_existing=Y` 时随清单输出） |
| W3 | 轮次对账与自动复核 | 每轮 SUCCESS 后做指纹命中/未命中对账；连续未命中达阈值自动转 RECHECKING（附证据），**不自动关闭**；复核中问题被再次命中自动复活 |
| W4 | 状态机开放 | RECHECKING 启用：人工「确认已修复→关闭」（close_source=auto_recheck）与「未修复→重新打开」 |
| W5 | 活跃视图与台账体验 | 列表默认「当前活跃」视图；详情抽屉增加生命周期时间线与复核证据块 |
| W6 | 工作台与通知联动 | 新增「待复核问题」卡片；本轮触发复核时，总结评论与 IM 迷你报告追加「疑似已修复」段 |

### 2.2 明确非范围（本期不做）

- 批量处置、严重级别差异化默认策略、「建议已修复」一键确认 → M8.1（数据模型已按可支撑设计，见 §10）；
- 同族问题启发式合并展示（跨指纹按文件+严重度聚类）：本期以 §5.3 的「单义族合并」保证行级连续性，替代大部分价值；真实措辞漂移率积累后再评估独立聚类展示；
- OCR 路径问题物化与对账：OCR 引擎输出未纳入统一问题协议，本期 OCR 轮次**不参与对账**（既不算命中也不算未命中），行为与现状一致；
- PR 合并/关闭后自动关闭存量问题；跨 PR 问题关联；问题指派/转办/逾期提醒（核心版后续切片）。

## 3. 协议 v1.2（W1）

### 3.1 变更点

协议附录由代码注入（`ReviewScoringConstants.protocolAppendix()`），**不需要改 review_template 种子数据**。

| 项 | v1.1 | v1.2 |
|---|---|---|
| 问题输出 | 「仅输出最重要的最多 3 个」 | 「输出发现的全部问题，按影响程度从高到低排序，最多 20 条」 |
| `topIssues` 字段语义 | ≤3 条 | 全量清单（≤20 条），仍按影响度降序 |
| `focusIssueCount` | 模型自报（0–3） | 模型仍可自报，**后端重算**：NEW 问题中 CRITICAL/HIGH 的数量（不封顶） |
| 新增上限常量 | `MAX_TOP_ISSUES=3` | 增加 `MAX_ISSUES=20`（解析截断，超出记日志并在截断标记落盘） |
| 兼容版本 | {1.0, 1.1} | {1.0, 1.1, 1.2} |

### 3.2 解析与下游口径

- `ReviewScoreResult.topIssues` 承载全量清单；**展示层**（总结评论 Top3 区块、IM 迷你报告审查结果区块、记录详情重点问题）一律取清单前 3 条渲染，超出以「共 N 个问题」提示并可跳转台账（按该 PR 过滤）；
- 结论判定（`resolveConclusion`）逻辑不变，遍历对象由「≤3 条」变为「全量清单」，语义更准（任何 NEW CRITICAL/HIGH 都触发 WARN/BLOCK 判定）；origin 打标、排序、范围统计逻辑作用于全量清单（打标仍在排序后、展示截断前执行）；
- `focusIssueCount` 后端重算后写回 run/task，历史 v1.0/1.1 结果保留模型自报值，不回填；
- v1.0/1.1 历史 run 的 `topIssuesJson` 不变、展示不变、物化行为不变（历史轮次不做对账回溯）。

### 3.3 OCR 路径

OCR 结构化结果不含统一协议问题清单，`persistSuccess` 路径不产出可物化问题 —— 与现状一致。OCR 轮次跳过 W3 对账（判定条件见 §5.1）。

## 4. 数据模型（W2/W3）

### 4.1 review_issue 新增列（sql/31）

| 列 | 类型 | 说明 |
|---|---|---|
| `family_key` | varchar(80) NULL | 族键 = SHA-256(filePath + "\0" + category)，同 §6 指纹算法风格；用于 §5.3 单义族合并。**存量行不 SQL 回填**，首次参与对账时由 Java 计算并回写，保证算法单一来源 |
| `missed_streak` | int NOT NULL DEFAULT 0 | 连续未命中轮数（命中即清零） |
| `last_seen_head_sha` | varchar(64) NULL | 最近一次命中轮的 head commit；同 commit 重跑不计未命中（§5.4） |
| `last_missed_run_id` | bigint NULL | 最近一次计未命中的 run；**对账幂等键**，同一 run 重复对账不重复计数 |
| `recheck_task_id` | bigint NULL | 触发转 RECHECKING 的轮次任务（复核证据） |
| `recheck_run_id` | bigint NULL | 同上，run 级 |
| `recheck_commit_sha` | varchar(64) NULL | 未命中轮的 head commit（「这个 commit 的审查没有再发现它」） |

索引：`KEY idx_issue_pr_family (project_id, pr_number, family_key)`。

既有列语义保持不变：`last_task_id/last_run_id` = 最近命中（物化）轮次；`close_source` 取值 manual / auto_recheck（字典去「预留」标注）。

### 4.2 不新建表

轮次命中/未命中事实通过三条途径承载，不新建 review_issue_round 表：
1. `review_issue_action` 流水（AUTO_RECHECK / AUTO_REOPEN 动作含证据快照）；
2. review_issue 上的对账状态列（§4.1）；
3. run 与 task 的既有 head SHA / finishedTime。

后续若需逐轮命中明细分析，可无损追加明细表，不影响本期状态机与接口。

### 4.3 参数（sys_config，运行期可调）

| key | 默认 | 说明 |
|---|---|---|
| `review.issue.recheck.missedRoundsThreshold` | 1 | 连续未命中 N 轮转 RECHECKING。人工确认是关闭闸门，误转成本低（一键重开），漏转成本高（修复无信号），故默认 1 |
| `review.protocol.maxIssues` | 20 | 单轮问题清单上限，解析截断 |

### 4.4 字典与常量

- `review_issue_status.RECHECKING`：字典备注去「预留，本期不实现」，label 由「复核中」改为「待复核」（list_class 保持 primary）；
- `review_issue_close_source.auto_recheck`：去「预留」备注；
- 新增动作类型常量：`ACTION_AUTO_RECHECK`、`ACTION_AUTO_REOPEN`（写入 review_issue_action.action_type，operator=system）；
- `ReviewIssueConstants` 移除 RECHECKING 的 API 拒绝逻辑。

## 5. 轮次对账（W3，核心逻辑）

### 5.1 触发条件

仅当同时满足时执行对账：
- 任务 SUCCESS 且 `parseStatus=SUCCESS`；
- 执行方式为 `LLM_DIRECT`（OCR 轮次跳过，见 §3.3）。

挂载点：**将问题物化/对账从 `deliverQuietly` 的第三步（评论→通知→物化）调整为第一步（对账→评论→通知）**。理由：总结评论与 IM 通知需要反映对账后的最新台账状态（新物化问题的处置态、本轮「疑似已修复」段）。对账失败沿用现有隔离策略（try/catch 吞异常 + warn 日志，不影响任务状态），失败时评论/通知退回不含复核段的渲染，行为等价现状。

### 5.2 对账输入

- `activeIssues`：该 PR 全部非终态问题（AWAITING_CONFIRM / AWAITING_FIX / RECHECKING）；
- `roundIssues`：本轮全量清单（协议 v1.2；v1.0/1.1 run 即其 topIssues ≤3 条）。

### 5.3 对账算法（按序执行）

```text
Pass 1 精确命中：roundIssues 按指纹查既有行（含终态）。
  - 命中非终态行 → 更新快照（severity/title/行号/描述等，沿用 applySnapshot），
    missed_streak=0，last_seen_head_sha=本轮 headSha；
    若该行原为 RECHECKING → 自动复活 AWAITING_FIX（ACTION_AUTO_REOPEN，operator=system，
    备注「第 N 轮审查再次命中，修复未生效」）；
  - 命中终态行 → 跳过（人工判定权威，不复活、不更新）。
Pass 2 单义族合并（措辞漂移保护）：对 Pass 1 未消费的 roundIssue，
  计算 family_key；若同族中「未被命中的活跃问题」恰好 1 条 → 合并进该行：
  更新快照并把该行 fingerprint 更新为本条新指纹（行身份跟随问题演化），
  missed_streak=0，last_seen_head_sha=本轮 headSha；同族 ≥2 条或 0 条 → 不合并。
Pass 3 新物化：剩余未消费 roundIssue 按指纹插入新行（AWAITING_CONFIRM），
  family_key 同批计算落库；同批指纹碰撞沿用 withBatchSuffix 规则。
  物化范围：全量清单（NEW 全部入账；EXISTING 仅在清单中即 reportExisting=Y 时出现）。
Pass 4 未命中判定：activeIssues 中本轮未命中（Pass 1/2 均未覆盖）的行：
  - 若本轮 headSha == 该行 last_seen_head_sha → 跳过（同 commit 重跑/重试，无新信息）；
  - 若 last_missed_run_id == 本轮 runId → 跳过（对账幂等，防重复计数）；
  - 否则 missed_streak+1，last_missed_run_id=本轮 runId；
  - missed_streak >= 阈值（sys_config）→ 转 RECHECKING：
    写 ACTION_AUTO_RECHECK 流水（operator=system，resolve_note 记「第 N 轮审查（commit 短SHA）未再命中」），
    填 recheck_task_id/recheck_run_id/recheck_commit_sha。
```

轮次编号 N = 该 PR 的 SUCCESS run 序号（可用 task 建单序或 run 数，文案用，不要求严格）。

### 5.4 正确性边界

- **只自动转复核，不自动关闭**：LLM 输出不确定，关闭必须人工确认（路线图 §3.2 保守原则）；
- 同 commit 重跑不计未命中（防人工重试刷出假复核）；
- 对账幂等（last_missed_run_id + 唯一键 uk_issue_pr_fingerprint）；
- 终态不复活；RECHECKING 再命中自动复活（修复未生效是事实信号）；
- FAILED 轮次不参与对账（无结论即无命中/未命中语义）。

## 6. 状态机（W4）

```text
AWAITING_CONFIRM --CONFIRM(人工)--> AWAITING_FIX
{AWAITING_CONFIRM, AWAITING_FIX} --AUTO_RECHECK(系统, 连续未命中)--> RECHECKING
RECHECKING --AUTO_REOPEN(系统, 再次命中)--> AWAITING_FIX
RECHECKING --CLOSE(人工确认已修复)--> CLOSED [close_source=auto_recheck]
{AWAITING_CONFIRM, AWAITING_FIX, RECHECKING} --CLOSE(人工)--> CLOSED [close_source=manual]
{AWAITING_CONFIRM, AWAITING_FIX, RECHECKING} --DISMISS(人工, 原因必填)--> IGNORED / FALSE_POSITIVE
RECHECKING --REOPEN(人工「未修复」)--> AWAITING_FIX
终态（CLOSED/IGNORED/FALSE_POSITIVE）不可转出。
```

接口变化：
- `close()` 放开 RECHECKING 前置态；由 RECHECKING 关闭时 close_source=auto_recheck，其余 manual；
- 新增 `PUT /review/issue/{issueId}/reopen`（权限复用 `review:issue:close`，同属处置权）：仅 RECHECKING 可调用，转 AWAITING_FIX，写 REOPEN 流水（operator=当前用户）；
- 移除 `transition()` 中「复核中状态本期未开放」拦截；confirm/dismiss 对 RECHECKING 保持拒绝（语义不通）；
- 处置后评论重渲染（`rerenderSummaryComment`）对所有新开放路径生效，失败隔离策略不变。

## 7. 展示与交互（W5/W6，国内企业台账习惯）

### 7.1 台账列表

- 新增视图切换「当前活跃 / 全部」，**默认当前活跃**（status IN AWAITING_CONFIRM, AWAITING_FIX, RECHECKING）；后端 `selectIssueList/countIssueList` 增加 `activeFlag` 查询参数，前端视图切换映射为该参数，与既有 status 下拉筛选并存（选了具体 status 时 status 优先）；
- 状态列经字典渲染「待复核」；路由 query 回填沿用（status=RECHECKING 直达）；
- 行操作对 RECHECKING 增加「复核」入口（打开详情抽屉）。

### 7.2 详情抽屉

- **生命周期时间线**：合并 action 流水（CONFIRM/CLOSE/DISMISS/AUTO_RECHECK/AUTO_REOPEN/REOPEN，含 from→to 状态与备注）按时间渲染；
- **复核证据块**（RECHECKING 显示）：未命中轮次任务链接（task_detail）、commit 短 SHA、连续未命中轮数、以及「疑似关联新发现问题」——本轮新物化、与该行同文件同严重度的问题链接（展示层查询，不落库）；
- RECHECKING 操作区：「确认已修复并关闭」（可填关闭说明）/「未修复，重新打开」。

### 7.3 记录详情 / 任务详情

- `topIssuesJson` 升级为全量清单后，重点问题区块取前 3 渲染；>3 时追加「共 N 个问题，其余见问题台账」（跳转台账并按该 PR 过滤）。

### 7.4 工作台

- 新增卡片 `CARD_ISSUE_RECHECKING`「待复核问题」（subtitle「修复待验证」），计数 = RECHECKING 且与台账列表同口径 DataScope；点击跳 `/review/issue?status=RECHECKING`；无 `review:issue:list` 权限不下发（同既有卡片策略）；
- `countOpenNewByProject`（项目维度未关闭新增问题数）口径加入 RECHECKING（复核确认前仍是未关闭问题）。

### 7.5 总结评论与 IM 通知

- 处置徽标映射补「待复核」（enricher 已按指纹挂状态，渲染端补 label）；
- 对账结果以 `ReviewRoundReconcileResult`（新增物化数、转复核清单、复活清单）传递给评论/通知装配：本轮存在转复核问题时，总结评论与 IM 迷你报告在审查结果区块后追加一段：
  `疑似已修复（N）：{title1} / {title2} / {title3}…（超过 3 条省略）— 请前往问题台账复核`；
  该段渲染失败或装配异常时静默降级（不阻塞评论/通知主体）。

## 8. 权限与安全

- **无新权限串、无新菜单**：列表/详情复用 `review:issue:list/query`，确认/关闭/忽略/复核确认复用 `review:issue:confirm/close`，重开复用 `review:issue:close`；
- 系统动作（AUTO_RECHECK/AUTO_REOPEN）operator=system，不经权限校验（内部链路，同现有物化）；
- 单条操作继续 `deptService.checkDeptDataScope(project.getDeptId())`；列表 DataScope 不变；
- 无新外部副作用；评论重渲染走既有投递记录与幂等键；
- 问题正文（description/evidence/suggestion）仍按企业代码资产处理，不出新增外发面。

## 9. 幂等、失败与兼容

| 场景 | 行为 |
|---|---|
| 对账整体异常 | 吞异常 + warn，任务状态与投递不受影响（评论/通知退化为不含复核段） |
| 同 run 重复对账 | last_missed_run_id 幂等；唯一键防重复插入 |
| 同 commit 人工重试产生新 run | headSha 守卫，不计未命中 |
| 历史问题（family_key=NULL） | 首次对账时 Java 计算回写，无 SQL 回填 |
| 历史 v1.0/1.1 run | 展示/物化行为不变；其物化出的问题正常参与后续轮次对账 |
| RECHECKING 积压无人复核 | 允许（状态可长期停留）；工作台卡片持续提示；不做超时升级（后续切片评估） |

## 10. 为 M8.1 预留（不返工检查点）

- 批量处置：本期状态机与动作流水均为逐条语义，批量接口是纯增量（同事务循环 + 统一原因），不动数据模型；
- 严重级别差异化策略（LOW 默认忽略仅通知）：落点为物化前的策略钩子，severity 已在行上，不动结构；
- 「建议已修复」一键确认：等价于本期「确认已修复并关闭」，交互增强不动后端；
- 同族聚类展示：family_key 已落库，展示层增量。

## 11. 验收

### 11.1 自动化（必须）

1. 协议 v1.2 解析：全量清单截断（21→20）、focusIssueCount 后端重算、1.0/1.1/1.2 三版兼容、结论判定在全量清单下不退化；
2. 对账六场景：精确命中清零 / 单义族合并（含指纹跟随更新）/ 多义族不合并 / 未命中达阈值转 RECHECKING 且证据齐全 / 同 commit 重跑不计未命中 / 同 run 重复对账不重复计数；
3. RECHECKING 三出口：人工确认关闭（close_source=auto_recheck）/ 人工重开 / 再命中自动复活；终态不复活；
4. **PR#3 回归原型**：round1 物化 3 条（AWAITING_CONFIRM）→ 人工确认 → round2 全未命中且 headSha 变化 → 3 条转 RECHECKING + 新发现 3 条 AWAITING_CONFIRM → round3 命中其中 1 条 → 该条复活 AWAITING_FIX，其余保持 RECHECKING；
5. 工作台卡片计数与权限（无 review:issue:list 不下发）；评论/通知「疑似已修复」段渲染与降级。

### 11.2 手工联调（本地资产恢复后）

webhook-test 新分支复刻 PR#3 剧本：植入漏洞 → 轮 1 BLOCK → 修复提交 → 轮 2 观察（台账自动转待复核、IM/评论出现「疑似已修复」、工作台卡片 +1）→ 确认关闭 → 活跃视图收敛、时间线完整。顺带验收 IM 三渠道迷你报告观感。

### 11.3 交付物

- `sql/31_issue_lifecycle_m8.sql`（幂等，含 ALTER/字典 UPDATE/sys_config INSERT）+ init-full.sql 重新生成与空库验证（**验证必须先替换脚本内库名**）；
- CHANGELOG Unreleased 更新；路线图 §7.4 进度注记；
- 后端测试与前端生产构建通过（验证命令见 delivery 规则）。

## 12. 决策记录（相对初稿的修正）

1. **放弃独立 family_key 复核匹配层**：初稿设想「指纹管去重、family_key 管跨轮复核匹配」。核码后修正：指纹本身不含行号（category+filePath+title），已足够稳定；family_key 收窄为「单义族合并」一个用途（1:1 时行身份跟随措辞演化），未命中判定按行（指纹）计。避免双键匹配带来的启发式歧义；
2. **不新建 review_issue_round 明细表**：对账事实由动作流水 + 对账状态列承载，阈值与幂等不依赖明细表，后续可无损追加；
3. **对账前置于投递**：调整 deliverQuietly 顺序，使评论/通知携带本轮生命周期信号。
