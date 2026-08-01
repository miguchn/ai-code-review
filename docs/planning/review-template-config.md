# 项目审查模板公共配置 — 设计方案

> **状态：已实施并通过后端测试与前端生产构建（2026-08-01）**  
> **实施差异（2026-08-02 独立 Review）：未建设模板复制端点**——`POST /review/template/{id}/copy` 与 `review:template:copy` 权限未实现，菜单残留已随 `sql/18` 清理；「复制内置为自定义」通过新增接口完成（服务端强制 `builtin_flag=0`、`version_no=1`）。正文中含「复制」的段落为原设计表述，以本注记为准。  
> 目标：补齐「项目审查模板」公共配置能力，理顺大模型配置 / 审查引擎 / 审查模板 / 项目管理四类对象的职责边界。真实 GitHub 双方式 E2E 仍依赖目标环境，不标为已完成。

## 0. 目标与成功指标

### 业务目标

让国内研发团队按「项目类型」复用一套稳定的审查提示词与输出约定：选语言 → 选审查方式 → 绑定模板/模型或引擎；执行时冻结快照，配置变更只影响新任务。

### 成功指标

1. 四类对象职责在菜单、表单、校验、执行链路上互不混淆；
2. 内置 6 套模板可查看、可复制，默认模板不可被直接破坏性删除；
3. 大模型审查真实消费项目所选模板与模型；审查引擎不走平台模板；
4. 任务快照含审查方式、模板版本与正文、模型或引擎信息，后续改配置不影响旧任务；
5. 不引入规则引擎、多模板组合、审批发布或自动推荐。

---

## 1. 对象职责（第一性原理）

| 对象 | 已有落点 | 职责 | 明确不负责 |
|---|---|---|---|
| **大模型配置** | `sys_ai_model_config`（模型服务 → 大模型配置） | 厂商、地址、密钥、模型名、超时/温度等调用参数 | 审查话术、语言栈、引擎选型 |
| **审查引擎配置** | 现有审查引擎页 + `review.engine.*`（模型服务 → 审查引擎） | 引擎可用性、可执行文件、超时并发、探测/测试调用 | 平台提示词、项目语言、模板版本 |
| **项目审查模板** | **本次新建/升级**（代码审查 → 审查模板） | 按项目类型沉淀审查提示词、审查重点、输出要求；含语言/技术栈、版本、内置标识 | 模型密钥、引擎进程、仓库凭据 |
| **项目管理配置** | `review_project`（代码审查 → 代码项目） | 选主要语言/技术类型、审查方式，并引用模板+模型或引擎 | 维护模板正文、维护模型密钥 |

关系一句话：

```text
项目 = 选「怎么审」（方式）+「用谁审」（模型或引擎）+「按什么标准审」（模板，仅大模型）
模板 = 审查标准文本资产（可复用、可版本快照）
模型 / 引擎 = 执行器，与模板解耦
```

---

## 2. 菜单及页面位置

### 2.1 菜单

优先落在现有「代码审查」一级目录（`menu_id=3`）下，新增二级菜单：

| 项 | 建议值 |
|---|---|
| 菜单名 | 审查模板 |
| parent | 代码审查（3） |
| path | `template` |
| component | `review/template/index` |
| perms | `review:template:list` |
| order_num | 建议插在「代码项目」之后、「审查任务」之前（便于先配模板再建项目） |

按钮权限（与现有 RuoYi 模式一致）：

- `review:template:query`
- `review:template:add`
- `review:template:edit`
- `review:template:remove`

### 2.2 与现有「提示词管理」的关系

M3 整改已在「模型服务」下挂了 `review_prompt`（提示词管理）。该命名与职责落点不合理：

- 提示词属于**审查业务资产**，不是模型服务资产；
- 「提示词」一词易与厂商 system prompt、引擎内部 prompt 混淆。

**迁移决策（实施时执行，本方案先锁定）：**

1. 以「审查模板」为正式产品名与菜单名；
2. 数据层将现有 `review_prompt` **升级/迁入** `review_template`（见第 3 节），避免并行两套表长期共存；
3. 下线「模型服务 → 提示词管理」菜单（或隐藏并在说明中指向新菜单）；
4. 项目字段由 `prompt_id` 演进为 `template_id`（可保留兼容列过渡一期）。

