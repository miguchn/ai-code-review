# M9 功能助手（产品内指引）Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 acr-ui 新增全局抽屉式「功能助手」，承载 14 篇产品内指引文档（平台接入/模型引擎/提交注释/投递/FAQ），零后端改动。

**Architecture:** 静态 markdown 随前端打包（Vite `import.meta.glob` 按 docId 懒加载），`marked + DOMPurify` 渲染（依赖已存在），Pinia 全局 store 控制抽屉开合与深链定位，navbar 图标入口 + 三个配置页文字链深链。

**Tech Stack:** Vue 3 `<script setup>`、Element Plus 2.13、Vite 6.4、marked 15 + dompurify 3.4（既有依赖）、Pinia（options API，同 store/modules/lock.js）。

**Spec:** `docs/planning/feature-assistant-m9.md`（内容清单与内容规范以 spec §3/§4 为准）

## Global Constraints

- 零新 npm 依赖、零后端改动、零 SQL、零新权限串；不改任何既有页面业务逻辑
- `MessageContentView.vue` 渲染行为不变（仅内部改调公共封装）
- 样式全部 CSS 变量，明暗双主题自适应；遵守 `rules/UI_THEME_RULES.md`：1px 边框、无阴影、无渐变发光、状态色不只靠颜色
- 单文件 ≤800 行；组件 PascalCase、composable/工具 camelCase
- Commit 格式：`<type>: <description>`（feat/fix/docs/refactor/test/chore/perf/ci）
- 内容规范（spec §4）：内网自包含、示例全占位符脱敏（`ghp_xxxx…`、`https://acr.example.com`）、第二人称步骤化
- 项目无前端测试框架且禁止新增依赖 → 每个任务的验证 = `npm run build:prod` + 指定的运行时检查（node 脚本 / Playwright）
- 本地验证环境（memory 资产）：Docker mysql8/redis 运行中；后端 `java -jar acr-admin/target/acr-admin.jar --spring.profiles.active=dev`（需 `export ACR_CREDENTIAL_MASTER_KEY="$(cat ~/.acr/master-key)"`）；前端 `cd acr-ui && npm run dev`（:80）；Playwright 脚本须放 acr-ui 目录内运行（node_modules 解析），token 经登录接口获取（captcha 从 redis 读）

---

### Task 1: 收敛公共 markdown 渲染封装 `utils/markdown.js`

**Files:**
- Create: `acr-ui/src/utils/markdown.js`
- Modify: `acr-ui/src/views/notify/components/MessageContentView.vue`（仅 script setup 部分，模板与样式不动）

**Interfaces:**
- Produces: `renderMarkdown(raw: string): string` —— GFM + breaks 渲染 + DOMPurify 消毒 + 链接新窗口 hook（模块级注册一次）。后续 Task 3 的 GuideContent 消费此函数。

- [ ] **Step 1: 写封装**

`acr-ui/src/utils/markdown.js`：

```js
import { marked } from 'marked'
import DOMPurify from 'dompurify'

marked.setOptions({ gfm: true, breaks: true })

// 链接统一新标签打开，并阻断 opener 引用（模块级注册一次，多处消费共享）
DOMPurify.addHook('afterSanitizeAttributes', (node) => {
  if (node.tagName === 'A') {
    node.setAttribute('target', '_blank')
    node.setAttribute('rel', 'noopener noreferrer')
  }
})

/**
 * 渲染简化 Markdown 为消毒后的 HTML 字符串。
 * @param {string} raw markdown 原文
 * @returns {string} 可安全 v-html 的 HTML；空输入返回 ''
 */
export function renderMarkdown(raw) {
  const text = raw == null ? '' : String(raw)
  if (!text.trim()) return ''
  const html = marked.parse(text, { async: false })
  return DOMPurify.sanitize(typeof html === 'string' ? html : String(html))
}
```

- [ ] **Step 2: MessageContentView 改调封装**

`MessageContentView.vue` script setup 中：删除 `import { marked }`、`import DOMPurify`、`marked.setOptions(...)`、`DOMPurify.addHook(...)` 四段；改为 `import { renderMarkdown } from '@/utils/markdown'`；`safeHtml` computed 体改为 `return renderMarkdown(props.body)`（保留原空值 trim 判断语义——`renderMarkdown` 内部已处理，直接返回即可）。模板、样式、props 全部不动。

- [ ] **Step 3: 构建验证**

Run: `cd acr-ui && npm run build:prod`
Expected: 构建成功。

- [ ] **Step 4: 运行时回归（投递消息渲染不变）**

本地环境起后端+vite，Playwright 登录后访问「通知管理 → 投递记录」，展开一条含 markdown 正文的投递详情，断言 `.markdown-body` 存在且含 `<a target="_blank">`；与重构前截图目视对比无差异。

- [ ] **Step 5: Commit**

```bash
git add acr-ui/src/utils/markdown.js acr-ui/src/views/notify/components/MessageContentView.vue
git commit -m "refactor: 收敛公共 markdown 渲染封装 utils/markdown.js"
```

---

### Task 2: 指引 store + manifest + 内容加载管线 + 首篇文档

