# 项目架构与最小骨架

## 1. 本次结构结论

项目保留现有单体、多 Maven 模块形态，并新增一个统一的 `acr-review` 业务模块。新增模块只解决主业务归属问题，不代表拆分微服务，也不按仓库、任务、规则、报告等功能继续拆 Maven 模块。

判断依据：

- `acr-admin` 应只承担启动、配置和 HTTP/Webhook 接入，不能承载审查编排与数据访问；
- `acr-system` 已承担用户、权限、部门、字典、业务系统和模型配置等平台治理职责；
- README 规划的项目接入、事件、任务、引擎、结果、通知、缺陷和质量治理属于同一条代码审核业务链；
- 将这条业务链继续放入 `acr-system` 会混淆平台治理与核心业务，统一放入 `acr-review` 可形成清晰且无循环的依赖边界。

## 2. 技术与依赖基线

沿用 Java 17、Spring Boot 4、MyBatis、MySQL、Redis、Quartz、Vue 3 和现有安全体系，不引入新框架或基础设施。

```text
acr-ui ──HTTP──> acr-admin
                   ├──> acr-framework ──> acr-system ──> acr-common
                   ├──> acr-review ─────> acr-system ──> acr-common
                   └──> acr-quartz ─────> acr-common
```

当前不让 `acr-quartz` 依赖 `acr-review`。进入报告、全量扫描或项目哨兵开发时，再根据真实调度入口评审最小依赖；Quartz 任务本身只负责触发，业务处理仍归 `acr-review`。

## 3. 模块职责

| 模块 | 当前职责 | 明确不承载 |
|---|---|---|
| `acr-admin` | Spring Boot 启动、运行配置、REST/Webhook Controller、协议校验、鉴权声明 | 审查业务规则、Mapper、流程编排、平台适配实现 |
| `acr-review` | 代码审核主业务的数据访问、用例服务、任务编排及 Git/引擎/通知适配 | 系统权限底座、通用框架配置 |
| `acr-system` | 用户、角色、部门、菜单、字典、参数、业务系统、模型配置等平台治理 | 审查任务、结果、缺陷、规则和报告主业务 |
| `acr-framework` | Security、JWT、Redis、Druid、AOP、线程池等框架能力 | 任何代码审核业务规则 |
| `acr-common` | 稳定的跨模块类型和工具、现有 AI Client 基础 | 单一业务模块专用对象或流程 |
| `acr-quartz` | 通用定时任务管理与触发入口 | 定时业务的具体实现 |
| `acr-ui` | 管理端 Web 页面与 API 调用 | 后端业务判断 |

`acr-generator` 与产品运行和代码审核业务无关，且会通过 `acr-admin` 依赖暴露演示型运行时接口，因此不再保留。

## 4. 业务归属

- M0 基础治理继续由 `acr-system` 提供；
- 产品路线图 M1-M8 的代码审核能力统一进入 `acr-review`；
- 对外 REST/Webhook 入口可放在 `acr-admin`，但只能调用 `acr-review` 暴露的用例；
- 前端页面和 API 在开发具体功能时再创建，不预建空目录；
- SQL、实体、Mapper、Service、Controller、适配接口均随首个可验收纵向切片创建。

不预先规定完整包树。开始某个功能时，优先建立满足当期实现的最小包，例如用例、数据访问或某个外部适配边界；只有在出现两个以上实现或必须隔离外部系统时才引入接口抽象。

## 5. 主流程边界

P0 目标链路保持为：

```text
Webhook 接入
  -> 验签与去重
  -> 创建审查任务
  -> 拉取 Diff
  -> 调用审查引擎
  -> 保存结果
  -> 平台回写
  -> 通知投递
  -> 后台查询
```

关键约束：

- Webhook 接收与最终审查结果解耦；
- 事件、任务、回写和通知分别具备稳定幂等标识；
- 外部调用失败可定位到步骤，不用一个数据库事务包住整条链路；
- API Key、Webhook Secret、仓库令牌不得明文回显或写入日志；
- OCR CLI 使用受控参数、目录、超时、输出大小和并发，不拼接 shell 命令；
- MySQL 保存业务事实，Redis 只用于缓存、限流和短期幂等辅助。

## 6. 当前最小目录

```text
ai-code-review/
├── acr-admin/
├── acr-review/
│   ├── pom.xml
│   └── README.md
├── acr-system/
├── acr-framework/
├── acr-common/
├── acr-quartz/
├── acr-ui/
├── docs/planning/
│   ├── product-roadmap.md
│   └── architecture-scaffold.md
├── skills/
│   └── plan-review-feature/
├── rules/
│   ├── architecture.md
│   └── delivery.md
└── sql/
```

`acr-review` 暂不建立 `src`、空类或分层占位目录；首个正式业务切片应先完成单项计划，再按实际类落地目录。

## 7. 暂不做

- 不拆分更多业务 Maven 模块或微服务；
- 不引入 MQ、工作流引擎、向量数据库或对象存储；
- 不重构现有系统管理实现；
- 不为 P1-P3 预建表、接口、页面或定时任务；
- 不提前固化尚未经实际功能验证的数据字段、状态枚举和 API 路径。
