# 统一审查评分与结构化结果协议 — 设计方案

> **状态：已实施并通过后端测试与前端生产构建（2026-08-01）**  
> 前置：`docs/planning/review-template-config.md`、`docs/planning/review-pipeline-m3.md`  
> 目标：在现有项目审查模板 / 任务执行 / 结果展示结构内，建立统一评分标准与可版本化 JSON 结果协议，为后续质量看板、报表、低分预警与通知提供稳定数据基础。

## 0. 目标与成功指标

### 业务目标

所有大模型审查（内置与自定义模板）产出同一口径的评分与 Top 3 重点问题；后端解析校验后落库标准化结果；前端按结构化数据展示，不再依赖原始 JSON 或模型自生成 Markdown。

### 成功指标

1. 五维评分（40/30/20/5/5）与总分由后端校验并重算，不信任模型汇总总分；
2. Top 3 重点问题字段完整，数量口径为「重点问题数」0～3；
3. 公共协议与模板正文分离：执行时平台统一追加，用户无法通过编辑模板覆盖；
4. 解析失败标记为结果格式异常（任务失败），不得当作成功结果；
5. 历史任务继续使用建单时模板正文快照；协议版本单独快照；
6. 预警分数线字段预留为空，详情页不展示「是否低于分数线」；本次不做通知。

---

## 1. 已锁定决策

| 项 | 决策 |
|---|---|
| 分数线展示 | 保存总分/维度分；`score_threshold` 预留 NULL；详情不展示是否低于分数线 |
| PR 上下文 | 执行时调 GitHub API 拉取 PR body + commits message，写入 Prompt，并落库截断摘要 |
| 公共协议落点 | 平台代码管理，执行时统一追加；模板正文只保留技术栈审查重点 |
| 旧协议 | 不保留 `summary/comments` 双轨解析；升级后只维护新协议 |
| 旧自定义模板 | 无需手工迁移；正文继续作技术要求，平台剥离冲突输出指令后追加新协议 |
| OCR 路径 | 不改造，仍走引擎结构化结果 |

---

## 2. 职责拆分

```text
模板正文（用户可编）     = 技术栈审查重点
平台公共协议（代码管理） = 评分标准 + Top3 规则 + 严格 JSON 协议 + Schema
最终 Prompt（执行时）    = 清理冲突输出指令后的模板正文
                         + PR/Commit/Diff 上下文
                         + 公共协议附录
```

关系：

- 模板管理页：编辑区只改技术栈正文；只读展示「平台统一审查规则」（API：`GET /review/template/platform-rules`）；
- 建单：仍冻结模板 `version_no` + 模板正文（技术栈部分）；
- 执行：基于任务快照正文 → 剥离冲突 → 渲染占位符（含 PR 描述/Commit）→ 追加公共协议 → 调用模型 → 解析标准化 → 落库。

---

## 3. 统一评分标准

| 维度编码 | 名称 | 满分 | 评估说明 |
|---|---|---|---|
| `CORRECTNESS` | 功能正确性与健壮性 | 40 | 功能实现、边界条件、异常处理和容错能力 |
| `SECURITY` | 安全性与潜在风险 | 30 | 注入、越权、敏感信息泄露及其他安全风险 |
| `PRACTICE` | 最佳实践与可维护性 | 20 | 代码结构、复杂度、重复代码、命名和维护成本 |
| `PERFORMANCE` | 性能与资源利用 | 5 | 明显性能问题、资源泄漏和无效消耗 |
| `COMMIT_QUALITY` | 提交信息质量 | 5 | PR 描述和 Commit Message 的清晰度与完整性 |
| — | 总分 | 100 | — |

规则：

- 各维度直接按对应满分评分，不做百分制二次加权；
- 模型返回各维度得分与简短理由；
- 总分 = 后端对各维度得分（校验在 `[0, maxScore]` 内后）求和；忽略模型自报 `totalScore`。

权重快照：执行时将上述满分写入 `score_weights_json`，供历史任务追溯。

---

## 4. Top 3 重点问题

