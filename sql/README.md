# SQL 文件规范

## 初始化策略

分两条路径，按环境状态二选一：

### 新装环境：一次性初始化（推荐）

执行 `init-full.sql` 一条命令完成全部初始化（库、46 张表结构、菜单/字典/参数/定时任务/内置审查模板等初始数据），等效于按序号执行完 `01`–`43` 全部增量脚本后的最终状态：

```bash
mysql --default-character-set=utf8mb4 -u root -p < sql/init-full.sql
```

- 脚本自带 `CREATE DATABASE IF NOT EXISTS ai_code_review`，无需预先建库；
- 含 `DROP TABLE`，可重复执行，但重复执行会重建全部表结构与初始数据，**已有业务数据的环境严禁使用**；
- 仅包含初始化数据，不含项目、凭据、任务、问题、投递等业务数据；
- 初始管理员 `admin / admin123`，首次登录后立即修改密码。

### 存量 / 升级环境：按序号执行增量脚本

已在共享环境执行过的库，继续按文件名前缀从小到大执行**尚未执行过**的增量脚本，不得使用 `init-full.sql`。含中文的增量脚本必须显式指定连接字符集：

```bash
mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/NN_xxx.sql
```

## 增量脚本清单

1. `01_core_schema.sql`：初始化系统核心表和基础数据。
2. `02_quartz_schema.sql`：初始化 Quartz 调度表。
3. `03_system_management.sql`：初始化模型配置、业务系统和相关权限。
4. `04_github_project_access.sql`：增加 GitHub 凭据与代码项目接入能力。
5. `05_github_pr_scope.sql`：增加 GitHub PR 审查范围配置。
6. `06_llm_model_service.sql`：升级 LLM 模型服务配置。
7. `07_review_engine.sql`：增加本地审查引擎菜单与权限。（已知问题：引擎按钮号段 1130-1132 与 04 凭据按钮冲突，由 12 修复，脚本本身按变更约束不再修改）
8. `08_github_pr_webhook.sql`：增加 GitHub PR Webhook 事件与审查任务。
9. `09_llm_custom_provider.sql`：大模型配置支持自定义厂商，并统一菜单文案。
10. `10_llm_menu_charset_fix.sql`：修复大模型配置菜单中文乱码（须 utf8mb4 连接）。
11. `11_llm_column_comment_charset_fix.sql`：修复 `custom_provider_name` 列注释乱码（须 utf8mb4 连接）。
12. `12_review_engine_button_fix.sql`：修复 07 引擎按钮号段冲突（改用 1136-1138），并清除角色 2 被误授的凭据按钮权限。
13. `13_review_pipeline_m3.sql`：M3 审查执行配置、任务执行摘要、执行历史表、重试权限与字典。
14. `14_review_dual_mode_prompt.sql`：双审查方式（`LLM_DIRECT`/`OCR_ENGINE`）、提示词表与菜单、任务详情权限补齐、执行快照提示词字段。
15. `15_review_template_config.sql`：将提示词升级为审查模板，并增加项目技术栈与模板执行快照。
16. `16_review_scoring_result_protocol.sql`：统一评分与结构化结果协议字段、失败类型字典、内置模板正文升级。
17. `17_review_project_engine_code_nullable.sql`：放宽 `review_project.engine_code` 可空，修复大模型审查保存失败。
18. `18_review_execution_hardening.sql`：放宽 `review_task_run.snapshot_review_mode` 可空、失败类型字典补 `RATE_LIMIT`、下线未使用的模板复制按钮（1148）。
19. `19_review_record_experience_m3_1.sql`：M3.1 审查记录体验（任务元数据字段、审查记录菜单；须 utf8mb4）。
20. `20_review_record_charset_fix.sql`：修复 19 在非 utf8mb4 连接下执行导致的菜单/列注释乱码。
21. `21_review_record_list_fields.sql`：补充 `changed_files`，校正 PR 发起人列注释（须 utf8mb4）。
22. `22_review_scope_config.sql`：M3.2 审查范围项目配置（`review_project` 增 `scope_exclude_patterns`/`scope_include_tests`/`scope_report_existing`/`scope_expand_enabled` 四列）、`review_task` 增对应四快照列（可空，NULL 按平台默认）、`review_task_run` 增 `scope_decision_json`（须 utf8mb4）。
23. `23_review_delivery_record.sql`：M4 GitHub PR 总结评论投递记录表 `review_delivery_record`、投递状态字典、审查记录下「投递重试」权限（须 utf8mb4）。
24. `24_notification_management_m5.sql`：M5 通知渠道表 `review_notify_channel`、项目 `notify_*` 三列、渠道/投递字典、`review.ui.base-url`、一级菜单「通知管理」及权限（须 utf8mb4）。
25. `25_issue_ledger_m6.sql`：M6 问题台账表 `review_issue` / `review_issue_action`、状态/来源/归属字典、审查中心「问题台账」菜单与权限（须 utf8mb4）。
26. `26_issue_delivery_trace_m6_1.sql`：M6.1 投递记录追溯（`review_delivery_record.trigger_source` 可空列 + 触发来源字典；须 utf8mb4）。
27. `27_sidebar_menu_ia.sql`：左侧菜单信息架构调整（审查中心 / 项目接入 / 策略配置；工作台侧栏由前端常量路由控制；须 utf8mb4）。
28. `28_delivery_menu_route_name.sql`：补齐「投递记录」菜单 `route_name`（缺失时路由名与前端组件名不一致，keep-alive 不缓存导致页面一直加载）。
29. `29_multi_git_provider_access.sql`：多 Git Provider 数据层（凭据 `server_url`、项目/事件 `repository_full_path`、平台字典、投递渠道扩展、MR/PR 事件参数；须 utf8mb4）。
30. `30_delivery_content_snapshot.sql`：投递正文快照（`review_delivery_record` 增 `content_snapshot` mediumtext 可空列，历史不回填；须 utf8mb4）。
31. `31_issue_lifecycle_m8.sql`：M8 问题生命周期（`review_issue` 增 `family_key`/`missed_streak`/`last_seen_head_sha`/`last_missed_run_id`/`recheck_task_id`/`recheck_run_id`/`recheck_commit_sha` 七列与 `idx_issue_pr_family` 索引；RECHECKING 字典改「待复核」并去预留备注、auto_recheck 去预留备注；新增转复核阈值与协议清单上限两个参数；幂等；须 utf8mb4）。
32. `32_issue_pr_close_source.sql`：M8.1 PR 关闭/合并联动关闭来源字典（`review_issue_close_source` 增 `pr_merged` / `pr_closed`；幂等；须 utf8mb4）。
33. `33_push_review_m10.sql`：M10 推送审查（项目推送开关/分支、任务事件来源、问题参考分支、四平台 Push 参数与字典；须 utf8mb4）。
34. `34_review_task_scheduling_recovery.sql`：企业级架构风险修复 S2（任务变更键、数据库调度、租约/epoch fencing、重试恢复参数与字典；升级前须停止旧实例；须 utf8mb4）。
35. `35_review_delivery_recovery.sql`：企业级架构风险修复 S3（持久化投递意图、租约领取、自动退避、人工处置与统一补发状态机；存量失败记录不自动补发；须 utf8mb4）。
36. `36_inline_comments_m11.sql`：M11 行内评论（`review_delivery_record.issue_id`、项目 `inline_comment_enabled`/`inline_severities`、四平台行内渠道字典；须 utf8mb4）。
37. `37_data_insights_m12.sql`：M12 数据洞察一期（`review_stats_daily` 聚合表、数据洞察菜单与 `insight:*:view` 权限、指标口径版本参数；须 utf8mb4）。
38. `38_data_insights_m12_2.sql`：M12 数据洞察二期（`review_commit_fact` / `review_member_stats_daily` / `review_insight_identity_claim`、成员分析菜单与 `insight:team:view`；须 utf8mb4）。
39. `39_review_resource_budget.sql`：企业级架构风险修复 S5（有界审查/投递执行池、项目并发、Git/工作区/OCR/LLM 预算参数；须 utf8mb4）。
40. `40_review_runtime_ops.sql`：企业级架构风险修复 S6（运行告警阈值、优雅停机参数、运行概览菜单与 `review:runtime:view`/`review:task:cancel`/`review:task:handle` 权限；须 utf8mb4）。
41. `41_sys_job_seed.sql`：基础定时任务种子（数据洞察聚合任务 `insightStatsJobTask.refreshRecent`/`fullRecalc` 幂等注册，用户无需手工创建；须 utf8mb4）。
42. `42_identity_binding.sql`：M12 身份关联（`sys_user_identity`、claim 存量迁移、`insight:identity:manage`；须 utf8mb4）。
43. `43_member_stats_lines.sql`：M12 补充成员增删行数（`review_member_stats_daily` 增 `additions_sum`/`deletions_sum`；须 utf8mb4）。

