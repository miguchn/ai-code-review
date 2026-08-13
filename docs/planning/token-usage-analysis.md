# Token 用量分析设计（数据洞察二级菜单）

- 状态：**设计评审稿**（2026-08-13）
- 菜单归属：数据洞察（menu 7）下新增二级菜单「Token 用量分析」
- 前置事实源：`data-insights-module-design.md`（洞察基础设施）、`production-readiness-governance.md` §5（Token/成本/配额当前标记为未交付）
- 本设计交付「用量与成本观察」，不交付预算配额强控（路线图企业版/规模化版项，另立项）

## 0. 底层数据现状核查结论（设计地基）

**当前系统 Token 数据采集为零**，核查证据：

| 链路 | 现状 | 结论 |
|---|---|---|
| LLM_DIRECT 直审 | `LlmCallServiceImpl.invokeHttp` 调用 OpenAI 兼容 `/chat/completions`，响应体标准包含 `usage`（prompt_tokens/completion_tokens/total_tokens），但 `LlmCallResult` 只解析 content/latency，**usage 被丢弃** | 可采集，改动最小、确定性最高，作为采集主通道 |
| OCR_ENGINE 引擎 | CLI 为原生二进制封装，输出 JSON 的 usage 字段无法静态确认（README 将 Avg Token 列为基准指标，内部大概率统计） | 尽力采集：开发联调时验证输出；有则落库，无则留空，页面如实展示缺口 |
| review_task_run | 无任何 token 字段；**已有完整归属键**：snapshot_model_id/name/provider、task→project_id、时间戳 | 加列即可，归属链路天然完整 |
| sys_ai_model_config | 无单价字段 | 成本测算需补两个单价列 |

**关联键完备性结论**：run → 模型（快照）→ 项目（task）→ 时间，四维分析所需的关联字段全部已存在，只缺用量数值本身。

## 1. 数据采集与数据模型（最小必要范围）

### 1.1 review_task_run 增 3 列（sql/48，幂等 ALTER）

| 列 | 类型 | 说明 |
|---|---|---|
| input_tokens | int NULL | 输入 token；无数据为 NULL（不用 0，区分「未采集」与「零消耗」） |
| output_tokens | int NULL | 输出 token |
| total_tokens | int NULL | 总 token（冗余落库，避免读取时相加口径漂移） |

数据缺口指标用 NULL/非 NULL 表达即可，不单独设采集通道字段。

采集实现点：
- `LlmCallResult` 增 promptTokens/completionTokens/totalTokens 三字段；`invokeHttp` 解析响应 `usage` 节点（解析失败静默置空，不得影响审查主流程——用量采集是旁路，永远不能反过来弄坏审查）
- LLM_DIRECT：执行服务在 run 落库时写入，成功与失败都写（失败请求同样消耗 token，成本观察必须包含）
- OCR_ENGINE：`ReviewEngineResultMapper` 尝试读取输出 JSON 的 usage/tokens 类字段，存在则写入

### 1.2 sys_ai_model_config 增 2 列（成本测算）

| 列 | 类型 | 说明 |
|---|---|---|
| input_price_per_1k | decimal(10,4) NULL | 输入单价（元/千 token） |
| output_price_per_1k | decimal(10,4) NULL | 输出单价（元/千 token） |

- 模型配置页表单补两个数字输入（平台权限，现有模型配置页即改）
- **单价未配置时成本显示「—」**，不用任何估算值冒充

### 1.3 查询方式：直查 review_task_run，不建聚合表

Token 数据是 run 级精确值（无弱匹配口径问题），数据量为审查次数级，平台洞察下钻已有直查源表先例（ReviewStatsSourceMapper）。因此**不新增聚合表、不扩展聚合任务**，页面所有维度直接查询 `review_task_run`（JOIN review_task 取项目归属），配套两个索引（sql/48 幂等创建）：

