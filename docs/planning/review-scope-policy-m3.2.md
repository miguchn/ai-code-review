# M3.2 审查范围策略：Diff 增量核心、高影响扩展与问题归属

> **状态（2026-08-02）：步 1–7 全部落地。** 前置：`docs/planning/review-pipeline-m3.md`、`docs/planning/review-scoring-result-protocol.md`。本设计明确审查范围、扩展条件、问题归属与可配置规则；实现按第 8 节分步计划推进。

## 1. 目标与成功指标

- 审查意见聚焦本次变更：默认只输出落在变更行（含高影响扩展纳入范围）的问题，禁止因单文件少量修改而扫描并上报整个文件的历史问题；
- 高影响变更不漏审：新增文件、公共接口签名、权限安全逻辑、配置文件、依赖声明、数据库脚本自动扩展审查范围；
- 归属可统计：每条结构化问题带 `origin=NEW/EXISTING`，后端按 Diff 行号映射打标，不信任模型自报；
- 范围可解释：每次执行落库范围决策快照（纳入/排除/扩展/截断及原因），可回查；
- 范围可配置：项目级排除规则与开关随任务快照冻结，改配置不改写历史结论。

## 2. 当前基线与缺口（2026-08-02 核查）

| 链路 | 现状 | 缺口 |
|---|---|---|
| LLM_DIRECT | GitHub Compare 拉全量 Diff（800KB 有界读取、400k 字符截断），整段文本经 `{{diff}}` 占位符进 Prompt | 无文件过滤、无排除规则；无范围指令，hunk 上下文行（未变更代码）上的问题会被模型一并上报；无新增/存量归属概念 |
| OCR_ENGINE | 真实工作区 + `ocr review --from base --to head`，范围完全交给 CLI | 平台无范围控制与决策记录；CLI 原生 `--exclude` glob 未使用 |
| 配置面 | 项目仅有主要技术栈、审查方式、模板、目标分支 | 无排除路径、无扩展开关、无存量问题策略 |
| 结果协议 v1.0 | `topIssues` 含 filePath/startLine/endLine | 无 `origin` 归属字段，无范围统计 |

关键存量资产：`GitHubPullRequestDiffFetcher` 有界读取、`ReviewPromptComposer` 平台协议追加、`OpenCodeReviewCliAdapter.writeDiffPatch`（已存在未启用）、OCR CLI `--exclude`。M3.2 在其上叠加范围决策，不改写执行骨架。

## 3. 审查范围分层定义

| 层 | 内容 | 角色 |
|---|---|---|
| L0 变更核心 | 本次 Diff 的新增/修改行 | **必审**，问题定位的唯一默认落点 |
| L1 结构上下文 | hunk 上下文行 + hunk header 函数签名（`@@ ... @@ funcname`，unified diff 自带）+ 高影响文件的完整内容 | 仅供理解，**不作为问题上报主体** |
| L2 调用上下文 | 公共签名变更时提示受影响调用方兼容风险（变更文件清单 + 签名前后对比） | 仅供理解；跨文件调用链分析依赖代码索引，列为后续候选，不在本切片 |
| L3 高影响扩展 | 命中第 4 节条件的文件，扩展为整个文件或关联模块 | 扩展纳入后视为可上报范围，归属 `NEW` |

规则：问题必须定位在 L0 或 L3 纳入范围；L1/L2 只帮助模型判断，不产生问题。模型被要求只报变更引入的问题；后端再用行号映射复核打标，双保险。

## 4. 高影响扩展条件（命中任一即扩展）

| 条件 | 判定方式（确定性，非模型判断） | 扩展范围 |
|---|---|---|
| 新增文件 | Diff `new file mode` | 整个文件（全部行为新增） |
| 删除文件 | Diff `deleted file mode` | 不扩展，仅记录；不审被删内容 |
| 公共方法/接口签名变更 | 变更行匹配 `public/protected` 方法声明、接口/导出符号定义（按语言的正则规则集，首版 Java + JS/TS/Vue + Python + Go） | 整个文件 + 提示调用方兼容性 |
| 权限/安全逻辑 | 路径或变更行命中安全规则（auth/login/permission/credential/secret/crypto/token 等关键字 + 项目安全路径 glob） | 整个文件 |
| 配置文件 | `*.yml/yaml/properties/xml/toml/env`、CI/Docker/Nginx 等路径规则 | 整个文件 |
| 依赖声明 | `pom.xml`、`package.json`、`go.mod`、`requirements.txt` 等 | 整个文件 + 版本变更提示 |
| 数据库脚本 | `sql/**`、`*.sql`、迁移目录 | 整个文件 |

约束：扩展后总量仍受 `MAX_DIFF_CHARS` 上限约束；扩展规则集代码内置常量，路径 glob 可在项目配置追加。

