# AI Code Review — AI & Rule-Engine Powered Code Review Platform

> **Language**: English | [简体中文](README.md)
>
> ⚠️ **Project Status**: This project is in early development. The core code review features are not yet implemented, and it is **not ready for production use**. The repository currently provides the foundational management layer (RBAC, business-system ownership, model configuration) along with full product planning and architecture scaffolding, for reference and collaboration. You're welcome to follow the progress, but please do not depend on it in production yet.

## Project Positioning

AI Code Review is an **intelligent code review management platform** for enterprise internal use, addressing these core pain points:

1. **Developers don't read reviews** — push results to DingTalk / WeCom / Feishu groups to actively reach developers
2. **All-English is hard to read** — native Chinese support, integrates domestic LLMs (DeepSeek / Qwen, etc.)
3. **No admin console** — visually manage model config, projects, user permissions, review rules
4. **No notifications or history** — graded notification push, persisted review results, issue tracking (fixed or not)
5. **Insufficient whole-codebase understanding** — integrates alibaba/open-code-review engine, supports Agent-level full-repo exploration

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                  AI Code Review Platform                 │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  Management Layer (this project)                         │
│  ├── Web Admin Console (Vue3 + Element Plus)             │
│  ├── DingTalk / WeCom / Feishu + Email notifications     │
│  ├── Dashboard + statistical reports                     │
│  ├── User / Role / Permission management (RBAC)          │
│  ├── Project management + review rule config             │
│  ├── Review record storage (MySQL) + history query       │
│  ├── Model config management (API Key, provider switch)  │
│  └── Daily / Weekly / Monthly reports auto-generation    │
│                                                          │
│  Engine Layer (external integration)                      │
│  ├── alibaba/open-code-review (OCR)                      │
│  │   ├── diff incremental review (per MR / Push)         │
│  │   ├── full scan (scheduled patrol)                    │
│  │   ├── built-in rules for 29 languages                 │
│  │   └── Agent repo exploration (read_file + code_search)│
│  └── Fallback: direct LLM call (DeepSeek / Qwen / OpenAI) │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

## Tech Stack

| Layer | Tech | Notes |
|---|------|------|
| Frontend | Vue 3 + Element Plus + ECharts + Vite | Admin console + dashboard |
| Backend | Java 17 + Spring Boot 4 + MyBatis + Druid | RESTful API |
| Database | MySQL 8 | Review records, users, config |
| Cache | Redis | Session, cache, rate limiting |
| Auth | Spring Security + JWT + RBAC | User / role / menu permissions |
| Review Engine | alibaba/open-code-review (Go CLI) | Invoked via subprocess |
| Notification | DingTalk / WeCom / Feishu Webhook + Email | Multi-channel push |

## Project Structure

```
ai-code-review/
├── acr-common/          # Common utilities (utils, base classes, AI Client abstraction, XSS filter)
├── acr-system/          # Platform governance (RBAC, org, dict, business system, model config)
├── acr-review/          # Code review business boundary (Maven module only, not yet implemented)
├── acr-framework/       # Framework glue (Security, JWT, Druid, Redis, AOP, rate limiting)
├── acr-admin/           # Bootstrap, config and Web entry
├── acr-quartz/          # Generic scheduled task management
├── acr-ui/              # Frontend (Vue3 + Element Plus)
├── docs/planning/       # Product roadmap and minimal architecture scaffold
├── skills/              # Per-feature planning skills
├── rules/               # Architecture and delivery constraints
└── sql/                 # Database scripts
```

## Planning & Development Entry

Before building the code review business, read in this order:

1. [Product Roadmap](docs/planning/product-roadmap.md): modules, dependencies, phases and acceptance criteria
2. [Architecture & Scaffold](docs/planning/architecture-scaffold.md): actual tech baseline, module ownership and key flows
3. [Collaboration Entry](AGENTS.md): planning skills, dev rules and baseline verification

The repo already provides business-system ownership management, plus model config (enable/disable, default model, connection test) — these only cover part of the upcoming "project onboarding" and "model management". `acr-review` currently only establishes the dependency boundary with no review business implemented. Refer to the "current baseline" in the product roadmap for actual progress.

## Modules

### Available (Foundation)

| Module | Features |
|------|------|
| Users | CRUD, password policy, online user monitoring |
| Roles | RBAC roles, data permissions, menu permissions |
| Departments | Org tree |
| Menus | Dynamic menus, button permissions |
| Dictionaries | System & business dicts |
| Config | Dynamic system parameters |
| Notices | In-app notifications, announcements |
| Operation Log | Audit log, login log |
| Scheduled Tasks | Task CRUD, execution log |
| System Monitor | CPU / Memory / JVM, Redis, Druid monitoring |

