# M7 基础工作台

> **状态（2026-08-03）：已实现，待真实环境验收。** 修订五点：①零项目引导态触发条件改为 `hasPermi('review:project:list')` 且 `projectCount===0`，无对应权限时 scope 字段返回 `null` 不展示，防止开发者误触发引导态（§1.4/§3.1/§4.1）；②任务页回填 `taskStatus` 时必须复位 `queueOnly=false`（§3.2/§4.6）；③`onActivated` 重新拉取 summary，解决 keep-alive 数字不更新（§4.6）；④`today` 字段无权限返回 `null`、前端渲染「—」（§4.2）；⑤更正「M6 催办口径」表述为「M6 PR 总结评论待办计数口径」，补聚合请求失败态（§3.2/§4.6）。前置：`docs/planning/product-roadmap.md`（§4.2 工作台、§5.1 行动优先、§6 首页工作台、§7.3 MVP）、`docs/planning/issue-ledger-m6.md`、`docs/planning/issue-delivery-trace-m6.1.md`（文档结构参照）、`rules/architecture.md`、`rules/delivery.md`、`rules/UI_THEME_RULES.md`。本设计把登录后首页从占位页替换为「我今天需要处理什么」的行动工作台：范围健康、权限驱动待办卡、今日摘要、最近动态；**不**做范围切换、逾期/复核卡、趋势图、角色化配置页、工作台内处置入口、新表/新权限串/新字典。做完 M7，MVP 功能面收口，进入真实试点验收。

## 1. 用户场景与验收标准

### 1.1 产品结论（一句话）

登录后首页即工作台，用**待办数字 + 一跳列表**回答「今天先处理什么」；数字必须与台账/任务/记录/投递列表在同权限、同筛选下的可见条数一致，不做第二套处置面，也不堆趋势图。

### 1.2 目标用户与场景

| 角色 | 场景 | 今天卡在哪 | 期望 |
|---|---|---|---|
| 项目负责人 / 质量负责人 | 早上打开后台，不知道先看问题、失败任务还是投递 | 首页是欢迎占位，要自己翻菜单 | 一眼看到待确认/待修复/高风险/失败/投递失败数量，点卡片进对应列表 |
| 开发者（有问题/任务权限） | 只关心自己权限范围内的待办 | 无权限的菜单仍可能误导，或数字与列表对不上 | 无权限的卡不出现；有权限的数字点进去条数对得上 |
| 新接入管理员 | 环境刚搭好，还没有任何项目 | 占位页看不出下一步 | 整页引导「接入第一个项目」（有 `review:project:add` 才显示入口） |
| 平台运维 | 关心失败任务与投递失败 | 分散在审查任务与投递记录两处 | 工作台卡片直达已筛好的失败列表 |

### 1.3 对照现状

```text
登录 → /index（acr-ui/src/views/index.vue）
  → 欢迎文案 + 能力卡片 +「业务模块开发中」❌ 无待办
  → 用户自行点「问题台账 / 审查任务 / 投递记录」

M1–M6.1 已具备：
  ✅ review_project / review_task / review_issue / review_delivery_record
  ✅ 各列表 @DataScope(deptAlias="d", userAlias="owner", permission=review:*:list)
  ✅ 问题状态 AWAITING_CONFIRM / AWAITING_FIX；存量 origin=EXISTING 排除在待办计数外（M6 PR 总结评论口径）
  ✅ 审查记录可筛 reviewConclusion=BLOCK；任务可筛 taskStatus=FAILED；投递可筛 deliveryStatus=FAILED
  ❌ 无工作台聚合 API；列表页多数未统一从 route.query 回填筛选（投递页仅有 taskId 先例）
```

### 1.4 验收标准