```
idx_run_token_time  (create_time, snapshot_model_id)   -- 趋势与模型维度
idx_run_task        （已有 task_id 索引，项目维度经 task.project_id 走既有索引）
```

真实规模压力出现时再引入聚合表——届时数据已在 run 表，迁移无成本。

### 1.4 统计口径（指标字典必须原文引用）

| 指标 | 口径 |
|---|---|
| 调用次数 | 产生 token 记录的 run 数；重试每次 run 各计一次（每次都是真实调用） |
| Token 合计 | SUM 忽略 NULL；NULL 记录不进合计，但计入「无 Token 数据」缺口指标 |
| 时间归属 | run 创建时间（调用发生时刻），非任务完成时间 |
| 估算成本 | input/1000×输入单价 + output/1000×输出单价，按**当前**单价；单价缺失为「—」 |
| 数据缺口 | token 字段为 NULL 的成功 run 占比（OCR 未输出 usage 时如实暴露） |
| 历史数据 | 采集上线前的 run 无 token 数据，**不回填**（无法还原）；页面用 dataSince 模式提示「数据自 X 起积累」 |

## 2. 页面结构（Token 用量分析，menu 138）

与项目分析/成员分析完全同构的标准三段式：

```
1. 搜索栏（v-show 显隐，label-width 84px）
   时间范围（近7天/近30天/自定义）+ 模型（下拉，模型配置接口）+ 项目（下拉，授权范围）+ 查询/重置
2. 操作行
   right-toolbar（搜索显隐 + 刷新）
3. 数据区（骨架屏 → 内容）
   ├── KPI 卡行（5 张）：总 Token / 输入 Token / 输出 Token / 调用次数 / 估算成本（带环比）
   ├── Token 趋势：堆叠柱或折线（输入/输出按日），X 轴全区间补零
   ├── 模型维度：占比环图 + 模型表（模型/厂商/调用次数/输入/输出/总Token/估算成本/占比，可排序）
   ├── 项目维度：排行条形图（Top10）+ 项目表（项目/业务系统/负责人/调用次数/总Token/估算成本，可排序）
   └── 审查维度：审查记录明细表（记录ID/项目/模型/审查方式/触发时间/输入/输出/总Token/估算成本，
       可排序分页），行点击跳既有审查记录详情页
```

### 2.1 分析链路与下钻（单页联动，不做多子页）

```
总体（KPI+趋势）
  → 点模型表某行 = 设置模型筛选并刷新（全页数据按该模型过滤）
  → 点项目表某行 = 设置项目筛选并刷新 + 滚动锚定到审查维度表
  → 审查维度表 = 当前筛选条件下的 run 明细，行点击 → 既有审查记录详情（详情展示该 run 的 token 明细）
```

决策理由：三个维度共享同一份筛选状态，单页 + 筛选联动即可覆盖「总体→模型/项目→单次审查」全链路；拆三个子页会复制三套筛选器和数据请求，违背不重复造轮子原则。

### 2.2 审查记录详情联动（顺带小项）

既有审查记录详情页补一行 Token 信息（输入/输出/总/估算成本），数据随既有详情接口带出 run 新字段——让下钻终点能看到单次审查的实际消耗。

## 3. 权限与数据范围

- 菜单 138「Token 用量分析」，权限串 `insight:token:view`，route_name `InsightTokenUsage`
- 授权：超级管理员（角色 1 全量）、审查平台管理员（2）、项目负责人（4）、质量/安全（5）、审计人员（6）——参照 sql/46 授权模式。项目负责人可见**自己负责项目**的用量与成本（2026-08-13 决策：知情权属合理诉求，数据范围由项目成员边界锁死，不越权）；开发人员（3）不授权
- 数据范围：与洞察同源——DataScope × 项目成员交集（复用 `InsightScopeQueries` + `projectAccessUserId` 模式），列表与聚合查询统一叠加
- 模型单价编辑沿用模型配置既有平台权限，不新增权限串

## 4. 组件复用清单

