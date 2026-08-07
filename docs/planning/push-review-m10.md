# M10 审查配置与推送审查设计（定稿）

- 状态：**定稿，产品决策已确认（2026-08-07）**，开发分支 `feature/m10-push-review`
- 取代：`docs/planning/review-config-m10.md`（设计草案，2026-08-06）
- 上游依据：`product-roadmap.md` §7.6「Push/merge 事件双触发路径（2026-08-05 方向确认）」
- 关联设计：`issue-lifecycle-m8.md`（台账生命周期）、`issue-ledger-lifecycle-view-m8.1.md`（台账可视化与批量处置）

---

## 1. 背景与目标

项目设置中的「审查范围」目前只回答「审哪些文件」，不回答「什么行为触发审查」——触发方式被隐含写死为合并请求一条线。不走 PR 合并流程、直接向主干推送的团队（在国内中小企业与存量系统维护团队中占比不低）无法接入。

M10 把「审查范围」升级为「审查配置」，回答两个问题：

1. **审查类型**：这个项目接受哪种触发行为——合并请求审查（现有 PR/MR 线）和/或推送审查（直接 push 线）；
2. **审查范围**：针对所选审查类型，分别配置「审哪些」。

目标：直推团队可把仓库接入 ACR 并获得完整治理闭环（发现 → 触达 → 处置 → 复核）；PR 团队配置体验与行为完全不变。

### 现状基线（2026-08-07 代码核对结论）

审查执行主干**已经是 SHA 驱动**，push 线天然可复用：

- Diff 拉取契约 `GitPullRequestDiffFetcher.fetchDiff(repo, access, baseSha, headSha)` 不依赖 PR 编号；
- OCR 引擎工作区准备 `GitPullRequestWorkspaceRequest(repo, access, baseSha, headSha, dir)` 同样 SHA 驱动；
- Webhook 接入骨架（验签、投递 ID 幂等、仓库 fullPath 匹配、事件落库）平台无关；
- IM 通知幂等键已是任务粒度（`{channelType}:{taskId}:REVIEW_DONE`），push 任务直接复用；
- `ReviewSummaryContentFactory` 对 `prNumber <= 0` 已有降级（PR 链接为空）。

真正的耦合点只有四处：Webhook 入口把非 PR 事件直接 IGNORED；四平台适配器无 push 载荷解析；任务/台账/投递按 `pr_number` 归组；PR 元数据拉取与总结评论回写步骤假定存在 PR。

## 2. 产品决策记录（2026-08-07 确认）

| 编号 | 决策 | 结论 |
|---|---|---|
| D1 | 审查类型单选/多选 | **多选**。存储用两个布尔列（对齐现有 `pr_review_enabled` 风格），不引入 CSV 字段。存量项目推送审查默认停用，零迁移零感知 |
| D2 | 双类型并存时的「双重审查」 | PR 合并会同时产生 push 事件；当推送触发分支与 PR 目标分支交集时，合并后额外触发一次推送后审查——**这是预期行为，不做运行时自动规避**（squash/rebase/merge-commit 三种合并 SHA 均无法可靠识别，自动规避必然脆弱）。台账 fingerprint 去重保证**用户可见结果不重复**（同一问题只有一条台账）；代价仅为一次额外模型调用、一条额外审查记录、一条额外 IM 通知。配置页做**交集提示**，把成本说透，选择权给用户。合并后审查可覆盖 PR 审查覆盖不到的冲突解决改动，对企业治理是兜底能力而非浪费 |
| D3 | push 线台账归组键 | **哨兵 0 + 分支列**：push 任务 `pr_number=0`；`review_issue` 增 `ref_branch`（PR 线空串、push 线分支名），唯一键升级为 `(project_id, pr_number, ref_branch, fingerprint)`。PR 线存量行 ref_branch='' 唯一性完全兼容 |
| D4 | push 线交付渠道 | **本期不写 commit 评论**。交付 = IM 分级通知 + 问题台账 + 审查记录详情。理由：Gitee commit 评论 API 受限，做了即四平台三套行为；推送审查价值在发现、通知与处置。commit 评论列为后续增强（见 §14） |
| D5 | 边界事件口径 | 分支删除 push（after 全 0）→ 忽略；新分支首次 push（before 全 0）→ 本期忽略并记录 IGNORED（走 PR 的团队新分支已被 PR 线覆盖，直推新分支场景留参数后补）；force push → compare 用两点直比（before..after），逐平台验证 |

