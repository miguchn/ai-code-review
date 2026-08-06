# GitHub 接入

这篇帮你完成 GitHub 仓库接入：创建访问令牌 → 在平台建凭据与项目 → 配置 Webhook → 验证 PR 能自动触发审查。

## 第 1 步：创建访问令牌

令牌用于平台读取仓库代码、Diff 与 PR 信息，推荐创建 Fine-grained token（权限可收敛到单仓库）。

**方式 A：Fine-grained token（推荐）**

1. 登录 GitHub，右上角头像 → **Settings** → 左侧最底部 **Developer settings** → **Personal access tokens** → **Fine-grained tokens** → **Generate new token**；
2. Token name 填可识别的名称，如 `acr-review`；Expiration 建议设置有效期（如 90 天）并记录轮换时间；
3. Repository access 选 **Only select repositories**，勾选要接入的仓库；
4. Permissions 按最小权限勾选：
   - `Contents` 选 **Read**（读取代码与 Diff，必选）；
   - `Pull requests` 选 **Read**（读取 PR 信息与提交列表，必选）；
   - 需要把审查总结评论回写到 PR 时，`Pull requests` 改选 **Read and write**；
5. 点击 **Generate token**，立即复制生成的令牌（形如 `github_pat_xxxx…`）。令牌只显示一次，离开页面后无法再查看。

**方式 B：Classic token**

Settings → Developer settings → Personal access tokens → **Tokens (classic)** → Generate new token (classic)，勾选 `repo` 权限即可，生成后复制令牌（形如 `ghp_xxxx…`）。

## 第 2 步：在平台创建凭据

1. 进入「项目接入 → 访问凭据」，点击「新增」；
2. 字段填写：
   - 凭据名称：如 `GitHub-审查机器人`；
   - Git 平台：选择 `GitHub`；
   - Token：粘贴第 1 步生成的令牌；
   - 状态：正常；
3. 保存后点击该行的「检测」，提示成功说明令牌有效、权限足够。

## 第 3 步：接入项目并配置 Webhook

**平台侧**：进入「项目接入 → 代码项目」新增项目（Git 平台选 GitHub，填仓库地址如 `https://github.com/your-org/your-repo`，选择上一步的凭据，启用合并请求审查并选目标分支与审查方式）。保存后再次编辑该项目，切换到 **Webhook** 页签：

1. 回调地址：复制「回调地址」输入框中的值，形如 `https://acr.example.com/webhook/github`；
2. Webhook Secret：点击「随机生成」生成一个并**另行记录**（保存后页面不回显明文），然后保存项目；

> Webhook Secret 留空将导致所有事件验签失败，必须配置。

**GitHub 侧**：进入仓库 **Settings → Webhooks → Add webhook**：

1. Payload URL：粘贴平台回调地址；
2. Content type：选择 `application/json`（必须）；
3. Secret：填入平台的 Webhook Secret；
4. Which events would you like to trigger?：选 **Let me select individual events**，只勾选 **Pull requests**；
5. 点击 **Add webhook** 保存。

> PR 的具体动作（opened / synchronize / reopened）由平台侧动作白名单控制，默认三者都触发审查，无需在 GitHub 单独配置；closed、labeled 等其他动作即使投递过来也会被平台自动忽略。

## 第 4 步：验证连通

1. 在仓库新建分支提交任意改动，向目标分支（如 `main`）发起 PR；
2. GitHub 侧：仓库 Settings → Webhooks，点击该 Webhook 查看 **Recent Deliveries**，应看到 2xx 响应；
3. 平台侧：项目 Webhook 页签的「最近接收」显示「已受理合并请求 #N，生成审查任务 #N」；
4. 「审查中心 → 审查任务」出现新任务（待执行/执行中），说明链路打通。

## 常见错误

| 现象 | 原因与处理 |
|---|---|
| Recent Deliveries 显示 4xx/超时 | 回调地址不可达或不是 `/webhook/github` 结尾，见「常见问题 → Webhook 没触发怎么办」 |
| 平台事件记录显示「Webhook 签名校验失败」 | Secret 不一致：在项目 Webhook 页签重新随机生成并保存，再同步更新到 GitHub Webhook 的 Secret |
| 「未匹配到已接入的代码项目」 | 项目仓库地址填错或 Git 平台选错，事件按仓库全路径精确匹配 |
| 凭据「检测」失败 | 令牌过期、权限不足或仓库未授权，重新生成令牌并更新凭据 |
| Recent Deliveries 正常但没有审查任务 | 事件被平台按规则忽略，见「常见问题 → Webhook 没触发怎么办」第 2 步 |

> 提示：GitHub Webhook 页面的 **Test delivery** 按钮发送的是 ping 事件而非真实 PR：项目已接入时平台记录为「非合并请求事件（ping）」，未接入时记录为「未匹配到已接入的代码项目」，不会创建审查任务，验证请以真实 PR 为准。