| 需求 | 复用对象 | 新增？ |
|---|---|---|
| KPI 卡 | `InsightKpiCard` + `formatKpiValue`/`formatChange` | 否 |
| 趋势/占比/排行图 | `@/utils/echarts` 按需注册（已有 Bar/Line/Grid/Legend/Tooltip）+ 三页既有渲染模式；环图需补 `PieChart` 注册 | 仅补组件注册 |
| 筛选栏/工具栏 | 标准 inline form + right-toolbar + `toIdParam`/`toRangePreset`/`toDateRangeParam` | 否 |
| 时间范围 | `InsightRange`（后端，90 天上限） | 否 |
| 表格 | el-table sortable + 既有 empty-text 规范 | 否 |
| 数据权限 | `InsightScopeQueries` 模式 | 否 |
| 查询性能 | run 表复合索引（sql/48 幂等创建） | 仅索引 |
| 模型下拉数据 | 既有模型配置查询接口（脱敏选项） | 否 |
| 页面骨架/空态 | insight 三页既有 skeleton/empty/dataSince 模式 | 否 |

**确需新增**：run 3 列采集逻辑、模型单价 2 列+表单 2 字段、查询索引 2 个、页面 vue + API 组、菜单/权限 SQL、PieChart 注册、记录详情 Token 行。

## 5. 迁移脚本（sql/48_token_usage_analysis.sql）

1. review_task_run 增 3 列（幂等 ALTER）
2. sys_ai_model_config 增 2 单价列（幂等 ALTER）
3. review_task_run 增索引 `idx_run_token_time (create_time, snapshot_model_id)`（幂等）
4. 菜单 138 + `insight:token:view` + 角色 2/4/5/6 授权（判重写法）
5. init-full 同步至 01–48（重新生成 + 临时库空库验证）

## 6. 验收标准

1. LLM_DIRECT 审查完成后，run 落库含 input/output/total tokens；失败 run 同样记录
2. 页面 KPI/趋势/模型表/项目表/审查明细与筛选联动正确；越权项目不可见（同洞察口径）
3. 模型/项目行点击联动筛选，审查明细行可跳记录详情，详情展示该次 run 的 token
4. 单价配置后估算成本正确；未配置显示「—」，不出现伪造数值
5. OCR 项目无 usage 时「数据缺口」指标如实显示，页面不报错不空白
6. 直查索引生效（趋势/模型/项目维度查询无全表扫描）；时间范围 7/30/自定义与既有洞察页行为一致
7. mvn test 全绿 + 前端生产构建通过；暗黑主题无硬编码色

## 7. 明确不做（防止范围蔓延）

- 不做预算、配额、告警（路线图企业版项，独立立项）；
- 不做按用户维度的 Token 统计（提交人与调用无直接对应，成员维度价值不成立）；
- 不做成本分摊/对账/账单导出（估算成本仅供观察，口径说明已声明）；
- 不做实时流式统计（日聚合 + 当日刷新与既有洞察一致，够用）；
- 不回填采集上线前的历史 token（物理不可还原）。

## 8. 风险与开放项

| 项 | 说明 | 处置 |
|---|---|---|
| OCR usage 输出未验证 | 静态无法确认 CLI JSON 是否含 usage（README 仅列基准指标 Avg Token）。实现已兼容 `usage`/`tokens` 及 `prompt_tokens`/`input_tokens`/`output_tokens`/`total_tokens` 等别名；有则落库，无则缺口指标兜底。真联调需在运行环境对 OCR 引擎实际输出验证。 | 有则落库，无则页面缺口指标兜底，不阻塞 |
| 成本为当前单价估算 | 单价调整影响历史成本展示 | 指标字典明示口径；如试点反馈需要历史准确成本，再评估单价快照 |
| 多轮调用场景 | 未来若一次审查多次调模型（重试/分段），run 级字段只记最后一次 | 当前架构一次 run 一次调用，字段语义=该 run 累计；若引入多调用需升级为调用流水表（本期不做） |
