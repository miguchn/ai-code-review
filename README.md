<div align="center">
  <h1>AI Code Review · V1.0</h1>
  <h4>自动检查代码，持续跟进问题。</h4>
  <p>企业级代码检查平台，开源并支持私有化部署。</p>
  <p>
    <a href="LICENSE"><img src="https://img.shields.io/github/license/miguchn/ai-code-review" alt="Apache License 2.0" /></a>
    <img src="https://img.shields.io/badge/version-1.0.0-blue.svg" alt="Version 1.0.0" />
    <img src="https://img.shields.io/badge/Java-17-orange.svg" alt="Java 17" />
    <img src="https://img.shields.io/badge/Spring_Boot-4.x-6DB33F.svg" alt="Spring Boot 4" />
    <img src="https://img.shields.io/badge/Vue-3.x-42b883.svg" alt="Vue 3" />
  </p>
  <p><a href="README.en.md">English</a> | 简体中文</p>
</div>

**V1.0 企业级正式版，适用于企业内部部署使用。** 提供项目权限、凭据加密、任务恢复和问题跟进，支持团队持续开展代码检查与质量管理。

AI Code Review 接收代码平台的 PR/MR 和 Push 事件，按团队设置调用模型检查代码，将结果写回代码平台并发送群通知。发现的问题可以指派、跟进、复核，直到结束处理；管理者可在授权范围内查看项目质量和模型用量。

[查看真实演示 PR](https://github.com/miguchn/acr-demo/pull/1#issuecomment-5201536018)：示例代码包含 SQL 注入、硬编码凭据等问题，评论由平台实际运行后生成并更新。

## 能做什么

| 能力 | 使用方式 |
| --- | --- |
| 连接代码仓库 | 支持 GitHub、GitLab、Gitee、Gitea，配置项目、分支及检查范围 |
| 自动检查 | PR/MR 更新或指定分支 Push 后触发，支持模型直审和 open-code-review 工具检查 |
| 查看结果 | 输出评分、重点问题、代码位置及修复建议，保留每次运行的配置和结果 |
| 回写评论 | 同一 PR/MR 更新一条总结评论；可开启重点问题的行内评论 |
| 群消息通知 | 支持钉钉、企业微信、飞书群机器人，可设置通知条件、间隔和失败补发 |
| 跟进问题 | 自动指派、逾期提醒、确认、修复、复核、关闭、忽略和误报处理 |
| 管理权限 | 按功能、部门和项目成员角色控制访问，凭据加密保存 |
| 查看质量 | 查看项目和成员统计、质量趋势、Token 用量及按当前单价估算的费用 |
| 运行维护 | 查看任务积压、失败和依赖状态，支持持久调度、重试与故障恢复 |

GitHub 已有真实仓库闭环验收记录；GitLab 已由维护者测试可用；Gitee、Gitea 已完成适配和契约测试，部署时需在实际使用的实例中验证。Push 检查用于提交后的问题发现，风险结论不会自动阻止代码合并。模型费用是估算值，成员统计用于工程改进。

代码上下文会发送到你配置的模型服务。接入企业仓库前，请确认模型、网络和数据处理范围符合团队要求。

## 界面预览

**检查结果写回 PR**

<p align="center"><img src="docs/images/readme-pr-comment.png" alt="PR 总结评论" width="720" /></p>

**问题跟进**

<p align="center"><img src="docs/images/readme-issue-ledger.png" alt="问题列表与处理状态" width="720" /></p>

**工作台**

<p align="center"><img src="docs/images/readme-workbench.png" alt="工作台待办与任务状态" width="720" /></p>

## 快速开始

准备 Docker Engine、Compose v2，并确保宿主机 80 端口可用。

```bash
cp .env.example .env
openssl rand -base64 32
# 将生成的值填入 .env 的 ACR_CREDENTIAL_MASTER_KEY
docker compose up -d --build
docker compose ps
```

四个服务均为 `healthy` 后，打开 <http://127.0.0.1>，使用 `admin / admin123` 登录并修改密码。默认 Compose 提供本机试用配置；团队部署请按[部署说明](docs/deployment.md)设置 HTTPS、独立密钥、数据库权限和备份，并完成[正式使用验收](docs/planning/v1-release-readiness.md)。

### 连接第一个仓库

1. 在「策略配置 → 大模型配置」添加模型，测试调用是否正常。
2. 在「项目接入 → 访问凭据」添加代码平台 Token；创建代码项目，选择分支、模板及检查方式。
3. 测试项目连接后启用，将项目生成的 Webhook 地址和 Secret 配置到代码平台。
4. 创建或更新 PR/MR，在检查任务、代码平台评论和问题列表中查看结果。
5. 需要群通知时，添加机器人渠道并绑定到项目；需要 Push 检查时，配置触发分支。

上述菜单路径对应当前界面；后台「功能助手」提供逐步指引。界面和内置帮助以中文为主。

Git 平台回调地址由 `ACR_WEBHOOK_CALLBACK_URL` 设置，必须能从代码平台访问。通知中的详情链接由 `review.ui.base-url` 设置，必须能从团队网络访问。默认后端镜像已包含 open-code-review CLI `1.11.6`；本地 Java 运行方式需按部署说明安装该工具。

新装时，MySQL 数据卷首次初始化会执行 `init-full.sql`，已包含 `01`–`50` 增量脚本的最终状态。已有数据卷不会重复初始化。

存量数据库升级前先备份，按 [SQL 说明](sql/README.md)执行尚未应用的增量脚本。`init-full.sql` 仅用于新装，会重建表结构。

## 文档与开发

后端采用 Java 17、Spring Boot 4、MyBatis、MySQL 和 Redis，前端采用 Vue 3。主业务位于 `acr-review`，模块关系见[架构说明](docs/planning/architecture-scaffold.md)。

| 文档 | 内容 |
| --- | --- |
| [部署说明](docs/deployment.md) | 安装、连接配置、升级与生产使用条件 |
| [正式使用验收](docs/planning/v1-release-readiness.md) | 已核验能力、待交付功能及部署验收要求 |
| [SQL 说明](sql/README.md) | 新装初始化和存量升级 |
| [更新记录](CHANGELOG.md) | 功能与修复记录 |
| [产品路线图](docs/planning/product-roadmap.md) | 后续规划及验收标准 |
| [贡献指南](CONTRIBUTING.md) / [安全政策](SECURITY.md) | 参与开发与报告安全问题 |

提交前运行 `mvn test`、`cd acr-ui && npm run test:unit` 和 `npm run build:prod`。新增业务需明确范围、权限和验收方式。

本项目基于 [Apache License 2.0](LICENSE) 开源，第三方组件遵循各自协议。
