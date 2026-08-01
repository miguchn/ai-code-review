# P0/M1 GitHub 项目接入设计

## 1. 目标与范围

本切片交付可独立验收的 GitHub 项目接入：平台管理员维护 GitHub PAT 凭据，项目负责人维护代码项目并绑定业务系统、部门、负责人和凭据，系统可真实调用 GitHub API 检测凭据或仓库连接。

本次只实现 GitHub；不实现 Webhook、代码克隆、Diff、审查任务、PR 评论、通知、规则、看板及 GitLab/Gitee/Gitea 占位代码。

## 2. 当前基线与处理结论

- `acr-review` 仅有 Maven 依赖边界，本次按真实代码创建最小目录。
- 业务系统已保存部门与负责人，普通用户只可见自己负责的业务系统；用户、部门和角色数据权限继续复用。
- 代码项目通过 `business_system_id` 归属业务系统，模型为一个业务系统关联多个代码仓库项目。业务系统数据和接口仍属于 `acr-system`，其唯一菜单入口移动到“代码审查”，避免与项目管理形成重复入口。
- 数据库采用按日期保存 SQL 文件的方式；本次新增增量脚本，不修改历史初始化脚本。
- 现有模型 Key 仅做展示脱敏，数据库仍是明文，没有可复用的服务端可逆加密能力。本功能不沿用该风险实现。
- 前端使用动态菜单、Element Plus 列表/弹窗和 `TableDataInfo` / `AjaxResult` 响应约定。

## 3. 数据对象与关系

### GitHub 凭据 `review_git_credential`

保存名称、Provider、认证方式、AES-GCM 密文、启停状态、最近检测结果和审计字段。`(provider, credential_name)` 唯一；状态和最近检测状态建查询索引。Token 不进入查询响应。

### 代码项目 `review_project`

保存项目名称、规范化仓库地址、owner/repository、默认分支、审查分支范围、业务系统、部门、负责人、凭据引用、启停状态、最近检测结果和审计字段。`(provider, repository_owner, repository_name)` 唯一；业务系统、部门、负责人、凭据、状态均建索引。

项目引用凭据；删除被项目引用的凭据前由业务服务检查并明确拒绝。仓库地址解析出的 owner/repository 由服务端覆盖保存，不信任客户端输入。

## 4. 安全与数据权限

- PAT 使用 AES-256-GCM 加密，每条密文使用随机 12 字节 IV；主密钥只从环境变量 `ACR_CREDENTIAL_MASTER_KEY` 读取，值为 Base64 编码的 32 字节密钥。
- Token 字段只写、密文字段忽略 JSON 序列化；列表和详情 SQL 不读取密文。编辑不提交 Token 时保留原值，修改 Token 必须重新输入。
- 操作日志全局排除 `token` 和 `tokenCiphertext`；Provider 不记录请求头、Token、GitHub 原始错误正文或带用户信息的 URL。
- 凭据管理按独立 RBAC 权限开放，默认仅超级管理员拥有；项目列表复用部门角色数据范围，并要求非管理员是业务系统负责人或项目负责人。新增/修改同时校验业务系统、部门、负责人和凭据有效性。

## 5. Provider 边界与失败分类

`GitProvider` 是 `acr-review` 内部的最小外部适配契约，提供仓库地址解析、PAT 验证和仓库连接检测。当前只有 `GitHubProvider` 实现，不创建其他平台目录或类。

GitHub Provider 支持 `https://github.com/{owner}/{repo}[.git]` 和 `git@github.com:{owner}/{repo}[.git]`，调用 `/user` 与 `/repos/{owner}/{repo}`。统一结果分类：地址错误、凭据无效、权限不足、仓库不存在、网络/API 异常、请求超时。GitHub 对无权访问的私有仓库可能返回 404，本期按“仓库不存在或不可见”提示，同时使用独立的凭据检测帮助区分失效 Token。

连接检测是用户显式触发的同步外部调用，设置连接/读取超时，不自动重试，不产生其他外部副作用。

## 6. API 与权限

| 资源 | API | 权限 |
|---|---|---|
| 项目 | `GET /review/project/list`、`GET /review/project/{id}` | `review:project:list/query` |
| 项目 | `POST /review/project`、`PUT /review/project`、`DELETE /review/project/{ids}` | `review:project:add/edit/remove` |
| 项目 | `PUT /review/project/{id}/status`、`POST /review/project/{id}/test` | `review:project:status/test` |
| 凭据 | `GET /review/credential/list`、`GET /review/credential/{id}` | `review:credential:list/query` |
| 凭据 | `POST /review/credential`、`PUT /review/credential`、`DELETE /review/credential/{ids}` | `review:credential:add/edit/remove` |
| 凭据 | `POST /review/credential/{id}/test` | `review:credential:test` |

Controller 只负责参数校验、权限、操作日志与用例调用，不访问 Mapper。

## 7. 页面与菜单