**Files:**
- Create: `acr-ui/src/store/modules/guide.js`
- Create: `acr-ui/src/guide/manifest.js`
- Create: `acr-ui/src/guide/loader.js`
- Create: `acr-ui/src/guide/content/quick-start/first-repo.md`
- Create: `acr-ui/src/guide/assets/.gitkeep`

**Interfaces:**
- Produces:
  - `useGuideStore()`：`{ visible: boolean, activeDocId: string|null }` + `open(docId?: string|null)` / `close()` / `select(docId: string)`
  - `GUIDE_GROUPS: [{key,title,docIds:string[]}]`、`GUIDE_DOCS: [{id,title,group,keywords:string[],file}]`、`DEFAULT_DOC_ID`、`findDoc(id): entry|null`
  - `loadDocContent(file: string): Promise<string|null>`、`getAssetUrl(fileName: string): string`
- Consumes: 无（Task 3 全部消费这些接口）

- [ ] **Step 1: store（options API，同 lock.js 模式）**

`acr-ui/src/store/modules/guide.js`：

```js
export const useGuideStore = defineStore('guide', {
  state: () => ({
    visible: false,
    activeDocId: null
  }),
  actions: {
    /** 打开抽屉；docId 为空时保持当前选中或回退默认篇 */
    open(docId = null) {
      this.activeDocId = docId || this.activeDocId || 'quick-start-first-repo'
      this.visible = true
    },
    close() {
      this.visible = false
    },
    select(docId) {
      this.activeDocId = docId
    }
  }
})

export default useGuideStore
```

（项目 store 经 Pinia 全局注册，`defineStore`/`computed` 等为 unplugin-auto-import 自动注入，与 lock.js 一致无需 import。）

- [ ] **Step 2: manifest（14 篇元数据）**

`acr-ui/src/guide/manifest.js`：

```js
export const DEFAULT_DOC_ID = 'quick-start-first-repo'

export const GUIDE_GROUPS = [
  { key: 'quick-start', title: '快速上手', docIds: ['quick-start-first-repo'] },
  { key: 'platforms', title: '平台接入指引', docIds: ['platform-github', 'platform-gitlab', 'platform-gitee', 'platform-gitea'] },
  { key: 'model-engine', title: '模型与引擎', docIds: ['model-config', 'engine-config', 'model-check-failed'] },
  { key: 'commit-message', title: '提交注释规范', docIds: ['commit-convention', 'commit-examples'] },
  { key: 'delivery', title: '投递与通知', docIds: ['delivery-channel', 'delivery-failed'] },
  { key: 'faq', title: '常见问题', docIds: ['faq-webhook-not-triggered', 'faq-review-not-run'] }
]

export const GUIDE_DOCS = [
  { id: 'quick-start-first-repo', title: '接入第一个仓库', group: 'quick-start', keywords: ['开始', '上手', '流程', '第一个', '接入'], file: 'quick-start/first-repo.md' },
  { id: 'platform-github', title: 'GitHub 接入', group: 'platforms', keywords: ['github', 'token', 'webhook', '凭据', 'secret'], file: 'platforms/github.md' },
  { id: 'platform-gitlab', title: 'GitLab 接入', group: 'platforms', keywords: ['gitlab', 'token', 'webhook', '凭据', 'merge request'], file: 'platforms/gitlab.md' },
  { id: 'platform-gitee', title: 'Gitee 接入', group: 'platforms', keywords: ['gitee', '码云', 'token', 'webhook', '凭据'], file: 'platforms/gitee.md' },
  { id: 'platform-gitea', title: 'Gitea 接入', group: 'platforms', keywords: ['gitea', 'token', 'webhook', '凭据', '自建'], file: 'platforms/gitea.md' },
  { id: 'model-config', title: '模型服务配置', group: 'model-engine', keywords: ['模型', 'apikey', 'provider', '厂商', '默认模型', '检测'], file: 'model-engine/model-config.md' },
  { id: 'engine-config', title: '审查引擎配置', group: 'model-engine', keywords: ['引擎', 'engine', 'ocr', '超时', '并发'], file: 'model-engine/engine-config.md' },
  { id: 'model-check-failed', title: '模型检测失败排查', group: 'model-engine', keywords: ['检测失败', '排查', '密钥', '额度', '网络'], file: 'model-engine/check-failed.md' },
  { id: 'commit-convention', title: '提交注释规范说明', group: 'commit-message', keywords: ['提交', '注释', 'commit', '规范', '首行'], file: 'commit-message/convention.md' },
  { id: 'commit-examples', title: '注释正误实例对照', group: 'commit-message', keywords: ['实例', '示例', '模板', '对照', 'feat', 'fix'], file: 'commit-message/examples.md' },
  { id: 'delivery-channel', title: '通知渠道配置', group: 'delivery', keywords: ['钉钉', '企微', '飞书', '机器人', '通知', '渠道'], file: 'delivery/channel-config.md' },
  { id: 'delivery-failed', title: '投递失败排查', group: 'delivery', keywords: ['投递失败', '加签', '关键词', '重试', '补发'], file: 'delivery/delivery-failed.md' },
  { id: 'faq-webhook-not-triggered', title: 'Webhook 没触发怎么办', group: 'faq', keywords: ['webhook', '没触发', '事件', '签名', '分支'], file: 'faq/webhook-not-triggered.md' },
  { id: 'faq-review-not-run', title: '审查没执行或结果不符预期', group: 'faq', keywords: ['没执行', '失败', '结果', '预期', '模板'], file: 'faq/review-not-run.md' }
]

export function findDoc(id) {
  return GUIDE_DOCS.find(d => d.id === id) || null
}
```

