# M6 问题台账基础闭环

> **状态（2026-08-03）：已实现，待真实环境验收。** 前置：`docs/planning/product-roadmap.md`（§3.2 问题对象、§7.3 MVP）、`docs/planning/review-comment-writeback-m4.md`、`docs/planning/review-scoring-result-protocol.md`（v1.1 Top3 / `origin`）、`docs/planning/notification-management-m5.md`（文档结构参照）、`docs/planning/review-pipeline-m3.md`、`docs/planning/review-scope-policy-m3.2.md`。本设计明确审查 SUCCESS 后将 Top3 物化为可处置问题、人工确认/关闭/忽略/误报闭环、动作流水审计，以及状态变化触发的 M4 持久总结评论重渲染；**不**做指派、转办、申诉、逾期、自动关闭、工作台与工单对接。
>
> **v1.1 修订记录（2026-08-03，review 后定稿）**：① 状态机对齐路线图 §3.2——`IGNORED`/`FALSE_POSITIVE` 恢复为独立终态（v1.0 曾并入 `close_reason`，与路线图"已忽略和误报作为有原因、有权限、有审计的终态"不符，已纠正）；② 问题身份由任务级幂等键 `{taskId}:{rank}` 改为 **PR 级指纹去重**（`{projectId}:{prNumber}:fingerprint` 唯一），对齐路线图"首次发现于任务，可在后续任务中复核"，避免同一 PR 多次提交产生重复问题；③ `review_issue_action`、接口、评论渲染装配、测试要点同步调整。

## 1. 目标与成功指标

- 审查 SUCCESS 后，Top 3 重点问题从 `review_task_run.top_issues_json` **物化**为 `review_issue`，可在后台确认、关闭、忽略、标记误报，形成「发现 → 确认 → 处置」最小闭环；
- 开发者日常动线仍在 **PR 与 IM**；平台负责配置、问题处置与审计证据；PR 总结评论在问题状态变化后同步展示处置状态（含忽略/误报原因）；
- **同一 PR 不重复建问题**：指纹去重保证同一问题跨任务只有一行；同一任务重试物化同样幂等；
- `CLOSED` / `IGNORED` / `FALSE_POSITIVE` 为三个终态，拒绝任何再转移；忽略/误报原因**必填**，关闭说明**选填**；
- 评论重渲染失败**不得**回滚已落库的处置动作；
- `origin=EXISTING` 存量问题可入账展示，但**明确不进**后续催办 / 未关闭统计口径；
- 审查中心新增「问题台账」子菜单；任务/记录详情 Top3 与台账联动。

## 2. 当前基线与缺口（2026-08-03 核查）

| 链路 | 现状 | 缺口 |
|---|---|---|
| 结构化结果 | 协议 v1.1：`topIssues` 含 severity/category/title/description/filePath/startLine/endLine/evidence/suggestion/origin；落库 `review_task_run.top_issues_json`；FAILED / `RESULT_FORMAT_INVALID` 不产生可用 Top3 | 无独立问题实体；无法跨任务处置与审计 |
| 审查执行 | SUCCESS 后挂钩 M4 GitHub 总结评论 + M5 IM；FAILED 不发评论 | SUCCESS 后无问题物化钩子 |
| PR 评论 | `ReviewSummaryContent` + `ReviewCommentBodyRenderer`；标记 `<!-- acr-review-summary -->`；PR 级幂等 upsert；Top3 仅展示严重度/归属/标题/定位 | 评论不感知平台侧处置状态；无「处置后重渲染」路径 |
| 前端/菜单 | 审查任务(125)、审查记录(128)；详情有 Top3 卡片，无问题跳转 | 无「问题台账」菜单与处置页 |
| 权限/审计 | 任务/记录/投递权限；通用操作日志 | 无 `review:issue:*`；无问题动作流水 |

可复用存量资产：`ReviewTopIssue` 字段全集（零协议改动）、M4 marker 幂等与 `ReviewSummaryContent` digest 模型、`review_delivery_record` 失败隔离范式、项目级 `@DataScope` + `deptService.checkDeptDataScope`、字典/菜单幂等 SQL（`sql/24_*` 风格）。M6 在 SUCCESS 落库之后叠加物化，在处置事务提交之后叠加评论重渲染，**不**改写 M1–M5 审查结论与 IM 主行为（评论正文允许扩展处置行，标记与 PR 级幂等键不变）。

