---
name: plan-review-feature
description: 将 AI Code Review 路线图中的单个业务条目收敛为可实施、可验收的开发计划。用于开始 Webhook、Git Provider、审查引擎、任务编排、结果回写、通知、缺陷、规则、报表或其他审查业务开发前，以及需求变更需要重新确认范围、依赖、数据、接口和测试边界时。
---

# 规划审查功能

## 工作流

1. 阅读仓库根 `README.md`、`docs/planning/product-roadmap.md`、`docs/planning/architecture-scaffold.md`。
2. 用代码知识图谱确认现有实现，不因 README 标为“待开发”就假定代码为空。
3. 只选择一个可独立验收的路线图条目；列出业务目标、用户、前置依赖、本次范围和非范围。
4. 将主要业务映射到 `acr-review`，把 HTTP/Webhook 接入映射到 `acr-admin`；不新增框架、组件或更多业务模块。
5. 明确对象、状态、接口、权限、幂等键、外部副作用和失败补偿。
6. 把工作拆成最小纵向切片，每一步写出可执行的验证方式。
7. 对照 `rules/architecture.md` 与 `rules/delivery.md` 做最后收敛。

## 输出格式

按以下顺序输出：

- 目标与成功指标；
- 当前基线与缺口；
- 本次范围 / 非范围；
- 依赖与待决策项；
- 数据、接口、权限和流程；
- 分步实现计划，每步附验证；
- 验收标准与风险。

除非用户同时明确要求实施，否则只产出计划，不创建实体、Mapper、Controller、页面或 SQL。
