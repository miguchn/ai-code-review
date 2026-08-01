# AI Code Review — 基于 AI 与规则引擎的智能代码审查平台

> **语言**：[English](README.en.md) | 简体中文
>
> ⚠️ **研发阶段声明**：本项目处于早期研发阶段，暂不可直接用于生产环境。当前已完成 GitHub 项目与加密 PAT 凭据接入的首个纵向切片，但 Webhook、审查任务、Diff、结果回写和通知等核心闭环尚未实现。欢迎关注进展，但请勿在生产中依赖。

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
│  ├── 工作台、审查任务、问题台账与项目接入                      │
│  ├── 审查方案、规则库、模型服务与质量门禁                      │
│  ├── 代码平台回写、通知预警、质量看板与报告                    │
│  ├── 组织/项目权限、凭据安全、业务审计与数据生命周期             │
│  └── 审查链路监控、失败恢复、容量与成本治理                    │
│                                                          │
│  引擎层（外部集成）                                         │
│  ├── alibaba/open-code-review (OCR)                      │
│  │   ├── diff 增量审查（每次 MR/Push）                      │
│  │   ├── 全量文件扫描（定时巡检）                            │
│  │   ├── 29 种语言内置审查规则                               │
│  │   └── Agent 代码库探索（read_file + code_search）        │
│  └── 可选内置 LLM 直调（需按版本验证后纳入降级策略）           │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

## 技术栈

| 层 | 技术 | 说明 |
|---|------|------|
| 前端 | Vue 3 + Element Plus + ECharts + Vite | 管理后台 + 数据看板 |
| 后端 | Java 17 + Spring Boot 4 + MyBatis + Druid | RESTful API |
| 数据库 | MySQL 8 | 审查记录、用户、配置 |
| 缓存 | Redis | 会话、缓存、限流 |
| 权限 | Spring Security + JWT + RBAC | 用户/角色/菜单权限 |
| 审查引擎 | alibaba/open-code-review (Go CLI) | 通过 subprocess 调用 |
| 通知 | 钉钉/企微/飞书 Webhook + 邮件 | 审查结果多渠道推送 |

## 项目结构

```
ai-code-review/
├── acr-common/          # 公共工具层（utils、基类、AI Client 抽象、XSS 过滤）
├── acr-system/          # 平台治理（RBAC、组织、字典、业务系统、模型配置）
├── acr-review/          # 代码审核主业务（项目、Git 凭据、Provider 与连接测试）
├── acr-framework/       # 框架胶水层（Security、JWT、Druid、Redis、AOP、限流）
├── acr-admin/           # 启动、配置与 Web 接入
├── acr-quartz/          # 通用定时任务管理与触发
├── acr-ui/              # 前端（Vue3 + Element Plus）
├── docs/planning/       # 产品路线图与最小架构骨架
├── skills/              # 单项业务规划技能
├── rules/               # 架构与交付约束
└── sql/                 # 数据库脚本
```

## 规划与开发入口

正式开发代码审查业务前，按以下顺序阅读：

1. [产品路线图](docs/planning/product-roadmap.md)：业务模块、依赖、阶段和验收标准
2. [架构与目录骨架](docs/planning/architecture-scaffold.md)：实际技术基线、模块归属和关键流程
3. [协作入口](AGENTS.md)：规划技能、开发规则和基础验证

当前仓库已具备业务系统归属管理，以及模型配置、启停、默认模型和连接测试的基础能力；P0/M1 已完成 GitHub 项目、加密 PAT 凭据、GitHub Provider、连接检测及对应权限和页面。一个业务系统可关联多个代码仓库项目；业务系统的代码和接口仍由 `acr-system` 提供，唯一菜单入口已移动到“代码审查”。实际完成度以产品路线图的“当前基线”为准。

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

### 已完成（P0/M1 第一个纵向切片）

