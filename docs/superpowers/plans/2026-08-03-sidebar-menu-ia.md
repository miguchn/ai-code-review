# 左侧菜单信息架构调整 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 按已确认规格重组侧栏（工作台可见 + 审查中心 / 项目接入 / 策略配置），系统管理与系统监控不动。

**Architecture:** 菜单树由 `sys_menu` 驱动；增量 SQL 调整名称/父级/排序并新建一级目录 `menu_id=6`。工作台在 `constantRoutes` 去隐藏。父级迁移会改变前端路由前缀，须同步工作台与项目页中硬编码的 `router.push`/`resolve` 路径；后端 API 与 `perms` 不变。

**Tech Stack:** MySQL `sys_menu` / `sys_role_menu`；Vue 3 + Vue Router（RuoYi 动态路由）。

## Global Constraints

- 不改 `perms`、后端接口、页面业务逻辑（仅允许因路由前缀变化的导航字符串）。
- 系统管理（1）、系统监控（2）子树冻结。
- 不预建数据洞察等空入口；提示词菜单 126 保持下线。
- SQL 幂等、utf8mb4；下一序号 `27`。

---

### Task 1: 增量 SQL 菜单迁移

**Files:**
- Create: `sql/27_sidebar_menu_ia.sql`
- Modify: `sql/README.md`（追加 27）
- Modify: `docs/deployment.md`（追加执行行）

**Interfaces:**
- Produces: `menu_id=6` path=`project-access`；一级 order：3=1,6=2,4=3,5=4,1=5,2=6；子菜单归属见规格 §5。

- [x] **Step 1: 编写幂等 SQL**

```sql
-- 新建「项目接入」；重命名审查中心/策略配置/业务系统/代码项目；
-- 迁移 parent_id + order_num；为拥有 121/122/123 的角色补授 menu_id=6。
```

完整语句写入 `sql/27_sidebar_menu_ia.sql`（utf8mb4、WHERE NOT EXISTS / 明确 UPDATE）。

- [x] **Step 2: 更新 `sql/README.md` 与 `docs/deployment.md` 清单**

- [x] **Step 3: 若本地 MySQL 可用则执行脚本；否则以脚本审阅为准**

---

### Task 2: 工作台侧栏可见 + 导航路径对齐

**Files:**
- Modify: `acr-ui/src/router/index.js`（去掉工作台 Layout 的 `hidden: true`）
- Modify: `acr-ui/src/views/index.vue`（`/review/project` → `/project-access/project`）
- Modify: `acr-ui/src/views/review/project/index.vue`（凭据/模板前端路由）

**Interfaces:**
- Consumes: 新前缀 `/project-access/*`、`/model-service/template`
- 保持：`/review/task|record|issue`、`/model-service/ai-model-config|engine`、`/notify/*`

- [x] **Step 1: 工作台父路由取消 hidden**
- [x] **Step 2: 更新硬编码前端路径**
  - `index.vue`: `/project-access/project`
  - `project/index.vue`: `/project-access/credential`、`/model-service/template`
- [x] **Step 3: `cd acr-ui && npm run build:prod`**

---

### Task 3: 文档与收尾

**Files:**
- Modify: `CHANGELOG.md`（简短条目）
- Modify: `docs/superpowers/specs/2026-08-03-sidebar-menu-ia-design.md` 状态为已实现（可选）

- [x] **Step 1: CHANGELOG 记录菜单 IA 调整与必要前端路径同步**
- [x] **Step 2: 对照规格 §7 验收清单自检**
