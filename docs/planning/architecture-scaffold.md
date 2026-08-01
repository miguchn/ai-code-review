# 项目架构与最小骨架

## 1. 本次结构结论

项目保留现有单体、多 Maven 模块形态，并新增一个统一的 `acr-review` 业务模块。新增模块只解决主业务归属问题，不代表拆分微服务，也不按仓库、任务、规则、报告等功能继续拆 Maven 模块。

判断依据：

- `acr-admin` 应只承担启动、配置和 HTTP/Webhook 接入，不能承载审查编排与数据访问；
- `acr-system` 已承担用户、权限、部门、字典、业务系统和模型配置等平台治理职责；
- 产品路线图中的项目接入、事件、任务、问题、策略、交付、报告和运行保障属于同一条代码审核业务链；
- 将这条业务链继续放入 `acr-system` 会混淆平台治理与核心业务，统一放入 `acr-review` 可形成清晰且无循环的依赖边界。

## 2. 技术与依赖基线

沿用 Java 17、Spring Boot 4、MyBatis、MySQL、Redis、Quartz、Vue 3 和现有安全体系，不引入新框架或基础设施。

```text
acr-ui ──HTTP──> acr-admin
                   ├──> acr-framework ──> acr-system ──> acr-common
                   ├──> acr-review ─────> acr-system ──> acr-common
                   └──> acr-quartz ─────> acr-common
```

当前不让 `acr-quartz` 依赖 `acr-review`。进入报告或全量扫描开发时，再根据真实调度入口评审最小依赖；Quartz 任务本身只负责触发，业务处理仍归 `acr-review`。

## 3. 模块职责

| 模块 | 当前职责 | 明确不承载 |
|---|---|---|
| `acr-admin` | Spring Boot 启动、运行配置、REST/Webhook Controller、协议校验、鉴权声明 | 审查业务规则、Mapper、流程编排、平台适配实现 |
| `acr-review` | 代码审核主业务的数据访问、用例服务、任务编排及 Git/引擎/通知适配 | 系统权限底座、通用框架配置 |
| `acr-system` | 用户、角色、部门、菜单、字典、参数、业务系统、模型服务基础配置等平台治理 | 代码项目、审查任务、问题、方案、规则和报告主业务 |
| `acr-framework` | Security、JWT、Redis、Druid、AOP、线程池等框架能力 | 任何代码审核业务规则 |
| `acr-common` | 稳定的跨模块类型和工具、现有 AI Client 基础 | 单一业务模块专用对象或流程 |
| `acr-quartz` | 通用定时任务管理与触发入口 | 定时业务的具体实现 |
| `acr-ui` | 管理端 Web 页面与 API 调用 | 后端业务判断 |

`acr-generator` 与产品运行和代码审核业务无关，且会通过 `acr-admin` 依赖暴露演示型运行时接口，因此不再保留。

## 4. 产品域与代码归属

产品菜单不是 Maven 模块，禁止按“审查中心、报告中心、通知预警”等一级菜单继续拆模块。稳定归属如下：

| 产品域 | 主要对象/能力 | 代码归属 | 边界说明 |
|---|---|---|---|
| 平台治理 | 用户、角色、部门、菜单、字典、参数、业务系统、模型服务基础配置 | `acr-system` | 业务系统继续作为通用治理数据和接口；因当前实际消费者是代码仓库项目，其唯一菜单入口位于“代码审查”，不改变代码归属 |
| 项目接入 | 代码项目、Provider 连接引用、分支/事件范围、连通性 | `acr-review` | 不把 Git 仓库当作业务系统；一个业务系统可关联多个代码项目 |
| 审查闭环 | 事件、审查任务、步骤、问题、整改复核、策略快照 | `acr-review` | “任务”只表示一次执行，“问题”跨任务跟踪整改 |
| 策略控制 | 审查方案、规则版本、模型选择、阈值、门禁策略 | `acr-review` | 引用 `acr-system` 的模型服务，不复制密钥或供应商配置 |
| 结果交付 | 评论/状态回写、通知、投递记录、业务预警 | `acr-review` | 每类外部副作用有独立幂等键和状态 |
| 洞察报告 | 指标事实、质量看板查询、报告快照、成本归集 | `acr-review` | 只消费稳定业务事实，不反向修改任务或问题 |
| 运行保障 | 审查 SLI、异常恢复、业务调度入口 | `acr-review`；通用调度仍在 `acr-quartz` | 服务器、Redis、Druid 等基础监控继续复用现有能力 |
| Web 接入 | 管理 REST、Webhook 协议、参数校验、鉴权声明 | `acr-admin` | Controller 只调用用例，不访问 Mapper 或编排主流程 |

前端页面和 API 在开发具体功能时再创建，不预建空目录。SQL、实体、Mapper、Service、Controller 和适配接口均随首个可验收纵向切片创建。

不预先规定完整包树。开始某个功能时，优先建立满足当期实现的最小包，例如用例、数据访问或某个外部适配边界；只有在出现两个以上实现或必须隔离外部系统时才引入接口抽象。