- 仅保留最重要的最多 3 个问题，按影响程度排序；
- 统计与文案统一为「重点问题数」，范围 0～3，禁止表述为「全部问题数量」；
- 每项至少：`rank`、`severity`、`category`、`title`、`description`、`filePath`、`startLine`、`endLine`、`evidence`、`suggestion`；
- 文件/行号无法确定时允许为空；禁止伪造位置；
- 后端对超长列表截断为 3，并重写 `rank` 与 `focusIssueCount`。

严重程度建议枚举：`CRITICAL` / `HIGH` / `MEDIUM` / `LOW` / `INFO`（入库前规范化大小写）。模型偶发的别名按兼容映射处理后再校验：`ERROR`/`BLOCKER`/严重/阻断 → `CRITICAL`，`WARNING`/`WARN`/警告 → `MEDIUM`；无法映射的值判为结果格式异常。

---

## 5. JSON 结果协议 v1.0

### 5.1 模型输出要求

- 只输出严格 JSON，禁止 Markdown 代码块、解释性前缀或其它自由文本；
- `protocolVersion` 必须为 `"1.0"`（或当前平台协议版本常量）。

### 5.2 最小字段

```json
{
  "protocolVersion": "1.0",
  "scores": [
    { "dimension": "CORRECTNESS", "score": 0, "maxScore": 40, "reason": "..." },
    { "dimension": "SECURITY", "score": 0, "maxScore": 30, "reason": "..." },
    { "dimension": "PRACTICE", "score": 0, "maxScore": 20, "reason": "..." },
    { "dimension": "PERFORMANCE", "score": 0, "maxScore": 5, "reason": "..." },
    { "dimension": "COMMIT_QUALITY", "score": 0, "maxScore": 5, "reason": "..." }
  ],
  "totalScore": 0,
  "summary": "审查摘要",
  "topIssues": [],
  "focusIssueCount": 0,
  "hasCriticalSecurityIssue": false
}
```

### 5.3 代码资产

- DTO：`ReviewScoreResult` 及嵌套类型（维度分、重点问题）；
- Schema：classpath 资源（如 `review/schema/review-score-result-v1.json`），与协议版本绑定；
- 语言模板不得各自定义不同返回结构。

---

## 6. 解析、结论与失败语义

### 6.1 解析流程

1. 提取纯 JSON（若含围栏则尝试剥离一层，仍失败则格式异常）；
2. 反序列化为 DTO，校验必填与枚举；
3. 校验五维齐全、满分与平台一致、得分范围；
4. 重算总分；校正 Top3 与 `focusIssueCount`；
5. 成功：`parse_status=SUCCESS`；失败：`parse_status=FAILED` + `parse_error`。

### 6.2 任务状态

| 情况 | task/run 状态 | failureType |
|---|---|---|
| 模型调用失败 | FAILED | `MODEL_CALL_FAILED` |
| JSON/字段/评分校验失败 | FAILED | `RESULT_FORMAT_INVALID` |
| 解析成功 | SUCCESS | — |

解析失败仍保存 `raw_response_excerpt` 与规范化失败说明，**不得**把无法解析内容写成成功 `result_json` 供报表使用。

### 6.3 审查结论（LLM 路径，本期）

- `hasCriticalSecurityIssue == true` → `BLOCK`
- 否则 Top3 中存在 `CRITICAL`/`HIGH` → `WARN`
- 否则 → `PASS`

不使用分数线。OCR 路径继续使用现有 `ReviewConclusionResolver`。

---

## 7. 数据落库

在现有 `review_task` / `review_task_run` 增量字段（命名以实现 SQL 为准）：

| 字段组 | 说明 |
|---|---|
| 评分 | `total_score`、五维分列或等价可查询结构 |
| 协议 | `protocol_version`、`score_weights_json`、`score_threshold`（NULL） |
| 问题 | `focus_issue_count`、`has_critical_security`、`top_issues_json` |
| 解析 | `parse_status`、`parse_error`、`raw_response_excerpt` |
| Prompt | `rendered_prompt`（最终完整提示词，超长截断）；建单模板正文快照保持不变 |
| 上下文摘要 | `pr_description`、`commit_messages`（截断） |
| 结果 | `result_json` = **规范化**结果；`result_summary` 用中文摘要 |

同时：任务表同步最新一次成功/失败的关键评分摘要字段，便于列表扩展（可选，至少 run 表完整）。

密钥、PAT、Webhook Secret 永不入库快照。

