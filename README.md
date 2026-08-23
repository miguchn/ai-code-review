<div align="center">
  <h1>AI Code Review — 企业内部 AI 代码审查治理平台</h1>
</div>

<div align="center">
  <h4>把 MR/PR 与 Push 事件转化为可追踪的 AI 审查、低噪声交付、问题整改和质量度量闭环。</h4>
</div>

> **先看真实结果（30 秒）**：[这个公开 PR](https://github.com/miguchn/acr-demo/pull/1#issuecomment-5201536018) 故意保留了 SQL 注入、硬编码凭据等典型问题，其中的 ACR 总结评论由本平台真实运行后自动回写并原地更新，不是宣传截图。

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/github/license/miguchn/ai-code-review" alt="License" /></a>
  <img src="https://img.shields.io/badge/status-V0.2_controlled_rollout-orange.svg" alt="V0.2 controlled rollout" />
  <img src="https://img.shields.io/badge/Java-17-orange.svg" alt="Java 17" />
  <img src="https://img.shields.io/badge/Spring_Boot-4.x-6DB33F.svg" alt="Spring Boot 4" />
  <img src="https://img.shields.io/badge/Vue-3.x-42b883.svg" alt="Vue 3" />
</p>
<p align="center">
  <img src="https://img.shields.io/badge/GitHub-E2E_verified-181717.svg" alt="GitHub E2E verified" />
  <img src="https://img.shields.io/badge/GitLab-adapter_tested-FC6D26.svg" alt="GitLab adapter tested" />
  <img src="https://img.shields.io/badge/Gitee-adapter_tested-C71D23.svg" alt="Gitee adapter tested" />
  <img src="https://img.shields.io/badge/Gitea-adapter_tested-609926.svg" alt="Gitea adapter tested" />
</p>
<p align="center">
  <a href="README.en.md">English</a> | 简体中文
</p>

<p align="center">
  <a href="#项目定位">项目定位</a> ·
  <a href="#已落地的核心能力">核心能力</a> ·
  <a href="#闭环与架构">闭环与架构</a> ·
  <a href="#快速开始">快速开始</a> ·
  <a href="#当前阶段与路线图">路线图</a>
</p>

## 项目定位

AI Code Review 面向企业内部研发团队，部署在代码托管平台旁侧。Git 平台继续负责代码协作与合并，本平台负责把代码事件连接成一条可治理的业务链：**可信接入 → 审查执行 → 结论交付 → 问题整改 → 复核关闭 → 质量度量**。

它不以“生成更多评论”为目标，而是解决评论无人跟进、重复审查刷屏、风险无法归责、结果无法复核、管理者看不到质量趋势等平台化问题。

| 关注点 | 常见的单次 AI Review | AI Code Review |
|---|---|---|
| 结论交付 | 留下评论后结束，复审容易刷屏 | 同一 MR/PR 维护一条总结评论，复审原地更新；投递可独立重试 |
| 问题跟进 | 依赖开发者自行记忆和处理 | 问题台账持续跟踪确认、修复、待复核、关闭、忽略和误报 |
| 责任与触达 | 结果散落在代码平台 | 自动指派、逾期标记，钉钉 / 企业微信 / 飞书群机器人分级触达 |
| 企业治理 | 以仓库或工具配置为主 | 项目权限、凭据治理、运行恢复、业务留痕和质量洞察统一管理 |
| 部署边界 | 常依赖外部托管服务 | 开源私有化部署；凭据加密，模型端点受控 |

当前版本定位为 **V0.2 核心版受控推广**。GitHub 已完成真实仓库全链路验收；GitLab、Gitee、Gitea 已完成适配器与契约测试，但仍需在目标企业的真实实例中验收。当前状态不等同于 V1.0 企业正式生产就绪。

## 已落地的核心能力

| 能力域 | 当前已实现的能力与边界 |
|---|---|
| 🧩 多平台接入与触发 | GitHub / GitLab / Gitee / Gitea 统一适配契约；支持 MR/PR 事件与配置分支 Push 审查。Push 路径用于合并后发现与治理，不是合并前强制门禁 |
| 🔐 可信 Webhook | 分平台验签、载荷限制、按仓库路径和目标分支匹配、Delivery 幂等去重；验签通过后才占用去重键，伪造或错位事件直接拒绝 |
| ⚙️ 双执行模式与范围治理 | `LLM_DIRECT` 大模型直审或可选 `OCR_ENGINE`；Diff 纳入/排除、测试文件、存量问题和高影响文件扩展策略随任务冻结。默认 Docker 镜像不内置 OCR CLI |
| 🧠 结构化审查结果 | 五维评分、后端重算总分、结构化问题清单、Top 3 重点问题与 NEW/EXISTING 归属；模型、模板、Prompt、范围和执行结果按运行留快照 |
| 💬 低噪声结果交付 | MR/PR 总结评论幂等新增或更新；严重/高危问题可按项目开关发布行内评论；审查结论、总结评论、行内评论和 IM 投递状态相互独立 |
| 📣 国内 IM 通知 | 钉钉 / 企业微信 / 飞书群机器人；项目级结论策略、低优先级冷却、失败通知、正文快照、失败补发和投递追踪。当前一个项目绑定一个渠道 |
| 📒 问题治理闭环 | 全量新增问题入账，支持确认、关闭、忽略、误报、待复核、重开、批量处置和生命周期时间线；按提交作者/PR 发起人/项目负责人自动指派，并提供逾期扫描与聚合提醒 |
| 👥 权限与敏感资产 | 有效访问取 **功能 RBAC × 部门 DataScope × 项目成员角色** 的交集；项目角色包含 OWNER / ADMIN / REVIEWER / VIEWER；Git Token、Webhook Secret、通知地址和模型 API Key 使用 AES-GCM 加密且不明文回显 |
| 🛡️ 可靠性与运行保障 | 数据库驱动的任务与投递调度、租约/epoch 围栏、失败退避与恢复、有界执行池、项目并发和 Git/工作区/OCR/LLM 资源预算；运行概览展示积压、失败、资源和依赖告警 |
| 📊 数据洞察 | 质量总览、项目明细、成员分析、提交趋势、增删行数、身份关联、指标口径说明；Token 用量按运行采集并基于当前模型单价估算成本，尚不包含价格版本、预算和配额控制 |
| 🧭 产品内指引 | 全局功能助手内置快速上手、四平台接入、模型与引擎、提交注释、通知投递和常见问题等 14 篇指引，关键配置页可直接深链 |

## 闭环与架构

<p align="center">
  <img src="docs/images/readme-loop-diagram.png" alt="AI Code Review 治理闭环示意图" width="760" />
</p>

代码平台仍是合并事实源；ACR 负责治理闭环：MR/PR 或 Push 事件 → 可信 Webhook → 审查执行 → 总结/行内评论与 IM 交付 → 问题台账 → 整改推送触发复核 → 洞察与运营。

项目采用单体、多 Maven 模块架构，核心边界保持清晰：

```text
GitHub / GitLab / Gitee / Gitea
              │ Webhook / API
              ▼
acr-ui ──HTTP──> acr-admin                 # 启动、REST/Webhook、鉴权与协议校验
                  ├──> acr-review          # 审查主业务、编排、Git/引擎/通知适配
                  │      ├──> MySQL         # 事件、任务、问题、投递与洞察事实
                  │      └──> Redis         # 缓存、限流与短期幂等辅助
                  ├──> acr-system          # 用户、角色、部门、业务系统、模型配置
                  └──> acr-quartz          # 通用定时任务触发
```

当前沿用 Java 17、Spring Boot 4、MyBatis、MySQL、Redis、Quartz 和 Vue 3；未引入 MQ、工作流引擎或向量数据库。业务主链集中在 `acr-review`，外部依赖失败不会改写已经形成的内部审查事实。

## 适用场景

| 场景 | 平台带来的价值 |
|---|---|
| 代码资产不能交给外部 SaaS | 在企业环境私有化部署，集中管理 Git 与模型凭据和代码外发边界 |
| 多团队、多仓库需要统一审查口径 | 按业务系统、项目、分支、范围、模型和模板配置，并保留每次执行快照 |
| 团队同时存在 MR/PR 与直接推送流程 | MR/PR 内回写结论；配置分支 Push 进入事后审查、通知和问题台账 |
| 质量/安全团队需要持续运营问题 | 从一次评论升级为可指派、可复核、可统计的治理台账 |
| 研发管理者需要观察趋势和成本 | 在授权范围内查看覆盖、结论、整改、成员与 Token 用量，不以个人排名替代工程判断 |

本项目不替代 Git 平台、人工审批、IDE 或 Jira/禅道等项目管理系统，也尚未提供强制合并门禁。

## 真实运行截图

以下截图来自真实运行环境；IM 通知卡片为使用真实运行数据生成的样式样例。公开可核验结果见[演示 PR](https://github.com/miguchn/acr-demo/pull/1#issuecomment-5201536018)。

**PR 总结评论回写**（高风险结论、Top 3 重点问题、全量问题数与范围统计）：

<p align="center"><img src="docs/images/readme-pr-comment.png" alt="PR 总结评论回写" width="720" /></p>

**IM 分级通知**（企微卡片样式样例；钉钉 / 企微 / 飞书三渠道）：

<p align="center"><img src="docs/images/readme-im-notify.png" alt="IM 分级通知卡片样例" width="560" /></p>

**问题台账**（严重度、阶段、责任归属、轮次轨迹与快捷筛选）：

<p align="center"><img src="docs/images/readme-issue-ledger.png" alt="问题台账" width="720" /></p>

**审查记录详情**（五维评分、模型评语、总结评论投递状态与外部评论 ID）：

<p align="center"><img src="docs/images/readme-record-detail.png" alt="审查记录详情" width="720" /></p>

**工作台**（今日待办、项目风险趋势和任务状态）：

<p align="center"><img src="docs/images/readme-workbench.png" alt="工作台" width="720" /></p>

## 快速开始

### Docker Compose 一键试用

要求：Docker Engine、Compose v2，且宿主机 80 端口空闲。首次构建需要下载后端与前端依赖，耗时取决于网络环境。

```bash
cp .env.example .env
openssl rand -base64 32
# 将上一步输出写入 .env 的 ACR_CREDENTIAL_MASTER_KEY
docker compose up -d --build
docker compose ps
```

四个服务均为 `healthy` 后打开 <http://127.0.0.1>，使用默认管理员 `admin / admin123` 登录，并立即修改默认密码。这是试用环境，不是生产加固方案；正式部署、升级与回滚说明见[部署文档](docs/deployment.md)。

如需让公网 Git 平台回调本机，须先准备其可访问的 HTTPS 地址，并在 `.env` 设置 `ACR_WEBHOOK_CALLBACK_URL`。团队需要访问 IM 消息中的详情链接时，还应在参数管理中把 `review.ui.base-url` 配置为团队可访问的前端地址。

### 接入第一个仓库（GitHub 示例）

1. 在「策略配置 → 大模型配置」新增模型并完成调用检测；或按下文准备可选 OCR 引擎；
2. 在「项目接入 → 访问凭据」新增 GitHub Token，再创建代码项目并选择目标分支、审查方式、模板和范围策略；
3. 项目连接检测通过后启用，复制平台生成的 Webhook 地址与 Secret；
4. 在 GitHub 仓库配置 Webhook，订阅 Pull Request；需要 Push 审查时，再在项目中开启并配置触发分支；
5. 创建/更新 PR 或推送配置分支，完成后在审查任务、PR 总结评论、投递记录和问题台账查看结果。

平台内「功能助手」提供同一流程的逐步说明及四个平台的 Token/Webhook 配置指引。当前 Web 界面和内置指引以中文为主，尚未实现完整国际化。

### 复用宿主机 MySQL（可选）

Compose 默认使用内置 MySQL。需要复用宿主机已有测试库时，可在 `.env` 设置 `ACR_MYSQL_HOST=host.docker.internal` 以及端口、库名、用户名和密码。存量库必须按 [SQL 说明](sql/README.md)执行尚未应用的增量脚本，**不得**对已有业务数据执行 `init-full.sql`。

### OCR 引擎（可选）

默认 Docker 后端镜像不安装 open-code-review CLI，不影响使用 `LLM_DIRECT`。需要启用 `OCR_ENGINE` 时，请在自定义运行环境中执行 `npm install -g @alibaba-group/open-code-review`，或向容器挂载已安装的可执行文件，并设置容器内命令/绝对路径，例如 `ACR_OCR_EXECUTABLE=ocr`。平台会在项目配置和运行概览展示探测状态；仅当存在启用中的 OCR 项目时，不可用状态才产生运行告警。

## 产品边界与选型建议

- 如果只需要在单个仓库本地或 CI 中生成审查结果，直接使用 open-code-review 等 CLI/引擎通常更轻量；
- 如果希望由厂商托管并获得更广泛的商业集成、SLA 或企业服务，商业 SaaS 可能更合适；
- 如果核心诉求是**开源私有化 + 国内 IM + 项目级权限 + 问题整改闭环 + 运行与质量洞察**，本项目提供的是这些能力的一体化治理平台；
- open-code-review 是本平台的可选执行引擎之一。ACR 在引擎之外负责可信事件、配置快照、投递补偿、问题治理、权限和运营数据。

## 当前阶段与路线图

| 阶段 | 当前状态 |
|---|---|
| V0.1 MVP | 已交付项目接入、可信 Webhook、双执行模式、总结回写、IM 通知、基础问题闭环和工作台；GitHub 真实仓库全链路验收通过 |
| V0.2 核心版 | 当前受控推广；已交付完整问题生命周期与自动指派、Push 审查、行内评论、数据洞察与 Token 用量、项目级权限、持久调度/恢复、资源预算和运行概览 |
| V1.0 企业正式准入 | 尚未完成：统一身份或完整账号生命周期、敏感对象完整业务审计与导出、数据留存与备份恢复演练、不可变报告/订阅、价格版本与配额，以及计划投产 Provider 的真实实例验收 |
| 后续候选 | 质量门禁影子评估、洞察证据下钻与表格导出、模板/模型发布回滚治理；均需单独立项和验收，不作为当前能力承诺 |

完整业务边界、依赖和验收口径见[产品路线图](docs/planning/product-roadmap.md)；生产准入真值表见[正式上线前完整性治理](docs/planning/production-readiness-governance.md)。

## 文档导航

| 文档 | 说明 |
|---|---|
| [部署说明](docs/deployment.md) | Docker Compose 试用、手动部署、生产配置与升级路径 |
| [产品路线图](docs/planning/product-roadmap.md) | 产品定位、非目标、能力真值与阶段验收 |
| [架构说明](docs/planning/architecture-scaffold.md) | 模块归属、依赖方向和主流程边界 |
| [SQL 脚本说明](sql/README.md) | 新装初始化、存量升级和脚本清单 |
| [CHANGELOG](CHANGELOG.md) | 已交付纵向切片与验证记录 |
| [贡献指南](CONTRIBUTING.md) / [安全政策](SECURITY.md) | 如何参与项目与报告安全问题 |

参考项目：[PR-Agent](https://github.com/qodo-ai/pr-agent)（Provider 与 PR 助手能力）、[AI-Codereview-Gitlab](https://github.com/sunmh207/AI-Codereview-Gitlab)（国内 IM 触达）、[open-code-review](https://github.com/alibaba/open-code-review)（可选审查引擎）。参考项目用于能力验证与技术取舍，不代表功能对等。

## 参与贡献

- 使用 Conventional Commits（`feat:`、`fix:`、`refactor:`、`docs:`），功能分支命名为 `feature/<name>`；
- 代码提交前通过 `mvn test` 与 `cd acr-ui && npm run build:prod`；
- 业务切片开发前先明确范围、非范围与可执行验收标准。

## 开源协议

本项目基于 [Apache License 2.0](LICENSE) 开源；第三方组件按各自原始协议授权。
