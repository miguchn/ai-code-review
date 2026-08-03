# M6.1 问题处置与投递记录追溯打磨

> **状态（2026-08-03）：已实现，待真实环境验收。** 修订三点：①`rerenderSummaryComment` 明确为接口签名变更并列出存量测试影响面（§4.3）；②IM 渠道 trigger_source 钉死为首次 `TASK_SUCCESS` / 补发 `MANUAL_RETRY`、不留 NULL（§4.2）；③失败原因展示钉死为截断 + tooltip/展开，删除行内换行选项（§1.4/§3.2）。前置：`docs/planning/issue-ledger-m6.md`、`docs/planning/review-comment-writeback-m4.md`、`docs/planning/notification-management-m5.md`。本设计解决 M6 处置后 PR 总结评论重渲染失败时，「处置动作 → 外部副作用」追溯链断裂的问题：提示可定位、投递记录可识别、触发来源可区分；**不**新建表/尝试历史、**不**新增通知事件、**不**新增权限串。

## 1. 用户场景与验收标准

### 1.1 产品结论（一句话）

用户在问题台账完成确认/关闭/忽略/误报后，若 GitHub 总结评论同步失败，必须能立刻看到**具体失败原因**，并一键进入投递记录页找到**刚才那次失败**的记录去补发；台账详情与投递列表要能回答「这条评论投递是任务回写、问题处置，还是手动重试触发的」。

### 1.2 目标用户与场景

| 角色 | 场景 | 今天卡在哪 | 期望 |
|---|---|---|---|
| 项目负责人 / 质量负责人 | 在问题台账关闭或标误报后，黄色提示「评论同步失败，可在投递记录重试」 | 不知道为什么失败；打开投递记录后，同一 PR 只有一条总结评论记录（幂等键原地更新），无法确认「是不是刚才处置触发的」；列表失败原因被截断 | 提示展示具体原因；可跳转投递记录页；该记录因「最后尝试时间」倒序自然在首行；可直接补发 |
| 平台运维 | 排查「处置成功但 PR 评论仍是旧态」 | 投递记录看不出触发来源（任务成功回写 / 处置重渲染 / 手动重试），与 M5 IM 行混在同一列表 | 记录带 `trigger_source`；失败原因全文可读 |
| 开发者（间接） | 在 GitHub PR 上看处置态 | 平台侧已关闭，PR 评论未更新且无人跟进 | 后台处置人能闭环补发，开发者最终在 PR 看到最新处置态 |

### 1.3 问题断点（对照现状）

```text
用户处置 issue（confirm / close / dismiss）
  → 本地事务：issue + action 已提交 ✅
  → 同步重渲染 PR 总结评论
       → 失败 → upsert review_delivery_record（同幂等键原地更新）✅
       → 接口仅返回 commentSyncStatus=FAILED ❌ 无原因、无 deliveryId
       → 前端固定文案「可在投递记录重试」❌ 无法跳转、无法对账
  → 用户打开投递记录
       → 按 last_attempt_time 倒序 ✅（失败行通常在首）
       → 但失败原因 tooltip 截断 ❌
       → 无触发来源 ❌ 认不出「刚才处置」
  → 台账详情抽屉
       → 无该 PR 总结评论投递状态 ❌ 第二入口也断
```

可复用存量：`failure_message` / `last_attempt_time` 列与列表字段已存在；`order by last_attempt_time desc` 已落地；`selectSummaryDelivery(projectId, prNumber)` 可取该 PR 总结评论投递行；权限 `review:delivery:list` / `review:delivery:retry` / `review:issue:*` 已具备。

### 1.4 验收标准

