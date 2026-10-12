# 后台文案对照稿

2026-10-12 · 原文案 → 新文案 · 文案对照稿；耗时指标已另行修正

按当前仓库的菜单、页面和实际行为整理，共 1764 条对照，其中 42 个菜单、106 个权限树功能名称，技术原名使用方式已按用户确认定稿。清楚的原文保留。

名称为 2–4 字，描述及短提示每条不超过 20 字。原来的长说明拆成多条短句，保留使用条件和行为限制。必要技术原名保留在说明、示例、单位及对应平台的帮助目录中。

统一用“检查”表达代码检查，用“复核”表达人工核实；重新运行任务与重新发送评论、群消息分别命名。检查完成、风险结论、发送状态、问题状态保持独立。

菜单层级、顺序、权限、功能、数据口径和技术代码值保持原样。评分名称只建议修改界面展示，评分标准、模型指令与历史记录保持原样。报告中心和旧提示词管理仍不作为已提供的功能。数据监控按用户要求不处理。

依据 [《Don't Make Me Think》作者介绍](https://sensible.com/dont-make-me-think/) 的可用性取向，以及 [《写给大家看的设计书》出版社说明](https://www.peachpit.com/store/non-designers-design-book-9780321193858) 的对比、重复、对齐和亲密原则：使用常用词，同一功能同名，名称与用途一起呈现。此次不调整界面布局。

## 技术原名处理方式

已确认：说明、示例和计量单位保留 Webhook、Token、GitHub、P95 等必要原名，方便对照外部平台。菜单、按钮和字段名称仍优先使用 2–4 字中文。平台接入帮助标题保留品牌名称；下表同时列明尚未提供的统计，不把它作为可用功能。

P95 原有说明与汇总方式不同：当前取各项目每日 P95 的最大值，不能描述为全局 P95。复核流转率目前尚未提供，不新增为可用统计。

| 位置 | 原文案 | 新文案 | 描述（≤20字） | 用途 | 备注 | 原文位置 |
| --- | --- | --- | --- | --- | --- | --- |
| 访问密钥 | Personal Access Token | 个人密钥 | 填写代码平台的个人访问密钥 | 字段 | Personal Access Token 保留在填写说明中 | [acr-ui/src/views/review/credential/index.vue:100](../../acr-ui/src/views/review/credential/index.vue#L100) |
| 模型设置 | 元/千 Token | 元/千 Token | 每一千 Token 的人民币单价 | 计量单位 | 按用户确认保留精确计价单位 | [acr-ui/src/views/system/aimodelconfig/index.vue:178](../../acr-ui/src/views/system/aimodelconfig/index.vue#L178) |
| 模型设置 | OpenAI Compatible Chat Completions 地址 | 兼容地址 | 填写兼容聊天接口的地址 | 字段 | 说明保留 OpenAI Compatible Chat Completions，避免误接其他接口 | [acr-ui/src/views/system/aimodelconfig/index.vue:126](../../acr-ui/src/views/system/aimodelconfig/index.vue#L126) |
| 检查模板 | 描述本技术栈的专项审查重点；支持占位符 {{pr_title}} {{pr_description}} {{commit_messages}} {{source_branch}} {{target_branch}} {{base_sha}} {{head_sha}} {{diff}}。无需编写评分规则或输出格式。 | 填写检查重点 | 支持变量，无需另写评分规则 | 说明 | 说明和示例保留全部原变量名：{{pr_title}} {{pr_description}} {{commit_messages}} {{source_branch}} {{target_branch}} {{base_sha}} {{head_sha}} {{diff}}；无需另写输出格式 | [acr-ui/src/views/review/template/index.vue:128](../../acr-ui/src/views/review/template/index.vue#L128) |
| 通用显示 | 子模块指针 | 版本引用 | 指向 Git 子模块版本的变更 | 名称 | 名称用短中文，说明保留 Git 子模块概念 | [acr-ui/src/utils/reviewDisplay.js:46](../../acr-ui/src/utils/reviewDisplay.js#L46) |
| 通知设置 | Webhook URL | 通知地址 | 填写群机器人的 Webhook 地址 | 字段 | 说明保留 Webhook URL，方便对应机器人设置 | [acr-ui/src/views/notify/channel/index.vue:98](../../acr-ui/src/views/notify/channel/index.vue#L98)<br>[acr-ui/src/views/notify/channel/index.vue:100](../../acr-ui/src/views/notify/channel/index.vue#L100)<br>[acr-ui/src/views/notify/channel/index.vue:106](../../acr-ui/src/views/notify/channel/index.vue#L106)<br>[acr-ui/src/views/notify/channel/index.vue:144](../../acr-ui/src/views/notify/channel/index.vue#L144) |
| 任务详情 | SHA | 提交编号 | SHA，代码提交的唯一标识 | 字段 | 说明保留 SHA，方便对应代码平台 | [acr-ui/src/views/review/task/detail.vue:119](../../acr-ui/src/views/review/task/detail.vue#L119) |
| 记录详情 | SHA | 提交编号 | SHA，代码提交的唯一标识 | 字段 | 说明保留 SHA，方便对应代码平台 | [acr-ui/src/views/review/record/detail.vue:232](../../acr-ui/src/views/review/record/detail.vue#L232) |
| 主机状态 | Java虚拟机信息 | 运行环境 | 查看 Java 虚拟机信息 | 名称 | 说明保留 Java 虚拟机 | [acr-ui/src/views/monitor/server/index.vue:103](../../acr-ui/src/views/monitor/server/index.vue#L103) |
| 主机状态 | Java名称 | 环境名称 | 查看 Java 虚拟机名称 | 字段 | 说明保留 Java 虚拟机 | [acr-ui/src/views/monitor/server/index.vue:108](../../acr-ui/src/views/monitor/server/index.vue#L108) |
| 主机状态 | Java版本 | 环境版本 | 查看 Java 版本 | 字段 | 说明保留 Java | [acr-ui/src/views/monitor/server/index.vue:110](../../acr-ui/src/views/monitor/server/index.vue#L110) |
| 定时任务 | cron 表达式 | 运行时间 | 填写 Cron 定时表达式 | 字段 | 说明和示例保留 Cron 语法 | [acr-ui/src/views/monitor/job/detail.vue:116](../../acr-ui/src/views/monitor/job/detail.vue#L116) |
| 定时任务 | Cron 表达式 | 运行时间 | 填写 Cron 定时表达式 | 字段 | 说明和示例保留 Cron 语法 | [acr-ui/src/views/monitor/job/index.vue:104](../../acr-ui/src/views/monitor/job/index.vue#L104) |
| 定时任务 | Cron 表达式生成器 | 设置时间 | 生成 Cron 定时表达式 | 名称 | 说明和示例保留 Cron 语法 | [acr-ui/src/views/monitor/job/index.vue:231](../../acr-ui/src/views/monitor/job/index.vue#L231) |
| 用户设置 | 登记企业微信 userid | 填写账号 | 填写企业微信 userid | 动作 | 说明保留 userid，与登录名区分 | [acr-ui/src/views/system/user/profile/commitEmail.vue:88](../../acr-ui/src/views/system/user/profile/commitEmail.vue#L88) |
| 用户设置 | 登记 open_id | 填写账号 | 填写飞书 open_id | 动作 | 说明保留 open_id，与登录名区分 | [acr-ui/src/views/system/user/profile/commitEmail.vue:89](../../acr-ui/src/views/system/user/profile/commitEmail.vue#L89) |
| 统计指标 | P95 审查时延 | 检查耗时 | 各项目每日成功任务 P95 的最大值 | 名称 | 保留 P95 说明；当前汇总不是全局 P95 | [acr-review/src/main/java/com/acr/review/insight/InsightMetrics.java:64](../../acr-review/src/main/java/com/acr/review/insight/InsightMetrics.java#L64) |
| 统计指标 | 复核流转率 | 复核流转率 | 当前尚未提供此项统计 | 未提供 | 隐藏规划项，不列为当前可用功能；本项不属于技术原名问题 | [acr-review/src/main/java/com/acr/review/insight/InsightMetrics.java:76](../../acr-review/src/main/java/com/acr/review/insight/InsightMetrics.java#L76) |
| 缓存状态 | Redis版本 | 缓存版本 | 查看 Redis 版本 | 字段 | 说明保留 Redis | [acr-ui/src/views/monitor/cache/index.vue:11](../../acr-ui/src/views/monitor/cache/index.vue#L11) |
| 缓存状态 | 使用CPU | 处理时间 | 查看 Redis 的 CPU 使用时间 | 字段 | 说明保留 CPU；展示累计 CPU 时间，非使用率 | [acr-ui/src/views/monitor/cache/index.vue:25](../../acr-ui/src/views/monitor/cache/index.vue#L25) |
| 缓存状态 | AOF是否开启 | 逐条记录 | 查看 Redis AOF 是否开启 | 字段 | 说明保留 AOF，避免与快照混淆 | [acr-ui/src/views/monitor/cache/index.vue:31](../../acr-ui/src/views/monitor/cache/index.vue#L31) |
| 缓存状态 | RDB是否成功 | 快照状态 | 查看 Redis RDB 保存结果 | 字段 | 说明保留 RDB，避免与逐条记录混淆 | [acr-ui/src/views/monitor/cache/index.vue:33](../../acr-ui/src/views/monitor/cache/index.vue#L33) |
| 运行概况 | OCR 占用 | 工具占用 | 查看 OCR 工具已占用名额 | 名称 | 说明保留 OCR，指服务器代码检查工具 | [acr-ui/src/views/monitor/runtime/index.vue:61](../../acr-ui/src/views/monitor/runtime/index.vue#L61) |
| 使用帮助 | GitHub 接入 | GitHub 接入 | 按对应平台的说明连接仓库 | 帮助标题 | 帮助目录保留平台原名，便于找到对应接入说明 | [acr-ui/src/guide/manifest.js:14](../../acr-ui/src/guide/manifest.js#L14) |
| 使用帮助 | GitLab 接入 | GitLab 接入 | 按对应平台的说明连接仓库 | 帮助标题 | 帮助目录保留平台原名，便于找到对应接入说明 | [acr-ui/src/guide/manifest.js:15](../../acr-ui/src/guide/manifest.js#L15) |
| 使用帮助 | Gitee 接入 | Gitee 接入 | 按对应平台的说明连接仓库 | 帮助标题 | 帮助目录保留平台原名，便于找到对应接入说明 | [acr-ui/src/guide/manifest.js:16](../../acr-ui/src/guide/manifest.js#L16) |
| 使用帮助 | Gitea 接入 | Gitea 接入 | 按对应平台的说明连接仓库 | 帮助标题 | 帮助目录保留平台原名，便于找到对应接入说明 | [acr-ui/src/guide/manifest.js:17](../../acr-ui/src/guide/manifest.js#L17) |

## 菜单对照

所有现有菜单均列出；数据监控保留原文案。

| 位置 | 原文案 | 新文案 | 描述（≤20字） | 用途 | 备注 | 原文位置 |
| --- | --- | --- | --- | --- | --- | --- |
| 一级菜单 | 工作台 | 工作台 | 查看今日待办和最近动态 | 菜单 |  | [acr-ui/src/router/index.js:59](../../acr-ui/src/router/index.js#L59) |
| 一级菜单 | 审查中心 | 代码检查 | 检查代码，跟进发现的问题 | 菜单 |  | [sql/init-full.sql:1387](../../sql/init-full.sql#L1387) |
| 审查中心 | 问题台账 | 问题跟进 | 确认问题，跟进修复和复核 | 菜单 |  | [sql/init-full.sql:1418](../../sql/init-full.sql#L1418) |
| 审查中心 | 审查任务 | 检查任务 | 查看检查进度，处理执行异常 | 菜单 |  | [sql/init-full.sql:1412](../../sql/init-full.sql#L1412) |
| 审查中心 | 审查记录 | 检查记录 | 查看检查结果和失败原因 | 菜单 |  | [sql/init-full.sql:1415](../../sql/init-full.sql#L1415) |
| 一级菜单 | 数据洞察 | 质量统计 | 查看项目质量和模型用量 | 菜单 |  | [sql/init-full.sql:1391](../../sql/init-full.sql#L1391) |
| 数据洞察 | 总览看板 | 质量总览 | 查看检查、问题和发送情况 | 菜单 |  | [sql/init-full.sql:1419](../../sql/init-full.sql#L1419) |
| 数据洞察 | 项目分析 | 项目统计 | 按项目查看质量和变化趋势 | 菜单 |  | [sql/init-full.sql:1420](../../sql/init-full.sql#L1420) |
| 数据洞察 | 成员分析 | 成员统计 | 查看提交、检查和问题统计 | 菜单 |  | [sql/init-full.sql:1422](../../sql/init-full.sql#L1422) |
| 数据洞察 | Token 用量分析 | 模型用量 | 查看模型用量和估算费用 | 菜单 |  | [sql/init-full.sql:1425](../../sql/init-full.sql#L1425) |
| 一级菜单 | 项目接入 | 项目设置 | 连接代码项目，设置检查方式 | 菜单 |  | [sql/init-full.sql:1390](../../sql/init-full.sql#L1390) |
| 项目接入 | 业务系统 | 业务系统 | 管理代码项目所属的业务系统 | 菜单 | 已清楚，保留 | [sql/init-full.sql:1408](../../sql/init-full.sql#L1408) |
| 项目接入 | 访问凭据 | 访问密钥 | 保存连接代码平台所需的密钥 | 菜单 |  | [sql/init-full.sql:1410](../../sql/init-full.sql#L1410) |
| 项目接入 | 代码项目 | 代码项目 | 连接仓库，设置检查范围 | 菜单 | 已清楚，保留 | [sql/init-full.sql:1409](../../sql/init-full.sql#L1409) |
| 一级菜单 | 策略配置 | 检查设置 | 设置检查模板、模型和工具 | 菜单 |  | [sql/init-full.sql:1388](../../sql/init-full.sql#L1388) |
| 策略配置 | 审查模板 | 检查模板 | 设置检查重点和检查要求 | 菜单 |  | [sql/init-full.sql:1414](../../sql/init-full.sql#L1414) |
| 策略配置 | 大模型配置 | 模型设置 | 连接模型，测试是否可用 | 菜单 |  | [sql/init-full.sql:1407](../../sql/init-full.sql#L1407) |
| 策略配置 | 审查引擎 | 检查工具 | 查看服务器上的检查工具 | 菜单 |  | [sql/init-full.sql:1411](../../sql/init-full.sql#L1411) |
| 一级菜单 | 通知管理 | 消息通知 | 设置通知，查看发送记录 | 菜单 |  | [sql/init-full.sql:1389](../../sql/init-full.sql#L1389) |
| 通知管理 | 通知渠道 | 通知设置 | 连接群机器人，测试消息发送 | 菜单 |  | [sql/init-full.sql:1416](../../sql/init-full.sql#L1416) |
| 通知管理 | 投递记录 | 发送记录 | 查看评论和群消息的发送情况 | 菜单 |  | [sql/init-full.sql:1417](../../sql/init-full.sql#L1417) |
| 一级菜单 | 系统管理 | 系统设置 | 管理账号、权限和基础设置 | 菜单 |  | [sql/init-full.sql:1385](../../sql/init-full.sql#L1385) |
| 系统管理 | 用户管理 | 账号管理 | 添加账号，设置账号权限 | 菜单 |  | [sql/init-full.sql:1392](../../sql/init-full.sql#L1392) |
| 系统管理 | 角色管理 | 角色权限 | 设置不同角色能做什么 | 菜单 |  | [sql/init-full.sql:1393](../../sql/init-full.sql#L1393) |
| 系统管理 | 菜单管理 | 菜单设置 | 设置菜单名称和显示方式 | 菜单 |  | [sql/init-full.sql:1394](../../sql/init-full.sql#L1394) |
| 系统管理 | 部门管理 | 部门设置 | 管理部门及上下级关系 | 菜单 |  | [sql/init-full.sql:1395](../../sql/init-full.sql#L1395) |
| 系统管理 | 岗位管理 | 岗位设置 | 管理岗位名称和状态 | 菜单 |  | [sql/init-full.sql:1396](../../sql/init-full.sql#L1396) |
| 系统管理 | 字典管理 | 选项设置 | 管理选项名称及对应值 | 菜单 |  | [sql/init-full.sql:1397](../../sql/init-full.sql#L1397) |
| 系统管理 | 参数设置 | 基础设置 | 调整系统的基础运行设置 | 菜单 |  | [sql/init-full.sql:1398](../../sql/init-full.sql#L1398) |
| 系统管理 | 通知公告 | 站内公告 | 发布通知和公告 | 菜单 |  | [sql/init-full.sql:1399](../../sql/init-full.sql#L1399) |
| 系统管理 | 日志管理 | 使用记录 | 查看操作和登录记录 | 菜单 |  | [sql/init-full.sql:1400](../../sql/init-full.sql#L1400) |
| 日志管理 | 操作日志 | 操作记录 | 查看谁在何时做了哪些操作 | 菜单 |  | [sql/init-full.sql:1426](../../sql/init-full.sql#L1426) |
| 日志管理 | 登录日志 | 登录记录 | 查看登录结果和异常 | 菜单 |  | [sql/init-full.sql:1427](../../sql/init-full.sql#L1427) |
| 系统管理 | 业务审计 | 业务记录 | 追溯权限、配置等业务操作 | 菜单 |  | [sql/init-full.sql:1424](../../sql/init-full.sql#L1424) |
| 一级菜单 | 系统监控 | 运行状态 | 查看任务、服务和运行异常 | 菜单 |  | [sql/init-full.sql:1386](../../sql/init-full.sql#L1386) |
| 系统监控 | 在线用户 | 在线账号 | 查看当前登录的账号 | 菜单 |  | [sql/init-full.sql:1401](../../sql/init-full.sql#L1401) |
| 系统监控 | 定时任务 | 定时任务 | 设置任务的执行时间 | 菜单 | 已清楚，保留 | [sql/init-full.sql:1402](../../sql/init-full.sql#L1402) |
| 系统监控 | 数据监控 | 数据监控 |  | 菜单 | 按用户要求不处理，保留原文案 | [sql/init-full.sql:1403](../../sql/init-full.sql#L1403) |
| 系统监控 | 服务监控 | 服务器 | 查看服务器资源使用情况 | 菜单 |  | [sql/init-full.sql:1404](../../sql/init-full.sql#L1404) |
| 系统监控 | 缓存监控 | 缓存状态 | 查看缓存运行和使用情况 | 菜单 |  | [sql/init-full.sql:1405](../../sql/init-full.sql#L1405) |
| 系统监控 | 缓存列表 | 缓存数据 | 查看和清理缓存数据 | 菜单 |  | [sql/init-full.sql:1406](../../sql/init-full.sql#L1406) |
| 系统监控 | 运行概览 | 检查运行 | 查看检查积压、资源和异常 | 菜单 |  | [sql/init-full.sql:1423](../../sql/init-full.sql#L1423) |

## 页面功能与短文案

同一原文在按钮和标题中的用途不同，分别列出。原文位置可点击核对。

### 代码项目

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 项目名称 | 项目名称 |  | 名称 | [acr-ui/src/views/review/project/index.vue:4](../../acr-ui/src/views/review/project/index.vue#L4) |
| 业务系统 | 业务系统 |  | 名称 | [acr-ui/src/views/review/project/index.vue:7](../../acr-ui/src/views/review/project/index.vue#L7) |
| Git 平台 | 代码平台 | 选择存放代码的平台 | 名称 | [acr-ui/src/views/review/project/index.vue:12](../../acr-ui/src/views/review/project/index.vue#L12) |
| 状态 | 状态 |  | 名称 | [acr-ui/src/views/review/project/index.vue:17](../../acr-ui/src/views/review/project/index.vue#L17) |
| 启用 | 启用 |  | 状态 | [acr-ui/src/views/review/project/index.vue:19](../../acr-ui/src/views/review/project/index.vue#L19) |
| 停用 | 停用 |  | 状态 | [acr-ui/src/views/review/project/index.vue:20](../../acr-ui/src/views/review/project/index.vue#L20) |
| 仓库 | 仓库 |  | 名称 | [acr-ui/src/views/review/project/index.vue:48](../../acr-ui/src/views/review/project/index.vue#L48) |
| 所属部门 | 所属部门 |  | 名称 | [acr-ui/src/views/review/project/index.vue:57](../../acr-ui/src/views/review/project/index.vue#L57) |
| 负责人 | 负责人 |  | 名称 | [acr-ui/src/views/review/project/index.vue:58](../../acr-ui/src/views/review/project/index.vue#L58) |
| 默认分支 | 默认分支 |  | 名称 | [acr-ui/src/views/review/project/index.vue:59](../../acr-ui/src/views/review/project/index.vue#L59) |
| 主要语言 | 主要语言 |  | 名称 | [acr-ui/src/views/review/project/index.vue:60](../../acr-ui/src/views/review/project/index.vue#L60) |
| 审查方式 | 检查方式 | 选择模型检查或工具检查 | 名称 | [acr-ui/src/views/review/project/index.vue:65](../../acr-ui/src/views/review/project/index.vue#L65) |
| 审查类型 | 检查时机 | 选择何时触发代码检查 | 名称 | [acr-ui/src/views/review/project/index.vue:70](../../acr-ui/src/views/review/project/index.vue#L70) |
| 仓库同步 | 仓库同步 |  | 名称 | [acr-ui/src/views/review/project/index.vue:79](../../acr-ui/src/views/review/project/index.vue#L79) |
| 最近检测 | 最近检测 |  | 名称 | [acr-ui/src/views/review/project/index.vue:89](../../acr-ui/src/views/review/project/index.vue#L89) |
| 状态 / 操作 | 状态操作 | 查看状态或操作项目 | 名称 | [acr-ui/src/views/review/project/index.vue:99](../../acr-ui/src/views/review/project/index.vue#L99) |
| 基础信息 | 基础信息 |  | 名称 | [acr-ui/src/views/review/project/index.vue:123](../../acr-ui/src/views/review/project/index.vue#L123) |
| 项目负责人 | 负责人 | 负责这个项目的成员 | 名称 | [acr-ui/src/views/review/project/index.vue:165](../../acr-ui/src/views/review/project/index.vue#L165) |
| 项目状态 | 项目状态 |  | 名称 | [acr-ui/src/views/review/project/index.vue:172](../../acr-ui/src/views/review/project/index.vue#L172) |
| 备注 | 备注 |  | 名称 | [acr-ui/src/views/review/project/index.vue:182](../../acr-ui/src/views/review/project/index.vue#L182) |
| 仓库与分支 | 仓库分支 | 设置代码仓库及分支 | 名称 | [acr-ui/src/views/review/project/index.vue:187](../../acr-ui/src/views/review/project/index.vue#L187) |
| 仓库地址 | 仓库地址 |  | 名称 | [acr-ui/src/views/review/project/index.vue:188](../../acr-ui/src/views/review/project/index.vue#L188) |
| 访问凭据 | 访问密钥 | 选择访问代码仓库的密钥 | 名称 | [acr-ui/src/views/review/project/index.vue:192](../../acr-ui/src/views/review/project/index.vue#L192) |
| 仓库信息 | 仓库信息 |  | 名称 | [acr-ui/src/views/review/project/index.vue:216](../../acr-ui/src/views/review/project/index.vue#L216) |
| 高级设置（系统统一配置） | 统一设置 | 查看平台统一配置 | 名称 | [acr-ui/src/views/review/project/index.vue:226](../../acr-ui/src/views/review/project/index.vue#L226) |
| 来源分支 | 来源分支 |  | 名称 | [acr-ui/src/views/review/project/index.vue:227](../../acr-ui/src/views/review/project/index.vue#L227) |
| 机器人分支 | 自动分支 | 机器人生成分支的名称前缀 | 名称 | [acr-ui/src/views/review/project/index.vue:228](../../acr-ui/src/views/review/project/index.vue#L228) |
| 合并请求触发事件 | 触发事件 | 这些合并请求事件会触发检查 | 名称 | [acr-ui/src/views/review/project/index.vue:231](../../acr-ui/src/views/review/project/index.vue#L231) |
| 说明 | 说明 |  | 名称 | [acr-ui/src/views/review/project/index.vue:241](../../acr-ui/src/views/review/project/index.vue#L241) |
| 回调地址 | 接收地址 | 把此地址填到代码平台通知设置 | 名称 | [acr-ui/src/views/review/project/index.vue:246](../../acr-ui/src/views/review/project/index.vue#L246) |
| Webhook Secret | 验证密钥 | 必须与代码平台设置保持一致 | 名称 | [acr-ui/src/views/review/project/index.vue:256](../../acr-ui/src/views/review/project/index.vue#L256) |
| 最近接收 | 最近接收 |  | 名称 | [acr-ui/src/views/review/project/index.vue:272](../../acr-ui/src/views/review/project/index.vue#L272) |
| 通知 | 通知 |  | 名称 | [acr-ui/src/views/review/project/index.vue:279](../../acr-ui/src/views/review/project/index.vue#L279) |
| 启用通知 | 启用通知 |  | 名称 | [acr-ui/src/views/review/project/index.vue:286](../../acr-ui/src/views/review/project/index.vue#L286) |
| 通知渠道 | 通知渠道 |  | 名称 | [acr-ui/src/views/review/project/index.vue:293](../../acr-ui/src/views/review/project/index.vue#L293) |
| 失败时通知 | 失败通知 | 检查失败时是否发群消息 | 名称 | [acr-ui/src/views/review/project/index.vue:309](../../acr-ui/src/views/review/project/index.vue#L309) |
| 结论通知策略 | 通知条件 | 选择哪些检查结论发群消息 | 名称 | [acr-ui/src/views/review/project/index.vue:316](../../acr-ui/src/views/review/project/index.vue#L316) |
| 仅警告和阻断（推荐） | 仅发风险 | 只发建议修改和高风险 | 名称 | [acr-ui/src/views/review/project/index.vue:319](../../acr-ui/src/views/review/project/index.vue#L319) |
| 仅阻断 | 仅高风险 | 只发高风险结论 | 名称 | [acr-ui/src/views/review/project/index.vue:320](../../acr-ui/src/views/review/project/index.vue#L320) |
| 全部结论 | 全部结论 |  | 名称 | [acr-ui/src/views/review/project/index.vue:321](../../acr-ui/src/views/review/project/index.vue#L321) |
| 低优先级冷却 | 通知间隔 | 间隔内限制普通结论通知次数 | 名称 | [acr-ui/src/views/review/project/index.vue:326](../../acr-ui/src/views/review/project/index.vue#L326) |
| 审查配置 | 检查设置 | 设置检查时机和范围 | 名称 | [acr-ui/src/views/review/project/index.vue:335](../../acr-ui/src/views/review/project/index.vue#L335) |
| 目标分支 | 目标分支 |  | 名称 | [acr-ui/src/views/review/project/index.vue:356](../../acr-ui/src/views/review/project/index.vue#L356) |
| 行内评论 | 代码评论 | 将重点问题写在对应代码行旁 | 名称 | [acr-ui/src/views/review/project/index.vue:378](../../acr-ui/src/views/review/project/index.vue#L378) |
| 严重度 | 风险等级 | 选择发布代码评论的风险等级 | 名称 | [acr-ui/src/views/review/project/index.vue:385](../../acr-ui/src/views/review/project/index.vue#L385) |
| 严重 | 严重 |  | 状态 | [acr-ui/src/views/review/project/index.vue:389](../../acr-ui/src/views/review/project/index.vue#L389) |
| 高 | 高风险 | 风险等级较高 | 状态 | [acr-ui/src/views/review/project/index.vue:390](../../acr-ui/src/views/review/project/index.vue#L390) |
| 中 | 中风险 | 风险等级中等 | 状态 | [acr-ui/src/views/review/project/index.vue:391](../../acr-ui/src/views/review/project/index.vue#L391) |
| 低 | 低风险 | 风险等级较低 | 状态 | [acr-ui/src/views/review/project/index.vue:392](../../acr-ui/src/views/review/project/index.vue#L392) |
| 触发分支 | 触发分支 |  | 名称 | [acr-ui/src/views/review/project/index.vue:399](../../acr-ui/src/views/review/project/index.vue#L399) |
| 排除路径 | 排除路径 |  | 名称 | [acr-ui/src/views/review/project/index.vue:414](../../acr-ui/src/views/review/project/index.vue#L414) |
| 高影响扩展 | 整篇检查 | 重要变更自动检查整个文件 | 名称 | [acr-ui/src/views/review/project/index.vue:421](../../acr-ui/src/views/review/project/index.vue#L421) |
| 审查测试文件 | 检查测试 | 是否检查测试文件的代码变更 | 名称 | [acr-ui/src/views/review/project/index.vue:430](../../acr-ui/src/views/review/project/index.vue#L430) |
| 上报存量问题 | 历史问题 | 是否保留本次变更外的旧问题 | 名称 | [acr-ui/src/views/review/project/index.vue:437](../../acr-ui/src/views/review/project/index.vue#L437) |
| 审查执行 | 检查方式 | 选择检查所用的模型或工具 | 名称 | [acr-ui/src/views/review/project/index.vue:447](../../acr-ui/src/views/review/project/index.vue#L447) |
| 模型配置 | 检查模型 | 选择用于检查的模型 | 名称 | [acr-ui/src/views/review/project/index.vue:471](../../acr-ui/src/views/review/project/index.vue#L471) |
| 审查模板 | 检查模板 | 选择这类代码的检查要求 | 名称 | [acr-ui/src/views/review/project/index.vue:484](../../acr-ui/src/views/review/project/index.vue#L484) |
| 审查引擎 | 检查工具 | 查看服务器上的代码检查工具 | 名称 | [acr-ui/src/views/review/project/index.vue:504](../../acr-ui/src/views/review/project/index.vue#L504) |
| 项目成员权限与原有功能权限、部门数据范围同时生效；负责人固定拥有 OWNER 权限。 | 成员权限同时受功能、部门范围限制。<br>负责人始终拥有项目管理权限。 |  | 说明 | [acr-ui/src/views/review/project/index.vue:540](../../acr-ui/src/views/review/project/index.vue#L540) |
| 成员 | 成员 |  | 名称 | [acr-ui/src/views/review/project/index.vue:542](../../acr-ui/src/views/review/project/index.vue#L542) |
| 项目角色 | 项目角色 |  | 名称 | [acr-ui/src/views/review/project/index.vue:547](../../acr-ui/src/views/review/project/index.vue#L547) |
| 项目管理员 | 管理员 | 管理这个项目 | 名称 | [acr-ui/src/views/review/project/index.vue:549](../../acr-ui/src/views/review/project/index.vue#L549) |
| 审查员 | 检查员 | 在授权范围内处理检查问题 | 名称 | [acr-ui/src/views/review/project/index.vue:550](../../acr-ui/src/views/review/project/index.vue#L550) |
| 只读成员 | 只读成员 |  | 名称 | [acr-ui/src/views/review/project/index.vue:551](../../acr-ui/src/views/review/project/index.vue#L551) |
| 姓名 | 姓名 |  | 名称 | [acr-ui/src/views/review/project/index.vue:559](../../acr-ui/src/views/review/project/index.vue#L559) |
| 部门 | 部门 |  | 名称 | [acr-ui/src/views/review/project/index.vue:560](../../acr-ui/src/views/review/project/index.vue#L560) |
| 操作 | 操作 |  | 名称 | [acr-ui/src/views/review/project/index.vue:564](../../acr-ui/src/views/review/project/index.vue#L564) |
| 仓库分支 | 仓库分支 |  | 名称 | [acr-ui/src/views/review/project/index.vue:575](../../acr-ui/src/views/review/project/index.vue#L575) |
| 分支名称 | 分支名称 |  | 名称 | [acr-ui/src/views/review/project/index.vue:578](../../acr-ui/src/views/review/project/index.vue#L578) |
| 标记 | 标记 |  | 名称 | [acr-ui/src/views/review/project/index.vue:579](../../acr-ui/src/views/review/project/index.vue#L579) |
| 搜索 | 搜索 |  | 名称 | [acr-ui/src/views/review/project/index.vue:24](../../acr-ui/src/views/review/project/index.vue#L24) |
| 重置 | 清空筛选 | 清空当前筛选条件 | 名称 | [acr-ui/src/views/review/project/index.vue:25](../../acr-ui/src/views/review/project/index.vue#L25) |
| 新增 | 新增 |  | 名称 | [acr-ui/src/views/review/project/index.vue:34](../../acr-ui/src/views/review/project/index.vue#L34) |
| 合并请求 | 合并请求 |  | 名称 | [acr-ui/src/views/review/project/index.vue:73](../../acr-ui/src/views/review/project/index.vue#L73) |
| 推送 | 推送 |  | 名称 | [acr-ui/src/views/review/project/index.vue:74](../../acr-ui/src/views/review/project/index.vue#L74) |
| 未配置 | 未配置 |  | 名称 | [acr-ui/src/views/review/project/index.vue:75](../../acr-ui/src/views/review/project/index.vue#L75) |
| 检测 | 连接测试 | 检查当前连接是否可用 | 名称 | [acr-ui/src/views/review/project/index.vue:108](../../acr-ui/src/views/review/project/index.vue#L108) |
| 修改 | 编辑 | 修改当前内容 | 名称 | [acr-ui/src/views/review/project/index.vue:109](../../acr-ui/src/views/review/project/index.vue#L109) |
| 删除 | 删除 |  | 名称 | [acr-ui/src/views/review/project/index.vue:111](../../acr-ui/src/views/review/project/index.vue#L111) |
| 查看接入指引 → | 接入帮助 | 查看代码平台接入方法 | 名称 | [acr-ui/src/views/review/project/index.vue:141](../../acr-ui/src/views/review/project/index.vue#L141) |
| 新增凭据 | 新增密钥 | 创建访问代码平台的密钥 | 名称 | [acr-ui/src/views/review/project/index.vue:200](../../acr-ui/src/views/review/project/index.vue#L200) |
| 刷新列表 | 刷新列表 |  | 名称 | [acr-ui/src/views/review/project/index.vue:202](../../acr-ui/src/views/review/project/index.vue#L202) |
| 完整路径 | 仓库路径 | 查看仓库在平台的完整位置 | 名称 | [acr-ui/src/views/review/project/index.vue:218](../../acr-ui/src/views/review/project/index.vue#L218) |
| 分支数量 | 分支数量 |  | 名称 | [acr-ui/src/views/review/project/index.vue:221](../../acr-ui/src/views/review/project/index.vue#L221) |
| 最近同步 | 最近同步 |  | 名称 | [acr-ui/src/views/review/project/index.vue:222](../../acr-ui/src/views/review/project/index.vue#L222) |
| 全部允许 | 全部允许 |  | 名称 | [acr-ui/src/views/review/project/index.vue:227](../../acr-ui/src/views/review/project/index.vue#L227) |
| 复制 | 复制 |  | 名称 | [acr-ui/src/views/review/project/index.vue:250](../../acr-ui/src/views/review/project/index.vue#L250) |
| 随机生成 | 随机生成 |  | 名称 | [acr-ui/src/views/review/project/index.vue:261](../../acr-ui/src/views/review/project/index.vue#L261) |
| 尚未接收 Webhook 事件。若 GitLab 已配置 Webhook 但此处始终为空，请检查 GitLab 服务器到回调地址的网络连通性（GitLab Admin Area 的「Allow requests to the local network from web hooks」开关 / 防火墙策略） | 还未收到代码平台通知。<br>请检查代码平台能否访问回调地址。 |  | 说明 | [acr-ui/src/views/review/project/index.vue:274](../../acr-ui/src/views/review/project/index.vue#L274) |
| 前往通知渠道 | 通知设置 | 打开群通知渠道设置 | 名称 | [acr-ui/src/views/review/project/index.vue:283](../../acr-ui/src/views/review/project/index.vue#L283) |
| 关闭后审查结束不会向 IM 群发送任何消息。 | 关闭后，检查结束不发群消息。 |  | 说明 | [acr-ui/src/views/review/project/index.vue:290](../../acr-ui/src/views/review/project/index.vue#L290) |
| 审查执行失败时发送简讯；失败通知始终绕过冷却频控。 | 检查失败时发群消息。<br>失败通知不受通知间隔限制。 |  | 说明 | [acr-ui/src/views/review/project/index.vue:313](../../acr-ui/src/views/review/project/index.vue#L313) |
| 控制审查成功后的摘要消息；执行失败由上方开关独立控制。 | 选择检查成功后要通知的结论。<br>失败通知由上方开关单独控制。 |  | 说明 | [acr-ui/src/views/review/project/index.vue:323](../../acr-ui/src/views/review/project/index.vue#L323) |
| 分钟。0 表示不频控；PASS/WARN 在窗口内最多发送一次，BLOCK 与执行失败始终立即通知。被抑制消息仍保留投递记录。 | 单位分钟，0 表示不限次数。<br>通过、建议修改在间隔内只发一次。<br>高风险和失败仍立即通知。<br>未发送的通知仍保留记录。 |  | 说明 | [acr-ui/src/views/review/project/index.vue:330](../../acr-ui/src/views/review/project/index.vue#L330) |
| 合并请求审查 | 合并检查 | 合并请求更新代码时检查 | 名称 | [acr-ui/src/views/review/project/index.vue:340](../../acr-ui/src/views/review/project/index.vue#L340) |
| 在合并请求（PR/MR）创建或更新代码时触发审查，审查结论以合并请求评论发布，问题进入问题台账。适合有合并请求协作流程的团队。 | 合并请求创建或更新代码时检查。<br>结论写入合并请求评论。<br>发现的问题进入问题清单。 |  | 说明 | [acr-ui/src/views/review/project/index.vue:341](../../acr-ui/src/views/review/project/index.vue#L341) |
| 推送审查 | 推送检查 | 直接推送到指定分支后检查 | 名称 | [acr-ui/src/views/review/project/index.vue:344](../../acr-ui/src/views/review/project/index.vue#L344) |
| 代码直接推送到指定分支（不经合并请求）时触发审查，审查结论以通知送达，问题进入问题台账。适合直接向主干推送代码的团队。本审查为推送后审查，不拦截推送。 | 向指定分支推送代码后检查。<br>结论通过通知送达。<br>发现的问题进入问题清单。<br>检查在推送后进行，不拦截推送。 |  | 说明 | [acr-ui/src/views/review/project/index.vue:345](../../acr-ui/src/views/review/project/index.vue#L345) |
| 审查范围 | 检查范围 | 选择检查哪些代码 | 名称 | [acr-ui/src/views/review/project/index.vue:351](../../acr-ui/src/views/review/project/index.vue#L351) |
| 来源分支默认全部允许，通常只选择 dev 或 develop，避免后续合并重复审查。 | 来源分支不设限制。<br>只选需要检查的目标分支。<br>少选目标分支，可减少重复检查。 |  | 说明 | [acr-ui/src/views/review/project/index.vue:363](../../acr-ui/src/views/review/project/index.vue#L363) |
| 查看全部分支 | 全部分支 | 查看仓库中的全部分支 | 名称 | [acr-ui/src/views/review/project/index.vue:367](../../acr-ui/src/views/review/project/index.vue#L367) |
| 刷新分支 | 刷新分支 |  | 名称 | [acr-ui/src/views/review/project/index.vue:371](../../acr-ui/src/views/review/project/index.vue#L371) |
| 将重点问题以行内评论发布到合并请求的代码行上，同一问题全生命周期只发布一次；未发布行内评论的问题仍可在总结评论与问题台账中查看。 | 重点问题写在对应代码行旁。<br>同一问题只发布一次代码评论。<br>其余问题仍可在总结和清单查看。 |  | 说明 | [acr-ui/src/views/review/project/index.vue:382](../../acr-ui/src/views/review/project/index.vue#L382) |
| 仅达到所选严重度的问题会发布行内评论。 | 达到所选风险等级才发布代码评论。 |  | 说明 | [acr-ui/src/views/review/project/index.vue:394](../../acr-ui/src/views/review/project/index.vue#L394) |
| 仅列出的分支上的推送会触发审查，其余分支推送忽略。 | 只有这些分支的推送会触发检查。 |  | 说明 | [acr-ui/src/views/review/project/index.vue:406](../../acr-ui/src/views/review/project/index.vue#L406) |
| 在平台默认排除之上追加，命中的文件不进入审查。留空表示不追加。 | 在默认排除文件之外追加。<br>匹配的文件不检查。<br>留空表示不追加。 |  | 说明 | [acr-ui/src/views/review/project/index.vue:418](../../acr-ui/src/views/review/project/index.vue#L418) |
| 关闭时自动排除 *Test.java、*_test.go、*.test.* 等测试文件的变更。 | 关闭后，不检查测试文件的变更。 |  | 说明 | [acr-ui/src/views/review/project/index.vue:434](../../acr-ui/src/views/review/project/index.vue#L434) |
| 开启后保留变更行之外的历史存量问题并标注来源；默认剔除，审查结果只聚焦本次变更。 | 开启后，保留变更外的历史问题。<br>默认只保留本次变更中的问题。 |  | 说明 | [acr-ui/src/views/review/project/index.vue:441](../../acr-ui/src/views/review/project/index.vue#L441) |
| 用于筛选推荐审查模板；不强制模板必须同技术栈。 | 按主要语言推荐模板。<br>仍可选用其他语言或框架的模板。 |  | 说明 | [acr-ui/src/views/review/project/index.vue:458](../../acr-ui/src/views/review/project/index.vue#L458) |
| 前往模型服务 | 模型设置 | 打开模型设置页面 | 名称 | [acr-ui/src/views/review/project/index.vue:480](../../acr-ui/src/views/review/project/index.vue#L480) |
| 前往审查模板 | 检查模板 | 打开检查模板页面 | 名称 | [acr-ui/src/views/review/project/index.vue:497](../../acr-ui/src/views/review/project/index.vue#L497) |
| 查看审查引擎 | 检查工具 | 打开检查工具页面 | 名称 | [acr-ui/src/views/review/project/index.vue:511](../../acr-ui/src/views/review/project/index.vue#L511) |
| 上一步 | 上一步 |  | 名称 | [acr-ui/src/views/review/project/index.vue:530](../../acr-ui/src/views/review/project/index.vue#L530) |
| 下一步 | 下一步 |  | 名称 | [acr-ui/src/views/review/project/index.vue:531](../../acr-ui/src/views/review/project/index.vue#L531) |
| 保 存 | 保存 | 保存当前填写的内容 | 名称 | [acr-ui/src/views/review/project/index.vue:532](../../acr-ui/src/views/review/project/index.vue#L532) |
| 取 消 | 取消 | 关闭窗口，放弃本次编辑 | 名称 | [acr-ui/src/views/review/project/index.vue:533](../../acr-ui/src/views/review/project/index.vue#L533) |
| 授权 | 授权 |  | 名称 | [acr-ui/src/views/review/project/index.vue:555](../../acr-ui/src/views/review/project/index.vue#L555) |
| 移除 | 移除 |  | 名称 | [acr-ui/src/views/review/project/index.vue:567](../../acr-ui/src/views/review/project/index.vue#L567) |
| 关 闭 | 关闭 | 关闭当前窗口 | 名称 | [acr-ui/src/views/review/project/index.vue:572](../../acr-ui/src/views/review/project/index.vue#L572) |
| 合并目标 | 合并目标 |  | 名称 | [acr-ui/src/views/review/project/index.vue:582](../../acr-ui/src/views/review/project/index.vue#L582) |
| 推送触发 | 推送触发 |  | 名称 | [acr-ui/src/views/review/project/index.vue:583](../../acr-ui/src/views/review/project/index.vue#L583) |
| Webhook | 平台通知 | 接收平台事件，自动创建检查任务 | 名称 | [acr-ui/src/views/review/project/index.vue:239](../../acr-ui/src/views/review/project/index.vue#L239) |
| 读取仓库信息 | 读取仓库 | 读取仓库名称及全部分支 | 名称 | [acr-ui/src/views/review/project/index.vue:205](../../acr-ui/src/views/review/project/index.vue#L205) |
| 仅看同技术栈 | 同类模板 | 只看当前语言或框架的模板 | 名称 | [acr-ui/src/views/review/project/index.vue:494](../../acr-ui/src/views/review/project/index.vue#L494) |
| 显示全部模板 | 全部模板 | 查看所有可用模板 | 名称 | [acr-ui/src/views/review/project/index.vue:494](../../acr-ui/src/views/review/project/index.vue#L494) |
| 新增代码审查项目 | 新增项目 | 接入一个代码仓库 | 名称 | [acr-ui/src/views/review/project/index.vue:945](../../acr-ui/src/views/review/project/index.vue#L945) |
| 修改代码审查项目 | 编辑项目 | 修改当前项目设置 | 名称 | [acr-ui/src/views/review/project/index.vue:979](../../acr-ui/src/views/review/project/index.vue#L979) |
| 新项目必须由可绑定平台凭据的平台角色创建 | 需有绑定访问密钥的权限才能新增。 |  | 说明 | [acr-ui/src/views/review/project/index.vue:31](../../acr-ui/src/views/review/project/index.vue#L31) |
| 项目保存后平台不可更换 | 保存后不能更换代码平台。 |  | 说明 | [acr-ui/src/views/review/project/index.vue:133](../../acr-ui/src/views/review/project/index.vue#L133) |
| 新项目默认停用，保存后可在列表中启用。 | 新项目先停用，保存后在列表启用。 |  | 说明 | [acr-ui/src/views/review/project/index.vue:176](../../acr-ui/src/views/review/project/index.vue#L176) |
| 选择凭据后读取仓库信息，系统将自动获取仓库名称、默认分支和全部分支。 | 选好访问密钥，再点「读取仓库」。<br>自动获取仓库名称和全部分支。 |  | 说明 | [acr-ui/src/views/review/project/index.vue:212](../../acr-ui/src/views/review/project/index.vue#L212) |
| Webhook 用于接收合并请求或 Push 事件并创建审查任务。Secret 加密保存，页面不回显明文。 | 收到合并请求或推送后创建检查任务。<br>验证密钥加密保存，不显示原值。 |  | 说明 | [acr-ui/src/views/review/project/index.vue:243](../../acr-ui/src/views/review/project/index.vue#L243) |
| 审查结束后向 IM 群发送结论摘要（钉钉/企微/飞书机器人）。渠道为平台级资产，项目仅绑定一个已启用渠道。 | 检查结束后向工作群发送结论。<br>每个项目只能选择一个已启用渠道。 |  | 说明 | [acr-ui/src/views/review/project/index.vue:282](../../acr-ui/src/views/review/project/index.vue#L282) |
| 默认仅审查本次变更内容，不扫描整个文件的历史问题；锁文件、依赖目录与构建产物由平台统一排除。以下配置随任务快照冻结，修改仅影响新建任务。 | 默认只检查本次变更。<br>自动排除锁文件、依赖和生成文件。<br>范围设置只影响新任务。 |  | 说明 | [acr-ui/src/views/review/project/index.vue:353](../../acr-ui/src/views/review/project/index.vue#L353) |
| 新增文件、公共方法/接口签名、权限安全逻辑、配置文件、依赖声明、数据库脚本变更时，自动扩展为整文件审查。建议保持启用。 | 新增文件时，检查整个文件。<br>公共方法、权限和配置变更也会扩展。<br>依赖和数据库脚本变更也会扩展。 |  | 说明 | [acr-ui/src/views/review/project/index.vue:426](../../acr-ui/src/views/review/project/index.vue#L426) |
| 审查方式二选一：大模型审查使用模型服务 + 审查模板；审查引擎使用本机 open-code-review，不使用平台模板。 | 模型检查使用模型和模板。<br>工具检查使用服务器上安装的工具。<br>工具检查不使用平台模板。 |  | 说明 | [acr-ui/src/views/review/project/index.vue:450](../../acr-ui/src/views/review/project/index.vue#L450) |
| 必选。建单时冻结模型快照，后续修改不影响已创建任务。 | 必须选择模型。<br>新任务保存当前设置，旧任务不变。 |  | 说明 | [acr-ui/src/views/review/project/index.vue:478](../../acr-ui/src/views/review/project/index.vue#L478) |
| 必选。建单时冻结模板版本与正文；修改模板只影响新任务。 | 必须选择模板。<br>任务保存当前版本，修改只影响新任务。 |  | 说明 | [acr-ui/src/views/review/project/index.vue:492](../../acr-ui/src/views/review/project/index.vue#L492) |
| 当前方式不绑定项目级模型与审查模板。请确保「模型服务」已配置并启用平台默认模型（引擎运行依赖），且「审查引擎」环境检测通过。 | 工具检查使用平台默认模型。<br>请先启用默认模型并通过工具检测。 |  | 说明 | [acr-ui/src/views/review/project/index.vue:514](../../acr-ui/src/views/review/project/index.vue#L514) |
| 留空保持不变，输入则更新 | 留空保留原密钥，填写则更新。 |  | 说明 | [acr-ui/src/views/review/project/index.vue:810](../../acr-ui/src/views/review/project/index.vue#L810) |
| 留空将导致 Webhook 全部验签失败。 | 未设置密钥，平台通知将无法通过验证。 |  | 说明 | [acr-ui/src/views/review/project/index.vue:825](../../acr-ui/src/views/review/project/index.vue#L825) |
| 未选择 | 未选择 | 尚未选择检查分支 | 状态 | [acr-ui/src/views/review/project/index.vue:1347](../../acr-ui/src/views/review/project/index.vue#L1347) |
| 连接正常 | 连接正常 | 可以连接代码仓库 | 状态 | [acr-ui/src/views/review/project/index.vue:1351](../../acr-ui/src/views/review/project/index.vue#L1351) |
| 连接失败 | 连接失败 | 暂时无法连接代码仓库 | 状态 | [acr-ui/src/views/review/project/index.vue:1351](../../acr-ui/src/views/review/project/index.vue#L1351) |
| 未检测 | 未检测 | 还未检测仓库连接 | 状态 | [acr-ui/src/views/review/project/index.vue:91](../../acr-ui/src/views/review/project/index.vue#L91) |
| 同步成功 | 同步成功 | 已读取仓库分支 | 状态 | [acr-ui/src/views/review/project/index.vue:1359](../../acr-ui/src/views/review/project/index.vue#L1359) |
| 同步失败 | 同步失败 | 仓库分支读取失败 | 状态 | [acr-ui/src/views/review/project/index.vue:1359](../../acr-ui/src/views/review/project/index.vue#L1359) |
| 未同步 | 未同步 | 还未读取仓库分支 | 状态 | [acr-ui/src/views/review/project/index.vue:81](../../acr-ui/src/views/review/project/index.vue#L81) |
| 项目启停请在列表中操作。 | 在项目列表中启用或停用。 |  | 说明 | [acr-ui/src/views/review/project/index.vue:176](../../acr-ui/src/views/review/project/index.vue#L176) |
| 合并请求合并到这些分支时，会额外触发一次推送后审查。问题台账自动去重、不会产生重复问题；如希望避免，可在推送审查中移除相应分支。 | 这些分支合并后会再做一次推送检查。<br>问题清单会自动去重。<br>不想再查，请移出推送分支。 |  | 说明 | [acr-ui/src/views/review/project/index.vue:411](../../acr-ui/src/views/review/project/index.vue#L411) |

### 访问密钥

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 凭据名称 | 密钥名称 | 便于识别这组访问密钥 | 名称 | [acr-ui/src/views/review/credential/index.vue:4](../../acr-ui/src/views/review/credential/index.vue#L4) |
| Git 平台 | 代码平台 | 选择存放代码的平台 | 名称 | [acr-ui/src/views/review/credential/index.vue:7](../../acr-ui/src/views/review/credential/index.vue#L7) |
| 状态 | 状态 |  | 名称 | [acr-ui/src/views/review/credential/index.vue:12](../../acr-ui/src/views/review/credential/index.vue#L12) |
| 启用 | 启用 |  | 状态 | [acr-ui/src/views/review/credential/index.vue:14](../../acr-ui/src/views/review/credential/index.vue#L14) |
| 停用 | 停用 |  | 状态 | [acr-ui/src/views/review/credential/index.vue:15](../../acr-ui/src/views/review/credential/index.vue#L15) |
| 服务地址 | 服务地址 |  | 名称 | [acr-ui/src/views/review/credential/index.vue:38](../../acr-ui/src/views/review/credential/index.vue#L38) |
| 认证方式 | 验证方式 | 当前仅支持个人访问密钥 | 名称 | [acr-ui/src/views/review/credential/index.vue:41](../../acr-ui/src/views/review/credential/index.vue#L41) |
| Token | 访问密钥 | 填写代码平台生成的密钥 | 名称 | [acr-ui/src/views/review/credential/index.vue:42](../../acr-ui/src/views/review/credential/index.vue#L42) |
| 使用状态 | 使用情况 | 查看关联项目数量 | 名称 | [acr-ui/src/views/review/credential/index.vue:45](../../acr-ui/src/views/review/credential/index.vue#L45) |
| 最近检测 | 最近检测 |  | 名称 | [acr-ui/src/views/review/credential/index.vue:52](../../acr-ui/src/views/review/credential/index.vue#L52) |
| 修改时间 | 修改时间 |  | 名称 | [acr-ui/src/views/review/credential/index.vue:63](../../acr-ui/src/views/review/credential/index.vue#L63) |
| 操作 | 操作 |  | 名称 | [acr-ui/src/views/review/credential/index.vue:66](../../acr-ui/src/views/review/credential/index.vue#L66) |
| Token 只在提交时发送，编辑页面不会回显历史值。 | 保存时发送密钥。<br>历史密钥不会显示。 |  | 说明 | [acr-ui/src/views/review/credential/index.vue:109](../../acr-ui/src/views/review/credential/index.vue#L109) |
| 备注 | 备注 |  | 名称 | [acr-ui/src/views/review/credential/index.vue:116](../../acr-ui/src/views/review/credential/index.vue#L116) |
| Git 平台用户身份映射 | 账号关联 | 将平台账号关联到本站成员 | 名称 | [acr-ui/src/views/review/credential/index.vue:128](../../acr-ui/src/views/review/credential/index.vue#L128) |
| Git 用户名 | 平台账号 | 代码平台中的账号名称 | 名称 | [acr-ui/src/views/review/credential/index.vue:130](../../acr-ui/src/views/review/credential/index.vue#L130) |
| 绑定系统用户 | 本站成员 | 选择该账号对应的成员 | 名称 | [acr-ui/src/views/review/credential/index.vue:131](../../acr-ui/src/views/review/credential/index.vue#L131) |
| 搜索 | 搜索 |  | 名称 | [acr-ui/src/views/review/credential/index.vue:19](../../acr-ui/src/views/review/credential/index.vue#L19) |
| 重置 | 清空筛选 | 清空当前筛选条件 | 名称 | [acr-ui/src/views/review/credential/index.vue:20](../../acr-ui/src/views/review/credential/index.vue#L20) |
| 新增 | 新增 |  | 名称 | [acr-ui/src/views/review/credential/index.vue:26](../../acr-ui/src/views/review/credential/index.vue#L26) |
| 检测 | 连接测试 | 检查当前连接是否可用 | 名称 | [acr-ui/src/views/review/credential/index.vue:69](../../acr-ui/src/views/review/credential/index.vue#L69) |
| 映射 | 账号关联 | 设置平台账号与本站成员关系 | 名称 | [acr-ui/src/views/review/credential/index.vue:70](../../acr-ui/src/views/review/credential/index.vue#L70) |
| 修改 | 编辑 | 修改当前内容 | 名称 | [acr-ui/src/views/review/credential/index.vue:71](../../acr-ui/src/views/review/credential/index.vue#L71) |
| 删除 | 删除 |  | 名称 | [acr-ui/src/views/review/credential/index.vue:72](../../acr-ui/src/views/review/credential/index.vue#L72) |
| 按平台查看凭据创建指引 → | 密钥帮助 | 查看各平台密钥的获取方法 | 名称 | [acr-ui/src/views/review/credential/index.vue:83](../../acr-ui/src/views/review/credential/index.vue#L83) |
| 确 定 | 保存 | 保存当前填写的内容 | 名称 | [acr-ui/src/views/review/credential/index.vue:122](../../acr-ui/src/views/review/credential/index.vue#L122) |
| 取 消 | 取消 | 关闭窗口，放弃本次编辑 | 名称 | [acr-ui/src/views/review/credential/index.vue:123](../../acr-ui/src/views/review/credential/index.vue#L123) |
| 绑定 | 绑定 |  | 名称 | [acr-ui/src/views/review/credential/index.vue:140](../../acr-ui/src/views/review/credential/index.vue#L140) |
| 仅显示通过 Webhook 自动发现的 Git 平台用户；未映射的按项目负责人兜底指派。 | 仅显示平台通知中发现的账号。<br>未关联账号的问题分给项目负责人。 |  | 说明 | [acr-ui/src/views/review/credential/index.vue:144](../../acr-ui/src/views/review/credential/index.vue#L144) |
| 新增 Git 访问凭据 | 新增密钥 | 创建代码平台的访问密钥 | 名称 | [acr-ui/src/views/review/credential/index.vue:262](../../acr-ui/src/views/review/credential/index.vue#L262) |
| 已配置 | 已配置 | 已保存访问密钥 | 状态 | [acr-ui/src/views/review/credential/index.vue:43](../../acr-ui/src/views/review/credential/index.vue#L43) |
| 未配置 | 未配置 | 尚未保存访问密钥 | 状态 | [acr-ui/src/views/review/credential/index.vue:43](../../acr-ui/src/views/review/credential/index.vue#L43) |
| 未使用 | 未使用 | 尚未被项目使用 | 状态 | [acr-ui/src/views/review/credential/index.vue:48](../../acr-ui/src/views/review/credential/index.vue#L48) |
| 凭据保存后平台不可更换 | 保存后不能更换代码平台。 |  | 说明 | [acr-ui/src/views/review/credential/index.vue:91](../../acr-ui/src/views/review/credential/index.vue#L91) |
| 留空保留原 Token；修改时请重新输入 | 留空保留原密钥，填写则更新。 |  | 说明 | [acr-ui/src/views/review/credential/index.vue:217](../../acr-ui/src/views/review/credential/index.vue#L217) |
| 未映射 | 未关联 | 还未关联到本站成员 | 状态 | [acr-ui/src/views/review/credential/index.vue:133](../../acr-ui/src/views/review/credential/index.vue#L133) |
| 连接正常 | 连接正常 | 密钥可以访问代码平台 | 状态 | [acr-ui/src/views/review/credential/index.vue:335](../../acr-ui/src/views/review/credential/index.vue#L335) |
| 连接失败 | 连接失败 | 密钥暂时无法访问代码平台 | 状态 | [acr-ui/src/views/review/credential/index.vue:335](../../acr-ui/src/views/review/credential/index.vue#L335) |
| 未检测 | 未检测 | 还未检测密钥是否可用 | 状态 | [acr-ui/src/views/review/credential/index.vue:54](../../acr-ui/src/views/review/credential/index.vue#L54) |

### 检查模板

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 模板名称 | 模板名称 |  | 名称 | [acr-ui/src/views/review/template/index.vue:4](../../acr-ui/src/views/review/template/index.vue#L4) |
| 编码 | 模板代号 | 用于区分模板，不能重复 | 名称 | [acr-ui/src/views/review/template/index.vue:7](../../acr-ui/src/views/review/template/index.vue#L7) |
| 技术栈 | 适用技术 | 选择适用的语言或框架 | 名称 | [acr-ui/src/views/review/template/index.vue:10](../../acr-ui/src/views/review/template/index.vue#L10) |
| 状态 | 状态 |  | 名称 | [acr-ui/src/views/review/template/index.vue:15](../../acr-ui/src/views/review/template/index.vue#L15) |
| 启用 | 启用 |  | 状态 | [acr-ui/src/views/review/template/index.vue:17](../../acr-ui/src/views/review/template/index.vue#L17) |
| 停用 | 停用 |  | 状态 | [acr-ui/src/views/review/template/index.vue:18](../../acr-ui/src/views/review/template/index.vue#L18) |
| 名称 | 模板名称 | 便于识别这个模板 | 名称 | [acr-ui/src/views/review/template/index.vue:35](../../acr-ui/src/views/review/template/index.vue#L35) |
| 类型 | 模板来源 | 区分内置和自定义模板 | 名称 | [acr-ui/src/views/review/template/index.vue:42](../../acr-ui/src/views/review/template/index.vue#L42) |
| 版本 | 版本 |  | 名称 | [acr-ui/src/views/review/template/index.vue:49](../../acr-ui/src/views/review/template/index.vue#L49) |
| 更新时间 | 更新时间 |  | 名称 | [acr-ui/src/views/review/template/index.vue:57](../../acr-ui/src/views/review/template/index.vue#L57) |
| 操作 | 操作 |  | 名称 | [acr-ui/src/views/review/template/index.vue:60](../../acr-ui/src/views/review/template/index.vue#L60) |
| 内置模板仅可查看。如需调整，请使用「复制」生成自定义模板后再修改。 | 内置模板只能查看。<br>请先复制，再修改。 |  | 说明 | [acr-ui/src/views/review/template/index.vue:77](../../acr-ui/src/views/review/template/index.vue#L77) |
| 正文 | 检查要求 | 填写这类代码的检查重点 | 名称 | [acr-ui/src/views/review/template/index.vue:126](../../acr-ui/src/views/review/template/index.vue#L126) |
| 备注 | 备注 |  | 名称 | [acr-ui/src/views/review/template/index.vue:131](../../acr-ui/src/views/review/template/index.vue#L131) |
| 搜索 | 搜索 |  | 名称 | [acr-ui/src/views/review/template/index.vue:22](../../acr-ui/src/views/review/template/index.vue#L22) |
| 重置 | 清空筛选 | 清空当前筛选条件 | 名称 | [acr-ui/src/views/review/template/index.vue:23](../../acr-ui/src/views/review/template/index.vue#L23) |
| 新增 | 新增 |  | 名称 | [acr-ui/src/views/review/template/index.vue:29](../../acr-ui/src/views/review/template/index.vue#L29) |
| 查看 | 查看 |  | 名称 | [acr-ui/src/views/review/template/index.vue:62](../../acr-ui/src/views/review/template/index.vue#L62) |
| 复制 | 复制 |  | 名称 | [acr-ui/src/views/review/template/index.vue:63](../../acr-ui/src/views/review/template/index.vue#L63) |
| 修改 | 编辑 | 修改当前内容 | 名称 | [acr-ui/src/views/review/template/index.vue:65](../../acr-ui/src/views/review/template/index.vue#L65) |
| 删除 | 删除 |  | 名称 | [acr-ui/src/views/review/template/index.vue:67](../../acr-ui/src/views/review/template/index.vue#L67) |
| 只需编写技术栈审查要求；上方公共规则由平台执行时自动追加。任务建单时会快照模板正文与版本，后续修改只影响新任务。 | 只需填写这类代码的检查要求。<br>检查时自动加入公共规则。<br>任务创建时保存模板内容和版本。<br>修改模板只影响新任务。 |  | 说明 | [acr-ui/src/views/review/template/index.vue:129](../../acr-ui/src/views/review/template/index.vue#L129) |
| 确 定 | 保存 | 保存当前填写的内容 | 名称 | [acr-ui/src/views/review/template/index.vue:136](../../acr-ui/src/views/review/template/index.vue#L136) |
| 复制为自定义 | 复制模板 | 复制一份可以修改的模板 | 名称 | [acr-ui/src/views/review/template/index.vue:138](../../acr-ui/src/views/review/template/index.vue#L138) |
| 关 闭 | 关闭 | 关闭当前窗口 | 名称 | [acr-ui/src/views/review/template/index.vue:139](../../acr-ui/src/views/review/template/index.vue#L139) |
| 新增审查模板 | 新增模板 | 创建可修改的检查模板 | 名称 | [acr-ui/src/views/review/template/index.vue:237](../../acr-ui/src/views/review/template/index.vue#L237) |
| 查看审查模板 | 查看模板 | 查看模板检查要求 | 名称 | [acr-ui/src/views/review/template/index.vue:245](../../acr-ui/src/views/review/template/index.vue#L245) |
| 修改审查模板 | 编辑模板 | 修改自定义模板检查要求 | 名称 | [acr-ui/src/views/review/template/index.vue:257](../../acr-ui/src/views/review/template/index.vue#L257) |
| 复制审查模板 | 复制模板 | 复制一份可编辑的检查模板 | 名称 | [acr-ui/src/views/review/template/index.vue:279](../../acr-ui/src/views/review/template/index.vue#L279) |
| 平台统一审查规则 | 公共规则 | 检查时自动加入的统一规则 | 名称 | [acr-ui/src/views/review/template/index.vue:83](../../acr-ui/src/views/review/template/index.vue#L83) |
| 内置 | 内置 | 平台自带，只能查看或复制 | 状态 | [acr-ui/src/views/review/template/index.vue:45](../../acr-ui/src/views/review/template/index.vue#L45) |
| 自定义 | 自定义 | 团队创建，可以修改 | 状态 | [acr-ui/src/views/review/template/index.vue:45](../../acr-ui/src/views/review/template/index.vue#L45) |
| 英文/下划线，唯一 | 使用英文或下划线，不能重复。 |  | 说明 | [acr-ui/src/views/review/template/index.vue:108](../../acr-ui/src/views/review/template/index.vue#L108) |

### 检查工具

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 引擎名称 | 工具名称 | 服务器上使用的检查工具 | 名称 | [acr-ui/src/views/review/engine/index.vue:17](../../acr-ui/src/views/review/engine/index.vue#L17) |
| 引擎类型 | 工具类型 | 查看检查工具的类型 | 名称 | [acr-ui/src/views/review/engine/index.vue:18](../../acr-ui/src/views/review/engine/index.vue#L18) |
| 可执行文件 | 程序路径 | 服务器上检查工具的位置 | 名称 | [acr-ui/src/views/review/engine/index.vue:19](../../acr-ui/src/views/review/engine/index.vue#L19) |
| 配置来源 | 配置来源 |  | 名称 | [acr-ui/src/views/review/engine/index.vue:20](../../acr-ui/src/views/review/engine/index.vue#L20) |
| 当前版本 | 当前版本 |  | 名称 | [acr-ui/src/views/review/engine/index.vue:21](../../acr-ui/src/views/review/engine/index.vue#L21) |
| 可用状态 | 可用状态 |  | 名称 | [acr-ui/src/views/review/engine/index.vue:22](../../acr-ui/src/views/review/engine/index.vue#L22) |
| 工作目录根路径 | 工作目录 | 临时代码文件的存放位置 | 名称 | [acr-ui/src/views/review/engine/index.vue:27](../../acr-ui/src/views/review/engine/index.vue#L27) |
| 默认超时 | 等待上限 | 超过此秒数后结束等待 | 名称 | [acr-ui/src/views/review/engine/index.vue:28](../../acr-ui/src/views/review/engine/index.vue#L28) |
| 最大并发 | 同时运行 | 最多同时执行的检查数 | 名称 | [acr-ui/src/views/review/engine/index.vue:29](../../acr-ui/src/views/review/engine/index.vue#L29) |
| 输出上限 | 输出上限 |  | 名称 | [acr-ui/src/views/review/engine/index.vue:30](../../acr-ui/src/views/review/engine/index.vue#L30) |
| 最近检测 | 最近检测 |  | 名称 | [acr-ui/src/views/review/engine/index.vue:31](../../acr-ui/src/views/review/engine/index.vue#L31) |
| 最近测试 | 最近测试 |  | 名称 | [acr-ui/src/views/review/engine/index.vue:40](../../acr-ui/src/views/review/engine/index.vue#L40) |
| 是否成功 | 测试结果 | 查看最近一次调用是否成功 | 名称 | [acr-ui/src/views/review/engine/index.vue:57](../../acr-ui/src/views/review/engine/index.vue#L57) |
| 执行耗时 | 用时 | 本次工具运行花费的时间 | 名称 | [acr-ui/src/views/review/engine/index.vue:62](../../acr-ui/src/views/review/engine/index.vue#L62) |
| 引擎版本 | 工具版本 | 本次调用使用的工具版本 | 名称 | [acr-ui/src/views/review/engine/index.vue:63](../../acr-ui/src/views/review/engine/index.vue#L63) |
| 退出码 | 结束代码 | 工具退出时返回的数字 | 名称 | [acr-ui/src/views/review/engine/index.vue:64](../../acr-ui/src/views/review/engine/index.vue#L64) |
| 失败类型 | 失败类型 |  | 名称 | [acr-ui/src/views/review/engine/index.vue:65](../../acr-ui/src/views/review/engine/index.vue#L65) |
| 失败原因 | 失败原因 |  | 名称 | [acr-ui/src/views/review/engine/index.vue:66](../../acr-ui/src/views/review/engine/index.vue#L66) |
| 标准输出 (stdout) | 运行输出 | 查看工具输出的运行内容 | 名称 | [acr-ui/src/views/review/engine/index.vue:69](../../acr-ui/src/views/review/engine/index.vue#L69) |
| 标准错误 (stderr) | 错误输出 | 查看工具写入错误通道的内容 | 名称 | [acr-ui/src/views/review/engine/index.vue:72](../../acr-ui/src/views/review/engine/index.vue#L72) |
| 结构化结果 | 解析结果 | 查看工具返回的分项结果 | 名称 | [acr-ui/src/views/review/engine/index.vue:75](../../acr-ui/src/views/review/engine/index.vue#L75) |
| 测试调用 | 试运行 | 用样例代码测试检查工具 | 名称 | [acr-ui/src/views/review/engine/index.vue:81](../../acr-ui/src/views/review/engine/index.vue#L81) |
| 模型配置 | 检查模型 | 选择用于检查的模型 | 名称 | [acr-ui/src/views/review/engine/index.vue:83](../../acr-ui/src/views/review/engine/index.vue#L83) |
| 审查引擎 | 检查工具 | 查看服务器上的代码检查工具 | 名称 | [acr-ui/src/views/review/engine/index.vue:6](../../acr-ui/src/views/review/engine/index.vue#L6) |
| 环境检测 | 环境检测 |  | 名称 | [acr-ui/src/views/review/engine/index.vue:9](../../acr-ui/src/views/review/engine/index.vue#L9) |
| 最近一次调用结果 | 测试结果 | 查看最近一次试运行结果 | 名称 | [acr-ui/src/views/review/engine/index.vue:54](../../acr-ui/src/views/review/engine/index.vue#L54) |
| 取消 | 取消 |  | 名称 | [acr-ui/src/views/review/engine/index.vue:98](../../acr-ui/src/views/review/engine/index.vue#L98) |
| 开始测试 | 开始测试 |  | 名称 | [acr-ui/src/views/review/engine/index.vue:99](../../acr-ui/src/views/review/engine/index.vue#L99) |
| 将使用所选模型配置调用审查引擎；未选择时使用当前默认模型。测试使用内置样例仓库，不会读取用户代码。 | 用所选模型试运行检查工具。<br>不选模型时，使用平台默认模型。<br>测试仅使用样例代码，不读项目代码。 |  | 说明 | [acr-ui/src/views/review/engine/index.vue:91](../../acr-ui/src/views/review/engine/index.vue#L91) |
| 暂无已启用的模型配置，请先在「大模型配置」中新增并启用。 | 暂无可用模型。<br>请在「模型设置」中新增并启用。 |  | 说明 | [acr-ui/src/views/review/engine/index.vue:91](../../acr-ui/src/views/review/engine/index.vue#L91) |
| 可用 | 可用 | 工具可以运行 | 状态 | [acr-ui/src/views/review/engine/index.vue:22](../../acr-ui/src/views/review/engine/index.vue#L22) |
| 不可用 | 不可用 | 工具暂时无法运行 | 状态 | [acr-ui/src/views/review/engine/index.vue:199](../../acr-ui/src/views/review/engine/index.vue#L199) |
| 未知 | 状态未知 | 当前可用状态尚不能确定 | 状态 | [acr-ui/src/views/review/engine/index.vue:200](../../acr-ui/src/views/review/engine/index.vue#L200) |
| 未测试 | 未测试 | 还未用样例代码试运行 | 状态 | [acr-ui/src/views/review/engine/index.vue:43](../../acr-ui/src/views/review/engine/index.vue#L43) |
| 未检测 | 未检测 | 还未检测工具运行环境 | 状态 | [acr-ui/src/views/review/engine/index.vue:21](../../acr-ui/src/views/review/engine/index.vue#L21) |
| (empty) | 暂无输出 | 工具输出为空时的显示文案 | 状态 | [acr-ui/src/views/review/engine/index.vue:70](../../acr-ui/src/views/review/engine/index.vue#L70) |

### 模型设置

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 配置名称 | 配置名称 |  | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:4](../../acr-ui/src/views/system/aimodelconfig/index.vue#L4) |
| 服务厂商 | 模型厂商 | 选择提供模型服务的厂商 | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:13](../../acr-ui/src/views/system/aimodelconfig/index.vue#L13) |
| 状态 | 状态 |  | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:23](../../acr-ui/src/views/system/aimodelconfig/index.vue#L23) |
| 启用 | 启用 |  | 状态 | [acr-ui/src/views/system/aimodelconfig/index.vue:25](../../acr-ui/src/views/system/aimodelconfig/index.vue#L25) |
| 禁用 | 停用 | 暂停使用这组模型设置 | 状态 | [acr-ui/src/views/system/aimodelconfig/index.vue:26](../../acr-ui/src/views/system/aimodelconfig/index.vue#L26) |
| 模型标识 | 模型代号 | 按厂商提供的模型名称填写 | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:58](../../acr-ui/src/views/system/aimodelconfig/index.vue#L58) |
| 启用状态 | 启用状态 |  | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:59](../../acr-ui/src/views/system/aimodelconfig/index.vue#L59) |
| 默认状态 | 默认模型 | 查看是否为平台默认模型 | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:70](../../acr-ui/src/views/system/aimodelconfig/index.vue#L70) |
| 最近检测结果 | 检测结果 | 查看最近一次测试结果 | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:76](../../acr-ui/src/views/system/aimodelconfig/index.vue#L76) |
| 最近检测时间 | 检测时间 | 查看最近一次测试时间 | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:77](../../acr-ui/src/views/system/aimodelconfig/index.vue#L77) |
| 操作 | 操作 |  | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:82](../../acr-ui/src/views/system/aimodelconfig/index.vue#L82) |
| 厂商名称 | 厂商名称 |  | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:122](../../acr-ui/src/views/system/aimodelconfig/index.vue#L122) |
| API 地址 | 服务地址 | 填写模型服务的调用地址 | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:125](../../acr-ui/src/views/system/aimodelconfig/index.vue#L125) |
| API Key | 访问密钥 | 填写模型厂商提供的密钥 | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:131](../../acr-ui/src/views/system/aimodelconfig/index.vue#L131) |
| 是否启用 | 启用状态 | 是否允许使用这组模型设置 | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:136](../../acr-ui/src/views/system/aimodelconfig/index.vue#L136) |
| 是否默认 | 默认模型 | 是否作为平台默认模型 | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:144](../../acr-ui/src/views/system/aimodelconfig/index.vue#L144) |
| 高级设置 | 高级设置 |  | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:154](../../acr-ui/src/views/system/aimodelconfig/index.vue#L154) |
| 超时(ms) | 等待上限 | 单位毫秒，限制模型等待时间 | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:157](../../acr-ui/src/views/system/aimodelconfig/index.vue#L157) |
| 最大 Token | 输出上限 | 限制模型最多输出的内容量 | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:162](../../acr-ui/src/views/system/aimodelconfig/index.vue#L162) |
| Temperature | 随机程度 | 越高，回答变化通常越多 | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:167](../../acr-ui/src/views/system/aimodelconfig/index.vue#L167) |
| 上下文长度 | 容量上限 | 模型一次能容纳的内容量 | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:172](../../acr-ui/src/views/system/aimodelconfig/index.vue#L172) |
| 输入单价 | 输入单价 |  | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:177](../../acr-ui/src/views/system/aimodelconfig/index.vue#L177) |
| 输出单价 | 输出单价 |  | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:182](../../acr-ui/src/views/system/aimodelconfig/index.vue#L182) |
| 排序 | 排序 |  | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:187](../../acr-ui/src/views/system/aimodelconfig/index.vue#L187) |
| 备注 | 备注 |  | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:192](../../acr-ui/src/views/system/aimodelconfig/index.vue#L192) |
| 搜索 | 搜索 |  | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:30](../../acr-ui/src/views/system/aimodelconfig/index.vue#L30) |
| 重置 | 清空筛选 | 清空当前筛选条件 | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:31](../../acr-ui/src/views/system/aimodelconfig/index.vue#L31) |
| 新增 | 新增 |  | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:43](../../acr-ui/src/views/system/aimodelconfig/index.vue#L43) |
| 默认 | 默认 |  | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:72](../../acr-ui/src/views/system/aimodelconfig/index.vue#L72) |
| 设为默认 | 设为默认 |  | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:73](../../acr-ui/src/views/system/aimodelconfig/index.vue#L73) |
| 修改 | 编辑 | 修改当前内容 | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:84](../../acr-ui/src/views/system/aimodelconfig/index.vue#L84) |
| 删除 | 删除 |  | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:85](../../acr-ui/src/views/system/aimodelconfig/index.vue#L85) |
| 查看模型配置指引 → | 模型帮助 | 查看模型设置方法 | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:101](../../acr-ui/src/views/system/aimodelconfig/index.vue#L101) |
| 基础配置 | 基础设置 | 填写模型连接信息 | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:103](../../acr-ui/src/views/system/aimodelconfig/index.vue#L103) |
| 是 | 设为默认 | 使用此模型作为平台默认 | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:146](../../acr-ui/src/views/system/aimodelconfig/index.vue#L146) |
| 否 | 非默认 | 不设为平台默认模型 | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:147](../../acr-ui/src/views/system/aimodelconfig/index.vue#L147) |
| 连接测试会发起一次最小模型请求，可能产生少量 Token；模型调用测试用于验证实际内容输出。 | 连接测试会调用模型，可能产生费用。<br>回答测试检查模型能否正常回答。 |  | 说明 | [acr-ui/src/views/system/aimodelconfig/index.vue:209](../../acr-ui/src/views/system/aimodelconfig/index.vue#L209) |
| 连接测试 | 连接测试 |  | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:213](../../acr-ui/src/views/system/aimodelconfig/index.vue#L213) |
| 模型调用测试 | 回答测试 | 检查模型是否能正常返回回答 | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:214](../../acr-ui/src/views/system/aimodelconfig/index.vue#L214) |
| 确 定 | 保存 | 保存当前填写的内容 | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:215](../../acr-ui/src/views/system/aimodelconfig/index.vue#L215) |
| 取 消 | 取消 | 关闭窗口，放弃本次编辑 | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:216](../../acr-ui/src/views/system/aimodelconfig/index.vue#L216) |
| 新增大模型配置 | 新增模型 | 创建一组模型连接设置 | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:367](../../acr-ui/src/views/system/aimodelconfig/index.vue#L367) |
| 修改大模型配置 | 编辑模型 | 修改当前模型设置 | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:379](../../acr-ui/src/views/system/aimodelconfig/index.vue#L379) |
| 其他/自定义 | 其他厂商 | 手动填写厂商名称和连接信息 | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:283](../../acr-ui/src/views/system/aimodelconfig/index.vue#L283) |
| 时延 | 用时 | 模型返回结果花费的时间 | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:202](../../acr-ui/src/views/system/aimodelconfig/index.vue#L202) |
| 错误类型 | 错误类型 | 查看连接或调用失败的类型 | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:203](../../acr-ui/src/views/system/aimodelconfig/index.vue#L203) |
| 响应 | 模型回答 | 查看模型实际返回的内容 | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:205](../../acr-ui/src/views/system/aimodelconfig/index.vue#L205) |
| 摘要 | 返回摘要 | 查看失败请求返回的内容片段 | 名称 | [acr-ui/src/views/system/aimodelconfig/index.vue:206](../../acr-ui/src/views/system/aimodelconfig/index.vue#L206) |
| 留空则不修改 | 留空保留原密钥，填写则更新。 |  | 说明 | [acr-ui/src/views/system/aimodelconfig/index.vue:132](../../acr-ui/src/views/system/aimodelconfig/index.vue#L132) |

### 业务系统

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 系统名称 | 系统名称 |  | 名称 | [acr-ui/src/views/system/businesssystem/index.vue:4](../../acr-ui/src/views/system/businesssystem/index.vue#L4) |
| 系统编码 | 系统代号 | 用于区分系统，不能重复 | 名称 | [acr-ui/src/views/system/businesssystem/index.vue:13](../../acr-ui/src/views/system/businesssystem/index.vue#L13) |
| 状态 | 状态 |  | 名称 | [acr-ui/src/views/system/businesssystem/index.vue:22](../../acr-ui/src/views/system/businesssystem/index.vue#L22) |
| 正常 | 启用 | 允许使用这个业务系统 | 状态 | [acr-ui/src/views/system/businesssystem/index.vue:24](../../acr-ui/src/views/system/businesssystem/index.vue#L24) |
| 停用 | 停用 |  | 状态 | [acr-ui/src/views/system/businesssystem/index.vue:25](../../acr-ui/src/views/system/businesssystem/index.vue#L25) |
| 所属部门 | 所属部门 |  | 名称 | [acr-ui/src/views/system/businesssystem/index.vue:50](../../acr-ui/src/views/system/businesssystem/index.vue#L50) |
| 管理用户 | 管理员 | 管理这个业务系统的成员 | 名称 | [acr-ui/src/views/system/businesssystem/index.vue:51](../../acr-ui/src/views/system/businesssystem/index.vue#L51) |
| 备注 | 备注 |  | 名称 | [acr-ui/src/views/system/businesssystem/index.vue:57](../../acr-ui/src/views/system/businesssystem/index.vue#L57) |
| 操作 | 操作 |  | 名称 | [acr-ui/src/views/system/businesssystem/index.vue:58](../../acr-ui/src/views/system/businesssystem/index.vue#L58) |
| 搜索 | 搜索 |  | 名称 | [acr-ui/src/views/system/businesssystem/index.vue:29](../../acr-ui/src/views/system/businesssystem/index.vue#L29) |
| 重置 | 清空筛选 | 清空当前筛选条件 | 名称 | [acr-ui/src/views/system/businesssystem/index.vue:30](../../acr-ui/src/views/system/businesssystem/index.vue#L30) |
| 新增 | 新增 |  | 名称 | [acr-ui/src/views/system/businesssystem/index.vue:42](../../acr-ui/src/views/system/businesssystem/index.vue#L42) |
| 修改 | 编辑 | 修改当前内容 | 名称 | [acr-ui/src/views/system/businesssystem/index.vue:60](../../acr-ui/src/views/system/businesssystem/index.vue#L60) |
| 删除 | 删除 |  | 名称 | [acr-ui/src/views/system/businesssystem/index.vue:61](../../acr-ui/src/views/system/businesssystem/index.vue#L61) |
| 确 定 | 保存 | 保存当前填写的内容 | 名称 | [acr-ui/src/views/system/businesssystem/index.vue:129](../../acr-ui/src/views/system/businesssystem/index.vue#L129) |
| 取 消 | 取消 | 关闭窗口，放弃本次编辑 | 名称 | [acr-ui/src/views/system/businesssystem/index.vue:130](../../acr-ui/src/views/system/businesssystem/index.vue#L130) |
| 新增业务系统 | 新增系统 | 登记一个业务系统 | 名称 | [acr-ui/src/views/system/businesssystem/index.vue:239](../../acr-ui/src/views/system/businesssystem/index.vue#L239) |
| 修改业务系统 | 编辑系统 | 修改当前业务系统 | 名称 | [acr-ui/src/views/system/businesssystem/index.vue:255](../../acr-ui/src/views/system/businesssystem/index.vue#L255) |

### 检查任务

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 审查任务是执行队列：重点关注待执行、执行中与失败重试。已完成结果请到「审查记录」查看。 | 跟踪检查进度，结果见「检查记录」 | 说明任务页用途 | 提示 | [acr-ui/src/views/review/task/index.vue:8](../../acr-ui/src/views/review/task/index.vue#L8) |
| 所属项目 | 所属项目 | 按项目查找任务 | 字段 | [acr-ui/src/views/review/task/index.vue:12](../../acr-ui/src/views/review/task/index.vue#L12)<br>[acr-ui/src/views/review/task/index.vue:56](../../acr-ui/src/views/review/task/index.vue#L56) |
| 类型 | 触发方式 | 区分合并请求与代码推送 | 字段 | [acr-ui/src/views/review/task/index.vue:17](../../acr-ui/src/views/review/task/index.vue#L17)<br>[acr-ui/src/views/review/task/index.vue:57](../../acr-ui/src/views/review/task/index.vue#L57) |
| 任务状态 | 检查状态 | 查看任务是否完成 | 字段 | [acr-ui/src/views/review/task/index.vue:22](../../acr-ui/src/views/review/task/index.vue#L22)<br>[acr-ui/src/views/review/task/index.vue:79](../../acr-ui/src/views/review/task/index.vue#L79) |
| 显示范围 | 查看范围 | 切换当前任务与全部任务 | 字段 | [acr-ui/src/views/review/task/index.vue:27](../../acr-ui/src/views/review/task/index.vue#L27) |
| 执行队列 | 进行任务 | 待检查、检查中及失败任务 | 选项 | [acr-ui/src/views/review/task/index.vue:29](../../acr-ui/src/views/review/task/index.vue#L29) |
| 全部任务 | 全部任务 | 包含已完成的任务 | 选项 | [acr-ui/src/views/review/task/index.vue:30](../../acr-ui/src/views/review/task/index.vue#L30) |
| 合并请求编号 | 请求编号 | 输入代码平台的合并请求编号 | 字段 | [acr-ui/src/views/review/task/index.vue:34](../../acr-ui/src/views/review/task/index.vue#L34)<br>[acr-ui/src/views/review/task/index.vue:35](../../acr-ui/src/views/review/task/index.vue#L35) |
| 创建时间 | 创建时间 | 按任务创建时间查找 | 字段 | [acr-ui/src/views/review/task/index.vue:37](../../acr-ui/src/views/review/task/index.vue#L37)<br>[acr-ui/src/views/review/task/index.vue:111](../../acr-ui/src/views/review/task/index.vue#L111) |
| 变更来源 | 代码来源 | 查看触发检查的代码变更 | 字段 | [acr-ui/src/views/review/task/index.vue:62](../../acr-ui/src/views/review/task/index.vue#L62) |
| 分支 | 代码分支 | 查看来源与目标分支 | 字段 | [acr-ui/src/views/review/task/index.vue:74](../../acr-ui/src/views/review/task/index.vue#L74) |
| 当前步骤 | 当前进度 | 查看检查正在做什么 | 字段 | [acr-ui/src/views/review/task/index.vue:94](../../acr-ui/src/views/review/task/index.vue#L94) |
| 执行次数 | 检查次数 | 包含首次检查与再次检查 | 字段 | [acr-ui/src/views/review/task/index.vue:100](../../acr-ui/src/views/review/task/index.vue#L100) |
| 失败原因 | 失败原因 | 查看检查失败的原因 | 字段 | [acr-ui/src/views/review/task/index.vue:103](../../acr-ui/src/views/review/task/index.vue#L103) |
| 操作 | 操作 | 选择任务操作 | 字段 | [acr-ui/src/views/review/task/index.vue:114](../../acr-ui/src/views/review/task/index.vue#L114) |
| 搜索 | 查找 | 按已选条件查找 | 动作 | [acr-ui/src/views/review/task/index.vue:43](../../acr-ui/src/views/review/task/index.vue#L43) |
| 重置 | 清空筛选 | 恢复默认查询条件 | 动作 | [acr-ui/src/views/review/task/index.vue:44](../../acr-ui/src/views/review/task/index.vue#L44) |
| 收起筛选 | 收起筛选 | 收起更多查找条件 | 动作 | [acr-ui/src/views/review/task/index.vue:46](../../acr-ui/src/views/review/task/index.vue#L46) |
| 更多筛选 | 更多筛选 | 展开更多查找条件 | 动作 | [acr-ui/src/views/review/task/index.vue:46](../../acr-ui/src/views/review/task/index.vue#L46) |
| 详情 | 查看详情 | 查看进度与失败原因 | 动作 | [acr-ui/src/views/review/task/index.vue:118](../../acr-ui/src/views/review/task/index.vue#L118) |
| 执行 | 开始检查 | 手动启动待检查任务 | 动作 | [acr-ui/src/views/review/task/index.vue:220](../../acr-ui/src/views/review/task/index.vue#L220) |
| 重试 | 重新检查 | 重新检查并保留历史记录 | 动作 | [acr-ui/src/views/review/task/index.vue:220](../../acr-ui/src/views/review/task/index.vue#L220) |
| 终止 | 停止检查 | 取消任务并停止后续检查 | 动作 | [acr-ui/src/views/review/task/index.vue:130](../../acr-ui/src/views/review/task/index.vue#L130) |
| 暂无审查任务 | 暂无检查任务 | 无符合条件的任务 | 提示 | [acr-ui/src/views/review/task/index.vue:55](../../acr-ui/src/views/review/task/index.vue#L55) |
| 查看任务执行概览与排障信息 | 查看检查进度与失败原因 | 说明详情入口 | 提示 | [acr-ui/src/views/review/task/index.vue:232](../../acr-ui/src/views/review/task/index.vue#L232) |
| 任务已失败，可重试；历史执行记录会保留。 | 可重新检查，历史记录保留 | 失败任务可再次检查 | 提示 | [acr-ui/src/views/review/task/index.vue:240](../../acr-ui/src/views/review/task/index.vue#L240) |
| 任务尚未开始。若长时间仍为待执行，可点「执行」手动触发。 | 尚未开始，可点「开始检查」 | 允许手动启动 | 提示 | [acr-ui/src/views/review/task/index.vue:241](../../acr-ui/src/views/review/task/index.vue#L241) |
| 待重试，可手动触发。 | 等待重试，也可手动开始 | 保留自动与手动方式 | 提示 | [acr-ui/src/views/review/task/index.vue:245](../../acr-ui/src/views/review/task/index.vue#L245) |
| 已完成任务不可再次执行。结果请到「审查记录」查看。 | 已完成，结果见「检查记录」 | 完成状态不代表通过 | 提示 | [acr-ui/src/views/review/task/index.vue:250](../../acr-ui/src/views/review/task/index.vue#L250) |
| 任务已取消，不可再执行。 | 已取消，无法再次检查 | 保留取消后限制 | 提示 | [acr-ui/src/views/review/task/index.vue:251](../../acr-ui/src/views/review/task/index.vue#L251) |
| 已被更新任务替代，本任务仅保留历史，不可再执行。 | 已有更新任务，本条仅保留历史 | 说明旧任务限制 | 提示 | [acr-ui/src/views/review/task/index.vue:256](../../acr-ui/src/views/review/task/index.vue#L256) |
| 当前账号没有「任务终止」权限（review:task:cancel）。 | 没有停止权限，请联系管理员 | 去掉权限代码 | 提示 | [acr-ui/src/views/review/task/index.vue:263](../../acr-ui/src/views/review/task/index.vue#L263) |
| 终止后状态变为「已取消」，进行中的旧执行写入将被围栏拒绝。 | 停止后取消任务，旧检查结果不再写入 | 保留停止后的结果处理 | 提示 | [acr-ui/src/views/review/task/index.vue:267](../../acr-ui/src/views/review/task/index.vue#L267) |
| 确认立即执行该待执行任务？ | 现在开始检查？ | 确认立即启动 | 提示 | [acr-ui/src/views/review/task/index.vue:288](../../acr-ui/src/views/review/task/index.vue#L288) |
| 仅当本次执行已中断（超过 30 分钟）时才可回收重试，确认继续？ | 仅可重试已中断超30分钟的检查，继续？ | 保留中断与时间条件 | 提示 | [acr-ui/src/views/review/task/index.vue:290](../../acr-ui/src/views/review/task/index.vue#L290) |
| 确认重新执行该任务？历史执行记录将保留。 | 重新检查？历史记录会保留 | 确认重试且保留历史 | 提示 | [acr-ui/src/views/review/task/index.vue:291](../../acr-ui/src/views/review/task/index.vue#L291) |
| 确认终止该任务？终止后状态变为「已取消」，且不会再被调度领取。 | 停止检查？任务取消后不再运行 | 确认停止及后续限制 | 提示 | [acr-ui/src/views/review/task/index.vue:304](../../acr-ui/src/views/review/task/index.vue#L304) |
| 已提交执行 | 已安排检查 | 表示已安排而非已完成 | 反馈 | [acr-ui/src/views/review/task/index.vue:292](../../acr-ui/src/views/review/task/index.vue#L292) |
| 已提交重试 | 已安排重试 | 表示已安排而非已完成 | 反馈 | [acr-ui/src/views/review/task/index.vue:292](../../acr-ui/src/views/review/task/index.vue#L292) |
| 已终止 | 已停止 | 说明任务已取消 | 反馈 | [acr-ui/src/views/review/task/index.vue:307](../../acr-ui/src/views/review/task/index.vue#L307) |

### 任务详情

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 审查任务详情 | 任务详情 | 查看单次检查任务 | 标题 | [acr-ui/src/views/review/task/detail.vue:4](../../acr-ui/src/views/review/task/detail.vue#L4) |
| 查看审查记录 | 检查记录 | 查看评分与问题 | 动作 | [acr-ui/src/views/review/task/detail.vue:8](../../acr-ui/src/views/review/task/detail.vue#L8) |
| 任务概览 | 任务概况 | 汇总任务基本信息 | 分区 | [acr-ui/src/views/review/task/detail.vue:29](../../acr-ui/src/views/review/task/detail.vue#L29) |
| 任务 ID | 任务编号 | 当前任务的唯一编号 | 字段 | [acr-ui/src/views/review/task/detail.vue:31](../../acr-ui/src/views/review/task/detail.vue#L31)<br>[acr-ui/src/views/review/task/detail.vue:173](../../acr-ui/src/views/review/task/detail.vue#L173) |
| 项目 | 所属项目 | 查看任务所属项目 | 字段 | [acr-ui/src/views/review/task/detail.vue:32](../../acr-ui/src/views/review/task/detail.vue#L32) |
| 事件来源 | 触发方式 | 区分合并请求与代码推送 | 字段 | [acr-ui/src/views/review/task/detail.vue:33](../../acr-ui/src/views/review/task/detail.vue#L33) |
| 合并请求 | 合并请求 | 查看触发检查的合并请求 | 字段 | [acr-ui/src/views/review/task/detail.vue:36](../../acr-ui/src/views/review/task/detail.vue#L36) |
| 推送引用 | 推送位置 | 查看推送分支与对应提交 | 字段 | [acr-ui/src/views/review/task/detail.vue:39](../../acr-ui/src/views/review/task/detail.vue#L39) |
| 发起人 | 发起人 | 发起代码变更的人 | 字段 | [acr-ui/src/views/review/task/detail.vue:43](../../acr-ui/src/views/review/task/detail.vue#L43) |
| 分支 | 代码分支 | 查看来源与目标分支 | 字段 | [acr-ui/src/views/review/task/detail.vue:44](../../acr-ui/src/views/review/task/detail.vue#L44) |
| 代码变更 | 代码变更 | 查看文件与增删行数 | 字段 | [acr-ui/src/views/review/task/detail.vue:45](../../acr-ui/src/views/review/task/detail.vue#L45) |
| 任务状态 | 检查状态 | 查看任务运行情况 | 字段 | [acr-ui/src/views/review/task/detail.vue:46](../../acr-ui/src/views/review/task/detail.vue#L46) |
| 当前步骤 | 当前进度 | 查看检查当前步骤 | 字段 | [acr-ui/src/views/review/task/detail.vue:59](../../acr-ui/src/views/review/task/detail.vue#L59) |
| 进度提示 | 进度说明 | 说明进度如何更新 | 字段 | [acr-ui/src/views/review/task/detail.vue:63](../../acr-ui/src/views/review/task/detail.vue#L63) |
| 审查进行中，页面每 5 秒自动刷新进度；LLM 审查可能耗时数十秒，请耐心等待 | 每5秒更新进度；模型检查可能较慢 | 保留刷新周期与等待提示 | 提示 | [acr-ui/src/views/review/task/detail.vue:64](../../acr-ui/src/views/review/task/detail.vue#L64) |
| 执行次数 | 检查次数 | 包含首次检查与再次检查 | 字段 | [acr-ui/src/views/review/task/detail.vue:66](../../acr-ui/src/views/review/task/detail.vue#L66) |
| 耗时 | 检查用时 | 本次检查所用时间 | 字段 | [acr-ui/src/views/review/task/detail.vue:67](../../acr-ui/src/views/review/task/detail.vue#L67)<br>[acr-ui/src/views/review/task/detail.vue:122](../../acr-ui/src/views/review/task/detail.vue#L122) |
| 审查方式 | 检查方式 | 查看使用模型或检查工具 | 字段 | [acr-ui/src/views/review/task/detail.vue:68](../../acr-ui/src/views/review/task/detail.vue#L68) |
| 创建时间 | 创建时间 | 查看任务创建时间 | 字段 | [acr-ui/src/views/review/task/detail.vue:72](../../acr-ui/src/views/review/task/detail.vue#L72) |
| 失败分类 | 失败类型 | 查看哪类环节出错 | 字段 | [acr-ui/src/views/review/task/detail.vue:73](../../acr-ui/src/views/review/task/detail.vue#L73) |
| 失败原因 | 失败原因 | 查看具体失败原因 | 字段 | [acr-ui/src/views/review/task/detail.vue:77](../../acr-ui/src/views/review/task/detail.vue#L77)<br>[acr-ui/src/views/review/task/detail.vue:125](../../acr-ui/src/views/review/task/detail.vue#L125) |
| 执行记录（排障） | 运行记录 | 查看每次检查的运行情况 | 分区 | [acr-ui/src/views/review/task/detail.vue:95](../../acr-ui/src/views/review/task/detail.vue#L95) |
| 仅展示每次 attempt 的技术信息。完整评分与重点问题请到审查记录查看。 | 这里只显示运行情况，评分见检查记录 | 区分运行过程与结果 | 提示 | [acr-ui/src/views/review/task/detail.vue:96](../../acr-ui/src/views/review/task/detail.vue#L96) |
| 暂无执行记录（待执行任务开始后会出现） | 检查开始后会显示运行记录 | 说明记录产生时机 | 提示 | [acr-ui/src/views/review/task/detail.vue:97](../../acr-ui/src/views/review/task/detail.vue#L97) |
| 范围决策快照 | 检查范围 | 查看本次采用的文件范围 | 分区 | [acr-ui/src/views/review/task/detail.vue:102](../../acr-ui/src/views/review/task/detail.vue#L102) |
| 次数 | 检查次数 | 查看第几次检查 | 字段 | [acr-ui/src/views/review/task/detail.vue:107](../../acr-ui/src/views/review/task/detail.vue#L107) |
| 状态 | 运行状态 | 查看每次运行是否成功 | 字段 | [acr-ui/src/views/review/task/detail.vue:108](../../acr-ui/src/views/review/task/detail.vue#L108) |
| 模型 / 引擎 | 检查工具 | 查看所用模型或工具 | 字段 | [acr-ui/src/views/review/task/detail.vue:113](../../acr-ui/src/views/review/task/detail.vue#L113) |
| 模板 | 检查模板 | 查看本次采用的检查模板 | 字段 | [acr-ui/src/views/review/task/detail.vue:116](../../acr-ui/src/views/review/task/detail.vue#L116) |
| 缺少任务 ID | 缺少任务编号 | 说明无法加载的原因 | 提示 | [acr-ui/src/views/review/task/detail.vue:173](../../acr-ui/src/views/review/task/detail.vue#L173) |
| 未获取到任务详情 | 未找到任务详情 | 说明没有返回详情 | 提示 | [acr-ui/src/views/review/task/detail.vue:183](../../acr-ui/src/views/review/task/detail.vue#L183) |
| 详情加载失败 | 详情未能加载 | 说明加载失败 | 提示 | [acr-ui/src/views/review/task/detail.vue:186](../../acr-ui/src/views/review/task/detail.vue#L186) |
| 当前账号没有执行/重试权限 | 没有检查权限，请联系管理员 | 说明无权开始或重试 | 提示 | [acr-ui/src/views/review/task/detail.vue:245](../../acr-ui/src/views/review/task/detail.vue#L245) |
| 可重试，历史执行记录会保留 | 可重新检查，历史记录保留 | 保留历史说明 | 提示 | [acr-ui/src/views/review/task/detail.vue:247](../../acr-ui/src/views/review/task/detail.vue#L247) |
| 可手动触发执行 | 可点「开始检查」立即开始 | 说明手动触发 | 提示 | [acr-ui/src/views/review/task/detail.vue:248](../../acr-ui/src/views/review/task/detail.vue#L248) |
| 执行中；中断超过 30 分钟可回收重试 | 检查中；中断超30分钟可重试 | 保留中断与时间条件 | 提示 | [acr-ui/src/views/review/task/detail.vue:254](../../acr-ui/src/views/review/task/detail.vue#L254) |
| 当前状态不可执行 | 当前状态无法开始检查 | 不扩大允许执行状态 | 提示 | [acr-ui/src/views/review/task/detail.vue:263](../../acr-ui/src/views/review/task/detail.vue#L263) |
| 当前账号没有任务终止权限 | 没有停止权限，请联系管理员 | 说明无权停止 | 提示 | [acr-ui/src/views/review/task/detail.vue:268](../../acr-ui/src/views/review/task/detail.vue#L268) |
| 终止后变为「已取消」，旧执行写入将被围栏拒绝 | 停止后取消任务，旧检查结果不再写入 | 保留旧结果限制 | 提示 | [acr-ui/src/views/review/task/detail.vue:269](../../acr-ui/src/views/review/task/detail.vue#L269) |

### 检查记录

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 所属项目 | 所属项目 | 按项目查找结果 | 字段 | [acr-ui/src/views/review/record/index.vue:4](../../acr-ui/src/views/review/record/index.vue#L4) |
| 变更来源 | 代码来源 | 区分合并请求与代码推送 | 字段 | [acr-ui/src/views/review/record/index.vue:9](../../acr-ui/src/views/review/record/index.vue#L9)<br>[acr-ui/src/views/review/record/index.vue:53](../../acr-ui/src/views/review/record/index.vue#L53) |
| 审查结论 | 检查结论 | 查看通过、需修改或高风险 | 字段 | [acr-ui/src/views/review/record/index.vue:14](../../acr-ui/src/views/review/record/index.vue#L14)<br>[acr-ui/src/views/review/record/index.vue:102](../../acr-ui/src/views/review/record/index.vue#L102) |
| 变更编号 | 请求编号 | 按合并请求编号查找 | 字段 | [acr-ui/src/views/review/record/index.vue:20](../../acr-ui/src/views/review/record/index.vue#L20) |
| 发起人 | 发起人 | 按代码变更发起人查找 | 字段 | [acr-ui/src/views/review/record/index.vue:23](../../acr-ui/src/views/review/record/index.vue#L23)<br>[acr-ui/src/views/review/record/index.vue:146](../../acr-ui/src/views/review/record/index.vue#L146) |
| Git 账号 | 代码平台账号 | 查找发起人时填写 | 提示 | [acr-ui/src/views/review/record/index.vue:24](../../acr-ui/src/views/review/record/index.vue#L24) |
| 完成时间 | 完成时间 | 按检查完成时间查找 | 字段 | [acr-ui/src/views/review/record/index.vue:26](../../acr-ui/src/views/review/record/index.vue#L26)<br>[acr-ui/src/views/review/record/index.vue:143](../../acr-ui/src/views/review/record/index.vue#L143) |
| 暂无审查记录 | 暂无检查记录 | 无符合条件的结果 | 提示 | [acr-ui/src/views/review/record/index.vue:44](../../acr-ui/src/views/review/record/index.vue#L44) |
| 项目名称 | 所属项目 | 查看记录所属项目 | 字段 | [acr-ui/src/views/review/record/index.vue:45](../../acr-ui/src/views/review/record/index.vue#L45) |
| 代码变更 | 代码变更 | 查看文件与增删行数 | 字段 | [acr-ui/src/views/review/record/index.vue:81](../../acr-ui/src/views/review/record/index.vue#L81) |
| 评分 | 总分 | 查看检查总分 | 字段 | [acr-ui/src/views/review/record/index.vue:110](../../acr-ui/src/views/review/record/index.vue#L110) |
| 重点问题 | 重点问题 | 查看最重要的三个新问题 | 字段 | [acr-ui/src/views/review/record/index.vue:115](../../acr-ui/src/views/review/record/index.vue#L115) |
| 基于结构化 Top 3 重点问题分级统计，非全量问题 | 仅统计前三个重点问题 | 避免当作全部问题数 | 提示 | [acr-ui/src/views/review/record/index.vue:118](../../acr-ui/src/views/review/record/index.vue#L118) |
| 严重 | 严重风险 | 最高风险等级 | 状态 | [acr-ui/src/views/review/record/index.vue:120](../../acr-ui/src/views/review/record/index.vue#L120) |
| 高 | 高风险 | 高风险等级 | 状态 | [acr-ui/src/views/review/record/index.vue:124](../../acr-ui/src/views/review/record/index.vue#L124) |
| 中 | 中风险 | 中风险等级 | 状态 | [acr-ui/src/views/review/record/index.vue:128](../../acr-ui/src/views/review/record/index.vue#L128) |
| 低 | 低风险 | 低风险等级 | 状态 | [acr-ui/src/views/review/record/index.vue:132](../../acr-ui/src/views/review/record/index.vue#L132) |
| 暂无问题 | 未发现问题 | 本次结果未显示问题 | 提示 | [acr-ui/src/views/review/record/index.vue:139](../../acr-ui/src/views/review/record/index.vue#L139) |
| 查看详情 | 查看详情 | 查看评分与检查摘要 | 动作 | [acr-ui/src/views/review/record/index.vue:159](../../acr-ui/src/views/review/record/index.vue#L159) |
| 查看问题 | 查看问题 | 查看此次结果中的问题 | 动作 | [acr-ui/src/views/review/record/index.vue:166](../../acr-ui/src/views/review/record/index.vue#L166) |
| 重新执行 | 重新检查 | 重新运行失败任务 | 动作 | [acr-ui/src/views/review/record/index.vue:178](../../acr-ui/src/views/review/record/index.vue#L178) |
| 通过 | 通过 | 检查结论为通过 | 状态 | [acr-ui/src/views/review/record/index.vue:204](../../acr-ui/src/views/review/record/index.vue#L204) |
| 建议修改 | 建议修改 | 建议修改后再提交 | 状态 | [acr-ui/src/views/review/record/index.vue:205](../../acr-ui/src/views/review/record/index.vue#L205) |
| 高风险 | 高风险 | 不表示平台已阻止合并 | 状态 | [acr-ui/src/views/review/record/index.vue:206](../../acr-ui/src/views/review/record/index.vue#L206) |
| 执行失败 | 检查失败 | 检查未能产出完整结果 | 状态 | [acr-ui/src/views/review/record/index.vue:207](../../acr-ui/src/views/review/record/index.vue#L207) |
| 暂无法生成来源链接 | 暂时无法打开代码来源 | 说明外链不可用 | 提示 | [acr-ui/src/views/review/record/index.vue:276](../../acr-ui/src/views/review/record/index.vue#L276) |
| 确认重新执行该失败任务？历史执行记录将保留。 | 重新检查？历史记录会保留 | 确认再次检查失败任务 | 提示 | [acr-ui/src/views/review/record/index.vue:283](../../acr-ui/src/views/review/record/index.vue#L283) |
| 已提交重新执行 | 已安排重试 | 表示已安排而非已完成 | 反馈 | [acr-ui/src/views/review/record/index.vue:286](../../acr-ui/src/views/review/record/index.vue#L286) |
| 暂无数据 | 暂无数据 | 没有可显示的数据 | 状态 | [acr-ui/src/views/review/record/index.vue:299](../../acr-ui/src/views/review/record/index.vue#L299)<br>[acr-ui/src/views/review/record/index.vue:307](../../acr-ui/src/views/review/record/index.vue#L307)<br>[acr-ui/src/views/review/record/index.vue:319](../../acr-ui/src/views/review/record/index.vue#L319)<br>[acr-ui/src/views/review/record/index.vue:328](../../acr-ui/src/views/review/record/index.vue#L328) |
| 未生成 | 未生成 | 失败后未能产出该结果 | 状态 | [acr-ui/src/views/review/record/index.vue:319](../../acr-ui/src/views/review/record/index.vue#L319)<br>[acr-ui/src/views/review/record/index.vue:327](../../acr-ui/src/views/review/record/index.vue#L327) |

### 记录详情

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 审查记录详情 | 记录详情 | 查看一次检查的结果 | 标题 | [acr-ui/src/views/review/record/detail.vue:4](../../acr-ui/src/views/review/record/detail.vue#L4) |
| 重新执行 | 重新检查 | 重新运行失败任务 | 动作 | [acr-ui/src/views/review/record/detail.vue:12](../../acr-ui/src/views/review/record/detail.vue#L12) |
| 项目 | 所属项目 | 查看记录所属项目 | 字段 | [acr-ui/src/views/review/record/detail.vue:22](../../acr-ui/src/views/review/record/detail.vue#L22) |
| 变更来源 | 代码来源 | 查看触发检查的代码来源 | 字段 | [acr-ui/src/views/review/record/detail.vue:26](../../acr-ui/src/views/review/record/detail.vue#L26) |
| 来源详情 | 变更详情 | 查看合并请求或推送详情 | 字段 | [acr-ui/src/views/review/record/detail.vue:30](../../acr-ui/src/views/review/record/detail.vue#L30)<br>[acr-ui/src/views/review/record/detail.vue:40](../../acr-ui/src/views/review/record/detail.vue#L40) |
| 分支 | 代码分支 | 查看来源与目标分支 | 字段 | [acr-ui/src/views/review/record/detail.vue:44](../../acr-ui/src/views/review/record/detail.vue#L44) |
| 文件 | 文件数 | 本次代码变更的文件数 | 字段 | [acr-ui/src/views/review/record/detail.vue:48](../../acr-ui/src/views/review/record/detail.vue#L48) |
| 新增 | 新增行数 | 本次新增的代码行数 | 字段 | [acr-ui/src/views/review/record/detail.vue:53](../../acr-ui/src/views/review/record/detail.vue#L53) |
| 删除 | 删除行数 | 本次删除的代码行数 | 字段 | [acr-ui/src/views/review/record/detail.vue:58](../../acr-ui/src/views/review/record/detail.vue#L58) |
| 审查结论 | 检查结论 | 查看检查给出的结论 | 字段 | [acr-ui/src/views/review/record/detail.vue:64](../../acr-ui/src/views/review/record/detail.vue#L64) |
| 审查结果 | 检查结果 | 查看总分与重点问题 | 分区 | [acr-ui/src/views/review/record/detail.vue:86](../../acr-ui/src/views/review/record/detail.vue#L86) |
| 总分 | 总分 | 五项评分的总分 | 字段 | [acr-ui/src/views/review/record/detail.vue:92](../../acr-ui/src/views/review/record/detail.vue#L92) |
| 审查摘要 | 检查摘要 | 查看结果概要 | 分区 | [acr-ui/src/views/review/record/detail.vue:111](../../acr-ui/src/views/review/record/detail.vue#L111) |
| 暂无审查摘要 | 暂无检查摘要 | 本次没有返回摘要 | 提示 | [acr-ui/src/views/review/record/detail.vue:112](../../acr-ui/src/views/review/record/detail.vue#L112) |
| Token 用量 | 模型用量 | 查看输入、输出与估算费用 | 分区 | [acr-ui/src/views/review/record/detail.vue:116](../../acr-ui/src/views/review/record/detail.vue#L116)<br>[acr-ui/src/views/review/record/detail.vue:198](../../acr-ui/src/views/review/record/detail.vue#L198) |
| 重点问题 | 重点问题 | 查看本次重点新问题 | 分区 | [acr-ui/src/views/review/record/detail.vue:121](../../acr-ui/src/views/review/record/detail.vue#L121)<br>[acr-ui/src/views/review/record/detail.vue:202](../../acr-ui/src/views/review/record/detail.vue#L202) |
| 暂无重点问题 | 暂无重点问题 | 本次没有重点新问题 | 提示 | [acr-ui/src/views/review/record/detail.vue:122](../../acr-ui/src/views/review/record/detail.vue#L122) |
| 未编号 | 未编号 | 问题没有排序编号 | 状态 | [acr-ui/src/views/review/record/detail.vue:126](../../acr-ui/src/views/review/record/detail.vue#L126) |
| 台账 | 问题跟进 | 打开该问题的跟进详情 | 动作 | [acr-ui/src/views/review/record/detail.vue:137](../../acr-ui/src/views/review/record/detail.vue#L137)<br>[acr-ui/src/views/review/record/detail.vue:174](../../acr-ui/src/views/review/record/detail.vue#L174) |
| 建议 | 修复建议 | 查看模型建议的修改方式 | 字段 | [acr-ui/src/views/review/record/detail.vue:145](../../acr-ui/src/views/review/record/detail.vue#L145)<br>[acr-ui/src/views/review/record/detail.vue:182](../../acr-ui/src/views/review/record/detail.vue#L182) |
| 存量 | 已有问题 | 本次变更前已存在的问题 | 状态 | [acr-ui/src/views/review/record/detail.vue:169](../../acr-ui/src/views/review/record/detail.vue#L169) |
| 提交说明 | 提交说明 | 查看本次代码提交说明 | 分区 | [acr-ui/src/views/review/record/detail.vue:190](../../acr-ui/src/views/review/record/detail.vue#L190) |
| 暂无提交说明 | 暂无提交说明 | 没有返回提交说明 | 提示 | [acr-ui/src/views/review/record/detail.vue:191](../../acr-ui/src/views/review/record/detail.vue#L191) |
| 暂无可用的审查结果 | 暂无可查看的检查结果 | 结果尚未生成或不可用 | 提示 | [acr-ui/src/views/review/record/detail.vue:195](../../acr-ui/src/views/review/record/detail.vue#L195) |
| 执行失败，评分与重点问题未生成。 | 检查失败，未生成评分与重点问题 | 保留没有生成结果的事实 | 提示 | [acr-ui/src/views/review/record/detail.vue:203](../../acr-ui/src/views/review/record/detail.vue#L203) |
| 执行记录 | 运行记录 | 查看每次检查的运行情况 | 分区 | [acr-ui/src/views/review/record/detail.vue:208](../../acr-ui/src/views/review/record/detail.vue#L208) |
| 技术排障信息：状态、模型/引擎、模板、SHA、耗时、范围决策与失败原因。 | 查看检查过程、所用配置与失败原因 | 说明运行记录用途 | 提示 | [acr-ui/src/views/review/record/detail.vue:209](../../acr-ui/src/views/review/record/detail.vue#L209) |
| 暂无执行记录 | 暂无运行记录 | 没有可查看的运行记录 | 提示 | [acr-ui/src/views/review/record/detail.vue:210](../../acr-ui/src/views/review/record/detail.vue#L210) |
| 范围决策快照 | 检查范围 | 查看本次采用的文件范围 | 分区 | [acr-ui/src/views/review/record/detail.vue:215](../../acr-ui/src/views/review/record/detail.vue#L215) |
| 模型 / 引擎 | 检查工具 | 查看所用模型或检查工具 | 字段 | [acr-ui/src/views/review/record/detail.vue:226](../../acr-ui/src/views/review/record/detail.vue#L226) |
| 模板 | 检查模板 | 查看采用的检查模板 | 字段 | [acr-ui/src/views/review/record/detail.vue:229](../../acr-ui/src/views/review/record/detail.vue#L229) |
| 未评分 | 未评分 | 没有返回评分 | 状态 | [acr-ui/src/views/review/record/detail.vue:332](../../acr-ui/src/views/review/record/detail.vue#L332) |
| 缺少记录 ID | 缺少记录编号 | 说明无法加载原因 | 提示 | [acr-ui/src/views/review/record/detail.vue:369](../../acr-ui/src/views/review/record/detail.vue#L369) |
| 未获取到审查记录 | 未找到检查记录 | 说明未返回结果 | 提示 | [acr-ui/src/views/review/record/detail.vue:379](../../acr-ui/src/views/review/record/detail.vue#L379) |

### 问题跟进

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 审查记录 | 检查记录 | 按检查记录编号查找问题 | 字段 | [acr-ui/src/views/review/issue/index.vue:9](../../acr-ui/src/views/review/issue/index.vue#L9) |
| 合并请求 | 合并请求 | 按合并请求编号查找 | 字段 | [acr-ui/src/views/review/issue/index.vue:12](../../acr-ui/src/views/review/issue/index.vue#L12) |
| Git 用户名 | 代码平台账号 | 填写代码平台用户名 | 提示 | [acr-ui/src/views/review/issue/index.vue:16](../../acr-ui/src/views/review/issue/index.vue#L16) |
| 源/目标分支 | 来源或目标分支 | 按任一分支查找 | 提示 | [acr-ui/src/views/review/issue/index.vue:19](../../acr-ui/src/views/review/issue/index.vue#L19) |
| 问题状态 | 处理状态 | 查看问题当前处理进度 | 字段 | [acr-ui/src/views/review/issue/index.vue:21](../../acr-ui/src/views/review/issue/index.vue#L21) |
| 严重度 | 风险等级 | 按风险高低查找 | 字段 | [acr-ui/src/views/review/issue/index.vue:32](../../acr-ui/src/views/review/issue/index.vue#L32)<br>[acr-ui/src/views/review/issue/index.vue:218](../../acr-ui/src/views/review/issue/index.vue#L218) |
| 归属 | 问题来源 | 区分新增与已有问题 | 字段 | [acr-ui/src/views/review/issue/index.vue:37](../../acr-ui/src/views/review/issue/index.vue#L37)<br>[acr-ui/src/views/review/issue/index.vue:280](../../acr-ui/src/views/review/issue/index.vue#L280) |
| 责任人 | 负责人 | 查看谁负责跟进问题 | 字段 | [acr-ui/src/views/review/issue/index.vue:42](../../acr-ui/src/views/review/issue/index.vue#L42)<br>[acr-ui/src/views/review/issue/index.vue:285](../../acr-ui/src/views/review/issue/index.vue#L285) |
| 指派给我 | 由我负责 | 仅看分配给自己的问题 | 选项 | [acr-ui/src/views/review/issue/index.vue:44](../../acr-ui/src/views/review/issue/index.vue#L44) |
| 未指派 | 未分配 | 尚未分配负责人 | 状态 | [acr-ui/src/views/review/issue/index.vue:45](../../acr-ui/src/views/review/issue/index.vue#L45)<br>[acr-ui/src/views/review/issue/index.vue:288](../../acr-ui/src/views/review/issue/index.vue#L288) |
| 逾期 | 已超期 | 查看超出处理期限的问题 | 字段 | [acr-ui/src/views/review/issue/index.vue:48](../../acr-ui/src/views/review/issue/index.vue#L48)<br>[acr-ui/src/views/review/issue/index.vue:50](../../acr-ui/src/views/review/issue/index.vue#L50)<br>[acr-ui/src/views/review/issue/index.vue:289](../../acr-ui/src/views/review/issue/index.vue#L289) |
| 关键词 | 关键词 | 按标题或文件查找 | 字段 | [acr-ui/src/views/review/issue/index.vue:53](../../acr-ui/src/views/review/issue/index.vue#L53) |
| 标题或文件路径 | 问题标题或文件位置 | 输入关键词查找 | 提示 | [acr-ui/src/views/review/issue/index.vue:54](../../acr-ui/src/views/review/issue/index.vue#L54) |
| 发现时间 | 发现时间 | 按首次发现时间查找 | 字段 | [acr-ui/src/views/review/issue/index.vue:56](../../acr-ui/src/views/review/issue/index.vue#L56) |
| 查看审查记录 | 检查记录 | 打开关联检查记录 | 动作 | [acr-ui/src/views/review/issue/index.vue:95](../../acr-ui/src/views/review/issue/index.vue#L95) |
| 打开合并请求 | 合并请求 | 打开代码平台中的请求 | 动作 | [acr-ui/src/views/review/issue/index.vue:96](../../acr-ui/src/views/review/issue/index.vue#L96) |
| 退出记录视图 | 全部记录 | 退出当前记录筛选 | 动作 | [acr-ui/src/views/review/issue/index.vue:97](../../acr-ui/src/views/review/issue/index.vue#L97) |
| 当前活跃 | 待处理 | 查看尚未结束跟进的问题 | 选项 | [acr-ui/src/views/review/issue/index.vue:111](../../acr-ui/src/views/review/issue/index.vue#L111) |
| 全部 | 全部问题 | 包含已关闭、忽略与误报 | 选项 | [acr-ui/src/views/review/issue/index.vue:43](../../acr-ui/src/views/review/issue/index.vue#L43)<br>[acr-ui/src/views/review/issue/index.vue:49](../../acr-ui/src/views/review/issue/index.vue#L49)<br>[acr-ui/src/views/review/issue/index.vue:112](../../acr-ui/src/views/review/issue/index.vue#L112) |
| 问题状态总览 | 处理概况 | 查看各处理阶段的问题数 | 分区 | [acr-ui/src/views/review/issue/index.vue:114](../../acr-ui/src/views/review/issue/index.vue#L114) |
| 待确认 | 待确认 | 等待判断问题是否需要修复 | 状态 | [acr-ui/src/views/review/issue/index.vue:121](../../acr-ui/src/views/review/issue/index.vue#L121) |
| 待修复 | 待修复 | 已确认需要修改代码 | 状态 | [acr-ui/src/views/review/issue/index.vue:131](../../acr-ui/src/views/review/issue/index.vue#L131) |
| 疑似修复 | 待复核 | 疑似修复，仍需人工确认 | 状态 | [acr-ui/src/views/review/issue/index.vue:141](../../acr-ui/src/views/review/issue/index.vue#L141)<br>[acr-ui/src/views/review/issue/index.vue:481](../../acr-ui/src/views/review/issue/index.vue#L481) |
| 待确认 + 疑似修复，需要人工判断或处置 | 包含待确认与待复核的问题 | 说明人工待办数量口径 | 提示 | [acr-ui/src/views/review/issue/index.vue:151](../../acr-ui/src/views/review/issue/index.vue#L151) |
| 待人工处置 | 待判断 | 需要人工确认或复核的问题 | 字段 | [acr-ui/src/views/review/issue/index.vue:152](../../acr-ui/src/views/review/issue/index.vue#L152) |
| 累计已关闭 | 累计关闭 | 全部已结束跟进的问题 | 字段 | [acr-ui/src/views/review/issue/index.vue:163](../../acr-ui/src/views/review/issue/index.vue#L163) |
| 暂无问题记录 | 暂无问题 | 无符合条件的问题 | 提示 | [acr-ui/src/views/review/issue/index.vue:181](../../acr-ui/src/views/review/issue/index.vue#L181) |
| 项目 / 业务系统 | 所属项目 | 同时显示项目与所属系统 | 字段 | [acr-ui/src/views/review/issue/index.vue:185](../../acr-ui/src/views/review/issue/index.vue#L185) |
| 变更来源 | 代码来源 | 查看合并请求或代码推送 | 字段 | [acr-ui/src/views/review/issue/index.vue:193](../../acr-ui/src/views/review/issue/index.vue#L193) |
| 问题 | 问题内容 | 查看问题标题与位置 | 字段 | [acr-ui/src/views/review/issue/index.vue:210](../../acr-ui/src/views/review/issue/index.vue#L210) |
| 阶段 | 处理进度 | 查看问题阶段与停留时间 | 字段 | [acr-ui/src/views/review/issue/index.vue:226](../../acr-ui/src/views/review/issue/index.vue#L226) |
| 待人工确认 | 请人工复核 | 不表示问题已经修复 | 提示 | [acr-ui/src/views/review/issue/index.vue:230](../../acr-ui/src/views/review/issue/index.vue#L230) |
| 审查轨迹 | 检查历史 | 查看问题各次检查情况 | 字段 | [acr-ui/src/views/review/issue/index.vue:237](../../acr-ui/src/views/review/issue/index.vue#L237) |
| 时间 | 相关时间 | 查看发现与最后变更时间 | 字段 | [acr-ui/src/views/review/issue/index.vue:293](../../acr-ui/src/views/review/issue/index.vue#L293) |
| 详情 | 查看详情 | 查看问题与处理历史 | 动作 | [acr-ui/src/views/review/issue/index.vue:309](../../acr-ui/src/views/review/issue/index.vue#L309) |
| 复核 | 复核 | 人工确认修复情况 | 动作 | [acr-ui/src/views/review/issue/index.vue:316](../../acr-ui/src/views/review/issue/index.vue#L316) |
| 严重 | 严重风险 | 最高风险等级 | 状态 | [acr-ui/src/views/review/issue/index.vue:363](../../acr-ui/src/views/review/issue/index.vue#L363) |
| 高 | 高风险 | 高风险等级 | 状态 | [acr-ui/src/views/review/issue/index.vue:364](../../acr-ui/src/views/review/issue/index.vue#L364) |
| 中 | 中风险 | 中风险等级 | 状态 | [acr-ui/src/views/review/issue/index.vue:365](../../acr-ui/src/views/review/issue/index.vue#L365) |
| 低 | 低风险 | 低风险等级 | 状态 | [acr-ui/src/views/review/issue/index.vue:366](../../acr-ui/src/views/review/issue/index.vue#L366) |
| 信息 | 提示信息 | 不映射为低风险 | 状态 | [acr-ui/src/views/review/issue/index.vue:367](../../acr-ui/src/views/review/issue/index.vue#L367) |
| 审查记录编号应为正整数 | 检查记录编号须为正整数 | 保留输入编号限制 | 提示 | [acr-ui/src/views/review/issue/index.vue:537](../../acr-ui/src/views/review/issue/index.vue#L537) |
| 最近命中 | 最近发现 | 最近一次检查仍发现该问题 | 字段 | [acr-ui/src/views/review/issue/index.vue:487](../../acr-ui/src/views/review/issue/index.vue#L487) |

### 问题详情

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 问题详情 | 问题详情 | 查看问题与处理情况 | 标题 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:13](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L13) |
| 问题生命周期 | 处理进度 | 从发现到结束的处理过程 | 分区 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:21](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L21)<br>[acr-ui/src/views/review/issue/IssueDetailDrawer.vue:514](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L514) |
| 业务系统 | 所属系统 | 查看项目所属业务系统 | 字段 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:56](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L56) |
| 最近命中提交 | 最近提交 | 最近发现问题对应的代码提交 | 字段 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:65](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L65) |
| 阶段进入 | 进入时间 | 进入当前处理阶段的时间 | 字段 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:67](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L67) |
| 关闭时间 | 关闭时间 | 问题结束跟进的时间 | 字段 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:68](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L68) |
| 关闭来源 | 关闭方式 | 区分人工、复核与请求结束 | 字段 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:71](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L71) |
| 责任人 | 负责人 | 查看谁负责跟进问题 | 字段 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:74](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L74) |
| 指派来源 | 分配方式 | 查看负责人如何确定 | 字段 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:78](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L78) |
| 指派时间 | 分配时间 | 查看负责人分配时间 | 字段 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:79](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L79) |
| 定位 | 问题位置 | 查看文件与代码行 | 分区 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:86](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L86) |
| 建议 | 修复建议 | 查看建议如何修改代码 | 分区 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:95](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L95) |
| 证据 | 相关代码 | 查看支持判断的代码片段 | 分区 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:100](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L100) |
| 归属 | 问题来源 | 区分新增与已有问题 | 分区 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:105](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L105) |
| 关联审查记录 | 检查记录 | 查看首次与最近发现记录 | 分区 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:110](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L110) |
| 首次且最近命中 | 发现记录 | 首次与最近发现为同次检查 | 字段 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:113](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L113) |
| 首次发现 | 首次发现 | 第一次检查发现该问题 | 字段 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:113](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L113) |
| 最近命中 | 最近发现 | 最近一次仍发现该问题 | 字段 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:118](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L118) |
| 复核证据 | 复核依据 | 查看需人工复核的原因 | 分区 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:126](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L126) |
| 待复核依据 | 复核依据 | 查看触发待复核的检查记录 | 字段 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:129](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L129) |
| 连续未命中轮数 | 未见次数 | 连续几次在检查范围内未发现 | 字段 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:141](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L141) |
| 疑似关联新发现问题 | 相关问题 | 可能与该问题相关的新问题 | 字段 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:145](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L145) |
| 总结评论投递 | 总结评论 | 查看总结评论发送情况 | 分区 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:161](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L161)<br>[acr-ui/src/views/review/issue/IssueDetailDrawer.vue:185](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L185) |
| 查看投递记录 | 发送记录 | 查看评论与通知发送情况 | 动作 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:162](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L162) |
| Push 审查不向代码平台发布总结评论；结果可在审查记录和通知投递中查看 | 推送结果见检查记录与通知，无总结评论 | 保留推送结果交付差异 | 提示 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:184](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L184) |
| 暂无该总结评论投递记录 | 暂无总结评论发送记录 | 说明未找到评论记录 | 提示 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:185](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L185) |
| 行内评论 | 代码评论 | 查看代码行上的评论 | 分区 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:189](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L189) |
| 已发布 | 已发布 | 评论已发布到代码平台 | 状态 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:192](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L192) |
| 待投递 | 待发送 | 等待发送评论 | 状态 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:193](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L193) |
| 自动重试中 | 自动重发 | 上次发送失败，将自动重试 | 状态 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:194](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L194) |
| 待人工处置 | 需处理 | 需要人工处理发送问题 | 状态 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:195](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L195) |
| 未发布 | 未发布 | 该评论未发布到代码平台 | 状态 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:196](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L196) |
| Push 审查不向代码平台发布行内评论 | 推送检查不发布代码评论 | 保留推送评论限制 | 提示 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:215](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L215) |
| 未发布 · 未达严重度门槛或未启用行内评论 | 未发布：风险未达要求或未开启代码评论 | 保留两类未发布原因 | 提示 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:216](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L216) |
| 生命周期时间线 | 处理历史 | 查看历次发现与处理 | 分区 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:220](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L220) |
| 暂无处置记录 | 暂无处理记录 | 尚无可查看的处理记录 | 提示 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:221](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L221) |
| 转派 | 转交 | 改由其他成员负责 | 动作 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:254](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L254)<br>[acr-ui/src/views/review/issue/IssueDetailDrawer.vue:300](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L300)<br>[acr-ui/src/views/review/issue/IssueDetailDrawer.vue:407](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L407) |
| 确认 | 确认问题 | 确认问题需要修复 | 动作 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:261](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L261)<br>[acr-ui/src/views/review/issue/issueLifecycle.js:85](../../acr-ui/src/views/review/issue/issueLifecycle.js#L85) |
| 关闭 | 关闭问题 | 填写说明后结束跟进 | 动作 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:263](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L263)<br>[acr-ui/src/views/review/issue/issueLifecycle.js:66](../../acr-ui/src/views/review/issue/issueLifecycle.js#L66) |
| 忽略 | 忽略问题 | 记录原因并结束主动跟进 | 动作 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:264](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L264)<br>[acr-ui/src/views/review/issue/IssueDetailDrawer.vue:338](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L338) |
| 误报 | 标记误报 | 认为检测有误并填写原因 | 动作 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:265](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L265)<br>[acr-ui/src/views/review/issue/IssueDetailDrawer.vue:339](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L339)<br>[acr-ui/src/views/review/issue/issueLifecycle.js:9](../../acr-ui/src/views/review/issue/issueLifecycle.js#L9) |
| 确认已修复并关闭 | 确认修复 | 人工确认修复后关闭问题 | 动作 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:273](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L273)<br>[acr-ui/src/views/review/issue/IssueDetailDrawer.vue:446](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L446) |
| 未修复，重新打开 | 继续修复 | 未修复，恢复到待修复 | 动作 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:274](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L274)<br>[acr-ui/src/views/review/issue/IssueDetailDrawer.vue:773](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L773) |
| 关闭说明 | 关闭说明 | 说明关闭原因或修复方式 | 字段 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:283](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L283) |
| 目标成员 | 新负责人 | 选择接手问题的项目成员 | 字段 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:302](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L302) |
| 请选择项目有效成员 | 选择有效的项目成员 | 保留可选成员范围 | 提示 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:306](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L306) |
| 转派原因 | 转交原因 | 填写更换负责人的原因 | 字段 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:317](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L317) |
| 必填：说明转派原因 | 请填写转交原因 | 转交原因必填 | 提示 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:324](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L324) |
| 确定转派 | 确认转交 | 提交新负责人与原因 | 动作 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:330](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L330) |
| 处置类型 | 处理方式 | 选择忽略或标记误报 | 字段 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:336](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L336) |
| 原因说明 | 处理原因 | 填写忽略或误报原因 | 字段 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:342](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L342) |
| 必填：说明忽略或误报原因 | 请填写忽略或误报原因 | 保留必须说明原因 | 提示 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:349](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L349) |
| 确认问题 | 确认问题 | 人工确认需要修复 | 状态 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:397](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L397) |
| 关闭问题 | 关闭问题 | 人工结束该问题的跟进 | 状态 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:398](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L398)<br>[acr-ui/src/views/review/issue/IssueDetailDrawer.vue:446](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L446) |
| 忽略/误报 | 结束跟进 | 以忽略或误报方式结束 | 状态 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:399](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L399) |
| 自动转复核 | 转为复核 | 系统转入待人工复核 | 状态 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:400](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L400) |
| 自动重开 | 自动重开 | 系统再次开启问题跟进 | 状态 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:401](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L401) |
| 重新打开 | 重新打开 | 人工恢复为待修复 | 状态 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:402](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L402) |
| 发现 | 首次发现 | 首次检查发现问题 | 状态 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:403](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L403)<br>[acr-ui/src/views/review/issue/issueLifecycle.js:77](../../acr-ui/src/views/review/issue/issueLifecycle.js#L77) |
| 再次命中 | 再次发现 | 后续检查再次发现问题 | 状态 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:404](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L404) |
| 未命中 | 未再发现 | 本次检查范围内未发现问题 | 状态 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:405](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L405) |
| 自动指派 | 自动分配 | 系统按规则分配负责人 | 状态 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:406](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L406) |
| 系统已发现，等待人工确认 | 待确认 | 先确认是否需要修复 | 提示 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:496](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L496) |
| 请判断该问题是否需要进入修复治理；确认后将转为待修复。 | 确认需要修复后，转为待修复 | 保留确认后的状态 | 提示 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:496](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L496) |
| 问题已确认，等待代码修复 | 待修复 | 需要修改代码 | 提示 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:499](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L499) |
| 下一次审查会继续跟踪是否命中；未命中达到阈值后进入待复核。 | 连续未发现达到设定次数后待复核 | 不是一次未发现就关闭 | 提示 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:499](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L499) |
| 疑似已修复，等待人工验证 | 待复核 | 仍需人工验证修复情况 | 提示 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:502](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L502) |
| 系统仅确认新代码中未再次命中，尚不能替代人工验证；请确认关闭或恢复为待修复。 | 未再发现不代表修复，请人工复核 | 保留人工确认边界 | 提示 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:502](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L502) |
| 问题已关闭 | 已关闭 | 结束跟进不等于全部已修复 | 提示 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:505](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L505) |
| 问题已完成验证或随合并请求关闭，可通过时间线追溯处置过程。 | 人工处理或请求结束后关闭，见处理历史 | 保留不同关闭来源 | 提示 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:505](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L505) |
| 问题已忽略 | 已忽略 | 不再进入待处理列表 | 提示 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:508](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L508) |
| 该问题不再进入活跃治理，原因可在处置说明和时间线中查看。 | 已结束跟进，原因见处理说明与历史 | 保留退出主动跟进及原因入口 | 提示 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:508](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L508)<br>[acr-ui/src/views/review/issue/IssueDetailDrawer.vue:511](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L511) |
| 问题已标记为误报 | 已误报 | 认为检测有误后结束跟进 | 提示 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:511](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L511) |
| 通过审查记录和处置时间线追溯问题变化。 | 查看检查记录与处理历史 | 说明追溯方式 | 提示 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:514](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L514) |
| 系统事件 | 自动操作 | 系统执行的操作 | 字段 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:555](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L555) |
| 人工处置 | 人工处理 | 用户执行的操作 | 字段 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:555](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L555) |
| 按提交作者 | 提交作者 | 由代码提交作者负责 | 选项 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:564](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L564) |
| 按 PR 发起人 | 发起人 | 由合并请求发起人负责 | 选项 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:565](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L565) |
| 按项目负责人 | 负责人 | 由项目负责人负责 | 选项 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:566](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L566) |
| 人工转派 | 人工转交 | 人工更换负责人 | 选项 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:567](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L567) |
| 已转派 | 已转交 | 已更换负责人 | 反馈 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:596](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L596) |
| 人工关闭 | 人工关闭 | 人工填写原因后关闭 | 选项 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:604](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L604) |
| 复核确认关闭 | 复核关闭 | 确认修复后关闭 | 选项 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:605](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L605) |
| 合并请求已合并 | 请求合并 | 随合并请求合并后关闭 | 选项 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:606](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L606) |
| 合并请求已关闭 | 请求关闭 | 随合并请求关闭后结束跟进 | 选项 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:607](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L607) |
| 评论同步失败，可在投递记录重试 | 评论未能更新，可到发送记录重试 | 保留内部操作与外部发送差异 | 提示 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:731](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L731) |
| 确认该问题需要修复？ | 确认需要修复这个问题？ | 确认问题后进入待修复 | 提示 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:742](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L742) |
| 已确认修复并关闭 | 已修复并关闭 | 仅用于人工修复确认成功 | 反馈 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:764](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L764) |
| 确认该问题未修复，重新打开为待修复？ | 问题未修复，恢复为待修复？ | 保留重新打开后的状态 | 提示 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:773](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L773) |
| 已重新打开 | 已恢复待修复 | 明确实际恢复状态 | 反馈 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:777](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L777) |
| 处置成功 | 已处理 | 反馈处理完成 | 反馈 | [acr-ui/src/views/review/issue/IssueDetailDrawer.vue:794](../../acr-ui/src/views/review/issue/IssueDetailDrawer.vue#L794) |

### 批量处理

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 确认 | 确认问题 | 确认所选问题需要修复 | 动作 | [acr-ui/src/views/review/issue/IssueBatchBar.vue:14](../../acr-ui/src/views/review/issue/IssueBatchBar.vue#L14) |
| 关闭 | 关闭问题 | 关闭所选问题并填写原因 | 动作 | [acr-ui/src/views/review/issue/IssueBatchBar.vue:26](../../acr-ui/src/views/review/issue/IssueBatchBar.vue#L26) |
| 忽略 | 忽略问题 | 忽略所选问题并填写原因 | 动作 | [acr-ui/src/views/review/issue/IssueBatchBar.vue:38](../../acr-ui/src/views/review/issue/IssueBatchBar.vue#L38)<br>[acr-ui/src/views/review/issue/IssueBatchBar.vue:80](../../acr-ui/src/views/review/issue/IssueBatchBar.vue#L80) |
| 误报 | 标记误报 | 标记所选问题为误报 | 动作 | [acr-ui/src/views/review/issue/IssueBatchBar.vue:50](../../acr-ui/src/views/review/issue/IssueBatchBar.vue#L50)<br>[acr-ui/src/views/review/issue/IssueBatchBar.vue:81](../../acr-ui/src/views/review/issue/IssueBatchBar.vue#L81) |
| 清除选择 | 取消选择 | 取消所有已选问题 | 动作 | [acr-ui/src/views/review/issue/IssueBatchBar.vue:54](../../acr-ui/src/views/review/issue/IssueBatchBar.vue#L54) |
| 关闭说明 | 关闭说明 | 说明关闭原因或修复方式 | 字段 | [acr-ui/src/views/review/issue/IssueBatchBar.vue:59](../../acr-ui/src/views/review/issue/IssueBatchBar.vue#L59) |
| 请说明关闭原因或修复方式 | 请填写关闭原因或修复方式 | 保留关闭说明要求 | 提示 | [acr-ui/src/views/review/issue/IssueBatchBar.vue:66](../../acr-ui/src/views/review/issue/IssueBatchBar.vue#L66) |
| 确定关闭 | 关闭问题 | 提交所选问题的关闭操作 | 动作 | [acr-ui/src/views/review/issue/IssueBatchBar.vue:72](../../acr-ui/src/views/review/issue/IssueBatchBar.vue#L72) |
| 处置类型 | 处理方式 | 选择忽略或误报 | 字段 | [acr-ui/src/views/review/issue/IssueBatchBar.vue:78](../../acr-ui/src/views/review/issue/IssueBatchBar.vue#L78) |
| 原因说明 | 处理原因 | 说明忽略或误报原因 | 字段 | [acr-ui/src/views/review/issue/IssueBatchBar.vue:84](../../acr-ui/src/views/review/issue/IssueBatchBar.vue#L84) |
| 必填：说明忽略或误报原因 | 请填写忽略或误报原因 | 原因必须填写 | 提示 | [acr-ui/src/views/review/issue/IssueBatchBar.vue:91](../../acr-ui/src/views/review/issue/IssueBatchBar.vue#L91) |
| 不满足处置条件 | 当前无法处理 | 说明操作条件不符 | 提示 | [acr-ui/src/views/review/issue/IssueBatchBar.vue:193](../../acr-ui/src/views/review/issue/IssueBatchBar.vue#L193)<br>[acr-ui/src/views/review/issue/IssueBatchBar.vue:198](../../acr-ui/src/views/review/issue/IssueBatchBar.vue#L198) |
| 部分问题不满足处置条件 | 部分问题当前无法处理 | 说明批量操作条件不符 | 提示 | [acr-ui/src/views/review/issue/IssueBatchBar.vue:198](../../acr-ui/src/views/review/issue/IssueBatchBar.vue#L198) |
| 批量处置未执行 | 未能处理 | 本次所选问题均未提交处理 | 标题 | [acr-ui/src/views/review/issue/IssueBatchBar.vue:199](../../acr-ui/src/views/review/issue/IssueBatchBar.vue#L199) |
| 知道了 | 知道了 | 关闭提示窗口 | 动作 | [acr-ui/src/views/review/issue/IssueBatchBar.vue:201](../../acr-ui/src/views/review/issue/IssueBatchBar.vue#L201) |

### 评论发送

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 代码平台评论 | 代码评论 | 查看评论发送情况 | 分区 | [acr-ui/src/views/review/components/DeliveryStatusView.vue:4](../../acr-ui/src/views/review/components/DeliveryStatusView.vue#L4)<br>[acr-ui/src/views/review/components/DeliveryStatusView.vue:16](../../acr-ui/src/views/review/components/DeliveryStatusView.vue#L16)<br>[acr-ui/src/views/review/components/DeliveryStatusView.vue:19](../../acr-ui/src/views/review/components/DeliveryStatusView.vue#L19) |
| 总结评论投递 | 总结评论 | 查看汇总评论发送情况 | 分区 | [acr-ui/src/views/review/components/DeliveryStatusView.vue:4](../../acr-ui/src/views/review/components/DeliveryStatusView.vue#L4) |
| 推送审查没有可挂载的代码平台评论；审查结果通过通知和问题台账交付。 | 推送结果见通知与问题跟进，无代码评论 | 保留推送交付差异 | 提示 | [acr-ui/src/views/review/components/DeliveryStatusView.vue:16](../../acr-ui/src/views/review/components/DeliveryStatusView.vue#L16) |
| 与审查结论独立：投递失败不影响任务成功状态。同一变更来源仅维护一条 ACR 总结评论。 | 发送失败不改检查结论；同次变更一条评论 | 保留独立状态与评论去重 | 提示 | [acr-ui/src/views/review/components/DeliveryStatusView.vue:18](../../acr-ui/src/views/review/components/DeliveryStatusView.vue#L18) |
| 本任务无需投递代码平台评论。 | 本任务无需发送代码评论 | 说明不需要评论 | 提示 | [acr-ui/src/views/review/components/DeliveryStatusView.vue:19](../../acr-ui/src/views/review/components/DeliveryStatusView.vue#L19) |
| 尚未投递总结评论，可点击右上角「补投递」。 | 尚未发送总结评论，可点「补发」 | 说明补发入口 | 提示 | [acr-ui/src/views/review/components/DeliveryStatusView.vue:21](../../acr-ui/src/views/review/components/DeliveryStatusView.vue#L21) |
| 本任务未投递总结评论。 | 本任务未发送总结评论 | 说明未发送 | 提示 | [acr-ui/src/views/review/components/DeliveryStatusView.vue:21](../../acr-ui/src/views/review/components/DeliveryStatusView.vue#L21) |
| 投递状态 | 发送状态 | 查看评论是否已发送 | 字段 | [acr-ui/src/views/review/components/DeliveryStatusView.vue:24](../../acr-ui/src/views/review/components/DeliveryStatusView.vue#L24) |
| 已投递 | 已发送 | 评论发送成功 | 状态 | [acr-ui/src/views/review/components/DeliveryStatusView.vue:25](../../acr-ui/src/views/review/components/DeliveryStatusView.vue#L25) |
| 自动重试中 | 自动重发 | 失败后系统将自动重试 | 状态 | [acr-ui/src/views/review/components/DeliveryStatusView.vue:26](../../acr-ui/src/views/review/components/DeliveryStatusView.vue#L26)<br>[acr-ui/src/views/review/components/DeliveryStatusView.vue:139](../../acr-ui/src/views/review/components/DeliveryStatusView.vue#L139) |
| 待投递 | 待发送 | 等待发送评论 | 状态 | [acr-ui/src/views/review/components/DeliveryStatusView.vue:27](../../acr-ui/src/views/review/components/DeliveryStatusView.vue#L27)<br>[acr-ui/src/views/review/components/DeliveryStatusView.vue:140](../../acr-ui/src/views/review/components/DeliveryStatusView.vue#L140) |
| 待人工处置 | 需处理 | 需人工排查配置或失败 | 状态 | [acr-ui/src/views/review/components/DeliveryStatusView.vue:28](../../acr-ui/src/views/review/components/DeliveryStatusView.vue#L28)<br>[acr-ui/src/views/review/components/DeliveryStatusView.vue:141](../../acr-ui/src/views/review/components/DeliveryStatusView.vue#L141) |
| 已跳过 | 已跳过 | 本次没有发送 | 状态 | [acr-ui/src/views/review/components/DeliveryStatusView.vue:29](../../acr-ui/src/views/review/components/DeliveryStatusView.vue#L29)<br>[acr-ui/src/views/review/components/DeliveryStatusView.vue:142](../../acr-ui/src/views/review/components/DeliveryStatusView.vue#L142) |
| 最后尝试时间 | 最近发送 | 最近一次尝试发送的时间 | 字段 | [acr-ui/src/views/review/components/DeliveryStatusView.vue:32](../../acr-ui/src/views/review/components/DeliveryStatusView.vue#L32) |
| 尝试次数 | 发送次数 | 包含成功与失败的发送尝试 | 字段 | [acr-ui/src/views/review/components/DeliveryStatusView.vue:35](../../acr-ui/src/views/review/components/DeliveryStatusView.vue#L35) |
| 外部评论 ID | 评论编号 | 代码平台中的评论编号 | 字段 | [acr-ui/src/views/review/components/DeliveryStatusView.vue:38](../../acr-ui/src/views/review/components/DeliveryStatusView.vue#L38) |
| 未返回可读失败原因 | 未提供具体失败原因 | 避免显示无意义片段 | 提示 | [acr-ui/src/views/review/components/DeliveryStatusView.vue:46](../../acr-ui/src/views/review/components/DeliveryStatusView.vue#L46) |
| 行内评论投递 | 代码评论 | 查看发布到代码行的评论 | 分区 | [acr-ui/src/views/review/components/DeliveryStatusView.vue:52](../../acr-ui/src/views/review/components/DeliveryStatusView.vue#L52)<br>[acr-ui/src/views/review/components/DeliveryStatusView.vue:55](../../acr-ui/src/views/review/components/DeliveryStatusView.vue#L55) |
| 按问题严重度将重点问题发布到代码行；同一问题全生命周期仅发布一次。 | 按风险发布到代码行，同一问题只发一次 | 保留风险筛选与一次发布 | 提示 | [acr-ui/src/views/review/components/DeliveryStatusView.vue:54](../../acr-ui/src/views/review/components/DeliveryStatusView.vue#L54) |
| 本任务暂无行内评论投递记录。 | 暂无代码评论发送记录 | 没有返回行内评论记录 | 提示 | [acr-ui/src/views/review/components/DeliveryStatusView.vue:55](../../acr-ui/src/views/review/components/DeliveryStatusView.vue#L55) |
| 失败与跳过原因 | 未发原因 | 查看失败和跳过原因 | 分区 | [acr-ui/src/views/review/components/DeliveryStatusView.vue:61](../../acr-ui/src/views/review/components/DeliveryStatusView.vue#L61) |
| 补投递 | 补发 | 补发尚未发送的总结评论 | 动作 | [acr-ui/src/views/review/components/DeliveryStatusView.vue:109](../../acr-ui/src/views/review/components/DeliveryStatusView.vue#L109) |
| 重试投递 | 重新发送 | 再次发送失败的总结评论 | 动作 | [acr-ui/src/views/review/components/DeliveryStatusView.vue:109](../../acr-ui/src/views/review/components/DeliveryStatusView.vue#L109) |
| 确认补投递总结评论？将按该变更来源最近一次成功审查结论创建或更新评论。 | 按最近成功检查结论补发或更新评论？ | 保留使用最新成功结果 | 提示 | [acr-ui/src/views/review/components/DeliveryStatusView.vue:161](../../acr-ui/src/views/review/components/DeliveryStatusView.vue#L161) |
| 确认重试投递总结评论？将按该变更来源最近一次成功审查结论更新评论。 | 按最近成功检查结论重新发送评论？ | 保留用最新结果更新评论 | 提示 | [acr-ui/src/views/review/components/DeliveryStatusView.vue:162](../../acr-ui/src/views/review/components/DeliveryStatusView.vue#L162) |
| 补投递已完成 | 已补发 | 补发操作完成 | 反馈 | [acr-ui/src/views/review/components/DeliveryStatusView.vue:169](../../acr-ui/src/views/review/components/DeliveryStatusView.vue#L169) |
| 投递重试已完成 | 已重新发送 | 重试发送完成 | 反馈 | [acr-ui/src/views/review/components/DeliveryStatusView.vue:169](../../acr-ui/src/views/review/components/DeliveryStatusView.vue#L169) |

### 群聊通知

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| IM 群通知投递 | 群聊通知 | 查看群通知发送情况 | 分区 | [acr-ui/src/views/review/components/ImDeliveryStatusView.vue:4](../../acr-ui/src/views/review/components/ImDeliveryStatusView.vue#L4)<br>[acr-ui/src/views/review/components/ImDeliveryStatusView.vue:8](../../acr-ui/src/views/review/components/ImDeliveryStatusView.vue#L8) |
| 查看投递记录 | 发送记录 | 查看本任务的发送记录 | 动作 | [acr-ui/src/views/review/components/ImDeliveryStatusView.vue:5](../../acr-ui/src/views/review/components/ImDeliveryStatusView.vue#L5) |
| 加载 IM 投递状态… | 正在读取群通知状态 | 说明加载中 | 提示 | [acr-ui/src/views/review/components/ImDeliveryStatusView.vue:7](../../acr-ui/src/views/review/components/ImDeliveryStatusView.vue#L7) |
| 本任务暂无 IM 群通知投递记录。 | 本任务暂无群通知发送记录 | 没有群通知记录 | 提示 | [acr-ui/src/views/review/components/ImDeliveryStatusView.vue:8](../../acr-ui/src/views/review/components/ImDeliveryStatusView.vue#L8) |
| 审查通过且无风险问题，按通知策略跳过通知 | 检查通过且无风险，按设置不发通知 | 保留跳过的依据 | 提示 | [acr-ui/src/views/review/components/ImDeliveryStatusView.vue:40](../../acr-ui/src/views/review/components/ImDeliveryStatusView.vue#L40) |
| 按通知策略跳过通知 | 按设置不发送通知 | 说明按策略跳过 | 提示 | [acr-ui/src/views/review/components/ImDeliveryStatusView.vue:40](../../acr-ui/src/views/review/components/ImDeliveryStatusView.vue#L40)<br>[acr-ui/src/views/review/components/ImDeliveryStatusView.vue:41](../../acr-ui/src/views/review/components/ImDeliveryStatusView.vue#L41) |
| 已送达通知渠道 | 通知渠道已接收 | 不暗示群成员已读 | 提示 | [acr-ui/src/views/review/components/ImDeliveryStatusView.vue:43](../../acr-ui/src/views/review/components/ImDeliveryStatusView.vue#L43) |
| 投递失败 | 发送失败 | 群通知未能发送 | 提示 | [acr-ui/src/views/review/components/ImDeliveryStatusView.vue:44](../../acr-ui/src/views/review/components/ImDeliveryStatusView.vue#L44) |
| 投递中… | 等待发送 | 待发送与等待重试均非已送达 | 提示 | [acr-ui/src/views/review/components/ImDeliveryStatusView.vue:45](../../acr-ui/src/views/review/components/ImDeliveryStatusView.vue#L45) |

### 检查范围

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| OCR 引擎路径 | 工具检查 | 使用已配置的检查工具 | 状态 | [acr-ui/src/views/review/components/ScopeDecisionView.vue:13](../../acr-ui/src/views/review/components/ScopeDecisionView.vue#L13) |
| 注入 Diff | 变更字数 | 实际交给工具的变更文字量 | 字段 | [acr-ui/src/views/review/components/ScopeDecisionView.vue:16](../../acr-ui/src/views/review/components/ScopeDecisionView.vue#L16) |
| 生效配置 | 本次设置 | 本次检查实际采用的设置 | 分区 | [acr-ui/src/views/review/components/ScopeDecisionView.vue:22](../../acr-ui/src/views/review/components/ScopeDecisionView.vue#L22) |
| 全部文件均纳入审查，无排除/扩展/截断。 | 所有文件均已检查，无排除、扩展或截断 | 保留三类范围状态 | 提示 | [acr-ui/src/views/review/components/ScopeDecisionView.vue:59](../../acr-ui/src/views/review/components/ScopeDecisionView.vue#L59) |
| 范围决策未生效，按全量变更审查 | 范围设置未生效，改查全部变更 | 保留降级后扩大范围 | 提示 | [acr-ui/src/views/review/components/ScopeDecisionView.vue:82](../../acr-ui/src/views/review/components/ScopeDecisionView.vue#L82) |
| 审查测试文件 | 检查测试 | 测试文件参与检查 | 状态 | [acr-ui/src/views/review/components/ScopeDecisionView.vue:100](../../acr-ui/src/views/review/components/ScopeDecisionView.vue#L100) |
| 排除测试文件 | 排除测试 | 测试文件不参与检查 | 状态 | [acr-ui/src/views/review/components/ScopeDecisionView.vue:100](../../acr-ui/src/views/review/components/ScopeDecisionView.vue#L100) |
| 上报存量问题 | 含旧问题 | 同时报告历史已有问题 | 状态 | [acr-ui/src/views/review/components/ScopeDecisionView.vue:101](../../acr-ui/src/views/review/components/ScopeDecisionView.vue#L101) |
| 不上报存量问题 | 仅新问题 | 不报告历史已有问题 | 状态 | [acr-ui/src/views/review/components/ScopeDecisionView.vue:101](../../acr-ui/src/views/review/components/ScopeDecisionView.vue#L101) |
| 高影响扩展关 | 关闭补充 | 不额外补充高影响文件 | 状态 | [acr-ui/src/views/review/components/ScopeDecisionView.vue:102](../../acr-ui/src/views/review/components/ScopeDecisionView.vue#L102) |
| 高影响扩展开 | 开启补充 | 额外补充高影响文件内容 | 状态 | [acr-ui/src/views/review/components/ScopeDecisionView.vue:102](../../acr-ui/src/views/review/components/ScopeDecisionView.vue#L102) |
| 纳入审查 | 检查文件 | 参与本次检查的文件 | 分区 | [acr-ui/src/views/review/components/ScopeDecisionView.vue:113](../../acr-ui/src/views/review/components/ScopeDecisionView.vue#L113) |
| 排除 | 排除文件 | 不参与本次检查的文件 | 分区 | [acr-ui/src/views/review/components/ScopeDecisionView.vue:118](../../acr-ui/src/views/review/components/ScopeDecisionView.vue#L118) |
| 高影响扩展 | 补充文件 | 额外补充内容的高影响文件 | 分区 | [acr-ui/src/views/review/components/ScopeDecisionView.vue:102](../../acr-ui/src/views/review/components/ScopeDecisionView.vue#L102)<br>[acr-ui/src/views/review/components/ScopeDecisionView.vue:125](../../acr-ui/src/views/review/components/ScopeDecisionView.vue#L125) |
| 记录类变更 | 仅记变更 | 只记录变更，不检查正文 | 分区 | [acr-ui/src/views/review/components/ScopeDecisionView.vue:139](../../acr-ui/src/views/review/components/ScopeDecisionView.vue#L139) |
| 预算截断丢弃 | 超限文件 | 超过可处理字数而未检查 | 分区 | [acr-ui/src/views/review/components/ScopeDecisionView.vue:145](../../acr-ui/src/views/review/components/ScopeDecisionView.vue#L145) |
| 解析警告 | 读取提醒 | 读取代码变更时出现的提醒 | 分区 | [acr-ui/src/views/review/components/ScopeDecisionView.vue:149](../../acr-ui/src/views/review/components/ScopeDecisionView.vue#L149) |
| 未传入的排除规则 | 无效规则 | 未传给工具的排除规则 | 分区 | [acr-ui/src/views/review/components/ScopeDecisionView.vue:154](../../acr-ui/src/views/review/components/ScopeDecisionView.vue#L154) |
| 含逗号的 glob 无法经 CLI --exclude 表达，未生效。 | 带逗号的排除规则未生效 | 准确说明规则失效 | 提示 | [acr-ui/src/views/review/components/ScopeDecisionView.vue:155](../../acr-ui/src/views/review/components/ScopeDecisionView.vue#L155) |

### 通知设置

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 渠道名称 | 通知名称 | 为这项群通知起个名字 | 字段 | [acr-ui/src/views/notify/channel/index.vue:4](../../acr-ui/src/views/notify/channel/index.vue#L4)<br>[acr-ui/src/views/notify/channel/index.vue:32](../../acr-ui/src/views/notify/channel/index.vue#L32)<br>[acr-ui/src/views/notify/channel/index.vue:90](../../acr-ui/src/views/notify/channel/index.vue#L90) |
| 渠道类型 | 通知平台 | 选择钉钉、企业微信或飞书 | 字段 | [acr-ui/src/views/notify/channel/index.vue:7](../../acr-ui/src/views/notify/channel/index.vue#L7)<br>[acr-ui/src/views/notify/channel/index.vue:33](../../acr-ui/src/views/notify/channel/index.vue#L33)<br>[acr-ui/src/views/notify/channel/index.vue:93](../../acr-ui/src/views/notify/channel/index.vue#L93) |
| 状态 | 使用状态 | 开启或停用此项群通知 | 字段 | [acr-ui/src/views/notify/channel/index.vue:12](../../acr-ui/src/views/notify/channel/index.vue#L12)<br>[acr-ui/src/views/notify/channel/index.vue:67](../../acr-ui/src/views/notify/channel/index.vue#L67)<br>[acr-ui/src/views/notify/channel/index.vue:107](../../acr-ui/src/views/notify/channel/index.vue#L107) |
| 新增 | 添加通知 | 新增群通知设置 | 动作 | [acr-ui/src/views/notify/channel/index.vue:26](../../acr-ui/src/views/notify/channel/index.vue#L26) |
| 暂无通知渠道 | 暂无通知设置 | 尚未添加群通知 | 提示 | [acr-ui/src/views/notify/channel/index.vue:31](../../acr-ui/src/views/notify/channel/index.vue#L31) |
| 加签 Secret | 验证密钥 | 群机器人校验消息用的密钥 | 字段 | [acr-ui/src/views/notify/channel/index.vue:45](../../acr-ui/src/views/notify/channel/index.vue#L45)<br>[acr-ui/src/views/notify/channel/index.vue:102](../../acr-ui/src/views/notify/channel/index.vue#L102) |
| 已配置 | 已填写 | 已保存地址或密钥 | 状态 | [acr-ui/src/views/notify/channel/index.vue:41](../../acr-ui/src/views/notify/channel/index.vue#L41)<br>[acr-ui/src/views/notify/channel/index.vue:48](../../acr-ui/src/views/notify/channel/index.vue#L48) |
| 未配置 | 未填写 | 尚未保存地址或密钥 | 状态 | [acr-ui/src/views/notify/channel/index.vue:41](../../acr-ui/src/views/notify/channel/index.vue#L41)<br>[acr-ui/src/views/notify/channel/index.vue:48](../../acr-ui/src/views/notify/channel/index.vue#L48) |
| 使用状态 | 使用情况 | 查看哪些项目使用此项设置 | 字段 | [acr-ui/src/views/notify/channel/index.vue:52](../../acr-ui/src/views/notify/channel/index.vue#L52) |
| 未使用 | 未使用 | 还没有项目使用 | 状态 | [acr-ui/src/views/notify/channel/index.vue:55](../../acr-ui/src/views/notify/channel/index.vue#L55) |
| 最近检测 | 最近测试 | 查看最近一次发送测试 | 字段 | [acr-ui/src/views/notify/channel/index.vue:59](../../acr-ui/src/views/notify/channel/index.vue#L59) |
| 尚未检测 | 尚未测试 | 还未进行发送测试 | 提示 | [acr-ui/src/views/notify/channel/index.vue:61](../../acr-ui/src/views/notify/channel/index.vue#L61) |
| 测试 | 发送测试 | 向目标群发送测试消息 | 动作 | [acr-ui/src/views/notify/channel/index.vue:75](../../acr-ui/src/views/notify/channel/index.vue#L75) |
| 修改 | 修改设置 | 修改群通知配置 | 动作 | [acr-ui/src/views/notify/channel/index.vue:76](../../acr-ui/src/views/notify/channel/index.vue#L76) |
| 停用 | 停用 | 停用此项通知设置 | 动作 | [acr-ui/src/views/notify/channel/index.vue:15](../../acr-ui/src/views/notify/channel/index.vue#L15)<br>[acr-ui/src/views/notify/channel/index.vue:69](../../acr-ui/src/views/notify/channel/index.vue#L69)<br>[acr-ui/src/views/notify/channel/index.vue:78](../../acr-ui/src/views/notify/channel/index.vue#L78)<br>[acr-ui/src/views/notify/channel/index.vue:110](../../acr-ui/src/views/notify/channel/index.vue#L110)<br>[acr-ui/src/views/notify/channel/index.vue:210](../../acr-ui/src/views/notify/channel/index.vue#L210) |
| 启用 | 启用 | 启用此项通知设置 | 动作 | [acr-ui/src/views/notify/channel/index.vue:14](../../acr-ui/src/views/notify/channel/index.vue#L14)<br>[acr-ui/src/views/notify/channel/index.vue:69](../../acr-ui/src/views/notify/channel/index.vue#L69)<br>[acr-ui/src/views/notify/channel/index.vue:78](../../acr-ui/src/views/notify/channel/index.vue#L78)<br>[acr-ui/src/views/notify/channel/index.vue:109](../../acr-ui/src/views/notify/channel/index.vue#L109)<br>[acr-ui/src/views/notify/channel/index.vue:210](../../acr-ui/src/views/notify/channel/index.vue#L210) |
| 删除 | 删除 | 删除此项通知设置 | 动作 | [acr-ui/src/views/notify/channel/index.vue:80](../../acr-ui/src/views/notify/channel/index.vue#L80) |
| 如：研发群钉钉机器人 | 例如：研发群通知 | 给出日常命名示例 | 提示 | [acr-ui/src/views/notify/channel/index.vue:91](../../acr-ui/src/views/notify/channel/index.vue#L91) |
| 留空保留原 URL；修改时请重新输入 | 不改则保留；换地址请重新输入 | 保留原地址的准确条件 | 提示 | [acr-ui/src/views/notify/channel/index.vue:100](../../acr-ui/src/views/notify/channel/index.vue#L100) |
| 请输入群机器人 Webhook URL | 粘贴群机器人的通知地址 | 说明所需地址 | 提示 | [acr-ui/src/views/notify/channel/index.vue:100](../../acr-ui/src/views/notify/channel/index.vue#L100) |
| 留空保留原 Secret；清空并提交可清除加签 | 不改保留；输入后清空可删除密钥 | 区分未输入与输入后清空 | 提示 | [acr-ui/src/views/notify/channel/index.vue:104](../../acr-ui/src/views/notify/channel/index.vue#L104) |
| 可选，钉钉等渠道启用加签时填写 | 可选，群机器人开启验证时填写 | 说明填写条件 | 提示 | [acr-ui/src/views/notify/channel/index.vue:104](../../acr-ui/src/views/notify/channel/index.vue#L104) |
| Webhook URL 与 Secret 只在提交时发送，编辑页面不会回显历史值。 | 地址和密钥仅提交时发送，不显示旧值 | 不承诺不发送已有值 | 提示 | [acr-ui/src/views/notify/channel/index.vue:106](../../acr-ui/src/views/notify/channel/index.vue#L106) |
| 备注 | 备注 | 补充群名称或通知用途 | 字段 | [acr-ui/src/views/notify/channel/index.vue:113](../../acr-ui/src/views/notify/channel/index.vue#L113) |
| 可选，例如渠道用途或群名称 | 可选，填写群名称或通知用途 | 说明备注内容 | 提示 | [acr-ui/src/views/notify/channel/index.vue:114](../../acr-ui/src/views/notify/channel/index.vue#L114) |
| 确 定 | 保存 | 保存通知设置 | 动作 | [acr-ui/src/views/notify/channel/index.vue:119](../../acr-ui/src/views/notify/channel/index.vue#L119) |
| 取 消 | 取消 | 放弃本次编辑 | 动作 | [acr-ui/src/views/notify/channel/index.vue:120](../../acr-ui/src/views/notify/channel/index.vue#L120) |
| 新增渠道时必须输入 Webhook URL | 请填写群机器人的通知地址 | 新建必须填写地址 | 提示 | [acr-ui/src/views/notify/channel/index.vue:144](../../acr-ui/src/views/notify/channel/index.vue#L144) |
| 渠道名称不能为空 | 请填写通知名称 | 名称不能为空 | 提示 | [acr-ui/src/views/notify/channel/index.vue:152](../../acr-ui/src/views/notify/channel/index.vue#L152) |
| 新增通知渠道 | 添加通知 | 填写新的群通知设置 | 标题 | [acr-ui/src/views/notify/channel/index.vue:178](../../acr-ui/src/views/notify/channel/index.vue#L178) |
| 修改通知渠道 | 修改通知 | 修改已有群通知设置 | 标题 | [acr-ui/src/views/notify/channel/index.vue:185](../../acr-ui/src/views/notify/channel/index.vue#L185) |
| 修改成功 | 已保存 | 修改已保存 | 反馈 | [acr-ui/src/views/notify/channel/index.vue:194](../../acr-ui/src/views/notify/channel/index.vue#L194) |
| 新增成功 | 已添加 | 新设置已添加 | 反馈 | [acr-ui/src/views/notify/channel/index.vue:194](../../acr-ui/src/views/notify/channel/index.vue#L194) |
| 删除成功 | 已删除 | 设置已删除 | 反馈 | [acr-ui/src/views/notify/channel/index.vue:203](../../acr-ui/src/views/notify/channel/index.vue#L203) |
| 发送正常 | 发送正常 | 最近一次测试发送成功 | 状态 | [acr-ui/src/views/notify/channel/index.vue:229](../../acr-ui/src/views/notify/channel/index.vue#L229) |
| 发送失败 | 发送失败 | 最近一次测试发送失败 | 状态 | [acr-ui/src/views/notify/channel/index.vue:229](../../acr-ui/src/views/notify/channel/index.vue#L229) |
| 未检测 | 未测试 | 尚未发送测试消息 | 状态 | [acr-ui/src/views/notify/channel/index.vue:229](../../acr-ui/src/views/notify/channel/index.vue#L229) |

### 发送记录

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 尝试时间 | 发送时间 | 按尝试发送的时间查找 | 字段 | [acr-ui/src/views/notify/delivery/index.vue:4](../../acr-ui/src/views/notify/delivery/index.vue#L4) |
| 所属项目 | 所属项目 | 按项目查找发送记录 | 字段 | [acr-ui/src/views/notify/delivery/index.vue:8](../../acr-ui/src/views/notify/delivery/index.vue#L8) |
| 投递渠道 | 发送方式 | 查看代码评论或群通知类型 | 字段 | [acr-ui/src/views/notify/delivery/index.vue:13](../../acr-ui/src/views/notify/delivery/index.vue#L13)<br>[acr-ui/src/views/notify/delivery/index.vue:47](../../acr-ui/src/views/notify/delivery/index.vue#L47) |
| 投递状态 | 发送状态 | 查看成功、失败或待发送 | 字段 | [acr-ui/src/views/notify/delivery/index.vue:18](../../acr-ui/src/views/notify/delivery/index.vue#L18)<br>[acr-ui/src/views/notify/delivery/index.vue:58](../../acr-ui/src/views/notify/delivery/index.vue#L58) |
| PR 编号 | 请求编号 | 按合并请求编号查找 | 字段 | [acr-ui/src/views/notify/delivery/index.vue:23](../../acr-ui/src/views/notify/delivery/index.vue#L23)<br>[acr-ui/src/views/notify/delivery/index.vue:24](../../acr-ui/src/views/notify/delivery/index.vue#L24) |
| 请输入 PR 编号 | 填写合并请求编号 | 明确编号来源 | 提示 | [acr-ui/src/views/notify/delivery/index.vue:24](../../acr-ui/src/views/notify/delivery/index.vue#L24) |
| 暂无投递记录 | 暂无发送记录 | 没有符合条件的发送记录 | 提示 | [acr-ui/src/views/notify/delivery/index.vue:36](../../acr-ui/src/views/notify/delivery/index.vue#L36) |
| 最后尝试时间 | 最近发送 | 最近一次尝试发送的时间 | 字段 | [acr-ui/src/views/notify/delivery/index.vue:37](../../acr-ui/src/views/notify/delivery/index.vue#L37) |
| 待首次尝试 | 尚未发送 | 尚未首次尝试发送 | 状态 | [acr-ui/src/views/notify/delivery/index.vue:39](../../acr-ui/src/views/notify/delivery/index.vue#L39) |
| 业务系统 | 所属系统 | 查看所属业务系统 | 字段 | [acr-ui/src/views/notify/delivery/index.vue:42](../../acr-ui/src/views/notify/delivery/index.vue#L42) |
| 项目名称 | 所属项目 | 查看所属项目 | 字段 | [acr-ui/src/views/notify/delivery/index.vue:46](../../acr-ui/src/views/notify/delivery/index.vue#L46) |
| 触发来源 | 触发原因 | 查看为何生成本次通知 | 字段 | [acr-ui/src/views/notify/delivery/index.vue:52](../../acr-ui/src/views/notify/delivery/index.vue#L52) |
| 变更编号 | 请求编号 | 合并请求编号或推送位置 | 字段 | [acr-ui/src/views/notify/delivery/index.vue:63](../../acr-ui/src/views/notify/delivery/index.vue#L63) |
| 任务 ID | 任务编号 | 查看关联检查任务编号 | 字段 | [acr-ui/src/views/notify/delivery/index.vue:69](../../acr-ui/src/views/notify/delivery/index.vue#L69) |
| 失败原因 | 失败原因 | 查看发送失败的具体原因 | 字段 | [acr-ui/src/views/notify/delivery/index.vue:70](../../acr-ui/src/views/notify/delivery/index.vue#L70) |
| 尝试次数 | 发送次数 | 包含成功与失败的发送尝试 | 字段 | [acr-ui/src/views/notify/delivery/index.vue:71](../../acr-ui/src/views/notify/delivery/index.vue#L71) |
| 下次处理时间 | 下次发送 | 查看系统计划的下次处理时间 | 字段 | [acr-ui/src/views/notify/delivery/index.vue:72](../../acr-ui/src/views/notify/delivery/index.vue#L72) |
| 较早投递未保留消息快照 | 早期记录未保存消息内容 | 说明历史消息不可查看 | 提示 | [acr-ui/src/views/notify/delivery/index.vue:80](../../acr-ui/src/views/notify/delivery/index.vue#L80) |
| 消息详情 | 查看消息 | 查看实际发送的标题与正文 | 动作 | [acr-ui/src/views/notify/delivery/index.vue:90](../../acr-ui/src/views/notify/delivery/index.vue#L90) |
| 补发 | 补发 | 将失败的发送重新排入队列 | 动作 | [acr-ui/src/views/notify/delivery/index.vue:95](../../acr-ui/src/views/notify/delivery/index.vue#L95) |
| 标记已处理 | 标记完成 | 标记人工处理并停止自动发送 | 动作 | [acr-ui/src/views/notify/delivery/index.vue:98](../../acr-ui/src/views/notify/delivery/index.vue#L98) |
| 消息详情 | 消息内容 | 查看本次发送的内容 | 标题 | [acr-ui/src/views/notify/delivery/index.vue:108](../../acr-ui/src/views/notify/delivery/index.vue#L108) |
| 确认将该记录加入补发队列？群机器人渠道在极端情况下仍可能出现重复消息 | 补发这条消息？群里可能收到重复消息 | 保留群通知重复的可能 | 提示 | [acr-ui/src/views/notify/delivery/index.vue:200](../../acr-ui/src/views/notify/delivery/index.vue#L200) |
| 已加入补发队列 | 已安排补发 | 排入队列不代表已经送达 | 反馈 | [acr-ui/src/views/notify/delivery/index.vue:204](../../acr-ui/src/views/notify/delivery/index.vue#L204) |
| 确认标记该投递为人工已处理？标记后不再自动投递。 | 标为已处理？此后不再自动发送 | 保留停止自动发送后果 | 提示 | [acr-ui/src/views/notify/delivery/index.vue:210](../../acr-ui/src/views/notify/delivery/index.vue#L210) |
| 已标记人工处理 | 已标记处理 | 反馈人工处理标记已完成 | 反馈 | [acr-ui/src/views/notify/delivery/index.vue:214](../../acr-ui/src/views/notify/delivery/index.vue#L214) |
| 暂无消息内容 | 暂无消息内容 | 没有可查看的正文 | 提示 | [acr-ui/src/views/notify/components/MessageContentView.vue:9](../../acr-ui/src/views/notify/components/MessageContentView.vue#L9) |
| 业务系统  | 所属系统  | 显示业务系统名称 | 提示 | [acr-ui/src/views/notify/delivery/index.vue:117](../../acr-ui/src/views/notify/delivery/index.vue#L117) |

### 通用显示

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 功能实现的正确性与健壮性 | 功能可靠 | 功能是否正确，异常时是否稳定 | 字段 | [acr-ui/src/utils/reviewDisplay.js:6](../../acr-ui/src/utils/reviewDisplay.js#L6) |
| 安全性与潜在风险 | 代码安全 | 查看安全问题与潜在风险 | 字段 | [acr-ui/src/utils/reviewDisplay.js:7](../../acr-ui/src/utils/reviewDisplay.js#L7) |
| 最佳实践与可维护性 | 易于维护 | 代码是否清晰，方便修改 | 字段 | [acr-ui/src/utils/reviewDisplay.js:8](../../acr-ui/src/utils/reviewDisplay.js#L8) |
| 性能与资源利用 | 运行效率 | 运行速度与资源使用情况 | 字段 | [acr-ui/src/utils/reviewDisplay.js:9](../../acr-ui/src/utils/reviewDisplay.js#L9) |
| 提交信息质量 | 提交说明 | 提交说明是否清楚完整 | 字段 | [acr-ui/src/utils/reviewDisplay.js:10](../../acr-ui/src/utils/reviewDisplay.js#L10) |
| 高 | 高风险 | 高风险等级 | 状态 | [acr-ui/src/utils/reviewDisplay.js:15](../../acr-ui/src/utils/reviewDisplay.js#L15) |
| 中 | 中风险 | 中风险等级 | 状态 | [acr-ui/src/utils/reviewDisplay.js:16](../../acr-ui/src/utils/reviewDisplay.js#L16) |
| 低 | 低风险 | 低风险等级 | 状态 | [acr-ui/src/utils/reviewDisplay.js:17](../../acr-ui/src/utils/reviewDisplay.js#L17) |
| 信息 | 提示信息 | 信息级提醒，不等同低风险 | 状态 | [acr-ui/src/utils/reviewDisplay.js:18](../../acr-ui/src/utils/reviewDisplay.js#L18) |
| 正确性 | 功能可靠 | 功能正确与稳定性问题 | 分类 | [acr-ui/src/utils/reviewDisplay.js:22](../../acr-ui/src/utils/reviewDisplay.js#L22) |
| 安全性 | 代码安全 | 代码中的安全问题 | 分类 | [acr-ui/src/utils/reviewDisplay.js:23](../../acr-ui/src/utils/reviewDisplay.js#L23) |
| 最佳实践 | 易于维护 | 代码规范与维护问题 | 分类 | [acr-ui/src/utils/reviewDisplay.js:24](../../acr-ui/src/utils/reviewDisplay.js#L24) |
| 性能 | 运行效率 | 性能与资源使用问题 | 分类 | [acr-ui/src/utils/reviewDisplay.js:25](../../acr-ui/src/utils/reviewDisplay.js#L25) |
| 提交质量 | 提交说明 | 提交信息质量问题 | 分类 | [acr-ui/src/utils/reviewDisplay.js:26](../../acr-ui/src/utils/reviewDisplay.js#L26) |
| 新增 | 新增问题 | 本次代码变更引入的问题 | 状态 | [acr-ui/src/utils/reviewDisplay.js:31](../../acr-ui/src/utils/reviewDisplay.js#L31) |
| 存量 | 已有问题 | 本次变更之前已有的问题 | 状态 | [acr-ui/src/utils/reviewDisplay.js:32](../../acr-ui/src/utils/reviewDisplay.js#L32) |
| 平台默认排除 | 默认排除 | 平台默认不检查这些文件 | 分类 | [acr-ui/src/utils/reviewDisplay.js:37](../../acr-ui/src/utils/reviewDisplay.js#L37) |
| 项目排除规则 | 项目排除 | 被项目设置排除的文件 | 分类 | [acr-ui/src/utils/reviewDisplay.js:38](../../acr-ui/src/utils/reviewDisplay.js#L38) |
| 测试文件 | 测试文件 | 因测试文件设置被排除 | 分类 | [acr-ui/src/utils/reviewDisplay.js:39](../../acr-ui/src/utils/reviewDisplay.js#L39) |
| 删除文件 | 已删文件 | 文件删除，只记录变更 | 分类 | [acr-ui/src/utils/reviewDisplay.js:43](../../acr-ui/src/utils/reviewDisplay.js#L43) |
| 二进制文件 | 非文本 | 无法按文本方式检查的文件 | 分类 | [acr-ui/src/utils/reviewDisplay.js:44](../../acr-ui/src/utils/reviewDisplay.js#L44) |
| 纯改名 | 只改名称 | 只修改名称，无正文变化 | 分类 | [acr-ui/src/utils/reviewDisplay.js:45](../../acr-ui/src/utils/reviewDisplay.js#L45) |
| 仅权限变更 | 只改权限 | 只修改文件访问或执行权限 | 分类 | [acr-ui/src/utils/reviewDisplay.js:47](../../acr-ui/src/utils/reviewDisplay.js#L47) |
| 空文件 | 空白文件 | 文件没有正文内容 | 分类 | [acr-ui/src/utils/reviewDisplay.js:48](../../acr-ui/src/utils/reviewDisplay.js#L48) |
| 权限/安全逻辑 | 安全相关 | 涉及权限或安全逻辑的文件 | 分类 | [acr-ui/src/utils/reviewDisplay.js:52](../../acr-ui/src/utils/reviewDisplay.js#L52) |
| 依赖声明 | 依赖清单 | 声明项目依赖的文件 | 分类 | [acr-ui/src/utils/reviewDisplay.js:53](../../acr-ui/src/utils/reviewDisplay.js#L53) |
| 数据库脚本 | 数据脚本 | 数据库变更的脚本文件 | 分类 | [acr-ui/src/utils/reviewDisplay.js:54](../../acr-ui/src/utils/reviewDisplay.js#L54) |
| 配置文件 | 设置文件 | 影响运行配置的文件 | 分类 | [acr-ui/src/utils/reviewDisplay.js:55](../../acr-ui/src/utils/reviewDisplay.js#L55) |
| 公共签名变更 | 接口变更 | 公共函数或接口声明发生变化 | 分类 | [acr-ui/src/utils/reviewDisplay.js:56](../../acr-ui/src/utils/reviewDisplay.js#L56) |
| 新增文件 | 新增文件 | 新建的文件 | 分类 | [acr-ui/src/utils/reviewDisplay.js:57](../../acr-ui/src/utils/reviewDisplay.js#L57) |
| 内容已在 Diff 中 | 已在变更 | 文件内容已在代码变更中 | 状态 | [acr-ui/src/utils/reviewDisplay.js:61](../../acr-ui/src/utils/reviewDisplay.js#L61) |
| 已纳入完整内容 | 完整补充 | 已补充文件完整内容 | 状态 | [acr-ui/src/utils/reviewDisplay.js:62](../../acr-ui/src/utils/reviewDisplay.js#L62) |
| 预算不足跳过 | 超限跳过 | 内容超出处理字数上限 | 状态 | [acr-ui/src/utils/reviewDisplay.js:63](../../acr-ui/src/utils/reviewDisplay.js#L63) |
| 拉取失败降级 | 补充失败 | 补充失败后仅按已取到内容检查 | 状态 | [acr-ui/src/utils/reviewDisplay.js:64](../../acr-ui/src/utils/reviewDisplay.js#L64) |
| 超拉取上限跳过 | 超限跳过 | 超出文件读取限制，未补充 | 状态 | [acr-ui/src/utils/reviewDisplay.js:65](../../acr-ui/src/utils/reviewDisplay.js#L65) |
| 未返回可读失败原因，请查看执行记录 | 未提供具体失败原因，请看运行记录 | 保留排查入口 | 提示 | [acr-ui/src/utils/reviewDisplay.js:77](../../acr-ui/src/utils/reviewDisplay.js#L77) |
| 本结论仅覆盖本次推送的变更（base..head 增量） | 结论仅覆盖本次推送的代码变更 | 保留推送检查范围 | 提示 | [acr-ui/src/utils/reviewDisplay.js:128](../../acr-ui/src/utils/reviewDisplay.js#L128) |
| 执行失败 | 检查失败 | 检查过程失败 | 状态 | [acr-ui/src/utils/reviewDisplay.js:158](../../acr-ui/src/utils/reviewDisplay.js#L158) |
| 通过 | 通过 | 检查结果通过 | 状态 | [acr-ui/src/utils/reviewDisplay.js:160](../../acr-ui/src/utils/reviewDisplay.js#L160) |
| 建议修改 | 建议修改 | 建议修改代码 | 状态 | [acr-ui/src/utils/reviewDisplay.js:161](../../acr-ui/src/utils/reviewDisplay.js#L161) |
| 高风险 | 高风险 | 不表示平台已阻止合并 | 状态 | [acr-ui/src/utils/reviewDisplay.js:162](../../acr-ui/src/utils/reviewDisplay.js#L162) |
| 已截断 | 内容截短 | 受字数上限影响，未检查全部内容 | 状态 | [acr-ui/src/utils/reviewDisplay.js:458](../../acr-ui/src/utils/reviewDisplay.js#L458) |
| 合并请求 | 合并请求 | 打开代码平台合并请求 | 字段 | [acr-ui/src/utils/reviewDisplay.js:239](../../acr-ui/src/utils/reviewDisplay.js#L239)<br>[acr-ui/src/utils/reviewDisplay.js:245](../../acr-ui/src/utils/reviewDisplay.js#L245) |
| 推送 | 代码推送 | 由提交代码触发检查 | 选项 | [acr-ui/src/utils/reviewDisplay.js:244](../../acr-ui/src/utils/reviewDisplay.js#L244) |

### 下发状态

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 待执行 | 待检查 | 等待开始检查 | 状态 | [sql/init-full.sql:1605](../../sql/init-full.sql#L1605) |
| 执行中 | 检查中 | 正在检查代码 | 状态 | [sql/init-full.sql:1606](../../sql/init-full.sql#L1606) |
| 已完成 | 已完成 | 检查完成不代表检查通过 | 状态 | [sql/init-full.sql:1607](../../sql/init-full.sql#L1607) |
| 已失败 | 检查失败 | 检查没有正常完成 | 状态 | [sql/init-full.sql:1608](../../sql/init-full.sql#L1608) |
| 已取消 | 已取消 | 任务已停止，不再检查 | 状态 | [sql/init-full.sql:1609](../../sql/init-full.sql#L1609) |
| 待重试 | 待重试 | 系统等待重新检查 | 状态 | [sql/init-full.sql:1678](../../sql/init-full.sql#L1678) |
| 已被替代 | 已有新版 | 已有更新任务，本条只保留历史 | 状态 | [sql/init-full.sql:1679](../../sql/init-full.sql#L1679) |
| 解析配置 | 读取设置 | 读取本次检查配置 | 状态 | [sql/init-full.sql:1623](../../sql/init-full.sql#L1623) |
| 准备工作区 | 准备代码 | 获取代码并准备检查目录 | 状态 | [sql/init-full.sql:1624](../../sql/init-full.sql#L1624)<br>[sql/init-full.sql:1737](../../sql/init-full.sql#L1737) |
| 调用审查引擎 | 运行工具 | 正在用配置的检查工具检查 | 状态 | [sql/init-full.sql:1625](../../sql/init-full.sql#L1625) |
| 调用大模型 | 调用模型 | 正在用所选模型检查 | 状态 | [sql/init-full.sql:1636](../../sql/init-full.sql#L1636) |
| 保存结果 | 保存结果 | 正在保存本次检查结果 | 状态 | [sql/init-full.sql:1626](../../sql/init-full.sql#L1626) |
| 已投递 | 已发送 | 评论或通知发送成功 | 状态 | [sql/init-full.sql:1645](../../sql/init-full.sql#L1645) |
| 自动重试中 | 自动重发 | 发送失败，系统将自动重试 | 状态 | [sql/init-full.sql:1646](../../sql/init-full.sql#L1646) |
| 待投递 | 待发送 | 等待发送评论或通知 | 状态 | [sql/init-full.sql:1683](../../sql/init-full.sql#L1683) |
| 待人工处置 | 需处理 | 需人工处理配置或发送失败 | 状态 | [sql/init-full.sql:1684](../../sql/init-full.sql#L1684)<br>[sql/init-full.sql:1724](../../sql/init-full.sql#L1724)<br>[sql/init-full.sql:1730](../../sql/init-full.sql#L1730) |
| 已跳过 | 已跳过 | 本次发送未执行 | 状态 | [sql/init-full.sql:1685](../../sql/init-full.sql#L1685) |
| 待修复通知配置 | 通知异常 | 先修正通知设置才能发送 | 状态 | [sql/init-full.sql:1686](../../sql/init-full.sql#L1686) |
| 待确认 | 待确认 | 人工判断是否需要修复 | 状态 | [sql/init-full.sql:1654](../../sql/init-full.sql#L1654) |
| 待修复 | 待修复 | 问题已确认，等待代码修复 | 状态 | [sql/init-full.sql:1655](../../sql/init-full.sql#L1655) |
| 疑似修复 | 待复核 | 疑似修复，仍需人工验证 | 状态 | [sql/init-full.sql:1659](../../sql/init-full.sql#L1659) |
| 已关闭 | 已关闭 | 结束跟进，不等于全部已修复 | 状态 | [sql/init-full.sql:1656](../../sql/init-full.sql#L1656) |
| 误报 | 误报 | 认为检测有误并结束跟进 | 状态 | [sql/init-full.sql:1658](../../sql/init-full.sql#L1658) |
| 钉钉机器人 | 钉钉通知 | 发送到钉钉群机器人 | 选项 | [sql/init-full.sql:1647](../../sql/init-full.sql#L1647)<br>[sql/init-full.sql:1651](../../sql/init-full.sql#L1651) |
| 企微机器人 | 企微通知 | 发送到企业微信群机器人 | 选项 | [sql/init-full.sql:1648](../../sql/init-full.sql#L1648)<br>[sql/init-full.sql:1652](../../sql/init-full.sql#L1652) |
| 飞书机器人 | 飞书通知 | 发送到飞书群机器人 | 选项 | [sql/init-full.sql:1649](../../sql/init-full.sql#L1649)<br>[sql/init-full.sql:1653](../../sql/init-full.sql#L1653) |
| 配置缺失 | 缺少设置 | 检查需要的设置不完整 | 状态 | [sql/init-full.sql:1627](../../sql/init-full.sql#L1627) |
| 凭据错误 | 密钥错误 | 访问代码平台的授权不可用 | 状态 | [sql/init-full.sql:1628](../../sql/init-full.sql#L1628) |
| 工作区准备失败 | 准备失败 | 未能取到代码或准备检查目录 | 状态 | [sql/init-full.sql:1629](../../sql/init-full.sql#L1629) |
| 引擎超时 | 运行超时 | 检查工具未在限定时间内完成 | 状态 | [sql/init-full.sql:1630](../../sql/init-full.sql#L1630) |
| 引擎调用失败 | 工具失败 | 检查工具未能正常运行 | 状态 | [sql/init-full.sql:1631](../../sql/init-full.sql#L1631) |
| 模型调用失败 | 模型失败 | 未能正常调用模型 | 状态 | [sql/init-full.sql:1632](../../sql/init-full.sql#L1632) |
| 并发超限 | 任务过多 | 同时运行的任务超过限制 | 状态 | [sql/init-full.sql:1633](../../sql/init-full.sql#L1633) |
| 未知错误 | 未知错误 | 暂无明确的失败原因 | 状态 | [sql/init-full.sql:1634](../../sql/init-full.sql#L1634) |
| 结果格式异常 | 结果异常 | 模型结果格式不符合要求 | 状态 | [sql/init-full.sql:1643](../../sql/init-full.sql#L1643) |
| API 限流 | 请求过多 | 代码平台暂时限制访问请求 | 状态 | [sql/init-full.sql:1644](../../sql/init-full.sql#L1644) |
| 依赖暂不可用 | 暂不可用 | 外部服务或网络暂不可用 | 状态 | [sql/init-full.sql:1678](../../sql/init-full.sql#L1678)<br>[sql/init-full.sql:1680](../../sql/init-full.sql#L1680) |
| 执行租约过期 | 执行中断 | 执行服务中断或失联，等待恢复 | 状态 | [sql/init-full.sql:1681](../../sql/init-full.sql#L1681) |
| 执行节点停机 | 节点停机 | 执行服务已停止，等待其他服务接管 | 状态 | [sql/init-full.sql:1682](../../sql/init-full.sql#L1682) |
| 任务回写 | 检查完成 | 检查完成后发送结果 | 选项 | [sql/init-full.sql:1664](../../sql/init-full.sql#L1664) |
| 问题处置 | 问题处理 | 问题处理后更新评论或通知 | 选项 | [sql/init-full.sql:1665](../../sql/init-full.sql#L1665) |
| 手动重试 | 人工重发 | 人工重新发送评论或通知 | 选项 | [sql/init-full.sql:1666](../../sql/init-full.sql#L1666) |
| 任务失败通知 | 检查失败 | 检查任务失败后发送通知 | 选项 | [sql/init-full.sql:1691](../../sql/init-full.sql#L1691) |
| 逾期提醒 | 超期提醒 | 提醒处理超期问题 | 选项 | [sql/init-full.sql:1692](../../sql/init-full.sql#L1692) |
| 合并请求 | 合并请求 | 由合并请求触发检查 | 选项 | [sql/init-full.sql:1676](../../sql/init-full.sql#L1676) |
| 推送 | 代码推送 | 由提交代码触发检查 | 选项 | [sql/init-full.sql:1677](../../sql/init-full.sql#L1677) |
| 新增 | 新增问题 | 本次代码变更引入的问题 | 选项 | [sql/init-full.sql:1594](../../sql/init-full.sql#L1594)<br>[sql/init-full.sql:1662](../../sql/init-full.sql#L1662) |
| 存量 | 已有问题 | 本次变更之前已有的问题 | 选项 | [sql/init-full.sql:1663](../../sql/init-full.sql#L1663) |

### 业务记录

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 操作人员 | 操作人 | 执行该项操作的用户 | 名称 | [acr-ui/src/views/system/audit/index.vue:10](../../acr-ui/src/views/system/audit/index.vue#L10) |
| 时间 | 时间 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/audit/index.vue:24](../../acr-ui/src/views/system/audit/index.vue#L24) |
| 对象 | 对象 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/audit/index.vue:27](../../acr-ui/src/views/system/audit/index.vue#L27) |
| 对象名称 | 对象名称 | 操作涉及的业务对象名称 | 名称 | [acr-ui/src/views/system/audit/index.vue:28](../../acr-ui/src/views/system/audit/index.vue#L28) |
| 原因 | 原因 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/audit/index.vue:29](../../acr-ui/src/views/system/audit/index.vue#L29) |
| 操作类型 | 操作类型 | 本次执行的业务动作 | 名称 | [acr-ui/src/views/system/audit/index.vue:4](../../acr-ui/src/views/system/audit/index.vue#L4) |
| 业务审计详情 | 操作详情 | 查看业务变更前后的记录 | 名称 | [acr-ui/src/views/system/audit/index.vue:45](../../acr-ui/src/views/system/audit/index.vue#L45) |
| 事件标识 | 事件编号 | 识别同一次业务变更 | 名称 | [acr-ui/src/views/system/audit/index.vue:47](../../acr-ui/src/views/system/audit/index.vue#L47) |
| 来源 | 来源 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/audit/index.vue:48](../../acr-ui/src/views/system/audit/index.vue#L48) |
| 对象 ID | 对象编号 | 操作涉及的业务记录编号 | 名称 | [acr-ui/src/views/system/audit/index.vue:49](../../acr-ui/src/views/system/audit/index.vue#L49) |
| 操作时间 | 操作时间 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/audit/index.vue:50](../../acr-ui/src/views/system/audit/index.vue#L50) |
| 操作原因 | 操作原因 | 此次业务操作的原因 | 名称 | [acr-ui/src/views/system/audit/index.vue:51](../../acr-ui/src/views/system/audit/index.vue#L51) |
| 前值 | 修改前 | 本次变更前的内容 | 名称 | [acr-ui/src/views/system/audit/index.vue:55](../../acr-ui/src/views/system/audit/index.vue#L55) |
| 后值 | 修改后 | 本次变更后的内容 | 名称 | [acr-ui/src/views/system/audit/index.vue:59](../../acr-ui/src/views/system/audit/index.vue#L59) |
| 关联对象 | 关联记录 | 同次操作涉及的其他记录 | 名称 | [acr-ui/src/views/system/audit/index.vue:63](../../acr-ui/src/views/system/audit/index.vue#L63) |
| 对象类型 | 对象类型 | 操作涉及的业务对象类别 | 名称 | [acr-ui/src/views/system/audit/index.vue:7](../../acr-ui/src/views/system/audit/index.vue#L7) |

### 主机状态

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 属性 | 属性 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/server/index.vue:11](../../acr-ui/src/views/monitor/server/index.vue#L11) |
| 启动时间 | 启动时间 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/server/index.vue:114](../../acr-ui/src/views/monitor/server/index.vue#L114) |
| 运行时长 | 运行时长 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/server/index.vue:116](../../acr-ui/src/views/monitor/server/index.vue#L116) |
| 安装路径 | 安装路径 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/server/index.vue:120](../../acr-ui/src/views/monitor/server/index.vue#L120) |
| 项目路径 | 项目路径 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/server/index.vue:124](../../acr-ui/src/views/monitor/server/index.vue#L124) |
| 运行参数 | 运行参数 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/server/index.vue:128](../../acr-ui/src/views/monitor/server/index.vue#L128) |
| 磁盘状态 | 磁盘状态 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/server/index.vue:139](../../acr-ui/src/views/monitor/server/index.vue#L139) |
| 盘符路径 | 磁盘位置 | 磁盘挂载的路径 | 名称 | [acr-ui/src/views/monitor/server/index.vue:144](../../acr-ui/src/views/monitor/server/index.vue#L144) |
| 文件系统 | 文件系统 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/server/index.vue:145](../../acr-ui/src/views/monitor/server/index.vue#L145) |
| 盘符类型 | 盘符类型 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/server/index.vue:146](../../acr-ui/src/views/monitor/server/index.vue#L146) |
| 总大小 | 总空间 | 磁盘的总容量 | 名称 | [acr-ui/src/views/monitor/server/index.vue:147](../../acr-ui/src/views/monitor/server/index.vue#L147) |
| 可用大小 | 剩余空间 | 磁盘当前可用容量 | 名称 | [acr-ui/src/views/monitor/server/index.vue:148](../../acr-ui/src/views/monitor/server/index.vue#L148) |
| 已用大小 | 已用空间 | 磁盘已经占用的容量 | 名称 | [acr-ui/src/views/monitor/server/index.vue:149](../../acr-ui/src/views/monitor/server/index.vue#L149) |
| 已用百分比 | 占用比例 | 磁盘空间已使用的比例 | 名称 | [acr-ui/src/views/monitor/server/index.vue:150](../../acr-ui/src/views/monitor/server/index.vue#L150) |
| 核心数 | 核心数 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/server/index.vue:17](../../acr-ui/src/views/monitor/server/index.vue#L17) |
| 用户使用率 | 程序占用 | 用户程序使用的处理器比例 | 名称 | [acr-ui/src/views/monitor/server/index.vue:21](../../acr-ui/src/views/monitor/server/index.vue#L21) |
| 系统使用率 | 系统占用 | 操作系统使用的处理器比例 | 名称 | [acr-ui/src/views/monitor/server/index.vue:25](../../acr-ui/src/views/monitor/server/index.vue#L25) |
| 当前空闲率 | 空闲比例 | 处理器当前空闲的比例 | 名称 | [acr-ui/src/views/monitor/server/index.vue:29](../../acr-ui/src/views/monitor/server/index.vue#L29) |
| 内存 | 内存 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/server/index.vue:40](../../acr-ui/src/views/monitor/server/index.vue#L40) |
| 总内存 | 总内存 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/server/index.vue:52](../../acr-ui/src/views/monitor/server/index.vue#L52) |
| 已用内存 | 已用内存 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/server/index.vue:57](../../acr-ui/src/views/monitor/server/index.vue#L57) |
| 剩余内存 | 剩余内存 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/server/index.vue:62](../../acr-ui/src/views/monitor/server/index.vue#L62) |
| 使用率 | 使用率 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/server/index.vue:67](../../acr-ui/src/views/monitor/server/index.vue#L67) |
| 服务器信息 | 主机信息 | 查看运行平台的服务器 | 名称 | [acr-ui/src/views/monitor/server/index.vue:79](../../acr-ui/src/views/monitor/server/index.vue#L79) |
| 服务器名称 | 主机名称 | 服务器的主机名 | 名称 | [acr-ui/src/views/monitor/server/index.vue:84](../../acr-ui/src/views/monitor/server/index.vue#L84) |
| 操作系统 | 操作系统 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/server/index.vue:86](../../acr-ui/src/views/monitor/server/index.vue#L86) |
| 服务器IP | 主机地址 | 服务器的网络地址 | 名称 | [acr-ui/src/views/monitor/server/index.vue:90](../../acr-ui/src/views/monitor/server/index.vue#L90) |
| 系统架构 | 系统架构 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/server/index.vue:92](../../acr-ui/src/views/monitor/server/index.vue#L92) |

### 全局入口

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 公告详情 | 公告详情 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/layout/components/HeaderNotice/DetailView.vue:2](../../acr-ui/src/layout/components/HeaderNotice/DetailView.vue#L2) |
| 暂无公告 | 暂无公告 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/layout/components/HeaderNotice/index.vue:14](../../acr-ui/src/layout/components/HeaderNotice/index.vue#L14) |
| 通知公告 | 通知公告 | 查看系统通知和公告 | 名称 | [acr-ui/src/layout/components/HeaderNotice/index.vue:6](../../acr-ui/src/layout/components/HeaderNotice/index.vue#L6) |
| 全部已读 | 全部已读 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/layout/components/HeaderNotice/index.vue:7](../../acr-ui/src/layout/components/HeaderNotice/index.vue#L7) |
| 主题模式 | 主题模式 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/layout/components/Navbar.vue:20](../../acr-ui/src/layout/components/Navbar.vue#L20) |
| 布局大小 | 界面大小 | 调整页面元素的显示密度 | 名称 | [acr-ui/src/layout/components/Navbar.vue:27](../../acr-ui/src/layout/components/Navbar.vue#L27) |
| 消息通知 | 通知公告 | 查看系统发布的通知和公告 | 名称 | [acr-ui/src/layout/components/Navbar.vue:33](../../acr-ui/src/layout/components/Navbar.vue#L33) |
| 个人中心 | 个人资料 | 编辑个人资料和提交身份 | 名称 | [acr-ui/src/layout/components/Navbar.vue:46](../../acr-ui/src/layout/components/Navbar.vue#L46) |
| 布局设置 | 布局设置 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/layout/components/Navbar.vue:49](../../acr-ui/src/layout/components/Navbar.vue#L49) |
| 锁定屏幕 | 锁定屏幕 | 锁定当前登录界面 | 名称 | [acr-ui/src/layout/components/Navbar.vue:52](../../acr-ui/src/layout/components/Navbar.vue#L52) |
| 退出登录 | 退出登录 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/layout/components/Navbar.vue:55](../../acr-ui/src/layout/components/Navbar.vue#L55) |
| 动态标题 | 页面标题 | 浏览器标题跟随当前页面 | 名称 | [acr-ui/src/layout/components/Settings/index.vue:105](../../acr-ui/src/layout/components/Settings/index.vue#L105) |
| 底部版权 | 版权信息 | 在页面底部显示版权信息 | 名称 | [acr-ui/src/layout/components/Settings/index.vue:112](../../acr-ui/src/layout/components/Settings/index.vue#L112) |
| 保存配置 | 保存设置 | 将当前界面设置存到本机 | 名称 | [acr-ui/src/layout/components/Settings/index.vue:120](../../acr-ui/src/layout/components/Settings/index.vue#L120) |
| 重置配置 | 恢复默认 | 清除本机设置并恢复默认 | 名称 | [acr-ui/src/layout/components/Settings/index.vue:121](../../acr-ui/src/layout/components/Settings/index.vue#L121) |
| 混合菜单 | 顶部侧栏 | 同时显示顶部和侧边菜单 | 名称 | [acr-ui/src/layout/components/Settings/index.vue:13](../../acr-ui/src/layout/components/Settings/index.vue#L13) |
| 顶部菜单 | 顶部菜单 | 在顶部显示菜单 | 名称 | [acr-ui/src/layout/components/Settings/index.vue:18](../../acr-ui/src/layout/components/Settings/index.vue#L18) |
| 主题风格设置 | 界面风格 | 选择当前界面的样式 | 名称 | [acr-ui/src/layout/components/Settings/index.vue:25](../../acr-ui/src/layout/components/Settings/index.vue#L25) |
| 菜单导航设置 | 菜单位置 | 选择侧边或顶部菜单 | 名称 | [acr-ui/src/layout/components/Settings/index.vue:4](../../acr-ui/src/layout/components/Settings/index.vue#L4) |
| 主题颜色 | 主题颜色 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/layout/components/Settings/index.vue:50](../../acr-ui/src/layout/components/Settings/index.vue#L50) |
| 系统布局配置 | 页面布局 | 调整页面与导航的显示方式 | 名称 | [acr-ui/src/layout/components/Settings/index.vue:57](../../acr-ui/src/layout/components/Settings/index.vue#L57) |
| 开启页签 | 开启页签 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/layout/components/Settings/index.vue:60](../../acr-ui/src/layout/components/Settings/index.vue#L60) |
| 持久化标签页 | 记住页签 | 再次打开时恢复已开页签 | 名称 | [acr-ui/src/layout/components/Settings/index.vue:67](../../acr-ui/src/layout/components/Settings/index.vue#L67) |
| 左侧菜单 | 左侧菜单 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/layout/components/Settings/index.vue:7](../../acr-ui/src/layout/components/Settings/index.vue#L7) |
| 显示页签图标 | 显示图标 | 在页签中显示页面图标 | 名称 | [acr-ui/src/layout/components/Settings/index.vue:74](../../acr-ui/src/layout/components/Settings/index.vue#L74) |
| 标签页样式 | 页签样式 | 选择页签的显示样式 | 名称 | [acr-ui/src/layout/components/Settings/index.vue:81](../../acr-ui/src/layout/components/Settings/index.vue#L81) |
| 卡片 | 卡片 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/layout/components/Settings/index.vue:84](../../acr-ui/src/layout/components/Settings/index.vue#L84) |
| 谷歌 | 谷歌 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/layout/components/Settings/index.vue:85](../../acr-ui/src/layout/components/Settings/index.vue#L85) |
| 固定 Header | 固定页头 | 滚动时保留顶部导航 | 名称 | [acr-ui/src/layout/components/Settings/index.vue:91](../../acr-ui/src/layout/components/Settings/index.vue#L91) |
| 显示 Logo | 显示标志 | 显示平台标志 | 名称 | [acr-ui/src/layout/components/Settings/index.vue:98](../../acr-ui/src/layout/components/Settings/index.vue#L98) |
| 关闭当前 | 关闭当前 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/layout/components/TagsView/index.vue:41](../../acr-ui/src/layout/components/TagsView/index.vue#L41) |
| 关闭其他 | 关闭其他 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/layout/components/TagsView/index.vue:42](../../acr-ui/src/layout/components/TagsView/index.vue#L42) |
| 关闭左侧 | 关闭左侧 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/layout/components/TagsView/index.vue:43](../../acr-ui/src/layout/components/TagsView/index.vue#L43) |
| 关闭右侧 | 关闭右侧 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/layout/components/TagsView/index.vue:44](../../acr-ui/src/layout/components/TagsView/index.vue#L44) |
| 全部关闭 | 全部关闭 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/layout/components/TagsView/index.vue:45](../../acr-ui/src/layout/components/TagsView/index.vue#L45) |
| 全屏显示 | 全屏显示 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/layout/components/TagsView/index.vue:47](../../acr-ui/src/layout/components/TagsView/index.vue#L47) |
| 退出全屏 | 退出全屏 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/layout/components/TagsView/index.vue:48](../../acr-ui/src/layout/components/TagsView/index.vue#L48) |
| 刷新页面 | 刷新页面 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/layout/components/TagsView/index.vue:55](../../acr-ui/src/layout/components/TagsView/index.vue#L55) |
| 更多菜单 | 更多菜单 | 打开未展开的导航菜单 | 名称 | [acr-ui/src/layout/components/TopBar/index.vue:7](../../acr-ui/src/layout/components/TopBar/index.vue#L7) |

### 在线用户

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 用户名称 | 登录账号 | 用于登录的账号名称 | 名称 | [acr-ui/src/views/monitor/online/index.vue:13](../../acr-ui/src/views/monitor/online/index.vue#L13) |
| 序号 | 序号 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/online/index.vue:32](../../acr-ui/src/views/monitor/online/index.vue#L32) |
| 会话编号 | 登录编号 | 识别当前的登录会话 | 名称 | [acr-ui/src/views/monitor/online/index.vue:37](../../acr-ui/src/views/monitor/online/index.vue#L37) |
| 登录名称 | 登录账号 | 用于登录的账号名称 | 名称 | [acr-ui/src/views/monitor/online/index.vue:38](../../acr-ui/src/views/monitor/online/index.vue#L38) |
| 所属部门 | 所属部门 | 用户归属的组织部门 | 名称 | [acr-ui/src/views/monitor/online/index.vue:39](../../acr-ui/src/views/monitor/online/index.vue#L39) |
| 登录地址 | 来源地址 | 登录设备的网络地址 | 名称 | [acr-ui/src/views/monitor/online/index.vue:4](../../acr-ui/src/views/monitor/online/index.vue#L4) |
| 主机 | 来源地址 | 当前登录设备的网络地址 | 名称 | [acr-ui/src/views/monitor/online/index.vue:40](../../acr-ui/src/views/monitor/online/index.vue#L40) |
| 登录地点 | 登录地点 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/online/index.vue:41](../../acr-ui/src/views/monitor/online/index.vue#L41) |
| 操作系统 | 操作系统 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/online/index.vue:42](../../acr-ui/src/views/monitor/online/index.vue#L42) |
| 浏览器 | 浏览器 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/online/index.vue:43](../../acr-ui/src/views/monitor/online/index.vue#L43) |
| 登录时间 | 登录时间 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/online/index.vue:44](../../acr-ui/src/views/monitor/online/index.vue#L44) |
| 强退 | 退出账号 | 强制结束该用户当前登录 | 名称 | [acr-ui/src/views/monitor/online/index.vue:51](../../acr-ui/src/views/monitor/online/index.vue#L51) |

### 定时任务

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 暂停 | 暂停 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/detail.vue:104](../../acr-ui/src/views/monitor/job/detail.vue#L104) |
| 下次执行时间 | 下次执行 | 定时任务下一次执行时间 | 名称 | [acr-ui/src/views/monitor/job/detail.vue:119](../../acr-ui/src/views/monitor/job/detail.vue#L119) |
| 日志编号 | 日志编号 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/detail.vue:12](../../acr-ui/src/views/monitor/job/detail.vue#L12) |
| 执行策略 | 错过执行 | 任务错过时间后的处理方式 | 名称 | [acr-ui/src/views/monitor/job/detail.vue:125](../../acr-ui/src/views/monitor/job/detail.vue#L125) |
| 默认策略 | 默认策略 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/detail.vue:126](../../acr-ui/src/views/monitor/job/detail.vue#L126) |
| 立即执行 | 立即执行 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/detail.vue:127](../../acr-ui/src/views/monitor/job/detail.vue#L127) |
| 执行一次 | 执行一次 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/detail.vue:128](../../acr-ui/src/views/monitor/job/detail.vue#L128) |
| 放弃执行 | 跳过执行 | 跳过错过时间的本次任务 | 名称 | [acr-ui/src/views/monitor/job/detail.vue:129](../../acr-ui/src/views/monitor/job/detail.vue#L129) |
| 并发执行 | 同时执行 | 是否允许同一任务同时执行 | 名称 | [acr-ui/src/views/monitor/job/detail.vue:134](../../acr-ui/src/views/monitor/job/detail.vue#L134) |
| 允许 | 允许 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/detail.vue:135](../../acr-ui/src/views/monitor/job/detail.vue#L135) |
| 禁止 | 禁止 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/detail.vue:136](../../acr-ui/src/views/monitor/job/detail.vue#L136) |
| 创建人 | 创建人 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/detail.vue:157](../../acr-ui/src/views/monitor/job/detail.vue#L157) |
| 执行状态 | 执行状态 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/detail.vue:16](../../acr-ui/src/views/monitor/job/detail.vue#L16) |
| 更新人 | 更新人 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/detail.vue:165](../../acr-ui/src/views/monitor/job/detail.vue#L165) |
| 更新时间 | 更新时间 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/detail.vue:168](../../acr-ui/src/views/monitor/job/detail.vue#L168) |
| 关 闭 | 关 闭 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/detail.vue:181](../../acr-ui/src/views/monitor/job/detail.vue#L181) |
| 开始时间 | 开始时间 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/detail.vue:24](../../acr-ui/src/views/monitor/job/detail.vue#L24) |
| 结束时间 | 结束时间 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/detail.vue:27](../../acr-ui/src/views/monitor/job/detail.vue#L27) |
| 记录时间 | 记录时间 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/detail.vue:32](../../acr-ui/src/views/monitor/job/detail.vue#L32) |
| 执行耗时 | 执行耗时 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/detail.vue:35](../../acr-ui/src/views/monitor/job/detail.vue#L35) |
| 任务名称 | 任务名称 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/detail.vue:46](../../acr-ui/src/views/monitor/job/detail.vue#L46) |
| 任务分组 | 任务分组 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/detail.vue:50](../../acr-ui/src/views/monitor/job/detail.vue#L50) |
| 日志信息 | 日志信息 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/detail.vue:57](../../acr-ui/src/views/monitor/job/detail.vue#L57) |
| 任务编号 | 任务编号 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/detail.vue:87](../../acr-ui/src/views/monitor/job/detail.vue#L87) |
| 调用目标字符串 | 执行方法 | 系统实际调用的方法与参数 | 名称 | [acr-ui/src/views/monitor/job/index.vue:103](../../acr-ui/src/views/monitor/job/index.vue#L103) |
| 调度日志 | 执行记录 | 查看定时任务的执行结果 | 名称 | [acr-ui/src/views/monitor/job/index.vue:126](../../acr-ui/src/views/monitor/job/index.vue#L126) |
| 任务组名 | 任务分组 | 任务所属的定时分组 | 名称 | [acr-ui/src/views/monitor/job/index.vue:13](../../acr-ui/src/views/monitor/job/index.vue#L13) |
| 调用方法 | 执行方法 | 系统实际调用的方法与参数 | 名称 | [acr-ui/src/views/monitor/job/index.vue:166](../../acr-ui/src/views/monitor/job/index.vue#L166) |
| 调用目标应指向已注册的 Spring Bean 方法 | 填写已注册的任务方法 | 仅后台已注册的方法可调用 | 提示 | [acr-ui/src/views/monitor/job/index.vue:170](../../acr-ui/src/views/monitor/job/index.vue#L170) |
| 生成表达式 | 设置时间 | 打开定时执行时间编辑器 | 名称 | [acr-ui/src/views/monitor/job/index.vue:186](../../acr-ui/src/views/monitor/job/index.vue#L186) |
| 是否并发 | 同时执行 | 是否允许同一任务同时执行 | 名称 | [acr-ui/src/views/monitor/job/index.vue:214](../../acr-ui/src/views/monitor/job/index.vue#L214) |
| 确 定 | 确 定 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/index.vue:225](../../acr-ui/src/views/monitor/job/index.vue#L225) |
| 取 消 | 取 消 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/index.vue:226](../../acr-ui/src/views/monitor/job/index.vue#L226) |
| 任务状态 | 任务状态 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/index.vue:23](../../acr-ui/src/views/monitor/job/index.vue#L23) |
| 添加任务 | 添加任务 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/index.vue:380](../../acr-ui/src/views/monitor/job/index.vue#L380) |
| 修改任务 | 修改任务 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/index.vue:390](../../acr-ui/src/views/monitor/job/index.vue#L390) |
| 日志 | 日志 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/index.vue:85](../../acr-ui/src/views/monitor/job/index.vue#L85) |
| 详细 | 详细 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/log.vue:122](../../acr-ui/src/views/monitor/job/log.vue#L122) |
| 执行时间 | 执行时间 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/log.vue:43](../../acr-ui/src/views/monitor/job/log.vue#L43) |
| 清空 | 清空 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/log.vue:77](../../acr-ui/src/views/monitor/job/log.vue#L77) |

### 岗位设置

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 岗位顺序 | 显示顺序 | 数值越小，位置越靠前 | 名称 | [acr-ui/src/views/system/post/index.vue:121](../../acr-ui/src/views/system/post/index.vue#L121) |
| 岗位状态 | 岗位状态 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/post/index.vue:124](../../acr-ui/src/views/system/post/index.vue#L124) |
| 岗位名称 | 岗位名称 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/post/index.vue:13](../../acr-ui/src/views/system/post/index.vue#L13) |
| 确 定 | 确 定 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/post/index.vue:139](../../acr-ui/src/views/system/post/index.vue#L139) |
| 取 消 | 取 消 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/post/index.vue:140](../../acr-ui/src/views/system/post/index.vue#L140) |
| 添加岗位 | 添加岗位 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/post/index.vue:233](../../acr-ui/src/views/system/post/index.vue#L233) |
| 修改岗位 | 修改岗位 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/post/index.vue:243](../../acr-ui/src/views/system/post/index.vue#L243) |
| 岗位编码 | 岗位代码 | 系统识别岗位的唯一代码 | 名称 | [acr-ui/src/views/system/post/index.vue:4](../../acr-ui/src/views/system/post/index.vue#L4) |
| 岗位编号 | 岗位编号 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/post/index.vue:82](../../acr-ui/src/views/system/post/index.vue#L82) |
| 岗位排序 | 显示顺序 | 数值越小，位置越靠前 | 名称 | [acr-ui/src/views/system/post/index.vue:85](../../acr-ui/src/views/system/post/index.vue#L85) |

### 工作台

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 审查结果概览 | 检查概况 | 查看近七天检查结论 | 名称 | [acr-ui/src/views/index/components/ConclusionOverview.vue:4](../../acr-ui/src/views/index/components/ConclusionOverview.vue#L4) |
| 暂无启用的模型服务 | 暂无启用模型 | 添加或启用模型 | 提示 | [acr-ui/src/views/index/components/ModelHealth.vue:12](../../acr-ui/src/views/index/components/ModelHealth.vue#L12) |
| 去配置 | 设置模型 | 添加或调整模型连接 | 名称 | [acr-ui/src/views/index/components/ModelHealth.vue:13](../../acr-ui/src/views/index/components/ModelHealth.vue#L13) |
| 个启用 | 个启用 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/index/components/ModelHealth.vue:17](../../acr-ui/src/views/index/components/ModelHealth.vue#L17) |
| 最近检测 | 最近检查 | 最近一次模型连接检查 | 名称 | [acr-ui/src/views/index/components/ModelHealth.vue:20](../../acr-ui/src/views/index/components/ModelHealth.vue#L20) |
| 默认 | 默认 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/index/components/ModelHealth.vue:32](../../acr-ui/src/views/index/components/ModelHealth.vue#L32) |
| 模型运行情况 | 模型状态 | 查看最近一次连接检查 | 名称 | [acr-ui/src/views/index/components/ModelHealth.vue:4](../../acr-ui/src/views/index/components/ModelHealth.vue#L4) |
| 管理 | 管理 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/index/components/ModelHealth.vue:5](../../acr-ui/src/views/index/components/ModelHealth.vue#L5) |
| 模型状态加载失败 | 模型状态加载失败，请重试 | 重新获取模型状态 | 提示 | [acr-ui/src/views/index/components/ModelHealth.vue:8](../../acr-ui/src/views/index/components/ModelHealth.vue#L8) |
| 接入项目 | 接入项目 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/index/components/QuickActions.vue:35](../../acr-ui/src/views/index/components/QuickActions.vue#L35) |
| 新增代码仓库与审查范围 | 添加仓库，选择检查范围 | 接入代码项目 | 提示 | [acr-ui/src/views/index/components/QuickActions.vue:35](../../acr-ui/src/views/index/components/QuickActions.vue#L35) |
| 问题台账 | 问题跟进 | 查看问题并跟进处理 | 名称 | [acr-ui/src/views/index/components/QuickActions.vue:36](../../acr-ui/src/views/index/components/QuickActions.vue#L36) |
| 确认、修复与关闭问题 | 确认问题，跟进修复 | 查看问题的处理状态 | 提示 | [acr-ui/src/views/index/components/QuickActions.vue:36](../../acr-ui/src/views/index/components/QuickActions.vue#L36) |
| 审查任务 | 检查任务 | 查看执行进度和失败原因 | 名称 | [acr-ui/src/views/index/components/QuickActions.vue:37](../../acr-ui/src/views/index/components/QuickActions.vue#L37) |
| 执行队列与失败重试 | 查看进度，重试失败任务 | 执行状态与失败原因 | 提示 | [acr-ui/src/views/index/components/QuickActions.vue:37](../../acr-ui/src/views/index/components/QuickActions.vue#L37) |
| 审查记录 | 检查记录 | 查看已完成的检查 | 名称 | [acr-ui/src/views/index/components/QuickActions.vue:38](../../acr-ui/src/views/index/components/QuickActions.vue#L38) |
| 已完成审查的结论与评分 | 查看检查结论和评分 | 已完成的检查结果 | 提示 | [acr-ui/src/views/index/components/QuickActions.vue:38](../../acr-ui/src/views/index/components/QuickActions.vue#L38) |
| 投递记录 | 发送记录 | 查看评论和通知的发送结果 | 名称 | [acr-ui/src/views/index/components/QuickActions.vue:39](../../acr-ui/src/views/index/components/QuickActions.vue#L39) |
| 评论回写与通知投递状态 | 查看评论和通知的发送结果 | 区分各次发送结果 | 提示 | [acr-ui/src/views/index/components/QuickActions.vue:39](../../acr-ui/src/views/index/components/QuickActions.vue#L39) |
| 快捷操作 | 常用操作 | 直接打开常用功能 | 名称 | [acr-ui/src/views/index/components/QuickActions.vue:4](../../acr-ui/src/views/index/components/QuickActions.vue#L4) |
| 模型服务 | 模型设置 | 设置模型并检查连接 | 名称 | [acr-ui/src/views/index/components/QuickActions.vue:40](../../acr-ui/src/views/index/components/QuickActions.vue#L40) |
| 模型连接与可用性管理 | 设置模型并检查连接 | 模型名称不等于可用状态 | 提示 | [acr-ui/src/views/index/components/QuickActions.vue:40](../../acr-ui/src/views/index/components/QuickActions.vue#L40) |
| 最近动态 | 最近动态 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/index/components/RecentActivity.vue:4](../../acr-ui/src/views/index/components/RecentActivity.vue#L4) |
| 查看全部 | 查看全部 | 打开完整记录列表 | 名称 | [acr-ui/src/views/index/components/RecentActivity.vue:5](../../acr-ui/src/views/index/components/RecentActivity.vue#L5) |
| 新增任务 | 新增任务 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/index/components/TodaySummary.vue:13](../../acr-ui/src/views/index/components/TodaySummary.vue#L13) |
| 成功任务 | 执行成功 | 任务执行成功的数量 | 名称 | [acr-ui/src/views/index/components/TodaySummary.vue:17](../../acr-ui/src/views/index/components/TodaySummary.vue#L17) |
| 失败任务 | 执行失败 | 任务执行失败的数量 | 名称 | [acr-ui/src/views/index/components/TodaySummary.vue:21](../../acr-ui/src/views/index/components/TodaySummary.vue#L21) |
| 关闭问题 | 已关问题 | 今日关闭的问题数 | 名称 | [acr-ui/src/views/index/components/TodaySummary.vue:25](../../acr-ui/src/views/index/components/TodaySummary.vue#L25) |
| 审查任务状态 | 今日任务 | 查看今日任务状态 | 名称 | [acr-ui/src/views/index/components/TodaySummary.vue:4](../../acr-ui/src/views/index/components/TodaySummary.vue#L4) |
| 今日 | 今日 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/index/components/TodaySummary.vue:5](../../acr-ui/src/views/index/components/TodaySummary.vue#L5) |
| 加载失败 | 加载失败 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/index/components/TodaySummary.vue:8](../../acr-ui/src/views/index/components/TodaySummary.vue#L8) |
| 暂无待办事项 | 暂无待办 | 当前没有待办事项 | 名称 | [acr-ui/src/views/index/components/TodoCards.vue:12](../../acr-ui/src/views/index/components/TodoCards.vue#L12) |
| 今日待办 | 今日待办 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/index/components/TodoCards.vue:4](../../acr-ui/src/views/index/components/TodoCards.vue#L4) |
| 点击卡片直达对应列表 | 点击卡片，查看对应事项 | 打开卡片对应列表 | 提示 | [acr-ui/src/views/index/components/TodoCards.vue:5](../../acr-ui/src/views/index/components/TodoCards.vue#L5) |
| 项目风险趋势 | 风险趋势 | 按完成日期查看检查结论 | 名称 | [acr-ui/src/views/index/components/TrendChart.vue:4](../../acr-ui/src/views/index/components/TrendChart.vue#L4) |
| 接入第一个项目 | 接入项目 | 添加首个项目并开始检查 | 名称 | [acr-ui/src/views/index/index.vue:12](../../acr-ui/src/views/index/index.vue#L12) |
| 当前账号没有项目接入权限，请联系管理员创建代码项目。 | 请联系管理员接入项目 | 当前账号无接入权限 | 提示 | [acr-ui/src/views/index/index.vue:13](../../acr-ui/src/views/index/index.vue#L13) |
| 工作台 | 工作台 | 查看待办和最近检查 | 名称 | [acr-ui/src/views/index/index.vue:20](../../acr-ui/src/views/index/index.vue#L20) |
| 最近任务 | 最近任务 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/index/index.vue:26](../../acr-ui/src/views/index/index.vue#L26) |
| 接入第一个项目才能开始审查 | 先接入项目，再开始检查 | 添加首个项目 | 提示 | [acr-ui/src/views/index/index.vue:7](../../acr-ui/src/views/index/index.vue#L7) |

### 成员统计

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 去关联 | 关联邮箱 | 关联用于提交代码的身份 | 名称 | [acr-ui/src/views/insight/member/index.vue:107](../../acr-ui/src/views/insight/member/index.vue#L107) |
| 被审任务 | 检查次数 | 与该成员关联的检查任务数 | 名称 | [acr-ui/src/views/insight/member/index.vue:119](../../acr-ui/src/views/insight/member/index.vue#L119) |
| 新增行数 | 新增行数 | 成功检查中新增的代码行数 | 名称 | [acr-ui/src/views/insight/member/index.vue:120](../../acr-ui/src/views/insight/member/index.vue#L120) |
| 删减行数 | 删除行数 | 成功检查中删除的代码行数 | 名称 | [acr-ui/src/views/insight/member/index.vue:121](../../acr-ui/src/views/insight/member/index.vue#L121) |
| 关联新增问题 | 新增问题 | 与该成员关联的新增问题数 | 名称 | [acr-ui/src/views/insight/member/index.vue:122](../../acr-ui/src/views/insight/member/index.vue#L122) |
| 未关闭问题 | 待办问题 | 该成员关联的未关闭问题 | 名称 | [acr-ui/src/views/insight/member/index.vue:123](../../acr-ui/src/views/insight/member/index.vue#L123) |
| 我的提交趋势 | 提交趋势 | 查看自己的代码提交数量 | 名称 | [acr-ui/src/views/insight/member/index.vue:126](../../acr-ui/src/views/insight/member/index.vue#L126) |
| 我的未关闭问题 | 我的待办 | 查看关联的未关闭问题 | 名称 | [acr-ui/src/views/insight/member/index.vue:130](../../acr-ui/src/views/insight/member/index.vue#L130) |
| 标题 | 标题 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/insight/member/index.vue:132](../../acr-ui/src/views/insight/member/index.vue#L132) |
| 成员提交趋势 | 提交趋势 | 查看选中成员的提交变化 | 名称 | [acr-ui/src/views/insight/member/index.vue:153](../../acr-ui/src/views/insight/member/index.vue#L153) |
| 提交次数 | 提交次数 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/insight/member/index.vue:178](../../acr-ui/src/views/insight/member/index.vue#L178) |
| 被审变更数 | 检查次数 | 与成员关联的检查任务数 | 名称 | [acr-ui/src/views/insight/member/index.vue:179](../../acr-ui/src/views/insight/member/index.vue#L179) |
| 关联问题数 | 关联问题 | 与该成员关联的问题数 | 名称 | [acr-ui/src/views/insight/member/index.vue:190](../../acr-ui/src/views/insight/member/index.vue#L190) |
| 未关闭问题数 | 待办问题 | 成员关联的未关闭问题数 | 名称 | [acr-ui/src/views/insight/member/index.vue:191](../../acr-ui/src/views/insight/member/index.vue#L191) |
| 未关联成员 | 未关联 | 提交身份尚未关联平台账号 | 名称 | [acr-ui/src/views/insight/member/index.vue:196](../../acr-ui/src/views/insight/member/index.vue#L196) |
| 以下提交身份尚未关联到平台账号 | 以下提交身份尚未关联账号 | 可由管理员关联 | 提示 | [acr-ui/src/views/insight/member/index.vue:197](../../acr-ui/src/views/insight/member/index.vue#L197) |
| 提交身份 | 提交身份 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/insight/member/index.vue:200](../../acr-ui/src/views/insight/member/index.vue#L200) |
| 指派 | 关联账号 | 把提交身份关联到平台用户 | 名称 | [acr-ui/src/views/insight/member/index.vue:214](../../acr-ui/src/views/insight/member/index.vue#L214) |
| 指派提交邮箱 | 关联账号 | 关联提交身份与平台用户 | 名称 | [acr-ui/src/views/insight/member/index.vue:221](../../acr-ui/src/views/insight/member/index.vue#L221) |
| 业务系统 | 业务系统 | 按项目所属业务系统筛选 | 名称 | [acr-ui/src/views/insight/member/index.vue:23](../../acr-ui/src/views/insight/member/index.vue#L23) |
| 成员数 | 成员数 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/insight/member/index.vue:311](../../acr-ui/src/views/insight/member/index.vue#L311) |
| 时间范围 | 日期范围 | 选择需要查看的日期 | 名称 | [acr-ui/src/views/insight/member/index.vue:4](../../acr-ui/src/views/insight/member/index.vue#L4) |
| 项目 | 项目 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/insight/member/index.vue:40](../../acr-ui/src/views/insight/member/index.vue#L40) |
| 成员 | 成员 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/insight/member/index.vue:50](../../acr-ui/src/views/insight/member/index.vue#L50) |
| 本人视图 | 我的数据 | 仅查看已关联身份的数据 | 名称 | [acr-ui/src/views/insight/member/index.vue:78](../../acr-ui/src/views/insight/member/index.vue#L78) |
| 团队视图 | 团队数据 | 查看权限范围内的成员数据 | 名称 | [acr-ui/src/views/insight/member/index.vue:79](../../acr-ui/src/views/insight/member/index.vue#L79) |
| 自定义 | 自定义 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/insight/member/index.vue:8](../../acr-ui/src/views/insight/member/index.vue#L8) |
| 本人视图仅展示已关联的提交身份；团队视图按授权范围统计 | 本人需关联邮箱，团队按权限显示 | 查看范围受权限控制 | 提示 | [acr-ui/src/views/insight/member/index.vue:81](../../acr-ui/src/views/insight/member/index.vue#L81) |
| 用于团队管理与辅导，不作为绩效评价直接输入 | 数据用于团队辅导，不用于绩效评价 | 指标不能直接评价个人绩效 | 提示 | [acr-ui/src/views/insight/member/index.vue:90](../../acr-ui/src/views/insight/member/index.vue#L90) |
| 加载失败 | 加载失败 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/insight/member/index.vue:95](../../acr-ui/src/views/insight/member/index.vue#L95) |

### 报告占位

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 当前版本未开放报告中心 | 报告暂未开放 | 当前入口隐藏且停用 | 提示 | [acr-ui/src/views/insight/report/placeholder.vue:3](../../acr-ui/src/views/insight/report/placeholder.vue#L3) |
| 企业版报告能力尚未开放 | 报告暂未开放 | 周月报告尚未交付 | 提示 | [acr-ui/src/views/insight/report/placeholder.vue:5](../../acr-ui/src/views/insight/report/placeholder.vue#L5) |
| 不可变周/月快照、审计导出、订阅与发送记录尚未交付，本入口默认隐藏并停用。当前请使用总览看板与项目分析查看准实时指标。 | 报告暂未开放，可查看质量统计 | 入口继续隐藏并停用 | 提示 | [acr-ui/src/views/insight/report/placeholder.vue:6](../../acr-ui/src/views/insight/report/placeholder.vue#L6) |
| 前往总览看板 | 质量总览 | 查看已有质量统计 | 名称 | [acr-ui/src/views/insight/report/placeholder.vue:8](../../acr-ui/src/views/insight/report/placeholder.vue#L8) |

### 操作记录

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 异常信息 | 失败原因 | 执行时产生的错误内容 | 名称 | [acr-ui/src/views/monitor/operlog/detail.vue:104](../../acr-ui/src/views/monitor/operlog/detail.vue#L104) |
| 业务类型 | 操作类型 | 本次操作的业务动作类别 | 名称 | [acr-ui/src/views/monitor/operlog/detail.vue:12](../../acr-ui/src/views/monitor/operlog/detail.vue#L12) |
| 操作时间 | 操作时间 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/operlog/detail.vue:17](../../acr-ui/src/views/monitor/operlog/detail.vue#L17) |
| 操作日志详细 | 操作详情 | 查看本次操作的完整记录 | 名称 | [acr-ui/src/views/monitor/operlog/detail.vue:2](../../acr-ui/src/views/monitor/operlog/detail.vue#L2) |
| 执行状态 | 执行状态 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/operlog/detail.vue:21](../../acr-ui/src/views/monitor/operlog/detail.vue#L21) |
| 操作人员 | 操作人 | 执行该项操作的用户 | 名称 | [acr-ui/src/views/monitor/operlog/detail.vue:31](../../acr-ui/src/views/monitor/operlog/detail.vue#L31) |
| 所属部门 | 所属部门 | 用户归属的组织部门 | 名称 | [acr-ui/src/views/monitor/operlog/detail.vue:37](../../acr-ui/src/views/monitor/operlog/detail.vue#L37) |
| 操作地址 | 来源地址 | 执行操作的客户端地址 | 名称 | [acr-ui/src/views/monitor/operlog/detail.vue:43](../../acr-ui/src/views/monitor/operlog/detail.vue#L43) |
| 请求信息 | 请求信息 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/operlog/detail.vue:52](../../acr-ui/src/views/monitor/operlog/detail.vue#L52) |
| 请求地址 | 请求地址 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/operlog/detail.vue:56](../../acr-ui/src/views/monitor/operlog/detail.vue#L56) |
| 基本信息 | 基本信息 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/operlog/detail.vue:6](../../acr-ui/src/views/monitor/operlog/detail.vue#L6) |
| 操作方法 | 操作方法 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/operlog/detail.vue:66](../../acr-ui/src/views/monitor/operlog/detail.vue#L66) |
| 消耗时间 | 耗时 | 本次操作使用的时间 | 名称 | [acr-ui/src/views/monitor/operlog/detail.vue:71](../../acr-ui/src/views/monitor/operlog/detail.vue#L71) |
| 请求参数 | 请求参数 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/operlog/detail.vue:78](../../acr-ui/src/views/monitor/operlog/detail.vue#L78) |
| 操作模块 | 功能名称 | 本次操作涉及的功能 | 名称 | [acr-ui/src/views/monitor/operlog/detail.vue:9](../../acr-ui/src/views/monitor/operlog/detail.vue#L9) |
| 返回参数 | 返回内容 | 接口返回的数据 | 名称 | [acr-ui/src/views/monitor/operlog/detail.vue:91](../../acr-ui/src/views/monitor/operlog/detail.vue#L91) |
| 日志编号 | 日志编号 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/operlog/index.vue:112](../../acr-ui/src/views/monitor/operlog/index.vue#L112) |
| 操作类型 | 操作类型 | 本次执行的业务动作 | 名称 | [acr-ui/src/views/monitor/operlog/index.vue:114](../../acr-ui/src/views/monitor/operlog/index.vue#L114) |
| 操作状态 | 操作状态 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/operlog/index.vue:121](../../acr-ui/src/views/monitor/operlog/index.vue#L121) |
| 操作日期 | 操作日期 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/operlog/index.vue:126](../../acr-ui/src/views/monitor/operlog/index.vue#L126) |
| 系统模块 | 功能名称 | 本次操作涉及的功能 | 名称 | [acr-ui/src/views/monitor/operlog/index.vue:13](../../acr-ui/src/views/monitor/operlog/index.vue#L13) |
| 详细 | 详细 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/operlog/index.vue:138](../../acr-ui/src/views/monitor/operlog/index.vue#L138) |
| 清空 | 清空 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/operlog/index.vue:96](../../acr-ui/src/views/monitor/operlog/index.vue#L96) |

### 模型用量

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 厂商 | 厂商 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/insight/token/index.vue:106](../../acr-ui/src/views/insight/token/index.vue#L106) |
| 调用次数 | 调用次数 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/insight/token/index.vue:107](../../acr-ui/src/views/insight/token/index.vue#L107) |
| 输入 | 输入用量 | 发送给模型的计费用量 | 名称 | [acr-ui/src/views/insight/token/index.vue:108](../../acr-ui/src/views/insight/token/index.vue#L108) |
| 输出 | 输出用量 | 模型生成内容的计费用量 | 名称 | [acr-ui/src/views/insight/token/index.vue:111](../../acr-ui/src/views/insight/token/index.vue#L111) |
| 总 Token | 总用量 | 输入与输出的计费用量合计 | 名称 | [acr-ui/src/views/insight/token/index.vue:114](../../acr-ui/src/views/insight/token/index.vue#L114) |
| 估算成本 | 预估费用 | 按当前单价估算，非实际账单 | 名称 | [acr-ui/src/views/insight/token/index.vue:117](../../acr-ui/src/views/insight/token/index.vue#L117) |
| 占比 | 占比 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/insight/token/index.vue:120](../../acr-ui/src/views/insight/token/index.vue#L120) |
| 项目排行 | 用量排序 | 按模型用量排列项目 | 名称 | [acr-ui/src/views/insight/token/index.vue:131](../../acr-ui/src/views/insight/token/index.vue#L131) |
| 项目维度 | 项目明细 | 按项目查看调用和用量 | 名称 | [acr-ui/src/views/insight/token/index.vue:137](../../acr-ui/src/views/insight/token/index.vue#L137) |
| 业务系统 | 业务系统 | 按项目所属业务系统筛选 | 名称 | [acr-ui/src/views/insight/token/index.vue:144](../../acr-ui/src/views/insight/token/index.vue#L144) |
| 负责人 | 负责人 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/insight/token/index.vue:145](../../acr-ui/src/views/insight/token/index.vue#L145) |
| 审查明细 | 用量明细 | 查看每次检查的模型用量 | 名称 | [acr-ui/src/views/insight/token/index.vue:159](../../acr-ui/src/views/insight/token/index.vue#L159) |
| 记录ID | 任务编号 | 打开该次检查的任务 | 名称 | [acr-ui/src/views/insight/token/index.vue:161](../../acr-ui/src/views/insight/token/index.vue#L161) |
| 审查方式 | 检查方式 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/insight/token/index.vue:168](../../acr-ui/src/views/insight/token/index.vue#L168) |
| 触发时间 | 触发时间 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/insight/token/index.vue:173](../../acr-ui/src/views/insight/token/index.vue#L173) |
| 模型 | 模型 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/insight/token/index.vue:21](../../acr-ui/src/views/insight/token/index.vue#L21) |
| 尚未采集到 Token 用量。完成一次大模型审查后刷新本页。 | 完成一次模型检查后刷新 | 采集开启前的历史用量不会回填 | 提示 | [acr-ui/src/views/insight/token/index.vue:248](../../acr-ui/src/views/insight/token/index.vue#L248) |
| 项目 | 项目 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/insight/token/index.vue:31](../../acr-ui/src/views/insight/token/index.vue#L31) |
| 时间范围 | 日期范围 | 选择需要查看的日期 | 名称 | [acr-ui/src/views/insight/token/index.vue:4](../../acr-ui/src/views/insight/token/index.vue#L4) |
| Token 用量加载失败 | 模型用量加载失败，请重试 | 重新获取模型用量 | 提示 | [acr-ui/src/views/insight/token/index.vue:55](../../acr-ui/src/views/insight/token/index.vue#L55) |
| 自定义 | 自定义 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/insight/token/index.vue:8](../../acr-ui/src/views/insight/token/index.vue#L8) |
| Token 趋势 | 用量趋势 | 按日期查看输入和输出用量 | 名称 | [acr-ui/src/views/insight/token/index.vue:86](../../acr-ui/src/views/insight/token/index.vue#L86) |
| 模型占比 | 模型占比 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/insight/token/index.vue:93](../../acr-ui/src/views/insight/token/index.vue#L93) |
| 模型维度 | 模型明细 | 按模型查看调用和用量 | 名称 | [acr-ui/src/views/insight/token/index.vue:99](../../acr-ui/src/views/insight/token/index.vue#L99) |

### 用户设置

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 登录账号 | 登录账号 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/authRole.vue:12](../../acr-ui/src/views/system/user/authRole.vue#L12) |
| 角色信息 | 角色信息 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/authRole.vue:19](../../acr-ui/src/views/system/user/authRole.vue#L19) |
| 序号 | 序号 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/authRole.vue:21](../../acr-ui/src/views/system/user/authRole.vue#L21) |
| 角色编号 | 角色编号 | 角色的唯一编号 | 名称 | [acr-ui/src/views/system/user/authRole.vue:27](../../acr-ui/src/views/system/user/authRole.vue#L27) |
| 角色名称 | 角色名称 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/authRole.vue:28](../../acr-ui/src/views/system/user/authRole.vue#L28) |
| 权限字符 | 角色标识 | 代码中引用角色的标识 | 名称 | [acr-ui/src/views/system/user/authRole.vue:29](../../acr-ui/src/views/system/user/authRole.vue#L29) |
| 基本信息 | 基本信息 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/authRole.vue:3](../../acr-ui/src/views/system/user/authRole.vue#L3) |
| 提交 | 提交 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/authRole.vue:41](../../acr-ui/src/views/system/user/authRole.vue#L41) |
| 返回 | 返回 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/authRole.vue:42](../../acr-ui/src/views/system/user/authRole.vue#L42) |
| 用户昵称 | 昵称 | 页面中显示的用户称呼 | 名称 | [acr-ui/src/views/system/user/authRole.vue:7](../../acr-ui/src/views/system/user/authRole.vue#L7) |
| 手机号码 | 手机号 | 联系用户的手机号码 | 名称 | [acr-ui/src/views/system/user/index.vue:10](../../acr-ui/src/views/system/user/index.vue#L10) |
| 归属部门 | 所属部门 | 用户归属的组织部门 | 名称 | [acr-ui/src/views/system/user/index.vue:103](../../acr-ui/src/views/system/user/index.vue#L103) |
| 邮箱 | 邮箱 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/index.vue:115](../../acr-ui/src/views/system/user/index.vue#L115) |
| 用户密码 | 登录密码 | 账号登录时使用的密码 | 名称 | [acr-ui/src/views/system/user/index.vue:127](../../acr-ui/src/views/system/user/index.vue#L127) |
| 用户性别 | 性别 | 账号资料中的性别 | 名称 | [acr-ui/src/views/system/user/index.vue:134](../../acr-ui/src/views/system/user/index.vue#L134) |
| 岗位 | 岗位 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/index.vue:150](../../acr-ui/src/views/system/user/index.vue#L150) |
| 角色 | 角色 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/index.vue:157](../../acr-ui/src/views/system/user/index.vue#L157) |
| 确 定 | 确 定 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/index.vue:174](../../acr-ui/src/views/system/user/index.vue#L174) |
| 取 消 | 取 消 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/index.vue:175](../../acr-ui/src/views/system/user/index.vue#L175) |
| 用户导入 | 导入用户 | 通过表格批量添加用户 | 名称 | [acr-ui/src/views/system/user/index.vue:183](../../acr-ui/src/views/system/user/index.vue#L183) |
| 组织机构 | 部门列表 | 按部门查找用户 | 名称 | [acr-ui/src/views/system/user/index.vue:3](../../acr-ui/src/views/system/user/index.vue#L3) |
| 修改成功，新密码是： | 密码已更新 | 更新成功后无需再次展示密码 | 提示 | [acr-ui/src/views/system/user/index.vue:356](../../acr-ui/src/views/system/user/index.vue#L356) |
| 导入 | 导入 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/index.vue:38](../../acr-ui/src/views/system/user/index.vue#L38) |
| 添加用户 | 添加用户 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/index.vue:410](../../acr-ui/src/views/system/user/index.vue#L410) |
| 修改用户 | 修改用户 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/index.vue:426](../../acr-ui/src/views/system/user/index.vue#L426) |
| 用户编号 | 用户编号 | 账号的唯一编号 | 名称 | [acr-ui/src/views/system/user/index.vue:48](../../acr-ui/src/views/system/user/index.vue#L48) |
| 部门 | 部门 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/index.vue:55](../../acr-ui/src/views/system/user/index.vue#L55) |
| 用户名称 | 登录账号 | 用于登录的账号名称 | 名称 | [acr-ui/src/views/system/user/index.vue:7](../../acr-ui/src/views/system/user/index.vue#L7) |
| 重置密码 | 重置密码 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/index.vue:80](../../acr-ui/src/views/system/user/index.vue#L80) |
| 分配角色 | 设置角色 | 为用户选择角色 | 名称 | [acr-ui/src/views/system/user/index.vue:83](../../acr-ui/src/views/system/user/index.vue#L83) |
| 移除 | 移除 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/profile/commitEmail.vue:14](../../acr-ui/src/views/system/user/profile/commitEmail.vue#L14) |
| 建议关联 | 建议关联 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/profile/commitEmail.vue:19](../../acr-ui/src/views/system/user/profile/commitEmail.vue#L19) |
| 确认关联 | 确认关联 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/profile/commitEmail.vue:26](../../acr-ui/src/views/system/user/profile/commitEmail.vue#L26) |
| 关联你用这些邮箱提交的代码，即可查看自己的审查数据。 | 关联提交邮箱，查看个人检查数据 | 支持提交邮箱或代码平台名称 | 提示 | [acr-ui/src/views/system/user/profile/commitEmail.vue:3](../../acr-ui/src/views/system/user/profile/commitEmail.vue#L3) |
| 手动添加 | 手动添加 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/profile/commitEmail.vue:31](../../acr-ui/src/views/system/user/profile/commitEmail.vue#L31) |
| 添加 | 添加 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/profile/commitEmail.vue:40](../../acr-ui/src/views/system/user/profile/commitEmail.vue#L40) |
| 我的 IM 账号 | 聊天账号 | 用于群内提醒时提到你 | 名称 | [acr-ui/src/views/system/user/profile/commitEmail.vue:48](../../acr-ui/src/views/system/user/profile/commitEmail.vue#L48) |
| 用于审查总结和逾期提醒在群里 @ 到你。取不到对应渠道身份时，消息里只显示姓名。 | 填好账号可在群内提醒中提到你 | 缺少渠道身份时仅显示姓名 | 提示 | [acr-ui/src/views/system/user/profile/commitEmail.vue:49](../../acr-ui/src/views/system/user/profile/commitEmail.vue#L49) |
| 已关联 | 已关联 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/profile/commitEmail.vue:6](../../acr-ui/src/views/system/user/profile/commitEmail.vue#L6) |
| 钉钉 | 钉钉 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/profile/commitEmail.vue:87](../../acr-ui/src/views/system/user/profile/commitEmail.vue#L87) |
| 企业微信 | 企业微信 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/profile/commitEmail.vue:88](../../acr-ui/src/views/system/user/profile/commitEmail.vue#L88) |
| 飞书 | 飞书 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/profile/commitEmail.vue:89](../../acr-ui/src/views/system/user/profile/commitEmail.vue#L89) |
| 基本资料 | 个人资料 | 编辑昵称和联系方式 | 名称 | [acr-ui/src/views/system/user/profile/index.vue:48](../../acr-ui/src/views/system/user/profile/index.vue#L48) |
| 修改密码 | 修改密码 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/profile/index.vue:55](../../acr-ui/src/views/system/user/profile/index.vue#L55) |
| 我的提交邮箱 | 提交邮箱 | 关联你提交代码时使用的身份 | 名称 | [acr-ui/src/views/system/user/profile/index.vue:58](../../acr-ui/src/views/system/user/profile/index.vue#L58) |
| 个人信息 | 个人资料 | 查看当前账号的信息 | 名称 | [acr-ui/src/views/system/user/profile/index.vue:8](../../acr-ui/src/views/system/user/profile/index.vue#L8) |
| 旧密码 | 旧密码 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/profile/resetPwd.vue:3](../../acr-ui/src/views/system/user/profile/resetPwd.vue#L3) |
| 新密码 | 新密码 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/profile/resetPwd.vue:6](../../acr-ui/src/views/system/user/profile/resetPwd.vue#L6) |
| 确认密码 | 确认密码 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/profile/resetPwd.vue:9](../../acr-ui/src/views/system/user/profile/resetPwd.vue#L9) |
| 选择 | 选择 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/profile/userAvatar.vue:36](../../acr-ui/src/views/system/user/profile/userAvatar.vue#L36) |
| 提 交 | 提 交 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/profile/userAvatar.vue:54](../../acr-ui/src/views/system/user/profile/userAvatar.vue#L54) |
| 性别 | 性别 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/profile/userInfo.vue:12](../../acr-ui/src/views/system/user/profile/userInfo.vue#L12) |
| 用户信息详情 | 用户资料 | 查看账号和联系信息 | 名称 | [acr-ui/src/views/system/user/view.vue:2](../../acr-ui/src/views/system/user/view.vue#L2) |
| 用户状态： | 用户状态 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/view.vue:43](../../acr-ui/src/views/system/user/view.vue#L43) |
| 其他信息 | 其他信息 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/view.vue:73](../../acr-ui/src/views/system/user/view.vue#L73) |
| 创建者： | 创建者 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/view.vue:77](../../acr-ui/src/views/system/user/view.vue#L77) |
| 更新者： | 更新者 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/view.vue:91](../../acr-ui/src/views/system/user/view.vue#L91) |
| 更新时间： | 更新时间 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/view.vue:97](../../acr-ui/src/views/system/user/view.vue#L97) |

### 登录记录

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 地址 | 地址 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/logininfor/index.vue:100](../../acr-ui/src/views/monitor/logininfor/index.vue#L100) |
| 登录地点 | 登录地点 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/logininfor/index.vue:101](../../acr-ui/src/views/monitor/logininfor/index.vue#L101) |
| 操作系统 | 操作系统 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/logininfor/index.vue:102](../../acr-ui/src/views/monitor/logininfor/index.vue#L102) |
| 浏览器 | 浏览器 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/logininfor/index.vue:103](../../acr-ui/src/views/monitor/logininfor/index.vue#L103) |
| 登录状态 | 登录状态 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/logininfor/index.vue:104](../../acr-ui/src/views/monitor/logininfor/index.vue#L104) |
| 描述 | 描述 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/logininfor/index.vue:109](../../acr-ui/src/views/monitor/logininfor/index.vue#L109) |
| 访问时间 | 访问时间 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/logininfor/index.vue:110](../../acr-ui/src/views/monitor/logininfor/index.vue#L110) |
| 用户名称 | 登录账号 | 用于登录的账号名称 | 名称 | [acr-ui/src/views/monitor/logininfor/index.vue:13](../../acr-ui/src/views/monitor/logininfor/index.vue#L13) |
| 登录时间 | 登录时间 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/logininfor/index.vue:37](../../acr-ui/src/views/monitor/logininfor/index.vue#L37) |
| 登录地址 | 来源地址 | 登录设备的网络地址 | 名称 | [acr-ui/src/views/monitor/logininfor/index.vue:4](../../acr-ui/src/views/monitor/logininfor/index.vue#L4) |
| 清空 | 清空 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/logininfor/index.vue:72](../../acr-ui/src/views/monitor/logininfor/index.vue#L72) |
| 解锁 | 解除锁定 | 解除账号登录错误次数限制 | 名称 | [acr-ui/src/views/monitor/logininfor/index.vue:82](../../acr-ui/src/views/monitor/logininfor/index.vue#L82) |
| 访问编号 | 记录编号 | 登录记录的唯一编号 | 名称 | [acr-ui/src/views/monitor/logininfor/index.vue:98](../../acr-ui/src/views/monitor/logininfor/index.vue#L98) |

### 系统参数

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 参数主键 | 设置编号 | 参数记录的唯一编号 | 名称 | [acr-ui/src/views/system/config/index.vue:101](../../acr-ui/src/views/system/config/index.vue#L101) |
| 参数键值 | 设置内容 | 当前参数的值 | 名称 | [acr-ui/src/views/system/config/index.vue:104](../../acr-ui/src/views/system/config/index.vue#L104) |
| 参数键名 | 设置代码 | 系统读取该项参数的代码 | 名称 | [acr-ui/src/views/system/config/index.vue:13](../../acr-ui/src/views/system/config/index.vue#L13) |
| 确 定 | 确 定 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/config/index.vue:159](../../acr-ui/src/views/system/config/index.vue#L159) |
| 取 消 | 取 消 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/config/index.vue:160](../../acr-ui/src/views/system/config/index.vue#L160) |
| 系统内置 | 系统预设 | 此设置是否由系统内置 | 名称 | [acr-ui/src/views/system/config/index.vue:22](../../acr-ui/src/views/system/config/index.vue#L22) |
| 添加参数 | 添加参数 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/config/index.vue:255](../../acr-ui/src/views/system/config/index.vue#L255) |
| 修改参数 | 修改参数 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/config/index.vue:265](../../acr-ui/src/views/system/config/index.vue#L265) |
| 参数名称 | 设置名称 | 该项参数在页面中的名称 | 名称 | [acr-ui/src/views/system/config/index.vue:4](../../acr-ui/src/views/system/config/index.vue#L4) |
| 刷新缓存 | 更新缓存 | 重新载入当前设置 | 名称 | [acr-ui/src/views/system/config/index.vue:94](../../acr-ui/src/views/system/config/index.vue#L94) |

### 统计指标

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 待确认问题 | 待确认 | 等待确认的新增问题 | 名称 | [acr-review/src/main/java/com/acr/review/domain/WorkbenchConstants.java:15](../../acr-review/src/main/java/com/acr/review/domain/WorkbenchConstants.java#L15) |
| 存量待确认 | 旧问题 | 等待确认的存量代码问题 | 名称 | [acr-review/src/main/java/com/acr/review/domain/WorkbenchConstants.java:16](../../acr-review/src/main/java/com/acr/review/domain/WorkbenchConstants.java#L16) |
| 待修复问题 | 待修复 | 已确认并等待修复的问题 | 名称 | [acr-review/src/main/java/com/acr/review/domain/WorkbenchConstants.java:17](../../acr-review/src/main/java/com/acr/review/domain/WorkbenchConstants.java#L17) |
| 指派给我 | 我的待办 | 分配给我的当前待办 | 名称 | [acr-review/src/main/java/com/acr/review/domain/WorkbenchConstants.java:18](../../acr-review/src/main/java/com/acr/review/domain/WorkbenchConstants.java#L18) |
| 疑似修复问题 | 待复核 | 疑似修复，仍需验证 | 名称 | [acr-review/src/main/java/com/acr/review/domain/WorkbenchConstants.java:19](../../acr-review/src/main/java/com/acr/review/domain/WorkbenchConstants.java#L19) |
| 高风险结论 | 高风险 | 近七天的高风险检查结论 | 名称 | [acr-review/src/main/java/com/acr/review/domain/WorkbenchConstants.java:20](../../acr-review/src/main/java/com/acr/review/domain/WorkbenchConstants.java#L20) |
| 失败任务 | 执行失败 | 任务未执行成功，不是代码不合格 | 名称 | [acr-review/src/main/java/com/acr/review/domain/WorkbenchConstants.java:21](../../acr-review/src/main/java/com/acr/review/domain/WorkbenchConstants.java#L21) |
| 待处理投递 | 待处理 | 需要人工处理的发送记录 | 名称 | [acr-review/src/main/java/com/acr/review/domain/WorkbenchConstants.java:22](../../acr-review/src/main/java/com/acr/review/domain/WorkbenchConstants.java#L22) |
| 修复待验证 | 疑似修复，等待复核 | 尚未确认修复成功 | 提示 | [acr-review/src/main/java/com/acr/review/domain/WorkbenchConstants.java:28](../../acr-review/src/main/java/com/acr/review/domain/WorkbenchConstants.java#L28) |
| 有效审查覆盖率 | 检查覆盖 | 已审且已送达的非空任务/接收事件 | 名称 | [acr-review/src/main/java/com/acr/review/insight/InsightMetrics.java:60](../../acr-review/src/main/java/com/acr/review/insight/InsightMetrics.java#L60) |
| 审查成功率 | 成功率 | 执行成功数/成功和失败的任务数 | 名称 | [acr-review/src/main/java/com/acr/review/insight/InsightMetrics.java:62](../../acr-review/src/main/java/com/acr/review/insight/InsightMetrics.java#L62) |
| 未处置重点问题数 | 重点待办 | 当前未关闭的严重和高风险问题 | 名称 | [acr-review/src/main/java/com/acr/review/insight/InsightMetrics.java:66](../../acr-review/src/main/java/com/acr/review/insight/InsightMetrics.java#L66) |
| 新增问题数 | 新增问题 | 统计期内新产生的问题 | 名称 | [acr-review/src/main/java/com/acr/review/insight/InsightMetrics.java:68](../../acr-review/src/main/java/com/acr/review/insight/InsightMetrics.java#L68) |
| 处置率 | 处理比例 | 确认加关闭动作数除以新增数 | 名称 | [acr-review/src/main/java/com/acr/review/insight/InsightMetrics.java:70](../../acr-review/src/main/java/com/acr/review/insight/InsightMetrics.java#L70) |
| 误报率 | 误报比例 | 误报关闭数/全部关闭数 | 名称 | [acr-review/src/main/java/com/acr/review/insight/InsightMetrics.java:72](../../acr-review/src/main/java/com/acr/review/insight/InsightMetrics.java#L72) |
| 交付成功率 | 发送成功 | 发送成功数/已结束的发送次数 | 名称 | [acr-review/src/main/java/com/acr/review/insight/InsightMetrics.java:74](../../acr-review/src/main/java/com/acr/review/insight/InsightMetrics.java#L74) |
| 提交活跃度 | 提交次数 | 按日期、项目和身份统计提交 | 名称 | [acr-review/src/main/java/com/acr/review/insight/InsightMetrics.java:78](../../acr-review/src/main/java/com/acr/review/insight/InsightMetrics.java#L78) |
| 新增行数 | 新增行数 | 成功检查中新增的代码行数 | 名称 | [acr-review/src/main/java/com/acr/review/insight/InsightMetrics.java:80](../../acr-review/src/main/java/com/acr/review/insight/InsightMetrics.java#L80) |
| 删减行数 | 删除行数 | 成功检查中删除的代码行数 | 名称 | [acr-review/src/main/java/com/acr/review/insight/InsightMetrics.java:82](../../acr-review/src/main/java/com/acr/review/insight/InsightMetrics.java#L82) |
| 总 Token | 总用量 | 输入与输出的计费用量合计 | 名称 | [acr-review/src/main/java/com/acr/review/insight/TokenUsageInsightServiceImpl.java:75](../../acr-review/src/main/java/com/acr/review/insight/TokenUsageInsightServiceImpl.java#L75) |
| 输入 Token | 输入用量 | 发送给模型的计费用量 | 名称 | [acr-review/src/main/java/com/acr/review/insight/TokenUsageInsightServiceImpl.java:77](../../acr-review/src/main/java/com/acr/review/insight/TokenUsageInsightServiceImpl.java#L77) |
| 输出 Token | 输出用量 | 模型生成内容的计费用量 | 名称 | [acr-review/src/main/java/com/acr/review/insight/TokenUsageInsightServiceImpl.java:79](../../acr-review/src/main/java/com/acr/review/insight/TokenUsageInsightServiceImpl.java#L79) |
| 调用次数 | 调用次数 | 模型调用的累计次数 | 名称 | [acr-review/src/main/java/com/acr/review/insight/TokenUsageInsightServiceImpl.java:81](../../acr-review/src/main/java/com/acr/review/insight/TokenUsageInsightServiceImpl.java#L81) |
| 估算成本 | 预估费用 | 按当前单价估算，非实际账单 | 名称 | [acr-review/src/main/java/com/acr/review/insight/TokenUsageInsightServiceImpl.java:83](../../acr-review/src/main/java/com/acr/review/insight/TokenUsageInsightServiceImpl.java#L83) |

### 缓存状态

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 运行模式 | 运行模式 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/cache/index.vue:13](../../acr-ui/src/views/monitor/cache/index.vue#L13) |
| 端口 | 端口 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/cache/index.vue:15](../../acr-ui/src/views/monitor/cache/index.vue#L15) |
| 客户端数 | 客户端数 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/cache/index.vue:17](../../acr-ui/src/views/monitor/cache/index.vue#L17) |
| 运行时间(天) | 运行天数 | 缓存服务连续运行的天数 | 名称 | [acr-ui/src/views/monitor/cache/index.vue:21](../../acr-ui/src/views/monitor/cache/index.vue#L21) |
| 使用内存 | 使用内存 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/cache/index.vue:23](../../acr-ui/src/views/monitor/cache/index.vue#L23) |
| 内存配置 | 内存配置 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/cache/index.vue:27](../../acr-ui/src/views/monitor/cache/index.vue#L27) |
| 网络入口/出口 | 网络收发 | 分别显示网络接收和发送流量 | 名称 | [acr-ui/src/views/monitor/cache/index.vue:37](../../acr-ui/src/views/monitor/cache/index.vue#L37) |
| 命令统计 | 命令统计 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/cache/index.vue:48](../../acr-ui/src/views/monitor/cache/index.vue#L48) |
| 内存信息 | 内存占用 | 查看缓存服务使用的内存 | 名称 | [acr-ui/src/views/monitor/cache/index.vue:57](../../acr-ui/src/views/monitor/cache/index.vue#L57) |
| 基本信息 | 基本信息 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/cache/index.vue:6](../../acr-ui/src/views/monitor/cache/index.vue#L6) |
| 缓存内容 | 缓存内容 | 查看指定缓存条目的值 | 名称 | [acr-ui/src/views/monitor/cache/list.vue:117](../../acr-ui/src/views/monitor/cache/list.vue#L117) |
| 清理全部 | 清理全部 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/cache/list.vue:124](../../acr-ui/src/views/monitor/cache/list.vue#L124) |
| 序号 | 序号 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/cache/list.vue:25](../../acr-ui/src/views/monitor/cache/list.vue#L25) |
| 缓存名称 | 缓存名称 | 应用使用的缓存分类名称 | 名称 | [acr-ui/src/views/monitor/cache/list.vue:31](../../acr-ui/src/views/monitor/cache/list.vue#L31) |
| 键名列表 | 缓存条目 | 查看当前分类的缓存条目 | 名称 | [acr-ui/src/views/monitor/cache/list.vue:66](../../acr-ui/src/views/monitor/cache/list.vue#L66) |
| 缓存列表 | 缓存列表 | 查看应用使用的缓存分类 | 名称 | [acr-ui/src/views/monitor/cache/list.vue:7](../../acr-ui/src/views/monitor/cache/list.vue#L7) |
| 缓存键名 | 缓存代码 | 系统识别缓存条目的代码 | 名称 | [acr-ui/src/views/monitor/cache/list.vue:89](../../acr-ui/src/views/monitor/cache/list.vue#L89) |

### 菜单设置

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 上级菜单 | 上级菜单 | 选择该菜单所在的位置 | 名称 | [acr-ui/src/views/system/menu/index.vue:108](../../acr-ui/src/views/system/menu/index.vue#L108) |
| 菜单类型 | 菜单类型 | 目录、页面或操作按钮 | 名称 | [acr-ui/src/views/system/menu/index.vue:120](../../acr-ui/src/views/system/menu/index.vue#L120) |
| 菜单图标 | 菜单图标 | 菜单旁显示的图标 | 名称 | [acr-ui/src/views/system/menu/index.vue:129](../../acr-ui/src/views/system/menu/index.vue#L129) |
| 显示排序 | 显示顺序 | 数值越小，位置越靠前 | 名称 | [acr-ui/src/views/system/menu/index.vue:153](../../acr-ui/src/views/system/menu/index.vue#L153) |
| 路由名称 | 页面标识 | 系统识别页面的唯一名称 | 名称 | [acr-ui/src/views/system/menu/index.vue:169](../../acr-ui/src/views/system/menu/index.vue#L169) |
| 路由地址 | 页面地址 | 浏览器访问该页面的路径 | 名称 | [acr-ui/src/views/system/menu/index.vue:197](../../acr-ui/src/views/system/menu/index.vue#L197) |
| 权限字符 | 权限代码 | 接口校验使用的权限代码 | 名称 | [acr-ui/src/views/system/menu/index.vue:224](../../acr-ui/src/views/system/menu/index.vue#L224) |
| 路由参数 | 页面参数 | 打开页面时传入的默认参数 | 名称 | [acr-ui/src/views/system/menu/index.vue:237](../../acr-ui/src/views/system/menu/index.vue#L237) |
| 选择是则会被`keep-alive`缓存，需要匹配组件的`name`和地址保持一致 | 组件名须与页面名称一致 | 需配合保留页面开关使用 | 提示 | [acr-ui/src/views/system/menu/index.vue:246](../../acr-ui/src/views/system/menu/index.vue#L246) |
| 是否缓存 | 保留页面 | 切换页面时是否保留页面状态 | 名称 | [acr-ui/src/views/system/menu/index.vue:249](../../acr-ui/src/views/system/menu/index.vue#L249) |
| 缓存 | 缓存 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/menu/index.vue:253](../../acr-ui/src/views/system/menu/index.vue#L253) |
| 不缓存 | 不缓存 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/menu/index.vue:254](../../acr-ui/src/views/system/menu/index.vue#L254) |
| 选择隐藏则路由将不会出现在侧边栏，但仍然可以访问 | 隐藏菜单后，页面仍可访问 | 隐藏不等于禁止访问 | 提示 | [acr-ui/src/views/system/menu/index.vue:262](../../acr-ui/src/views/system/menu/index.vue#L262) |
| 显示状态 | 菜单显示 | 隐藏后仍可直接访问页面 | 名称 | [acr-ui/src/views/system/menu/index.vue:265](../../acr-ui/src/views/system/menu/index.vue#L265) |
| 选择停用则路由将不会出现在侧边栏，也不能被访问 | 停用后，菜单隐藏且页面不可访问 | 停用改变页面访问状态 | 提示 | [acr-ui/src/views/system/menu/index.vue:281](../../acr-ui/src/views/system/menu/index.vue#L281) |
| 菜单状态 | 菜单状态 | 停用后页面不能访问 | 名称 | [acr-ui/src/views/system/menu/index.vue:284](../../acr-ui/src/views/system/menu/index.vue#L284) |
| 确 定 | 确 定 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/menu/index.vue:300](../../acr-ui/src/views/system/menu/index.vue#L300) |
| 取 消 | 取 消 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/menu/index.vue:301](../../acr-ui/src/views/system/menu/index.vue#L301) |
| 菜单名称 | 菜单名称 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/menu/index.vue:4](../../acr-ui/src/views/system/menu/index.vue#L4) |
| 添加菜单 | 添加菜单 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/menu/index.vue:416](../../acr-ui/src/views/system/menu/index.vue#L416) |
| 修改菜单 | 修改菜单 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/menu/index.vue:435](../../acr-ui/src/views/system/menu/index.vue#L435) |
| 保存排序 | 保存排序 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/menu/index.vue:46](../../acr-ui/src/views/system/menu/index.vue#L46) |
| 展开/折叠 | 展开收起 | 切换整个树状列表的展开状态 | 名称 | [acr-ui/src/views/system/menu/index.vue:54](../../acr-ui/src/views/system/menu/index.vue#L54) |
| 外链 | 外链 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/menu/index.vue:75](../../acr-ui/src/views/system/menu/index.vue#L75) |
| 目录 | 目录 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/menu/index.vue:76](../../acr-ui/src/views/system/menu/index.vue#L76) |
| 菜单 | 菜单 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/menu/index.vue:78](../../acr-ui/src/views/system/menu/index.vue#L78) |
| 按钮 | 按钮 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/menu/index.vue:79](../../acr-ui/src/views/system/menu/index.vue#L79) |
| 排序 | 排序 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/menu/index.vue:82](../../acr-ui/src/views/system/menu/index.vue#L82) |
| 权限标识 | 权限代码 | 接口校验使用的权限代码 | 名称 | [acr-ui/src/views/system/menu/index.vue:87](../../acr-ui/src/views/system/menu/index.vue#L87) |
| 组件路径 | 页面文件 | 页面文件在系统中的路径 | 名称 | [acr-ui/src/views/system/menu/index.vue:88](../../acr-ui/src/views/system/menu/index.vue#L88) |

### 角色设置

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 手机号码 | 手机号 | 联系用户的手机号码 | 名称 | [acr-ui/src/views/system/role/authUser.vue:14](../../acr-ui/src/views/system/role/authUser.vue#L14) |
| 添加用户 | 添加用户 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/role/authUser.vue:37](../../acr-ui/src/views/system/role/authUser.vue#L37) |
| 批量取消授权 | 移除所选 | 移除所选用户的该项角色 | 名称 | [acr-ui/src/views/system/role/authUser.vue:47](../../acr-ui/src/views/system/role/authUser.vue#L47) |
| 用户名称 | 登录账号 | 用于登录的账号名称 | 名称 | [acr-ui/src/views/system/role/authUser.vue:5](../../acr-ui/src/views/system/role/authUser.vue#L5) |
| 用户昵称 | 昵称 | 页面中显示的用户称呼 | 名称 | [acr-ui/src/views/system/role/authUser.vue:63](../../acr-ui/src/views/system/role/authUser.vue#L63) |
| 邮箱 | 邮箱 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/role/authUser.vue:64](../../acr-ui/src/views/system/role/authUser.vue#L64) |
| 手机 | 手机 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/role/authUser.vue:65](../../acr-ui/src/views/system/role/authUser.vue#L65) |
| 取消授权 | 移除角色 | 取消该用户的当前角色 | 名称 | [acr-ui/src/views/system/role/authUser.vue:78](../../acr-ui/src/views/system/role/authUser.vue#L78) |
| 角色编号 | 角色编号 | 角色的唯一编号 | 名称 | [acr-ui/src/views/system/role/index.vue:103](../../acr-ui/src/views/system/role/index.vue#L103) |
| 显示顺序 | 显示顺序 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/role/index.vue:113](../../acr-ui/src/views/system/role/index.vue#L113) |
| 权限字符 | 角色标识 | 代码中引用角色的标识 | 名称 | [acr-ui/src/views/system/role/index.vue:13](../../acr-ui/src/views/system/role/index.vue#L13) |
| 数据权限 | 可看范围 | 角色可访问的数据范围 | 名称 | [acr-ui/src/views/system/role/index.vue:137](../../acr-ui/src/views/system/role/index.vue#L137) |
| 分配用户 | 设置成员 | 为角色添加或移除用户 | 名称 | [acr-ui/src/views/system/role/index.vue:140](../../acr-ui/src/views/system/role/index.vue#L140) |
| 角色顺序 | 显示顺序 | 列表中角色的排列顺序 | 名称 | [acr-ui/src/views/system/role/index.vue:172](../../acr-ui/src/views/system/role/index.vue#L172) |
| 平台级角色可管理全局凭据、模型、模板和通知渠道，仅超级管理员可维护。 | 仅超级管理员可维护全平台角色 | 该角色管理平台级资源 | 提示 | [acr-ui/src/views/system/role/index.vue:180](../../acr-ui/src/views/system/role/index.vue#L180) |
| 菜单权限 | 功能权限 | 角色可使用的菜单和功能 | 名称 | [acr-ui/src/views/system/role/index.vue:191](../../acr-ui/src/views/system/role/index.vue#L191) |
| 展开/折叠 | 展开收起 | 切换整个树状列表的展开状态 | 名称 | [acr-ui/src/views/system/role/index.vue:192](../../acr-ui/src/views/system/role/index.vue#L192) |
| 全选/全不选 | 选中全部 | 再次点击取消全部选择 | 名称 | [acr-ui/src/views/system/role/index.vue:193](../../acr-ui/src/views/system/role/index.vue#L193) |
| 父子联动 | 同步选择 | 选择上级时同步选择下级 | 名称 | [acr-ui/src/views/system/role/index.vue:194](../../acr-ui/src/views/system/role/index.vue#L194) |
| 确 定 | 确 定 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/role/index.vue:212](../../acr-ui/src/views/system/role/index.vue#L212) |
| 取 消 | 取 消 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/role/index.vue:213](../../acr-ui/src/views/system/role/index.vue#L213) |
| 权限范围 | 可看范围 | 选择角色可访问的数据范围 | 名称 | [acr-ui/src/views/system/role/index.vue:227](../../acr-ui/src/views/system/role/index.vue#L227) |
| 全部数据权限 | 全部数据 | 仍受项目权限等限制 | 名称 | [acr-ui/src/views/system/role/index.vue:294](../../acr-ui/src/views/system/role/index.vue#L294) |
| 自定数据权限 | 指定部门 | 选择可访问数据的部门 | 名称 | [acr-ui/src/views/system/role/index.vue:295](../../acr-ui/src/views/system/role/index.vue#L295) |
| 本部门数据权限 | 当前部门 | 当前部门的数据 | 名称 | [acr-ui/src/views/system/role/index.vue:296](../../acr-ui/src/views/system/role/index.vue#L296) |
| 本部门及以下数据权限 | 下属部门 | 包含本部门与全部下级部门 | 名称 | [acr-ui/src/views/system/role/index.vue:297](../../acr-ui/src/views/system/role/index.vue#L297) |
| 仅本人数据权限 | 本人数据 | 仅本人相关数据 | 名称 | [acr-ui/src/views/system/role/index.vue:298](../../acr-ui/src/views/system/role/index.vue#L298) |
| 角色层级 | 角色范围 | 区分全平台和部门角色 | 名称 | [acr-ui/src/views/system/role/index.vue:37](../../acr-ui/src/views/system/role/index.vue#L37) |
| 平台级 | 全平台 | 角色可用于平台级配置 | 名称 | [acr-ui/src/views/system/role/index.vue:39](../../acr-ui/src/views/system/role/index.vue#L39) |
| 角色名称 | 角色名称 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/role/index.vue:4](../../acr-ui/src/views/system/role/index.vue#L4) |
| 部门级 | 部门内 | 角色归属于部门 | 名称 | [acr-ui/src/views/system/role/index.vue:40](../../acr-ui/src/views/system/role/index.vue#L40) |
| 添加角色 | 添加角色 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/role/index.vue:447](../../acr-ui/src/views/system/role/index.vue#L447) |
| 修改角色 | 修改角色 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/role/index.vue:470](../../acr-ui/src/views/system/role/index.vue#L470) |
| 选择用户 | 选择用户 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/role/selectUser.vue:3](../../acr-ui/src/views/system/role/selectUser.vue#L3) |

### 质量总览

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 暂无投递数据 | 暂无发送记录 | 当前范围没有发送数据 | 提示 | [acr-ui/src/views/insight/overview/index.vue:100](../../acr-ui/src/views/insight/overview/index.vue#L100) |
| 渠道 | 渠道 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/insight/overview/index.vue:101](../../acr-ui/src/views/insight/overview/index.vue#L101) |
| 尝试 | 发送次数 | 各通知方式已结束的发送次数 | 名称 | [acr-ui/src/views/insight/overview/index.vue:106](../../acr-ui/src/views/insight/overview/index.vue#L106) |
| 成功率 | 成功率 | 成功占已结束尝试的比例 | 名称 | [acr-ui/src/views/insight/overview/index.vue:108](../../acr-ui/src/views/insight/overview/index.vue#L108) |
| 聚合尚未产生数据。配置定时任务 insightStatsJobTask.refreshRecent() 后重试。 | 请配置统计更新任务后重试 | 管理员需配置现有统计更新任务 | 提示 | [acr-ui/src/views/insight/overview/index.vue:165](../../acr-ui/src/views/insight/overview/index.vue#L165) |
| 业务系统 | 业务系统 | 按项目所属业务系统筛选 | 名称 | [acr-ui/src/views/insight/overview/index.vue:22](../../acr-ui/src/views/insight/overview/index.vue#L22) |
| 指标说明 | 计算说明 | 查看每项统计如何计算 | 名称 | [acr-ui/src/views/insight/overview/index.vue:33](../../acr-ui/src/views/insight/overview/index.vue#L33) |
| 看板加载失败 | 统计加载失败，请重试 | 重新获取统计数据 | 提示 | [acr-ui/src/views/insight/overview/index.vue:41](../../acr-ui/src/views/insight/overview/index.vue#L41) |
| 时间范围 | 日期范围 | 选择需要查看的日期 | 名称 | [acr-ui/src/views/insight/overview/index.vue:5](../../acr-ui/src/views/insight/overview/index.vue#L5) |
| 审查任务趋势 | 任务趋势 | 按日期查看成功和失败任务 | 名称 | [acr-ui/src/views/insight/overview/index.vue:70](../../acr-ui/src/views/insight/overview/index.vue#L70) |
| 导出 PNG | 保存图片 | 将当前图表保存为图片 | 名称 | [acr-ui/src/views/insight/overview/index.vue:71](../../acr-ui/src/views/insight/overview/index.vue#L71) |
| 新增问题趋势 | 问题趋势 | 按日期查看新增问题 | 名称 | [acr-ui/src/views/insight/overview/index.vue:79](../../acr-ui/src/views/insight/overview/index.vue#L79) |
| 自定义 | 自定义 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/insight/overview/index.vue:9](../../acr-ui/src/views/insight/overview/index.vue#L9) |
| 问题类别分布 | 问题分类 | 按类别查看问题数量 | 名称 | [acr-ui/src/views/insight/overview/index.vue:91](../../acr-ui/src/views/insight/overview/index.vue#L91) |
| 交付渠道健康 | 发送情况 | 按渠道查看发送结果 | 名称 | [acr-ui/src/views/insight/overview/index.vue:99](../../acr-ui/src/views/insight/overview/index.vue#L99) |

### 运行概况

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 告警 | 告警 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:113](../../acr-ui/src/views/monitor/runtime/index.vue#L113) |
| 内存 + 日志，阈值可在参数设置调整 | 告警标准可在系统参数中调整 | 告警暂存于内存和日志 | 提示 | [acr-ui/src/views/monitor/runtime/index.vue:114](../../acr-ui/src/views/monitor/runtime/index.vue#L114) |
| 级别 | 级别 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:118](../../acr-ui/src/views/monitor/runtime/index.vue#L118) |
| 标题 | 标题 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:125](../../acr-ui/src/views/monitor/runtime/index.vue#L125) |
| 看哪里 | 看哪里 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:126](../../acr-ui/src/views/monitor/runtime/index.vue#L126) |
| 怎么办 | 怎么办 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:127](../../acr-ui/src/views/monitor/runtime/index.vue#L127) |
| 打开任务 | 查看任务 | 打开告警关联的任务 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:130](../../acr-ui/src/views/monitor/runtime/index.vue#L130) |
| 打开投递 | 查看发送 | 打开发送记录核对原因 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:131](../../acr-ui/src/views/monitor/runtime/index.vue#L131) |
| 积压与处置 | 积压处理 | 查看久未完成的任务和发送 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:138](../../acr-ui/src/views/monitor/runtime/index.vue#L138) |
| 超龄待执行 | 等待过久 | 等待时间超过告警标准的任务 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:140](../../acr-ui/src/views/monitor/runtime/index.vue#L140) |
| 租约过期 | 执行过期 | 任务超过规定时间未续期 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:141](../../acr-ui/src/views/monitor/runtime/index.vue#L141) |
| 投递滞留 | 发送积压 | 等待发送时间超过告警标准 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:142](../../acr-ui/src/views/monitor/runtime/index.vue#L142) |
| 项目 | 项目 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:146](../../acr-ui/src/views/monitor/runtime/index.vue#L146) |
| 摘要 | 摘要 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:147](../../acr-ui/src/views/monitor/runtime/index.vue#L147) |
| 年龄 | 等待时长 | 距离任务或发送创建的时间 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:148](../../acr-ui/src/views/monitor/runtime/index.vue#L148) |
| 任务 | 任务 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:153](../../acr-ui/src/views/monitor/runtime/index.vue#L153) |
| 运行记录 | 运行记录 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:159](../../acr-ui/src/views/monitor/runtime/index.vue#L159) |
| 投递 | 发送记录 | 打开对应的发送记录 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:165](../../acr-ui/src/views/monitor/runtime/index.vue#L165) |
| 任务面 | 任务状态 | 检查任务的运行状态 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:25](../../acr-ui/src/views/monitor/runtime/index.vue#L25) |
| 待执行 | 待执行 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:27](../../acr-ui/src/views/monitor/runtime/index.vue#L27) |
| 待重试 | 待重试 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:28](../../acr-ui/src/views/monitor/runtime/index.vue#L28) |
| 执行中 | 执行中 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:29](../../acr-ui/src/views/monitor/runtime/index.vue#L29) |
| 已被替代 | 已被替代 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:30](../../acr-ui/src/views/monitor/runtime/index.vue#L30) |
| 最老待执行年龄 | 最长等待 | 待执行任务的最长等待时间 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:32](../../acr-ui/src/views/monitor/runtime/index.vue#L32) |
| 近 24h 重试 | 近期重试 | 最近二十四小时的重试次数 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:41](../../acr-ui/src/views/monitor/runtime/index.vue#L41) |
| 近 24h 超时 | 近期超时 | 最近二十四小时的超时次数 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:42](../../acr-ui/src/views/monitor/runtime/index.vue#L42) |
| 资源面 | 资源占用 | 检查和发送资源的占用情况 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:54](../../acr-ui/src/views/monitor/runtime/index.vue#L54) |
| 审查队列 | 检查队列 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:56](../../acr-ui/src/views/monitor/runtime/index.vue#L56) |
| 审查活跃线程 | 运行数量 | 正在执行检查的线程数 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:57](../../acr-ui/src/views/monitor/runtime/index.vue#L57) |
| 审查拒绝数 | 拒绝次数 | 检查线程池拒绝的累计次数 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:58](../../acr-ui/src/views/monitor/runtime/index.vue#L58) |
| 投递队列 | 投递队列 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:59](../../acr-ui/src/views/monitor/runtime/index.vue#L59) |
| 工作区占用 | 工作占用 | 使用中的仓库工作目录数量 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:60](../../acr-ui/src/views/monitor/runtime/index.vue#L60) |
| LLM 占用 | 模型占用 | 同时调用模型的资源占用 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:62](../../acr-ui/src/views/monitor/runtime/index.vue#L62) |
| 磁盘占用 | 磁盘占用 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:63](../../acr-ui/src/views/monitor/runtime/index.vue#L63) |
| 交付面 | 发送状态 | 结果发送任务的运行状态 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:70](../../acr-ui/src/views/monitor/runtime/index.vue#L70) |
| 待投递 | 待投递 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:72](../../acr-ui/src/views/monitor/runtime/index.vue#L72) |
| 待人工处置 | 待处理 | 需要人工处理的发送记录 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:73](../../acr-ui/src/views/monitor/runtime/index.vue#L73) |
| 最老待投递年龄 | 最长等待 | 待发送记录的最长等待时间 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:75](../../acr-ui/src/views/monitor/runtime/index.vue#L75) |
| 运行概览汇总审查调度、资源预算与投递队列状态。告警给出可执行处置建议，可跳转到任务或投递证据。 | 查看任务、资源和发送状态 | 告警可打开任务或发送记录 | 提示 | [acr-ui/src/views/monitor/runtime/index.vue:8](../../acr-ui/src/views/monitor/runtime/index.vue#L8) |
| 审查引擎可用性 | 工具状态 | 检查服务器上的工具是否可用 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:91](../../acr-ui/src/views/monitor/runtime/index.vue#L91) |
| 版本探测结果缓存 60 秒 | 检查结果保留一分钟 | 显示最近一次引擎检查结果 | 提示 | [acr-ui/src/views/monitor/runtime/index.vue:92](../../acr-ui/src/views/monitor/runtime/index.vue#L92) |

### 通用文案

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 暂无数据 | 暂无数据 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/layout/components/HeaderNotice/DetailView.vue:6](../../acr-ui/src/layout/components/HeaderNotice/DetailView.vue#L6) |
| 加载中… | 加载中… | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/index/components/ModelHealth.vue:11](../../acr-ui/src/views/index/components/ModelHealth.vue#L11) |
| 重试 | 重试 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/index/components/ModelHealth.vue:9](../../acr-ui/src/views/index/components/ModelHealth.vue#L9) |
| 数量 | 数量 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/insight/member/index.vue:133](../../acr-ui/src/views/insight/member/index.vue#L133) |
| 操作 | 操作 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/insight/member/index.vue:209](../../acr-ui/src/views/insight/member/index.vue#L209) |
| 取消 | 取消 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/insight/member/index.vue:241](../../acr-ui/src/views/insight/member/index.vue#L241) |
| 确定 | 确定 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/insight/member/index.vue:242](../../acr-ui/src/views/insight/member/index.vue#L242) |
| 查询 | 查询 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/insight/member/index.vue:71](../../acr-ui/src/views/insight/member/index.vue#L71) |
| 重置 | 重置 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/insight/member/index.vue:72](../../acr-ui/src/views/insight/member/index.vue#L72) |
| 成功 | 成功次数 | 已成功完成的发送次数 | 名称 | [acr-ui/src/views/insight/overview/index.vue:107](../../acr-ui/src/views/insight/overview/index.vue#L107) |
| 备注 | 备注 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/cache/list.vue:39](../../acr-ui/src/views/monitor/cache/list.vue#L39) |
| 创建时间 | 创建时间 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/detail.vue:160](../../acr-ui/src/views/monitor/job/detail.vue#L160) |
| 正常 | 正常 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/detail.vue:17](../../acr-ui/src/views/monitor/job/detail.vue#L17) |
| 失败 | 失败 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/detail.vue:18](../../acr-ui/src/views/monitor/job/detail.vue#L18) |
| 状态 | 状态 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/index.vue:105](../../acr-ui/src/views/monitor/job/index.vue#L105) |
| 搜索 | 搜索 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/index.vue:34](../../acr-ui/src/views/monitor/job/index.vue#L34) |
| 新增 | 新增 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/index.vue:47](../../acr-ui/src/views/monitor/job/index.vue#L47) |
| 修改 | 修改 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/index.vue:57](../../acr-ui/src/views/monitor/job/index.vue#L57) |
| 删除 | 删除 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/index.vue:67](../../acr-ui/src/views/monitor/job/index.vue#L67) |
| 导出 | 导出 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/index.vue:76](../../acr-ui/src/views/monitor/job/index.vue#L76) |
| 关闭 | 关闭 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/job/log.vue:94](../../acr-ui/src/views/monitor/job/log.vue#L94) |
| 异常 | 异常 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/operlog/detail.vue:23](../../acr-ui/src/views/monitor/operlog/detail.vue#L23) |
| 复制 | 复制 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/operlog/detail.vue:82](../../acr-ui/src/views/monitor/operlog/detail.vue#L82) |
| 类型 | 类型 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/operlog/index.vue:31](../../acr-ui/src/views/monitor/operlog/index.vue#L31) |
| 刷新 | 刷新 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/monitor/runtime/index.vue:12](../../acr-ui/src/views/monitor/runtime/index.vue#L12) |
| 详情 | 详情 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/audit/index.vue:30](../../acr-ui/src/views/system/audit/index.vue#L30) |
| 查看 | 查看 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/audit/index.vue:32](../../acr-ui/src/views/system/audit/index.vue#L32) |
| 保存 | 保存 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/user/profile/commitEmail.vue:57](../../acr-ui/src/views/system/user/profile/commitEmail.vue#L57) |

### 通知公告

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 序号 | 序号 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/notice/ReadUsers.vue:26](../../acr-ui/src/views/system/notice/ReadUsers.vue#L26) |
| 登录名称 | 登录账号 | 用于登录的账号名称 | 名称 | [acr-ui/src/views/system/notice/ReadUsers.vue:27](../../acr-ui/src/views/system/notice/ReadUsers.vue#L27) |
| 用户名称 | 昵称 | 页面字段实际显示用户昵称 | 名称 | [acr-ui/src/views/system/notice/ReadUsers.vue:28](../../acr-ui/src/views/system/notice/ReadUsers.vue#L28) |
| 所属部门 | 所属部门 | 用户归属的组织部门 | 名称 | [acr-ui/src/views/system/notice/ReadUsers.vue:29](../../acr-ui/src/views/system/notice/ReadUsers.vue#L29) |
| 手机号码 | 手机号 | 联系用户的手机号码 | 名称 | [acr-ui/src/views/system/notice/ReadUsers.vue:30](../../acr-ui/src/views/system/notice/ReadUsers.vue#L30) |
| 阅读时间 | 阅读时间 | 用户首次阅读该公告的时间 | 名称 | [acr-ui/src/views/system/notice/ReadUsers.vue:31](../../acr-ui/src/views/system/notice/ReadUsers.vue#L31) |
| 操作人员 | 创建者 | 实际按公告创建者查询 | 名称 | [acr-ui/src/views/system/notice/index.vue:13](../../acr-ui/src/views/system/notice/index.vue#L13) |
| 内容 | 内容 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/notice/index.vue:145](../../acr-ui/src/views/system/notice/index.vue#L145) |
| 确 定 | 确 定 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/notice/index.vue:153](../../acr-ui/src/views/system/notice/index.vue#L153) |
| 取 消 | 取 消 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/notice/index.vue:154](../../acr-ui/src/views/system/notice/index.vue#L154) |
| 添加公告 | 添加公告 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/notice/index.vue:249](../../acr-ui/src/views/system/notice/index.vue#L249) |
| 修改公告 | 修改公告 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/notice/index.vue:259](../../acr-ui/src/views/system/notice/index.vue#L259) |
| 公告标题 | 公告标题 | 用户看到的公告名称 | 名称 | [acr-ui/src/views/system/notice/index.vue:4](../../acr-ui/src/views/system/notice/index.vue#L4) |
| 公告类型 | 公告类型 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/notice/index.vue:79](../../acr-ui/src/views/system/notice/index.vue#L79) |
| 创建者 | 创建者 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/notice/index.vue:89](../../acr-ui/src/views/system/notice/index.vue#L89) |
| 阅读用户 | 已读用户 | 查看谁已读该公告 | 名称 | [acr-ui/src/views/system/notice/index.vue:97](../../acr-ui/src/views/system/notice/index.vue#L97) |

### 部门设置

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 显示排序 | 显示顺序 | 数值越小，位置越靠前 | 名称 | [acr-ui/src/views/system/dept/index.vue:114](../../acr-ui/src/views/system/dept/index.vue#L114) |
| 负责人 | 负责人 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/dept/index.vue:119](../../acr-ui/src/views/system/dept/index.vue#L119) |
| 联系电话 | 联系电话 | 部门负责人的联系号码 | 名称 | [acr-ui/src/views/system/dept/index.vue:124](../../acr-ui/src/views/system/dept/index.vue#L124) |
| 邮箱 | 邮箱 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/dept/index.vue:129](../../acr-ui/src/views/system/dept/index.vue#L129) |
| 部门状态 | 部门状态 | 该部门是否可用 | 名称 | [acr-ui/src/views/system/dept/index.vue:134](../../acr-ui/src/views/system/dept/index.vue#L134) |
| 确 定 | 确 定 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/dept/index.vue:148](../../acr-ui/src/views/system/dept/index.vue#L148) |
| 取 消 | 取 消 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/dept/index.vue:149](../../acr-ui/src/views/system/dept/index.vue#L149) |
| 添加部门 | 添加部门 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/dept/index.vue:241](../../acr-ui/src/views/system/dept/index.vue#L241) |
| 修改部门 | 修改部门 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/dept/index.vue:262](../../acr-ui/src/views/system/dept/index.vue#L262) |
| 部门名称 | 部门名称 | 部门在页面中显示的名称 | 名称 | [acr-ui/src/views/system/dept/index.vue:4](../../acr-ui/src/views/system/dept/index.vue#L4) |
| 保存排序 | 保存排序 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/dept/index.vue:46](../../acr-ui/src/views/system/dept/index.vue#L46) |
| 展开/折叠 | 展开收起 | 切换整个树状列表的展开状态 | 名称 | [acr-ui/src/views/system/dept/index.vue:54](../../acr-ui/src/views/system/dept/index.vue#L54) |
| 排序 | 排序 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/dept/index.vue:68](../../acr-ui/src/views/system/dept/index.vue#L68) |
| 上级部门 | 上级部门 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/dept/index.vue:97](../../acr-ui/src/views/system/dept/index.vue#L97) |

### 页面选项

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 字典类型 | 选项代码 | 系统识别这组选项的代码 | 名称 | [acr-ui/src/views/system/dict/data.vue:130](../../acr-ui/src/views/system/dict/data.vue#L130) |
| 数据标签 | 显示文字 | 用户在页面中看到的选项文字 | 名称 | [acr-ui/src/views/system/dict/data.vue:133](../../acr-ui/src/views/system/dict/data.vue#L133) |
| 数据键值 | 选项值 | 系统实际保存的选项值 | 名称 | [acr-ui/src/views/system/dict/data.vue:136](../../acr-ui/src/views/system/dict/data.vue#L136) |
| 样式属性 | 样式类 | 选项的自定义样式类名称 | 名称 | [acr-ui/src/views/system/dict/data.vue:139](../../acr-ui/src/views/system/dict/data.vue#L139) |
| 字典标签 | 显示文字 | 用户在页面中看到的选项文字 | 名称 | [acr-ui/src/views/system/dict/data.vue:14](../../acr-ui/src/views/system/dict/data.vue#L14) |
| 显示排序 | 显示顺序 | 数值越小，位置越靠前 | 名称 | [acr-ui/src/views/system/dict/data.vue:142](../../acr-ui/src/views/system/dict/data.vue#L142) |
| 回显样式 | 标签样式 | 选项标签在页面中的样式 | 名称 | [acr-ui/src/views/system/dict/data.vue:145](../../acr-ui/src/views/system/dict/data.vue#L145) |
| 确 定 | 确 定 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/dict/data.vue:170](../../acr-ui/src/views/system/dict/data.vue#L170) |
| 取 消 | 取 消 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/dict/data.vue:171](../../acr-ui/src/views/system/dict/data.vue#L171) |
| 默认 | 默认 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/dict/data.vue:200](../../acr-ui/src/views/system/dict/data.vue#L200) |
| 主要 | 主要 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/dict/data.vue:201](../../acr-ui/src/views/system/dict/data.vue#L201) |
| 信息 | 信息 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/dict/data.vue:203](../../acr-ui/src/views/system/dict/data.vue#L203) |
| 警告 | 警告 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/dict/data.vue:204](../../acr-ui/src/views/system/dict/data.vue#L204) |
| 危险 | 危险 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/dict/data.vue:205](../../acr-ui/src/views/system/dict/data.vue#L205) |
| 字典名称 | 选项名称 | 这组选项在页面中的名称 | 名称 | [acr-ui/src/views/system/dict/data.vue:4](../../acr-ui/src/views/system/dict/data.vue#L4) |
| 字典编码 | 选项编号 | 单个选项的唯一编号 | 名称 | [acr-ui/src/views/system/dict/data.vue:91](../../acr-ui/src/views/system/dict/data.vue#L91) |
| 字典键值 | 选项值 | 系统实际保存的选项值 | 名称 | [acr-ui/src/views/system/dict/data.vue:98](../../acr-ui/src/views/system/dict/data.vue#L98) |
| 字典排序 | 显示顺序 | 数值越小，位置越靠前 | 名称 | [acr-ui/src/views/system/dict/data.vue:99](../../acr-ui/src/views/system/dict/data.vue#L99) |
| 共计条目 | 共计条目 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/dict/detail.vue:31](../../acr-ui/src/views/system/dict/detail.vue#L31) |
| 停用 | 停用 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/dict/detail.vue:43](../../acr-ui/src/views/system/dict/detail.vue#L43) |
| 标签 | 标签 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/dict/detail.vue:51](../../acr-ui/src/views/system/dict/detail.vue#L51) |
| 键值 | 键值 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/dict/detail.vue:58](../../acr-ui/src/views/system/dict/detail.vue#L58) |
| 字典编号 | 选项编号 | 这组选项的唯一编号 | 名称 | [acr-ui/src/views/system/dict/index.vue:106](../../acr-ui/src/views/system/dict/index.vue#L106) |
| 列表 | 列表 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/system/dict/index.vue:127](../../acr-ui/src/views/system/dict/index.vue#L127) |
| 刷新缓存 | 更新缓存 | 重新载入当前设置 | 名称 | [acr-ui/src/views/system/dict/index.vue:99](../../acr-ui/src/views/system/dict/index.vue#L99) |

### 项目统计

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 类别 | 类别 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/insight/project/detail.vue:100](../../acr-ui/src/views/insight/project/detail.vue#L100) |
| 处置漏斗 | 处理进展 | 新增、确认和关闭的数量 | 名称 | [acr-ui/src/views/insight/project/detail.vue:109](../../acr-ui/src/views/insight/project/detail.vue#L109) |
| 时间范围 | 日期范围 | 选择需要查看的日期 | 名称 | [acr-ui/src/views/insight/project/detail.vue:15](../../acr-ui/src/views/insight/project/detail.vue#L15) |
| 请确认项目已产生审查任务，并已执行聚合刷新任务。 | 请检查检查任务和统计更新 | 需要已检查任务及统计更新 | 提示 | [acr-ui/src/views/insight/project/detail.vue:155](../../acr-ui/src/views/insight/project/detail.vue#L155) |
| 自定义区间 | 自选日期 | 沿用列表中选择的日期 | 名称 | [acr-ui/src/views/insight/project/detail.vue:19](../../acr-ui/src/views/insight/project/detail.vue#L19) |
| 返回列表 | 返回列表 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/insight/project/detail.vue:5](../../acr-ui/src/views/insight/project/detail.vue#L5) |
| 审查任务趋势 | 任务趋势 | 按日期查看成功和失败任务 | 名称 | [acr-ui/src/views/insight/project/detail.vue:55](../../acr-ui/src/views/insight/project/detail.vue#L55) |
| 导出 PNG | 保存图片 | 将当前图表保存为图片 | 名称 | [acr-ui/src/views/insight/project/detail.vue:56](../../acr-ui/src/views/insight/project/detail.vue#L56) |
| 问题趋势 | 问题趋势 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/insight/project/detail.vue:64](../../acr-ui/src/views/insight/project/detail.vue#L64) |
| 项目提交趋势 | 提交趋势 | 按日期查看项目提交 | 名称 | [acr-ui/src/views/insight/project/detail.vue:76](../../acr-ui/src/views/insight/project/detail.vue#L76) |
| 严重度分布 | 风险分布 | 按风险等级统计问题 | 名称 | [acr-ui/src/views/insight/project/detail.vue:87](../../acr-ui/src/views/insight/project/detail.vue#L87) |
| 严重度 | 风险等级 | 区分严重、高、中、低风险 | 名称 | [acr-ui/src/views/insight/project/detail.vue:89](../../acr-ui/src/views/insight/project/detail.vue#L89) |
| 类别分布 | 问题分类 | 按类别统计问题 | 名称 | [acr-ui/src/views/insight/project/detail.vue:98](../../acr-ui/src/views/insight/project/detail.vue#L98) |
| 暂无 | 暂无 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/insight/project/detail.vue:99](../../acr-ui/src/views/insight/project/detail.vue#L99) |
| 业务系统 | 业务系统 | 按项目所属业务系统筛选 | 名称 | [acr-ui/src/views/insight/project/index.vue:21](../../acr-ui/src/views/insight/project/index.vue#L21) |
| 项目 | 项目 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/insight/project/index.vue:26](../../acr-ui/src/views/insight/project/index.vue#L26) |
| 项目矩阵加载失败 | 项目统计加载失败，请重试 | 重新获取项目统计 | 提示 | [acr-ui/src/views/insight/project/index.vue:46](../../acr-ui/src/views/insight/project/index.vue#L46) |
| 暂无可见项目或选定范围内无审查数据 | 当前范围暂无项目数据 | 也可能没有项目查看权限 | 提示 | [acr-ui/src/views/insight/project/index.vue:56](../../acr-ui/src/views/insight/project/index.vue#L56) |
| 请确认数据权限范围内已接入代码项目，并完成聚合刷新。数据积累起始日期见详情页空态提示。 | 请检查项目权限和统计更新 | 可在详情查看统计起始日期 | 提示 | [acr-ui/src/views/insight/project/index.vue:57](../../acr-ui/src/views/insight/project/index.vue#L57) |
| 负责人 | 负责人 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/insight/project/index.vue:66](../../acr-ui/src/views/insight/project/index.vue#L66) |
| 任务数 | 任务数 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/insight/project/index.vue:67](../../acr-ui/src/views/insight/project/index.vue#L67) |
| 成功率 | 成功率 | 成功占已结束尝试的比例 | 名称 | [acr-ui/src/views/insight/project/index.vue:68](../../acr-ui/src/views/insight/project/index.vue#L68) |
| 新增问题 | 新增问题 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/insight/project/index.vue:71](../../acr-ui/src/views/insight/project/index.vue#L71) |
| 未关闭重点 | 重点待办 | 未关闭的严重和高风险问题 | 名称 | [acr-ui/src/views/insight/project/index.vue:72](../../acr-ui/src/views/insight/project/index.vue#L72) |
| 处置率 | 处理比例 | 确认加关闭动作数除以新增数 | 名称 | [acr-ui/src/views/insight/project/index.vue:73](../../acr-ui/src/views/insight/project/index.vue#L73) |
| 最近审查时间 | 最近检查 | 项目最近一次检查时间 | 名称 | [acr-ui/src/views/insight/project/index.vue:76](../../acr-ui/src/views/insight/project/index.vue#L76) |
| 自定义 | 自定义 | 保留已清楚表达的名称 | 名称 | [acr-ui/src/views/insight/project/index.vue:8](../../acr-ui/src/views/insight/project/index.vue#L8) |

### 状态 / review_task_status

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 待执行 | 待检查 | 任务已排队，等待开始 | 状态 | [sql/init-full.sql:1605](../../sql/init-full.sql#L1605) |
| 执行中 | 检查中 | 正在检查代码 | 状态 | [sql/init-full.sql:1606](../../sql/init-full.sql#L1606) |
| 已完成 | 已完成 | 检查已结束，请查看结论 | 状态 | [sql/init-full.sql:1607](../../sql/init-full.sql#L1607) |
| 已失败 | 检查失败 | 查看失败原因并处理 | 状态 | [sql/init-full.sql:1608](../../sql/init-full.sql#L1608) |
| 已取消 | 已取消 | 任务已终止，不再继续执行 | 状态 | [sql/init-full.sql:1609](../../sql/init-full.sql#L1609) |
| 待重试 | 待重试 | 系统将自动重新检查 | 状态 | [sql/init-full.sql:1678](../../sql/init-full.sql#L1678) |
| 已被替代 | 已有新版 | 已有新任务，此次只保留记录 | 状态 | [sql/init-full.sql:1679](../../sql/init-full.sql#L1679) |

### 状态 / review_webhook_process_status

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 已接收 | 已收到 | 已收到代码平台的消息 | 状态 | [sql/init-full.sql:1610](../../sql/init-full.sql#L1610) |
| 已受理 | 已受理 | 消息符合检查条件 | 状态 | [sql/init-full.sql:1611](../../sql/init-full.sql#L1611) |
| 已忽略 | 已跳过 | 消息不需要创建检查任务 | 状态 | [sql/init-full.sql:1612](../../sql/init-full.sql#L1612) |
| 重复投递 | 重复消息 | 同一消息已处理过 | 状态 | [sql/init-full.sql:1613](../../sql/init-full.sql#L1613) |
| 接入失败 | 接收失败 | 接收或处理消息时出错 | 状态 | [sql/init-full.sql:1614](../../sql/init-full.sql#L1614) |

### 状态 / review_mode

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 审查引擎 | 工具检查 | 调用服务器上的检查工具 | 状态 | [sql/init-full.sql:1618](../../sql/init-full.sql#L1618) |
| 大模型审查 | 模型检查 | 直接调用模型检查代码 | 状态 | [sql/init-full.sql:1635](../../sql/init-full.sql#L1635) |

### 状态 / review_conclusion

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 通过 | 通过 | 此次检查结果通过 | 状态 | [sql/init-full.sql:1620](../../sql/init-full.sql#L1620) |
| 警告 | 建议修改 | 发现需要关注的问题 | 状态 | [sql/init-full.sql:1621](../../sql/init-full.sql#L1621) |
| 阻断 | 高风险 | 发现高风险，不会自动阻止合并 | 状态 | [sql/init-full.sql:1622](../../sql/init-full.sql#L1622) |

### 状态 / review_task_step

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 解析配置 | 读取设置 | 读取此次检查所用设置 | 状态 | [sql/init-full.sql:1623](../../sql/init-full.sql#L1623) |
| 准备工作区 | 准备代码 | 准备检查所需的代码文件 | 状态 | [sql/init-full.sql:1624](../../sql/init-full.sql#L1624) |
| 调用审查引擎 | 运行工具 | 调用服务器上的检查工具 | 状态 | [sql/init-full.sql:1625](../../sql/init-full.sql#L1625) |
| 保存结果 | 保存结果 | 保存检查评分和问题 | 状态 | [sql/init-full.sql:1626](../../sql/init-full.sql#L1626) |
| 调用大模型 | 调用模型 | 调用模型检查代码 | 状态 | [sql/init-full.sql:1636](../../sql/init-full.sql#L1636) |

### 状态 / review_task_failure_type

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 配置缺失 | 缺少设置 | 补齐检查所需设置 | 状态 | [sql/init-full.sql:1627](../../sql/init-full.sql#L1627) |
| 凭据错误 | 密钥错误 | 检查代码平台访问密钥 | 状态 | [sql/init-full.sql:1628](../../sql/init-full.sql#L1628) |
| 工作区准备失败 | 准备失败 | 代码文件准备失败 | 状态 | [sql/init-full.sql:1629](../../sql/init-full.sql#L1629) |
| 引擎超时 | 运行超时 | 检查工具未按时完成 | 状态 | [sql/init-full.sql:1630](../../sql/init-full.sql#L1630) |
| 引擎调用失败 | 工具失败 | 检查工具运行失败 | 状态 | [sql/init-full.sql:1631](../../sql/init-full.sql#L1631) |
| 模型调用失败 | 模型失败 | 模型调用失败 | 状态 | [sql/init-full.sql:1632](../../sql/init-full.sql#L1632) |
| 并发超限 | 任务过多 | 同时运行的任务已达上限 | 状态 | [sql/init-full.sql:1633](../../sql/init-full.sql#L1633) |
| 未知错误 | 未知错误 | 查看详情定位原因 | 状态 | [sql/init-full.sql:1634](../../sql/init-full.sql#L1634) |
| 结果格式异常 | 结果异常 | 检查结果格式不符合要求 | 状态 | [sql/init-full.sql:1643](../../sql/init-full.sql#L1643) |
| API 限流 | 请求过多 | 代码平台限制请求，稍后重试 | 状态 | [sql/init-full.sql:1644](../../sql/init-full.sql#L1644) |
| 依赖暂不可用 | 暂不可用 | 外部服务异常，将自动重试 | 状态 | [sql/init-full.sql:1680](../../sql/init-full.sql#L1680) |
| 执行租约过期 | 执行中断 | 执行节点失联，由系统接管 | 状态 | [sql/init-full.sql:1681](../../sql/init-full.sql#L1681) |
| 执行节点停机 | 节点停机 | 原节点停机，由系统接管 | 状态 | [sql/init-full.sql:1682](../../sql/init-full.sql#L1682) |

### 状态 / review_delivery_status

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 已投递 | 已发送 | 评论或消息已成功发送 | 状态 | [sql/init-full.sql:1645](../../sql/init-full.sql#L1645) |
| 自动重试中 | 自动重发 | 上次失败，将按计划重发 | 状态 | [sql/init-full.sql:1646](../../sql/init-full.sql#L1646) |
| 待投递 | 待发送 | 等待发送评论或群消息 | 状态 | [sql/init-full.sql:1683](../../sql/init-full.sql#L1683) |
| 待人工处置 | 需处理 | 设置有误或自动重试已用尽 | 状态 | [sql/init-full.sql:1684](../../sql/init-full.sql#L1684) |
| 已跳过 | 已跳过 | 已被新结果替代或受发送规则限制 | 状态 | [sql/init-full.sql:1685](../../sql/init-full.sql#L1685) |

### 状态 / review_issue_status

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 待确认 | 待确认 | 先核实问题是否需要修复 | 状态 | [sql/init-full.sql:1654](../../sql/init-full.sql#L1654) |
| 待修复 | 待修复 | 已确认问题，等待修复 | 状态 | [sql/init-full.sql:1655](../../sql/init-full.sql#L1655) |
| 已关闭 | 已关闭 | 已结束跟进，可查看关闭原因 | 状态 | [sql/init-full.sql:1656](../../sql/init-full.sql#L1656) |
| 已忽略 | 已忽略 | 已填写原因，不再跟进 | 状态 | [sql/init-full.sql:1657](../../sql/init-full.sql#L1657) |
| 误报 | 误报 | 已确认不是实际问题 | 状态 | [sql/init-full.sql:1658](../../sql/init-full.sql#L1658) |
| 疑似修复 | 待复核 | 后续检查未发现，仍需核实 | 状态 | [sql/init-full.sql:1659](../../sql/init-full.sql#L1659) |

### 状态 / review_issue_origin

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 新增 | 新增问题 | 此次代码改动带来的问题 | 状态 | [sql/init-full.sql:1662](../../sql/init-full.sql#L1662) |
| 存量 | 已有问题 | 代码改动之前就存在的问题 | 状态 | [sql/init-full.sql:1663](../../sql/init-full.sql#L1663) |

### 状态 / review_delivery_trigger_source

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 任务回写 | 检查完成 | 检查结束后发送结果 | 状态 | [sql/init-full.sql:1664](../../sql/init-full.sql#L1664) |
| 问题处置 | 问题处理 | 问题状态变更后更新评论 | 状态 | [sql/init-full.sql:1665](../../sql/init-full.sql#L1665) |
| 手动重试 | 手动重发 | 由用户重新发送 | 状态 | [sql/init-full.sql:1666](../../sql/init-full.sql#L1666) |
| 任务失败通知 | 检查失败 | 检查失败时发送通知 | 状态 | [sql/init-full.sql:1691](../../sql/init-full.sql#L1691) |
| 逾期提醒 | 逾期提醒 | 提醒跟进超过期限的问题 | 状态 | [sql/init-full.sql:1692](../../sql/init-full.sql#L1692) |

### 状态 / review_event_source

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 合并请求 | 合并申请 | 创建或更新合并申请触发 | 状态 | [sql/init-full.sql:1676](../../sql/init-full.sql#L1676) |
| 推送 | 代码推送 | 推送指定分支后检查 | 状态 | [sql/init-full.sql:1677](../../sql/init-full.sql#L1677) |

### 使用帮助

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 快速上手 | 快速上手 | 从连接仓库到查看检查结果 | 名称 | [acr-ui/src/guide/manifest.js:4](../../acr-ui/src/guide/manifest.js#L4) |
| 平台接入指引 | 连接仓库 | 按代码平台查看连接步骤 | 名称 | [acr-ui/src/guide/manifest.js:5](../../acr-ui/src/guide/manifest.js#L5) |
| 模型与引擎 | 检查工具 | 设置模型或服务器检查工具 | 名称 | [acr-ui/src/guide/manifest.js:6](../../acr-ui/src/guide/manifest.js#L6) |
| 提交注释规范 | 提交说明 | 了解提交说明的写法 | 名称 | [acr-ui/src/guide/manifest.js:7](../../acr-ui/src/guide/manifest.js#L7) |
| 投递与通知 | 消息通知 | 设置通知并处理发送失败 | 名称 | [acr-ui/src/guide/manifest.js:8](../../acr-ui/src/guide/manifest.js#L8) |
| 常见问题 | 常见问题 | 查找常见问题的解决方法 | 名称 | [acr-ui/src/guide/manifest.js:9](../../acr-ui/src/guide/manifest.js#L9) |
| 接入第一个仓库 | 连接仓库 | 按步骤完成第一个仓库连接 | 名称 | [acr-ui/src/guide/manifest.js:13](../../acr-ui/src/guide/manifest.js#L13) |
| 模型服务配置 | 模型设置 | 填写模型地址和访问密钥 | 名称 | [acr-ui/src/guide/manifest.js:18](../../acr-ui/src/guide/manifest.js#L18) |
| 审查引擎配置 | 工具设置 | 检查服务器工具是否可用 | 名称 | [acr-ui/src/guide/manifest.js:19](../../acr-ui/src/guide/manifest.js#L19) |
| 模型检测失败排查 | 模型排查 | 查找模型无法连接的原因 | 名称 | [acr-ui/src/guide/manifest.js:20](../../acr-ui/src/guide/manifest.js#L20) |
| 提交注释规范说明 | 提交说明 | 了解提交说明如何影响检查 | 名称 | [acr-ui/src/guide/manifest.js:21](../../acr-ui/src/guide/manifest.js#L21) |
| 注释正误实例对照 | 填写示例 | 对照示例写好提交说明 | 名称 | [acr-ui/src/guide/manifest.js:22](../../acr-ui/src/guide/manifest.js#L22) |
| 通知渠道配置 | 通知设置 | 连接群机器人并测试发送 | 名称 | [acr-ui/src/guide/manifest.js:23](../../acr-ui/src/guide/manifest.js#L23) |
| 投递失败排查 | 发送排查 | 查看发送失败原因并补发 | 名称 | [acr-ui/src/guide/manifest.js:24](../../acr-ui/src/guide/manifest.js#L24) |
| Webhook 没触发怎么办 | 未触发 | 按步骤排查自动检查未触发 | 名称 | [acr-ui/src/guide/manifest.js:25](../../acr-ui/src/guide/manifest.js#L25) |
| 审查没执行或结果不符预期 | 检查排查 | 排查检查失败或结论异常 | 名称 | [acr-ui/src/guide/manifest.js:26](../../acr-ui/src/guide/manifest.js#L26) |
| 功能助手 | 使用帮助 | 查找操作方法和常见问题 | 名称 | [acr-ui/src/guide/components/GuideEntry.vue:2](../../acr-ui/src/guide/components/GuideEntry.vue#L2)<br>[acr-ui/src/guide/components/GuideDrawer.vue:12](../../acr-ui/src/guide/components/GuideDrawer.vue#L12) |
| 任务式指引 | 操作引导 | 跟着步骤完成常用操作 | 名称 | [acr-ui/src/guide/components/GuideDrawer.vue:18](../../acr-ui/src/guide/components/GuideDrawer.vue#L18) |
| 指引目录 | 帮助目录 | 查找需要的操作说明 | 名称 | [acr-ui/src/guide/components/GuideDrawer.vue:54](../../acr-ui/src/guide/components/GuideDrawer.vue#L54) |
| 可随时重新开始 | 随时可以重新开始 |  | 提示 | [acr-ui/src/guide/components/GuideDrawer.vue:19](../../acr-ui/src/guide/components/GuideDrawer.vue#L19) |
| 搜索指引：如 webhook、token、注释 | 搜索帮助，如密钥、通知、提交说明 |  | 提示 | [acr-ui/src/guide/components/GuideDrawer.vue:36](../../acr-ui/src/guide/components/GuideDrawer.vue#L36) |
| 没有匹配的指引，换个关键词试试 | 没找到相关帮助，换个词试试 |  | 提示 | [acr-ui/src/guide/components/GuideDrawer.vue:42](../../acr-ui/src/guide/components/GuideDrawer.vue#L42) |
| 加载中… | 正在加载 |  | 提示 | [acr-ui/src/guide/components/GuideContent.vue:3](../../acr-ui/src/guide/components/GuideContent.vue#L3) |
| 该文档暂未提供 | 这篇帮助暂不可用 |  | 提示 | [acr-ui/src/guide/components/GuideContent.vue:4](../../acr-ui/src/guide/components/GuideContent.vue#L4) |

### 操作引导

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 管理员：接入第一个项目 | 连接项目 |  | 名称 | [acr-ui/src/tour/manifest.js:12](../../acr-ui/src/tour/manifest.js#L12) |
| 从工作台开始 | 开始接入 |  | 名称 | [acr-ui/src/tour/manifest.js:19](../../acr-ui/src/tour/manifest.js#L19) |
| 先准备访问凭据 | 准备密钥 |  | 名称 | [acr-ui/src/tour/manifest.js:26](../../acr-ui/src/tour/manifest.js#L26) |
| 确认连接可用 | 测试连接 |  | 名称 | [acr-ui/src/tour/manifest.js:33](../../acr-ui/src/tour/manifest.js#L33) |
| 新建代码项目 | 添加项目 |  | 名称 | [acr-ui/src/tour/manifest.js:40](../../acr-ui/src/tour/manifest.js#L40) |
| 沿向导完成接入 | 填写设置 |  | 名称 | [acr-ui/src/tour/manifest.js:48](../../acr-ui/src/tour/manifest.js#L48) |
| 进入 Webhook 配置 | 自动检查 |  | 名称 | [acr-ui/src/tour/manifest.js:55](../../acr-ui/src/tour/manifest.js#L55) |
| 核对回调与密钥 | 核对设置 |  | 名称 | [acr-ui/src/tour/manifest.js:63](../../acr-ui/src/tour/manifest.js#L63) |
| 验证首次触发 | 验证检查 |  | 名称 | [acr-ui/src/tour/manifest.js:70](../../acr-ui/src/tour/manifest.js#L70) |
| 开发者：处理第一个审查问题 | 处理问题 |  | 名称 | [acr-ui/src/tour/manifest.js:77](../../acr-ui/src/tour/manifest.js#L77) |
| 先看今日待办 | 今日待办 |  | 名称 | [acr-ui/src/tour/manifest.js:84](../../acr-ui/src/tour/manifest.js#L84) |
| 打开待确认问题 | 确认问题 |  | 名称 | [acr-ui/src/tour/manifest.js:91](../../acr-ui/src/tour/manifest.js#L91) |
| 确认当前治理阶段 | 查看状态 |  | 名称 | [acr-ui/src/tour/manifest.js:98](../../acr-ui/src/tour/manifest.js#L98) |
| 找到需要处理的问题 | 筛选问题 |  | 名称 | [acr-ui/src/tour/manifest.js:105](../../acr-ui/src/tour/manifest.js#L105) |
| 阅读问题证据 | 核实问题 |  | 名称 | [acr-ui/src/tour/manifest.js:113](../../acr-ui/src/tour/manifest.js#L113) |
| 看懂生命周期 | 处理过程 |  | 名称 | [acr-ui/src/tour/manifest.js:120](../../acr-ui/src/tour/manifest.js#L120) |
| 选择正确处置 | 选择操作 |  | 名称 | [acr-ui/src/tour/manifest.js:127](../../acr-ui/src/tour/manifest.js#L127) |
| 管理者：看懂工作台与数据洞察 | 查看质量 |  | 名称 | [acr-ui/src/tour/manifest.js:134](../../acr-ui/src/tour/manifest.js#L134) |
| 确认数据范围 | 统计范围 |  | 名称 | [acr-ui/src/tour/manifest.js:141](../../acr-ui/src/tour/manifest.js#L141) |
| 识别当日风险 | 今日风险 |  | 名称 | [acr-ui/src/tour/manifest.js:148](../../acr-ui/src/tour/manifest.js#L148) |
| 观察短期趋势 | 风险趋势 |  | 名称 | [acr-ui/src/tour/manifest.js:155](../../acr-ui/src/tour/manifest.js#L155) |
| 下钻最近异常 | 最近异常 |  | 名称 | [acr-ui/src/tour/manifest.js:162](../../acr-ui/src/tour/manifest.js#L162) |
| 统一分析范围 | 筛选范围 |  | 名称 | [acr-ui/src/tour/manifest.js:169](../../acr-ui/src/tour/manifest.js#L169) |
| 先读四个核心指标 | 关键指标 |  | 名称 | [acr-ui/src/tour/manifest.js:176](../../acr-ui/src/tour/manifest.js#L176) |
| 结合趋势定位变化 | 变化趋势 |  | 名称 | [acr-ui/src/tour/manifest.js:183](../../acr-ui/src/tour/manifest.js#L183) |
| 下钻到项目 | 项目统计 |  | 名称 | [acr-ui/src/tour/manifest.js:190](../../acr-ui/src/tour/manifest.js#L190) |
| “接入项目”会直达代码项目配置，是管理员最短的开始路径。 | 从工作台的“接入项目”开始 |  | 说明 | [acr-ui/src/tour/manifest.js:20](../../acr-ui/src/tour/manifest.js#L20) |
| 新增对应 Git 平台的最小权限 Token，系统不会回显已保存的明文。 | 添加密钥，只开通所需权限<br>保存后不会显示完整密钥 |  | 说明 | [acr-ui/src/tour/manifest.js:27](../../acr-ui/src/tour/manifest.js#L27) |
| 在凭据列表执行“检测”，连接正常后再把它绑定到代码项目。 | 先测试连接，再将密钥用于代码项目 |  | 说明 | [acr-ui/src/tour/manifest.js:34](../../acr-ui/src/tour/manifest.js#L34) |
| 点击“新增”，按向导填写归属、仓库、分支和审查范围。 | 点击“添加”，填写项目和仓库信息 |  | 说明 | [acr-ui/src/tour/manifest.js:41](../../acr-ui/src/tour/manifest.js#L41) |
| 依次完成基础信息、仓库与分支，并确认需要触发的审查类型。 | 填写归属、仓库和分支<br>选择检查方式与范围 |  | 说明 | [acr-ui/src/tour/manifest.js:49](../../acr-ui/src/tour/manifest.js#L49) |
| 切到 Webhook 页，把平台生成的回调地址和 Secret 配到代码仓库。 | 将生成的地址和密钥填到代码平台 |  | 说明 | [acr-ui/src/tour/manifest.js:56](../../acr-ui/src/tour/manifest.js#L56) |
| 回调地址、事件类型和 Secret 必须与 Git 平台配置完全一致。 | 地址、事件和密钥须与代码平台一致 |  | 说明 | [acr-ui/src/tour/manifest.js:64](../../acr-ui/src/tour/manifest.js#L64) |
| 在 Git 平台创建或更新合并请求、或推送配置分支后，到这里确认任务已进入队列。 | 提交合并申请或推送指定分支<br>到检查任务中确认是否收到 |  | 说明 | [acr-ui/src/tour/manifest.js:71](../../acr-ui/src/tour/manifest.js#L71) |
| 工作台把待确认、待修复、待复核和失败事项聚合为可直达的卡片。 | 查看待确认、待修复和待复核事项 |  | 说明 | [acr-ui/src/tour/manifest.js:85](../../acr-ui/src/tour/manifest.js#L85) |
| 点击“待确认问题”会带着状态和归属筛选进入问题台账。 | 打开“待确认”，查看筛选后的问题 |  | 说明 | [acr-ui/src/tour/manifest.js:92](../../acr-ui/src/tour/manifest.js#L92) |
| 状态总览可快速切换待确认、待修复、疑似修复和累计关闭问题。 | 按问题状态查看需要跟进的事项 |  | 说明 | [acr-ui/src/tour/manifest.js:99](../../acr-ui/src/tour/manifest.js#L99) |
| 优先结合严重度、阶段、责任人和代码位置判断处置顺序。 | 按风险、责任人和代码位置安排处理 |  | 说明 | [acr-ui/src/tour/manifest.js:106](../../acr-ui/src/tour/manifest.js#L106) |
| 详情汇总上下文、定位、建议和审查轨迹，先核实再改变状态。 | 查看代码位置、建议和历史<br>核实后再改变状态 |  | 说明 | [acr-ui/src/tour/manifest.js:114](../../acr-ui/src/tour/manifest.js#L114) |
| 问题会沿发现、确认、待修复、疑似修复到关闭持续留痕。 | 查看从发现到关闭的处理过程 |  | 说明 | [acr-ui/src/tour/manifest.js:121](../../acr-ui/src/tour/manifest.js#L121) |
| 需要整改就确认或转派，不需治理则填写原因后关闭、忽略或标记误报。 | 需要修复就确认或转派<br>关闭、忽略或误报时填写原因 |  | 说明 | [acr-ui/src/tour/manifest.js:128](../../acr-ui/src/tour/manifest.js#L128) |
| 先看可见项目数和最近任务时间，避免在错误范围内解读指标。 | 先确认可见项目和最近检查时间 |  | 说明 | [acr-ui/src/tour/manifest.js:142](../../acr-ui/src/tour/manifest.js#L142) |
| 待办卡片优先暴露高风险结论、逾期问题、失败任务和投递异常。 | 先看高风险、逾期和失败事项 |  | 说明 | [acr-ui/src/tour/manifest.js:149](../../acr-ui/src/tour/manifest.js#L149) |
| 项目风险趋势按审查完成日期展示近期通过、警告、阻断和失败变化。 | 按检查完成日期查看风险变化 |  | 说明 | [acr-ui/src/tour/manifest.js:156](../../acr-ui/src/tour/manifest.js#L156) |
| 最近动态提供任务级入口，用于核对风险结论发生在哪个项目和变更。 | 打开最近异常，查看项目和代码改动 |  | 说明 | [acr-ui/src/tour/manifest.js:163](../../acr-ui/src/tour/manifest.js#L163) |
| 用时间范围和业务系统筛选固定统计口径，再比较不同周期。 | 先统一时间和业务系统，再比较数据 |  | 说明 | [acr-ui/src/tour/manifest.js:170](../../acr-ui/src/tour/manifest.js#L170) |
| 覆盖率、成功率、P95 时延和未处置重点问题共同反映质量与运行健康。 | 查看覆盖、成功、耗时和待处理问题 |  | 说明 | [acr-ui/src/tour/manifest.js:177](../../acr-ui/src/tour/manifest.js#L177) |
| 任务、问题类别和交付健康图表用于判断指标变化来自哪里。 | 结合任务、问题和发送趋势定位变化 |  | 说明 | [acr-ui/src/tour/manifest.js:184](../../acr-ui/src/tour/manifest.js#L184) |
| 项目矩阵用任务量、成功率、重点问题和处置率定位需要关注的项目。 | 按检查量、成功率和处理进度看项目 |  | 说明 | [acr-ui/src/tour/manifest.js:191](../../acr-ui/src/tour/manifest.js#L191) |
| 从平台凭据到 Webhook，再确认首个审查任务。 | 连接仓库，配置自动检查，查看任务 |  | 说明 | [acr-ui/src/tour/manifest.js:13](../../acr-ui/src/tour/manifest.js#L13) |
| 从工作台待办进入台账，完成确认或其他处置。 | 从待办进入问题，核实后选择操作 |  | 说明 | [acr-ui/src/tour/manifest.js:78](../../acr-ui/src/tour/manifest.js#L78) |
| 从当日风险信号下钻到趋势、指标与项目矩阵。 | 查看今日风险、质量趋势和项目统计 |  | 说明 | [acr-ui/src/tour/manifest.js:135](../../acr-ui/src/tour/manifest.js#L135) |

### 公共页面

| 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- |
| 401错误! | 无法访问 |  | 名称 | [acr-ui/src/views/error/401.vue:9](../../acr-ui/src/views/error/401.vue#L9) |
| 您没有访问权限！ | 你没有此页面的访问权限 |  | 提示 | [acr-ui/src/views/error/401.vue:11](../../acr-ui/src/views/error/401.vue#L11) |
| 对不起，您没有访问权限，请不要进行非法操作！您可以返回主页面 | 请联系管理员，或返回工作台 |  | 提示 | [acr-ui/src/views/error/401.vue:12](../../acr-ui/src/views/error/401.vue#L12) |
| 回首页 | 回工作台 |  | 按钮 | [acr-ui/src/views/error/401.vue:16](../../acr-ui/src/views/error/401.vue#L16) |
| 404错误! | 无此页面 |  | 名称 | [acr-ui/src/views/error/404.vue:12](../../acr-ui/src/views/error/404.vue#L12) |
| 找不到网页！ | 找不到此页面 |  | 提示 | [acr-ui/src/views/error/404.vue:30](../../acr-ui/src/views/error/404.vue#L30) |
| 对不起，您正在寻找的页面不存在。尝试检查URL的错误，然后按浏览器上的刷新按钮或尝试在我们的应用程序中找到其他内容。 | 请检查页面地址，或返回工作台 |  | 提示 | [acr-ui/src/views/error/404.vue:18](../../acr-ui/src/views/error/404.vue#L18) |
| 返回首页 | 回工作台 |  | 按钮 | [acr-ui/src/views/error/404.vue:21](../../acr-ui/src/views/error/404.vue#L21) |
| 系统已锁定，请输入密码解锁 | 已锁屏，输入登录密码即可解锁 |  | 提示 | [acr-ui/src/views/lock.vue:14](../../acr-ui/src/views/lock.vue#L14) |
| 请输入登录密码 | 输入登录密码 |  | 提示 | [acr-ui/src/views/lock.vue:17](../../acr-ui/src/views/lock.vue#L17) |
| 退出重新登录 | 重新登录 |  | 按钮 | [acr-ui/src/views/lock.vue:27](../../acr-ui/src/views/lock.vue#L27) |

## 权限树功能名称

这些名称显示在授权界面，权限标识及实际授权范围保持原样。

| 位置 | 原文案 | 新文案 | 描述（≤20字） | 用途 | 原文位置 |
| --- | --- | --- | --- | --- | --- |
| 用户管理 / 权限 | 用户查询 | 查看账号 | 查看账号 | 功能名称 | [sql/init-full.sql:1428](../../sql/init-full.sql#L1428) |
| 用户管理 / 权限 | 用户新增 | 添加账号 | 添加账号 | 功能名称 | [sql/init-full.sql:1429](../../sql/init-full.sql#L1429) |
| 用户管理 / 权限 | 用户修改 | 编辑账号 | 编辑账号 | 功能名称 | [sql/init-full.sql:1430](../../sql/init-full.sql#L1430) |
| 用户管理 / 权限 | 用户删除 | 删除账号 | 删除账号 | 功能名称 | [sql/init-full.sql:1431](../../sql/init-full.sql#L1431) |
| 用户管理 / 权限 | 用户导出 | 导出账号 | 导出账号 | 功能名称 | [sql/init-full.sql:1432](../../sql/init-full.sql#L1432) |
| 用户管理 / 权限 | 用户导入 | 导入账号 | 导入账号 | 功能名称 | [sql/init-full.sql:1433](../../sql/init-full.sql#L1433) |
| 用户管理 / 权限 | 重置密码 | 重置密码 | 为账号重新设置登录密码 | 功能名称 | [sql/init-full.sql:1434](../../sql/init-full.sql#L1434) |
| 角色管理 / 权限 | 角色查询 | 查看角色 | 查看角色 | 功能名称 | [sql/init-full.sql:1435](../../sql/init-full.sql#L1435) |
| 角色管理 / 权限 | 角色新增 | 添加角色 | 添加角色 | 功能名称 | [sql/init-full.sql:1436](../../sql/init-full.sql#L1436) |
| 角色管理 / 权限 | 角色修改 | 编辑角色 | 编辑角色 | 功能名称 | [sql/init-full.sql:1437](../../sql/init-full.sql#L1437) |
| 角色管理 / 权限 | 角色删除 | 删除角色 | 删除角色 | 功能名称 | [sql/init-full.sql:1438](../../sql/init-full.sql#L1438) |
| 角色管理 / 权限 | 角色导出 | 导出角色 | 导出角色 | 功能名称 | [sql/init-full.sql:1439](../../sql/init-full.sql#L1439) |
| 菜单管理 / 权限 | 菜单查询 | 查看菜单 | 查看菜单 | 功能名称 | [sql/init-full.sql:1440](../../sql/init-full.sql#L1440) |
| 菜单管理 / 权限 | 菜单新增 | 添加菜单 | 添加菜单 | 功能名称 | [sql/init-full.sql:1441](../../sql/init-full.sql#L1441) |
| 菜单管理 / 权限 | 菜单修改 | 编辑菜单 | 编辑菜单 | 功能名称 | [sql/init-full.sql:1442](../../sql/init-full.sql#L1442) |
| 菜单管理 / 权限 | 菜单删除 | 删除菜单 | 删除菜单 | 功能名称 | [sql/init-full.sql:1443](../../sql/init-full.sql#L1443) |
| 部门管理 / 权限 | 部门查询 | 查看部门 | 查看部门 | 功能名称 | [sql/init-full.sql:1444](../../sql/init-full.sql#L1444) |
| 部门管理 / 权限 | 部门新增 | 添加部门 | 添加部门 | 功能名称 | [sql/init-full.sql:1445](../../sql/init-full.sql#L1445) |
| 部门管理 / 权限 | 部门修改 | 编辑部门 | 编辑部门 | 功能名称 | [sql/init-full.sql:1446](../../sql/init-full.sql#L1446) |
| 部门管理 / 权限 | 部门删除 | 删除部门 | 删除部门 | 功能名称 | [sql/init-full.sql:1447](../../sql/init-full.sql#L1447) |
| 岗位管理 / 权限 | 岗位查询 | 查看岗位 | 查看岗位 | 功能名称 | [sql/init-full.sql:1448](../../sql/init-full.sql#L1448) |
| 岗位管理 / 权限 | 岗位新增 | 添加岗位 | 添加岗位 | 功能名称 | [sql/init-full.sql:1449](../../sql/init-full.sql#L1449) |
| 岗位管理 / 权限 | 岗位修改 | 编辑岗位 | 编辑岗位 | 功能名称 | [sql/init-full.sql:1450](../../sql/init-full.sql#L1450) |
| 岗位管理 / 权限 | 岗位删除 | 删除岗位 | 删除岗位 | 功能名称 | [sql/init-full.sql:1451](../../sql/init-full.sql#L1451) |
| 岗位管理 / 权限 | 岗位导出 | 导出岗位 | 导出岗位 | 功能名称 | [sql/init-full.sql:1452](../../sql/init-full.sql#L1452) |
| 字典管理 / 权限 | 字典查询 | 查看选项 | 查看选项 | 功能名称 | [sql/init-full.sql:1453](../../sql/init-full.sql#L1453) |
| 字典管理 / 权限 | 字典新增 | 添加选项 | 添加选项 | 功能名称 | [sql/init-full.sql:1454](../../sql/init-full.sql#L1454) |
| 字典管理 / 权限 | 字典修改 | 编辑选项 | 编辑选项 | 功能名称 | [sql/init-full.sql:1455](../../sql/init-full.sql#L1455) |
| 字典管理 / 权限 | 字典删除 | 删除选项 | 删除选项 | 功能名称 | [sql/init-full.sql:1456](../../sql/init-full.sql#L1456) |
| 字典管理 / 权限 | 字典导出 | 导出选项 | 导出选项 | 功能名称 | [sql/init-full.sql:1457](../../sql/init-full.sql#L1457) |
| 参数设置 / 权限 | 参数查询 | 查看设置 | 查看设置 | 功能名称 | [sql/init-full.sql:1458](../../sql/init-full.sql#L1458) |
| 参数设置 / 权限 | 参数新增 | 添加设置 | 添加设置 | 功能名称 | [sql/init-full.sql:1459](../../sql/init-full.sql#L1459) |
| 参数设置 / 权限 | 参数修改 | 编辑设置 | 编辑设置 | 功能名称 | [sql/init-full.sql:1460](../../sql/init-full.sql#L1460) |
| 参数设置 / 权限 | 参数删除 | 删除设置 | 删除设置 | 功能名称 | [sql/init-full.sql:1461](../../sql/init-full.sql#L1461) |
| 参数设置 / 权限 | 参数导出 | 导出设置 | 导出设置 | 功能名称 | [sql/init-full.sql:1462](../../sql/init-full.sql#L1462) |
| 通知公告 / 权限 | 公告查询 | 查看公告 | 查看公告 | 功能名称 | [sql/init-full.sql:1463](../../sql/init-full.sql#L1463) |
| 通知公告 / 权限 | 公告新增 | 添加公告 | 添加公告 | 功能名称 | [sql/init-full.sql:1464](../../sql/init-full.sql#L1464) |
| 通知公告 / 权限 | 公告修改 | 编辑公告 | 编辑公告 | 功能名称 | [sql/init-full.sql:1465](../../sql/init-full.sql#L1465) |
| 通知公告 / 权限 | 公告删除 | 删除公告 | 删除公告 | 功能名称 | [sql/init-full.sql:1466](../../sql/init-full.sql#L1466) |
| 操作日志 / 权限 | 操作查询 | 查看操作 | 查看系统操作记录 | 功能名称 | [sql/init-full.sql:1467](../../sql/init-full.sql#L1467) |
| 操作日志 / 权限 | 操作删除 | 删除记录 | 删除所选操作记录 | 功能名称 | [sql/init-full.sql:1468](../../sql/init-full.sql#L1468) |
| 操作日志 / 权限 | 日志导出 | 导出记录 | 下载操作或登录记录 | 功能名称 | [sql/init-full.sql:1469](../../sql/init-full.sql#L1469) |
| 登录日志 / 权限 | 登录查询 | 查看登录 | 查看账号登录记录 | 功能名称 | [sql/init-full.sql:1470](../../sql/init-full.sql#L1470) |
| 登录日志 / 权限 | 登录删除 | 删除记录 | 删除所选登录记录 | 功能名称 | [sql/init-full.sql:1471](../../sql/init-full.sql#L1471) |
| 登录日志 / 权限 | 日志导出 | 导出记录 | 下载操作或登录记录 | 功能名称 | [sql/init-full.sql:1472](../../sql/init-full.sql#L1472) |
| 登录日志 / 权限 | 账户解锁 | 解锁账号 | 解除账号登录锁定 | 功能名称 | [sql/init-full.sql:1473](../../sql/init-full.sql#L1473) |
| 在线用户 / 权限 | 在线查询 | 查看在线 | 查看当前登录账号 | 功能名称 | [sql/init-full.sql:1474](../../sql/init-full.sql#L1474) |
| 在线用户 / 权限 | 批量强退 | 批量退出 | 让所选账号退出当前登录 | 功能名称 | [sql/init-full.sql:1475](../../sql/init-full.sql#L1475) |
| 在线用户 / 权限 | 单条强退 | 退出登录 | 让该账号退出当前登录 | 功能名称 | [sql/init-full.sql:1476](../../sql/init-full.sql#L1476) |
| 定时任务 / 权限 | 任务查询 | 查看任务 | 查看任务 | 功能名称 | [sql/init-full.sql:1477](../../sql/init-full.sql#L1477) |
| 定时任务 / 权限 | 任务新增 | 添加任务 | 添加任务 | 功能名称 | [sql/init-full.sql:1478](../../sql/init-full.sql#L1478) |
| 定时任务 / 权限 | 任务修改 | 编辑任务 | 编辑任务 | 功能名称 | [sql/init-full.sql:1479](../../sql/init-full.sql#L1479) |
| 定时任务 / 权限 | 任务删除 | 删除任务 | 删除任务 | 功能名称 | [sql/init-full.sql:1480](../../sql/init-full.sql#L1480) |
| 定时任务 / 权限 | 状态修改 | 设置状态 | 启用或暂停定时任务 | 功能名称 | [sql/init-full.sql:1481](../../sql/init-full.sql#L1481) |
| 定时任务 / 权限 | 任务导出 | 导出任务 | 导出任务 | 功能名称 | [sql/init-full.sql:1482](../../sql/init-full.sql#L1482) |
| 大模型配置 / 权限 | 模型查询 | 查看模型 | 查看模型 | 功能名称 | [sql/init-full.sql:1483](../../sql/init-full.sql#L1483) |
| 大模型配置 / 权限 | 模型新增 | 添加模型 | 添加模型 | 功能名称 | [sql/init-full.sql:1484](../../sql/init-full.sql#L1484) |
| 大模型配置 / 权限 | 模型修改 | 编辑模型 | 编辑模型 | 功能名称 | [sql/init-full.sql:1485](../../sql/init-full.sql#L1485) |
| 大模型配置 / 权限 | 模型删除 | 删除模型 | 删除模型 | 功能名称 | [sql/init-full.sql:1486](../../sql/init-full.sql#L1486) |
| 业务系统 / 权限 | 业务系统查询 | 查看系统 | 查看系统 | 功能名称 | [sql/init-full.sql:1487](../../sql/init-full.sql#L1487) |
| 业务系统 / 权限 | 业务系统新增 | 添加系统 | 添加系统 | 功能名称 | [sql/init-full.sql:1488](../../sql/init-full.sql#L1488) |
| 业务系统 / 权限 | 业务系统修改 | 编辑系统 | 编辑系统 | 功能名称 | [sql/init-full.sql:1489](../../sql/init-full.sql#L1489) |
| 业务系统 / 权限 | 业务系统删除 | 删除系统 | 删除系统 | 功能名称 | [sql/init-full.sql:1490](../../sql/init-full.sql#L1490) |
| 代码项目 / 权限 | 项目查询 | 查看项目 | 查看项目 | 功能名称 | [sql/init-full.sql:1491](../../sql/init-full.sql#L1491) |
| 代码项目 / 权限 | 项目新增 | 添加项目 | 添加项目 | 功能名称 | [sql/init-full.sql:1492](../../sql/init-full.sql#L1492) |
| 代码项目 / 权限 | 项目修改 | 编辑项目 | 编辑项目 | 功能名称 | [sql/init-full.sql:1493](../../sql/init-full.sql#L1493) |
| 代码项目 / 权限 | 项目删除 | 删除项目 | 删除项目 | 功能名称 | [sql/init-full.sql:1494](../../sql/init-full.sql#L1494) |
| 代码项目 / 权限 | 项目启停 | 启停项目 | 启停项目 | 功能名称 | [sql/init-full.sql:1495](../../sql/init-full.sql#L1495) |
| 代码项目 / 权限 | 项目连接与同步 | 连接同步 | 检测仓库连接并同步分支 | 功能名称 | [sql/init-full.sql:1496](../../sql/init-full.sql#L1496) |
| 访问凭据 / 权限 | 凭据查询 | 查看密钥 | 查看密钥 | 功能名称 | [sql/init-full.sql:1497](../../sql/init-full.sql#L1497) |
| 访问凭据 / 权限 | 凭据新增 | 添加密钥 | 添加密钥 | 功能名称 | [sql/init-full.sql:1498](../../sql/init-full.sql#L1498) |
| 访问凭据 / 权限 | 凭据修改 | 编辑密钥 | 编辑密钥 | 功能名称 | [sql/init-full.sql:1499](../../sql/init-full.sql#L1499) |
| 访问凭据 / 权限 | 凭据删除 | 删除密钥 | 删除密钥 | 功能名称 | [sql/init-full.sql:1500](../../sql/init-full.sql#L1500) |
| 访问凭据 / 权限 | 凭据连接测试 | 测试连接 | 确认密钥能连接代码平台 | 功能名称 | [sql/init-full.sql:1501](../../sql/init-full.sql#L1501) |
| 审查任务 / 权限 | 任务查询 | 查看任务 | 查看任务 | 功能名称 | [sql/init-full.sql:1502](../../sql/init-full.sql#L1502) |
| 审查引擎 / 权限 | 引擎查询 | 查看工具 | 查看工具 | 功能名称 | [sql/init-full.sql:1503](../../sql/init-full.sql#L1503) |
| 审查引擎 / 权限 | 环境检测 | 检测环境 | 检查服务器是否已安装工具 | 功能名称 | [sql/init-full.sql:1504](../../sql/init-full.sql#L1504) |
| 审查引擎 / 权限 | 测试调用 | 测试运行 | 试用当前检查工具 | 功能名称 | [sql/init-full.sql:1505](../../sql/init-full.sql#L1505) |
| 审查任务 / 权限 | 任务重试 | 重试任务 | 重试任务 | 功能名称 | [sql/init-full.sql:1506](../../sql/init-full.sql#L1506) |
| 审查模板 / 权限 | 模板查询 | 查看模板 | 查看模板 | 功能名称 | [sql/init-full.sql:1511](../../sql/init-full.sql#L1511) |
| 审查模板 / 权限 | 模板新增 | 添加模板 | 添加模板 | 功能名称 | [sql/init-full.sql:1512](../../sql/init-full.sql#L1512) |
| 审查模板 / 权限 | 模板修改 | 编辑模板 | 编辑模板 | 功能名称 | [sql/init-full.sql:1513](../../sql/init-full.sql#L1513) |
| 审查模板 / 权限 | 模板删除 | 删除模板 | 删除模板 | 功能名称 | [sql/init-full.sql:1514](../../sql/init-full.sql#L1514) |
| 审查记录 / 权限 | 记录查询 | 查看记录 | 查看记录 | 功能名称 | [sql/init-full.sql:1515](../../sql/init-full.sql#L1515) |
| 审查记录 / 权限 | 投递重试 | 重新发送 | 重新发送失败的评论或通知 | 功能名称 | [sql/init-full.sql:1516](../../sql/init-full.sql#L1516) |
| 通知渠道 / 权限 | 渠道查询 | 查看通知 | 查看通知 | 功能名称 | [sql/init-full.sql:1517](../../sql/init-full.sql#L1517) |
| 通知渠道 / 权限 | 渠道新增 | 添加通知 | 添加通知 | 功能名称 | [sql/init-full.sql:1518](../../sql/init-full.sql#L1518) |
| 通知渠道 / 权限 | 渠道修改 | 编辑通知 | 编辑通知 | 功能名称 | [sql/init-full.sql:1519](../../sql/init-full.sql#L1519) |
| 通知渠道 / 权限 | 渠道删除 | 删除通知 | 删除通知 | 功能名称 | [sql/init-full.sql:1520](../../sql/init-full.sql#L1520) |
| 通知渠道 / 权限 | 渠道启停 | 启停通知 | 启停通知 | 功能名称 | [sql/init-full.sql:1521](../../sql/init-full.sql#L1521) |
| 通知渠道 / 权限 | 测试发送 | 测试发送 | 发送测试消息，检查连接 | 功能名称 | [sql/init-full.sql:1522](../../sql/init-full.sql#L1522) |
| 通知渠道 / 权限 | 渠道列表 | 通知列表 | 通知列表 | 功能名称 | [sql/init-full.sql:1523](../../sql/init-full.sql#L1523) |
| 投递记录 / 权限 | 投递查询 | 查看发送 | 查看发送 | 功能名称 | [sql/init-full.sql:1524](../../sql/init-full.sql#L1524) |
| 投递记录 / 权限 | 投递列表 | 发送列表 | 发送列表 | 功能名称 | [sql/init-full.sql:1525](../../sql/init-full.sql#L1525) |
| 问题台账 / 权限 | 问题列表 | 问题列表 | 问题列表 | 功能名称 | [sql/init-full.sql:1526](../../sql/init-full.sql#L1526) |
| 问题台账 / 权限 | 问题查询 | 查看问题 | 查看问题 | 功能名称 | [sql/init-full.sql:1527](../../sql/init-full.sql#L1527) |
| 问题台账 / 权限 | 问题确认 | 确认问题 | 确认问题需要修复 | 功能名称 | [sql/init-full.sql:1528](../../sql/init-full.sql#L1528) |
| 问题台账 / 权限 | 问题关闭 | 关闭问题 | 关闭问题并保留处理记录 | 功能名称 | [sql/init-full.sql:1529](../../sql/init-full.sql#L1529) |
| 总览看板 / 权限 | 总览查看 | 查看总览 | 查看整体质量统计 | 功能名称 | [sql/init-full.sql:1530](../../sql/init-full.sql#L1530) |
| 项目分析 / 权限 | 项目查看 | 查看项目 | 查看项目质量统计 | 功能名称 | [sql/init-full.sql:1531](../../sql/init-full.sql#L1531) |
| 成员分析 / 权限 | 团队查看 | 查看团队 | 查看权限范围内的成员统计 | 功能名称 | [sql/init-full.sql:1532](../../sql/init-full.sql#L1532) |
| 运行概览 / 权限 | 运行概览查看 | 查看运行 | 查看检查资源、积压和异常 | 功能名称 | [sql/init-full.sql:1533](../../sql/init-full.sql#L1533) |
| 审查任务 / 权限 | 任务终止 | 终止任务 | 取消任务，停止继续执行 | 功能名称 | [sql/init-full.sql:1534](../../sql/init-full.sql#L1534) |
| 审查任务 / 权限 | 任务处置 | 异常处理 | 处理积压或标记人工处理 | 功能名称 | [sql/init-full.sql:1535](../../sql/init-full.sql#L1535) |
| 成员分析 / 权限 | 成员身份管理 | 关联账号 | 管理提交身份与账号的关联 | 功能名称 | [sql/init-full.sql:1536](../../sql/init-full.sql#L1536) |
| Token 用量分析 / 权限 | Token 用量查看 | 查看用量 | 查看模型用量和估算费用 | 功能名称 | [sql/init-full.sql:1537](../../sql/init-full.sql#L1537) |

## 核对结果

已核对 86 个源文件中的原文位置；按已确认的技术原名例外，名称字数、描述长度及短提示长度均通过检查。同一出处、同一用途没有冲突的新文案。

原文对照以提交 `9f1d923` 为基线。P95 指标已另行按真实口径修正，其余建议尚未批量应用界面；检查算法与功能保持原样。未进行后台部署或界面实测。