## 3. 本次范围 / 非范围

### 3.1 范围

1. 新建 `review_issue`：任务 `task_status=SUCCESS` 时物化 `top_issues_json`（协议 v1.1 既有字段，**零协议改动**）；`FAILED` **不**物化；
2. **问题指纹与 PR 级去重**：`fingerprint = hash(category + filePath + normalize(title))`，唯一键 `{projectId}:{prNumber}:fingerprint`；同一 PR 新任务再次发现同一问题 → 刷新快照不重复建行；同一任务重试同样幂等（见 §5.2）；
3. 状态机：`AWAITING_CONFIRM → AWAITING_FIX → CLOSED`；`IGNORED` / `FALSE_POSITIVE` 为**独立终态**，原因必填；关闭说明选填；`RECHECKING` 枚举预留不实现流转（对齐路线图 §3.2）；
4. 预留仅两处：`RECHECKING` 状态值 + `close_source`（`manual` / `auto_recheck`）；**其余字段一律不预留**（含 assignee）。`fingerprint` 为本期实现字段，不属于预留；
5. 新建 `review_issue_action` 动作流水（操作者 / 动作 / 原→新状态 / 说明快照 / 时间），一块实体；
6. 问题每次状态变化 → 触发该 PR 的 M4 持久总结评论重渲染 upsert；失败隔离 + 降级/补偿；PR 尚无成功评论时的兜底；
7. 存量（`origin=EXISTING`）入账但排除催办/未关闭统计口径；
8. 前端：审查中心「问题台账」；筛选与详情；任务/记录 Top3 联动；
9. 权限 `review:issue:*`；SQL `sql/25_issue_ledger_m6.sql`（幂等风格对齐 `sql/24`）；单测覆盖**指纹去重（含跨任务）**、状态机边界、评论失败隔离、数据权限；`mvn test` + `npm run build:prod`；CHANGELOG 与路线图 §7 同步补 M6。

### 3.2 非范围

- 指派、转办、申诉、逾期提醒、自动关闭、自动重审关闭（`RECHECKING` / `close_source=auto_recheck` 仅枚举预留）；
- **定向复核（新提交审查时携带存量问题逐一验证并更新状态）、问题手工合并、指纹算法调优** → 核心版，本期不做；
- 首页工作台、OCR 专项问题输出改造、工单（Jira/禅道等）对接；
- 为问题加 `assignee` 或其它未使用预留列；
- 改写评分协议 v1.1、M4「一 PR 一条总结评论」幂等键/标记、M5 IM 渠道与策略；
- inline 评论、Status Check、第二 Git Provider、新 Maven 模块/框架。

## 4. 依赖与待决策项（设计定稿）

| 项 | 定稿 |
|---|---|
| 产品定位 | 开发者日常在 PR/IM；平台做配置与处置；不做通用缺陷/工单系统 |
| 物化触发 | 仅 `task_status=SUCCESS` 且 `top_issues_json` 可解析为列表；空数组 → 不建行；`FAILED` 不物化 |
| 协议 | **零改动**；字段直接映射 `ReviewTopIssue` |
| OCR | 不单独建设 OCR 问题输出；若某 SUCCESS run 的 `top_issues_json` 非空则同样物化，空则无问题 |
| 问题身份 | `fingerprint = hash(category + filePath + normalize(title))`；**不含行号**（随提交漂移）、不含 description/evidence/suggestion（措辞不稳）；null 分量按空串参与；`normalize` = trim + 空白折叠 + 小写 |
| 去重范围 | PR 级：`UNIQUE(project_id, pr_number, fingerprint)`；跨 PR 的同一问题各自独立 |
| 指纹碰撞 | 同 PR 同文件、同类别、同标题的两条不同问题：同批物化内第二条起追加批次序号后缀；跨任务匹配因此类后缀失配时按「未命中 → 新建」降级（碰撞罕见，接受；核心版调优） |
| 重试语义 | 新任务物化先按指纹匹配：命中且未终态 → 刷新快照 + 更新 `last_task_id`/`last_run_id`，**不改状态**；命中且已终态 → 整行不动；未命中 → 新建并记首次发现 |
| 状态机 | 见 §5.3；`CLOSED`/`IGNORED`/`FALSE_POSITIVE` 三终态；处理说明字段 `resolve_note`：关闭选填、忽略/误报必填 |
| `close_source` | 进入任一终态均写入；本期一律 `manual`；`auto_recheck` 仅字典预留，无代码路径写入 |
| `RECHECKING` | 仅字典/常量预留；实装后为 `AWAITING_FIX → CLOSED` 间过渡态；本期 API 一律 400 |
| 评论联动 | 处置事务提交成功后调用评论重渲染；失败不回滚处置；复用 M4 marker + PR 级幂等键；Top3 处置态按指纹关联 PR 级问题集合渲染 |
| 存量口径 | `origin=EXISTING` 入账可查、可处置；默认统计与「待处置」计数排除；列表可用归属筛选显式查看 |
| 无 assignee | 展示复用任务 `pr_author` 等已有元数据，表上不加负责人字段 |

