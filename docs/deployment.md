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
mysql -u root -p ai_code_review < sql/ry_20260417.sql
mysql -u root -p ai_code_review < sql/quartz.sql
mysql -u root -p ai_code_review < sql/sys_manage_20260512.sql
```

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

# 配置模型
ocr config provider    # 选择供应商
ocr config model       # 选择模型

# 验证
ocr --version
```

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

1. 在 GitLab 项目设置中添加 Webhook
2. URL: `http://your-server:8080/webhook/gitlab`
3. Trigger Events: Push Events + Merge Request Events
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
