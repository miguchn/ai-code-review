# AI Code Review — AI & Rule-Engine Powered Code Review Platform

> **Language**: English | [简体中文](README.md)
>
> ⚠️ **Project Status**: This project is in early development. The core code review features are not yet implemented, and it is **not ready for production use**. The repository currently provides the foundational management layer (RBAC, business-system ownership, model configuration) along with full product planning and architecture scaffolding, for reference and collaboration. You're welcome to follow the progress, but please do not depend on it in production yet.

## Project Positioning

AI Code Review is an **enterprise-internal code review governance platform** deployed alongside GitLab, GitHub, Gitee, or Gitea. It turns code changes into a traceable review, remediation, verification, measurement, and audit loop; the Git platform remains the system of record for code collaboration and merging.

It focuses on reliable review coverage, actionable findings, governed rules and models, consistent quality metrics, and project-scoped security and auditability. It does not replace the Git platform, generic project or defect management, BI/APM, or human approval, and it does not provide individual performance rankings. See the [Product Roadmap](docs/planning/product-roadmap.md) for product boundaries, target navigation, and release gates.

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                  AI Code Review Platform                 │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  Product & Governance Layer (this project)               │
│  ├── Workbench, review tasks, findings and onboarding    │
│  ├── Review policies, rule library, models and gates      │
│  ├── Writeback, notifications, dashboard and reports     │
│  ├── Project-scoped access, secrets, audit and retention │
│  └── Review SLI, recovery, capacity and cost governance  │
│                                                          │
│  Engine Layer (external integration)                      │
│  ├── alibaba/open-code-review (OCR)                      │
│  │   ├── diff incremental review (per MR / Push)         │
│  │   ├── full scan (scheduled patrol)                    │
│  │   ├── built-in rules for 29 languages                 │
│  │   └── Agent repo exploration (read_file + code_search)│
│  └── Optional direct LLM path, enabled only after release │
│      validation                                          │
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

The roadmap is organized around a real vertical review loop rather than implementing every platform and channel at once. P0 includes security, authorization, audit, and operations readiness.

| Release | Product goal | Core scope |
|------|------|------|
| MVP (V0.1 internal pilot) | Minimum trustworthy loop on one platform | One Git provider and MR/PR event, onboarding, webhook, one review path, tasks/findings, summary writeback, one notification channel, scoped access, audit, recovery, and basic workbench |
| Core (V0.2 controlled rollout) | Actionable findings and governed policy | Inline comments, remediation and verification, versioned rules/review policies, model strategy, notification policy, shadow gate, and role-based workbench |
| Enterprise (V1.0 internal GA) | Enterprise-internal commercial readiness | Identity/account lifecycle, quality dashboard, report center, alerts, usage attribution, data lifecycle, backup/recovery, business SLI, and audit export |
| Scale (V1.1) | More integrations and controlled enforcement | A second Git provider, enforced gates and bypass, budgets/limits, more channels, full scans, and resource isolation |
| Later candidates | Independently justified initiatives | SARIF, approval-based false-positive optimization, session replay, and proactive sentinel capabilities |

The target top-level navigation is: Workbench, Review Center, Project Onboarding, Policy Configuration, Insights, Notifications & Alerts, System Governance, and Operations. Review history is part of Review Tasks; findings use a dedicated ledger; Quartz schedules remain separate from review executions. Detailed submenus, metrics, priorities, and acceptance gates are in the [Product Roadmap](docs/planning/product-roadmap.md).

## Reference Projects

| Project | Use | Notes |
|------|------|------|
| [PR-Agent](https://github.com/The-PR-Agent/pr-agent) | Reference | Provider capability contracts, automatic/manual triggers, incremental review, large-change handling, and low-noise persistent comments |
| [AI-Codereview-Gitlab](https://github.com/sunmh207/AI-Codereview-Gitlab) | Reference | China-oriented IM delivery, a lightweight webhook-to-writeback/report loop, and Agent sandbox/resource guidance |
| [alibaba/open-code-review](https://github.com/alibaba/open-code-review) | Engine candidate | Deterministic file selection/grouping/rule matching, Agent context exploration, structured positioning, recovery, and telemetry |

Reference projects validate capabilities and trade-offs; their entire feature sets are not copied into the MVP. See the [Product Roadmap](docs/planning/product-roadmap.md#23-本地参考项目能力审计) for the audit.

## Quick Start

### Requirements

- Java 17+
- Node.js 18+
- MySQL 8.0+
- Redis 6.0+
- Maven 3.8+

### Backend

```bash
# 1. Initialize database (01-03 create schema; 04+ are incremental, run in order; utf8mb4 required for Chinese content)
mysql -u root -p < sql/01_core_schema.sql
mysql -u root -p < sql/02_quartz_schema.sql
mysql -u root -p < sql/03_system_management.sql
for f in sql/{04..18}_*.sql; do mysql --default-character-set=utf8mb4 -u root -p ai_code_review < "$f"; done

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