## 5. 数据、接口、权限和流程

### 5.1 物化字段映射（协议 v1.1 → `review_issue`）

| TopIssue 字段 | 列 | 说明 |
|---|---|---|
| `rank` | `issue_rank` | 最近一次 Top3 位次（快照，不参与唯一键） |
| `severity` | `severity` | 规范化后入库 |
| `category` | `category` | 可空；参与指纹（null 按空串） |
| `title` | `title` | 必填语义；空则落「未命名问题」；参与指纹（normalize 后） |
| `description` | `description` | mediumtext 可空 |
| `filePath` | `file_path` | 可空；参与指纹（null 按空串） |
| `startLine` / `endLine` | `start_line` / `end_line` | 可空；**不参与指纹**（随提交漂移） |
| `evidence` | `evidence` | mediumtext 可空 |
| `suggestion` | `suggestion` | mediumtext 可空 |
| `origin` | `origin` | `NEW` / `EXISTING`；缺省按 `NEW`（对齐 v1.0 兼容） |

关联上下文（非协议字段，由任务/项目写入）：`project_id`、`provider`（本期 `GITHUB`）、`pr_number`、首次发现与最近物化的 task/run 标识（见 §5.2）。

### 5.2 表 `review_issue`（新建）

| 列 | 说明 |
|---|---|
| `issue_id` | PK |
| `project_id` | 项目 |
| `provider` | 本期 `GITHUB` |
| `pr_number` | PR 号 |
| `fingerprint` | 问题身份：`hash(category + filePath + normalize(title))`，varchar(64)；碰撞处理见 §4 |
| `first_task_id` / `first_run_id` | 首次发现的审查任务与 run |
| `last_task_id` / `last_run_id` | 最近一次物化/刷新所依据的任务与 `review_task_run` |
| `issue_rank` | 最近一次 Top3 位次（快照） |
| `severity` / `category` / `title` / `description` | 快照 |
| `file_path` / `start_line` / `end_line` | 定位快照 |
| `evidence` / `suggestion` | 证据与建议快照 |
| `origin` | `NEW` / `EXISTING` |
| `status` | `AWAITING_CONFIRM` / `AWAITING_FIX` / `CLOSED` / `IGNORED` / `FALSE_POSITIVE`（字典另含 `RECHECKING` 预留） |
| `resolve_note` | 处理说明，≤500：关闭说明**选填**；忽略/误报原因**必填**；未处置为空 |
| `close_source` | 进入终态时写入：`manual`（本期）/ `auto_recheck`（预留） |
| `closed_by` / `closed_time` | 进入终态的操作者与时间；未终态为空 |
| `create_by` / `create_time` / `update_by` / `update_time` | 审计字段 |

约束与索引：

- `UNIQUE (project_id, pr_number, fingerprint)` —— PR 级问题唯一；
- `KEY (project_id, status)`、`KEY (origin, status)`（台账筛选与统计口径）；
- 不再单列 `KEY (project_id, pr_number)`：唯一键前缀已覆盖；
- **不加** `assignee`、逾期、工单外键等列。

物化算法（SUCCESS 落库之后，与 GitHub/IM 投递同级钩子，失败隔离）：

```text
解析 top_issues_json → List<ReviewTopIssue>（最多 3）
批次内维护已用指纹集合 used
对每条 issue:
  fp = fingerprint(category, filePath, normalize(title))
  若 fp ∈ used → fp = fp + ":" + 批次序号   （同批碰撞兜底，见 §4）
  used.add(fp)
  按 (project_id, pr_number, fp) 查既有行
  若无行 → INSERT：status=AWAITING_CONFIRM，first/last task_id+run_id=当前，close_source 空
  若有行且 status 为终态（CLOSED/IGNORED/FALSE_POSITIVE）→ 整行不动
  若有行且未终态 → 刷新快照字段（含 issue_rank）+ last_task_id/last_run_id，保留 status/处置字段
任一步异常 → 记日志，不改 task_status，不回滚审查结论
```

