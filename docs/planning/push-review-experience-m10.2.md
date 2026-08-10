# M10.2 推送审查体验修复与对账语义修正 设计

- 状态：**定稿待审**（2026-08-10，三轮全流程联调后立项）
- 上游依据：2026-08-10「发现 → 跨轮跟踪 → 修复 → 复核」三轮全流程演练；`push-review-m10.md` §6.5 遗留
- 关联设计：`issue-lifecycle-m8.md`（对账机制）、`push-review-m10.md`（push 线）

---

## 1. 背景：三轮全流程演练暴露的问题

演练脚本：第 1 轮提交含 SQL 注入/硬编码凭据/吞异常的 PaymentGateway（OCR 审查 BLOCK，3 问题进台账）→ 第 2 轮仅删一行注释（PASS）→ 第 3 轮修复 SQL 注入（PASS）。

暴露四类问题：

1. **对账语义错位（机制问题）**：第 2 轮只删了一行注释，3 个问题全部被转「待复核」——比第 3 轮真正修复还早一轮。根因：push 线审查范围是**本次推送增量**（before..after），不是分支全量；现有对账「本轮未命中即计 missed」是 PR 线全量 diff 语义下的设计，用在增量 diff 上，「没碰到」被误判为「疑似修复」。
2. **OCR 输出全英文（可读性问题）**：category 显示 SECURITY、问题标题描述建议全英文（"SQL Injection vulnerability: `accountId` is concatenated..."），中国大陆企业用户不可读。
3. **代码变更「暂无数据」（信息缺失）**：push 任务建单时 additions/deletions/changedFiles 置 null（M10 设计未回填），任务/记录详情「代码变更」显示「暂无数据」。
4. **台账看不懂（信息架构问题）**：「审查轨迹：首次 #19 → 最近命中 #19 → 复核依据 #20 · 未命中 2」——任务编号无语义、missed 是内部术语、「待复核」状态不解释「疑似已修复」含义。

## 2. 产品决策记录（对抗式讨论）

### D1 对账语义：Diff 覆盖感知（方案 A，小黑已批准）

**决策**：对账时判断「本轮 diff 覆盖的文件集」——问题所在文件**未被本轮 diff 覆盖** → 不计 missed（保持原状态与 missed_streak）；**被覆盖但未命中** → 计 missed（现有逻辑）。

**对抗质疑与回应**：

- *质疑 1：跨文件重构时问题真的被修复了，但原文件不在 diff 里怎么办？* —— 该问题保持原状态而非转待复核，确实会晚发现。但台账兜底在：问题仍在列表可见、人工可随时关闭；相比「每次无关推送都批量假转待复核」的噪音，晚发现的成本更低。复核是人工动作，噪音会直接摧毁用户对「疑似已修复」信号的信任——信任一旦失去，整个复核机制作废。
- *质疑 2：PR 线要不要同步改？* —— PR 线每次 synchronize 的 diff 是 PR 全量（base...head），问题文件只要还在 PR 变更集里就持续被覆盖，语义天然正确，**不改**，避免动 M8 已验证路径。本修正仅对 `event_source=PUSH` 生效。
- *质疑 3：文件被重命名/移动怎么办？* —— diff 覆盖集按变更后路径（新路径）计；问题 filePath 是旧路径时视为未覆盖，不计 missed。可接受：重命名场景少，且问题保持原状态不失联。
- *质疑 4：OCR 路径目前没有 diff 文件集数据。* —— OCR 路径工作区有完整 git 历史，`git diff --name-only base head` 直接可得（与已有 `hasNoCommitDiff` 同源）；LLM 路径从已拉取的 diffResult 解析文件名（DiffParseResult 已有）。不新增外部调用。

### D2 OCR 中文输出：--background 注入语言指令

**决策**：OCR CLI 调用追加 `--background` 参数，注入输出语言指令：「输出语言要求：所有审查发现必须使用简体中文（问题标题、描述、修复建议）；severity 与 category 字段保持英文枚举值不变」。

**事实依据（已实测验证，2026-08-10）**：对 acr-demo 第 1 轮 diff 手工注入后，OCR 输出「**SQL注入漏洞**：accountId 直接拼接进SQL字符串...」全中文，且加粗标题格式与 M10.1 mapper 的标题提取逻辑天然兼容。

**对抗质疑与回应**：