1. **首页即工作台**：登录后 `/index` 展示工作台，替换现有占位欢迎页；零项目引导态仅当用户有 `review:project:list` 且 `projectCount === 0` 时触发（无项目接入权限的用户不得误触发引导态）；有 `review:project:add` 显示「接入第一个项目」入口，无权限则仅文案说明。
2. **范围与健康**：按权限展示项——`projectCount` 需 `review:project:list`、`latestTaskTime` 需 `review:task:list`；无对应权限的字段返回 `null`、前端不展示该项（不显示 0，避免误解）；数据范围与对应列表一致。
3. **待办卡片（最多 5 张，权限驱动）**：后端按用户菜单权限决定下发哪些卡；无权限的 type **不返回**；有权限但 count=0 仍返回，前端显示灰色 `0`、**不隐藏**；点击带 `query` 跳转对应列表且筛选已回填。
4. **计数 = 列表可见条数（硬验收）**：每张卡的 `count` 与跳转后列表在**相同 DataScope + 相同筛选条件**下的总条数一致（含 AWAITING_CONFIRM 排除 EXISTING、高风险 7 天窗口与日期回填）。
5. **今日摘要**：`newTasks` / `successTasks` / `failedTasks` / `closedIssues` 四个数字，口径一律数据库 `CURDATE()`；范围与任务/问题列表同源 DataScope；无对应 list 权限时字段返回 `null`、前端渲染「—」，与有权限的「0 条」区分。
6. **最近动态**：最多 5 条审查任务，结构 `{ type, title, time, link }`；中文状态/结论，不暴露技术枚举；相对时间 + 悬停绝对时间；附「查看全部」进任务列表。
7. **契约与边界**：仅 `GET /workbench/summary`；登录即可调，**无新权限串**；`cards` 数据驱动，前端不硬编码卡片种类；无新表、无新字典；`mvn test` 与 `npm run build:prod` 通过；可用现有测试数据验收，不依赖外部 PAT。

## 2. 边界（做 / 不做）

### 2.1 做

| # | 能力 | 说明 |
|---|---|---|
| 1 | 范围与健康 | 按权限展示可见项目数 + 最近任务时间（无权限项为 null 不展示）；零项目整页引导（仅对有项目接入权限的用户） |
| 2 | 待办卡片 | 固定候选 5 种，后端按权限裁剪下发；点击导航到既有列表 |
| 3 | 今日摘要 | 今日新增/成功/失败任务 + 今日关闭问题，四数字 |
| 4 | 最近动态 | 最近 5 条审查任务 +「查看全部」 |
| 5 | 列表 query 回填 | 问题/任务/记录/投递四个列表 `onMounted` 支持 route query 回填筛选 |

### 2.2 不做

| 项 | 理由 |
|---|---|
| 范围切换下拉 | 数据范围由既有部门/项目权限决定，MVP 不做前端切范围 |
| 逾期卡片 | 无 due date 数据源；随核心版指派一起上 |
| 待复核卡片 | M6 状态机无复核实装（`RECHECKING` 仅预留）；核心版完整流转再加 |
| 趋势图 / 看板 | 企业版「数据洞察」职责；工作台行动优先 |
| 角色化配置页 | 核心版由后端按角色组装卡片，**不是**用户可配置；本期契约已按数据驱动预留 |
| 工作台内第二套处置入口 | 只导航；处置仍在台账/任务/投递页 |
| 「快捷操作」独立区域 | 卡片点击即快捷操作；零项目引导承担接入入口 |
| 新表 / 新权限串 / 新字典 | 聚合读现有表；卡片 type→标题由后端 `title` 下发，前端仅允许 type→图标等常量映射 |
| 新 Maven 模块 / 消息队列 | 遵循 architecture.md |

## 3. 交互

### 3.1 页面结构（自上而下）

