# M9 功能助手（产品内指引）— 设计文档

- 阶段：核心版（V0.2）体验切片
- 日期：2026-08-05
- 状态：待评审
- 前置：`docs/planning/product-roadmap.md`、M1–M7 与多平台扩展已收口

## 0. 目标与成功指标

### 业务目标

平台配置点分散在 7 个页面（项目接入、凭据、Webhook、模型、引擎、模板、投递渠道），各平台参数含义与获取路径依赖隐性知识；提交注释与审查效果的关联不可见。本切片提供全局可达、不打断操作的产品内指引，让用户在卡壳点就地获得答案。

### 成功指标

1. 新管理员不打开 README，仅依靠平台内指引独立完成「建凭据 → 接项目 → 配 Webhook → 触发首次审查」；
2. 功能助手入口全局可见，打开抽屉不离开当前页面、不丢失表单状态；
3. 关键配置页（项目接入、凭据、模型配置）有文字链直达对应指引文档；
4. 文档内容自包含，内网环境（无外网）可完整执行所有指引；
5. 零后端改动、零 SQL、零新权限、零新前端依赖。

### 明确不含

- 全文搜索引擎（内容 ≤14 篇，本地标题/关键词过滤足够）；
- 上下文自动感知（仅显式锚点深链）；
- 浏览统计、反馈埋点、文档版本化；
- 后端可维护文档（建表 + CRUD + 权限，现阶段过度设计，确有运营诉求再立切片）；
- 多语言（系统无 i18n，v1 仅中文）。

## 1. 现状基线与资产盘点

| 资产 | 现状 | 本切片用法 |
|---|---|---|
| `marked@15.0.12` + `dompurify@3.4.13` | 已在依赖，`MessageContentView.vue` 有完整渲染管线 | 抽公共 `utils/markdown.js`，抽屉复用；不改 MessageContentView 行为 |
| Navbar `right-menu-item` 插槽 | header-search / header-notice 同款模式 | 入口图标注入，不动其他入口 |
| 暗黑模式 | `html.dark` + CSS 变量双套 | 抽屉与 markdown 样式全部走变量 |
| 提交注释机制 | 每 PR 最多 30 条 commit、只取首行、单行 500 字符截断进审查上下文（`GitHubPullRequestCommitMessagesFormatter` 等四平台同款） | 注释规范文档的机制依据 |
| 审查范围机制 | 默认只审 Diff 变更行、存量问题不打标上报（M3.2） | 「小步提交审查更准」的机制依据 |
| docs/images | 仅有 README 宣传图 | 指引截图机制预留，v1 内容先文字版 |

配置点清单（指引内容的事实源）：项目接入（平台/仓库/凭据/目标分支/审查方式/模板/模型）、凭据管理（四平台 token）、Webhook（URL/Secret/事件订阅）、模型配置（provider/apiKey/模型名/检测）、审查引擎、审查模板、投递渠道（钉钉/企微/飞书）。

## 2. 产品形态与交互

### 2.1 入口与容器

- Navbar 右侧新增问号图标入口（tooltip「功能助手」），位于消息通知左侧，`right-menu-item hover-effect` 同款样式；
- `el-drawer` 右侧滑出，宽 560px，视口 <768px 时全屏；挂在 layout 层，不受 keep-alive 影响；
- 抽屉打开/关闭仅操作全局 store，不产生路由变化，页面表单状态不丢失。

### 2.2 抽屉内布局（单列三段）

```text
┌────────────────────────────┐
│ 功能助手              [×]  │
│ ┌────────────────────────┐ │
│ │ 搜索：输入标题/关键词   │ │  ← 本地过滤，即时出结果列表
│ └────────────────────────┘ │
│ 目录（6 域分组，手风琴）     │
│  ▸ 快速上手                │
│  ▸ 平台接入指引            │
│  ▸ 模型与引擎              │
│  ▸ 提交注释规范            │
│  ▸ 投递与通知              │
│  ▸ 常见问题                │
│ ────────────────────────── │
│ 内容区（markdown 渲染）     │
│  ……锚点滚动……              │
└────────────────────────────┘
```