1. **提示增强**：confirm / close / dismiss 在 `commentSyncStatus=FAILED` 时，响应额外返回失败原因与 `deliveryId`；前端黄色提示展示具体原因，并提供「去投递记录」跳转（进入投递记录列表页即可，**不做** `deliveryId` / 锚点等定位参数）；成功或 SKIPPED 不误报失败。
2. **投递记录列表**：展示「最后尝试时间」与「失败原因」（单元格截断展示 + tooltip 或展开全文，在既有 `failure_message` ≤500 落库上限内完整可读，不以截断为唯一可读形态，不行内全量换行）；排序与筛选维持现状（`last_attempt_time desc, delivery_id desc`；筛选项不变）。
3. **触发来源**：`review_delivery_record` 新增可空列 `trigger_source`；三口分别赋值——M4 任务 SUCCESS 回写 → `TASK_SUCCESS`，M6 处置重渲染 → `ISSUE_DISPOSITION`，投递记录页/任务详情手动重试 → `MANUAL_RETRY`；原地更新以**最近一次尝试**来源为准；历史行保持 `NULL`、不回填；列表可读触发来源。
4. **台账详情**：问题详情抽屉展示该 PR 总结评论（`GITHUB_PR_SUMMARY_COMMENT`）的投递状态（含失败原因摘要），可跳转投递记录页（同样不做定位参数）。
5. **硬约束成立**：不新建表/尝试历史/事件流；不新增通知事件、不动 M5 渠道逻辑；不新增权限串；SQL 仅 `sql/26_*.sql` 加列；`mvn test` 与 `npm run build:prod` 通过。

## 2. 边界（做 / 不做）

### 2.1 做（三条，不多不少）

| # | 能力 | 说明 |
|---|---|---|
| 1 | 处置响应与提示增强 | 失败原因 + `deliveryId`；前端展示原因并可跳转投递记录页 |
| 2 | 投递记录列表可读性 | 「最后尝试时间」+「失败原因全文」；排序/筛选不动 |
| 3 | 触发来源 + 台账抽屉投递态 | 加列 `trigger_source` 三口赋值；抽屉展示 PR 总结评论投递状态并可跳转 |

### 2.2 不做

- 不新建表、不做尝试历史表、不做事件流；幂等键下仍**只有一条**总结评论投递记录；
- 不新增任何通知事件，不动 M5 渠道适配、策略、频控、聚合；
- 不新增权限串；复用 `review:delivery:list`、`review:delivery:query`、`review:delivery:retry` 与既有 `review:issue:*`；
- SQL 走幂等脚本 `sql/26_issue_delivery_trace_m6_1.sql`，**只加列不改列**；历史 `trigger_source` 不回填；
- 不做投递记录导出、不做按 `deliveryId` 深链定位/高亮、不做 IM 行触发来源专项治理（IM 首次投递写 `TASK_SUCCESS`、补发写 `MANUAL_RETRY`，见 §4.2；**不**为此扩范围）；
- 不改 M4「一 PR 一条总结评论」幂等键/标记，不改 M6 状态机与评论失败隔离原则（处置成功不因评论失败回滚）。

## 3. 交互

### 3.1 问题台账 · 处置后提示

```text
处置成功 toast（已有）
若 commentSyncStatus === FAILED：
  追加 warning：
    文案：评论同步失败：{failureMessage}
         （failureMessage 空则降级为「评论同步失败，可在投递记录重试」）
    操作：「去投递记录」→ router.push('/notify/delivery')
         （无 query；依赖列表默认按最后尝试时间倒序，刚失败的行在首）
若无 review:delivery:list：仅展示原因文案，不展示跳转（或跳转后由路由守卫挡回，实现期选打扰更小者）
```

成功 / SKIPPED：保持现有成功提示，不出现失败跳转。

### 3.2 投递记录列表

| 列 | 行为 |
|---|---|
| 最后尝试时间 | 展示 `lastAttemptTime`（现列名「最近尝试」统一为「最后尝试时间」） |
| 失败原因 | 单元格截断展示（1-2 行）+ tooltip 或展开查看**全文**；不得行内全量换行撑高行，也不得以截断为唯一可读形态 |
| 触发来源 | 新增字典标签列：`TASK_SUCCESS`→任务回写 / `ISSUE_DISPOSITION`→问题处置 / `MANUAL_RETRY`→手动重试；`NULL` 显示「—」 |
| 其余 | 项目、渠道、状态、PR、任务、尝试次数、补发：维持现状 |
| 筛选 / 排序 | **不改**；不做按触发来源筛选（本期非范围） |

### 3.3 问题台账 · 详情抽屉

在「来源任务」与「动作时间线」之间（或动作时间线之上）增加「PR 总结评论投递」摘要区：

| 展示 | 说明 |
|---|---|
| 投递状态 | SUCCESS / FAILED / 无记录 |
| 最后尝试时间 | 有记录时展示 |
| 失败原因 | FAILED 时全文或可展开全文 |
| 触发来源 | 有值时展示字典标签 |
| 链接 | 「查看投递记录」→ `/notify/delivery`（无定位参数）；需 `review:delivery:list` |

