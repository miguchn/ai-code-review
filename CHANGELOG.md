# Changelog

## [Unreleased] - 2026-08-02

### M3 审查流水线独立 Review 加固

- 修复执行链断点：run 记录建立在配置解析之前并回填快照审查方式，执行期任意异常统一落 FAILED，消除任务卡死 RUNNING 的僵尸态；新增 `18_review_execution_hardening.sql` 放宽 `snapshot_review_mode` 可空
- RUNNING 超过 30 分钟视为执行中断，claimTask 支持超时领取，任务可人工重试回收（含前端重试入口与执行中自动刷新）
- 安全：PAT 改经 `GIT_CONFIG_*` 环境变量注入 git 子进程，不再出现在进程命令行参数；错误消息按 GitHub Token 格式正则兜底脱敏
- 健壮性：base/head SHA 入口格式校验；Diff 响应体有界读取（≤800KB）；GitHub 限流识别为独立失败类型 `RATE_LIMIT`；PR 元数据非 JSON 响应按不可用降级
- 权限：任务详情与重试接口补项目部门数据范围校验；移除未使用的模板复制端点（复用新增接口与 `review:template:add` 权限）
- 解析容错：模型输出支持任意位置 markdown 围栏与括号配平提取；评分维度缺失/行号倒置判格式异常；模板渲染改单趟替换防占位符二次展开；结论严重度改精确匹配防子串误判
- 任务列表查询不再拉取 mediumtext 快照正文
- 历史任务（快照冻结上线前建单）执行时按项目当前配置补冻结快照并落库；快照抽取为共享服务 `ReviewTaskSnapshotService`，建单与补冻结同一套校验；项目未配置时给出配置指引而非「不支持的审查方式：null」

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
