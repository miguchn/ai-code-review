# AI Code Review — 企业级代码审查治理平台

> **语言**：[English](README.en.md) | 简体中文
>
> ⚠️ **研发阶段声明**：本项目 MVP（V0.1）功能面已收口：GitHub、GitLab、Gitee、Gitea 四平台均已实现「项目与凭据配置 → Webhook 接收 → 审查执行 → 总结评论回写 → IM 通知 → 问题处置 → 工作台」主链路；自动化测试已通过，**GitHub 真实环境全链路验收通过（2026-08-04）**：真实测试仓库 PR 驱动「Webhook 验签 → 审查执行 → 总结评论回写 → 企微通知 → 问题台账处置 → 整改复审」完整闭环；GitLab/Gitee/Gitea 以契约测试覆盖，待有实例时真实闭环，「连续运行两周」标准仍在累积。整体仍处内部试点阶段，请勿在生产中依赖。

## 项目定位

AI Code Review 是一个面向企业内部研发团队的**代码审查治理平台**。它部署在 GitLab、GitHub、Gitee、Gitea 等代码托管平台旁侧，把代码变更转化为可追踪的审查、整改、复核、度量和审计闭环；代码平台仍是代码协作与合并事实源。

产品主要解决：

1. **审查覆盖不可控**：以可信 Webhook、幂等任务和失败恢复保证应审变更被处理；
2. **结果难触达、难行动**：将结论回写代码平台并分级通知，问题可以确认、修复、申诉和复核；
3. **规则和模型不可治理**：按项目发布有版本的审查方案，模型密钥、降级、成本和变更可追踪；
4. **质量不可度量**：统一覆盖率、时延、有效问题、关闭和误报口径，支持看板与周期报告；
5. **企业安全与审计不足**：按组织、业务系统和项目隔离数据，记录关键配置、权限、决策和外部交付。

本项目不替代 Git 平台、通用项目管理、缺陷工单、BI、APM 或人工审批，也不以个人问题数或提交数做绩效排名。详细边界、目标菜单和版本准入见[产品路线图](docs/planning/product-roadmap.md)。

## 架构设计