- [ ] **Step 3: loader（md 懒加载 + 图片 URL 映射）**

`acr-ui/src/guide/loader.js`：

```js
// markdown 原文：按 docId 对应文件懒加载（glob 只注册路径，文件缺失不影响构建）
const mdModules = import.meta.glob('./content/**/*.md', { query: '?raw', import: 'default' })
// 指引截图：构建期解析为带 hash 的 URL，markdown 中以文件名引用
const assetModules = import.meta.glob('./assets/*.{png,jpg,jpeg,svg,webp}', { eager: true, import: 'default' })

const assetUrls = {}
for (const [path, url] of Object.entries(assetModules)) {
  assetUrls[path.split('/').pop()] = url
}

/** 按文件名取图片 URL；不存在返回空串 */
export function getAssetUrl(fileName) {
  return assetUrls[fileName] || ''
}

/**
 * 加载 markdown 原文。
 * @param {string} file manifest 中的相对路径，如 'platforms/github.md'
 * @returns {Promise<string|null>} 原文；缺失或加载失败返回 null
 */
export async function loadDocContent(file) {
  const loader = mdModules[`./content/${file}`]
  if (!loader) return null
  try {
    return await loader()
  } catch {
    return null
  }
}
```

- [ ] **Step 4: 首篇真实内容 `quick-start/first-repo.md`**

```markdown
# 接入第一个仓库

按以下 5 步完成从「接入仓库」到「看到第一条审查结论」的完整链路。每步都可点击页面内的「查看接入指引 →」回到本文档对应章节。

## 第 1 步：创建 Git 平台凭据

进入「代码审查 → 凭据管理」，点击「新增」：

1. 选择 Git 平台（GitHub / GitLab / Gitee / Gitea）；
2. 按平台要求填入访问令牌（Personal Access Token）。令牌的创建路径与最小权限见「平台接入指引」对应平台篇；
3. 保存。令牌仅用于拉取代码与 Diff，平台加密存储。

## 第 2 步：接入代码项目

进入「代码审查 → 代码项目」，点击「新增」：

1. 选择业务系统与 Git 平台，填写仓库地址（如 `https://github.com/your-org/your-repo`）；
2. 选择上一步创建的凭据；
3. 填写目标分支（如 `main`、`develop`），只有合入这些分支的 PR/MR 会触发审查；
4. 选择审查方式：「大模型审查」（需先在模型服务配置模型）或「审查引擎」；
5. 大模型方式需再选审查模板与模型，保存。

## 第 3 步：配置 Webhook

项目保存后，在项目详情/列表获取该项目的 Webhook 地址与 Secret，到 Git 平台仓库设置中添加 Webhook：

- 地址：平台展示的项目专属 Webhook URL；
- Secret：与平台展示值保持一致；
- 订阅事件：PR（Pull Request）的打开、更新、重新打开。

各平台具体配置入口见「平台接入指引」。

## 第 4 步：发起一个 PR 触发审查

在仓库中新建分支提交改动，向目标分支发起 PR。平台收到 Webhook 事件后会生成审查任务并异步执行。

> 提交注释的首行会进入审查上下文（每个 PR 最多取 30 条、每条只取首行）。规范写法见「提交注释规范」。

## 第 5 步：查看审查结果

- 「审查任务」查看执行中的任务；
- 「审查记录」查看已结束的结论：通过 / 建议修改 / 高风险 / 执行失败；
- 配置了通知渠道时，结论摘要会自动投递到钉钉/企微/飞书群。

## 卡住的时候

