# GitLab 接入

这篇帮你完成 GitLab（含自建实例）仓库接入：创建访问令牌 → 在平台建凭据与项目 → 配置 Webhook → 验证 MR 能自动触发审查。

## 第 1 步：创建访问令牌

两种令牌二选一：

**方式 A：个人访问令牌（Personal Access Token）**

1. 登录 GitLab，右上角头像 → **Edit profile**（或 Preferences）→ 左侧 **Access Tokens**；
2. Token name 填 `acr-review`；Expiration date 建议设置（如 90 天）；
3. Scopes 按最小权限勾选：
   - 只读代码与 MR：勾 `read_api`；
   - 需要把审查总结评论回写到 MR：勾 `api`；
4. 点击 **Create personal access token**，立即复制生成的令牌（形如 `glpat-xxxx…`），只显示一次。

**方式 B：项目访问令牌（Project Access Token）**

适合不想绑定个人账号的场景：进入项目 → **Settings → Access Tokens**，按提示创建（Scopes 同上，角色选择以你的 GitLab 版本可选项为准，需能读取仓库与 MR）。创建后复制令牌。

## 第 2 步：在平台创建凭据

1. 进入「项目接入 → 访问凭据」，点击「新增」；
2. 字段填写：
   - 凭据名称：如 `GitLab-审查机器人`；
   - Git 平台：选择 `GitLab`；
   - Token：粘贴第 1 步生成的令牌；
   - 状态：正常；
3. 保存后点击该行的「检测」，提示成功说明令牌有效、能访问目标仓库。

## 第 3 步：接入项目并配置 Webhook

**平台侧**：进入「项目接入 → 代码项目」新增项目（Git 平台选 GitLab，填仓库地址如 `https://gitlab.example.com/your-group/your-project`，选择凭据，启用合并请求审查并选目标分支与审查方式）。保存后再次编辑该项目，切换到 **Webhook** 页签：

1. 回调地址：复制输入框中的值，形如 `https://acr.example.com/webhook/gitlab`；
2. Webhook Secret：点击「随机生成」生成一个并**另行记录**（保存后页面不回显明文），然后保存项目。

**GitLab 侧**：进入项目 **Settings → Webhooks → Add new webhook**：

1. URL：粘贴平台回调地址；
2. Secret token：填入平台的 Webhook Secret（与平台保持完全一致）；
3. Trigger：勾选 **Merge request events**；
4. SSL verification 保持默认启用（回调地址为 HTTPS 时）；
5. 点击 **Add webhook** 保存。

> GitLab 的 MR 动作（open / update / reopen）由平台映射为 opened / synchronize / reopened 后按动作白名单过滤，默认三者都触发审查；其他动作会被自动忽略。

## 第 4 步：验证连通

1. 在仓库新建分支提交改动，向目标分支发起 Merge Request；
2. GitLab 侧：项目 Settings → Webhooks，点击该 Webhook 查看 **Recent events**（最近投递），应看到 2xx 响应；
3. 平台侧：项目 Webhook 页签的「最近接收」显示「已受理合并请求 #N，生成审查任务 #N」；
4. 「审查中心 → 审查任务」出现新任务，说明链路打通。

## 常见错误

| 现象 | 原因与处理 |
|---|---|
| Recent events 显示 4xx/超时 | 回调地址不可达、或自建 GitLab 无法访问平台地址，见「常见问题 → Webhook 没触发怎么办」 |
| 平台事件记录显示「Webhook 签名校验失败」 | Secret token 与平台 Webhook Secret 不一致，两侧重新设置同一值 |
| 「未匹配到已接入的代码项目」 | 平台按项目完整路径（group/project，可含子组）精确匹配，检查项目仓库地址是否一致 |
| 凭据「检测」失败 | 令牌 scope 不足或过期：只读至少 `read_api`，回写评论需 `api` |
| 只有部分 MR 触发审查 | MR 目标分支不在项目「目标分支」列表，编辑项目补选 |
