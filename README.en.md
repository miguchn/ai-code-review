# AI Code Review — Enterprise Code Review Governance Platform

> **Language**: English | [简体中文](README.md)
>
> ⚠️ **Project Status**: The MVP (V0.1) feature scope is complete: all four platforms — GitHub, GitLab, Gitee, and Gitea — implement the main loop of "project & credential setup → webhook reception → review execution → summary comment writeback → IM notification → issue handling → workbench". Automated tests pass; closed-loop acceptance on real platform instances is still pending. The project remains in an internal pilot stage — do not depend on it in production yet.

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
│  ├── Direct LLM review (model service + templates,        │
│  │   LLM_DIRECT)                                          │
│  └── alibaba/open-code-review (OCR, OCR_ENGINE)           │
│      ├── diff incremental review (per MR / Push)          │
│      ├── full scan (scheduled patrol, planned)            │
│      ├── built-in rules for 29 languages                  │
│      └── Agent repo exploration (read_file + code_search) │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

## Tech Stack

| Layer | Tech | Notes |
|---|------|------|
| Frontend | Vue 3 + Element Plus + ECharts + Vite | Admin console |
| Backend | Java 17 + Spring Boot 4 + MyBatis + Druid | RESTful API |
| Database | MySQL 8 | Review records, users, config |
| Cache | Redis | Session, cache, rate limiting |
| Auth | Spring Security + JWT + RBAC | User / role / menu permissions |
| Review Engine | alibaba/open-code-review (Go CLI) | Invoked via subprocess |
| Notification | DingTalk / WeCom / Feishu bot Webhook | Review summary push |

## Project Structure

```
ai-code-review/
├── acr-common/          # Common utilities (utils, base classes, AI Client abstraction, XSS filter)
├── acr-system/          # Platform governance (RBAC, org, dict, business system, model config)
├── acr-review/          # Code review business (projects, Git credentials, GitAdapterRegistry, four-provider adapters)
├── acr-framework/       # Framework glue (Security, JWT, Druid, Redis, AOP, rate limiting)
├── acr-admin/           # Bootstrap, config and Web entry
├── acr-quartz/          # Generic scheduled task management
├── acr-ui/              # Frontend (Vue3 + Element Plus)
├── docs/planning/       # Product roadmap, architecture scaffold and slice design docs
├── skills/              # Per-feature planning skills
├── rules/               # Architecture and delivery constraints
└── sql/                 # Database scripts
```

## Planning & Development Entry

Before building the code review business, read in this order:

1. [Product Roadmap](docs/planning/product-roadmap.md): modules, dependencies, phases and acceptance criteria
2. [Architecture & Scaffold](docs/planning/architecture-scaffold.md): actual tech baseline, module ownership and key flows
3. [Collaboration Entry](AGENTS.md): planning skills, dev rules and baseline verification

The repo has implemented the main path of vertical slices M1–M7 and the four-platform Git Provider: Git integration supports **GitHub / GitLab / Gitee / Gitea** (`GitAccessContext` + `GitAdapterRegistry`, credential `server_url`, project `repository_full_path`, webhook at `/webhook/{provider}`). Refer to the "current baseline" in the [Product Roadmap](docs/planning/product-roadmap.md) for the authoritative completion status.

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

### Completed (MVP Vertical Slices M1–M7 + Four Git Providers)

| Module | Features |
|------|------|
| Business System Ownership | One business system owns multiple repository projects; entry under "Project Onboarding" |
| Git Credentials | Encrypted PAT storage for all four providers, no echo in responses, blank-keeps-original on edit, delete protection for referenced credentials, connectivity check; `server_url` for self-hosted GitLab/Gitea |
| Code Projects | Repository metadata auto-read, full branch sync, business-system/department/owner/credential binding, enable/disable, connectivity check |
| PR Review Scope | PR review enabled by default, multi-select target branches from real branches, `dev`/`develop` recommended first, unified per-platform event defaults |
| Git Provider | Unified `GitAccessContext` / `GitAdapterRegistry` contract; GitHub, GitLab, Gitee, Gitea adapters (connectivity, diff/metadata, workspace, summary comment) |
| Multi-platform Webhook | `POST /webhook/{github,gitlab,gitee,gitea}` with signature verification and dedup; projects matched by `repository_full_path`; GitHub path stays backward compatible |
| PR Webhook Intake (M2) | HMAC-SHA256 verification, project matching, action whitelist and target-branch checks, delivery-id dedup, full event lifecycle persisted |
| Review Tasks (M2/M3/M3.1) | Async execution after event intake; task page as execution queue; failure classification and safe retry; standalone detail page |
| Review Records (M3.1) | Finished-review history list and standalone detail; score/summary/Top 3 and execution runs in tabs; reuses task tables, no extra result table |
| Review Execution (M3) | Mutually exclusive review modes: direct LLM (model service + template) or OCR engine; snapshot frozen at task creation |
| Review Scope Policy (M3.2) | Review scope centered on the current diff, with auto-expansion for high-impact changes (new files, public signatures, security/permission logic, config, dependencies, DB scripts); findings classified as "new in this change" vs "pre-existing" — pre-existing findings stay out of Top 3 and do not affect scoring; project-level exclude/test-file/existing-report/expansion switches frozen with the task snapshot; scope decision snapshot persisted |
| Summary Comment Writeback (M4) | Idempotent upsert of one summary comment per MR/PR after a successful review (conclusion, score, Top 3 attribution, scope stats); independent delivery records; failure retry decoupled from task state |
| IM Notification (M5) | DingTalk / WeCom / Feishu bot delivery of review summaries or failure notices; platform-level channel management (encrypted storage, test send, enable/disable) + single channel bound per project; delivery record filtering and resend |
| Issue Ledger (M6/M6.1) | Top 3 findings materialized as issues after SUCCESS (PR-level fingerprint dedup); confirm/close/ignore/false-positive loop with action trail; summary comment re-rendered after disposition; delivery trigger-source tracing |
| Workbench (M7) | Action-oriented home page after login: scope health, permission-driven todo cards, today summary, recent activity; list pages support query backfill |
| Review Templates (M3) | Template management under Policy Configuration; built-in Java/Python/Go/Vue/React/full-stack; edits do not affect historical tasks |
| Unified Scoring Protocol | Five-dimension scoring (40/30/20/5/5) + Top 3 focus issues + JSON protocol v1.0/v1.1 (v1.1 adds finding attribution `origin` and scope stats); backend validates and recomputes totals; parse failures flagged separately |
| Project Webhook Config (M2) | Callback URL display/copy, encrypted secret without echo, last received time and result |

