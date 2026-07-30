# AI Code Review — 基于 AI 与规则引擎的智能代码审查平台

> **语言**：[English](README.en.md) | 简体中文
>
> ⚠️ **研发阶段声明**：本项目处于早期研发阶段，核心代码审查业务尚未落地，暂不可直接用于生产环境。当前仓库提供基础管理底座、业务系统归属、模型配置等基础能力，以及完整的产品规划与架构骨架，供协作与参考。欢迎关注进展，但请勿在生产中依赖。

## 项目定位

AI Code Review 是一个面向企业内部使用的**智能代码审查管理平台**，解决以下核心痛点：

1. **开发者不看 review**：审查结果推送到钉钉/企微/飞书群，主动触达开发者
2. **全英文看不懂**：原生中文支持，对接国产大模型（DeepSeek/通义千问等）
3. **没有管理后台**：可视化管理模型配置、项目、用户权限、审查规则
4. **没有通知和历史**：通知分级推送、审查结果持久化存储、问题跟踪（改了没改）
5. **全量代码理解不足**：集成 alibaba/open-code-review 引擎，支持 Agent 级全库探索

## 架构设计

```
┌──────────────────────────────────────────────────────────┐
│                   AI Code Review 平台                      │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  管理层（本项目）                                          │
│  ├── Web 管理后台（Vue3 + Element Plus）                   │
│  ├── 钉钉/企微/飞书 + 邮件 通知推送                            │
│  ├── Dashboard 数据看板 + 统计报表                          │
│  ├── 用户/角色/权限管理（RBAC）                             │
│  ├── 项目管理 + 审查规则配置                                │
│  ├── 审查记录存储（MySQL）+ 历史查询                         │
│  ├── 模型配置管理（API Key、供应商切换）                      │
│  └── 日报/周报/月报自动生成                                 │
│                                                          │
│  引擎层（外部集成）                                         │
│  ├── alibaba/open-code-review (OCR)                      │
│  │   ├── diff 增量审查（每次 MR/Push）                      │
│  │   ├── 全量文件扫描（定时巡检）                            │
│  │   ├── 29 种语言内置审查规则                               │
│  │   └── Agent 代码库探索（read_file + code_search）        │
│  └── 降级：内置 LLM 直调（DeepSeek/通义/OpenAI）            │
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
├── acr-review/          # 代码审核主业务边界（当前仅 Maven 模块说明）
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

当前仓库已具备业务系统归属管理，以及模型配置、启停、默认模型和连接测试的基础能力；它们只覆盖后续“项目接入”和“模型管理”的一部分。`acr-review` 当前只建立依赖边界，没有实现任何代码审核业务。实际完成度以产品路线图的“当前基线”为准。

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

### 规划中（代码审查业务）

以下优先级保留产品方向，具体拆分、依赖和验收以 `docs/planning/product-roadmap.md` 为准。

| 模块 | 功能 | 优先级 |
|------|------|:---:|
| 多平台项目接入 | 兼容 GitHub/GitLab/Gitee/Gitea，统一 Git Provider 抽象层（认证、API 适配），项目与业务系统/部门归属绑定 | P0 |
| Webhook 接收 | 多平台 webhook 事件接收（PR/MR/Push）、签名鉴权、事件去重、幂等 | P0 |
| 审查引擎对接 | 集成 alibaba/OCR，subprocess 调用、JSON 解析、超时/重试、降级内置 LLM 直调 | P0 |
| 结果回写 | 多平台 MR/PR inline 评论回写（GitHub/GitLab/Gitee/Gitea API 适配） | P0 |
| 审查任务编排 | webhook→拉 diff→调 OCR→落库→回写→通知 全链路编排 | P0 |
| 通知推送 | 钉钉/企微/飞书 Webhook + 邮件，多渠道策略、严重度分级触发、@提交者、聚合去重 | P0 |
| 审查记录 | 审查结果存储（task→result→comment 三级）、历史查询、详情查看 | P0 |
| 缺陷流转 | 缺陷指派/认领、状态流转（待修复/已修复/忽略/误报）、误报标记 | P1 |
| 模型管理 | 多供应商配置、API Key 加密存储、默认模型、超时/Token 上限、降级链路 | P1 |
| 审查规则 | 项目级规则配置、自定义 prompt、规则库共享、版本管理 | P1 |
| 数据看板 | 团队/项目/个人代码质量统计、趋势图、严重度/分类分布 | P1 |
| 成员分析 | 开发者提交行为分析、问题分布、个人质量画像 | P1 |
| 成本统计 | Token 消耗统计、按项目/模型维度成本、配额限流 | P2 |
| 日报/周报 | 自动生成代码质量报告、定时推送 | P2 |
| 全量扫描 | 定时全库扫描、项目克隆到本地仓库、按项目包路径区分存储、历史遗留问题发现 | P2 |
| Session 回放 | 审查会话可视化、多用户共享回放（OCR Session Viewer 改造） | P2 |
| 项目哨兵 | 主动监控 + 预警规则 | P2 |
| 合并卡点 | HIGH 问题自动阻止 MR/PR 合并（多平台状态检查 API） | P2 |
| 误报回流 | 误报反馈沉淀、规则/prompt 优化回流 | P3 |
| SARIF 对接 | JSON→SARIF 转换、对接 SonarQube/GitHub Code Scanning | P3 |

## 参考项目

| 项目 | 用途 | 说明 |
|------|------|------|
| [PR-Agent](https://github.com/The-PR-Agent/pr-agent) | 参考 | AI Code Review 引擎设计、prompt 模板、Git Provider 架构 |
| [AI-Codereview-Gitlab](https://github.com/sunmh207/AI-Codereview-Gitlab) | 参考 | 国内企业落地方案、通知推送、Dashboard、日报功能设计 |
| [alibaba/open-code-review](https://github.com/alibaba/open-code-review) | 集成 | 审查引擎、全量扫描、29 种语言规则、Agent 代码库探索 |

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

# 2. 修改数据库配置
# 编辑 acr-admin/src/main/resources/application-dev.yml

# 3. 编译运行
mvn clean install -DskipTests
cd acr-admin
mvn spring-boot:run
```

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
