# Changelog

## [Unreleased]

### 问题生命周期管理（M8）

- 协议 v1.2：模型输出全量问题清单（上限 20，运行期参数 `review.protocol.maxIssues` 可调），Top3 降级为展示层概念；解析兼容 1.0/1.1/1.2；`focusIssueCount` 由后端重算为 NEW 问题中 CRITICAL/HIGH 数量；协议附录仍由代码注入，审查模板种子数据无需变更
- 物化扩围：全量清单 NEW 问题全部入账台账（不再限 Top3）；EXISTING 口径维持现状
- 轮次对账：每轮 SUCCESS（仅 LLM_DIRECT 且解析成功）后按指纹四步对账——精确命中清零连续未命中 / 单义族合并（措辞漂移保护，行身份跟随新指纹）/ 新物化 / 未命中判定；连续未命中达阈值（默认 1，运行期参数 `review.issue.recheck.missedRoundsThreshold`）自动转「待复核」并附证据（触发轮任务 / commit / 连续轮数），**不自动关闭**，关闭必须人工确认；待复核问题被再次命中自动复活为待修复
- 对账守卫：同 commit 重跑不计未命中、同 run 重复对账幂等、终态不复活、OCR 轮次不参与；对账异常隔离，不影响任务状态与投递
- 状态机开放 RECHECKING：人工「确认已修复并关闭」（close_source=auto_recheck）与「未修复，重新打开」（`PUT /review/issue/{issueId}/reopen`，复用 `review:issue:close` 权限）；确认/忽略对确认待复核保持拒绝；无新权限串、无新菜单
- 台账默认「当前活跃」视图（activeFlag，与 status 筛选并存且 status 优先）；详情抽屉新增生命周期时间线（含系统动作）与复核证据块（含「疑似关联新发现问题」展示层查询）；工作台新增「待复核问题」卡片
- 总结评论与 IM 迷你报告按全量清单渲染 Top3 与「共 N 个问题，其余见问题台账」；本轮触发复核时追加「疑似已修复（N）」段（≤3 标题省略），处置后评论重渲染时该段从当前台账派生、保持与台账一致，派生失败静默降级
- 脚本 `sql/31_issue_lifecycle_m8.sql`（幂等）；后端测试与前端生产构建通过

### 工作台重设计

- 首页工作台整体重设计：信息架构重组为「今日待办（6 卡）/ 快捷操作 / 项目风险趋势（近 14 天堆叠柱状图）/ 审查任务状态（今日摘要）/ 审查结果概览（近 7 天结论分布与占比条）/ 模型运行情况 / 最近动态」七个区块，双列布局（≥1200px 主侧 2:1，窄屏单列堆叠）；面板/状态/间距全部接入主题 CSS 变量，浅色与暗黑模式自动适配；趋势图 ECharts 按需引入，浅/暗双套色板经可访问性对比验证
- 新增 `GET /workbench/trend?days=14`：审查结论（PASS/WARN/BLOCK）与执行失败按天聚合（与审查记录同 DataScope，缺失日期补零，days 上限 31）；无 `review:record:list` 权限返回 null，前端隐藏趋势与概览区块
- 新增 `GET /workbench/models`：启用模型健康摘要（登录可调、字段白名单脱敏），派生 `checkStatus`（SUCCESS/FAILED/NEVER）与在线数/最近检测时间汇总
- 待办卡按问题归属拆分（5→6 卡）：新增「存量待确认」（origin=EXISTING）；「待修复问题」口径扩为全部归属（修复队列不区分本次变更/存量，卡片计数与列表条数保持一致）；卡片新增 `subtitle` 归属副标题；最近动态条目新增 `conclusion` 结论字段（前端状态色点映射）
- 既有接口行为不变：summary/today/scope/recent 口径与列表 DataScope 对账逻辑保持 M7 约定；后端 371 例测试与前端生产构建通过

### 缺陷修复

