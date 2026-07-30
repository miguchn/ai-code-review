# 架构规则

## 模块归属

- `acr-admin`：启动入口、运行配置、REST/Webhook Controller、协议校验和权限声明；
- `acr-review`：代码审核业务的数据访问、用例、编排和 Git/引擎/通知适配；
- `acr-system`：用户、权限、组织、字典、参数、业务系统和模型配置；
- `acr-framework`：Security、JWT、Redis、Druid、AOP、线程池等框架能力；
- `acr-common`：稳定的跨模块通用类型与工具；
- `acr-quartz`：通用调度管理与触发入口；
- `acr-ui`：Vue 管理端。

依赖方向：

```text
acr-admin -> acr-framework -> acr-system -> acr-common
acr-admin -> acr-review -> acr-system -> acr-common
acr-admin -> acr-quartz -> acr-common
```

不得建立反向依赖或循环依赖，也不得按审查小功能继续拆 Maven 模块。`acr-quartz` 需要调用审查业务时，必须在对应 P2 功能中单独评审依赖。

## 实现边界

- `acr-admin` Controller 不直接访问 Mapper，不实现任务编排；
- `acr-system` 不新增 ReviewTask、ReviewFinding、规则或报告等审查业务对象；
- `acr-review` 只在当前功能需要时创建包和类，不一次性铺满分层目录；
- 只有两个以上实现或必须隔离外部系统时才创建接口抽象；
- Provider、审查引擎和通知渠道的差异不得散落在业务用例中；
- Quartz 类只触发业务用例，具体业务仍由 `acr-review` 完成；
- `acr-common` 不接收单一业务模块的专用对象。

## 集成约束

- 在没有容量证据前，使用 MySQL 状态、现有线程池和 Quartz，不引入消息队列；
- Webhook 原始载荷如需保存，必须限制大小、脱敏并明确保留期；
- OCR CLI 作为受控外部进程调用，限制参数、工作目录、超时、输出和并发；
- Redis 只用于缓存、限流和短期幂等辅助，MySQL 保存业务事实。