- Webhook 没触发 → 见「常见问题 → Webhook 没触发怎么办」
- 模型检测失败 → 见「模型与引擎 → 模型检测失败排查」
- 审查没执行或结果不符预期 → 见「常见问题 → 审查没执行或结果不符预期」
```

- [ ] **Step 5: manifest 完整性自检脚本（node 一次性运行，不落库）**

Run: `cd acr-ui && node -e "const m=require('./src/guide/manifest.js');const ids=new Set(m.GUIDE_DOCS.map(d=>d.id));const g=m.GUIDE_GROUPS.flatMap(x=>x.docIds);if(g.length!==14||!g.every(i=>ids.has(i)))throw new Error('manifest mismatch');console.log('manifest OK: 14 docs')"` —— 若 ESM 报错则改用 `node --input-type=module -e "import('...').then(...)"` 形式；亦可直接由构建步骤覆盖（manifest 被 import 即校验引用）。两者任一通过即可。

- [ ] **Step 6: 构建验证**

Run: `cd acr-ui && npm run build:prod`
Expected: 成功（content 仅 1 篇不报错——glob 懒加载只登记存在的文件）。

- [ ] **Step 7: Commit**

```bash
git add acr-ui/src/store/modules/guide.js acr-ui/src/guide/
git commit -m "feat: 功能助手 store、目录元数据与 markdown 加载管线"
```

---

### Task 3: 抽屉三组件 + navbar/layout 挂载（walking skeleton）

**Files:**
- Create: `acr-ui/src/guide/components/GuideContent.vue`
- Create: `acr-ui/src/guide/components/GuideDrawer.vue`
- Create: `acr-ui/src/guide/components/GuideEntry.vue`
- Modify: `acr-ui/src/layout/components/Navbar.vue`（注入入口，第 31 行「消息通知」tooltip 之前）
- Modify: `acr-ui/src/layout/index.vue`（模板根部 `</div>` 前挂 `<guide-drawer />`）

**Interfaces:**
- Consumes: Task 1 `renderMarkdown`；Task 2 `useGuideStore`/`GUIDE_GROUPS`/`GUIDE_DOCS`/`DEFAULT_DOC_ID`/`findDoc`/`loadDocContent`/`getAssetUrl`
- Produces: `<guide-entry />`（无 props）、`<guide-drawer />`（无 props）、`<GuideContent :doc="manifestEntry" />`

- [ ] **Step 1: GuideContent.vue（渲染 + 代码块复制 + 图片重写）**

```vue
<template>
  <div class="guide-content">
    <div v-if="loading" class="guide-content__state">加载中…</div>
    <div v-else-if="missing" class="guide-content__state">该文档暂未提供</div>
    <div v-else ref="bodyRef" class="guide-content__body guide-md" v-html="html" @click="onBodyClick" />
  </div>
</template>

<script setup>
import { renderMarkdown } from '@/utils/markdown'
import { loadDocContent, getAssetUrl } from '../loader'

const props = defineProps({
  /** manifest 文档条目 */
  doc: { type: Object, required: true }
})

const html = ref('')
const loading = ref(false)
const missing = ref(false)
const bodyRef = ref(null)

watch(() => props.doc?.id, load, { immediate: true })

async function load() {
  if (!props.doc) return
  loading.value = true
  missing.value = false
  const raw = await loadDocContent(props.doc.file)
  loading.value = false
  if (raw == null) {
    html.value = ''
    missing.value = true
    return
  }
  html.value = renderMarkdown(raw)
  await nextTick()
  enhanceBody()
}

/** 代码块外包一层并追加复制按钮；markdown 图片文件名重写为构建期 URL */
function enhanceBody() {
  const root = bodyRef.value
  if (!root) return
  root.querySelectorAll('pre').forEach(pre => {
    if (pre.parentElement?.classList.contains('code-block')) return
    const wrap = document.createElement('div')
    wrap.className = 'code-block'
    pre.parentNode.insertBefore(wrap, pre)
    wrap.appendChild(pre)
    const btn = document.createElement('button')
    btn.type = 'button'
    btn.className = 'code-block__copy'
    btn.textContent = '复制'
    wrap.appendChild(btn)
  })
  root.querySelectorAll('img').forEach(img => {
    const name = (img.getAttribute('src') || '').split('/').pop()
    const url = name ? getAssetUrl(name) : ''
    if (url) {
      img.src = url
      img.classList.add('guide-md__img')
    }
  })
}

function onBodyClick(e) {
  const btn = e.target.closest('.code-block__copy')
  if (!btn) return
  const pre = btn.parentElement?.querySelector('pre')
  copyText(pre?.innerText || '', btn)
}

/** 复制（含 http 内网非安全上下文降级），成功后按钮短暂反馈 */
function copyText(text, btn) {
  const done = () => {
    btn.textContent = '已复制'
    setTimeout(() => { btn.textContent = '复制' }, 1200)
  }
  if (navigator.clipboard?.writeText) {
    navigator.clipboard.writeText(text).then(done).catch(() => legacyCopy(text, done))
  } else {
    legacyCopy(text, done)
  }
}

function legacyCopy(text, done) {
  const ta = document.createElement('textarea')
  ta.value = text
  ta.style.position = 'fixed'
  ta.style.opacity = '0'
  document.body.appendChild(ta)
  ta.select()
  try { document.execCommand('copy'); done() } catch { /* 忽略：无剪切板权限 */ }
  document.body.removeChild(ta)
}
</script>
```

样式（scoped，全部 CSS 变量；`.guide-md` 排版在 MessageContentView `.markdown-body` 基础上扩展表格/图片/提示块）：

```scss
.guide-content__state {
  padding: 40px 0;
  color: var(--text-secondary);
  font-size: 13px;
  text-align: center;
}

/* 标题/段落/列表/行内代码/引用块样式照搬 MessageContentView.vue 的 .markdown-body 规则，
   选择器改为 .guide-content__body :deep(...)，再追加： */