| 模块 | 功能 |
|------|------|
| 业务系统归属 | 一个业务系统关联多个代码仓库项目，入口位于“代码审查”一级目录 |
| GitHub 凭据 | PAT 加密存储、响应不回显、编辑留空保留、引用删除保护、连接检测 |
| GitHub 项目 | 仓库信息自动读取、全部分支分页同步、业务系统/部门/负责人/凭据绑定、启停、连接检测 |
| PR 审查范围 | 默认启用 PR 审查、从真实分支中多选目标分支、优先推荐 `dev`/`develop`、平台统一事件默认值 |
| Git Provider | 统一地址解析、凭据校验、仓库授权和仓库信息读取契约；当前只实现 GitHub Provider |
| GitHub Provider | GitHub API 访问、仓库/默认分支/全部分支获取及地址/凭据/权限/仓库/网络/超时失败分类 |

### 规划中（代码审查业务）

路线图不再按“同时做全平台、全渠道”排列，而按真实纵向闭环分期。P0 同时包含安全、权限、审计和运行保障，不把它们留到功能上线之后。

| 版本 | 产品目标 | 核心范围 |
|------|------|------|
| MVP（V0.1 内部试点） | 一个平台完成最小可信闭环 | 单 Git 平台 MR/PR、项目接入、Webhook、单审查路径、任务/问题、总结回写、单通知渠道、权限审计、失败恢复和基础工作台 |
| 核心版（V0.2 受控推广） | 让结论可整改、策略可治理 | inline 评论、问题流转与复核、规则库与审查方案版本、模型策略、通知策略、影子门禁和角色化工作台 |
| 企业版（V1.0 内部正式使用） | 达到企业内部商用准入 | 统一身份/账号生命周期、质量看板、报告中心、通知预警、成本归集、数据生命周期、备份恢复、业务 SLI 和审计导出 |
| 规模化版（V1.1） | 扩平台并引入受控强治理 | 第二 Git Provider、强制门禁与旁路、预算限额、更多通知渠道、全量扫描和资源隔离 |
| 后续候选 | 经场景验证后独立立项 | SARIF、审批式误报优化、Session 回放、主动哨兵等 |

当前左侧一级菜单以“代码审查”为核心业务入口，依次包含业务系统管理、项目管理和访问凭据；系统管理、系统监控等治理与运维入口靠后。后续目标菜单随实际纵向切片逐步增加，不预建空入口。完整子菜单、指标口径、优先级和验收门槛见[产品路线图](docs/planning/product-roadmap.md)。

## 参考项目

| 项目 | 用途 | 说明 |
|------|------|------|
| [PR-Agent](https://github.com/The-PR-Agent/pr-agent) | 参考 | Provider 能力契约、自动/手动触发、增量审查、大变更处理和低噪声持久评论 |
| [AI-Codereview-Gitlab](https://github.com/sunmh207/AI-Codereview-Gitlab) | 参考 | 国内 IM 触达、Webhook→回写→日报/看板的轻量闭环，以及 Agent 沙箱与资源提示 |
| [alibaba/open-code-review](https://github.com/alibaba/open-code-review) | 引擎候选 | 确定性文件筛选/分组/规则匹配、Agent 上下文探索、结构化定位、恢复和遥测 |

参考项目只用于验证能力和取舍：PR 描述/问答/补文档、娱乐化审查风格、个人排行榜、一次铺满多平台、静默自动降级、Session Viewer 等不会直接进入 MVP。具体审计见[产品路线图](docs/planning/product-roadmap.md#23-本地参考项目能力审计)。

## 快速开始

### 环境要求

- Java 17+
- Node.js 18+
- MySQL 8.0+
- Redis 6.0+
- Maven 3.8+

### 后端启动

```bash
# 1. 初始化数据库
mysql -u root -p < sql/ry_20260417.sql
mysql -u root -p < sql/quartz.sql
mysql -u root -p < sql/sys_manage_20260512.sql
mysql -u root -p < sql/review_project_github_20260801.sql
mysql -u root -p < sql/review_project_pr_scope_20260801.sql

# 2. 为 GitHub PAT 配置稳定的 32 字节 Base64 主密钥
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