本区只读摘要，**不**在抽屉内直接调补发（补发仍在投递记录页 / 任务详情既有入口），避免权限与副作用入口扩散。

## 4. 技术方案

### 4.1 数据：`trigger_source`（只加列）

脚本：`sql/26_issue_delivery_trace_m6_1.sql`（幂等：`WHERE` 列不存在再 `ADD COLUMN` 的惯用写法，对齐既有 `sql/2x` 风格）。

| 列 | 类型 | 说明 |
|---|---|---|
| `trigger_source` | `varchar(32) NULL` | `TASK_SUCCESS` / `ISSUE_DISPOSITION` / `MANUAL_RETRY`；历史 `NULL` |

字典：`review_delivery_trigger_source`（三项中文标签）；菜单/权限**不加**。

`updateDeliveryResult` / `insertDelivery` 同步写入 `trigger_source`；原地更新覆盖为**本次尝试**来源。

### 4.2 三口赋值

| 入口 | 赋值 | 说明 |
|---|---|---|
| M4 任务 SUCCESS 后 GitHub 总结评论首次/后续 upsert | `TASK_SUCCESS` | `ReviewDeliveryServiceImpl` 任务成功回写路径 |
| M6 处置后 `rerenderSummaryComment` | `ISSUE_DISPOSITION` | confirm/close/dismiss 事务提交后 |
| `POST .../delivery/{taskId}/retry` 与 `POST .../delivery/record/{deliveryId}/retry` | `MANUAL_RETRY` | 任务详情补投递/重试、投递记录页补发 |

IM 渠道 upsert：**首次投递写 `TASK_SUCCESS`（任务终态触发），记录页补发写 `MANUAL_RETRY`，不留 `NULL`**——IM 与 GitHub 评论共用同一 insert/update mapper，传正确值零成本，留 NULL 会让 IM 行在列表中永远显示「—」，本迭代要解决的「认不出」问题会在 IM 行原样复现。`ISSUE_DISPOSITION` 仅用于 GitHub 总结评论重渲染路径（处置不重发 IM，与 M6 非范围一致）；**不得**为此新增第四枚举或单独需求。

### 4.3 处置接口响应扩展

路径不变：`PUT /review/issue/{id}/confirm|close|dismiss`。

现响应：

```json
{ "commentSyncStatus": "FAILED" }
```

改为（字段名实现期可微调，语义固定）：

```json
{
  "commentSyncStatus": "FAILED",
  "commentSyncFailureMessage": "GitHub API 403：Resource not accessible by personal access token",
  "deliveryId": 1024
}
```

| 字段 | 何时有值 |
|---|---|
| `commentSyncStatus` | 始终：`SUCCESS` / `FAILED` / `SKIPPED` |
| `commentSyncFailureMessage` | 仅 `FAILED`；来自该次 upsert 的 `failure_message`（已脱敏） |
| `deliveryId` | `FAILED` 时尽量返回；若落库也失败可为 `null`（提示仍展示原因或降级文案） |

实现要点：**直接修改 `IReviewDeliveryService.rerenderSummaryComment` 签名**为返回结果对象（status + failureMessage + deliveryId），**不得新增重载方法**；生产调用方仅 `ReviewIssueServiceImpl.scheduleCommentRerender` 一处，存量 5 处测试（`ReviewDeliveryServiceImplTest` 3 个 rerender 用例、`ReviewIssueServiceImplTest` 2 处 mock stub）随签名同步适配。deliveryId 取 upsert 后回查幂等键或 insert 生成键；Controller 组装 Map，**不**改变处置 HTTP 成功语义（评论失败仍 200 + 业务 data）。

详情接口 `GET /review/issue/{issueId}`：在 `ReviewIssueDetail` 上附加该 PR 的总结评论投递摘要（复用 `selectSummaryDelivery`），供抽屉展示；权限仍 `review:issue:query`，投递字段随详情下发（项目数据范围已校验）；前端跳转投递页再受 `review:delivery:list` 控制。

### 4.4 模块边界