### 4.1 分类顺序与边界规则（2026-08-02 复核补充）

每个文件按以下顺序落入唯一类别，**排除优先于高影响**：

1. **记录类**（不审、不扩展，仅落决策快照）：删除文件、二进制文件、纯改名（无 hunk）、gitlink 子模块指针、仅 mode 变更；
2. **排除**：平台默认排除 → 项目排除 glob → 测试文件（`scope_include_tests=N` 时）。锁文件（`package-lock.json`、`yarn.lock`、`go.sum` 等）始终排除——有效变更在 `package.json`/`go.mod` 等清单文件，不双重审查机器生成内容；
3. **高影响扩展**：命中第 4 节条件的幸存文件；
4. **普通纳入**：其余文件的 L0 hunk。

精确化两点：

- **新增文件的整文件扩展是免费的**：unified diff 的新增文件 hunk 已含全部行，无需拉取全文；全文拉取（L3）仅针对**已存在文件**（签名/安全/配置/依赖/DB 脚本），由执行层按 head SHA 拉取。单文件拉取失败退回 L0（保留其 hunk），快照记 `expansionDegraded`，不阻断审查；
- **两段式预算**：① 全部纳入文件的 L0 hunk 优先保留，极端超限时从普通文件起按**文件边界**整文件丢弃并记 `droppedFiles`（不切断 hunk）；② L3 扩展全文在剩余预算内按规则优先级（SECURITY > DEPENDENCY > DB_SCRIPT > CONFIG > SIGNATURE）整文件纳入或整文件跳过并记录，不半切文件内容误导模型。

> **L0 预算丢弃优先级（2026-08-02 实施复核修正）**：殤 A 先按规则优先级纳入 expanded 的 L0，单/累计超限的整文件丢弃并置 `expandedOverflow`；殤 B 仅当 expanded 未溢出时，普通文件在剩余预算内纳入（溢出整文件丢）。**expanded 一旦溢出，普通文件全部让位记 `droppedFiles`**，不顶占漏审的扩展预算——保证审查聚焦高影响文件，避免「审了普通文件却漏审高影响」的假完整。该规则只影响超大 PR（L0 累计 > `MAX_DIFF_CHARS`），日常 PR 不触发。

### 4.2 解析健壮性

GitHub 服务端可能截断超大 Diff（尾部半行/残缺 hunk）。解析器遇不可解析尾部必须 graceful 停止并记 `parseWarnings`，不抛异常；整体解析失败才由执行层降级为全量 Diff 行为（第 9 节）。

## 5. 问题归属（NEW / EXISTING）

- **打标在后端**：解析统一 Diff 建立「文件 → 变更行区间」映射；模型返回的问题按 `filePath + startLine/endLine` 判定——落在变更行区间或 L3 扩展文件内为 `NEW`，否则为 `EXISTING`。模型自报归属仅作参考，不作判定依据。
- **邻近宽限（2026-08-02 复核补充）**：变更可能使相邻上下文行出错（如改了调用方）。问题行未命中新增行、但落在同一含新增行的 hunk 内且与最近新增行距离 ≤ 3 行时，仍判 `NEW`；其余上下文行判 `EXISTING`。解析层同时产出 `addedLineRanges` 与 `hunkRightRanges` 支撑判定。
- **EXISTING 默认处理**：不进 Top 3、不计 `focus_issue_count`、不影响评分与结论；`reportExistingIssues=N`（默认）时直接从结构化结果剔除并计数落库，`=Y` 时保留并标注，仅信息展示。
- **无法判定时**：文件不在 Diff 中（模型编造/扩展文件）或行号缺失，按 `NEW` 计但落 `originUnverifiable` 计数；连续出现视为提示词/解析问题排查。
- **协议升级 v1.1**：`topIssues[]` 增 `origin`；结果增 `scopeStats`（includedFiles/excludedFiles/expandedFiles/truncated/newCount/existingCount）。v1.0 结果按兼容解析，`origin` 缺省视为 `NEW`。

## 6. 可配置规则

| 配置 | 落点 | 默认 |
|---|---|---|
| 平台默认排除 | 代码内置常量：锁文件、生成代码、`vendor/dist/node_modules`、二进制、minified、快照类测试资源 | 始终生效 |
| 项目排除路径 glob | `review_project.scope_exclude_patterns`（换行分隔，≤2000 字符） | 空 |
| 审查测试文件 | `review_project.scope_include_tests` | `N`（排除 `*Test.java`、`*_test.go`、`*test*/**` 等） |
| 上报存量问题 | `review_project.scope_report_existing` | `N` |
| 高影响扩展 | `review_project.scope_expand_enabled` | `Y` |