> 物化失败与评论投递失败同级：审查事实已成立；运维可通过任务重试再次触发物化（幂等）。LLM 措辞漂移（title 改写）导致指纹失配时会新建行——本期接受，核心版以定向复核与指纹调优收敛。

### 5.3 状态机（对齐路线图 §3.2）

```text
                  confirm
AWAITING_CONFIRM ────────────► AWAITING_FIX
      │                            │
      │ close（说明选填）           │ close（说明选填）
      ├──────────► CLOSED ◄────────┤
      │                            │
      │ ignore / false-positive    │ ignore / false-positive
      │ （原因必填）                │ （原因必填）
      ├──────────► IGNORED ◄───────┤
      └──────────► FALSE_POSITIVE ◄┘

CLOSED / IGNORED / FALSE_POSITIVE：三个终态，拒绝任何再转移
RECHECKING：字典预留；实装后为 AWAITING_FIX → CLOSED 间过渡态，本期 API 一律 400
```

规则：

| 动作 | 合法转移 | `resolve_note` | 说明 |
|---|---|---|---|
| 确认 | `AWAITING_CONFIRM → AWAITING_FIX` | 无 | 表示「确认为有效问题，待修复」 |
| 关闭 | `AWAITING_CONFIRM / AWAITING_FIX → CLOSED` | 选填 | 问题已修复 |
| 忽略 | `AWAITING_CONFIRM / AWAITING_FIX → IGNORED` | **必填** | 认可问题但不予修复（风险可接受等），留痕 |
| 误报 | `AWAITING_CONFIRM / AWAITING_FIX → FALSE_POSITIVE` | **必填** | AI 误报，留痕并作为审查质量信号 |
| — | 自任一终态出发 | — | 拒绝；提示终态不可再转 |
| — | 涉及 `RECHECKING` | — | 拒绝 |

进入终态时：写 `resolve_note`（按上表必填/选填校验）、`close_source='manual'`、`closed_by`/`closed_time`。每次合法转移写一条 `review_issue_action`。忽略/误报允许不经确认直接标记（收到问题即可判误报）。

### 5.4 表 `review_issue_action`（新建）

| 列 | 说明 |
|---|---|
| `action_id` | PK |
| `issue_id` | 问题 |
| `operator` | 操作者用户名（对齐现有 `create_by` 口径） |
| `action_type` | `CONFIRM` / `CLOSE` / `DISMISS`（忽略与误报共用，由 `to_status` 区分；未来可扩 `RECHECK`，本期不写） |
| `from_status` | 原状态 |
| `to_status` | 新状态（`DISMISS` 时为 `IGNORED` / `FALSE_POSITIVE`） |
| `resolve_note` | 处理说明快照（关闭说明或忽略/误报原因），可空 |
| `create_time` | 动作时间 |

索引：`KEY (issue_id, create_time)`。只追加不改删（业务上）；不做通用审计框架。

### 5.5 亮点：PR 总结评论重渲染

**目标**：平台侧处置结果回到开发者日常 PR 动线；同一条 ACR 总结评论展示各 Top3 问题的处置状态与忽略/误报原因。

**内容模型扩展**（复用 `ReviewSummaryContent` digest，不另起文案系统）：

- 评论骨架（评分、结论）仍取自该 PR 最近 SUCCESS 任务 + run；Top3 每条的处置态按指纹在 **PR 级问题集合**（`project_id + pr_number`）中关联渲染，不再按 `task_id + rank` 关联；
- Top3 每条追加展示：`待确认` / `待修复` / `已关闭` / `已忽略` / `误报`；`IGNORED` / `FALSE_POSITIVE` 附 `resolve_note` 截断（≤80 字）；`CLOSED` 有说明时同样附截断；
- 指纹未关联到问题行的 Top3 项（如物化失败的降级期）仅展示原内容，不渲染处置行；
- 固定标记 `<!-- acr-review-summary -->` 与 PR 级幂等键 `GITHUB:{projectId}:{prNumber}:SUMMARY_COMMENT` **不变**；
- IM 摘要卡**本期不强制**同步处置态（非范围）；避免 M5 群消息刷屏；
- 已知行为：问题跌出后续 run 的 Top3 后不再出现在评论中（处置记录仍在台账与动作流水）；评论全量展示未关闭问题清单属核心版演进。