- 修复项目管理中 Webhook Secret 配置状态恒显示"未配置"：列表/详情 SQL 按安全设计不读取密文列，原 `fillWebhookView` 却用恒为空的密文字段判断配置状态；改为 `projectSelect` 直接输出布尔列 `webhook_secret_configured`（列表与详情同步修正），密文不出服务端的边界不变

### 体验优化

- 项目表单 Webhook Secret 增加「随机生成」按钮：`acr_` 前缀 + 32 位小写 hex（16 字节 CSPRNG，前端生成），一键填入且允许自行填写；新建/未配置态提示补充「留空将导致 Webhook 全部验签失败」，编辑已配置态文案不变；纯前端改动，加密存储与响应不回显边界不变（2026-08-04 联调发现 Secret 自填且强制验签、留空无提示易漏填）

### IM 通知迷你报告式重构与投递正文快照

- IM 通知重构为「迷你报告」版式：标题行（结论图标 + 总分）→ 提交信息块（提交人 / 源→目标分支 / Commit 信息 / 审查时间 / 合并请求链接）→ 审查结果（逐条四层：标题 / 位置 / 描述 / 建议，描述与建议压平换行后按 120 字截断）→ 范围统计 → 总结 → 「查看合并请求 / 查看审查详情」双链接；仅使用标题 / 加粗 / 列表 / 链接四种三渠道（企微 / 钉钉 / 飞书）交集语法；FAILED 消息仅保留标题 + 提交信息块 + 失败类型 + 详情链接
- `ReviewSummaryContent` 新增三字段：commitMessage（run.commitMessages 首行）/ summaryText（run.resultSummary）/ reviewTime（run.finishedTime）；渲染器保持纯函数不查库
- 飞书新增「简化 Markdown → post 富文本」转换：标题 / 加粗标签 → 加粗段落、列表项 → 圆点段落、markdown 链接 → 链接元素，其余降级普通段落；仍不使用互动卡片
- `review_delivery_record` 新增 `content_snapshot` 列（mediumtext、可空、历史不回填）：JSON `{"kind","channelType","title","body"}`，IM 与总结评论投递均落库，重试原地覆盖以最近一次为准；列表查询仅输出轻量布尔标记 `has_content_snapshot`，不拉取大字段
- 新增「查看消息内容」端点 `GET /review/delivery/record/{deliveryId}/content`：复用 `review:delivery:list` 权限与列表同口径部门数据范围；投递记录列表增加「消息详情」抽屉（marked 渲染 + DOMPurify 消毒、主题 CSS 变量浅暗适配、链接新标签页打开），无快照行置灰并提示「较早投递未保留消息快照」
- 脚本 `sql/30_delivery_content_snapshot.sql`；后端 378 例测试与前端生产构建通过

### 权限收缩（数据范围底线加固）

- 修复项目表单 `options` 超出部门可见面的暴露：非管理员用户的部门收敛为「本部门 + 子部门树」（过滤停用 / 已删除），负责人候选收敛为上述部门范围内用户；无所属部门用户 fail-close 返回空列表；管理员全量、业务系统既有按用户过滤、凭据 / 模板 / 模型等平台级共享资源保持现状
- `repository-info` 明确设计边界：Git 凭据为平台级共享资源，由管理员统一维护，访问控制依赖 `review:project:test` 权限串 + 操作日志审计；@Log 文案平台中立化为「读取仓库信息」

### 开源协议

- 开源协议由 MIT 切换为 Apache License 2.0：LICENSE 全文替换（版权行 Copyright 2026 MiguCHN），README（中英文）重组为标准开源结构（徽章、核心特性、快速开始上移、首个仓库接入指引、文档索引、贡献指引）并同步协议说明；切换由唯一版权持有人完成，历史版本仍按 MIT 授权

### 待改进