- 默认选中「快速上手 → 接入第一个仓库」；
- 搜索有输入时目录区切换为结果列表，点击直达文档；
- 内容区代码块带「复制」按钮（注释模板、Webhook URL 为高频复制对象）。

### 2.3 深链

- `guideStore.open(docId)`：打开抽屉并选中文档；
- 文字链侵入点（仅加链接，不改页面逻辑）：
  - 项目接入表单 → 按已选平台动态链 `platform-{github|gitlab|gitee|gitea}`，未选平台打开目录；
  - 凭据管理 → 同上；
  - 模型配置 → `model-config`；
- 文案统一「查看接入指引 →」/「查看配置指引 →」。

## 3. 信息架构与内容清单（v1 全 6 域 14 篇）

| 域 | docId | 篇目 | 核心内容 |
|---|---|---|---|
| 快速上手 | `quick-start-first-repo` | 接入第一个仓库 | 5 步串联：建凭据→接项目→配 Webhook→触发→看结果，含每步跳转入口 |
| 平台接入 | `platform-github` | GitHub 接入 | Fine-grained/Classic token 创建路径与最小权限、Webhook URL/Secret/事件订阅（pull_request: opened/synchronize/reopened）、连通验证 |
| 平台接入 | `platform-gitlab` | GitLab 接入 | PAT/Project Access Token 与 scope、Webhook 配置与 Secret Token、Merge Request 事件 |
| 平台接入 | `platform-gitee` | Gitee 接入 | 私人令牌与权限、WebHook 配置、密码/签名二选一说明 |
| 平台接入 | `platform-gitea` | Gitea 接入 | 令牌创建、Webhook 配置、事件选择 |
| 模型与引擎 | `model-config` | 模型服务配置 | 各 provider 参数词典（apiKey 获取路径）、默认模型含义、检测按钮作用 |
| 模型与引擎 | `engine-config` | 审查引擎配置 | 引擎可执行文件、超时并发、适用场景（与模型方式怎么选） |
| 模型与引擎 | `model-check-failed` | 模型检测失败排查 | 网络/密钥/模型名/额度四类失败决策树 |
| 提交注释 | `commit-convention` | 注释规范说明 | 机制依据：只取首行、30 条上限、500 字符截断；Conventional Commits 前缀；小步 PR / squash 建议 |
| 提交注释 | `commit-examples` | 正误实例对照 | 6+ 组「不推荐/推荐」diff 对照 + 可复制模板 |
| 投递与通知 | `delivery-channel` | 通知渠道配置 | 钉钉/企微/飞书机器人创建与 webhook 地址、项目绑定与失败通知开关 |
| 投递与通知 | `delivery-failed` | 投递失败排查 | 机器人关键词/加签、网络出口、失败重试入口 |
| 常见问题 | `faq-webhook-not-triggered` | Webhook 没触发 | 事件落库排查路径（含忽略原因）、分支不匹配、签名错误、内网穿透说明 |
| 常见问题 | `faq-review-not-run` | 审查没执行/结果不符预期 | 任务状态决策树、模型/引擎检测、模板快照说明 |

## 4. 内容规范（企业内部系统约束）

1. **内网自包含**：操作步骤完整描述（页面、按钮、字段、勾选项），外部官方文档链接仅作补充参考，不得作为必要步骤；
2. **示例脱敏**：token/Secret/URL 一律明显占位符（`ghp_xxxx…`、`https://acr.example.com`），禁止出现形似真实凭据的字符串；
3. **图片机制预留**：截图集中放 `src/guide/assets/`，markdown 中以文件名引用，渲染层经 vite 解析重写 src；v1 可先纯文字，补图不改机制；图片在明暗主题下均需 1px 边框可辨；
4. **防腐烂约定**：功能变更 PR 必须同步更新对应指引文档，CHANGELOG 联动检查；
5. **口吻**：第二人称、步骤化、可执行；不写营销话术，不堆概念。