```text
┌─────────────────────────────────────────────────────────┐
│ 范围与健康：可见项目 N · 最近任务时间（相对 + 悬停绝对）   │
├─────────────────────────────────────────────────────────┤
│ 待办卡片行（1–5 张，后端下发顺序即展示顺序）               │
│  [待确认] [待修复] [高风险7天] [失败任务] [投递失败]      │
│   count=0 → 灰色数字，仍可点进空列表                       │
├─────────────────────────────────────────────────────────┤
│ 今日摘要：新增任务 / 成功 / 失败 / 关闭问题（四数字）       │
├─────────────────────────────────────────────────────────┤
│ 最近动态：5 条 · 项目 · PR · 状态/结论 · 相对时间          │
│                              [查看全部 → /review/task]   │
└─────────────────────────────────────────────────────────┘

零项目（hasPermi('review:project:list') 且 projectCount === 0）：
┌─────────────────────────────────────────────────────────┐
│ 空态整页：接入第一个项目才能开始审查                       │
│ 有 review:project:add → 主按钮跳转项目新建/项目页          │
│ 无权限 → 仅说明文案，引导联系管理员                        │
└─────────────────────────────────────────────────────────┘
（触发零项目引导态时不渲染待办/摘要/动态，整页切换引导态；不保留两套 UI 形态）
```

视觉：遵守 `rules/UI_THEME_RULES.md`——内容优先、克制分层、品牌绿只用于主操作/链接；状态不单靠颜色；后台页不用登录页大圆角半透明风格。卡片用轻边框 + 浅底，非装饰性大卡片墙。

### 3.2 待办卡片定义（候选全集）

| type | 标题（后端 `title`） | 计数口径 | `link` | `query`（须与列表筛选项字段名一致） | 可见权限 |
|---|---|---|---|---|---|
| `ISSUE_AWAITING_CONFIRM` | 待确认问题 | `review_issue.status=AWAITING_CONFIRM` **且** `origin='NEW'`（不含 EXISTING，对齐 M6 PR 总结评论待办计数口径） | `/review/issue` | `{ "status": "AWAITING_CONFIRM", "origin": "NEW" }` | `review:issue:list` |
| `ISSUE_AWAITING_FIX` | 待修复问题 | `status=AWAITING_FIX` **且** `origin='NEW'`（与 M6 PR 总结评论待办计数口径一致） | `/review/issue` | `{ "status": "AWAITING_FIX", "origin": "NEW" }` | `review:issue:list` |
| `HIGH_RISK_CONCLUSION` | 高风险结论（7天） | 最近 **7** 天（窗口钉死）`review_task`：`task_status=SUCCESS` 且 `review_conclusion=BLOCK`；时间字段用 `ifnull(finished_time, create_time)`，与审查记录列表日期口径一致 | `/review/record` | `{ "reviewConclusion": "BLOCK", "beginTime": "<T-6>", "endTime": "<T>" }`（日期为请求当日算出的 `yyyy-MM-dd`，含首尾共 7 天） | `review:record:list` |
| `TASK_FAILED` | 失败任务 | `review_task.task_status=FAILED` 存量 | `/review/task` | `{ "taskStatus": "FAILED" }` | `review:task:list` |
| `DELIVERY_FAILED` | 投递失败 | `review_delivery_record.delivery_status=FAILED` 存量（含 GitHub 评论与 IM） | `/notify/delivery` | `{ "deliveryStatus": "FAILED" }` | `review:delivery:list` |

说明：

- 产品跳转示意里的 `?conclusion=BLOCK` 为口语；**契约 `query` 键必须用列表真实字段** `reviewConclusion`，避免前端二次映射。
- AWAITING_* 的 `origin=NEW` 是为实现「数字 = 列表可见条数」硬验收相对路线图表格的必要补全；台账页须支持 `origin` query 回填（列表已有归属筛选项）。
- 高风险卡必须把 7 天窗口写入 `beginTime`/`endTime`，审查记录页回填到既有日期范围控件；否则列表会变成全量 BLOCK，与卡面数字不一致。
- **任务页队列视图复位**：审查任务列表默认队列视图（`queueOnly=true`，只显示在途任务）。route query 回填 `taskStatus` 时必须同时置 `queueOnly=false`，否则 FAILED 行被队列视图排除，卡面数字与列表条数不一致。