- 四项项目配置随 M3 既有快照机制冻结进任务快照（`updateTaskSnapshot` 同批落库），执行只读快照；
- 项目表单新增「审查范围」分区展示与编辑，复用 `review:project:edit` 权限，不新增菜单；
- 范围配置不进审查模板正文，属于平台范围策略，与评分协议一样由平台统一控制。

## 7. 两条执行路径统一

- **LLM_DIRECT**：Diff 解析 → 文件级范围决策（排除/扩展）→ 生成 scoped diff（仅纳入文件的 hunks + 扩展文件完整内容段）→ 替换 `{{diff}}` 注入；平台协议追加范围指令块（只报变更引入问题、归属规则、扩展说明）。
- **OCR_ENGINE**：同一份范围决策的平台排除 + 项目排除合并后经 CLI 原生 `--exclude` 传入；扩展文件由 CLI 在工作区内自然可见（整文件本就存在于工作区），范围决策快照同样落库。`--preview` 可用于排障对照。
- 统一口径的保证：范围决策（哪些文件审、哪些排除、哪些扩展）由平台确定性代码完成，两条路径共用同一决策服务与快照结构。

### 7.1 步 3-4 实施定稿（2026-08-02 实施前复核补充）

- **webhook 建单链路补列**：`ReviewProjectMapper.selectByRepository`（webhook 受理的项目查询）必须查询范围四列，否则自动建单冻结的 scope 恒为 NULL、项目配置不生效；
- **快照列可空 + 执行层默认**：任务范围快照列保持可空，M3.2 前冻结的历史任务为 NULL；`ReviewScopeConfig.fromTaskSnapshot` 将 NULL 归一为平台默认（不审测试/不报存量/开启扩展），历史任务行为不变；
- **扩展全文段位置**：决策服务输出的 scoped diff 无文件内插入点，全文段统一**追加在 scoped diff 末尾**，带 `===== 高影响扩展文件完整内容（规则：X，变更行见上方 Diff）：path =====` 标题；第二段预算（剩余 = `MAX_DIFF_CHARS` − L0 已用）由 `ReviewScopePromptAssembler` 整文件纳入或整文件跳过；
- **拉取保护**：单文件全文上限 256KB（`MAX_EXPANDED_FILE_BYTES`，超限记 `FILE_TOO_LARGE` 降级），单次执行拉取数量上限 30（`MAX_EXPANDED_FETCH_COUNT`，超出记 `FETCH_LIMIT_SKIPPED`）；contents API 用 `raw` media type 免 base64；
- **范围指令块与协议解耦**：`ReviewScoringConstants.scopeInstructionBlock(scopeApplied, hasFullContent)` 位于模板正文与输出协议之间；决策失败降级全量 Diff 时不出现"已经过平台范围筛选"表述，但"只报变更引入问题"约束始终生效；origin 输出要求不在本块，归步 5 协议 v1.1；
- **空范围**：`effectiveFileCount=0` 时不调用模型，任务/执行记录按 `SUCCESS + PASS` 落库，summary 说明"无有效审查范围"；
- **决策快照**：`review_task_run.scope_decision_json`（mediumtext，OCR 路径步 6 复用同列），内容 = 决策服务快照 + 扩展文件最终处置（IN_DIFF/FULL/BUDGET_SKIPPED/DEGRADED/FETCH_LIMIT_SKIPPED）+ 生效配置 + `finalDiffChars`；决策异常记 `degraded=DECISION_FAILED`。

### 7.2 步 5-7 实施定稿（2026-08-02 实施前复核补充）

- **归属判定先于 Top 3 截断**：若模型把存量问题排进前三，先截后剔会导致存量占位、其后新增问题丢失。解析管线为：校验 → 按严重度排序 → 逐条归属判定 → 剔除/保留存量 → 新增问题截断 Top 3 → 重排 rank；`focusIssueCount` 只计新增；
- **`hasCriticalSecurityIssue` 旗标与存量 CRITICAL**：布尔旗标无法关联到具体 issue，打标生效时 `BLOCK` 需「旗标为真 且 存在 origin=NEW 的 CRITICAL」；存量 CRITICAL 不再阻断。未打标（决策降级/无分类器）保持旗标即阻断；
- **协议双版本兼容**：解析接受 `"1.0"`/`"1.1"`（1.0 为真子集），落库统一 `"1.1"`，避免模型偶发回写旧版本号被误判格式异常；
- **scopeStats 后端注入**：`result_json.scopeStats`（included/excluded/expanded/truncated/newCount/existingCount/originUnverifiable），计数为截断/剔除前发现总数；决策降级时 scopeStats 与 origin 打标整体缺省；
- **OCR `--exclude` 语义**（CLI 实跑校准）：逗号分隔 gitignore 风格；平台 glob 语法兼容；含逗号的项目 glob 无法表达，剔除并记 `skippedExcludePatterns`；测试文件 glob 在 `includeTests=N` 时合并传入；
- **OCR 决策快照独立结构**：`pathMode=OCR_ENGINE` + 分类结果（excluded/expanded/recordOnly）+ `appliedExcludeGlobs` + 生效配置 +「L0 预算截断不适用」说明；不照搬 LLM 的 includedFiles/truncated 字段（CLI 在真实工作区审查 --exclude 之外的全部变更文件）；Diff 不可用或决策异常 → 不加排除规则（引擎全量审查）+ 快照记 `DIFF_UNAVAILABLE`/`DECISION_FAILED`，审查不阻断；
- **前端展示**：任务详情与记录详情的执行记录表加「范围决策快照」展开行（共享 `ScopeDecisionView` 组件，含生效配置与降级标记）；记录详情重点问题卡片加归属标签，存量问题（`reportExisting=Y` 保留时）独立分区展示、不计入重点问题分级统计；v1.0 历史结果无 origin 不显示标签。