## init-full.sql 维护规则

- `init-full.sql` 是增量脚本执行完成后的最终状态快照（2026-08-11 同步，含 01–43 全部增量中的结构/参数/菜单/定时任务种子）。
- **新增编号增量脚本后必须同步重新生成**，否则新装环境会缺失该脚本的变更。生成方式（在已执行全部增量脚本的本地库上）：

```bash
# 1. 导出表结构最终态（去除 AUTO_INCREMENT 当前值）
docker exec mysql8 mysqldump -uroot -proot --default-character-set=utf8mb4 --single-transaction \
  --skip-comments --add-drop-table --no-data ai_code_review \
  | sed -E 's/ AUTO_INCREMENT=[0-9]+//g' > /tmp/acr_schema.sql

# 2. 导出初始化数据（仅脚本体系内的种子表，勿含业务数据）
docker exec mysql8 mysqldump -uroot -proot --default-character-set=utf8mb4 --single-transaction \
  --skip-comments --no-create-info --complete-insert --extended-insert=FALSE \
  ai_code_review sys_user sys_user_role sys_role sys_role_menu sys_dept sys_menu \
  sys_dict_type sys_dict_data sys_config sys_job review_template > /tmp/acr_seed.sql

# 3. 按 init-full.sql 现有头部注释格式组装：文件头说明 + CREATE DATABASE/USE + 结构 + 数据；
#    更新头部「生成日期」与「基线」提交号。
```

- 重新生成后必须在空库执行一次验证（建临时库导入，与源库对比表清单、结构与种子数据行数），通过后删除临时库。

## 命名规则

- 文件名统一使用 `NN_purpose.sql` 格式。
- `NN` 为两位十进制连续序号，从 `01` 开始，表示全局执行顺序。
- `purpose` 使用小写英文 `snake_case`，准确描述脚本用途，不再附加日期。
- 新增脚本使用当前最大序号加一，不插号、不复用或调整已有序号。例如，下一个脚本应命名为 `30_xxx.sql`。
- 文件名中的顺序必须与脚本依赖一致；新增脚本前应先确认所有前置表、字段、菜单或配置已由更小序号的脚本创建。
- 文件重命名后，必须同步更新 README、部署文档和 SQL 前置说明中的引用。

## 变更约束

- 已在共享环境执行的脚本不得修改执行语句；后续结构或数据变更应新增下一个连续序号脚本。
- 不得通过复制旧脚本保留无序号或日期命名的兼容副本，避免同一逻辑被重复执行。
- 新增增量脚本必须同步重新生成 `init-full.sql`（见上节）。
- SQL 内容、幂等策略和执行方式应遵守 `rules/delivery.md`。