.guide-content__body :deep(table) {
  width: 100%;
  margin: 0.75em 0;
  border-collapse: collapse;
  font-size: 0.95em;
}
.guide-content__body :deep(th),
.guide-content__body :deep(td) {
  padding: 6px 10px;
  border: 1px solid var(--border-light);
  text-align: left;
}
.guide-content__body :deep(thead th) {
  background: var(--neutral-card);
  color: var(--text-primary);
  font-weight: 600;
}
.guide-content__body :deep(.code-block) {
  position: relative;
}
.guide-content__body :deep(.code-block__copy) {
  position: absolute;
  top: 6px;
  right: 6px;
  padding: 2px 8px;
  border: 1px solid var(--border-light);
  border-radius: 4px;
  background: var(--neutral-card);
  color: var(--text-secondary);
  font-size: 12px;
  cursor: pointer;
}
.guide-content__body :deep(.code-block__copy:hover) {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary);
}
.guide-md__img {
  max-width: 100%;
  border: 1px solid var(--border-light);
  border-radius: 8px;
}
```

注意：上述 `:deep(.code-block)` 类是 JS 运行时注入的类，写在 scoped 里需经 `:deep()` 命中，已按要求处理。

- [ ] **Step 2: GuideDrawer.vue（搜索 + 目录 + 内容编排）**

```vue
<template>
  <el-drawer
    v-model="drawerVisible"
    :size="drawerSize"
    :with-header="false"
    append-to-body
    custom-class="guide-drawer"
    @closed="onClosed"
  >
    <div class="guide-drawer__inner">
      <header class="guide-drawer__header">
        <span class="guide-drawer__title">功能助手</span>
        <el-icon class="guide-drawer__close" @click="guideStore.close()"><Close /></el-icon>
      </header>

      <el-input
        v-model="keyword"
        class="guide-drawer__search"
        placeholder="搜索指引：如 webhook、token、注释"
        clearable
        :prefix-icon="Search"
      />

      <div v-if="keyword" class="guide-drawer__results">
        <div v-if="!searchResults.length" class="guide-drawer__empty">没有匹配的指引，换个关键词试试</div>
        <div
          v-for="doc in searchResults"
          :key="doc.id"
          class="guide-nav__item"
          :class="{ 'is-active': doc.id === guideStore.activeDocId }"
          @click="selectDoc(doc.id)"
        >{{ doc.title }}<span class="guide-nav__group">{{ groupTitle(doc.group) }}</span></div>
      </div>

      <el-collapse v-else v-model="openGroups" class="guide-drawer__nav">
        <el-collapse-item v-for="group in GUIDE_GROUPS" :key="group.key" :name="group.key" :title="group.title">
          <div
            v-for="id in group.docIds"
            :key="id"
            class="guide-nav__item"
            :class="{ 'is-active': id === guideStore.activeDocId }"
            @click="selectDoc(id)"
          >{{ findDoc(id)?.title }}</div>
        </el-collapse-item>
      </el-collapse>

      <div ref="contentScrollRef" class="guide-drawer__content">
        <GuideContent v-if="activeDoc" :key="activeDoc.id" :doc="activeDoc" />
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { GUIDE_GROUPS, GUIDE_DOCS, DEFAULT_DOC_ID, findDoc } from '../manifest'
import useGuideStore from '@/store/modules/guide'
import GuideContent from './GuideContent.vue'
import useAppStore from '@/store/modules/app'

const guideStore = useGuideStore()
const appStore = useAppStore()

const keyword = ref('')
const openGroups = ref(GUIDE_GROUPS.map(g => g.key))
const contentScrollRef = ref(null)

const drawerSize = computed(() => (appStore.device === 'mobile' ? '100%' : '560px'))

const drawerVisible = computed({
  get: () => guideStore.visible,
  set: (v) => (v ? guideStore.open() : guideStore.close())
})

const activeDoc = computed(() => findDoc(guideStore.activeDocId) || findDoc(DEFAULT_DOC_ID))

const searchResults = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return []
  return GUIDE_DOCS.filter(d =>
    d.title.toLowerCase().includes(kw) || d.keywords.some(k => k.toLowerCase().includes(kw))
  )
})

function groupTitle(groupKey) {
  return GUIDE_GROUPS.find(g => g.key === groupKey)?.title || ''
}

function selectDoc(id) {
  guideStore.select(id)
  keyword.value = ''
  nextTick(() => { contentScrollRef.value && (contentScrollRef.value.scrollTop = 0) })
}

function onClosed() {
  keyword.value = ''
}
</script>
```

样式（scoped）：`.guide-drawer__inner` flex 列高 100%；header 16px/600 + 关闭图标；nav 区 `flex-shrink:0; max-height: 40%; overflow:auto`；content 区 `flex:1; overflow-y:auto; padding: 4px 20px 24px`；`.guide-nav__item` 行高 32px、圆角 6px、hover `var(--bg-hover)`、`.is-active` 用 `var(--brand-bg-soft)` 底 + `var(--brand-text)` 字；`.guide-nav__group` 右侧 12px `var(--text-assist)`。el-collapse 去默认边框（`--el-collapse-border-color: transparent`）。

- [ ] **Step 3: GuideEntry.vue（navbar 图标入口）**

```vue
<template>
  <el-tooltip content="功能助手" effect="dark" placement="bottom">
    <div class="right-menu-item hover-effect guide-entry" @click="guideStore.open()">
      <el-icon :size="18"><QuestionFilled /></el-icon>
    </div>
  </el-tooltip>