### 3.3 卡片与动态点击

```text
卡片点击：
  router.push({ path: card.link, query: card.query })
  → 目标列表激活时（onActivated：首次挂载或 keep-alive 重入 tab）：把 route.query 写入 queryParams（及记录页 dateRange）
  → 再 getList()

最近动态行点击：
  优先 link 指向任务详情或任务列表带 taskId（实现期与现有详情路由对齐）
  「查看全部」→ /review/task（无额外强制筛选）

文案：
  一律中文状态名（待确认、待修复、高风险、已失败、投递失败、通过…）
  不在 UI 暴露 AWAITING_CONFIRM / BLOCK / FAILED 等枚举作主文案（筛选值仍走枚举码）
```

### 3.4 今日摘要与最近动态展示

| 区域 | 展示 |
|---|---|
| 今日摘要 | 四个指标标签 + 数字；无额外下钻（需要时用户点对应待办卡或菜单） |
| 最近动态 | `title` 建议含：项目名 · PR #n · 中文状态/结论；`time` ISO 或 `yyyy-MM-dd HH:mm`；前端相对时间，悬停 `el-tooltip` 绝对时间 |
| 空态 | 无动态时一行「暂无审查动态」；不假装有数据 |

## 4. 技术方案

### 4.1 接口契约（结构冻结，不得更改）

`GET /workbench/summary`

- 鉴权：登录用户即可（`@PreAuthorize` 登录即可 / 与现有首页同级，**不**新增 `review:workbench:*`）。
- 响应：`AjaxResult`，`data` 如下：

```json
{
  "scope": {
    "projectCount": 3,
    "latestTaskTime": "2026-08-03 15:20"
  },
  "cards": [
    {
      "type": "ISSUE_AWAITING_CONFIRM",
      "title": "待确认问题",
      "count": 4,
      "link": "/review/issue",
      "query": { "status": "AWAITING_CONFIRM", "origin": "NEW" }
    }
  ],
  "today": {
    "newTasks": 5,
    "successTasks": 4,
    "failedTasks": 1,
    "closedIssues": 2
  },
  "recent": [
    {
      "type": "TASK",
      "title": "demo-repo · PR #12 · 高风险",
      "time": "2026-08-03 15:20",
      "link": "/review/task"
    }
  ]
}
```

字段约定：

| 字段 | 规则 |
|---|---|
| `scope.projectCount` | 当前用户 DataScope 下项目数；无 `review:project:list` 权限时返回 `null`、前端不展示该项；仅当值为 `0` 时触发引导态（为 0 自然蕴含已有该权限） |
| `scope.latestTaskTime` | 可见范围内最近一条任务的 `ifnull(finished_time, create_time)` 格式化字符串；无任务或无 `review:task:list` 权限时返回 `null`、前端不展示该项 |
| `cards` | 有序列表；仅含当前用户**有对应 list 权限**的项；`count` 可为 0 |
| `cards[].type` | 上表五枚之一；核心版角色化只改组装逻辑，不改本字段集合语义 |
| `cards[].title` | 后端下发中文标题；前端不靠字典、不以硬编码列表决定「有哪些卡」 |
| `cards[].link` / `query` | 前端原样用于 `router.push`；`query` 值均为字符串 |
| `today.*` | 非负整数；无对应 list 权限时返回 `null`，前端渲染「—」 |
| `recent` | 0–5 条；`type` 本期固定 `TASK` |

### 4.2 今日摘要口径

一律服务器时区下 MySQL `CURDATE()`（与部署说明中的 DB 时区一致；文档在 `docs/deployment.md` 实现期补一句即可）：