---

## 8. Prompt 占位符与上下文

现有：`{{pr_title}}` `{{source_branch}}` `{{target_branch}}` `{{base_sha}}` `{{head_sha}}` `{{diff}}`

新增：

- `{{pr_description}}`：PR 描述（body）
- `{{commit_messages}}`：本次变更相关 commit message 列表（截断拼接）

获取：执行阶段 GitHub API（PR 详情 + commits），失败则按可重试/配置类失败处理或空串+备注（实现时优先明确失败，避免静默缺上下文导致提交信息维度失真；若 API 部分失败可降级为空并在摘要注明「未能获取提交信息」——默认：**PR 元数据拉取失败不阻断审查，描述/提交记空并在 rendered 上下文标注缺失**，保证 Diff 审查可继续）。

> 实现口径（确认）：Diff 成功即可继续；PR body/commits 拉取失败时记空串并在 Prompt 中标注「未获取到」，不导致整任务失败。

---

## 9. 冲突指令清理

已知旧模板输出块特征（示例级匹配）：

- 「若可能，按 JSON 返回：」及后续旧 `summary/conclusion/comments` 示例；
- 「请用中文输出可执行建议」后紧跟旧 JSON schema 段。

策略：

1. SQL 升级内置 6 套：正文改为纯技术栈重点 + 上下文占位（无输出协议），`version_no` +1；
2. 运行时对任意模板正文做确定性剥离（正则/标记块），再追加平台协议；
3. 不保留旧解析兼容。

---

## 10. 前端展示

任务详情（大模型成功结果）：

- 总分；
- 五个维度得分 + 理由；
- 审查摘要；
- Top 3 重点问题卡片/列表；
- 模板、模型、协议版本、模板版本、耗时等执行信息；
- 原始响应：具备诊断权限时折叠查看（复用 `review:task:query` 或现有详情权限，不新增菜单）。

不展示：原始 JSON 作为主视图、「是否低于分数线」、把 JSON 先转 Markdown。

模板页：只读展示「平台统一审查规则」（五维评分说明 + Top 3 规则 + 维护提示）；数据来自 `ReviewScoringConstants.platformRulesForUi()`，与执行 Prompt 附录同源。不对普通用户展示 JSON Schema。

---

## 11. 模块与类落点

```text
acr-review
  ├─ domain/result/ReviewScoreResult（及子类型）
  ├─ service/ReviewScoringConstants（维度/权重/协议版本）
  ├─ service/ReviewPromptComposer（清理 + 拼接公共协议）
  ├─ service/ReviewPromptRenderer（扩展占位符）
  ├─ service/ReviewScoreResultParser（解析/校验/重算）
  ├─ service/ReviewConclusionResolver（LLM 分支或新 ScoreConclusionResolver）
  ├─ git/... PullRequestMetadataFetcher（PR body + commits）
  └─ ReviewTaskExecutionServiceImpl（编排接入）

acr-admin：无新业务逻辑，详情接口透传字段

acr-ui：任务详情结构化展示；模板页协议说明
```

不新增 Maven 模块；不建通知/预警配置。

---

## 12. 非范围

- 通知策略、预警分数线配置、投递记录；
- 规则引擎、拖拽 Prompt、自动调分、AI 改模板；
- GitHub 评论 Markdown 回写（后续由平台按结构化结果渲染）；
- OCR 引擎结果协议改造；
- 模板多版本历史表（任务/run 快照足够）。

---

## 13. 验收清单

- [x] 后端单测：评分范围、总分重算、JSON 解析、Top3 限制、冲突剥离、协议追加、解析失败
- [x] `mvn test` 通过
- [x] `cd acr-ui && npm run build:prod` 通过
- [x] SQL 幂等升级内置模板与表字段（`sql/16_review_scoring_result_protocol.sql`）
- [x] 文档与路线图基线同步
- [x] 详情页结构化展示；诊断区可看原始响应摘要
- [x] 分数线字段为空且 UI 不展示低于分数线

---

## 14. 后续演进（本次不做）

在「通知配置」独立建设触发条件（总分/维度/严重安全/重点问题数/执行或解析失败等）；通知匹配记录保存策略版本与实际分数线；阈值不写入模板与提示词。
