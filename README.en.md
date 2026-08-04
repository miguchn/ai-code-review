<div align="center">
    <h1>AI Code Review — Enterprise Code Review Governance Platform</h1>
</div>

<div align="center">
    <em>Every line of your code must withstand AI scrutiny.</em>
</div>

<div align="center">
  <a href="https://github.com/miguchn/ai-code-review">
     <img src="docs/images/readme-hero.png" alt="AI Code Review — AI robot reviewing code" width="760" />
  </a>
</div>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/github/license/miguchn/ai-code-review" alt="License" /></a>
  <img src="https://img.shields.io/badge/Java-17-orange.svg" alt="Java 17" />
  <img src="https://img.shields.io/badge/Spring_Boot-4.x-6DB33F.svg" alt="Spring Boot 4" />
  <img src="https://img.shields.io/badge/Vue-3.x-42b883.svg" alt="Vue 3" />
</p>
<p align="center">
  <img src="https://img.shields.io/badge/GitHub-supported-181717.svg" alt="GitHub supported" />
  <img src="https://img.shields.io/badge/GitLab-supported-FC6D26.svg" alt="GitLab supported" />
  <img src="https://img.shields.io/badge/Gitee-supported-C71D23.svg" alt="Gitee supported" />
  <img src="https://img.shields.io/badge/Gitea-supported-609926.svg" alt="Gitea supported" />
</p>
<p align="center">
  English | <a href="README.md">简体中文</a>
</p>

---

## What is AI Code Review?

AI Code Review is an **enterprise-internal code review governance platform** deployed alongside your Git hosting platform. It turns MR/PR code changes into a traceable loop of review, remediation, verification, measurement, and audit — while GitHub, GitLab, Gitee, or Gitea remains the system of record for code collaboration and merging.

The main loop:

**Project & credential setup → webhook event intake → review execution → summary comment writeback → IM notification → issue disposition → workbench**

It focuses on: **uncontrollable review coverage** (reliable webhooks, idempotent tasks, failure recovery), **results that are hard to reach or act on** (writeback to the code platform, tiered notification, confirm/fix/appeal loops), **ungoverned rules and models** (versioned review policies, encrypted keys, tracked degradation), **unmeasurable quality** (unified coverage/latency/valid-issue metrics), and **weak enterprise security and audit** (org/business-system/project data isolation with audit trails).

It does not replace the Git platform, generic project or defect management, BI/APM, or human approval, and it does not provide individual performance rankings.

> ⚠️ **Project Status**: The MVP (V0.1) feature scope is complete, and **GitHub passed full-loop acceptance in a real environment (2026-08-04)**. GitLab/Gitee/Gitea are covered by contract tests pending real instances. The project remains in an internal pilot stage — do not depend on it in production yet. See the [Product Roadmap](docs/planning/product-roadmap.md) for the full acceptance record and release gates.

## Core Features

- **Unified four-platform access**: `GitAccessContext` + `GitAdapterRegistry` contract with GitHub / GitLab / Gitee / Gitea adapters; PAT and webhook secret stored with AES-GCM encryption, no echo in responses, connectivity checks, delete protection for referenced credentials
- **Trustworthy webhook intake**: per-platform signature verification (HMAC-SHA256 for GitHub), project matching by `repository_full_path`, action whitelist and target-branch checks, delivery-id dedup, full event lifecycle persisted
- **Dual review engines**: direct LLM review (model service + templates) or the alibaba/open-code-review engine, chosen per project; the policy snapshot is frozen when the task is created
- **Review scope policy**: diff-centric review with auto-expansion for high-impact changes (new files, public signatures, security/permission logic, config, dependencies, DB scripts); findings classified as "new in this change" vs "pre-existing" — pre-existing findings stay out of Top 3 and do not affect scoring; the scope decision snapshot is persisted and inspectable
- **Summary comment writeback**: one persistent summary comment (conclusion, score, Top 3 attribution, scope stats) idempotently upserted on the MR/PR after a successful review — re-reviews update in place instead of spamming; independent delivery records with per-failure retry
- **IM three-channel notification**: DingTalk / WeCom / Feishu bot delivery of review summaries or failure notices; encrypted channel management, test sends, enable/disable; delivery record filtering and resend
- **Issue ledger**: Top 3 focus findings materialized as issues (PR-level fingerprint dedup); confirm / close / ignore / false-positive loop with an action trail; summary comment re-rendered after disposition
- **Action-oriented workbench**: the home page answers "what should I handle today" — scope health, permission-driven todo cards, today's summary, recent activity
- **Unified scoring protocol**: five-dimension scoring (40/30/20/5/5) + Top 3 focus issues + versioned JSON protocol v1.0/v1.1; the backend validates and recomputes totals
- **Enterprise governance foundation**: RBAC + department data scope + business-system/project-owner isolation, with operation and login audit logs

## Quick Start

### Requirements

- Java 17+
- Node.js 18+
- MySQL 8.0+
- Redis 6.0+
- Maven 3.8+

### Backend

