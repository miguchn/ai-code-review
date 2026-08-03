# M4 GitHub PR 审查结果回写：总结评论与投递记录

> **状态（2026-08-03）：实施中。** 前置：`docs/planning/review-pipeline-m3.md`、`docs/planning/review-scoring-result-protocol.md`、`docs/planning/review-scope-policy-m3.2.md`、`docs/planning/review-record-experience-m3.1.md`。本设计明确审查成功后的 PR 总结评论回写、投递幂等与失败重试；实现按第 8 节分步计划推进。

## 1. 目标与成功指标

- 审查成功后，开发者在 GitHub PR 上即可看到低噪声总结评论（结论、总分、Top3 带新增/存量标签、范围统计一行），无需离开代码平台；
- 同一 PR 永远只有一条 ACR 总结评论：回写前按固定 HTML 标记查找已有评论，有则更新、无则新建；
- 投递事实独立落库（`review_delivery_record`），与审查任务状态分离；回写失败不回滚审查结论；
- 失败可在后台单独重试（重试前重新查已有评论），密钥不进日志、评论与错误信息。

## 2. 当前基线与缺口（2026-08-03 核查）

| 链路 | 现状 | 缺口 |
|---|---|---|
| 审查执行 | `ReviewTaskExecutionServiceImpl` 在 `persistSuccess` / `persistLlmSuccess` / `persistEmptyScopeSuccess` 后任务落 `SUCCESS`；失败走 `fail` 落 `FAILED` | 成功后无外部副作用钩子 |
| GitHub 适配 | `GitHubProvider` / Diff / Metadata / FileContent 均用 OkHttp + PAT；尚无 Issue/PR 评论 API | 缺 list/create/update comment |
| 结构化结果 | LLM 路径有结论、总分、Top3（含 `origin`）、`scopeStats`；OCR / 空范围成功字段可能部分缺省 | 评论渲染需对缺省字段友好 |
| 投递事实 | 无交付表；路线图 §3.2 要求交付记录独立幂等键与状态 | 需新建 `review_delivery_record` |
| 前端 | 任务/记录详情「执行记录」区展示 attempt 技术信息；M3.1 明确「不展示交付状态（待 M4）」 | 需展示投递状态与失败重试 |
| 权限 | `review:task:retry` 管任务重跑 | 投递重试建议独立权限，避免与任务重跑语义混淆 |

关键存量资产：项目加密 PAT、`CredentialCryptoService` 脱敏、任务详情数据范围校验、记录详情 `执行记录` Tab、字典与菜单幂等 SQL 模式。M4 在审查成功落库之后叠加投递，不改写 M1–M3.2 审查行为。

## 3. 本次范围 / 非范围

### 3.1 范围

1. 审查 `task_status=SUCCESS` 后异步/同步投递 GitHub PR **Issue 级总结评论**（非 inline review comment）；
2. 评论 Markdown：结论、总分、Top3（带新增/存量）、范围统计一行；正文含固定标记；
3. 回写前列出 PR 评论并按标记匹配：命中则 PATCH 更新，否则 POST 新建；
4. `review_delivery_record` 表 + 幂等 SQL `sql/23_review_delivery_record.sql`，同步 `docs/deployment.md`、`sql/README.md`；
5. 投递失败不影响任务 SUCCESS；后台重试入口；前端执行记录区展示投递状态；
6. 单测覆盖评论渲染与幂等更新；`mvn test` + `npm run build:prod`；CHANGELOG 补 M4。

### 3.2 非范围

- inline 评论、GitHub Status Check、Checks API、合并卡点；
- 通知渠道（钉钉/企微/飞书）、通用多渠道抽象；
- 第二 Git Provider、新 Maven 模块/框架/依赖；
- 改写 M1–M3.2 审查范围、评分协议或执行骨架行为；
- 预建未使用的表字段、实体或「通知中心」菜单。

## 4. 依赖与待决策项（设计定稿）

| 项 | 定稿 |
|---|---|
| 触发条件 | 仅 `task_status=SUCCESS`（含空范围 PASS、OCR 成功）；`FAILED`（前端称「执行失败」）**不发评论** |
| 评论类型 | GitHub Issue Comment（`/repos/{owner}/{repo}/issues/{pr}/comments`），同一 PR 会话可见 |
| 持久化策略 | PR 级幂等键 + 评论正文固定标记双重保障「一 PR 一条」 |
| 投递时机 | 审查结果事务/落库成功之后调用投递用例；投递异常 catch 落失败记录，**不**改任务状态 |
| 重试权限 | 新增 `review:delivery:retry`（挂在审查记录菜单下），不复用 `review:task:retry` |
| 重试内容来源 | 一律以「该 PR 最近一次 SUCCESS 任务」的结论渲染；路径中的 taskId 仅用于权限校验与定位项目/PR，防止旧结论覆盖最新评论 |
| OCR / 缺字段 | 缺总分显示 `--`；无 Top3 显示「暂无重点问题」；无 scopeStats 显示「范围统计：—」 |
| 并发 | 同 PR 并发投递极少见；以 DB 唯一键 +「先查标记再写」收敛；冲突时更新已有行 |

