# M11 行内评论（Inline Comments）设计

- 状态：**定稿待审**（2026-08-10）
- 上游依据：`product-roadmap.md` §6「结果回写：MVP 总结评论；核心版 inline/状态检查」、§7.4 核心版承诺
- 关联设计：`review-comment-writeback-m4.md`（总结评论）、`issue-ledger-m6.md`（台账）、`push-review-m10.md` §14（精确评论演进线）
- 并行任务：企业级架构风险修复（S3 投递恢复已合入，见 `sql/35_review_delivery_recovery.sql`）——本切片与其协同、不冲突，见 §10

---

## 1. 背景与目标

总结评论回答「这个变更整体怎么样」，行内评论回答「第几行出了什么问题」。竞品形态（PR-Agent / CodeRabbit）以行级反馈为核心交互；我们的产品原则 §2.3「代码平台内完成主要反馈」也要求开发者不必离开 PR 就能看到精确定位。

M10.1 之后，台账问题已带完整定位信息（file_path / start_line / end_line / evidence / suggestion）并在对账后固化 issueId——**行内评论的内容与幂等锚点已经齐备**，缺的只是投递管道。

目标：PR 线审查成功后，把达到严重度门槛的问题以行内评论发布到代码平台，一问题一评论、全生命周期只发一次，投递全程走既有意图队列（可重试、可审计、可人工补发）。

## 2. 产品决策记录（对抗式讨论）

### D1 发送范围：项目可配的严重度门槛，默认仅 CRITICAL+HIGH

**决策**：项目级开关 `inline_comment_enabled`（默认停用，对齐 push_review_enabled 的保守默认）+ 严重度白名单 `inline_severities`（默认 `CRITICAL,HIGH`）。

**对抗质疑与回应**：
- *为什么不发全部问题？* —— 低噪声优先（产品原则 1）。行内评论直接贴在代码行上，噪声的侵入性远高于总结评论里的列表；中低问题进台账与总结评论即可。企业试点期宁可少发，信任建立后再放开。
- *为什么默认停用？* —— 存量项目开启行内评论是行为突变（每个 PR 突然多出一批评论）；与 M10 新审查类型默认停用同一哲学：新交付方式 opt-in。
- *为什么不按问题数限流（如最多 5 条）？* —— 门槛是质量语义，限流是数量语义，两者可叠加但首版只做门槛（YAGNI）；门槛内问题数失控的场景在真实试点出现后再加。

### D2 幂等模型：issueId 粒度，一问题一评论，全生命周期只发一次

**决策**：投递幂等键 `{provider}:{projectId}:{issueId}:INLINE_COMMENT`。同一问题跨轮次、跨重试只发布一条行内评论；新问题新评论；问题转待复核/关闭**不**追加评论更新。

**对抗质疑与回应**：
- *为什么不像总结评论那样原地更新？* —— 行内评论平台语义是「针对某提交某行的意见」，原地编辑会丢失讨论上下文；GitHub/GitLab 的行内评论也不支持无痕迹的跨提交重定位。一问题一评论是平台原生语义。
- *问题后来被证误报了，评论还挂着？* —— 评论正文带问题编号与台账指引，人工在平台侧回复/解决即可；自动撤回评论是「替用户做决定」，首版不做（演进项 §12）。
- *重复投递怎么防？* —— 三层：① 意图键唯一（数据库层）② 投递执行前先按标记查找已存在评论（`<!-- acr:inline:issue-{id} -->`，复用 M4 findCommentWithMarker 模式）③ 成功后 external_id 落库。重试绝不产生第二条评论。
- *PR 更新（新提交推上来）后旧评论位置失效？* —— 平台自动标记 outdated（GitHub/GitLab 原生行为），评论仍可见可追溯，符合「历史证据不消失」的审计要求。不做重定位。

### D3 平台能力矩阵：四平台支持度声明，受限平台降级