渲染示例（相对 M4 增量示意）：

```markdown
1. **[高][新增]** 密码明文传输 — `UserController.java` L42-48
   - 说明…
   - 处置：误报（与本次变更无关）
```

**触发时机**：

```text
处置用例（confirm / close / dismiss）
  → 同一本地事务：更新 review_issue + INSERT review_issue_action
  → 事务提交成功
  → 调用「PR 评论重渲染」用例（同步 try/catch 或同线程，失败吞掉）
       → 定位 project + pr_number
       → 取该 PR 最近 SUCCESS 任务 + run 装配 digest 骨架
       → 按指纹关联 PR 级问题集合，渲染 Top3 处置态
       → list 评论找标记 → update / create
       → upsert review_delivery_record（M4 语义）
```

**失败隔离（强制）**：

- 处置事务已提交后，评论 API 4xx/5xx/超时/PAT 失效 → **仅**投递记录 FAILED + 日志；**不**回滚 `review_issue` / `review_issue_action`；
- 接口对前端仍返回处置成功，并可附带 `commentSyncStatus=FAILED`（可选）提示「评论同步失败，可稍后在投递记录重试」。

**降级与补偿**：

| 场景 | 策略 |
|---|---|
| 评论重渲染失败 | 处置成功；delivery FAILED；运维/有权限用户走既有 `review:delivery:retry`（按 PR 最近 SUCCESS 重渲染 + 指纹关联问题集合，自然带上最新处置态） |
| 重渲染过程中无 SUCCESS 任务 | 理论上不应出现（问题来自 SUCCESS）；若出现则跳过评论并记日志 |
| PR 尚无成功评论（首次投递曾失败或评论被删） | **兜底：create** 新总结评论（与 M4「无标记则新建」一致），写入标记与当前 digest |
| GitHub 限流 | FAILED + 文案提示稍后重试；不阻塞处置 |
| 并发处置同 PR 多问题 | 以「最后一次重渲染」为准；DB 唯一键 + 标记查找收敛双评论风险（同 M4） |

### 5.6 存量问题口径（`origin=EXISTING`）

- **入账**：随 SUCCESS 物化，台账可筛选查看；
- **处置**：允许确认/关闭/忽略/误报（与 NEW 同一状态机），便于清理噪音；
- **统计排除**：一切「待确认数 / 待修复数 / 未关闭数 / 催办候选」默认 `origin='NEW'`（或 `origin <> 'EXISTING'`）；文档与 API 备注写明；
- **评分/Top3 既有规则不变**：存量本就不进 focus 计数与结论（M3.2）；台账统计与之对齐「不催办存量」。

### 5.7 模块边界

```text
acr-admin
  ReviewIssueController          # 列表/详情/确认/关闭/忽略误报；权限声明
acr-review
  ├─ domain/mapper/ReviewIssue*
  ├─ domain/mapper/ReviewIssueAction*
  ├─ service/IReviewIssueService
  ├─ service/impl/ReviewIssueServiceImpl
  │     # 指纹计算、物化去重、状态机、动作流水、数据范围
  ├─ service/ReviewIssueMaterializer  # 或并入 IssueService：SUCCESS 后 upsert
  ├─ delivery/
  │     ReviewSummaryContent(+Factory)  # 扩展处置态字段（指纹关联）
  │     ReviewCommentBodyRenderer       # Top3 追加处置行
  │     ReviewDeliveryServiceImpl       # 复用 upsert；供处置后重渲染调用
  └─ ReviewTaskExecutionServiceImpl     # SUCCESS 落库后挂钩物化（失败隔离）
```

- Controller 不访问 Mapper；
- 不新建 Maven 模块；不建通用工作流引擎。

### 5.8 主流程

```text
审查执行落库 SUCCESS
  →（既有）M4 GitHub 总结评论
  →（既有）M5 IM（若开启）
  →（M6）物化 top_issues_json → review_issue（指纹匹配，幂等）
  → 物化异常 catch，任务保持 SUCCESS

用户确认 / 关闭 / 忽略 / 标记误报
  → 校验数据范围 + 状态机 + 说明必填/选填
  → 更新 issue + 写 action（本地事务）
  → 提交后重渲染该 PR 总结评论（失败隔离）
```