```bash
# 1. Initialize the database (one-shot script: full schema + menus/dicts/configs/built-in templates)
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

### OCR Review Engine (optional)

```bash
npm install -g @alibaba-group/open-code-review
ocr config provider   # configure model provider
ocr config model      # select model
```

### Onboard Your First Repository

1. Under "Project Onboarding → Git Credentials", add a platform PAT (encrypted, never echoed);
2. Under "Project Onboarding → Code Projects", paste the repository URL, let the platform read repository metadata, bind the business system / department / owner / credential, and multi-select PR target branches;
3. Configure the webhook on the Git platform: use the callback URL shown on the project page, with a secret matching the project configuration;
4. Open an MR/PR targeting one of the selected branches — the review task, record, and issue ledger appear under "Review Center".

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

## Feature Details

### Platform Foundation

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

### Code Review Business (MVP Vertical Slices M1–M7 + Four Git Providers)

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
| Review Scope Policy (M3.2) | Review scope centered on the current diff, with auto-expansion for high-impact changes; findings classified as "new in this change" vs "pre-existing" — pre-existing findings stay out of Top 3 and do not affect scoring; scope decision snapshot persisted |
| Summary Comment Writeback (M4) | Idempotent upsert of one summary comment per MR/PR after a successful review (conclusion, score, Top 3 attribution, scope stats); independent delivery records; failure retry decoupled from task state |
| IM Notification (M5) | DingTalk / WeCom / Feishu bot delivery of review summaries or failure notices; platform-level channel management (encrypted storage, test send, enable/disable) + single channel bound per project; delivery record filtering and resend |
| Issue Ledger (M6/M6.1) | Top 3 findings materialized as issues after SUCCESS (PR-level fingerprint dedup); confirm/close/ignore/false-positive loop with action trail; summary comment re-rendered after disposition; delivery trigger-source tracing |
| Workbench (M7) | Action-oriented home page after login: scope health, permission-driven todo cards, today summary, recent activity; list pages support query backfill |
| Review Templates (M3) | Template management under Policy Configuration; built-in Java/Python/Go/Vue/React/full-stack; edits do not affect historical tasks |
| Project Webhook Config (M2) | Callback URL display/copy, encrypted secret without echo, last received time and result |

## Release Roadmap

Releases advance as real vertical loops; security, authorization, audit, and operations readiness are built into every slice. Acceptance gates per stage are in the [Product Roadmap](docs/planning/product-roadmap.md).

| Release | Product goal | Core scope |
|------|------|------|
| MVP (V0.1 internal pilot) | Minimum trustworthy loop; **feature scope complete (2026-08-04); GitHub real-environment full-loop acceptance passed**, remaining platforms and continuous-operation gates pending | Four-platform MR/PR, onboarding, webhook, dual review paths, tasks/issues, summary writeback, IM three-channel notification, scoped access, audit, recovery, basic workbench |
| Core (V0.2 controlled rollout) | Actionable findings and governed policy | Inline comments, remediation and verification, versioned rules/review policies, model strategy, notification policy, shadow gate, and role-based workbench |
| Enterprise (V1.0 internal GA) | Enterprise-internal commercial readiness | Identity/account lifecycle, quality dashboard, report center, alerts, usage attribution, data lifecycle, backup/recovery, business SLI, and audit export |
| Scale (V1.1) | Controlled enforcement and platform ecosystem | Enforced gates and bypass, budgets/limits, more channels, full scans, and resource isolation |
| Later candidates | Independently justified initiatives | SARIF, approval-based false-positive optimization, session replay, and proactive sentinel capabilities |

The current top-level navigation is: Workbench, Review Center, Project Onboarding, Policy Configuration, Notification Management, System Management, and System Monitor. Target menus are added incrementally as vertical slices land — no empty entries are pre-built.

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

See [docs/deployment.md](docs/deployment.md) for more details.

## Documentation

| Document | Description |
|------|------|
| [Product Roadmap](docs/planning/product-roadmap.md) | Modules, dependencies, phases and acceptance gates (incl. the MVP real-environment acceptance record) |
| [Architecture & Scaffold](docs/planning/architecture-scaffold.md) | Actual tech baseline, module ownership and key flows |
| [Deployment](docs/deployment.md) | Environment setup, initialization and runtime |
| [Slice Design Docs](docs/planning/) | Design and completion status of each M1–M7 vertical slice |
| [Collaboration Entry](AGENTS.md) | Planning skills, dev rules and baseline verification |
| [SQL Scripts](sql/README.md) | One-shot initialization for fresh installs, incremental upgrades for existing ones |

## Reference Projects

| Project | Use | Notes |
|------|------|------|
| [PR-Agent](https://github.com/The-PR-Agent/pr-agent) | Reference | Provider capability contracts, automatic/manual triggers, incremental review, large-change handling, and low-noise persistent comments |
| [AI-Codereview-Gitlab](https://github.com/sunmh207/AI-Codereview-Gitlab) | Reference | China-oriented IM delivery, a lightweight webhook-to-writeback/report loop, and Agent sandbox/resource guidance |
| [alibaba/open-code-review](https://github.com/alibaba/open-code-review) | Engine candidate | Deterministic file selection/grouping/rule matching, Agent context exploration, structured positioning, recovery, and telemetry |

Reference projects validate capabilities and trade-offs; they are not feature templates. PR-assistant capabilities (description/Q&A/doc filling), entertainment-style review, individual rankings, shipping every platform at once, and silent auto-degradation do not enter the product. See the [Product Roadmap](docs/planning/product-roadmap.md#23-本地参考项目能力审计) for the audit.

## Contributing

- Conventional Commits (`feat:`, `fix:`, `refactor:`, `docs:`)
- Feature branches: `feature/<name>`, fix branches: `fix/<issue>`
- Must pass build and unit tests before commit (`mvn test` + `cd acr-ui && npm run build:prod`)
- Before slicing a code review feature, use `skills/plan-review-feature/` to scope and define acceptance
- New business must follow `rules/architecture.md` and `rules/delivery.md`; frontend UI must follow `rules/UI_THEME_RULES.md`

## License

This project is licensed under the [Apache License 2.0](LICENSE): free to use, modify, distribute, and commercialize. Redistributions and modifications must retain the copyright notice and license text, and modified files must carry prominent change notices. The license includes an express patent grant from contributors and a patent retaliation clause.

Third-party dependencies (Spring Boot, Vue, Element Plus, alibaba/open-code-review, etc.) remain under their own original licenses.