| 平台 | 行内评论能力 | 实现路径 |
|---|---|---|
| GitHub | ✅ 完整 | `POST /pulls/{n}/comments`（body + commit_id + path + line/start_line/side） |
| GitLab | ✅ 完整 | `POST /merge_requests/{iid}/discussions` + position（需 MR diff_refs 三 SHA） |
| Gitea | ✅ 完整 | `POST /pulls/{index}/reviews`（event=COMMENT + comments[]） |
| Gitee | ⚠️ 待实测 | PR 评论 API 的 path/line 参数支持度存疑，联调验证；不支持则降级仅总结评论并记录 SKIPPED |

**对抗质疑**：*为什么不先只做 GitHub？* —— 四平台统一契约是本产品的立身之本（M2 多平台扩展的既有纪律）；接口按四平台设计、实现按能力降级，与 M10 commit 评论的 D4 决策同一方法论。

### D4 内容形态：中文、紧凑、可回查

```
🚨 严重 · 安全 · 问题 #52
SQL 注入漏洞
memberId 未校验直接拼接进 SQL 字符串，可被构造恶意输入改变查询语义。

💡 建议：使用 PreparedStatement 参数化查询…

<!-- acr:inline:issue-52 -->
—— AI Code Review · 审查记录 #22 · 处置与复核请前往问题台账
```

- 严重度图标/中文 + 分类中文 + 问题编号（与台账、通知口径统一，M10.2 中文命名规范的延续）；
- 描述取 issue.description 截断（行内不堆长文，完整内容在台账）；建议取 suggestion 截断；
- 尾部标记注释不可见但可检索（幂等查找锚点）；
- 落库 content_snapshot（投递快照语义既有纪律：详情展示发送时快照）。

### D5 与总结评论的关系：并行投递，总结评论预告行内

**决策**：行内意图与总结意图同一时机入队（deliverAfterSuccess），各自独立投递、互不阻塞；总结评论范围段追加确定性文案「本次审查生成 N 条行内评论（严重 x · 高 y），单独发布于代码平台」——N 由入队时的门槛过滤结果计算（确定性，不依赖行内实际投递成败）。

**对抗质疑**：*行内投递失败时总结评论说「生成 N 条」不是误导吗？* —— 「生成」描述的是审查事实（问题确实产出了），投递失败在投递记录可查、可补发；若改成「已发布 N 条」则必须等行内全部成功才能渲染总结，把两条独立管道耦合成串行，违背「局部投递失败不影响其他交付」的既有纪律。

### D6 push 线排除

push 任务无 PR 可挂行内评论，本切片不涉及 push 线（commit 评论是独立演进项 §12）。enqueue 入口对 push 任务直接短路（与 enqueueSummary 同款守卫）。

## 3. 数据模型

**review_delivery_record**（1 列 + 1 索引，sql/36，沿用 34/35 的幂等存储过程风格）：

| 变更 | 说明 |
|---|---|
| + issue_id bigint NULL | 行内评论意图关联的问题 ID（总结/IM 记录为 NULL） |
| + KEY idx_delivery_issue (issue_id) | 问题详情反查投递状态 |

**review_project**（2 列）：

| 列 | 类型 | 默认 | 说明 |
|---|---|---|---|
| inline_comment_enabled | char(1) NOT NULL | '1'（停用） | 是否启用行内评论（0启用 1停用） |
| inline_severities | varchar(40) | 'CRITICAL,HIGH' | 行内评论严重度白名单，逗号分隔 |

**字典** `review_delivery_channel` 增 4 个渠道值：`GITHUB_PR_INLINE_COMMENT` / `GITLAB_MR_INLINE_COMMENT` / `GITEE_PR_INLINE_COMMENT` / `GITEA_PR_INLINE_COMMENT`（中文名「GitHub 行内评论」等）。

**不新增表**：问题-评论关联用 delivery_record.issue_id 表达；评论正文快照沿用 content_snapshot。

## 4. 流程设计

### 4.1 入队（ReviewDeliveryIntentService 新增方法，纯增量）