## 3. PM 视角复核：为什么这套设计是稳的

### 3.1 功能边界

- 推送线严格继承 §7.6 已确认口径：**只做合并后审查**——发现、通知、台账处置；不做质量门禁、不阻断推送。两条线的语义边界一句话可讲清：PR 线管「合并前」，push 线管「合并后/直推」。
- 台账、投递、引擎、验签**全部复用，不建平行机制**。最大的重构风险是「两套治理逻辑各自演化」，本设计从对象模型层面杜绝：push 任务就是 ReviewTask，push 问题就是 ReviewIssue，仅以 `event_source` / `ref_branch` 区分归组维度。
- 非目标不变：不做 PR 助手、不做通用缺陷库、推送审查不与 M11 指派/SLA 耦合（但数据结构兼容，见 §3.4）。

### 3.2 数据结构

- `event_source` 把「任务从哪来」显式化，`pr_number` 哨兵 0 保住存量唯一键、索引与查询模式不动——迁移面收敛到一次加列 + 两个索引重建。
- `ref_branch` 的本质是把台账归组维度从「PR 维度」升级为「引用维度」：未来若出现按 tag、按 commit 的审查触发，归组键扩展方向已经打开，不需要再动唯一键语义。
- 延续宽表冻结约定：`review_task` 快照列不再新增，本切片零快照列变更；后续新快照一律走 JSON（`scope_decision_json` 先例）。

### 3.3 交互逻辑（国内企业后台习惯）

- 配置页采用「先定审查方式、再定审查范围」的两段式结构，与企业用户对「先选模式、再填参数」的心智一致；类型选项卡片带说明文案，推送审查文案明确写出「推送后审查，不拦截推送」——诚实预期管理，避免企业客户误以为是门禁。
- 交集提示用非阻断 warning：国内企业后台惯例是「提示到位、不替用户做决定」。
- 列表类页面用字典 tag 区分审查类型，PR 列对 push 行降级展示分支——不新增列、不改筛选主结构，最小认知负担。

### 3.4 扩展性

- 再接新触发类型（tag 推送、定时全量扫描）只需：适配器解析 + `event_source` 枚举值 + 归组键哨兵约定，接入骨架、执行管线、台账、投递全部不动。
- 平台事件白名单参数化（`review.<provider>.pushEvents`），各平台事件类型名变化不改代码。
- commit 评论交付是纯增量扩展点：投递渠道常量体系（`ReviewDeliveryConstants.channelForProvider`）与投递记录表已就位，后续加「按平台支持度降级」不影响本期结构。
- M11 问题指派/SLA 届时对 push 线问题同样生效（assignee/due 加在台账层，与归组键正交）。

## 4. 两种审查类型定义

| 维度 | 合并请求审查（现有线） | 推送审查（新线） |
|---|---|---|
| 触发事件 | PR/MR opened / reopened / synchronize | push 到配置的触发分支（before/after 增量） |
| 审查时机 | 合并前 | 合并后 / 直推后（不做合并前拦截） |
| Diff 来源 | PR base...head | push before..after（两点直比） |
| 结果交付 | PR 总结评论（原地更新）+ IM + 台账 | IM 分级通知 + 台账（无 PR 评论可挂；commit 评论后续增强） |
| 台账归组 | (project, pr_number, '') | (project, 0, 分支名) |
| 关闭联动 | PR 关闭/合并联动关闭问题 | 无（分支删除事件忽略，见 D5） |
| 引擎/验签/凭据/幂等 | 复用 | 复用 |

## 5. 数据模型

### 5.1 变更清单

**review_project**（2 列）：

| 列 | 类型 | 默认 | 说明 |
|---|---|---|---|
| push_review_enabled | char(1) NOT NULL | '1'（停用） | 是否启用推送审查（0启用 1停用，对齐 pr_review_enabled 语义） |
| push_trigger_branches | varchar(1000) | NULL | 推送触发分支，换行/逗号分隔，支持 `release/*` 通配（复用 `GlobPattern`） |