### Planned (Code Review Business)

Priorities indicate product direction; detailed breakdowns, dependencies and acceptance are in `docs/planning/product-roadmap.md`.

| Module | Features | Priority |
|------|------|:---:|
| Multi-platform project onboarding | GitHub / GitLab / Gitee / Gitea support, unified Git Provider abstraction (auth, API adaptation), project↔business-system/dept binding | P0 |
| Webhook receiver | Multi-platform event receiver (PR / MR / Push), signature verification, dedup, idempotency | P0 |
| Engine integration | alibaba/OCR subprocess call, JSON parsing, timeout/retry, fallback to direct LLM | P0 |
| Result writeback | Multi-platform MR / PR inline comment (GitHub / GitLab / Gitee / Gitea API adaptation) | P0 |
| Review orchestration | webhook→fetch diff→invoke OCR→persist→writeback→notify full chain | P0 |
| Notification push | DingTalk / WeCom / Feishu Webhook + Email, multi-channel strategy, severity-triggered, @committer, dedup | P0 |
| Review records | Storage (task→result→comment 3-level), history query, detail view | P0 |
| Defect workflow | Assign / claim, state flow (to-fix / fixed / ignored / false-positive), false-positive flag | P1 |
| Model management | Multi-provider config, encrypted API Key, default model, timeout/token limits, fallback chain | P1 |
| Review rules | Project-level rule config, custom prompts, rule library sharing, versioning | P1 |
| Dashboard | Team / project / personal quality stats, trends, severity/category distribution | P1 |
| Member analysis | Developer commit behavior, issue distribution, personal quality profile | P1 |
| Cost stats | Token consumption, cost by project/model, quota & rate limiting | P2 |
| Daily / Weekly reports | Auto-generated quality reports, scheduled push | P2 |
| Full scan | Scheduled full-repo scan, local repo clone, per-project-path storage, legacy issue discovery | P2 |
| Session replay | Review session visualization, multi-user shared replay (OCR Session Viewer rework) | P2 |
| Project sentinel | Proactive monitoring + alert rules | P2 |
| Merge gate | Auto-block MR / PR on HIGH issues (multi-platform status check API) | P2 |
| False-positive feedback | Feedback sediment, rule / prompt optimization loop | P3 |
| SARIF export | JSON→SARIF conversion, integrate SonarQube / GitHub Code Scanning | P3 |

## Reference Projects

| Project | Use | Notes |
|------|------|------|
| [PR-Agent](https://github.com/The-PR-Agent/pr-agent) | Reference | Engine design, prompt templates, Git Provider architecture |
| [AI-Codereview-Gitlab](https://github.com/sunmh207/AI-Codereview-Gitlab) | Reference | Domestic enterprise landing solution, notifications, dashboard, reports |
| [alibaba/open-code-review](https://github.com/alibaba/open-code-review) | Integration | Review engine, full scan, 29-language rules, Agent repo exploration |

## Quick Start

### Requirements

- Java 17+
- Node.js 18+
- MySQL 8.0+
- Redis 6.0+
- Maven 3.8+

### Backend

```bash
# 1. Initialize database
mysql -u root -p < sql/ry_20260417.sql
mysql -u root -p < sql/quartz.sql
mysql -u root -p < sql/sys_manage_20260512.sql

# 2. Adjust DB config
# edit acr-admin/src/main/resources/application-dev.yml

# 3. Build & run
mvn clean install -DskipTests
cd acr-admin
mvn spring-boot:run
```

### Frontend

```bash
cd acr-ui
npm install
npm run dev
```

### OCR Engine (optional)

```bash
npm install -g @alibaba-group/open-code-review
ocr config provider   # configure model provider
ocr config model      # select model
```

## Deployment

Docker Compose deployment (TBD):

```yaml
# Planned deployment architecture
services:
  mysql:       # MySQL 8.0
  redis:       # Redis 7
  backend:     # Spring Boot app
  frontend:    # Nginx + Vue3 static assets
  ocr:         # alibaba/open-code-review CLI (bundled in backend container)
```

## Development Conventions

- Conventional Commits (`feat:`, `fix:`, `refactor:`, `docs:`)
- Feature branches: `feature/<name>`, fix branches: `fix/<issue>`
- Must pass build and unit tests before commit
- Before slicing a code review feature, use `skills/plan-review-feature/` to scope and define acceptance
- New business must follow `rules/architecture.md` and `rules/delivery.md`