```
enqueueInlineComments(task, run, triggerSource, operator)：
  守卫：task SUCCESS、非 PUSH、prNumber>0、project.inline_comment_enabled='0'
  issues = resolveTopIssues(run) 中 issueId 非空且 severity ∈ inline_severities 者
  对每个 issue：
    channel = inlineChannelForProvider(provider)
    key = {provider}:{projectId}:{issueId}:INLINE_COMMENT
    upsert PENDING 意图（issue_id 落库）→ 发布 ReviewDeliveryPendingEvent
  返回入队数（供总结评论文案）
```

`deliverAfterSuccess` 在 enqueueSummary 之后调用 enqueueInlineComments（仅投递服务内改动，**不碰执行服务**）。

### 4.2 调度执行（ReviewDeliveryDispatcher 增行内分支）

```
channel 命中 isInlineCommentChannel：
  1. findInlineCommentWithMarker(repo, access, prNumber, marker) —— 已存在则直接补 external_id 置 SUCCESS（重试防重）
  2. 渲染评论正文（ReviewInlineCommentRenderer，内容快照随记录落库）
  3. client.createInlineComment(repo, access, prNumber, inlineRequest)
  4. external_id 落库 → SUCCESS；异常按既有状态机退避（next_attempt_at / last_error_code / 租约，全部复用 S3 机制）
  5. Gitee 平台返回「不支持」类错误 → SKIPPED + 失败信息说明（不无限重试）
```

### 4.3 平台适配契约（git 包，接口扩展用 default 方法保兼容）

```java
// GitPullRequestCommentClient 追加：
default boolean supportsInlineComments() { return false; }
default GitPullRequestComment createInlineComment(repo, access, prNumber, GitInlineCommentRequest req) { throw 不支持; }
default Optional<GitPullRequestComment> findInlineCommentWithMarker(repo, access, prNumber, marker) { return empty; }

// 新 record GitInlineCommentRequest：
// path, startLine, endLine, body, headSha（GitHub commit_id / GitLab position 所需由适配器内部解析）
```

GitLab 实现需先取 MR diff_refs（base_sha/start_sha/head_sha）再构造 position——适配器内部完成，不外泄。

### 4.4 失败与降级

- 单条行内失败不影响其他行内、不影响总结评论与 IM（意图各自独立）；
- 达到重试上限 → MANUAL（待人工处置，S3 既有语义），投递记录页可补发；
- 平台拒绝（行号越界/文件已删等）→ 记录失败原因，问题在台账不受影响。

## 5. 交互设计（中国大陆企业习惯）

1. **项目设置「审查配置」区**：勾选合并请求审查时展示「行内评论」开关 + 严重度多选（严重/高/中/低，默认严重+高）；提示文案：`将重点问题以行内评论发布到合并请求的代码行上，同一问题全生命周期只发布一次；未发布行内评论的问题仍可在总结评论与问题台账中查看。`
2. **投递记录页**：渠道列展示行内评论渠道（字典中文）；行内记录支持补发；「变更编号」列已有，issue_id 作为隐藏关联不加列。
3. **问题详情抽屉**：新增「行内评论」状态行——已发布（绿色，可跳转平台评论）/ 待投递 / 自动重试中 / 待人工处置 / 未发布（问题不满足门槛或平台不支持时注明原因）。
4. **总结评论**：范围段追加行内预告（D5）。
5. **任务/记录详情**：DeliveryStatusView 增行内投递小节（成功 x / 失败 y / 待投递 z，失败可展开原因）。

## 6. 参数、字典、常量

| 类别 | 内容 |
|---|---|
| 常量 | `ReviewDeliveryConstants` 增 4 个行内渠道常量、`isInlineCommentChannel`、`inlineChannelForProvider`、`inlineIdempotencyKey(provider, projectId, issueId)`、`SNAPSHOT_KIND_INLINE_COMMENT` |
| 字典 | review_delivery_channel +4 行内渠道 |
| 标记 | `<!-- acr:inline:issue-{issueId} -->` |