**review_task**（1 列）：

| 列 | 类型 | 默认 | 说明 |
|---|---|---|---|
| event_source | varchar(20) NOT NULL | 'PR' | 事件来源（PR=合并请求 / PUSH=推送）。push 任务 pr_number=0，source_branch=target_branch=推送分支，base_sha/head_sha=before/after，pr_title 承载提交摘要，pr_author 承载推送人 |

**review_issue**（1 列 + 2 索引重建）：

| 变更 | 内容 |
|---|---|
| + ref_branch | varchar(255) NOT NULL DEFAULT ''，参考分支（PR 线空串；push 线=推送分支） |
| uk 重建 | `uk_issue_pr_fingerprint (project_id, pr_number, fingerprint)` → `uk_issue_ref_fingerprint (project_id, pr_number, ref_branch, fingerprint)` |
| 索引重建 | `idx_issue_pr_family` → `idx_issue_ref_family (project_id, pr_number, ref_branch, family_key)` |

**review_webhook_event**：零变更（现有 source_branch/target_branch/base_sha/head_sha 字段直接承载 push 事件；push 事件 action/pr_number/pr_title 为 NULL）。

**review_delivery_record**：零变更（push 行 pr_number=0；IM 投递幂等键已为任务粒度；channel 复用 IM 三渠道常量）。

### 5.2 迁移脚本

`sql/33_push_review_m10.sql`（含参数与字典初始化，见 §8）。存量安全论证：

- push_review_enabled 默认 '1' 停用 → 存量项目推送事件仍走 IGNORED，行为逐字节不变；
- ref_branch 默认 '' → 存量台账行在新唯一键下唯一性不变（同 project+pr+fingerprint 仍唯一）；
- event_source 默认 'PR' → 存量任务语义不变。

## 6. 流程设计

### 6.1 Webhook 受理（ReviewWebhookServiceImpl.push 分支）

现有主干不变：载荷限长 → deliveryId → eventType → 事件落库（provider+deliveryId 幂等）→ 仓库匹配 → 项目状态 → 验签。验签后分流：

```
isPullRequestEventType  → 现有 PR 分支（不变）
isPushEventType         → push 分支（新增）：
  1. 解析 GitPushEvent（ref/分支名、before、after、推送人、commits 摘要）
  2. 删除分支（after 全 0）→ IGNORED「分支删除推送，忽略」
  3. 新建分支（before 全 0）→ IGNORED「新分支首次推送，暂不审查」
  4. 项目 push_review_enabled != '0' → IGNORED「项目未启用推送审查」
  5. 分支名按 pushEvents 参数白名单校验事件类型；按 push_trigger_branches
     （精确名 + glob 通配，复用 GlobPattern）匹配 → 不匹配 IGNORED
  6. createTaskFromPushEvent 建单（事务语义同 PR 线：任务与受理结果同生共死，
     提交后调度异步执行）
其他事件类型           → IGNORED（现状不变）
```

### 6.2 适配器契约扩展

`GitWebhookAdapter` 增加：

- `boolean isPushEventType(String eventType)`
- `GitPushEvent parsePushEvent(String eventType, String deliveryId, byte[] payload)`

`GitPushEvent` record（平台无关业务事实，对齐 GitPullRequestEvent 风格）：`deliveryId, repositoryOwner, repositoryName, repositoryFullPath, branch, beforeSha, afterSha, pusher, commitCount, headCommitMessage, created, deleted`。

四平台事件类型：GitHub `push`、Gitea `push`、GitLab `Push Hook`、Gitee `Push Hook`。测试 fixture 必须使用各平台**真实载荷样本**。

### 6.3 执行管线适配（ReviewTaskExecutionServiceImpl）

管线步骤（RESOLVE_CONFIG → PREPARE_WORKSPACE → INVOKE_ENGINE/INVOKE_MODEL → PERSIST_RESULT）不变，按 event_source 分流：

- `applyPrMetadata`：PUSH 任务跳过（无 PR 元数据可拉）；
- Diff 拉取与工作区准备：零改动（本就 SHA 驱动）；
- 结论、评分、范围决策、协议解析：零改动。