## 5. 主流程与对象边界

MVP（V0.1）目标链路保持为：

```text
Webhook 接入
  -> 验签与去重
  -> 创建审查任务
  -> 拉取 Diff
  -> 冻结审查方案版本
  -> 调用审查引擎
  -> 保存问题与审查结论
  -> 平台回写与通知投递
  -> 问题整改与人工复核
  -> 工作台/后台查询
```

关键约束：

- Webhook 接收与最终审查结果解耦；
- 事件、任务、回写和通知分别具备稳定幂等标识；
- 外部调用失败可定位到步骤，不用一个数据库事务包住整条链路；
- 审查任务状态、审查结论、回写状态和通知状态相互独立，不能压缩成一个成功/失败字段；
- 审查问题拥有独立整改生命周期；首版允许人工复核，后续自动重审不得按行号或文案简单匹配后静默关闭；
- 每个任务冻结实际使用的规则、Prompt、模型和阈值版本，配置更新不改写历史结论；
- API Key、Webhook Secret、仓库令牌不得明文回显或写入日志；
- OCR CLI 使用受控参数、目录、超时、输出大小和并发，不拼接 shell 命令；
- MySQL 保存业务事实，Redis 只用于缓存、限流和短期幂等辅助。

### 5.1 核心对象语义

| 对象 | 状态职责 | 主要关联 |
|---|---|---|
| 接入事件 | 是否可信接收、是否被忽略或建单 | 项目、外部事件唯一键、审查任务 |
| 审查任务 | 待执行、执行中、已完成、已失败、已取消 | 事件、变更版本、方案快照、步骤、问题 |
| 审查结论 | 通过、警告、阻断 | 审查任务、质量门禁 |
| 审查问题 | 待确认、待修复、待复核、已关闭、已忽略、误报 | 首次/最近任务、规则版本、责任人、历史 |
| 交付记录 | 待投递、成功、失败、跳过 | 任务/问题、渠道、外部幂等键 |
| 报告快照 | 生成中、已生成、失败、已发送 | 时间范围、权限范围、指标版本、发送记录 |

这些是产品语义，不要求首个切片一次性创建全部对象或状态。每个切片仍以最小可验收范围为准。

## 6. 当前最小目录（P0/M1）

```text
ai-code-review/
├── acr-admin/
├── acr-review/
│   ├── pom.xml
│   ├── README.md
│   └── src/
│       ├── main/
│       │   ├── java/com/acr/review/
│       │   │   ├── domain/        # 凭据、项目、模板、任务、执行记录、评分结果 DTO
│       │   │   ├── engine/        # OCR CLI 适配与工作区管理
│       │   │   ├── git/           # Provider / Webhook / PR 工作区、Diff、元数据
│       │   │   ├── mapper/        # 审查业务数据访问
│       │   │   ├── security/      # PAT / Webhook Secret AES-GCM
│       │   │   └── service/       # 项目、模板、Webhook、任务执行、评分解析/协议组合
│       │   └── resources/
│       │       ├── mapper/review/
│       │       └── review/schema/ # 评分结果 JSON Schema
│       └── test/java/com/acr/review/
├── acr-system/
├── acr-framework/
├── acr-common/
├── acr-quartz/
├── acr-ui/
├── docs/planning/
│   ├── product-roadmap.md
│   ├── architecture-scaffold.md
│   ├── github-project-access-m1.md
│   ├── github-pr-webhook-m2.md
│   ├── review-pipeline-m3.md
│   ├── review-template-config.md
│   └── review-scoring-result-protocol.md
├── skills/
│   └── plan-review-feature/
├── rules/
│   ├── architecture.md
│   └── delivery.md
└── sql/
    ├── 01_core_schema.sql … 17_review_project_engine_code_nullable.sql
    └── 18_review_execution_hardening.sql
```

`acr-admin` 承担 REST/Webhook 接入；页面和 API 位于 `acr-ui/src/views/review`、`acr-ui/src/views/system/aimodelconfig` 与 `acr-ui/src/api/review`。审查方式在项目级二选一（大模型审查 / 审查引擎）；审查模板为代码审查下的公共配置；大模型路径的评分与 JSON 协议由平台统一追加，模板页通过 `GET /review/template/platform-rules` 只读展示同一数据源。当前仅实现 GitHub，没有为 GitLab/Gitee/Gitea、通知或回写预建空实现。

## 7. 暂不做

- 不拆分更多业务 Maven 模块或微服务；
- 不引入 MQ、工作流引擎、向量数据库或对象存储；
- 不重构现有系统管理实现；
- 不为后续版本预建表、接口、页面或定时任务；
- 不提前固化尚未经实际功能验证的数据字段、状态枚举和 API 路径；
- 不在 MVP 同时实现全部 Git Provider、通知渠道、Push/全量扫描、自动模型降级或强制合并门禁；
- 不建设通用任务、缺陷、报表、APM 或审批平台，也不提供个人质量排行榜。