## 7. 幂等、重试与审计

- 意图键唯一（DB 唯一约束已有 idempotency_key）+ 标记查找 + external_id 三层防重（D2）；
- 重试走 S3 状态机（退避、租约、上限转 MANUAL），不新建重试机制；
- 人工补发复用投递记录页「补发」（requeue）；
- 审计：意图写入/状态流转沿用 review_delivery_record 既有字段（create_by/trigger_source/last_attempt_time）。

## 8. 范围 / 非范围

**范围**：PR 线行内评论（四平台适配 + 能力降级）、项目开关与严重度门槛、意图入队与调度执行、问题详情/投递记录/总结评论/任务详情的展示联动、迁移 SQL。

**非范围**：push 线 commit 评论、行内讨论串回复、问题关闭后评论自动撤回/标注、status check / 质量门禁、按问题数限流、行内评论模板自定义。

## 9. 实现步骤与验证

| 步 | 内容 | 验证 |
|---|---|---|
| 1 | 迁移 SQL（36）+ 项目两列 + 渠道字典 + 常量 | dev 库执行；幂等重跑无副作用 |
| 2 | GitInlineCommentRequest + 接口 default 扩展 + GitHub/GitLab/Gitea 实现 + Gitee 尽力而为 | 四平台单测（载荷/参数构造断言） |
| 3 | IntentService.enqueueInlineComments + Dispatcher 行内分支 + 渲染器 | 单测：门槛过滤、issueId 幂等键、标记查找防重、push 短路 |
| 4 | 总结评论行内预告文案 + 项目设置表单 | 单测 + `npm run build:prod` |
| 5 | 问题详情/投递记录/任务详情展示联动 | 前端构建 + 页面走查 |
| 6 | 端到端联调 | 见 §11 |

## 10. 与企业级架构修复任务的协同边界（冲突规避）

| 区域 | 架构任务 | 本切片 | 纪律 |
|---|---|---|---|
| engine 包 / LlmCallService / OkHttpUtils / WorkspacePreparer / 执行服务 | S1/S2 进行中 | **不碰** | — |
| delivery 包（IntentService / Dispatcher / Constants） | S3 已合入，后续可能继续打磨 | **只加不改**：新增方法与分支，不重构既有方法签名与状态机 | Cursor 改这三个文件时仅追加；发现需要改既有逻辑时停下来上报 |
| sql/ | 34/35 已入库 | 36 独立脚本，沿用同款幂等存储过程风格，不改 34/35 | — |
| 前端 | 投递页「下次处理时间」等已就位 | 在既有页面追加展示，不重排既有列 | — |

## 11. 验收标准

1. 开启行内评论的项目：PR 审查 SUCCESS 后，CRITICAL/HIGH 问题各得一条行内评论，定位到文件与行；中低问题不发行内；
2. 同一问题二次/多次审查不重复发评论（意图键 + 标记查找双验证）；
3. 补发（requeue）不产生重复评论（先查标记）；
4. 行内投递失败：总结评论与 IM 不受影响；重试退避正常，上限转 MANUAL；
5. push 任务零行内意图；未开启项目零行内意图；
6. 问题详情可见行内评论状态与平台跳转；投递记录可见行内渠道记录；
7. Gitee 能力实测：支持则闭环，不支持则 SKIPPED 且说明清晰；
8. PR 线存量行为（总结评论/IM/台账）零回归，`mvn test` 全绿。

## 12. 后续演进（本期不实现）

- **commit 评论**（push 线的精确交付，M10 §14 已立项）：复用本切片渲染器与意图模式，渠道换 commit comment API；
- **问题关闭回标**：问题关闭/误报后在原行内评论追加「已关闭」回复（平台 discussion 能力允许时）；
- **status check / 门禁影子**：核心版门禁切片，与本切片正交；
- **行内评论模板与限流**：真实试点反馈后再评估。