## 8. 分步实现计划

| 步 | 内容 | 验证 | 状态（2026-08-02） |
|---|---|---|---|
| 1 | 统一 Diff 解析器：文件/hunk/变更行区间/new·deleted file 识别（`com.acr.review.scope.UnifiedDiffParser`） | 单测覆盖标准/新增/删除/重命名/二进制/截断尾部，11 例 | ✅ 已完成 |
| 2 | 范围决策服务：默认排除 + 项目配置 + 高影响规则 + 优先级截断，输出 scoped diff 与决策快照（`ReviewScopeDecisionService` / `ReviewScopeRules` / `ReviewScopeConfig` / `ReviewScopeDecision` / `GlobPattern`） | 单测覆盖各扩展条件、排除优先级、截断顺序，13 例 | ✅ 已完成（纯确定性代码，未接入执行链，不改变现有行为） |
| 3 | 项目配置四列 + SQL 脚本（`sql/22_review_scope_config.sql`）+ 快照冻结 + 项目表单「审查范围」tab | SQL 幂等实跑；快照服务/配置转换单测 7 例；表单保存回显联调（归一化生效） | ✅ 已完成 |
| 4 | LLM 路径接入 scoped diff + 范围指令块 + 决策快照落库（`GitHubFileContentFetcher` / `ReviewScopePromptAssembler` / `executeLlmPath` 接入） | 单测：排除/扩展/降级/空范围/预算跳过/指令块变体 15 例；全量 158 例绿 | ✅ 已完成 |
| 5 | 归属打标（`IssueOriginClassifier`，邻近宽限 ≤3 行）+ 协议 v1.1（topIssues.origin + scopeStats）+ EXISTING 剔除/保留 + v1.0 兼容 | 单测：分类器 9 例、解析管线 5 例、执行链 2 例 | ✅ 已完成 |
| 6 | OCR 路径接入 `--exclude`（`ReviewScopeRules.mergedExcludeGlobs` + 适配器参数）+ 决策快照落库 | 单测：适配器 2 例、规则合并 4 例、执行链 2 例 | ✅ 已完成 |
| 7 | 范围决策快照在任务/记录详情可见（`ScopeDecisionView` 展开行 + 归属标签 + 存量分区） | 前端生产构建通过；记录/任务详情 API 透出 `scopeDecisionJson` 联调核对 | ✅ 已完成 |

每步独立可验收；步 1–2 先行合入不改变现有行为，步 4/6 接入后生效。

## 9. 验收标准与风险

**验收**：构造含「小改动 + 历史问题」的 PR，结果只报变更引入问题；构造新增文件/签名变更/配置变更 PR，扩展范围生效且决策快照可回查；全部文件被排除时任务按「无有效审查范围」完成（结论 PASS、不调用模型、summary 说明）；历史 v1.0 结果展示不受影响。

**风险与对策**：

- Diff 解析失败 → 降级为现有全量 Diff 行为并记录降级原因，不阻断审查；
- 行号映射误判 → 宁缺毋滥：误判为 EXISTING 会漏报新增问题，故映射区间取 hunk 全部新增行，边界宁宽；
- OCR `--exclude` 语义与平台 glob 差异 → 以 CLI 实跑校准，差异记入文档；
- 扩展导致 Prompt 变长 → 截断优先级 + 决策快照中的 truncated 标记保证可解释；
- 签名变更的语言正则误报 → 扩展只是多审不漏审，代价为 token，可接受并可通过开关关闭。

## 10. 非范围

- 跨文件调用链分析、方法/类 AST 级边界提取（L2 完整能力，依赖代码索引，后续候选）；
- 全量扫描/Push 审查的范围策略（规模化版单独立项）；
- 质量门禁联动（score_threshold 仍预留，不在本切片启用）；
- GitHub 评论回写、通知、问题整改闭环（M4 及以后）。
