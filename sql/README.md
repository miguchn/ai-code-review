# SQL 文件规范

## 执行顺序

数据库脚本必须按文件名前缀从小到大执行：

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

含中文的增量脚本执行时请显式指定连接字符集，例如：

```bash
mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/09_llm_custom_provider.sql
mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/10_llm_menu_charset_fix.sql
mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/11_llm_column_comment_charset_fix.sql
mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/13_review_pipeline_m3.sql
mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/14_review_dual_mode_prompt.sql
mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/15_review_template_config.sql
mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/16_review_scoring_result_protocol.sql
mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/17_review_project_engine_code_nullable.sql
mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/18_review_execution_hardening.sql
```

## 命名规则

- 文件名统一使用 `NN_purpose.sql` 格式。
- `NN` 为两位十进制连续序号，从 `01` 开始，表示全局执行顺序。
- `purpose` 使用小写英文 `snake_case`，准确描述脚本用途，不再附加日期。
- 新增脚本使用当前最大序号加一，不插号、不复用或调整已有序号。例如，下一个脚本应命名为 `09_xxx.sql`。
- 文件名中的顺序必须与脚本依赖一致；新增脚本前应先确认所有前置表、字段、菜单或配置已由更小序号的脚本创建。
- 文件重命名后，必须同步更新 README、部署文档和 SQL 前置说明中的引用。

## 变更约束

- 已在共享环境执行的脚本不得修改执行语句；后续结构或数据变更应新增下一个连续序号脚本。
- 不得通过复制旧脚本保留无序号或日期命名的兼容副本，避免同一逻辑被重复执行。
- SQL 内容、幂等策略和执行方式应遵守 `rules/delivery.md`。