OCR 引擎路径对 push 的兼容性以联调验证为准；若 CLI 不支持，降级策略为「push 线暂仅支持大模型直审」，配置页对 OCR_ENGINE + push 组合提示（不阻塞本切片）。

### 6.4 交付（ReviewDeliveryServiceImpl）

- **PR 线不变**；
- PUSH 任务 SUCCESS/FAILED 后：跳过总结评论回写（无 PR 可挂），IM 通知走现有 `deliverNotifyAfterTerminal`（幂等键任务粒度已就位）；
- 内容渲染（`ReviewSummaryContentFactory` / IM 渲染器）：push 任务不渲染 PR 链接，标题与分支信息按 push 语义渲染（分支名 + 短 SHA + 提交摘要）；「查看合并请求」链接降级去除，保留「查看审查详情」；
- 问题处置后的评论重渲染（`rerenderSummaryComment`）：仅 PR 线语义，push 线问题处置不触发（pr_number=0 分支直接跳过）。

### 6.5 台账与轮次对账（ReviewIssueServiceImpl）

- 物化：push 任务问题 `pr_number=0` + `ref_branch=推送分支`；fingerprint 算法不变（文件+规则+位置特征），跨轮去重按新唯一键；
- 轮次对账（M8）：对账分组键从 (project, pr) 扩展为 (project, pr_number, ref_branch)；missed_streak、自动转待复核、复核证据逻辑不变；
- PR 关闭联动：仅 PR 线，push 线无对应联动（D5）。

## 7. 交互设计

### 7.1 项目设置「审查配置」区

原「审查范围」卡片升级为「审查配置」，自上而下两个分区：

**审查类型（上区）**——两个带说明文案的 checkbox 选项卡片：

- **合并请求审查**：`在合并请求（PR/MR）创建或更新代码时触发审查，审查结论以合并请求评论发布，问题进入问题台账。适合有合并请求协作流程的团队。`
- **推送审查**：`代码直接推送到指定分支（不经合并请求）时触发审查，审查结论以通知送达，问题进入问题台账。适合直接向主干推送代码的团队。本审查为推送后审查，不拦截推送。`

校验：至少选择一项，未选时保存阻断并提示「请至少选择一种审查类型」。

**审查范围（下区，随类型联动）**：

- 公共字段（只出现一份）：排除路径 glob、审查测试文件、上报历史存量问题、高影响变更扩展整文件；
- 勾选合并请求审查 → 展示「目标分支」（现有控件平移）；
- 勾选推送审查 → 展示「触发分支」（必填，换行分隔，支持通配，placeholder 示例 `main`、`release/*`）+ 提示：`仅列出的分支上的推送会触发审查，其余分支推送忽略。`

**交集提示（D2）**：推送触发分支与 PR 目标分支存在交集时，展示非阻断 warning：

`以下分支同时是合并请求目标分支：<分支列表>。合并请求合并到这些分支时，会额外触发一次推送后审查。问题台账自动去重、不会产生重复问题；如希望避免，可在推送审查中移除相应分支。`

### 7.2 列表与详情展示

- 任务列表：新增「类型」列（字典 `review_event_source` tag）；「PR」列对 push 行展示「分支名 @短SHA」；筛选区加类型下拉；
- 问题台账：「PR」列对 push 行展示分支名；筛选语义不变；
- 审查记录详情：头部信息展示事件来源；push 任务不展示 PR 链接区。

## 8. 参数、字典、常量

| 类别 | 内容 |
|---|---|
| 参数 | `review.github.pushEvents`=push、`review.gitlab.pushEvents`=Push Hook、`review.gitee.pushEvents`=Push Hook、`review.gitea.pushEvents`=push（对齐现有 prEvents 参数风格，运行期可调） |
| 字典 | `review_event_source`：PR=合并请求（primary，默认）、PUSH=推送（success） |
| 常量 | `ReviewPipelineConstants` 增 `EVENT_SOURCE_PR/EVENT_SOURCE_PUSH`；`GitProviderCodes` 复用 |

## 9. 幂等、重试与失败边界

