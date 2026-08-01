# Claude Code 项目入口

所有自动化协作统一遵守根目录 `AGENTS.md`。开始代码审查业务任务前，依次阅读：

1. `docs/planning/product-roadmap.md`
2. `docs/planning/architecture-scaffold.md`
3. `rules/architecture.md`
4. `rules/delivery.md`

`rules/` 目录下的项目规则均为强制约束，开发前必须读取并遵守。涉及 `acr-ui` 页面、组件、样式、图标或插图时，还必须阅读 `rules/UI_THEME_RULES.md`。

基础验证命令：

```bash
mvn test
cd acr-ui && npm run build:prod
```

不要在未完成单项功能规划前生成业务 CRUD，也不要新增框架、组件或继续拆分业务 Maven 模块。