| 指标 | SQL 语义 |
|---|---|
| `newTasks` | `review_task`：`DATE(create_time) = CURDATE()` |
| `successTasks` | `task_status='SUCCESS'` 且 `DATE(ifnull(finished_time, create_time)) = CURDATE()` |
| `failedTasks` | `task_status='FAILED'` 且 `DATE(ifnull(finished_time, create_time)) = CURDATE()` |
| `closedIssues` | `review_issue.status='CLOSED'` 且 `DATE(closed_time) = CURDATE()`（不含 IGNORED / FALSE_POSITIVE） |

今日任务三项与 `review:task:list` 同款 DataScope；关闭问题与 `review:issue:list` 同款 DataScope。**用户无对应 list 权限时该字段返回 `null`（键位保留），前端渲染「—」**——DataScope 机制对无权限用户本身返回空数据（`dept_id = 0` 片段），此时「0」对用户是不诚实的，「—」用于区分「没有数据」与「不可见」。前端渲染规则：值为 `null` → 「—」，否则直接展示数字。

### 4.3 数据范围一致性（实现要点，硬约束）

**原则：工作台数字 = 对应列表在相同筛选下的可见条数。**

实现要点：

1. **禁止**在工作台写一套「裸表 count、无 dept/owner join」的捷径 SQL。
2. 聚合 count / 最近任务查询必须：
   - join 路径与各列表一致：`review_*` → `review_project p` → `sys_dept d` → `sys_user owner`；
   - 使用 `${params.dataScope}`；
   - `@DataScope(deptAlias = "d", userAlias = "owner", permission = "review:xxx:list")`，`permission` 与卡片/列表权限一一对应。
3. 推荐结构：在各既有 Service（Issue / Task / Record / Delivery / Project）上增加 **count/汇总查询方法**（带正确 `@DataScope`），由 `WorkbenchServiceImpl` 组装；`WorkbenchServiceImpl` 自身用 `SecurityUtils.hasPermi(...)` 决定是否调用并放入 `cards`。
4. 单测必须断言：同一用户、同一筛选下，`card.count == list total`（可用固定夹具数据）。
5. 高风险 7 天、AWAITING_* + `origin=NEW` 的筛选条件必须同时出现在 count SQL 与返回的 `query` 中。

### 4.4 模块边界

```text
acr-admin
  WorkbenchController              # GET /workbench/summary；仅协议与登录校验
acr-review
  domain/WorkbenchSummary*         # 或 Map/DTO：Scope/Card/Today/Recent
  service/IWorkbenchService
  service/impl/WorkbenchServiceImpl
       → 调用 Project/Issue/Task/Record/Delivery 的带 @DataScope 计数与最近查询
  mapper XML                       # 优先扩展既有 Mapper 的 count 语句；不新建业务表
acr-ui
  views/index.vue                  # 替换为工作台（路由 /index 不变）
  api/workbench.js                 # getSummary
  views/review/issue|task|record/index.vue
  views/notify/delivery/index.vue  # onMounted 回填 route.query
```

- Controller **不**访问 Mapper；
- 不新建 Maven 模块；
- 不把聚合逻辑放进 `acr-system`。

### 4.5 主流程

```text
用户登录进入 /index
  → GET /workbench/summary
  → WorkbenchServiceImpl：
       projectCount（hasPermi('review:project:list') 才查询，否则 null）/ latestTaskTime（同规则，review:task:list）
       for each 候选卡:
         if hasPermi(list权限) → count(同列表口径) → append card(title/link/query)
       today 四指标（CURDATE + DataScope，无权限为 null）
       recent 5 条任务（中文 title 装配）
  → 前端：
       projectCount==0 → 引导态
       else 渲染 cards/today/recent
       卡片点击 → 列表页 query 回填 → getList
```

### 4.6 前端要点