- 事件幂等：`(provider, delivery_id)` 唯一键，push 事件天然继承；一事件一任务（`uk_task_event`）；
- 重复推送（用户连续 push）：每次 push 各自建任务，接受即审，不做防抖（见 §14）；台账 fingerprint 保证问题不重复；
- 投递幂等：IM 键 `{channelType}:{taskId}:REVIEW_DONE` 不变；
- 失败补偿：push 任务失败重试复用现有任务重试入口；验签失败/未匹配项目/分支不匹配均落事件记录并可排障（现状能力）。

## 10. 本次范围 / 非范围

**范围**：审查配置页（类型双选 + 联动范围 + 交集提示）、四平台 push 事件解析与受理、push 建单与执行分流、push 交付（IM + 台账）、台账归组键与对账扩展、任务列表/台账/详情类型展示、参数与字典、迁移 SQL。

**非范围**：commit 评论回写、推送防抖/聚合窗口、新分支首次 push 审查、分支删除的台账联动、质量门禁与合并前拦截、角色化工作台、导出。

## 11. 实现步骤与验证

| 步 | 内容 | 验证 |
|---|---|---|
| 1 | 迁移 SQL（本仓库已出：sql/33_push_review_m10.sql）+ 领域层（GitPushEvent、常量、domain 字段） | dev 库执行通过；`mvn test` 存量全绿 |
| 2 | 四平台适配器 push 解析（真实载荷 fixture，含删除/新建分支/force push 样本） | 适配器单测全绿 |
| 3 | Webhook 入口 push 分支 + push 建单 | ReviewWebhookServiceImplTest 扩展用例（启用/停用、分支匹配/不匹配、删除/新建分支） |
| 4 | 执行管线分流（跳 PR 元数据/评论回写），LLM 与 OCR 两路径 push 任务 | 执行服务单测 + webhook-test 仓库真实 push 联调 |
| 5 | 台账归组与对账带 ref_branch | M8 存量用例零回归 + push 归组/对账新用例 |
| 6 | push IM 通知（分支/提交信息渲染，PR 链接降级） | 通知渠道真实发送验证 |
| 7 | 前端：审查配置区 + 列表展示 + 交集提示 | `npm run build:prod` + 页面走查 |
| 8 | 端到端验收 | 见 §12 |

## 12. 验收标准

1. 存量项目升级后 PR 审查行为与升级前逐字节一致（推送默认停用，push 事件仍 IGNORED）；
2. 新建项目可配置两种审查类型并分别保存审查范围；至少选一项的阻断校验生效；交集提示正确展示；
3. 推送审查端到端：向 webhook-test 仓库直推目标分支 → 事件受理 → 建单 → 审查执行 → IM 通知到账 → 台账按分支归组可见；
4. 同一问题二次推送不重复建台账；推送修复后轮次对账自动转待复核；
5. PR 合并触发的推送后审查（双类型交集场景）：台账不产生重复问题；
6. 分支删除/新建分支推送被正确 IGNORED 并有事件记录；
7. 四平台 push 载荷解析单测全绿；`mvn test` 与 `npm run build:prod` 通过。

## 13. 风险

| 风险 | 等级 | 应对 |
|---|---|---|
| OCR 引擎 CLI 对 push 场景兼容性未实测 | 中 | 步骤 4 联调验证；不通则降级「push 线暂仅支持大模型直审」+ 配置提示，不阻塞切片 |
| 台账唯一键重建触碰 M8 对账 | 中 | 迁移 SQL 已在 dev 库先行验证；M8 存量用例零回归作为门槛 |
| GitLab/Gitee Push Hook 载荷字段差异 | 中 | fixture 强制使用真实样本；逐平台验证 force push 两点比较 |
| 双类型交集带来额外模型成本 | 低 | 配置页交集提示；用户可自选规避 |

## 14. 后续演进（M10 之后，本期不实现）

- **commit 评论交付**：按平台支持度分级（GitHub/GitLab/Gitea 支持、Gitee 降级 IM），投递渠道常量体系已预留；
- **推送防抖/聚合窗口**：同项目同分支短窗口内多次推送聚合为一次审查（参数化窗口时长），需任务延迟调度支持；
- **新分支首次 push 审查**：与默认分支 compare，参数开关；
- **分支删除台账联动**：删除分支上活跃问题的处置策略（提示/自动转待复核），需产品定义；
- **M11 问题指派与 SLA**：台账层扩展，对 PR 线与 push 线问题同等生效。