### Release Plan

Releases advance as real vertical loops; security, authorization, audit, and operations readiness are built into every slice. Acceptance gates per stage are in the [Product Roadmap](docs/planning/product-roadmap.md).

| Release | Product goal | Core scope |
|------|------|------|
| MVP (V0.1 internal pilot) | Minimum trustworthy loop; **feature scope complete (2026-08-04), pending real-environment acceptance** | Four-platform MR/PR, onboarding, webhook, dual review paths, tasks/issues, summary writeback, IM three-channel notification, scoped access, audit, recovery, basic workbench |
| Core (V0.2 controlled rollout) | Actionable findings and governed policy | Inline comments, remediation and verification, versioned rules/review policies, model strategy, notification policy, shadow gate, and role-based workbench |
| Enterprise (V1.0 internal GA) | Enterprise-internal commercial readiness | Identity/account lifecycle, quality dashboard, report center, alerts, usage attribution, data lifecycle, backup/recovery, business SLI, and audit export |
| Scale (V1.1) | Controlled enforcement and platform ecosystem | Enforced gates and bypass, budgets/limits, more channels, full scans, and resource isolation |
| Later candidates | Independently justified initiatives | SARIF, approval-based false-positive optimization, session replay, and proactive sentinel capabilities |

The current top-level navigation is: Workbench, Review Center, Project Onboarding, Policy Configuration, Notification Management, System Management, and System Monitor. Target menus are added incrementally as vertical slices land — no empty entries are pre-built. Review records live alongside review tasks; findings use a dedicated ledger; Quartz schedules remain separate from review executions. Detailed submenus, metrics, priorities, and acceptance gates are in the [Product Roadmap](docs/planning/product-roadmap.md).

## Reference Projects

| Project | Use | Notes |
|------|------|------|
| [PR-Agent](https://github.com/The-PR-Agent/pr-agent) | Reference | Provider capability contracts, automatic/manual triggers, incremental review, large-change handling, and low-noise persistent comments |
| [AI-Codereview-Gitlab](https://github.com/sunmh207/AI-Codereview-Gitlab) | Reference | China-oriented IM delivery, a lightweight webhook-to-writeback/report loop, and Agent sandbox/resource guidance |
| [alibaba/open-code-review](https://github.com/alibaba/open-code-review) | Engine candidate | Deterministic file selection/grouping/rule matching, Agent context exploration, structured positioning, recovery, and telemetry |

Reference projects validate capabilities and trade-offs; they are not feature templates. PR-assistant capabilities (description/Q&A/doc filling), entertainment-style review, individual rankings, shipping every platform at once, and silent auto-degradation do not enter the product. See the [Product Roadmap](docs/planning/product-roadmap.md#23-本地参考项目能力审计) for the audit.

## Quick Start

### Requirements

- Java 17+
- Node.js 18+
- MySQL 8.0+
- Redis 6.0+
- Maven 3.8+

### Backend

```bash
# 1. Initialize database (one-shot script: full schema + menus/dicts/configs/built-in templates)
#    For existing environments, apply incremental scripts per sql/README.md instead
mysql --default-character-set=utf8mb4 -u root -p < sql/init-full.sql

# 2. Configure a stable 32-byte Base64 master key for Git credentials and webhook secrets
export ACR_CREDENTIAL_MASTER_KEY="$(openssl rand -base64 32)"

# 3. Adjust DB config
# edit acr-admin/src/main/resources/application-dev.yml

# 4. Build & run
mvn clean install -DskipTests
cd acr-admin
mvn spring-boot:run
```

`ACR_CREDENTIAL_MASTER_KEY` must be provided by the deployment's secret management and stay consistent across restarts, scaling, and recovery; rotate credentials before changing the master key. The repo ships no default value.

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