1. **数据驱动卡片**：`v-for="card in summary.cards"`；禁止前端写死五张卡再按权限 `v-if` 隐藏（权限裁剪在后端）。
2. **count=0**：灰色样式 + 可点击；不得 `v-if="count>0"`。
3. **type→图标**：若需要图标，用前端常量映射（如 `CARD_ICON[type]`）；标题以接口 `title` 为准；**不**为此建字典。
4. **列表回填**：四个列表在激活时（`onActivated`，首次挂载或 keep-alive 重入 tab 都触发）读取 `route.query` 写入与表单字段同名的 `queryParams`；记录页额外把 `beginTime`/`endTime` 写入 `dateRange`；投递页在现有 `taskId` 回填基础上扩展 `deliveryStatus`；**任务页 query 含 `taskStatus` 时同时置 `queueOnly=false`（默认队列视图只显示在途任务，会排除 FAILED）**。不能用 setup 顶层一次性回填代替——RuoYi tab 页 keep-alive，重入已打开的列表 tab 时 setup/onMounted 不重跑，卡片筛选会失效。
5. **相对时间**：复用或对齐项目内既有时间工具；悬停显示绝对时间。
6. 首页路由 meta 文案可由「首页」改为「工作台」（constantRoutes，无菜单 SQL）。
7. **时效性**：工作台在 `onActivated` 重新拉取 summary——RuoYi tab 页 keep-alive，从待办列表回到工作台不会重跑 `onMounted`，处置完问题回来后数字必须刷新。
8. **权限空值**：`scope.projectCount` 为 `null` 时不展示该项；引导态仅当 `scope.projectCount === 0` 触发；`today` 字段为 `null` 渲染「—」。
9. **失败态**：summary 请求失败时展示错误提示与重试按钮，不白屏。

### 4.7 SQL / 权限 / 字典

- **无新表、无新权限串、无新字典、无新菜单行**（`/index` 已是登录后落地页）。
- 若实现期发现审查记录日期回填需要后端时区说明，只改部署文档，不建参数项（除非多环境时区成为真实问题再单独立项）。

## 5. 测试要点

至少覆盖：

1. **权限裁剪**：无 `review:issue:list` 时 cards 不含两个 ISSUE_*；无 `review:delivery:list` 不含 DELIVERY_FAILED；全无业务 list 权限时 `cards=[]`，接口仍 200。
2. **数字 = 列表**：同一用户夹具下，五类 count 分别等于对应 list 接口 total（含 origin=NEW、BLOCK+7 天日期、FAILED 存量）。
3. **EXISTING 排除**：插入 `origin=EXISTING` 且 `AWAITING_CONFIRM` 的问题，待确认卡 count 不增加；list 带 `origin=NEW` 亦不可见。
4. **7 天窗口**：8 天前的 BLOCK 任务不计入高风险卡；`query.beginTime/endTime` 跨度含首尾 7 天。
5. **今日口径**：跨日边界用固定时钟或 SQL 条件单测；`closedIssues` 只计 `CLOSED`，不计误报/忽略。
6. **零项目**：`projectCount=0`；前端引导态；`review:project:add` 有无两种入口。
7. **count=0 卡仍返回**：有权限、零数据时 card 存在且 count=0。
8. **模块边界**：Controller 无 Mapper 注入；无新权限注解字符串。
9. **回归**：既有 issue/task/record/delivery 列表筛选与 DataScope 单测仍绿。
10. **构建**：`mvn test`；`cd acr-ui && npm run build:prod`。
11. **引导态防误触发**：无 `review:project:list`、有 `review:task:list` 的用户：`projectCount=null`，不触发引导态，卡片/摘要正常渲染。
12. **queueOnly 复位**：任务页挂载时回填 `taskStatus=FAILED` 且 `queueOnly=false`，列表 total 与失败任务卡面一致。
13. **today 空值**：无对应 list 权限时 `today` 字段为 `null`，前端渲染「—」。

页面验收（现有测试数据，不依赖 PAT）：