```text
acr-admin
  ReviewIssueController     # 处置响应扩字段；详情已有
acr-review
  ReviewDeliveryRecord      # + triggerSource
  ReviewDeliveryRecordMapper(+xml)  # insert/update/select 含新列
  ReviewDeliveryServiceImpl # 三口传入 triggerSource；rerender 返回结果对象
  ReviewIssueServiceImpl    # 透传 rerender 结果；详情装配 summaryDelivery
acr-ui
  review/issue/index.vue    # 失败提示+跳转；抽屉投递摘要
  notify/delivery/index.vue # 列名/全文/触发来源
sql/26_issue_delivery_trace_m6_1.sql
```

不新建 Maven 模块；Controller 不访问 Mapper。

### 4.5 主流程（失败可追溯）

```text
处置用例提交成功
  → rerenderSummaryComment(..., ISSUE_DISPOSITION)
       → upsert 同行：status/failure_message/last_attempt_time/trigger_source=ISSUE_DISPOSITION
       → 返回 { status, failureMessage, deliveryId }
  → 前端：成功 toast +（若 FAILED）原因 warning +「去投递记录」
  → 用户进入投递记录页：首行即刚更新的失败行（最后尝试时间最新）
  → 补发 → trigger_source=MANUAL_RETRY；成功则状态翻 SUCCESS
```

## 5. 测试要点

至少覆盖：

1. **处置失败响应**：mock GitHub 失败 → issue/action 已提交；响应含 `commentSyncStatus=FAILED`、非空 `commentSyncFailureMessage`、非空 `deliveryId`；对应行 `trigger_source=ISSUE_DISPOSITION`；
2. **三口来源**：任务 SUCCESS 回写 → `TASK_SUCCESS`；处置重渲染 → `ISSUE_DISPOSITION`；手动 retry → `MANUAL_RETRY`；连续两次不同来源原地更新后，列为**最近一次**来源；
3. **历史空值**：无 `trigger_source` 的旧行列表展示「—」，不报错；
4. **详情装配**：`GET /issue/{id}` 含该 PR 总结评论投递摘要；无投递记录时前端友好空态；
5. **权限**：不新增权限串；无 `review:delivery:list` 时跳转入口不可用或不可达；数据范围仍按项目；
6. **回归**：处置成功且评论成功时不出现失败提示；M5 IM 投递与列表筛选/排序行为不变；
7. 前端：`npm run build:prod`；后端：`mvn test`。

## 6. 分步实现计划

| 步 | 内容 | 验证 |
|---|---|---|
| 1 | 设计文档定稿（含 review 修订：签名变更声明、IM 赋值钉死、失败原因展示形态） | ✅ 已完成（2026-08-03） |
| 2 | SQL `26_issue_delivery_trace_m6_1.sql`（加列 + 字典）+ `docs/deployment.md` / `sql/README.md` | ✅ |
| 3 | 领域/Mapper/`upsert` 透传 `triggerSource`；`rerenderSummaryComment` 返回结果对象；三口赋值单测 | ✅ |
| 4 | 处置 Controller 响应扩字段；详情装配 summary 投递摘要 | ✅ |
| 5 | 前端：处置失败提示+跳转；投递列表列名/全文/触发来源；台账抽屉投递摘要；CHANGELOG 补 M6.1 | ✅ |

每步可独立提交；步 2–3 合入不改变用户可见文案，步 5 后验收闭环。

## 7. 风险与对策

| 风险 | 对策 |
|---|---|
| 跳转无定位参数，首行被其他渠道刚失败的记录挤掉 | 接受；列表仍按最后尝试时间全局倒序。用户可凭触发来源=问题处置 + PR 号识别；若后续仍难认再单独立项「可选 query 高亮」 |
| upsert 成功但回查 `deliveryId` 失败 | 仍返回 failureMessage；deliveryId 可空；提示降级文案 |
| 列名「最近尝试」→「最后尝试时间」与任务详情文案不一致 | 投递记录页与台账抽屉统一用「最后尝试时间」；任务详情 `DeliveryStatusView` 可顺手统一文案（小改动，不算新范围） |
| 误把 IM 处置联动做进来 | 硬约束：处置不重发 IM；`ISSUE_DISPOSITION` 仅 GitHub 总结评论重渲染路径 |

## 8. 非范围（再声明）

尝试历史表 / 事件流 / 新通知事件 / M5 渠道与策略改动 / 新权限串 / 投递导出 / `deliveryId` 深链定位 / 通知频控 / 其他渠道适配 / 改幂等键或一 PR 多评论。