- 【产品分析】问题生命周期管理——多轮审查下的问题聚合与整改自动复核（2026-08-04 联调经典案例 PR#3：两轮审查 6 条记录、语义上仅 3 个活跃问题；漏洞修复后复审 27→78 分但台账无任何修复信号回写）：
  - 整改自动复核：同一 PR 新一轮审查未命中既有问题指纹时，自动转 `RECHECKING`（状态与 `auto_recheck` 关闭来源已在常量预留，本期未实现），附验证依据（轮次任务/commit），人工复核后关闭；不直接自动关闭（LLM 输出不确定性，title 措辞变化会产生新指纹），可评估"连续 N 轮未命中"阈值
  - 台账视图聚合：列表默认"当前活跃"视图，PR 维度提供问题时间线（发现→不再命中→复核关闭），同族问题合并展示，历史全量保留；解决反复审查导致的清单膨胀与统计失真
  - 验收基线案例：PR#3 两轮联调（task#6 查出 SQL 注入/命令注入/硬编码密码 → 修复提交 → task#7 复审未命中原问题、新发现修复代码 3 个质量问题），作为该功能的回归测试原型
- 【产品分析】问题处置效率优化——逐条人工确认负担重（2026-08-04 联调实测：6 个问题逐条确认/关闭/驳回，操作重复枯燥）：后续迭代需评估批量处置（批量确认/关闭/忽略/误报）、按严重级别差异化默认策略（如 LOW 级默认忽略仅通知）、结合复审证据的"建议已修复"智能预判一键确认等方向，由产品经理定交互方案
- 【授权红线】凭据 / 通知渠道 / 审查模板为无部门归属的平台级共享资源，仅靠权限串把关：部门级角色严禁授予 `review:credential:*` / `review:notify:*` 权限串，否则可列出并管理全局凭据（含密文操作）；如需多部门凭据隔离应单独立项（凭据 + 渠道一并加 `dept_id` 归属，联动 CRUD / 表单 / 数据范围）
- 【待真实渠道验收】IM 迷你报告在企微不支持列表语法（`- ` / `1.` 前缀渲染为纯文本，一行一条仍可读）、飞书 post 可能压缩问题行缩进：三渠道真实发送截图验收后确认观感（2026-08-05 重构已完成，验收未做）

### 文档与 SQL 收口（MVP 功能面收口后）

- MVP（V0.1）功能面收口定稿：README（中英文）、部署文档、产品路线图的当前能力、模块清单与初始化指引更新至与实际功能一致；路线图基线更新至 2026-08-04 并补充 MVP 达成状态
- 新增 `sql/init-full.sql` 一次性初始化脚本（01–29 全部增量执行完成后的最终状态快照，含表结构与初始数据）：新环境一条命令完成初始化，存量环境继续按编号增量脚本升级；`sql/README.md` 补充双轨初始化策略与重新生成规则；`docs/deployment.md` 与本地 `init-local.sh` 同步
- GitHub 真实环境全链路验收通过后（2026-08-04）：README（中英文）研发阶段声明与版本规划 MVP 行更新为「GitHub 真实环境全链路验收通过」；路线图 §7.3 补充验收记录（四类审查剧本、各环节结果、权限边界确认与遗留项），§2.1 基线补充 Secret 状态显示缺陷修复条目

## [0.3.0] - 2026-08-04

MVP（V0.1）功能收口版本：M1–M7 纵向切片 + 四平台 Git Provider 全部落地，待真实环境验收。

### 多平台 Git Provider 接入（GitLab / Gitee / Gitea）