## 5. 技术设计

### 5.1 文件结构（全部新增于 acr-ui）

```text
src/guide/
├── manifest.js               # 目录元数据：docId/title/group/keywords/文件路径
├── content/                  # markdown 原文（按域分目录）
│   ├── quick-start/first-repo.md
│   ├── platforms/{github,gitlab,gitee,gitea}.md
│   ├── model-engine/{model-config,engine-config,check-failed}.md
│   ├── commit-message/{convention,examples}.md
│   ├── delivery/{channel-config,delivery-failed}.md
│   └── faq/{webhook-not-triggered,review-not-run}.md
├── assets/                   # 截图预留目录（v1 可为空，含 .gitkeep）
└── components/
    ├── GuideDrawer.vue       # 抽屉容器：搜索 + 目录 + 内容编排
    ├── GuideContent.vue      # markdown 渲染 + 代码块复制 + 图片 src 重写
    └── GuideEntry.vue        # navbar 图标入口
src/store/modules/guide.js    # visible/activeDocId/open(docId)/close()
src/utils/markdown.js         # marked + DOMPurify 公共封装（自 MessageContentView 收敛）
```

- markdown 加载：Vite 6 `import.meta.glob('./content/**/*.md', { query: '?raw', import: 'default' })`，按 docId 懒加载；
- 图片解析：`import.meta.glob('./assets/*.{png,jpg,svg,webp}', { eager: true, import: 'default' })` 建 URL 映射，渲染时重写 `img src`；
- 搜索：manifest 的 title + keywords 本地包含匹配（中文子串），无索引库；
- 单文件 ≤800 行，超出拆子组件。

### 5.2 样式约束（rules/UI_THEME_RULES.md）

- 抽屉与内容全部 CSS 变量，明暗双主题自适配；
- `.guide-md` 排版体系：标题层级对比、代码块 `--neutral-card` 底 + 1px 边框、表格斑马纹、引用块左侧 3px 语义色条；
- 无渐变、无发光、无阴影（除 drawer 自带遮罩）；
- 状态语义色仅用于「注意/警告」提示块，配图标不只靠颜色。

### 5.3 不改动的既有行为

MessageContentView 渲染结果、navbar 其他入口、布局设置、路由与菜单表、任何业务接口与权限。

## 6. 实施步骤

1. `utils/markdown.js` 收敛 + GuideDrawer/GuideContent/GuideEntry 骨架 + store；
2. manifest + 内容加载管线（md 懒加载 + 图片映射）+ 搜索过滤；
3. navbar 注入入口 + 三处文字链深链；
4. 14 篇内容撰写（本切片主要工作量）；
5. 明暗主题与响应式打磨；
6. 全量验证。

## 7. 验证

1. `cd acr-ui && npm run build:prod` 通过；后端零改动无需 mvn test；
2. 本地真实环境页面验证（Playwright）：
   - 分辨率 1440/1280/1024/800/移动端宽度（抽屉全屏降级）；
   - 浅色 + 暗色（重点：代码块边框、表格、引用块可辨）；
   - 抽屉开合不重置项目表单已填内容；
   - 搜索中文关键词命中、无结果空态；
   - 三处文字链深链直达对应文档；
   - 长文档锚点滚动、代码块复制；
   - keep-alive 切 tab 后抽屉状态符合预期（关闭态不残留）；
3. 内容验收：按成功指标 1 走查「接入第一个仓库」全链路；
4. 脱敏检查：全内容 grep 无形似真实凭据字符串。

## 8. 后续候选（本切片不做）

- 上下文自动感知（按当前路由预选文档）；
- 内容浏览统计与高频缺口分析；
- 截图补全与 GIF 操作演示；
- 后端可维护文档（确需运营化时另立切片）。
