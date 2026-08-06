# Webhook 没触发怎么办

这篇帮你排查「发起 PR/MR 后平台没有生成审查任务」的问题，按从 Git 平台到平台内部的顺序逐步定位。

## 第 1 步：确认 Git 平台是否发出了事件

到 Git 平台查看该 Webhook 的投递记录：

- GitHub：仓库 Settings → Webhooks → 点击该 Webhook → **Recent Deliveries**；
- GitLab：项目 Settings → Webhooks → 点击该 Webhook → **Recent events**（最近事件）；
- Gitee：仓库 管理 → WebHooks → 查看该 Hook 的投递记录；
- Gitea：仓库 设置 → Webhooks → 点击该 Webhook 查看最近投递。

两种情况：

- **完全没有投递记录**：事件订阅不对。确认 Webhook 勾选的是 PR/MR 事件（GitHub: Pull requests；GitLab: Merge request events；Gitee: Pull Request；Gitea: Pull Request），push 等其他事件不会创建审查任务；
- **有投递但响应非 2xx 或超时**：多为回调地址不可达或配置错误，跳第 3、4 步；若响应状态码为 **413**，是 Webhook 载荷超过 256KB 上限，见第 2 步的「载荷超限」说明。

## 第 2 步：确认事件是否到达平台

投递返回 2xx 但没有审查任务时，说明事件已到达平台但被规则过滤。查看项目 Webhook 页签的「最近接收」提示（展示最近一次事件的处理结果），常见原因与处理：

| 处理结果 | 含义与处理 |
|---|---|
| 已受理合并请求 #N，生成审查任务 #N | 正常，去「审查中心 → 审查任务」查看 |
| 未匹配到已接入的代码项目 | 仓库未在平台接入，或项目仓库地址/平台选择与实际仓库不一致（按仓库完整路径精确匹配） |
| Webhook 签名校验失败 | Secret 不一致，见第 3 步 |
| 目标分支 X 不在审查范围 | PR 的目标分支不在项目「目标分支」列表，编辑项目补选 |
| 合并请求动作 X 不在启用范围 | 平台动作白名单默认为 opened / reopened / synchronize，其他动作（如 closed）本就忽略 |
| 项目未启用合并请求审查 | 编辑项目开启「启用合并请求审查」 |
| 项目已停用 | 启用项目 |
| 非合并请求事件 | 订阅了多余事件（如 push/issue），被忽略属正常 |

> 另有两种结果**不会**显示在「最近接收」中（发生在项目匹配之前），需管理员查看 Webhook 事件表：
> - **Webhook 载荷超过大小限制**（平台返回 413）：默认上限 256KB，超大载荷需管理员调整后端参数 `review.webhook.max-payload-bytes`；
> - **重复投递**：平台幂等保护，同一事件不会重复建任务，无需处理。

## 第 3 步：核对 Secret 是否一致

平台的 Webhook Secret 保存后不回显明文，不确定时最稳妥的做法是重设：

1. 编辑项目 → Webhook 页签 → Webhook Secret 点击「随机生成」→ 保存项目；
2. 把新值同步到 Git 平台的对应位置：GitHub Webhook 的 Secret、GitLab 的 Secret token、Gitee WebHook 的密码/签名、Gitea Webhook 的 Secret；
3. 再发起一次 PR 验证。

> Webhook Secret 留空会导致所有事件验签失败，必须配置。

## 第 4 步：确认回调地址可达

回调地址在项目 Webhook 页签的「回调地址」展示，格式为 `https://acr.example.com/webhook/{github|gitlab|gitee|gitea}`（按平台区分）。

1. 地址若显示为 `http://localhost:8080/...`，说明后端未配置对外可达的回调地址（环境变量 `ACR_WEBHOOK_CALLBACK_URL`），Git 平台无法访问本机地址，需联系管理员配置为 Git 平台可达的域名；
2. Git 平台在公网、本平台在内网时，需要通过反向代理/网关暴露回调地址；cloudflared 等隧道工具仅适合联调，不适合生产长期使用；
3. 快速验证可达性：在能访问 Git 平台的网络位置执行 `curl -i -X POST <回调地址>`，能收到平台返回（如「缺少 Webhook 投递 ID」的 JSON 提示）即说明链路可达。

## 常见误区

- 用 GitHub 的 **Test delivery** 测试：它发送的是 ping 事件而非真实 PR，平台会记录为「非合并请求事件（ping）」（项目已接入时）或「未匹配到已接入的代码项目」（未接入时），不会建任务，验证请以真实 PR 为准；
- 改了项目配置却没保存：目标分支、启用开关都需保存后才生效；
- 仓库地址带 `.git` 后缀或大小写与实际不一致：匹配按仓库完整路径精确进行。