左侧新增一级目录“代码审查”，按“业务系统管理 → 项目管理 → 访问凭据”排列；“业务系统管理”复用原页面、API 和 `system:businesssystem:*` 权限，只移动唯一菜单入口。一级菜单按核心业务优先排序，“系统管理”和“系统监控”统一靠后。页面沿用现有查询表单、表格、状态标签、分页和弹窗；项目页提供新增、编辑、删除、启停和连接测试，凭据页提供新增、编辑、删除和连接测试。Token 使用密码输入框，编辑时始终为空。

## 8. 验收标准

1. 增量 SQL 可在本地 MySQL 执行，表、唯一键、索引、一级目录、二级菜单及其 13 项功能权限完整。
2. PAT 密文可解密用于 GitHub API，但数据库、列表/详情响应、操作日志和异常均不出现明文。
3. 项目可绑定业务系统、部门、负责人和凭据，仓库地址由 Provider 校验并解析；重复仓库被唯一键和业务校验阻止。
4. 凭据和项目连接测试能保存统一结果，错误类别和超时提示明确；被引用凭据不可删除。
5. 后端测试、根 Maven 构建和前端生产构建通过；本地启动后浏览器完成菜单、CRUD、启停、连接测试和不回显 Token 的核心流程。

## 9. 实施状态（2026-08-01）

上述代码、SQL、权限、菜单和页面已完成。增量 SQL 已在本地开发 MySQL 重复执行并验证；浏览器已完成业务系统、凭据和项目的新增/编辑/删除、未检测项目启用拦截、引用凭据删除拦截、Token 不回显和暗黑模式检查。真实 GitHub API 已验证无效 PAT 的安全失败路径；因未提供可用测试 PAT，浏览器未验证私有仓库成功连接，Provider 成功响应由自动测试覆盖。

## 10. M1 补充切片：面向 PR 审查的仓库信息与分支范围

### 10.1 目标与成功指标

在不实现 Webhook 和实际审查的前提下，把 GitHub 项目配置补齐到后续 MVP PR 审查可直接消费的状态：用户填写仓库地址并选择公共凭据后，由系统读取仓库 owner、仓库名、默认分支和全部分支；项目只配置是否启用 PR 审查及 PR 目标分支。

成功指标：

- GitHub 分支超过 100 条时可分页读取完整列表；
- owner、repository、默认分支和分支数量均来自 GitHub，不要求用户手填；
- 新项目默认启用 PR 审查，并优先推荐实际存在的 `dev`、`develop` 等长期分支；
- PR 目标分支只能从本次读取的真实分支中选择，来源分支默认全部允许；
- 保存最近分支同步状态、结果和时间；同步失败不覆盖上一次成功读取的仓库元数据；
- 后续 Webhook 可直接按项目仓库和 PR base 分支判断是否应审，无需按动态来源分支预建配置。

### 10.2 当前基线与缺口

- 已有 GitHub Provider 可解析仓库地址、验证 PAT、读取仓库基本信息和默认分支，但尚未读取分页分支；
- `review_project.review_branches` 是手工填写的通配范围，不符合“目标分支选择优先”的 PR 审查模型；
- 项目弹框为平铺表单，仓库技术字段和业务字段混排，缺少读取、推荐、查看全部分支和刷新操作；
- 系统已有 `sys_config` 参数能力，可承载平台统一默认值，无需新增配置中心。

### 10.3 本次范围与非范围

本次范围：GitHub 仓库信息读取、全部分支分页获取、PR 审查启用开关、PR 目标分支多选、长期分支默认推荐、公共默认参数、同步状态，以及对应 API、页面和测试。

本次不实现：Webhook、Push 审查、PR 事件持久化、审查任务、Diff/代码获取、SHA 处理、OCR、评论或状态回写；不增加其他 Git Provider 实现，也不保存动态来源分支清单。

### 10.4 数据、唯一性与公共参数

继续使用 `review_project` 的 `(provider, repository_owner, repository_name)` 唯一键，不新建分支配置表。增量脚本将旧 `review_branches` 收敛为 `pr_target_branches`，并增加：

- `pr_review_enabled`：是否启用 PR 审查，`0` 启用、`1` 停用；新项目默认启用；
- `pr_target_branches`：已选择的 PR 目标分支，去重后以逗号保存；
- `last_branch_sync_status`：`UNSYNCED`、`SUCCESS`、`FAILED`；
- `last_branch_sync_message`、`last_branch_sync_time`：最近同步结果和时间。

历史项目的 `*` 通配配置迁移为空并暂时关闭 PR 审查，避免未经真实分支读取就扩大审查范围。分支列表仅在读取响应中用于展示和选择，不为每个分支建表，也不持久化全部分支快照。

公共参数复用 `sys_config`：

- `review.github.longLivedBranches`：`dev,develop,main,int,uat`；
- `review.github.robotBranchPrefixes`：默认机器人分支前缀；
- `review.github.prEvents`：`opened,reopened,synchronize`。

这些参数由系统统一维护，项目表不复制，普通项目用户只在折叠的高级设置中查看生效值。

### 10.5 Provider、API、权限和流程