不新增一级菜单。

### 2.3 页面形态

- 列表：名称、编码、适用语言/技术栈、内置/自定义、版本、状态、更新时间；
- 详情/编辑：提示词正文（大文本）+ 审查重点/输出要求说明（可合入正文，或拆 `focus_hint` / `output_schema_hint` 只读辅助字段，**首期建议合入正文，减少字段膨胀**）；
- 内置模板：详情可看；编辑入口改为「复制为自定义」；禁止删除；编码锁定；
- 自定义模板：完整 CRUD；删除前校验是否被项目引用。

---

## 3. 数据对象与字段

### 3.1 `review_template`（由 `review_prompt` 升级）

| 字段 | 类型 | 说明 |
|---|---|---|
| `template_id` | bigint PK | 原 `prompt_id` |
| `template_name` | varchar(100) | 展示名，如「Java 审查模板」 |
| `template_code` | varchar(64) UK | 稳定编码，如 `builtin_java` |
| `tech_stack` | varchar(40) | 适用语言/技术栈字典值：`JAVA`/`PYTHON`/`GO`/`VUE`/`REACT`/`FULLSTACK` |
| `content` | mediumtext | 提示词正文（含占位符约定） |
| `version_no` | int | 基础版本号，自定义模板每次「保存正文」+1；内置种子为 1 |
| `builtin_flag` | char(1) | `1` 内置 / `0` 自定义 |
| `status` | char(1) | `0` 启用 / `1` 停用 |
| `remark` | varchar(500) | 备注 |
| 审计字段 | | `create_by/time`、`update_by/time` |

索引：`uk_template_code`；`idx_template_stack_status (tech_stack, status)`。

**首期不建**：模板发布流、草稿态、多版本历史表、多语言 i18n 表。版本号足够支撑「任务快照比对与展示」。

### 3.2 内置模板种子（至少 6 套）

| 编码 | 名称 | tech_stack |
|---|---|---|
| `builtin_java` | Java | JAVA |
| `builtin_python` | Python | PYTHON |
| `builtin_go` | Go | GO |
| `builtin_vue` | Vue | VUE |
| `builtin_react` | React | REACT |
| `builtin_fullstack` | 全栈通用 | FULLSTACK |

正文约定（与现有渲染器对齐，避免另起占位符体系）：

- `{{pr_title}}` `{{source_branch}}` `{{target_branch}}` `{{base_sha}}` `{{head_sha}}` `{{diff}}`
- 正文只写技术栈审查重点与占位符（含 `{{pr_description}}` / `{{commit_messages}}`）；统一评分标准与 JSON 输出协议由平台在执行时追加，见 `docs/planning/review-scoring-result-protocol.md`

历史 `default_pr_review`：迁移为 `builtin_fullstack` 或保留编码映射一期后废弃。

### 3.3 `review_project` 增量

| 字段 | 说明 |
|---|---|
| `primary_stack` | 项目主要语言/技术类型（字典，同 `tech_stack`） |
| `review_mode` | 已有：`LLM_DIRECT` / `OCR_ENGINE` |
| `model_id` | 已有：仅大模型审查必填 |
| `template_id` | 替代/演进自 `prompt_id`：仅大模型审查必填 |
| `engine_code` | 已有：仅审查引擎必填 |

互斥清理规则保持：

- `LLM_DIRECT` → 要求 `model_id`+`template_id`，`engine_code=NULL`
- `OCR_ENGINE` → 要求 `engine_code`，`model_id=NULL`、`template_id=NULL`

### 3.4 任务快照（满足「改配置只影响新任务」）

用户要求：**创建审查任务时**即冻结配置。相对当前 M3「领取执行时再读项目配置」需调整。

**推荐策略（最小改动、语义正确）：**

1. **建单时**（Webhook → `ReviewTaskCreateService`）从项目读取并写入 `review_task` 执行配置快照字段；
2. **执行时**只读任务快照，不再回读项目的方式/模板/模型/引擎（项目停用、凭据仍按项目实时校验）；
3. **run 表**在每次 attempt 复制任务快照 + 补充引擎版本/耗时/结果；重试沿用任务创建时快照，不刷新为项目最新配置。

