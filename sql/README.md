# SQL 文件规范

## 执行顺序

数据库脚本必须按文件名前缀从小到大执行：

1. `01_core_schema.sql`：初始化系统核心表和基础数据。
2. `02_quartz_schema.sql`：初始化 Quartz 调度表。
3. `03_system_management.sql`：初始化模型配置、业务系统和相关权限。
4. `04_github_project_access.sql`：增加 GitHub 凭据与代码项目接入能力。
5. `05_github_pr_scope.sql`：增加 GitHub PR 审查范围配置。
6. `06_llm_model_service.sql`：升级 LLM 模型服务配置。
7. `07_review_engine.sql`：增加本地审查引擎菜单与权限。
8. `08_github_pr_webhook.sql`：增加 GitHub PR Webhook 事件与审查任务。

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