## 5. 数据、接口、权限和流程

### 5.1 评论标记与正文模板

固定标记（HTML 注释，渲染不可见）：

```text
<!-- acr-review-summary -->
```

正文结构（Markdown）：

```markdown
## AI Code Review 审查结论

| 项目 | 内容 |
|---|---|
| 结论 | 通过 / 建议修改 / 高风险 |
| 总分 | 87 / 100 |
| 任务 | #{taskId} · `{headSha短}` |

### Top 3 重点问题

1. **[高][新增]** 标题 — `path` L12-14
   - 说明…
2. …

### 范围统计

纳入 3 · 排除 2 · 扩展 1 · 新增问题 2 · 存量 1

---
*由 AI Code Review 自动生成并更新；请勿手动删除本标记评论。*
<!-- acr-review-summary -->
```

约束：

- 结论文案与后台一致：`PASS→通过`、`WARN→建议修改`、`BLOCK→高风险`；
- Top3 的 `origin`：`NEW→新增`、`EXISTING→存量`，缺省按新增；
- 不写入 PAT、API Key、Webhook Secret、完整 Prompt 或模型原始响应；
- 失败原因与日志脱敏沿用现有 Token 正则兜底。

### 5.2 表 `review_delivery_record`

| 列 | 说明 |
|---|---|
| `delivery_id` | PK |
| `task_id` | 最近一次发起/成功投递所关联的审查任务 |
| `run_id` | 最近一次关联的 `review_task_run`（可空，兼容历史） |
| `project_id` | 项目 |
| `provider` | 固定 `GITHUB`（本期） |
| `channel` | `GITHUB_PR_SUMMARY_COMMENT` |
| `pr_number` | PR 号 |
| `idempotency_key` | 唯一键，格式见下 |
| `external_id` | GitHub comment id（成功后必填） |
| `delivery_status` | `SUCCESS` / `FAILED` |
| `failure_message` | 失败原因（≤500，已脱敏） |
| `attempt_count` | 投递尝试次数 |
| `last_attempt_time` | 最近尝试时间 |
| `create_time` / `update_time` / `create_by` / `update_by` | 审计字段 |

幂等键：

```text
GITHUB:{projectId}:{prNumber}:SUMMARY_COMMENT
```

含义：一个项目下的一个 PR，在「总结评论」渠道只有一行投递记录；新任务成功时更新该行的 `task_id`/`run_id`/状态/外部 id，而不是新建多条刷屏。

状态语义：

| 状态 | 含义 |
|---|---|
| `SUCCESS` | 评论已创建或更新成功 |
| `FAILED` | 最近一次尝试失败，可重试 |

投递是同步调用，无中间等待态：不预插记录，投递结束（成功或失败）后一次性 upsert，避免进程中断留下无法重试的僵尸行。

字典：`review_delivery_status`（投递状态）随 SQL 初始化。

### 5.3 模块边界

```text
acr-admin
  ReviewDeliveryController（重试 REST + 权限）
acr-review
  ├─ delivery/
  │    ReviewCommentBodyRenderer      # 纯函数渲染 Markdown
  │    ReviewDeliveryConstants        # 标记、渠道、状态
  │    domain/mapper/ReviewDeliveryRecord*
  ├─ git/
  │    GitPullRequestCommentClient    # 能力接口（list/create/update）
  │    github/GitHubPullRequestCommentClient
  ├─ service/
  │    IReviewDeliveryService
  │    impl/ReviewDeliveryServiceImpl # 幂等查建、调用适配、落库
  └─ ReviewTaskExecutionServiceImpl   # SUCCESS 后调用 deliver，异常不外抛改任务状态
```

- Controller 不访问 Mapper；
- 不新建 Maven 模块；GitHub 差异只在 `git/github`；
- 本期只有一个渠道实现，**不**建通知/多渠道框架。

### 5.4 主流程

```text
审查执行落库 SUCCESS
  → ReviewDeliveryService.deliverAfterSuccess(task, run)
       → 解密 PAT（不入日志）
       → list issue comments → 找含标记的评论
       → 渲染 Markdown
       → 有则 update(commentId)，无则 create → 记 external_id
       → 投递结束后 upsert 投递记录（幂等键，SUCCESS，attempt++，external_id）
       → 任一步失败 → upsert FAILED + failure_message
  → 调用方吞掉投递异常（仅 log），任务保持 SUCCESS
```

人工重试：

```text
POST /review/delivery/{taskId}/retry
  → 权限 review:delivery:retry + 项目数据范围
  → taskId 仅用于权限校验与定位项目/PR
  → 找该 PR 最近一次 SUCCESS 任务，以其结论渲染（防旧结论覆盖新评论）
  → 重新 list 评论（不信任仅 DB 中的 external_id：评论可能被删）
  → 有标记则更新；无则新建；更新投递记录
```

若 DB 有 `external_id` 但 GitHub 上已删且无标记评论 → 新建并回写新 `external_id`。