- 统一访问上下文 `GitAccessContext` 与按 `providerCode` 解析的 `GitAdapterRegistry`；业务层不再硬编码单个 GitHub Bean
- 项目与 Webhook 事件以 `repository_full_path` 作为跨平台唯一匹配键；凭据表新增 `server_url` 支持 GitLab/Gitea 自建实例（GitHub/Gitee 默认官方地址）
- Webhook 接入路由：`POST /webhook/{github,gitlab,gitee,gitea}`；各平台验签、动作映射与 Delivery 去重收敛在 `git/{github,gitlab,gitee,gitea}` 适配包
- 投递渠道扩展：`GITLAB_MR_SUMMARY_COMMENT`、`GITEE_PR_SUMMARY_COMMENT`、`GITEA_PR_SUMMARY_COMMENT`；幂等键 `{provider}:{projectId}:{prNumber}:SUMMARY_COMMENT`（GitHub 旧键与 `GITHUB_PR_SUMMARY_COMMENT` 不变）
- 前端凭据/项目页支持平台选择、`server_url` 与分平台 Webhook/Token 说明；MR/PR 外链按平台生成
- GitHub 存量行为与自动测试全量回归通过；GitLab/Gitee/Gitea 以契约测试覆盖，**未声称**真实生产环境闭环验收
- 设计：`docs/superpowers/specs/2026-08-03-multi-git-provider-access-design.md`；计划：`docs/superpowers/plans/2026-08-03-multi-git-provider-access.md`；脚本：`sql/29_multi_git_provider_access.sql`
- Review 修复（一）：Gitee/Gitea 合并请求外链改为 `/pulls/{n}`（前后端同源，含 `.git`/尾斜杠清理加固）；Gitee 五个适配 Bean 补 `@Autowired`、执行服务删除失效的平台直注入（两者均阻断启动，已真实启动验证）；deployment 文档明确 GitHub/Gitee 凭据 `server_url` 必须留空
- Review 修复（二）：Webhook 项目匹配改为严格 `repository_full_path` 单键，删除 owner/name 兜底（该兜底无合法命中场景，仅引入错绑风险），未匹配事件记录载荷 fullPath 便于排障；Gitee 不可映射动作记 IGNORED 而非 FAILED
- Review 修复（三）：IM 通知与工作台文案统一为平台中立「合并请求」；投递结果 upsert 改为平台中立命名，失败消息脱敏下沉到 `GitTokenSanitizer`；前端切换平台清空仓库地址残留、`server_url` 增加格式预校验、禁用态补充 tooltip、读取仓库信息后展示完整路径，平台兜底常量收敛至 `constants/gitProviders.js`

### 左侧菜单信息架构调整

- 侧栏中度对齐路线图：工作台（侧栏可见）→ 审查中心 → 项目接入 → 策略配置 → 通知管理 → 系统管理 → 系统监控
- 「代码审查」更名为「审查中心」（问题台账 / 审查任务 / 审查记录）；新建「项目接入」（业务系统 / 访问凭据 / 代码项目）；「模型服务」更名为「策略配置」并收拢审查模板
- 系统管理、系统监控子树不变；权限串与后端接口不变；因菜单父级迁移同步前端路由前缀（`/project-access/*`、`/model-service/template`）
- 设计：`docs/superpowers/specs/2026-08-03-sidebar-menu-ia-design.md`；脚本：`sql/27_sidebar_menu_ia.sql`

### M7 基础工作台

- 登录后首页 `/index` 替换为行动工作台：范围与健康、权限驱动待办卡（最多 5 张）、今日摘要、最近动态；零项目引导态仅对有 `review:project:list` 且 `projectCount=0` 的用户触发
- `GET /workbench/summary`：登录可调、无新权限串；`cards` 数据驱动；计数复用各列表同款 `@DataScope`；今日指标无权限返回 `null`（前端「—」）
- 问题/任务/记录/投递四个列表支持 route query 回填筛选；任务页回填 `taskStatus` 时复位 `queueOnly=false`；工作台 `onActivated` 刷新
- 设计文档：`docs/planning/workbench-m7.md`；无新表/新权限/新字典

### M6.1 问题处置与投递记录追溯打磨

- 处置接口（confirm/close/dismiss）失败时返回 `commentSyncFailureMessage` + `deliveryId`；前端展示具体原因并可跳转投递记录页（无定位参数）
- `review_delivery_record` 新增可空列 `trigger_source`（`TASK_SUCCESS` / `ISSUE_DISPOSITION` / `MANUAL_RETRY`）；三口赋值，原地更新以最近一次为准；历史不回填
- 投递记录列表展示「最后尝试时间」「触发来源」；失败原因保留截断 + tooltip 全文；问题台账详情抽屉展示该 PR 总结评论投递摘要并可跳转
- 设计文档：`docs/planning/issue-delivery-trace-m6.1.md`；脚本：`sql/26_issue_delivery_trace_m6_1.sql`