```
┌──────────────────────────────────────────────────────────┐
│                   AI Code Review 平台                      │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  产品与治理层（本项目）                                      │
│  ├── 工作台、审查任务、审查记录、问题台账与项目接入              │
│  ├── 审查方案、规则库、模型服务与质量门禁                      │
│  ├── 代码平台回写、通知预警、质量看板与报告                    │
│  ├── 组织/项目权限、凭据安全、业务审计与数据生命周期             │
│  └── 审查链路监控、失败恢复、容量与成本治理                    │
│                                                          │
│  引擎层（外部集成）                                         │
│  ├── 大模型直审（平台模型服务 + 审查模板，LLM_DIRECT）        │
│  └── alibaba/open-code-review (OCR，OCR_ENGINE)            │
│      ├── diff 增量审查（每次 MR/Push）                      │
│      ├── 全量文件扫描（定时巡检，规划中）                     │
│      ├── 29 种语言内置审查规则                               │
│      └── Agent 代码库探索（read_file + code_search）        │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

## 技术栈

| 层 | 技术 | 说明 |
|---|------|------|
| 前端 | Vue 3 + Element Plus + ECharts + Vite | 管理后台 |
| 后端 | Java 17 + Spring Boot 4 + MyBatis + Druid | RESTful API |
| 数据库 | MySQL 8 | 审查记录、用户、配置 |
| 缓存 | Redis | 会话、缓存、限流 |
| 权限 | Spring Security + JWT + RBAC | 用户/角色/菜单权限 |
| 审查引擎 | alibaba/open-code-review (Go CLI) | 通过 subprocess 调用 |
| 通知 | 钉钉/企微/飞书群机器人 Webhook | 审查结论摘要推送 |

## 项目结构

```
ai-code-review/
├── acr-common/          # 公共工具层（utils、基类、AI Client 抽象、XSS 过滤）
├── acr-system/          # 平台治理（RBAC、组织、字典、业务系统、模型配置）
├── acr-review/          # 代码审核主业务（项目、Git 凭据、GitAdapterRegistry、四平台 Provider 适配）
├── acr-framework/       # 框架胶水层（Security、JWT、Druid、Redis、AOP、限流）
├── acr-admin/           # 启动、配置与 Web 接入
├── acr-quartz/          # 通用定时任务管理与触发
├── acr-ui/              # 前端（Vue3 + Element Plus）
├── docs/planning/       # 产品路线图、架构骨架与各切片设计文档
├── skills/              # 单项业务规划技能
├── rules/               # 架构与交付约束
└── sql/                 # 数据库脚本
```

## 规划与开发入口

正式开发代码审查业务前，按以下顺序阅读：

1. [产品路线图](docs/planning/product-roadmap.md)：业务模块、依赖、阶段和验收标准
2. [架构与目录骨架](docs/planning/architecture-scaffold.md)：实际技术基线、模块归属和关键流程
3. [协作入口](AGENTS.md)：规划技能、开发规则和基础验证

当前仓库已落地 M1–M7 纵向切片与四平台 Git Provider 主链路：Git 接入支持 **GitHub / GitLab / Gitee / Gitea**（`GitAccessContext` + `GitAdapterRegistry`，凭据 `server_url`、项目 `repository_full_path`、Webhook `/webhook/{provider}`）。实际完成度以[产品路线图](docs/planning/product-roadmap.md)的「当前基线」为准。

## 功能模块

### 已有（基础管理能力）

| 模块 | 功能 |
|------|------|
| 用户管理 | 用户 CRUD、密码策略、在线用户监控 |
| 角色管理 | RBAC 角色、数据权限、菜单权限 |
| 部门管理 | 组织架构树 |
| 菜单管理 | 动态菜单、按钮权限 |
| 字典管理 | 系统字典、业务字典 |
| 参数配置 | 系统参数动态配置 |
| 通知公告 | 站内通知、公告管理 |
| 操作日志 | 操作审计、登录日志 |
| 定时任务 | 任务 CRUD、执行日志 |
| 系统监控 | CPU/内存/JVM、Redis、Druid 监控 |

### 已完成（MVP 纵向切片 M1–M7 + 四平台 Git Provider）

| 模块 | 功能 |
|------|------|
| 业务系统归属 | 一个业务系统关联多个代码仓库项目，入口位于「项目接入」一级目录 |
| Git 访问凭据 | 四平台 PAT 加密存储、响应不回显、编辑留空保留、引用删除保护、连接检测；GitLab/Gitea 自建实例 `server_url` |
| 代码项目 | 仓库信息自动读取、全部分支分页同步、业务系统/部门/负责人/凭据绑定、启停、连接检测 |
| PR 审查范围 | 默认启用 PR 审查、从真实分支中多选目标分支、优先推荐 `dev`/`develop`、平台统一事件默认值 |
| Git Provider | 统一 `GitAccessContext` / `GitAdapterRegistry` 契约；已实现 GitHub、GitLab、Gitee、Gitea 四平台适配（连接检测、Diff/元数据/工作区/总结评论） |
| 多平台 Webhook | `POST /webhook/{github,gitlab,gitee,gitea}` 验签与去重；按 `repository_full_path` 匹配项目；GitHub 路径与行为保持兼容 |
| PR Webhook 接入（M2） | HMAC-SHA256 验签、项目匹配、动作白名单与目标分支判断、Delivery 幂等去重、事件全过程落库 |
| 审查任务（M2/M3/M3.1） | 事件建单后异步执行；任务页作执行队列；失败分类与安全重试；独立详情页展示排障信息 |
| 审查记录（M3.1） | 已完成审查历史列表与独立详情；评分/摘要/Top3 与执行记录分 Tab；复用任务表不另建结果表 |
| 审查执行（M3） | 审查方式二选一：大模型审查（模型服务+审查模板）或审查引擎（open-code-review）；建单冻结快照 |
| 审查范围策略（M3.2） | 以本次 Diff 变更行为审查核心，高影响变更（新增文件/公共签名/权限安全/配置/依赖/数据库脚本）自动扩展；问题区分「本次变更新增/历史存量」，存量默认不进 Top 3、不影响评分与结论；项目级排除路径/测试文件/存量上报/扩展开关随任务快照冻结，范围决策快照落库可查 |
| 总结评论回写（M4） | 审查成功后向 MR/PR 幂等回写/更新一条总结评论（结论、总分、Top3 归属、范围统计）；`review_delivery_record` 独立记录投递状态，失败可单独重试 |
| IM 通知（M5） | 钉钉/企微/飞书群机器人投递审查结论摘要或失败简讯；平台级通知渠道管理（加密存储、测试发送、启停）+ 项目单渠道绑定；投递记录筛选与补发 |
| 问题台账（M6/M6.1） | 审查 SUCCESS 后 Top 3 物化为问题（PR 级指纹去重）；确认/关闭/忽略/误报闭环与动作流水；处置后重渲染总结评论；投递触发来源追溯 |
| 工作台（M7） | 登录后行动工作台：范围健康、权限驱动待办卡、今日摘要、最近动态；四个列表支持 query 回填 |
| 审查模板（M3） | 策略配置下模板管理；内置 Java/Python/Go/Vue/React/全栈；模板正文仅技术栈重点；修改不影响历史任务 |
| 统一评分协议 | 大模型路径五维评分（40/30/20/5/5）+ Top 3 重点问题 + JSON 协议 v1.0/v1.1（v1.1 增加问题归属 `origin` 与范围统计）；后端校验重算总分；解析失败单独标记 |
| 项目 Webhook 配置（M2） | 回调地址展示与复制、Secret 加密保存不回显、最近接收时间与结果 |

### 版本规划

版本按真实纵向闭环分期推进，安全、权限、审计与运行保障随每个切片同步建设。各阶段验收标准见[产品路线图](docs/planning/product-roadmap.md)。

| 版本 | 产品目标 | 核心范围 |
|------|------|------|
| MVP（V0.1 内部试点） | 最小可信闭环；**功能面已收口（2026-08-04），GitHub 真实环境全链路验收通过**，余三平台真实环境与连续运行标准待完成 | 四平台 MR/PR、项目接入、Webhook、双审查路径、任务/问题、总结回写、IM 三渠道通知、权限审计、失败恢复和基础工作台 |
| 核心版（V0.2 受控推广） | 让结论可整改、策略可治理 | inline 评论、问题流转与复核、规则库与审查方案版本、模型策略、通知策略、影子门禁和角色化工作台 |
| 企业版（V1.0 内部正式使用） | 达到企业内部商用准入 | 统一身份/账号生命周期、质量看板、报告中心、通知预警、成本归集、数据生命周期、备份恢复、业务 SLI 和审计导出 |
| 规模化版（V1.1） | 引入受控强治理与平台生态增强 | 强制门禁与旁路、预算限额、更多通知渠道、全量扫描和资源隔离 |
| 后续候选 | 经场景验证后独立立项 | SARIF、审批式误报优化、Session 回放、主动哨兵等 |

当前左侧一级菜单顺序为：工作台 → 审查中心 → 项目接入 → 策略配置 → 通知管理 → 系统管理 → 系统监控。后续目标菜单随实际纵向切片逐步增加，不预建空入口。完整子菜单见[菜单 IA 设计](docs/superpowers/specs/2026-08-03-sidebar-menu-ia-design.md)，指标与验收见[产品路线图](docs/planning/product-roadmap.md)。

## 参考项目

| 项目 | 用途 | 说明 |
|------|------|------|
| [PR-Agent](https://github.com/The-PR-Agent/pr-agent) | 参考 | Provider 能力契约、自动/手动触发、增量审查、大变更处理和低噪声持久评论 |
| [AI-Codereview-Gitlab](https://github.com/sunmh207/AI-Codereview-Gitlab) | 参考 | 国内 IM 触达、Webhook→回写→日报/看板的轻量闭环，以及 Agent 沙箱与资源提示 |
| [alibaba/open-code-review](https://github.com/alibaba/open-code-review) | 引擎候选 | 确定性文件筛选/分组/规则匹配、Agent 上下文探索、结构化定位、恢复和遥测 |

参考项目只用于验证能力和取舍，不作为功能模板：PR 助手类能力（描述/问答/补文档）、娱乐化审查风格、个人排行榜、一次铺满多平台、静默自动降级等不进入产品。具体审计见[产品路线图](docs/planning/product-roadmap.md#23-本地参考项目能力审计)。

## 快速开始

### 环境要求

- Java 17+
- Node.js 18+
- MySQL 8.0+
- Redis 6.0+
- Maven 3.8+

### 后端启动

```bash
# 1. 初始化数据库（一次性脚本，含全部表结构与菜单/字典/参数/内置模板等初始数据）
#    存量环境升级请按 sql/README.md 执行增量脚本，勿用此方式
mysql --default-character-set=utf8mb4 -u root -p < sql/init-full.sql