### 5.5 接口与权限

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/review/delivery/{taskId}/retry` | `review:delivery:retry` | 失败（或需刷新）重试投递 |
| （透出） | 任务/记录详情已有 GET | 原权限 | 详情附加 `delivery` 摘要对象 |

详情透出字段（挂在任务详情/记录详情响应，不另开列表页）：

```json
{
  "delivery": {
    "deliveryId": 1,
    "deliveryStatus": "FAILED",
    "externalId": "123456",
    "failureMessage": "GitHub API 超时",
    "attemptCount": 2,
    "lastAttemptTime": "..."
  }
}
```

无投递记录时 `delivery=null`（历史 SUCCESS 任务在 M4 前完成、或 FAILED 任务）。

菜单：审查记录（128）下新增按钮 `1150`「投递重试」`review:delivery:retry`；角色 2 同步授权（与记录查询一致）。

### 5.6 前端（执行记录区）

在任务详情「执行记录」与记录详情「执行记录」Tab **上方**增加「PR 评论投递」区块（风格对齐现有 `detail-section` / `el-descriptions` / `el-tag`，不新开页面）：

- 状态标签：已投递 / 投递失败 / —（无记录）；
- 展示最近尝试时间、失败原因（失败时）；
- `FAILED` 且具备 `review:delivery:retry` 时显示「重试投递」按钮；
- SUCCESS 任务无 delivery 行时显示「—」（不伪造成功）。

## 6. 失败分类与安全

| 场景 | 处理 |
|---|---|
| GitHub 4xx/5xx / 超时 / 限流 | 投递 `FAILED`，任务仍 `SUCCESS`；限流文案可提示稍后重试 |
| PAT 无效/过期 | `FAILED`，中文原因不含 token 正文 |
| 评论被人工删除 | 重试时按标记找不到 → 新建 |
| 标记被改坏导致双评论 | 以「第一条含标记」为准更新；设计文档注明勿删标记 |
| 任务 FAILED | 不创建投递、不调 GitHub |

安全：PAT 仅内存使用；错误消息复用现有脱敏；评论与日志禁止回显密钥。

## 7. 测试要点

- `ReviewCommentBodyRenderer`：结论映射、缺字段、Top3 origin 标签、范围统计一行、固定标记始终存在；
- `ReviewDeliveryService`：无已有评论 → create；有标记 → update；GitHub 失败不改 task_status；幂等键冲突更新同行；重试前重新 list；重试以最近 SUCCESS 任务结论渲染；
- `GitHubPullRequestCommentClient`：请求路径/方法与 Authorization 头（可用 MockWebServer 或现有 OkHttp mock 风格）；
- 前端：生产构建通过即可（无强制 E2E）。

## 8. 分步实现计划

| 步 | 内容 | 验证 |
|---|---|---|
| 1 | 设计文档定稿（本文） | 人工确认范围与表结构 |
| 2 | SQL `23_review_delivery_record.sql`（建表 + 字典 + 权限）+ 更新 `docs/deployment.md`、`sql/README.md` | 脚本幂等、utf8mb4；清单连续编号 |
| 3 | 领域对象 / Mapper / 常量 / `ReviewCommentBodyRenderer` + 单测 | 渲染单测绿 |
| 4 | `GitPullRequestCommentClient` + GitHub 实现 + 投递服务（幂等 create/update） | 投递与幂等单测绿 |
| 5 | 执行链 SUCCESS 后挂钩；失败隔离；重试 API + Controller | 全量 `mvn test`；执行失败不发评论用例 |
| 6 | 详情透出 delivery；前端执行记录区展示与重试；CHANGELOG | `npm run build:prod` |

每步可独立提交；步 2–3 合入不改变线上行为，步 5 接入后生效。

## 9. 验收标准与风险

**验收**：

- 真实或模拟：SUCCESS 任务在对应 PR 产生/更新一条含标记的总结评论；
- 同 PR 再次 SUCCESS（synchronize 新任务）只更新原评论，不新增第二条 ACR 评论；
- FAILED 任务无评论、无强制投递记录；
- 人为制造 GitHub 失败 → 任务仍 SUCCESS，投递 FAILED，后台重试可恢复；
- `mvn test` 全绿；`cd acr-ui && npm run build:prod` 通过；CHANGELOG 有 M4 条目；
- 分支不合并 main，等待 Review。

**风险与对策**：

- GitHub 评论列表分页：首版拉取前若干页（如每页 100、最多 3 页）查找标记；找不到则新建（极端刷屏 PR 需后续加强）；
- 超大 Top3 描述撑爆评论：单字段截断（如描述 ≤500 字），保证评论整体可读；
- 与任务重试混淆：文案区分「重新执行审查」与「重试投递」；
- 历史 SUCCESS 无投递行：前端显示 —，不自动回填（避免无提示刷历史 PR）；若需补投递，运维可对单任务点重试（SUCCESS 且无记录时允许创建）。

## 10. 非范围（再声明）

inline 评论、Status Check、通知渠道、多 Provider、通用投递框架、问题台账整改闭环、质量门禁联动。