### M6 问题台账基础闭环

- 审查 SUCCESS 后将 `top_issues_json` 物化为 `review_issue`（PR 级指纹去重，FAILED 不物化）；支持确认 / 关闭 / 忽略 / 误报，动作写入 `review_issue_action`
- 状态机：`AWAITING_CONFIRM → AWAITING_FIX`，终态 `CLOSED` / `IGNORED` / `FALSE_POSITIVE`；忽略/误报原因必填，关闭说明选填；`RECHECKING` 仅预留
- 处置成功后重渲染该 PR 的 M4 总结评论（按指纹挂处置态）；评论失败不回滚处置，可走投递重试补偿
- 审查中心「问题台账」菜单与 `review:issue:*` 权限；任务/记录详情 Top3 联动 `issueId` 与处置徽标；存量 `EXISTING` 入账但不计入未关闭统计口径
- 设计文档：`docs/planning/issue-ledger-m6.md`；脚本：`sql/25_issue_ledger_m6.sql`

### M5 IM 三渠道通知与投递记录

- 审查 SUCCESS/FAILED 结束后按项目绑定向钉钉/企微/飞书群机器人投递结论摘要或失败简讯；与 GitHub 总结评论共用内容模型，投递失败不回滚任务状态
- 新增平台级 `review_notify_channel`（Webhook URL/加签 Secret 加密存储、测试发送、启停）；项目表单增加「通知」分区（启用开关、单渠道下拉、失败时通知）
- `review_delivery_record` 扩展 IM 三渠道与任务级幂等键；投递记录页支持筛选与按记录补发（`POST /review/delivery/record/{deliveryId}/retry`），保留 M4 按 taskId 重试 PR 评论
- 前端「通知管理」：通知渠道、投递记录两页；任务/记录详情展示 IM 投递摘要并链至投递记录
- 设计文档：`docs/planning/notification-management-m5.md`；脚本：`sql/24_notification_management_m5.sql`

### M4 GitHub PR 审查结果回写

- 审查 `SUCCESS` 后向 GitHub PR 回写/更新一条总结评论（结论、总分、Top3 新增/存量标签、范围统计）；正文含固定标记 `<!-- acr-review-summary -->`，同 PR 仅一条
- 新增 `review_delivery_record` 与幂等键 `GITHUB:{projectId}:{prNumber}:SUMMARY_COMMENT`；投递结束后一次性 upsert（`SUCCESS`/`FAILED`），失败不改写审查结论与任务状态；`FAILED` 任务不发评论
- 后台「投递重试」权限 `review:delivery:retry`：重试前重新 list 评论，并以该 PR 最近一次 SUCCESS 任务结论渲染，防止旧结论覆盖
- 任务/记录详情执行记录区展示投递状态，失败可重试，无记录的成功任务可补投递
- 设计文档：`docs/planning/review-comment-writeback-m4.md`；脚本：`sql/23_review_delivery_record.sql`

### M3.2 审查范围策略（设计 + 全部 7 步实现）

