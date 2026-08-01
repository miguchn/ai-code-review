# P0/M3 真实代码审查全流程打通设计

> **实施状态（2026-08-01）：主链路与双审查方式整改已落地并通过后端测试与前端生产构建。**
> **加固（2026-08-02 独立 Review）：run 建立在配置解析前并回填快照方式，任意执行异常落 FAILED 不再僵尸；RUNNING 超 30 分钟可重试回收；PAT 改经 GIT_CONFIG_* 环境变量注入不再进进程参数；SHA 入口校验；Diff 有界读取；GitHub 限流独立失败类型；任务详情/重试补部门数据范围校验；结果解析容错（围栏/配平/边界）与模板单趟渲染；移除未使用的模板复制端点。**
> 真实 GitHub PR 端到端验收仍依赖已接入项目、可用 PAT、模型服务 / OCR CLI 与可达 Webhook。本切片不含 PR 评论回写、通知中心、问题台账完整整改与多引擎降级。

## 1. 目标与范围

闭环：

```text
GitHub PR 事件
  → Webhook 验签/去重/匹配/建单（复用 M2）
  → 事务提交后异步领取执行
  → 按项目审查方式二选一分流：
       · LLM_DIRECT：拉取 PR Diff → 渲染提示词 → LlmCallService
       · OCR_ENGINE：准备 Git 工作区 → open-code-review（运行时注入平台默认模型）
  → 保存结构化结果、执行快照与耗时
  → 更新任务状态；后台查看详情并可安全重试
```

本次不做：GitHub 评论回写、通知中心、规则编排、多引擎调度、多模型降级、问题台账完整整改流、Push/全量扫描、第二 Git Provider。

## 2. 双审查方式（互斥）

| 方式 | 编码 | 项目必填 | 执行链路 |
|---|---|---|---|
| 大模型审查 | `LLM_DIRECT` | `model_id` + `template_id` | GitHub Diff API → 模板渲染 → 平台模型服务 |
| 审查引擎 | `OCR_ENGINE` | `engine_code=OPEN_CODE_REVIEW` | Git fetch 工作区 → OCR CLI；**不**使用平台审查模板 |

约束：

- 禁止把大模型与审查引擎设计成必须同时配置的上下级；
- 历史值 `OCR_PR_DIFF` 运行时与数据迁移均视为 `OCR_ENGINE`；
- **建单时**冻结方式/模板版本正文/模型或引擎；执行只读任务快照；模板修改只影响新任务。审查模板详见 `docs/planning/review-template-config.md`。

## 3. 关键产品决策

1. **引擎路径保留真实 Git 工作区**（OCR `--from/--to` 需要仓库上下文）。
2. **大模型路径使用 Diff + 平台提示词**，降低对本地 OCR 的依赖。
3. **执行状态与审查结论分离**：`task_status` / `review_conclusion`。
4. **每次执行一条 run 记录**，重试追加 attempt。
5. **Webhook 线程不跑审查**：事务提交后异步调度。

## 4. 状态与步骤

任务状态：`PENDING → RUNNING → SUCCESS / FAILED`；建单后异步自动执行。`PENDING`（调度丢失或进程重启后卡住）与 `FAILED` 均可人工触发执行（CAS 领取）。

执行步骤：

- 共用：`RESOLVE_CONFIG → PREPARE_WORKSPACE`
- 引擎：`INVOKE_ENGINE → PERSIST_RESULT`
- 大模型：`INVOKE_MODEL → PERSIST_RESULT`

## 5. 数据对象

- `review_prompt`：提示词管理（菜单挂在「模型服务」下）
- `review_project`：`review_mode`、`model_id`/`prompt_id`（仅大模型）、`engine_code`（仅引擎）
- `review_task` / `review_task_run`：步骤、失败分类、attempt、快照（含提示词正文）

密钥、PAT、Webhook Secret 仍不进快照、响应与日志。

## 6. 模块边界

```text
acr-admin  ReviewProject / ReviewPrompt / ReviewTask Controller
acr-review
  ├─ ReviewProjectService（双方式校验与表单选项）
  ├─ ReviewPromptService
  ├─ ReviewTaskExecutionService（双路径编排）
  ├─ GitPullRequestWorkspacePreparer / GitPullRequestDiffFetcher
  └─ OpenCodeReviewCliAdapter / LlmCallService
```

## 7. 验收清单

- [x] 后端测试通过
- [x] 前端生产构建通过
- [x] 项目新增/编辑分区：基础信息 / 仓库与分支 / Webhook / 审查执行
- [x] 审查方式二选一，页面与执行链路一致
- [x] 提示词管理与历史快照隔离
- [x] 任务详情/重试按权限与状态判断，禁用有中文说明
- [x] SQL `14_review_dual_mode_prompt.sql` 幂等可执行
- [ ] 真实 GitHub PR `opened/reopened/synchronize` 双方式端到端（需环境）

## 8. 已知边界

- `RUNNING` 任务超过 30 分钟视为中断，可人工重试回收（claimTask 超时领取）；无自动巡检回收
- 快照冻结上线前建单的历史任务无执行快照：执行时按项目当前配置补冻结并落库后继续；项目未配置审查方式时失败并给出配置指引，不再报「不支持的审查方式：null」
- 已知体验问题（2026-08-02 人工验收反馈）：任务详情把审查任务与执行记录（run）同页混排，信息密度过高不利于阅读；后续版本迭代拆分任务概览与执行记录视图
- 大仓库 fetch / Diff 受超时与截断限制（Diff 原始字节读取上限 800KB，渲染再截断至 400K 字符）
- 结构化结果 JSON 限长 512KB
- 审查结论为启发式基础判定，非正式质量门禁
- 引擎路径仍依赖平台默认模型作为 OCR 运行时模型
- GitHub API 限流识别为独立失败类型 `RATE_LIMIT`（429 或 403 且额度归零）
