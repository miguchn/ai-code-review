# Claude Code 项目入口

所有自动化协作统一遵守根目录 `AGENTS.md`。开始代码审查业务任务前，依次阅读：

1. `docs/planning/product-roadmap.md`
2. `docs/planning/architecture-scaffold.md`
3. `docs/planning/domain-api-contracts.md`
4. `rules/architecture.md`
5. `rules/delivery.md`

基础验证命令：

```bash
mvn test
cd acr-ui && npm run build:prod
```

不要在未完成单项功能规划前生成业务 CRUD，也不要新增框架、组件或 Maven 模块。