- *质疑 1：background 参数本意是业务背景，塞语言指令算滥用吗？* —— CLI 无独立 language 参数（已核对 1.8.10 全部参数），background 是唯一注入点；指令措辞保持中性（仅约束输出语言与枚举值），不干预审查判断。OCR 升级出 language 参数后立即迁移。
- *质疑 2：模型不听指令仍输出英文怎么办？* —— 指令遵循率实测 100%（qwen3.8-max）；即使偶发英文，展示层仍有 severity/category 中文映射兜底，title 英文可读性差但不阻塞流程。
- *质疑 3：指令文本硬编码还是可配置？* —— 硬编码常量（`ReviewEngineProperties` 或适配器常量），不进 sys_config——这是平台行为不是用户配置，避免配置面膨胀。

### D3 OCR 标题规范化：剥离 severity 前缀

**决策**：`ReviewEngineResultMapper` 标题提取后，额外剥离英文/中文 severity 前缀（`Critical:`、`High:`、`Medium:`、`Low:`、`严重:`、`高危:` 等，含变体）。

**对抗质疑与回应**：

- *为什么必须做*：实测 OCR 标题格式**轮次间不稳定**——任务 18 输出 "Critical: SQL Injection Vulnerability"（带前缀），任务 19 输出 "SQL Injection vulnerability: ..."（不带）。指纹 = hash(category + filePath + title)，**同一问题前缀不同即指纹不同** → 跨轮被当作新问题重复物化。这是指纹稳定性的实际威胁，不是美化。
- *质疑：剥前缀会不会误伤正常标题？* —— 只剥行首的「severity 词 + 冒号」模式，误伤面极小；即使误伤，同轮内一致剥离，不影响轮内去重，只提升跨轮稳定性。

### D4 push 线代码变更统计回填

**决策**：任务执行阶段回填 additions/deletions/changedFiles——LLM 路径从 diffResult 统计；OCR 路径工作区执行 `git diff --shortstat base head` 解析。写入 task 既有三列（无 schema 变更）。

**对抗质疑**：*为什么建单时不做？* —— 建单在 webhook 快路径（同步响应），不做重操作是 M2 既定纪律；统计需要 diff，放在异步执行阶段自然且不影响受理时延。

### D5 台账可读性改造

**决策**（展示层，不动数据模型）：

1. **审查轨迹人性化**：任务编号链接保留（可点击跳转），主文案改为时间语义——「08-10 10:13 发现 → 08-10 10:15 疑似修复」；「未命中 N」改为「连续 N 轮未命中」并加 tooltip 解释「最近 N 次审查的变更范围内未再发现该问题」。
2. **「待复核」状态显式语义**：RECHECKING 的字典标签改为「疑似修复」，列表阶段列补副文案「待人工确认」；详情页顶部提示沿用 M8.2 的「疑似已修复、待人工验证」。
3. **「阶段滞留」措辞**：「滞留 X」改为「已 X」（中性）。

**对抗质疑与回应**：

- *质疑 1：改字典标签影响面？* —— `review_issue_status` 字典被列表/详情/通知/工作台引用，改标签一处全局生效，无代码分支；「待复核」→「疑似修复」与通知文案「疑似已修复」口径统一，降低认知分裂。风险：存量文档/记忆里「待复核」术语需同步（本文档负责）。
- *质疑 2：为什么不重做台账信息架构？* —— 本切片只解决「看不懂」，不做列重组/视图重构（YAGNI）；三轮演练证明字段本身够用，问题在措辞与呈现。

### D6 push 结论范围标注

**决策**：push 任务的记录详情「审查结论」区与 IM 通知总结段追加范围说明：「本结论仅覆盖本次推送的变更（base..head 增量）」。

**对抗质疑**：*会不会显得产品不自信？* —— 恰恰相反：第 2 轮「PASS」但台账 3 个待复核，不解释范围用户会认为平台自相矛盾。诚实标注口径是企业产品信任的基础（与 M10 配置页交集提示同一设计哲学）。

## 3. 技术方案要点

### 3.1 对账修正（核心）

