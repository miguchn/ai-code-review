# acr-review

`acr-review` 是代码审核主业务的统一承载模块，后续用于项目与 Git 接入、Webhook 事件、审查任务与结果、引擎编排、平台回写、通知及质量治理。

当前只建立 Maven 依赖边界，不包含业务实现，也不预建空的 Controller、Service、Mapper、实体或适配器。实现具体纵向功能时再按实际需要创建最小包结构。

依赖方向：

```text
acr-admin -> acr-review -> acr-system -> acr-common
```

- `acr-admin` 只保留启动、配置以及 REST/Webhook 接入；
- `acr-review` 承载代码审核业务规则、数据访问、流程编排和外部适配；
- `acr-system` 提供用户、权限、部门、业务系统和模型配置等平台治理能力；
- `acr-common` 只保留稳定的跨模块通用能力。