# 2. 为 Git 平台凭据与 Webhook Secret 配置稳定的 32 字节 Base64 主密钥
export ACR_CREDENTIAL_MASTER_KEY="$(openssl rand -base64 32)"

# 3. 修改数据库配置
# 编辑 acr-admin/src/main/resources/application-dev.yml

# 4. 编译运行
mvn clean install -DskipTests
cd acr-admin
mvn spring-boot:run
```

`ACR_CREDENTIAL_MASTER_KEY` 必须由部署环境的密钥管理机制提供，并在重启、扩容和恢复时保持一致；更换主密钥前需要先设计并执行凭据轮换，仓库中不提供默认值。

### 前端启动

```bash
cd acr-ui
npm install
npm run dev
```

### OCR 引擎安装（可选）

```bash
npm install -g @alibaba-group/open-code-review
ocr config provider   # 配置模型供应商
ocr config model      # 选择模型
```

## 部署

Docker Compose 部署方案（待完善）：

```yaml
# 规划中的部署架构
services:
  mysql:       # MySQL 8.0
  redis:       # Redis 7
  backend:     # Spring Boot 应用
  frontend:    # Nginx + Vue3 静态资源
  ocr:         # alibaba/open-code-review CLI（集成在 backend 容器中）
```

## 开发规范

- Conventional Commits（`feat:`, `fix:`, `refactor:`, `docs:`）
- 功能分支：`feature/<name>`，修复分支：`fix/<issue>`
- 代码提交前必须通过编译和单元测试
- 每个代码审查业务切片开发前，先按 `skills/plan-review-feature/` 明确范围与验收
- 新业务遵守 `rules/architecture.md` 和 `rules/delivery.md`

## 开源协议

本项目基于 [MIT License](LICENSE) 开源：可自由使用、修改、分发和商用，保留版权声明与协议文本即可。

本项目依赖的第三方组件（Spring Boot、Vue、Element Plus、alibaba/open-code-review 等）按各自原始协议授权，不因本协议而改变。
