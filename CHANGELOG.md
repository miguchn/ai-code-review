# Changelog

## [0.2.0] - 2026-07-30

### 项目骨架复核

- 新增统一的 `acr-review` 核心业务模块边界，不预建业务类；
- 明确 `acr-admin` 只承担启动、配置和 Web 接入；
- 移除代码生成模块、演示接口、示例定时任务及对应前端、菜单和数据表；
- 收缩规划、skills、agents 和 rules，只保留当前阶段需要的最小协作骨架。

## [0.1.0] - 2026-07-30

### 初始化

- 基于 ApiHub 项目公共层重构，去除业务模块（doc/ai/asset）
- 项目重命名：apihub → ai-code-review（acr）
- 包名重构：com.apihub → com.acr
- 保留模块：common、system、framework、admin、quartz、generator、ui
- 保留能力：RBAC 权限、用户管理、字典、日志、定时任务、代码生成器、AI Client 抽象
- 创建 README.md、CLAUDE.md、部署文档
- 初始化 Git 仓库
