# AI Code Review 协作入口

本文件是仓库内自动化开发与评审工作的总入口。开始业务任务前，以现有代码、`README.md` 和 `docs/planning/` 为事实来源。

## 必读顺序

1. `README.md`：产品定位、技术栈和当前能力；
2. `docs/planning/product-roadmap.md`：业务边界、依赖、阶段和验收目标；
3. `docs/planning/architecture-scaffold.md`：模块归属与最小结构；
4. `rules/architecture.md`、`rules/delivery.md`：开发与交付约束。

## 工作方式

- 开始单项业务开发前，使用 `skills/plan-review-feature/` 收敛范围、依赖和验收；
- 代码发现优先使用项目知识图谱；配置、SQL、文案或图谱不足时再使用文本搜索；
- 先定义成功标准，再做最小变更；
- 文档与代码不一致时先记录差异，不静默扩展范围。

## 强制边界

- `acr-admin` 只放启动、配置和 REST/Webhook 接入，不放主要业务逻辑；
- 代码审核主业务统一进入 `acr-review`，不继续堆入 `acr-system`；
- `acr-system` 保持平台治理职责，`acr-common` 只放稳定的跨模块能力；
- 不按仓库、任务、规则、报告等小功能继续拆 Maven 模块；
- Controller 只处理协议、校验、权限和用例调用，不直接访问 Mapper；
- 外部平台差异收敛在适配边界，Webhook 必须先验签再去重；
- 凭据不得明文回显、写日志或进入版本库；
- 不预建未被当前功能使用的实体、Mapper、Service、Controller、页面或配置。

## 基础验证

- 后端：`mvn test`
- 前端：`cd acr-ui && npm run build:prod`

后续业务实现必须补充与风险相匹配的测试。