- 新增设计文档 `docs/planning/review-scope-policy-m3.2.md`：审查范围默认以本次 Diff 变更行为核心（禁止单文件少量修改即扫描上报整个文件历史问题），高影响变更（新增文件/公共签名/权限安全/配置/依赖/数据库脚本）自动扩展；问题按后端 Diff 行号映射区分新增与存量（协议 v1.1 `origin`），存量默认不进 Top 3 与评分；项目级范围配置随任务快照冻结；LLM 注入 scoped diff、OCR 经 `--exclude` 统一口径，范围决策快照落库
- 步 1-2 已落地（`com.acr.review.scope`，未接入执行链、不改变现有行为）：统一 Diff 解析器（文件/hunk/右侧变更行区间/新增·删除·改名·二进制·gitlink·mode-only 识别，残缺尾部容错记 warnings）；范围决策服务（记录类→默认排除→项目排除→测试文件→高影响→普通的分类顺序，scoped diff 按文件边界截断，扩展按 SECURITY > DEPENDENCY > DB_SCRIPT > CONFIG > SIGNATURE > NEW_FILE 优先级，决策快照 `toSnapshotMap`）；新增 24 例单测
- 步 3 项目范围配置：`review_project` 增四列（`scope_exclude_patterns` 换行分隔 glob ≤2000、`scope_include_tests`/`scope_report_existing` 默认 N、`scope_expand_enabled` 默认 Y）并随任务快照同批冻结（`review_task` 增四快照列，可空，NULL 按平台默认执行，历史任务行为不变）；项目表单新增「审查范围」tab，复用 `review:project:add/edit` 权限；webhook 建单链路 `selectByRepository` 同步补列；幂等脚本 `sql/22_review_scope_config.sql`
- 步 4 LLM 路径接入：执行时 Diff 解析 → 范围决策 → 高影响扩展全文按 head SHA 经 GitHub contents API（raw，单文件 256KB 上限、单次 30 个上限）拉取并竞争剩余预算（整文件纳入或跳过），scoped diff 替换 `{{diff}}` 注入；平台协议前追加范围指令块（只报变更引入问题、上下文行仅供理解、扩展段说明，决策降级时不出现"已筛选"表述）；全部文件被排除时不调用模型按 PASS 落库并说明；决策异常降级全量 Diff 不阻断审查；决策快照（含扩展处置 IN_DIFF/FULL/BUDGET_SKIPPED/DEGRADED/FETCH_LIMIT_SKIPPED 与生效配置）落 `review_task_run.scope_decision_json`；新增 20 例单测
- 步 5 归属打标与协议 v1.1：`IssueOriginClassifier` 按 Diff 行号映射判定问题归属（命中新增行、或同一含新增行 hunk 内距最近新增行 ≤3 行 → NEW；扩展全文文件整体 NEW；文件不在 Diff 或行号缺失按 NEW 计并单列 originUnverifiable；其余 EXISTING）；归属判定在排序后、Top 3 截断前执行，存量问题不占 Top 3 名额；EXISTING 不进 Top 3、不计 focusIssueCount、不影响评分与结论（`scope_report_existing=Y` 时标注保留仅信息展示）；`topIssues[].origin` + 后端注入 `scopeStats`；`hasCriticalSecurityIssue` 旗标在打标生效时需存在新增 CRITICAL 才阻断；解析兼容 1.0/1.1 双版本，落库统一 1.1；新增 16 例单测
- 步 6 OCR 路径范围接入：平台默认排除 + 测试文件（按开关）+ 项目排除合并后经 CLI 原生 `--exclude` 传入（逗号分隔 gitignore 风格，含逗号 glob 剔除并记快照）；OCR 决策快照独立结构落 `scope_decision_json`（分类结果 + 生效排除规则数 + 生效配置 + 截断不适用说明）；Diff 不可用或决策异常时不加排除规则、引擎全量审查不阻断；新增 8 例单测
- 步 7 范围决策前端可见：任务详情与记录详情的执行记录表新增「范围决策快照」展开行（共享 `ScopeDecisionView` 组件：纳入/排除/扩展/记录类/截断/降级/生效配置）；记录详情重点问题卡片新增归属标签（新增/存量），存量问题独立分区展示且不计入重点问题分级统计；v1.0 历史结果不显示归属标签

### M3.1 审查任务与审查记录体验优化