</template>

<script setup>
import useGuideStore from '@/store/modules/guide'

const guideStore = useGuideStore()
</script>

<style scoped>
.guide-entry {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
</style>
```

（`QuestionFilled`、`Close`、`Search` 图标已由 `@/components/SvgIcon/svgicon` 全局注册，无需 import。）

- [ ] **Step 4: Navbar.vue 注入入口**

Navbar.vue 模板中，在「消息通知」tooltip 块（`<el-tooltip content="消息通知" ...>`）**之前**插入：

```html
        <guide-entry id="guide-entry" />
```

script setup 追加 import：

```js
import GuideEntry from '@/guide/components/GuideEntry.vue'
```

（其余不动；`right-menu-item` 样式由 GuideEntry 根节点自带类命中 navbar 的非 scoped 继承——注意 Navbar 样式是 scoped，`.right-menu-item` 定义在 Navbar scoped 内对子组件根节点仍生效（scoped 属性会落在子组件根），实测若 padding 未生效，把 `.guide-entry` 的 `padding: 0 6px; height: 100%` 补进 GuideEntry scoped 样式即可。）

- [ ] **Step 5: layout/index.vue 挂载抽屉**

模板根部最后一个 `</div>` 之前插入 `<guide-drawer />`；script 追加 `import GuideDrawer from '@/guide/components/GuideDrawer.vue'`。

- [ ] **Step 6: 构建 + 运行时冒烟**

Run: `cd acr-ui && npm run build:prod` → 成功。
Playwright（acr-ui 目录内脚本）：登录 → 点击 navbar 问号图标 → 断言 `.el-drawer` 可见、含「接入第一个仓库」标题与「第 1 步」正文 → 点击目录「GitHub 接入」→ 断言显示「该文档暂未提供」（占位降级生效）→ 关闭抽屉。

- [ ] **Step 7: Commit**

```bash
git add acr-ui/src/guide/components/ acr-ui/src/layout/
git commit -m "feat: 功能助手抽屉骨架——navbar 入口、目录搜索、markdown 渲染与代码复制"
```

---

### Task 4: 三处配置页文字链深链

**Files:**
- Modify: `acr-ui/src/views/review/project/index.vue`（平台 form-item 后，约 133 行）
- Modify: `acr-ui/src/views/review/credential/index.vue`（dialog 内 `<el-form>` 开后，约 81 行）
- Modify: `acr-ui/src/views/system/aimodelconfig/index.vue`（dialog 内 `<el-form>` 开后，约 100 行）

**Interfaces:**
- Consumes: `useGuideStore().open(docId|null)`

- [ ] **Step 1: project/index.vue**

平台选择 `el-form-item`（`<el-select v-model="form.provider" placeholder="请选择平台" :disabled="!!form.projectId" @change="handleProviderChange">` 所在项）闭合标签后插入：

```html
                      <div class="guide-deep-link">
                        <el-button link type="primary" size="small" @click="openPlatformGuide">查看接入指引 →</el-button>
                      </div>
```

script setup 追加：

```js
import useGuideStore from '@/store/modules/guide'

const guideStore = useGuideStore()
const PLATFORM_GUIDE_DOC = { github: 'platform-github', gitlab: 'platform-gitlab', gitee: 'platform-gitee', gitea: 'platform-gitea' }

function openPlatformGuide() {
  guideStore.open(PLATFORM_GUIDE_DOC[form.provider] || null)
}
```

页面尾部 scoped 样式追加（三个页面共用同一规则，各自文件内添加）：

```scss
.guide-deep-link {
  margin: -8px 0 10px;
  line-height: 22px;
}
```

- [ ] **Step 2: credential/index.vue**

dialog 内 `<el-form ref="credentialRef" ...>` 开标签后插入：

```html
        <div class="guide-deep-link">
          <el-button link type="primary" size="small" @click="openPlatformGuide">按平台查看凭据创建指引 →</el-button>
        </div>
```

script 追加同 Step 1 的 import/常量/函数（该页 form 字段同为 `provider`，直接复用）。

- [ ] **Step 3: aimodelconfig/index.vue**

dialog 内 `<el-form ref="modelRef" ... class="model-config-form">` 开标签后插入：

```html
         <div class="guide-deep-link">
           <el-button link type="primary" size="small" @click="guideStore.open('model-config')">查看模型配置指引 →</el-button>
         </div>
```

script 追加 `import useGuideStore from '@/store/modules/guide'` 与 `const guideStore = useGuideStore()`。

- [ ] **Step 4: 构建 + 运行时验证**

`npm run build:prod` 成功。Playwright：打开项目新增对话框 → 点「查看接入指引 →」（未选平台）→ 抽屉打开且为默认篇；选择平台 GitLab 后再点 → 直达对应条目（文档未写完时显示占位降级，Task 5 完成后复验正文）；抽屉打开状态下关闭抽屉 → 表单已填内容（仓库地址等）保持不变。

- [ ] **Step 5: Commit**

```bash
git add acr-ui/src/views/review/project/index.vue acr-ui/src/views/review/credential/index.vue acr-ui/src/views/system/aimodelconfig/index.vue
git commit -m "feat: 项目/凭据/模型配置页接入指引深链"
```

---

### Task 5: 13 篇指引内容撰写

**Files:** Create `acr-ui/src/guide/content/` 下 13 个 md（路径与 manifest.file 一致）：

`platforms/{github,gitlab,gitee,gitea}.md`、`model-engine/{model-config,engine-config,check-failed}.md`、`commit-message/{convention,examples}.md`、`delivery/{channel-config,delivery-failed}.md`、`faq/{webhook-not-triggered,review-not-run}.md`

**Interfaces:**
- Consumes: spec §3 各篇「核心内容」列（事实清单）；spec §4 内容规范（内网自包含/占位符脱敏/步骤化口吻）

- [ ] **Step 1: 先写 `platforms/github.md` 作为四平台篇的统一模板**

结构固定为：① 创建访问令牌（路径、最小权限勾选项、有效期建议）→ ② 在平台创建凭据（字段对照）→ ③ 配置 Webhook（URL/Secret/事件三项 + 平台侧操作路径）→ ④ 验证连通（发测试 PR / 看事件落库）→ ⑤ 常见错误。GitHub 篇要点：Fine-grained PAT 选 `Pull requests: Read` + `Contents: Read`（回写评论需 `Pull requests: Read and write`）；Classic PAT 勾 `repo`；Webhook 事件订阅 `Pull requests`（opened/synchronize/reopened 由平台侧动作白名单控制）；Content type `application/json`。

- [ ] **Step 2: 按模板写 gitlab/gitee/gitea 三篇**

各篇只替换平台差异事实，结构不变：
- GitLab：PAT 最小 scope `read_api`（回写评论需 `api`）；支持 Project Access Token；Webhook 在「Settings → Webhooks」，Trigger 勾 `Merge request events`，Secret token 与平台一致；
- Gitee：私人令牌（设置 → 安全设置 → 私人令牌），最小权限 `projects`（回写需 `pull_requests`）；WebHook 在仓库「管理 → WebHooks」，支持密码/签名两种校验方式，本平台用签名（Secret）；
- Gitea：「设置 → 应用 → 生成令牌」勾 `repository` 相关权限；Webhook 在仓库「设置 → Webhooks → Gitea」，事件勾 Pull Request；
- **事实存疑处理**：token 权限项名称/路径以对应平台官方文档校对；执行者无法确认的点在交付说明中列出，由评审确认，禁止编造。

- [ ] **Step 3: model-engine 三篇**

- `model-config.md`：provider 参数字典（服务厂商/服务地址/API Key/模型名/超时/温度）、各主流厂商 apiKey 控制台获取路径（DeepSeek、通义、智谱、OpenAI 兼容等，与 `LlmProviderCode` 枚举对齐）、「设为默认」的含义（新建项目默认选中）、检测按钮验证什么；自定义厂商（custom）需填厂商名。
- `engine-config.md`：审查引擎适用场景（离线/内网/规则化）、可执行文件与探测、超时并发含义、与「大模型审查」怎么选。
- `check-failed.md`：决策树——网络不通（内网代理/出站）→ 密钥错误/过期 → 模型名不存在 → 额度不足；每支给出检查动作与修复动作；说明检测记录见模型列表「最近检测结果」列。

- [ ] **Step 4: commit-message 两篇**

- `convention.md` 机制事实（必须与代码一致）：每个 PR 最多取 **30 条** commit 进审查上下文、每条**只取首行**、单行 **500 字符**截断（`GitHubPullRequestCommitMessagesFormatter` MAX_COMMITS=30/MAX_LINE_LENGTH=500，四平台同款）；默认只审本次 Diff 变更行（M3.2）。建议：首行 ≤50 字自包含意图；Conventional Commits 前缀（feat/fix/refactor/docs/test/chore/perf）；一个 PR 控制在 30 个 commit 内，超出先 squash；正文换行写动机与影响面。
- `examples.md`：≥6 组「不推荐 / 推荐」对照（如「修 bug」→「fix: 修复台账列表分页越界导致的重复数据」），每组一句「为什么更好」；文末给可直接复制的模板代码块（feat/fix/refactor 三条）。

- [ ] **Step 5: delivery 两篇**

- `channel-config.md`：钉钉/企微/飞书群机器人创建路径与 webhook 地址获取；平台「通知管理 → 渠道管理」字段对照；项目绑定单渠道与 `notify_on_failure` 含义；投递内容形态（SUCCESS 摘要卡 / FAILED 简讯）。
- `delivery-failed.md`：机器人安全设置（自定义关键词需含「审查」等平台消息必含词/加签 secret/IP 白名单）、内网出站限制、投递记录列表定位失败原因与「补发」入口。

- [ ] **Step 6: faq 两篇**

- `webhook-not-triggered.md`：排查顺序——① Git 平台 Webhook  Recent deliveries 是否发出 → ② 平台事件是否落库（事件含忽略原因：分支不匹配/动作白名单外/签名错误/载荷超限/缺 Delivery 头）→ ③ Secret 是否一致 → ④ 内网部署需隧道/网关可达（cloudflared 等仅联调用）。
- `review-not-run.md`：任务状态决策树（待执行→执行中→成功/失败）、失败任务看失败原因与重试、模型/引擎检测先行、模板快照说明（改模板不影响已建任务）、存量问题默认不上报（M3.2 口径）。

- [ ] **Step 7: 脱敏与规范自检**

Run: `grep -rnE "ghp_[A-Za-z0-9]{20,}|glpat-[A-Za-z0-9_-]{15,}|sk-[A-Za-z0-9]{20,}|xox[baprs]-" acr-ui/src/guide/content/`
Expected: 无输出（占位符如 `ghp_xxxx…` 不命中长 token 模式）。同时目视检查每篇：步骤可独立执行、无外部链接作为必要步骤、代码块可复制。

- [ ] **Step 8: 构建 + 抽屉全量浏览**

`npm run build:prod` 成功。Playwright：遍历 14 个 docId 逐个 `guideStore.open(id)`（经页面 `window` 注入或逐一点击目录），断言每篇无「该文档暂未提供」且正文 >200 字符；抽查锚点滚动与代码块复制按钮存在。

- [ ] **Step 9: Commit**

```bash
git add acr-ui/src/guide/content/
git commit -m "docs: 功能助手 13 篇指引内容（平台接入/模型引擎/提交注释/投递/FAQ）"
```

---

### Task 6: 明暗主题与响应式打磨 + 全量验证矩阵

**Files:**
- Modify: `acr-ui/src/guide/components/GuideDrawer.vue`、`GuideContent.vue`（按验证结果修样式）
- Modify: `CHANGELOG.md`（Unreleased 补 M9 条目）

**Interfaces:**
- Consumes: 全部前序产物

- [ ] **Step 1: Playwright 验证矩阵（脚本放 acr-ui 目录内运行）**

覆盖并逐项记录结果：
1. 分辨率 1440 / 1280 / 1024 / 800（800 视口抽屉应 100% 宽——`appStore.device` 为 desktop 时仍 560px，属预期；仅 device=mobile 全屏，以实测为准记录行为）；
2. 浅色 + 暗色：代码块边框、表格、引用块、`.is-active` 目录项、复制按钮在暗色下可辨；
3. 抽屉开合不重置项目表单已填内容（Task 4 已测，回归）；
4. 搜索中文关键词命中（如「webhook」命中 4+ 篇）、无结果空态；
5. 三处深链直达对应文档（Task 5 后应为正文而非占位）；
6. 长文档滚动、代码块「复制」点击后按钮变「已复制」；
7. keep-alive：打开抽屉 → 切别的 tab → 切回 → 抽屉不残留（关闭态）；
8. 无 JS console 错误。

- [ ] **Step 2: 按验证结果修样式**

典型修复点：暗色下 `--neutral-code` 与抽屉底色对比、el-collapse 暗色边框、`.guide-md img` 暗色边框。修完重跑构建与截图对比。

- [ ] **Step 3: 构建**

Run: `cd acr-ui && npm run build:prod`
Expected: 成功。

- [ ] **Step 4: 内容验收走查**

按 spec 成功指标 1：以「只看功能助手、不看 README」方式走一遍「接入第一个仓库」全链路（可在测试仓库 miguchn/webhook-test 上真实执行），记录卡点。

- [ ] **Step 5: CHANGELOG + Commit**

CHANGELOG.md Unreleased 新增「功能助手（M9）」段落：抽屉入口、14 篇指引、三处深链、markdown 公共封装收敛。

```bash
git add acr-ui/src/guide/ CHANGELOG.md
git commit -m "feat: 功能助手明暗主题与响应式打磨、全量验证"
```

---

## Self-Review 记录

- Spec 覆盖：§2.1 入口/容器 → Task 3；§2.2 抽屉布局 → Task 3；§2.3 深链 → Task 4；§3 内容 14 篇 → Task 2（1 篇）+ Task 5（13 篇）；§4 内容规范 → Task 5 Step 7 + Global Constraints；§5.1 文件结构 → Task 1–3；§5.2 样式 → Task 3/6；§5.3 不改动项 → Task 1 约束；§7 验证 → 各 Task 验证步 + Task 6 矩阵。无缺口。
- 占位符扫描：无 TBD/TODO；Task 5 各篇给事实清单与模板，属内容任务正常粒度。
- 类型一致性：`renderMarkdown`、`useGuideStore().open/close/select`、`findDoc`、`loadDocContent`、`getAssetUrl`、`GUIDE_DOCS/GUIDE_GROUPS/DEFAULT_DOC_ID` 在产出与消费任务间签名一致；`PLATFORM_GUIDE_DOC` 的 key 与后端 provider 枚举（github/gitlab/gitee/gitea）一致。
- 已知执行注意点：① Navbar scoped 样式对子组件根的命中问题已在 Task 3 Step 4 给出兜底；② Task 5 平台事实存疑项须交付时列出，禁止编造；③ `import.meta.glob` 对缺失 md 不报错，占位降级由 GuideContent `missing` 态覆盖。
