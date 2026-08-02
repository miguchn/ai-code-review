# Changelog

## [Unreleased] - 2026-08-02

### M3.2 审查范围策略（设计 + 步 1-4 实现）

- 新增设计文档 `docs/planning/review-scope-policy-m3.2.md`：审查范围默认以本次 Diff 变更行为核心（禁止单文件少量修改即扫描上报整个文件历史问题），高影响变更（新增文件/公共签名/权限安全/配置/依赖/数据库脚本）自动扩展；问题按后端 Diff 行号映射区分新增与存量（协议 v1.1 `origin`），存量默认不进 Top 3 与评分；项目级范围配置随任务快照冻结；LLM 注入 scoped diff、OCR 经 `--exclude` 统一口径，范围决策快照落库
- 步 1-2 已落地（`com.acr.review.scope`，未接入执行链、不改变现有行为）：统一 Diff 解析器（文件/hunk/右侧变更行区间/新增·删除·改名·二进制·gitlink·mode-only 识别，残缺尾部容错记 warnings）；范围决策服务（记录类→默认排除→项目排除→测试文件→高影响→普通的分类顺序，scoped diff 按文件边界截断，扩展按 SECURITY > DEPENDENCY > DB_SCRIPT > CONFIG > SIGNATURE > NEW_FILE 优先级，决策快照 `toSnapshotMap`）；新增 24 例单测
- 步 3 项目范围配置：`review_project` 增四列（`scope_exclude_patterns` 换行分隔 glob ≤2000、`scope_include_tests`/`scope_report_existing` 默认 N、`scope_expand_enabled` 默认 Y）并随任务快照同批冻结（`review_task` 增四快照列，可空，NULL 按平台默认执行，历史任务行为不变）；项目表单新增「审查范围」tab，复用 `review:project:add/edit` 权限；webhook 建单链路 `selectByRepository` 同步补列；幂等脚本 `sql/22_review_scope_config.sql`
- 步 4 LLM 路径接入：执行时 Diff 解析 → 范围决策 → 高影响扩展全文按 head SHA 经 GitHub contents API（raw，单文件 256KB 上限、单次 30 个上限）拉取并竞争剩余预算（整文件纳入或跳过），scoped diff 替换 `{{diff}}` 注入；平台协议前追加范围指令块（只报变更引入问题、上下文行仅供理解、扩展段说明，决策降级时不出现"已筛选"表述）；全部文件被排除时不调用模型按 PASS 落库并说明；决策异常降级全量 Diff 不阻断审查；决策快照（含扩展处置 IN_DIFF/FULL/BUDGET_SKIPPED/DEGRADED/FETCH_LIMIT_SKIPPED 与生效配置）落 `review_task_run.scope_decision_json`；新增 20 例单测

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