- 审查任务收敛为执行队列：默认仅展示待执行/执行中/失败，列表字段精简为项目、PR、分支、状态、步骤、执行次数、失败原因、创建时间与操作
- 新增「审查记录」菜单与列表/详情接口（复用 `review_task` / `review_task_run`，不新建业务表）；支持按项目、PR、提交者、结论与时间筛选
- 任务详情与记录详情改为独立页面，去掉高密度大抽屉；记录详情按「审查结果 / 执行记录」分页签展示
- 补充 PR 提交者与增删行数：Webhook 建单写入，执行时复用 PR 详情 API 回填；Commit Message 继续落在 run 表并在记录详情展示
- 新增幂等脚本 `sql/19_review_record_experience_m3_1.sql`；历史空字段前端统一显示「—」
- 审查记录：已结束任务（`SUCCESS` + `FAILED`）按完成时间倒序；列表展示项目/业务系统、可点击 PR、发起人 login、分支、代码变更（文件数/+/-）、结论映射（通过/建议修改/高风险/执行失败）、评分、重点问题分级（Top3）、完成时间；操作含查看详情、查看问题、打开 PR、失败重新执行
- 补充 `changed_files`（与 additions/deletions 同源，复用既有 PR 详情请求，不另调 GitHub）；不展示交付状态（待 M4）
- 设计文档：`docs/planning/review-record-experience-m3.1.md`

Review 修复（2026-08-02）：

- 任务/记录查询三处 `sys_business_system` 改 LEFT JOIN：业务系统被物理删除时（删除无引用校验），任务列表、任务详情、审查记录与执行/重试不再整体失效，业务系统名展示为「—」
- 任务详情「查看审查记录」入口覆盖 FAILED 任务（此前仅 SUCCESS 可见，与记录口径 SUCCESS+FAILED 不一致）
- 移除前端未使用的 `formatDiffLines` / `runTimelineType` 导出；修正 `router/index.js` 新增路由行尾与全文件一致

### M3 审查流水线独立 Review 加固

- 修复执行链断点：run 记录建立在配置解析之前并回填快照审查方式，执行期任意异常统一落 FAILED，消除任务卡死 RUNNING 的僵尸态；新增 `18_review_execution_hardening.sql` 放宽 `snapshot_review_mode` 可空
- RUNNING 超过 30 分钟视为执行中断，claimTask 支持超时领取，任务可人工重试回收（含前端重试入口与执行中自动刷新）
- 安全：PAT 改经 `GIT_CONFIG_*` 环境变量注入 git 子进程，不再出现在进程命令行参数；错误消息按 GitHub Token 格式正则兜底脱敏
- 健壮性：base/head SHA 入口格式校验；Diff 响应体有界读取（≤800KB）；GitHub 限流识别为独立失败类型 `RATE_LIMIT`；PR 元数据非 JSON 响应按不可用降级
- 权限：任务详情与重试接口补项目部门数据范围校验；移除未使用的模板复制端点（复用新增接口与 `review:template:add` 权限）
- 解析容错：模型输出支持任意位置 markdown 围栏与括号配平提取；评分维度缺失/行号倒置判格式异常；模板渲染改单趟替换防占位符二次展开；结论严重度改精确匹配防子串误判
- 任务列表查询不再拉取 mediumtext 快照正文
- 历史任务（快照冻结上线前建单）执行时按项目当前配置补冻结快照并落库；快照抽取为共享服务 `ReviewTaskSnapshotService`，建单与补冻结同一套校验；项目未配置时给出配置指引而非「不支持的审查方式：null」

## [0.2.0] - 2026-07-30

### 项目骨架复核

- 新增统一的 `acr-review` 核心业务模块边界，不预建业务类；
- 明确 `acr-admin` 只承担启动、配置和 Web 接入；
- 移除代码生成模块、演示接口、示例定时任务及对应前端、菜单和数据表；
- 收缩规划、skills、agents 和 rules，只保留当前阶段需要的最小协作骨架。

## [0.1.0] - 2026-07-30

### 初始化

- 基于 ApiHub 项目公共层重构，去除业务模块（doc/ai/asset）
- 项目重命名：apihub → ai-code-review（acr）
- 包名重构：com.apihub → com.acr
- 保留模块：common、system、framework、admin、quartz、generator、ui
- 保留能力：RBAC 权限、用户管理、字典、日志、定时任务、代码生成器、AI Client 抽象
- 创建 README.md、CLAUDE.md、部署文档
- 初始化 Git 仓库
