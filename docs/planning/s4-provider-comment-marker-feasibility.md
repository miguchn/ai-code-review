# S4 四平台总结评论 marker 可行性确认表

> 对应企业级架构风险修复设计 §5.3 第 5 条 / §8.1 S4。  
> 确认对象：总结评论「按 marker 查找 → 编辑更新」能力；行内评论不在本表范围。

检索约定：正文包含稳定常量 `<!-- acr-review-summary -->`（`ReviewDeliveryConstants.COMMENT_MARKER`）。  
S4 起新建评论在其后追加 `<!-- acr-run:{runId} -->`；查找仍只用原 marker，存量评论可命中。

| 平台 | Provider | 列表评论 API | 按 marker 检索 | 编辑/更新评论 | 创建评论 | 限流与分页策略（实现现状） | 结论 | 降级策略 |
|---|---|---|---|---|---|---|---|---|
| GitHub | `GITHUB` | `GET /repos/{owner}/{repo}/issues/{pr}/comments` | 支持：响应 `body` 本地 `contains(marker)` | 支持：`PATCH .../issues/comments/{id}` | 支持：`POST .../issues/{pr}/comments` | `per_page=100`，最多 3 页（合计 ≤300）；遇限流按投递退避 | **可用：查找—编辑—更新** | 无需降级 |
| GitLab | `GITLAB` | `GET /projects/{id}/merge_requests/{iid}/notes` | 支持：note `body` 本地 `contains(marker)` | 支持：`PUT .../notes/{id}` | 支持：`POST .../notes` | 同上分页上限；项目路径经 URL 编码；遇限流按投递退避 | **可用：查找—编辑—更新** | 无需降级 |
| Gitee | `GITEE` | `GET /repos/{owner}/{repo}/pulls/{number}/comments`（issue 评论列表语义） | 支持：`body` 本地 `contains(marker)` | 支持：更新评论接口（既有 `GiteePullRequestCommentClient`） | 支持：创建评论接口 | 同上分页上限；token query/header 按适配层处理；遇限流按投递退避 | **可用：查找—编辑—更新** | 无需降级 |
| Gitea | `GITEA` | `GET /repos/{owner}/{repo}/issues/{index}/comments` | 支持：`body` 本地 `contains(marker)` | 支持：`PATCH .../issues/comments/{id}` | 支持：`POST .../issues/{index}/comments` | 同上分页上限；兼容 `/api/v1` serverUrl；遇限流按投递退避 | **可用：查找—编辑—更新** | 无需降级 |

## 证据落点（代码）

| 平台 | 客户端 |
|---|---|
| GitHub | `acr-review/.../git/github/GitHubPullRequestCommentClient.java` |
| GitLab | `acr-review/.../git/gitlab/GitLabPullRequestCommentClient.java` |
| Gitee | `acr-review/.../git/gitee/GiteePullRequestCommentClient.java` |
| Gitea | `acr-review/.../git/gitea/GiteaPullRequestCommentClient.java` |

统一契约：`GitPullRequestCommentClient#findCommentWithMarker` / `#updateIssueComment` / `#createIssueComment`。  
单元测试均覆盖「旧格式 marker 可命中并更新」。

## 能力不足时的统一降级（本期未触发）

若某平台后续失去编辑能力：追加新评论，并在正文标注「替代任务/run」关系；投递记录不得静默丢弃。本期四平台均具备编辑能力，不启用降级。

## SQL 字典说明

`review_task_status.SUPERSEDED`（「已被替代」）已由 `sql/34_review_task_scheduling_recovery.sql` 幂等写入，本切片不重复增量脚本。