- 已知 `review_issue` 约 3 条、任务/投递存量 → 打开工作台数字与点进列表条数人工对账；
- 失败任务卡 / 投递失败卡在有失败行时 >0，点进筛选项已选中。
- 处置一条问题后切 tab 回到工作台，待办数字随之刷新（`onActivated`）。

## 6. 分步实现计划

| 步 | 内容 | 验证 |
|---|---|---|
| 1 | 设计文档定稿（本文） | ✅ 人工确认：契约结构、五卡口径、DataScope 一致性、非范围 |
| 2 | 后端：各 Mapper count/最近查询 + 带 `@DataScope` 的 Service 方法 + `IWorkbenchService` 组装；`WorkbenchController` | ✅ 单测：权限裁剪、筛选口径、7 天窗口；`WorkbenchServiceImplTest` |
| 3 | 前端：`api/workbench.js` + 重写 `views/index.vue`（含零项目引导、卡片/摘要/动态、相对时间） | ✅ 数据驱动卡片、count=0、引导态、`onActivated` |
| 4 | 四个列表页 route query 回填（issue/task/record/delivery） | ✅ 含 task 页 `queueOnly=false` |
| 5 | CHANGELOG + 路线图 §7.3 标记 M7 完成；`docs/deployment.md` 补今日口径时区说明 | ✅ `mvn test` + `npm run build:prod` |

每步可独立提交、可独立验证；步 2 合入不改变首页观感，步 3 后首页切换，步 4 后跳转闭环。

## 7. 风险与对策

| 风险 | 对策 |
|---|---|
| 工作台 count 与列表 total 漂移（join/DataScope/筛选不一致） | 硬验收项 + 复用同款 DataScope 片段；单测对账；禁止裸 count |
| 高风险卡 7 天 vs 列表全量 BLOCK | `query` 必带 `beginTime`/`endTime`；记录页必须回填 dateRange |
| 待确认口径漏写 origin，导致跳进列表变多 | 契约与卡表钉死 `origin=NEW`；与 M6 PR 总结评论待办计数口径对齐 |
| 前端硬编码五张卡，核心版角色化时返工 | 契约强制 `cards` 数组；前端只 `v-for`；本期文档写明 |
| DB 时区与「今日」认知不一致 | 统一 `CURDATE()`；部署文档注明 DB 时区；不引入应用层各算各的 |
| 零项目引导对无项目接入权限的用户误触发（DataScope 对无权限用户返回空数据，count=0） | 引导态仅 `hasPermi('review:project:list')` 且 `projectCount===0` 触发；无权限时 `projectCount` 返回 `null` 不展示 |
| 零项目引导与「全 0 工作台」两套 UI | 钉死整页引导，不保留两种形态 |
| 误加 `review:workbench:view` 权限 | 硬约束：登录可调；卡片可见性复用既有 `review:*:list` |
| 误建字典映射 type→标题 | 标题后端下发；前端最多 type→图标常量 |
| 处置待办后回到工作台数字未刷新（tab keep-alive） | `onActivated` 重新拉取 |
| 任务页默认队列视图排除 FAILED，卡片跳转后列表为空 | 回填 `taskStatus` 时同时置 `queueOnly=false` |
| today 对无权限用户显示 0 造成误导 | 无权限返回 `null`，前端渲染「—」 |

## 8. 非范围（再声明）

范围切换下拉、逾期卡片、待复核卡片、趋势图/质量看板、角色化配置页、工作台内确认/关闭/重试等处置动作、「快捷操作」独立区、新表、新权限串、新字典、新 Maven 模块、第二 Git Provider、指派/申诉/自动关闭、企业版报告与成本。

---

**与路线图关系**：对应 `product-roadmap.md` §5.1「首页工作台：行动优先」的 MVP 基础切片，以及 §7.3「基础工作台（未完成）」；核心版「面向角色的工作台」仅扩展后端 `cards` 组装策略，**不**变更本接口 JSON 结构。
