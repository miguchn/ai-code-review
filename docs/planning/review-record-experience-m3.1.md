# M3.1 审查任务与审查记录体验优化

> **实施状态（2026-08-02）：已落地。** 审查任务定位为执行队列；审查记录展示已结束任务（SUCCESS + FAILED）；复用 `review_task` / `review_task_run`，不新建业务表；PR 发起人与变更规模随既有 PR 元数据请求落库。不含 GitHub PR 评论回写、交付状态、通知、问题整改与质量门禁。

## 1. 目标与成功指标

- 运维人员能在「审查任务」快速定位待执行 / 执行中 / 失败任务并重试；
- 业务人员能在「审查记录」按项目、PR、发起人、结论与完成时间查阅已结束结果；
- 详情改为独立页面；记录详情分区展示「审查结果」与「执行记录」；
- 历史空字段前端友好展示为 `--`，不报错。

## 2. 范围 / 非范围

### 范围

1. 精简审查任务列表，默认队列视图（PENDING/RUNNING/FAILED）；
2. 审查记录：`SUCCESS + FAILED`，按完成时间倒序；
3. 列表字段：项目+业务系统、可点击 PR、PR 发起人（login）、分支、代码变更（文件数/+/-）、结论映射、评分、重点问题分级（Top3）、完成时间、操作；
4. 操作：查看详情、查看问题（详情默认锚定重点问题区）、打开 PR；失败记录「重新执行」（复用 `review:task:retry`）；
5. 补充 `pr_author` / `additions` / `deletions` / `changed_files`（Webhook + 执行时同一 PR 详情请求回填）；
6. 复用部门/项目数据范围。

### 非范围

- 交付状态 / GitHub 评论回写（M4）
- 独立问题页面、全量问题台账
- 为展示名单独请求 GitHub User API；不以 commit.author.name 兜底

## 3. 结论映射（不新增状态字典）

| 展示 | 来源 |
|---|---|
| 通过 | `task_status=SUCCESS` 且 `review_conclusion=PASS` |
| 建议修改 | `SUCCESS` 且 `WARN` |
| 高风险 | `SUCCESS` 且 `BLOCK` |
| 执行失败 | `task_status=FAILED`（评分与重点问题显示 `--`） |

## 4. 数据与接口

| 对象 | 变化 |
|---|---|
| `review_task` | + `pr_author`、`additions`、`deletions`、`changed_files` |
| `review_task_run` | 继续承载 Commit Message / 评分 / Top3 |
| 菜单 | 审查记录 `128`，`review:record:list` / `review:record:query` |

脚本：`19` / `20`（乱码修复）/ `21`（changed_files）；执行须 `--default-character-set=utf8mb4`。

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/review/record/list` | SUCCESS+FAILED；筛项目/PR/发起人/结论（含 FAILED）/完成时间 |
| GET | `/review/record/{id}` | 已结束任务详情 |
| POST | `/review/task/{id}/retry` | 失败记录重新执行（既有权限） |

## 5. 已知边界

- 历史空字段显示 `--`；
- 重点问题为结构化 Top 3 分级统计，页面标明非全量；
- OCR 路径复用同一 PR 元数据请求补齐发起人与变更规模；
- 交付状态待 M4 接入，本切片不硬编码「未回写」；
- 业务系统可被物理删除（删除无引用校验）：任务/记录查询已用 LEFT JOIN 兜底，名称展示为「—」；部门或负责人被删除时同类可见性风险仍存在（M1 既有边界，未在本切片收敛）。
