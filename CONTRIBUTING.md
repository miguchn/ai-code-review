# 贡献指南

感谢你对 AI Code Review 的兴趣！本文件说明如何搭建开发环境、报告问题和提交贡献。参与本项目即视为同意 [贡献者公约](CODE_OF_CONDUCT.md)。

## 开发环境

- Java 17+、Maven 3.8+、Node.js 18+、MySQL 8.0+、Redis 6.0+

```bash
# 1. 初始化数据库（空库一次性初始化；存量环境按 sql/README.md 走增量脚本）
mysql --default-character-set=utf8mb4 -u root -p < sql/init-full.sql

# 2. 凭据主密钥（32 字节 Base64，勿提交到任何仓库）
export ACR_CREDENTIAL_MASTER_KEY="$(openssl rand -base64 32)"

# 3. 后端
mvn clean install -DskipTests
cd acr-admin && mvn spring-boot:run

# 4. 前端
cd acr-ui && npm install && npm run dev
```

数据库配置在 `acr-admin/src/main/resources/application-dev.yml`。更多细节见 [README 快速开始](README.md#快速开始) 与 [docs/deployment.md](docs/deployment.md)。

## 报告问题

- 先用仓库内搜索和 [docs/planning/](docs/planning/) 确认是否为已知设计或已规划事项；
- Bug 报告请附：版本号/分支、复现步骤、期望与实际行为、后端日志关键片段（**脱敏**：不要贴 PAT、Webhook Secret、主密钥等任何凭据）；
- **安全漏洞不要走公开 issue**，流程见 [SECURITY.md](SECURITY.md)。

## 提交 Pull Request

1. 从 `main` 拉取最新代码，创建功能/修复分支：`feature/<name>` 或 `fix/<issue>`；
2. 涉及代码审查业务的新功能，先阅读对应 [切片设计文档](docs/planning/) 或按 `skills/plan-review-feature/` 收敛范围，避免无规划的大面积改动；
3. 遵守 `rules/architecture.md` 与 `rules/delivery.md` 的架构和交付约束；前端 UI 另需遵守 `rules/UI_THEME_RULES.md`；
4. 提交前必须通过基础验证：

   ```bash
   mvn test
   cd acr-ui && npm run build:prod
   ```

5. Commit 使用 Conventional Commits：`feat:` / `fix:` / `refactor:` / `docs:` / `test:` / `chore:`，中文描述即可；
6. PR 描述说明动机、改动范围和验证方式；数据库结构变更必须附带 `sql/` 下按编号递增的增量脚本（已执行的增量脚本不改语句，见 `sql/README.md`）。

## 代码风格约定

- 后端：Java 17 + Spring Boot 4 + MyBatis，沿用 RuoYi 风格分层（controller/service/mapper/domain），权限用 `@PreAuthorize` + `@DataScope`；
- 前端：Vue 3 组合式 API + Element Plus，组件按业务目录组织；
- 密文（PAT、Webhook Secret、通知 Webhook、模型 API Key）一律 AES-GCM 加密存储，接口响应不回显，日志脱敏；
- 小步提交、小文件优先；一个 PR 只解决一个问题域。

## 评审与合并

- 至少说明自测路径（接口/页面/自动化测试）；
- 涉及权限、凭据、Webhook 验签等安全敏感面的改动，会在评审中重点核对；
- 维护者可能要求拆分或补充文档，属于正常流程。