`review_task` 建议新增（或复用命名清晰的 snapshot 列）：

| 字段 | 说明 |
|---|---|
| `snapshot_review_mode` | 审查方式 |
| `snapshot_template_id/name/code` | 模板引用 |
| `snapshot_template_version` | 模板版本号 |
| `snapshot_prompt_content` | 模板正文（大模型路径必填；引擎路径为空） |
| `snapshot_model_id/name/provider/model` | 大模型路径 |
| `snapshot_engine_code/name` | 引擎路径 |

`review_task_run` 保持现有快照列，执行时从任务快照拷贝；可增加 `snapshot_template_version`、`snapshot_template_code`。

密钥、PAT、Webhook Secret **永不**进快照。

### 3.5 字典

新增/复用：

- `review_tech_stack`：JAVA / PYTHON / GO / VUE / REACT / FULLSTACK（中文标签）
- `review_mode`：已有，继续用
- `review_engine_code`：已有，继续用

---

## 4. 项目配置交互

沿用现有项目弹窗分区（基础信息 / 仓库与分支 / Webhook / **审查执行**），在「审查执行」Tab 调整为：

```text
1. 项目主要语言/技术类型 *（primary_stack）
2. 审查方式 *（二选一 radio）
   ├─ 大模型审查
   │    ├─ 大模型配置 *（仅启用项；来自模型服务）
   │    └─ 审查模板 *（仅启用项；默认按 primary_stack 过滤，允许切换查看其它栈）
   └─ 审查引擎
        └─ 审查引擎 *（当前仅 OPEN_CODE_REVIEW）
             说明：引擎使用自身审查能力，不使用平台审查模板
```

交互细则：

1. 切换 `primary_stack` 时，若当前模板的 `tech_stack` 不匹配，提示「建议选择同技术栈模板」，**不强制清空**（避免误伤）；保存不做硬匹配校验，只做启用状态校验。
2. 切换审查方式时清空对侧字段（与现网互斥逻辑一致）。
3. 模板下拉展示：`名称（技术栈 · v版本）`；无可用模板时引导「前往审查模板」。
4. 新增与编辑共用同一表单模型、校验与回显，不拆两套页面。

---

## 5. 两条执行分支

```text
                    ┌─ LLM_DIRECT ─┐
建单冻结快照 ───────┤              ├─→ 异步领取 → 执行 → 落结果
                    └─ OCR_ENGINE ─┘
```

### 5.1 大模型审查

1. 建单：写入方式=`LLM_DIRECT`、模板 id/code/version、**模板正文**、模型 id/名称/厂商/模型标识；
2. 执行：`PREPARE_WORKSPACE`（GitHub Diff）→ 用**任务快照中的正文**渲染占位符 → `INVOKE_MODEL`（`LlmCallService.chat(snapshotModelId, rendered)`）→ `PERSIST_RESULT`；
3. 不读取项目当前模板/模型；项目仅提供凭据与仓库坐标。

### 5.2 审查引擎

1. 建单：写入方式=`OCR_ENGINE`、`engine_code`；模板相关快照字段置空；
2. 执行：准备 Git 工作区 → `INVOKE_ENGINE`（现有 OCR CLI）；
3. **不读取、不注入平台审查模板**；不要求项目绑定模板；
4. OCR 若仍需模型环境变量，继续复用引擎侧已有「平台默认模型 / 引擎测试所选模型」机制，这属于**引擎运行依赖**，不是「项目审查模板」。文档与 UI 必须写清，避免再次混成上下级。

### 5.3 失败语义（保持现有分类）

- 模板停用或缺失：应在**建单时**已冻结，执行期不再因模板停用失败；若历史脏数据缺正文 → `CONFIG_MISSING`
- 模型停用/密钥失效：执行期 `CONFIG_MISSING` / `MODEL_CALL_FAILED`
- 引擎不可用：`ENGINE_FAILED` 等现有分类

---

## 6. 模板、项目、任务引用关系