`GitProvider` 增加“读取仓库信息”能力，GitHub 实现依次验证凭据、读取仓库元数据，并按 `per_page=100` 分页读取 `/repos/{owner}/{repo}/branches`。平台差异不进入项目用例。

多平台扩展采用最小统一契约：`GitProvider.providerCode()` 是 Provider 选择键，地址解析、凭据校验、仓库授权、仓库信息和分支读取均通过 Provider 接口返回平台无关结果；GitHub API 路径、状态码解释和认证头只允许存在于 `git/github` 适配器。项目用例只依赖 `GitProvider`，不得依赖 `GitHubProvider` 或拼接 GitHub API。当前只有 GitHub Bean，不创建 GitLab、Gitee 或企业自建平台空实现。

Webhook、PR 事件读取和仓库授权模式在本轮没有实际业务调用，因此不提前扩充接口方法。后续实现可信事件切片时，应在 `acr-review` 的外部适配边界增加经过真实用例验证的平台无关能力，并通过 `providerCode` 选择实现；PR 编号、base/head SHA 等业务事实进入统一事件对象，GitHub 请求签名、事件头和载荷差异仍留在 GitHub 适配器。

新增 `POST /review/project/repository-info`，请求只包含项目 ID（可选）、仓库地址和凭据 ID，响应包含规范仓库地址、owner、repository、默认分支、全部分支、分支数量、推荐目标分支及统一失败原因。该操作复用 `review:project:test` 权限；Controller 只做校验、权限和用例调用。

读取是用户显式触发的只读外部调用，不自动重试，也没有 GitHub 写副作用。已有项目使用当前已保存的仓库和凭据刷新时，成功则更新仓库元数据与同步状态，失败只记录失败状态；新增或修改关键接入配置时，保存前由服务端重新校验目标分支属于 GitHub 实际分支，防止绕过前端提交任意通配规则。

后续 Webhook 约束固定为：按仓库接收 PR 事件，以项目配置的 `pr_target_branches` 匹配 base 分支；来源分支不预配置；实际任务必须保存事件中的 PR 编号、base SHA 和 head SHA，不能用分支当前 HEAD 替代事件版本。

### 10.6 页面结构与失败场景

项目新增、编辑弹框按“基本信息、仓库接入、业务归属、PR 审查范围、状态与备注”分区。仓库接入区提供公共凭据下拉、凭据管理快捷入口和“读取仓库信息”；成功后展示仓库全名、默认分支、分支数量和同步时间。分支使用可搜索多选，并提供“查看全部分支”和“刷新分支”，不在弹框内铺满。

高级设置默认折叠，只展示来源分支全部允许、机器人分支前缀和 PR 触发事件。静态说明紧邻对应表单项，使用辅助文字层级，不单独占用大块提示区域。

失败继续区分地址错误、凭据无效、权限不足、仓库不存在或不可见、网络/API 异常和超时；页面保留用户已经填写的业务归属和备注，明确提示可执行的处置方式，不显示 Token 或 GitHub 原始响应正文。

### 10.7 分步实施与验证

1. 新增增量 SQL和参数数据；验证列定义、历史数据迁移和参数唯一性。
2. 扩展 Provider 与分页测试；验证多页分支、失败分类和请求不泄露 Token。
3. 扩展项目用例、Mapper 和 REST；验证推荐顺序、目标分支真实性、同步成功/失败持久化及数据权限。
4. 调整项目弹框和列表；验证读取回填、多选、查看全部、刷新、折叠高级设置和凭据快捷入口。
5. 执行后端测试、根 Maven 构建、前端生产构建和本地浏览器核心流程。

### 10.8 验收风险

- 未提供可用 PAT 时，真实 GitHub 成功路径只能由 MockWebServer 自动测试覆盖，浏览器至少验证地址/凭据失败路径和响应脱敏；
- GitHub 私有仓库无权限时可能返回 404，继续提示“仓库不存在或当前 Token 不可见”；
- 不持久化全部分支意味着重新调整目标分支前需要刷新 GitHub 分支，这是有意的最小设计，避免形成易过期的分支资产表。

### 10.9 实施状态（2026-08-01）

本补充切片已完成。增量 SQL 已在本地开发 MySQL 连续执行两次，新增列和三项 `sys_config` 参数均通过校验；GitHub Provider 已覆盖 102 个分支跨页读取及 `dev` 推荐测试。项目页面已按五个业务分区重组，仓库读取说明改为紧邻凭据操作的行内辅助文字，读取失败同位置显示脱敏错误；重复打开弹框会重置到顶部。

浏览器已验证一级菜单、项目与凭据页面、凭据新增/编辑不回显 Token、真实 GitHub API 无效 PAT 分类提示、业务系统选择后部门自动带出、负责人下拉、PR 范围和高级设置布局，以及浅色/暗色模式。浏览器产生的临时凭据和业务系统均已删除。由于没有可用 PAT，本轮未在浏览器中完成成功分支回填和项目保存；对应成功、分页、推荐链路由 MockWebServer 自动测试覆盖。