```
reconcileAfterSuccess(task, run)：
  PUSH 任务 → 先取本轮覆盖文件集 coveredFiles：
    OCR 路径（无 diffResult）：工作区 git diff --name-only base head
      —— 注意：reconcile 发生在 deliverQuietly，工作区已清理？
      → 方案：覆盖文件集在 PERSIST_RESULT 前（工作区存活期）算好，
        随 run 传递（run 级内存字段或 reconcile 参数扩展）
    LLM 路径：diffResult/DiffParseResult 已含文件列表，直接取
  对账循环中 missed 判定：
    issue.filePath ∈ coveredFiles → 现有逻辑（未命中计 missed）
    issue.filePath ∉ coveredFiles → 跳过 missed 累计，保持原状态
  PR 线（event_source != PUSH）：完全走现有逻辑，零改动
```

**实现注意**：`reconcileAfterSuccess(task, run)` 签名需接收 coveredFiles（Set<String>）。OCR 路径在 executeOcrPath 工作区存活期计算；LLM 路径从 DiffParseResult 提取。空 diff 短路路径（hasNoCommitDiff）不触发对账（无问题可审）。

### 3.2 OCR 中文与前缀剥离

- `OpenCodeReviewCliAdapter.buildReviewArgs`：追加 `--background` + 常量指令文本（含语言要求）。
- `ReviewEngineResultMapper`：title 提取后剥 severity 前缀（正则：`^\s*(critical|high|medium|low|info|严重|高危|中危|低危)\s*[:：]\s*`，忽略大小写）。

### 3.3 代码变更统计

- LLM 路径：`DiffParseResult` 已有文件级信息 → 统计文件数/增行/删行，`persistLlmSuccess` 前写 task 三列。
- OCR 路径：工作区 `git diff --shortstat base head`（输出 "1 file changed, 25 insertions(+), 3 deletions(-)"）解析；与 hasNoCommitDiff 同一 git 调用窗口。

### 3.4 展示层

- 台账列：审查轨迹文案重构、字典标签 RECHECKING→疑似修复、滞留措辞。
- 记录详情/通知：push 结论范围标注。

## 4. 范围

**本次范围**：D1 对账修正（PUSH 线）、D2 中文指令、D3 前缀剥离、D4 变更统计回填、D5 台账可读性、D6 范围标注。

**非范围**：台账信息架构重构、PR 线对账逻辑、OCR 引擎升级/language 参数迁移、通知模板体系、问题指派。

## 5. 实现步骤与验证

| 步 | 内容 | 验证 |
|---|---|---|
| 1 | D1 对账修正：coveredFiles 采集（双路径）+ reconcile 签名扩展 + PUSH 线 missed 判定 | 单测：PUSH 任务 diff 未覆盖文件不计 missed、覆盖未命中计 missed、PR 线零回归 |
| 2 | D2 --background 中文指令 + D3 前缀剥离 | 单测：命令参数断言、前缀剥离各变体；联调：acr-demo 推送验证中文输出 |
| 3 | D4 变更统计回填（双路径） | 单测 + 联调：任务/记录详情代码变更有数 |
| 4 | D5 台账可读性 + D6 范围标注 | `npm run build:prod` + 页面走查 |
| 5 | 端到端复演 | 重跑三轮剧本：第 2 轮删注释后 3 问题**保持原状态**（不再批量转待复核）、第 3 轮修复后 SQL 注入问题转「疑似修复」、凭据问题保持 |

## 6. 验收标准

1. 三轮剧本复演结果符合步骤 5 预期（对账语义修正生效）；
2. OCR 审查输出中文标题/描述/建议，category/severity 展示中文映射；
3. push 任务的任务/记录详情「代码变更」显示真实文件数与增删行数；
4. 台账审查轨迹可读（时间语义 + 编号可跳转），RECHECKING 显示「疑似修复」；
5. push 记录详情与通知含「仅覆盖本次推送变更」范围标注；
6. PR 线（webhook-test 存量用例）零回归，`mvn test` 全绿。

## 7. 风险

| 风险 | 等级 | 应对 |
|---|---|---|
| reconcile 签名变更触碰 M8 对账 | 中 | PR 线路径零改动为硬门槛，存量用例全绿；PUSH 线新用例覆盖三种判定分支 |
| OCR 中文指令偶发不遵循 | 低 | 展示层中文映射兜底；发现不遵循再评估 prompt 强化 |
| 前缀剥离误伤标题 | 低 | 仅行首 severity 词 + 冒号模式；轮内一致性不受影响 |
| 工作区清理时机与 coveredFiles 采集冲突 | 中 | 采集点固定在 PERSIST_RESULT 前（工作区存活期），随 run 传递，不在 reconcile 时刻访问工作区 |