```text
review_template (1) ──引用──► review_project.template_id
                                │ 仅 LLM_DIRECT
                                ▼
                          建单时拷贝 ──► review_task.snapshot_*
                                            │
                                            ▼ 每次 attempt
                                      review_task_run.snapshot_*

sys_ai_model_config (1) ──► review_project.model_id ──建单──► task/run 模型快照
engine_code 字典/配置   ──► review_project.engine_code ──建单──► task/run 引擎快照
```

约束：

- 项目引用的是模板 **ID**；任务保存的是 **版本号 + 正文**，因此模板后续编辑/停用不改写历史任务；
- 删除模板：若仍被项目引用则拒绝；历史任务只靠快照，不依赖模板行仍存在；
- 内置模板：禁止删除；禁止改 `template_code` / `builtin_flag`；正文是否允许在管理端直接改——**建议禁止直接改，只允许「复制后改自定义」**，从根上避免「改默认坑全站」。

---

## 7. 接口、权限、校验与快照策略

### 7.1 接口（`acr-admin` 接入 / `acr-review` 用例）

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/review/template/list` | `review:template:list` | 分页列表 |
| GET | `/review/template/{id}` | `review:template:query` | 详情 |
| POST | `/review/template` | `review:template:add` | 新增自定义 |
| PUT | `/review/template` | `review:template:edit` | 编辑自定义（正文变更 `version_no+1`） |
| DELETE | `/review/template/{ids}` | `review:template:remove` | 删自定义（校验引用） |

项目 options 接口扩展：返回 `templates`（启用列表，含 `techStack`/`versionNo`），不再返回旧 `prompts` 命名（可兼容一期双字段）。

### 7.2 校验

**模板**

- 名称/编码/正文/技术栈/状态必填；编码唯一；
- 内置：拒删、拒改编码、拒改 builtin 标识；编辑接口对内置直接拒绝并提示新增自定义模板；
- 自定义删除前 `count(project.template_id)`。

**项目**

- `primary_stack` 必填（新建/编辑统一）；
- `LLM_DIRECT`：`model_id` 启用存在；`template_id` 启用存在；
- `OCR_ENGINE`：`engine_code` 合法；清空 model/template。

**任务建单**

- 项目启用、PR 范围匹配等沿用 M2；
- 按方式校验并写入快照；大模型路径快照正文为空则建单失败（避免产生无法执行的 PENDING）。

### 7.3 快照策略（最终口径）

| 时机 | 行为 |
|---|---|
| 建单成功 | 冻结方式 + 模板版本/正文 或 引擎 + 模型信息到 `review_task` |
| 执行领取 | **只读任务快照**组装执行计划 |
| 重试 | 不刷新快照；新 attempt 复制同一任务快照 |
| 改项目/模板/模型 | 仅影响之后新建的任务 |

这与「后续配置修改只能影响新任务」严格一致；比「执行时现读项目」更符合企业追责预期。

---

## 8. 对现有 M3 链路需要调整的位置

| 位置 | 现状 | 调整 |
|---|---|---|
| 菜单 `126` 提示词管理 | 挂在模型服务 | 迁到代码审查「审查模板」，perms 更名 |
| `review_prompt` | 字段过窄 | 升级为 `review_template`（语言、内置、版本） |
| `ReviewProject` | `prompt_id` | → `template_id` + `primary_stack` |
| 项目表单审查执行区 | 提示词下拉 | 改为模板下拉 + 主要语言；引擎侧去掉模板 |
| `ReviewTaskCreateServiceImpl` | 只建 PENDING | **增加建单快照写入** |
| `ReviewTaskExecutionServiceImpl` | 执行时读项目配置 | 改为读任务快照；LLM 用快照正文；OCR **停止任何平台模板注入** |
| 任务详情 UI | 展示 prompt 快照 | 展示模板名称/编码/版本 + 正文；引擎任务不展示模板 |
| SQL | 14 已落 prompt | 新增 `15_review_template_*.sql` 做升级迁移（不改已执行的 14） |
| 文档 | M3 写「提示词管理」 | 同步改为审查模板；标明引擎路径不使用平台模板 |

**明确不改：**

- Webhook 验签/去重/匹配；
- OCR CLI 适配器主体、工作区准备、CAS 领取、run 历史模型；
- 模型服务与审查引擎现有页面（只改引用关系与文案引导）。

---

## 9. 可独立验收的开发范围

### 本次范围（一个纵向切片即可验收）

1. 审查模板表结构升级 + 6 个内置种子 + 字典 + 菜单权限；
2. 模板管理页（列表/详情/新增自定义/编辑自定义/复制/删除保护）；
3. 项目「审查执行」配置：主要语言、方式二选一、模板+模型 或 引擎；
4. 建单快照 + 执行读快照；
5. 任务详情展示模板版本/正文（LLM）与引擎信息（OCR）；
6. 自动测试：模板校验、内置保护、项目互斥校验、建单快照、执行不回读项目模板；
7. 文档与 SQL README 同步；**不**把真实 GitHub E2E 标为已完成。

### 非范围

- 规则引擎、模板编排、多模板组合、按文件自动选模板；
- 模板审批发布、灰度、A/B；
- 模板版本历史表 / diff 对比 UI；
- 引擎侧改写阿里 OCR 内部 prompt；
- PR 评论回写、通知、问题台账；
- 继续扩展 M3 的超时回收、多引擎降级等。

### 分步实现与验证（编码阶段按此执行）

| 步 | 内容 | 验证 |
|---|---|---|
| A | SQL：表升级、种子、字典、菜单；下线旧提示词菜单 | 脚本幂等执行；库内 6 条 builtin |
| B | 模板领域服务 + Controller + 单测 | 内置拒删拒改；复制产生自定义；引用保护 |
| C | 项目字段/校验/options/表单 | 新增编辑一致；方式互斥；无模板时中文提示 |
| D | 建单快照 | 改项目模板后，旧 PENDING 任务快照不变 |
| E | 执行读快照 | LLM 使用快照正文；OCR 不读模板；单元测试覆盖 |
| F | 任务详情 UI + 构建 | `mvn test`；`npm run build:prod` |

### 风险与待决策（已给推荐默认，实施前只需确认）

| 项 | 推荐默认 |
|---|---|
| 内置模板正文是否允许管理员直接改 | **否**，只允许复制后改 |
| 技术栈与模板是否强制一致 | **否**，仅筛选与弱提示 |
| 快照时机 | **建单冻结**（本方案强制） |
| 表是否 rename | **逻辑名 `review_template`**；物理表可 rename 或保留 `review_prompt` 一期内兼容，推荐一次性 rename + 字段升级，减少双名债务 |
| 旧 `prompt_id` 数据 | 迁移到 `template_id`；`default_pr_review` → `builtin_fullstack` |

---

## 10. 架构与交付约束对照

- 业务在 `acr-review`，REST 在 `acr-admin`，模型配置仍在 `acr-system`；
- 不新增 Maven 模块、不引入 MQ/工作流/规则引擎；
- 公共渲染/快照组装放在 `acr-review` service 层（扩展现有 `ReviewPromptRenderer` → 可改名为 `ReviewTemplateRenderer`）；
- 字典承载技术栈与方式选项；稳定编码用常量；
- 已执行的 `14_*.sql` 不改写，增量用下一序号脚本。

---

## 11. 验收标准（切片完成时）

- [x] 代码审查下可见「审查模板」，内置 6 套可查看/复制，不可删除破坏；
- [x] 项目可配置主要语言 + 审查方式，大模型必选模型与模板，引擎必选引擎且不选模板；
- [x] 新建任务带齐快照；修改模板/项目后仅新任务变化；
- [x] LLM 执行使用任务快照模板正文；OCR 执行不依赖平台模板；
- [x] `mvn test` 与前端生产构建通过；
- [x] 文档未将未做的 E2E/回写标为完成；
- [ ] 真实 GitHub 双方式端到端（需环境）。

---

**结论：** 先把「审查模板」从模型服务旁路纠正为代码审查下的公共配置资产，并用建单快照切断配置漂移；M3 双执行分支保持互斥，只调整配置读取时机与对象命名，不重做引擎与模型底座。确认本方案后，再按第 9 节 A→F 编码。
