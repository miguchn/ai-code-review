# 部署说明

## 环境要求

| 组件 | 版本要求 | 说明 |
|------|---------|------|
| JDK | 17+ | 后端运行环境 |
| Maven | 3.8+ | 后端构建 |
| Node.js | 18+ | 前端构建 |
| MySQL | 8.0+ | 主数据库 |
| Redis | 6.0+ | 缓存/会话 |
| npm | 9+ | 前端包管理 |
| OCR (可选) | 最新 | alibaba/open-code-review CLI |

## 开发环境部署

### 1. 数据库初始化

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE ai_code_review DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"

# 导入基础表结构
mysql -u root -p ai_code_review < sql/01_core_schema.sql
mysql -u root -p ai_code_review < sql/02_quartz_schema.sql
mysql -u root -p ai_code_review < sql/03_system_management.sql
mysql -u root -p ai_code_review < sql/04_github_project_access.sql
mysql -u root -p ai_code_review < sql/05_github_pr_scope.sql
mysql -u root -p ai_code_review < sql/06_llm_model_service.sql
mysql -u root -p ai_code_review < sql/07_review_engine.sql
mysql -u root -p ai_code_review < sql/08_github_pr_webhook.sql
mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/09_llm_custom_provider.sql
mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/10_llm_menu_charset_fix.sql
mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/11_llm_column_comment_charset_fix.sql
mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/12_review_engine_button_fix.sql
mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/13_review_pipeline_m3.sql
mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/14_review_dual_mode_prompt.sql
mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/15_review_template_config.sql
mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/16_review_scoring_result_protocol.sql
mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/17_review_project_engine_code_nullable.sql
mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/18_review_execution_hardening.sql
mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/19_review_record_experience_m3_1.sql
mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/20_review_record_charset_fix.sql
mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/21_review_record_list_fields.sql
mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/22_review_scope_config.sql
mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/23_review_delivery_record.sql
mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/24_notification_management_m5.sql
mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/25_issue_ledger_m6.sql
mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/26_issue_delivery_trace_m6_1.sql
mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/27_sidebar_menu_ia.sql
mysql --default-character-set=utf8mb4 -u root -p ai_code_review < sql/28_delivery_menu_route_name.sql
```

> 含中文的 SQL 必须使用 `--default-character-set=utf8mb4`（或脚本内 `SET NAMES utf8mb4`）执行，避免菜单/字典文案乱码。

启动后端前必须配置 `ACR_CREDENTIAL_MASTER_KEY`（Base64 编码的 32 字节密钥，可用
`openssl rand -base64 32` 生成）。升级环境首次启动时会使用该密钥将历史明文模型 API Key
原地迁移为 AES-256-GCM 密文；未配置或密钥无效时应用会拒绝带明文凭据启动。
模型调用地址默认仅允许公网 HTTPS。可信开发环境如需连接本机或内网兼容服务，可显式设置
`ACR_LLM_ALLOW_HTTP=true` 和 `ACR_LLM_ALLOW_PRIVATE_ENDPOINTS=true`；生产环境不应开启。

其他可选环境变量：

| 变量 | 默认值 | 说明 |
|---|---|---|
| `ACR_WEBHOOK_CALLBACK_URL` | `http://localhost:8080` | 生成 GitHub Webhook 回调地址的外网 base URL，生产环境必须改为公网可达地址 |
| `ACR_REVIEW_LLM_TIMEOUT_SECONDS` | `120` | 大模型审查单次执行超时秒数，记录进任务运行快照 |

### 2. 后端配置

编辑 `acr-admin/src/main/resources/application-dev.yml`：

```yaml
spring:
  datasource:
    druid:
      master:
        url: jdbc:mysql://localhost:3306/ai_code_review?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true
        username: root
        password: your_password
  data:
    redis:
      host: localhost
      port: 6379
      password:        # 如有密码请配置
```

> 工作台「今日摘要」与高风险 7 天窗口依赖 MySQL `CURDATE()` / 会话日期。请保持 JDBC `serverTimezone` 与业务时区一致（开发默认 `GMT+8`）；更换时区后「今日」边界会随之变化。

### 3. 启动后端

```bash
# 编译
mvn clean install -DskipTests

# 启动
cd acr-admin
mvn spring-boot:run
# 或
java -jar target/acr-admin.jar
```

后端默认端口：`8080`

### 4. 启动前端

```bash
cd acr-ui
npm install
npm run dev
```

前端默认端口：`80`（Vite 代理到后端 8080）

### 5. 访问

- 前端页面：http://localhost
- 后端 API：http://localhost:8080
- Swagger 文档：http://localhost:8080/swagger-ui.html
- Druid 监控：http://localhost:8080/druid

默认管理员：`admin / admin123`

## OCR 引擎安装（可选）

```bash
# 安装
npm install -g @alibaba-group/open-code-review

# 验证
ocr version
```

平台通过 `review.engine.*` 或以下环境变量配置本地 CLI 适配，不在业务代码中写死路径：

| 环境变量 | 说明 | 默认值 |
|---------|------|--------|
| `ACR_OCR_EXECUTABLE` | CLI 可执行文件路径 | `ocr` |
| `ACR_OCR_WORKSPACE_ROOT` | 独立工作目录根路径 | `${java.io.tmpdir}/acr-review-engine` |
| `ACR_OCR_TIMEOUT_SECONDS` | 单次调用超时（秒） | `600` |
| `ACR_OCR_MAX_CONCURRENCY` | 最大并发数 | `2` |
| `ACR_OCR_MAX_OUTPUT_BYTES` | stdout/stderr 输出上限 | `1048576` |

测试调用时，平台会将已配置的 AI 模型映射为 `OCR_LLM_URL` / `OCR_LLM_TOKEN` / `OCR_LLM_MODEL` 等环境变量注入子进程，密钥不会写入日志。

管理入口：**模型服务 → 审查引擎**，提供环境检测与内置样例测试调用。

## 生产环境部署（Docker Compose）

待完善。规划的容器架构：

```yaml
services:
  mysql:
    image: mysql:8.0
    # ...
  redis:
    image: redis:7-alpine
    # ...
  backend:
    build: ./acr-admin
    depends_on: [mysql, redis]
    # 容器内安装 OCR CLI
  frontend:
    image: nginx:alpine
    volumes:
      - ./acr-ui/dist:/usr/share/nginx/html
```

## GitLab Webhook 配置（待实现）

以下仅为 MVP（V0.1）试点建议，最终 URL、Secret 和支持版本须在该切片开发前确认：

1. 在 GitLab 项目设置中添加 Webhook
2. URL: `http://your-server:8080/webhook/gitlab`
3. Trigger Events: Merge Request Events（Push Events 不进入 MVP）
4. Secret Token: 配置在管理后台

## 通知推送配置（待实现）

### 钉钉

1. 钉钉群 → 群设置 → 智能群助手 → 添加机器人 → 自定义
2. 获取 Webhook URL
3. 在管理后台「通知配置」中填入

### 企业微信

1. 企业微信管理后台 → 应用管理 → 创建应用
2. 获取 Webhook URL
3. 在管理后台「通知配置」中填入

### 飞书

1. 飞书开放平台 → 创建机器人
2. 获取 Webhook URL
3. 在管理后台「通知配置」中填入

### 邮件（待实现）

1. 规划通过 SMTP 发送，配置发件服务器与收件规则
2. 在管理后台「通知配置」中启用邮件渠道
3. 支持按严重度分级触发、@提交者