### 5.9 接口与权限

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/review/issue/list` | `review:issue:list` | 筛选：项目、状态、严重度、归属、关键词（title/path）；数据范围按项目 |
| GET | `/review/issue/{issueId}` | `review:issue:query` | 详情：快照字段 + 来源任务摘要 + 动作时间线 |
| PUT | `/review/issue/{issueId}/confirm` | `review:issue:confirm` | 待确认 → 待修复 |
| PUT | `/review/issue/{issueId}/close` | `review:issue:close` | → `CLOSED`；body：`resolveNote` 选填 |
| PUT | `/review/issue/{issueId}/dismiss` | `review:issue:close` | → `IGNORED` / `FALSE_POSITIVE`；body：`dismissType` 必填、`resolveNote` **必填**（复用 close 权限，避免权限碎片化） |

任务/记录详情已有 GET：Top3 项按指纹附加 `issueId`（若已物化），供前端跳转；无新权限。

请求体示例：

```json
// PUT /close
{ "resolveNote": "已修复，密码字段改为加密存储（可选填）" }

// PUT /dismiss
{ "dismissType": "FALSE_POSITIVE", "resolveNote": "与本次变更无关，属历史实现（必填）" }
```

**菜单（2026-08-03 经本地库 `ai_code_review.sys_menu` 核验空闲；SQL 落地前复核一次）**

| menu_id | 名称 | 类型 | 说明 |
|---|---|---|---|
| 131 | 问题台账 | C | 挂「代码审查」`parent_id=3`；`path=issue`，`component=review/issue/index`；`order_num` 紧随审查记录(128) 之后（建议 7） |
| 1160 | 问题列表 | F | `review:issue:list` |
| 1161 | 问题查询 | F | `review:issue:query` |
| 1162 | 问题确认 | F | `review:issue:confirm` |
| 1163 | 问题关闭 | F | `review:issue:close`（覆盖忽略/误报动作） |

角色 2 同步授权（对齐 M4/M5）。字典建议：`review_issue_status`（`AWAITING_CONFIRM` / `AWAITING_FIX` / `CLOSED` / `IGNORED` / `FALSE_POSITIVE` + 预留 `RECHECKING`）、`review_issue_close_source`、`review_issue_origin`（若前端筛选项不便复用既有文案）。

SQL 文件：`sql/25_issue_ledger_m6.sql`（建表 + 字典 + 菜单权限；`CREATE TABLE IF NOT EXISTS` / `WHERE NOT EXISTS` 幂等；utf8mb4）。

### 5.10 前端

1. **问题台账**列表：顶部筛选（项目 / 状态 / 严重度 / 归属 NEW|EXISTING / 关键词）+ 表格 + 分页；状态与严重度标签对齐现有 `el-tag` 风格；遵守 `rules/UI_THEME_RULES.md`；
2. **详情**（抽屉或独立页，优先抽屉）：定位、建议、证据、归属、来源任务链接、PR 信息、动作时间线；待确认显示「确认」；未终态显示「关闭」「忽略」「误报」——关闭弹窗说明选填，忽略/误报弹窗原因必填；
3. **任务/记录详情 Top3**：每条可跳转已物化 `issueId`（按指纹关联）；展示当前处置状态徽标；未物化（历史任务）仅展示原 Top3；
4. 不在本切片做工作台待办聚合。

## 6. 失败分类与安全

| 场景 | 处理 |
|---|---|
| 物化 JSON 解析失败 | 跳过物化并打日志；任务仍 SUCCESS |
| 非法状态转移 / 忽略误报缺原因 | 业务校验 400；不写 action |
| 对终态（CLOSED/IGNORED/FALSE_POSITIVE）再操作 | 拒绝 |
| 指纹同批碰撞 | 追加批次序号后缀；不报唯一键冲突 |
| 评论重渲染失败 | 处置已成功；delivery FAILED；可补发 |
| 跨项目访问 | 列表 `@DataScope`；详情/处置前 `checkDeptDataScope`（对齐投递/任务） |
| 密钥与代码资产 | 评论/日志不写 PAT；证据/建议按企业代码资产权限展示，不新增明文外发 |

## 7. 测试要点

至少覆盖：

1. **指纹去重与物化幂等**：同一任务 SUCCESS → 物化 N 条；任务重试再次 SUCCESS → 行数不变；**同一 PR 新任务再次发现同一问题 → 行数不变、快照刷新、`last_task_id`/`last_run_id` 更新、未终态状态不变**；终态行完全冻结；同批指纹碰撞（同文件同类别同标题两条）不报唯一键冲突；
2. **FAILED 不物化**：FAILED / 无 Top3 不产生 `review_issue`；
3. **状态机边界**：合法 confirm/close/dismiss；三个终态再转均失败；dismiss 缺 `dismissType` 或 `resolveNote` 失败；close 说明选填可通过；`RECHECKING` 不可用；
4. **PR 评论重渲染失败隔离**：mock GitHub 失败时 issue 状态与 action 已提交且保持；delivery 为 FAILED；
5. **数据权限边界**：无项目范围用户不可 list/query/confirm/close/dismiss 他项问题；
6. 渲染单测：digest/评论按指纹关联处置态，含忽略/误报原因截断；标记仍在；未关联问题行的 Top3 项不渲染处置行；
7. 存量口径：未关闭统计用例排除 `EXISTING`；
8. 前端：`npm run build:prod` 通过即可。

## 8. 分步实现计划

| 步 | 内容 | 验证 | 状态 |
|---|---|---|---|
| 1 | 设计文档定稿（本文 v1.1，已合入 review 意见） | 人工确认状态机、指纹去重、表结构、评论失败隔离 | ✅ |
| 2 | SQL `25_issue_ledger_m6.sql`（表、字典、菜单 131/1160–1163）+ `docs/deployment.md` / `sql/README.md` | 脚本幂等、utf8mb4；清单连续编号；落地前复核 menu_id | ✅ |
| 3 | `review_issue` / `action` 领域与 Mapper；指纹计算 + 物化去重服务 + 幂等单测；执行链 SUCCESS 挂钩 | 指纹去重与重试幂等单测绿 | ✅ |
| 4 | 状态机 + 动作流水 + REST/权限 + 数据范围单测 | 状态机与权限单测绿 | ✅ |
| 5 | digest/评论渲染扩展（指纹关联）+ 处置后重渲染 + 失败隔离/兜底 create | 渲染与隔离单测绿；`mvn test` | ✅ |
| 6 | 前端台账页、详情处置、任务/记录 Top3 联动；CHANGELOG 与路线图 §7 同步 | `npm run build:prod` | ✅ |

每步可独立提交；步 2–3 合入不改变线上处置行为，步 5 接入评论联动后生效。

## 9. 验收标准与风险

**验收**：

- SUCCESS 任务产生对应 `review_issue`；FAILED 无；同任务重试不重复行；**同 PR 多次提交不产生重复问题（指纹去重生效）**；
- 可完成确认、关闭、忽略、误报四类动作；三个终态不可再转；忽略/误报原因必填、关闭说明选填；动作时间线完整；
- 处置后 PR 总结评论更新处置态；人为制造评论失败 → 处置仍成功，投递 FAILED，重试可恢复；
- 无成功评论时处置可兜底新建带标记评论；
- 存量问题可查但不计入未关闭/催办口径；
- 跨项目不可见；`mvn test` 与 `npm run build:prod` 通过；CHANGELOG 有 M6。

**风险与对策**：

- 重试/新提交后 Top3 内容变化与已确认状态冲突：定稿为未终态刷新快照、终态冻结；
- **LLM 措辞漂移（title 改写）导致指纹失配 → 同一问题新建行**：本期接受（低频），核心版以定向复核与指纹调优收敛；台账列表可按标题关键词人工识别；
- 评论展示处置态导致正文变长：处置行短标签 + note 截断；
- 物化钩子失败导致「有结论无台账」：失败隔离 + 任务重试可补物化；详情 Top3 无 `issueId` 时提示未入账；
- menu_id 撞号：以本地库核验为准，SQL 执行前再查一次。

## 10. 非范围（再声明）

指派/转办/申诉/逾期提醒/自动关闭/自动重审、定向复核与问题手工合并/指纹调优（核心版）、`RECHECKING` 实装、工作台、OCR 问题输出专项、工单对接、`assignee` 及任何额外预留列、协议改动、M5 IM 处置态同步、inline/Status Check/多 Provider/新模块。
