<div align="center">
  <h1>AI Code Review — Self-Hosted Review Governance for Enterprise Teams</h1>
</div>

<div align="center">
  <h4>Turn MR/PR and push events into a traceable loop of AI review, low-noise delivery, remediation, re-verification, and quality measurement.</h4>
</div>

> **See a real result first (30 seconds):** [this public PR](https://github.com/miguchn/acr-demo/pull/1#issuecomment-5201536018) deliberately keeps typical flaws such as SQL injection and hardcoded credentials. Its ACR summary comment was automatically written back and updated in place by a real platform run — it is not a marketing screenshot.

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/github/license/miguchn/ai-code-review" alt="License" /></a>
  <img src="https://img.shields.io/badge/status-V0.2_controlled_rollout-orange.svg" alt="V0.2 controlled rollout" />
  <img src="https://img.shields.io/badge/Java-17-orange.svg" alt="Java 17" />
  <img src="https://img.shields.io/badge/Spring_Boot-4.x-6DB33F.svg" alt="Spring Boot 4" />
  <img src="https://img.shields.io/badge/Vue-3.x-42b883.svg" alt="Vue 3" />
</p>
<p align="center">
  <img src="https://img.shields.io/badge/GitHub-E2E_verified-181717.svg" alt="GitHub E2E verified" />
  <img src="https://img.shields.io/badge/GitLab-adapter_tested-FC6D26.svg" alt="GitLab adapter tested" />
  <img src="https://img.shields.io/badge/Gitee-adapter_tested-C71D23.svg" alt="Gitee adapter tested" />
  <img src="https://img.shields.io/badge/Gitea-adapter_tested-609926.svg" alt="Gitea adapter tested" />
</p>
<p align="center">
  English | <a href="README.md">简体中文</a>
</p>

<p align="center">
  <a href="#product-positioning">Positioning</a> ·
  <a href="#implemented-capabilities">Capabilities</a> ·
  <a href="#governance-loop-and-architecture">Architecture</a> ·
  <a href="#quick-start">Quick Start</a> ·
  <a href="#current-stage-and-roadmap">Roadmap</a>
</p>

## Product Positioning

AI Code Review is designed for internal enterprise engineering teams and runs alongside the source-code hosting platform. The Git platform remains the system of record for collaboration and merging; this platform connects code events into a governable workflow: **trusted intake → review execution → conclusion delivery → remediation → re-verification → quality measurement**.

The goal is not to generate more comments. It addresses platform-level problems: feedback that nobody follows up, noisy repeated reviews, risks with no owner, conclusions that cannot be re-verified, and quality trends that managers cannot see.

| Concern | Typical one-off AI review | AI Code Review |
|---|---|---|
| Delivering conclusions | Leaves comments and stops; repeated runs create noise | Maintains one summary comment per MR/PR, updates it in place, and retries delivery independently |
| Following up issues | Relies on developers to remember and act | Tracks confirmation, remediation, recheck, closure, dismissal, and false positives in an issue ledger |
| Ownership and reach | Results remain scattered across the Git platform | Auto-assignment, overdue flags, and tiered group-bot delivery to DingTalk / WeCom / Feishu |
| Enterprise governance | Primarily repository- or tool-level configuration | Central project authorization, credential governance, runtime recovery, business traces, and quality insights |
| Deployment boundary | Often depends on an externally hosted service | Open-source and self-hosted, with encrypted credentials and controlled model endpoints |

The current release is **V0.2 Core under controlled rollout**. GitHub has passed an end-to-end acceptance run against a real repository. GitLab, Gitee, and Gitea have adapter and contract-test coverage, but still require acceptance against each target enterprise's real instance. This is not yet V1.0 enterprise production readiness.

## Implemented Capabilities

| Capability area | What is implemented today, including its boundary |
|---|---|
| 🧩 Multi-platform intake and triggers | A unified adapter contract for GitHub / GitLab / Gitee / Gitea; MR/PR events and push review on configured branches. Push review is post-merge detection and governance, not a pre-merge enforcement gate |
| 🔐 Trusted webhooks | Per-platform signature verification, payload limits, repository-path and target-branch matching, and delivery-idempotent deduplication. A dedup key is claimed only after successful verification; forged or misrouted events are rejected |
| ⚙️ Dual execution modes and scope governance | Direct LLM review with `LLM_DIRECT`, or optional `OCR_ENGINE`; frozen policies for diff inclusion/exclusion, test files, existing findings, and high-impact file expansion. The default Docker image does not bundle the OCR CLI |
| 🧠 Structured review results | Five-dimension scoring, backend-recomputed totals, a structured issue list, top-three highlights, and NEW/EXISTING classification; model, template, prompt, scope, and execution snapshots are retained per run |
| 💬 Low-noise delivery | Idempotent creation or update of an MR/PR summary comment; project-controlled inline comments for critical/high findings; conclusion, summary, inline-comment, and IM delivery states remain independent |
| 📣 Chinese enterprise IM | DingTalk / WeCom / Feishu group bots; project-level conclusion policy, cooldown for lower-priority messages, failure notifications, body snapshots, retry, and delivery tracing. One project currently binds one channel |
| 📒 Issue-governance loop | All new findings enter the ledger; confirmation, closure, dismissal, false-positive marking, recheck, reopen, batch disposition, and a lifecycle timeline; automatic assignment from commit author/PR author/project owner plus overdue scans and aggregated reminders |
| 👥 Authorization and sensitive assets | Effective access is the intersection of **functional RBAC × department DataScope × project membership role**; project roles are OWNER / ADMIN / REVIEWER / VIEWER. Git tokens, webhook secrets, notification endpoints, and model API keys use AES-GCM encryption and are never returned in plaintext |
| 🛡️ Reliability and operations | Database-driven task and delivery dispatch, lease/epoch fencing, backoff and recovery, bounded executors, per-project concurrency, Git/workspace/OCR/LLM resource budgets, plus a runtime overview for backlog, failures, resources, and dependency alerts |
| 📊 Data insights | Quality overview, project detail, member analysis, commit trends, added/deleted lines, identity binding, and metric definitions; token usage is captured per run and cost is estimated using current model prices. Price versions, budgets, and quotas are not implemented |
| 🧭 In-product guidance | A global feature assistant with 14 Chinese guides covering first setup, all four platforms, models and engines, commit messages, delivery, and troubleshooting; key configuration pages deep-link into the relevant guide |

## Governance Loop and Architecture

<p align="center">
  <img src="docs/images/readme-loop-diagram.png" alt="AI Code Review governance loop" width="760" />
</p>

The Git platform remains the merge system of record; ACR owns the governance loop: MR/PR or push event → trusted webhook → review execution → summary/inline comments and IM delivery → issue ledger → remediation push triggers recheck → insights and operations.

The project is a modular monolith with explicit responsibilities:

```text
GitHub / GitLab / Gitee / Gitea
              │ Webhook / API
              ▼
acr-ui ──HTTP──> acr-admin                 # bootstrap, REST/webhooks, auth, protocol validation
                  ├──> acr-review          # review domain, orchestration, Git/engine/IM adapters
                  │      ├──> MySQL         # event, task, issue, delivery, and insight facts
                  │      └──> Redis         # cache, rate limiting, short-lived idempotency support
                  ├──> acr-system          # users, roles, departments, business systems, model config
                  └──> acr-quartz          # generic scheduled-job triggers
```

The stack uses Java 17, Spring Boot 4, MyBatis, MySQL, Redis, Quartz, and Vue 3. It does not introduce a message queue, workflow engine, or vector database. The review business flow stays in `acr-review`, and an external delivery failure cannot rewrite an internal review fact that has already been established.

## Use Cases

| Scenario | What the platform adds |
|---|---|
| Source code cannot be sent to an external SaaS | Self-host inside the enterprise and centrally control Git/model credentials and code-egress boundaries |
| Multiple teams and repositories need one review standard | Configure by business system, project, branch, scope, model, and template, while retaining each run's effective snapshots |
| Teams use both MR/PR and direct-push workflows | Write conclusions back into MR/PRs; send configured-branch pushes into post-merge review, notification, and the issue ledger |
| Quality or security teams need to operate findings continuously | Turn one-off comments into an assignable, re-verifiable, measurable governance ledger |
| Engineering managers need quality and cost signals | View coverage, conclusions, remediation, members, and token usage within the authorized scope, without turning individual rankings into an engineering verdict |

This project does not replace the Git platform, human approval, an IDE, or project-management systems such as Jira. It also does not yet provide a mandatory merge gate.

## Screenshots from Real Runs

The screenshots below come from a real environment. The IM card is a style sample rendered from real run data. See the [public demo PR](https://github.com/miguchn/acr-demo/pull/1#issuecomment-5201536018) for a result you can verify directly.

**PR summary comment write-back** — high-risk conclusion, top-three findings, total finding count, and scope statistics:

<p align="center"><img src="docs/images/readme-pr-comment.png" alt="PR summary comment write-back" width="720" /></p>

**Tiered IM notification** — WeCom card style sample; DingTalk / WeCom / Feishu are supported channels:

<p align="center"><img src="docs/images/readme-im-notify.png" alt="Tiered IM notification card sample" width="560" /></p>

**Issue ledger** — severity, stage, ownership, round history, and quick filters:

<p align="center"><img src="docs/images/readme-issue-ledger.png" alt="Issue ledger" width="720" /></p>

**Review record detail** — five-dimension scores, model commentary, summary-delivery state, and external comment ID:

<p align="center"><img src="docs/images/readme-record-detail.png" alt="Review record detail" width="720" /></p>

**Workbench** — today's queue, project risk trend, and task status:

<p align="center"><img src="docs/images/readme-workbench.png" alt="Workbench" width="720" /></p>

## Quick Start

### One-command trial with Docker Compose

Requires Docker Engine, Compose v2, and a free host port 80. The first build downloads full backend and frontend dependencies; duration depends on your network.

```bash
cp .env.example .env
openssl rand -base64 32
# Copy the previous command's output into ACR_CREDENTIAL_MASTER_KEY in .env
docker compose up -d --build
docker compose ps
```

When all four services report `healthy`, open <http://127.0.0.1> and sign in with the default administrator `admin / admin123`. Change the default password immediately. This is a trial environment, not a hardened production setup; see the [deployment guide](docs/deployment.md) for production configuration, upgrades, and rollback.

To receive callbacks from a public Git provider, first expose an HTTPS address reachable by that provider and set `ACR_WEBHOOK_CALLBACK_URL` in `.env`. If teammates need to open detail links from IM messages, set the `review.ui.base-url` system parameter to a frontend address they can reach.

### Connect the first repository (GitHub example)

1. Under `策略配置 → 大模型配置` (Strategy Configuration → LLM Configuration), add a model and complete the call test, or prepare the optional OCR engine below;
2. Under `项目接入 → 访问凭据` (Project Onboarding → Access Credentials), add a GitHub token, then create a code project and select its target branches, review mode, template, and scope policy;
3. Enable the project after its connection test passes, then copy the generated webhook URL and secret;
4. Configure the GitHub repository webhook and subscribe to Pull Request events. Enable and configure push branches in the project only if push review is needed;
5. Create/update a PR or push a configured branch. When processing finishes, inspect the review task, PR summary comment, delivery record, and issue ledger.

The in-product `功能助手` (Feature Assistant) provides the same step-by-step flow plus token and webhook instructions for all four providers. The current web UI and embedded guides are Chinese-first; full i18n is not implemented.

### Reuse a host MySQL database (optional)

Compose uses its bundled MySQL service by default. To reuse an existing test database on the host, set `ACR_MYSQL_HOST=host.docker.internal` plus the port, database, username, and password in `.env`. Apply only the missing incremental scripts described in the [SQL guide](sql/README.md); **never** run `init-full.sql` against a database that already contains business data.

### OCR engine (optional)

The default backend image does not install the open-code-review CLI, and `LLM_DIRECT` works without it. To enable `OCR_ENGINE`, install it in a custom runtime with `npm install -g @alibaba-group/open-code-review`, or mount an existing executable into the container, then set its in-container command or absolute path, for example `ACR_OCR_EXECUTABLE=ocr`. Project configuration and the runtime overview expose the probe result; an unavailable OCR engine becomes a runtime alert only when at least one enabled project uses it.

## Product Boundaries & Selection Guidance

- If you only need review output for one repository in a local or CI workflow, a CLI/engine such as open-code-review is usually lighter;
- If you prefer a vendor-managed service with a broader commercial integration catalog, SLA, or enterprise services, a commercial SaaS may fit better;
- If the central requirement is **open-source self-hosting + Chinese IM + project-level authorization + remediation governance + runtime and quality insights**, this project packages those concerns into one governance platform;
- open-code-review is one optional execution engine. ACR adds trusted events, configuration snapshots, compensating delivery, issue governance, authorization, and operational data around the engine.

## Current Stage and Roadmap

| Stage | Current status |
|---|---|
| V0.1 MVP | Project onboarding, trusted webhooks, dual execution modes, summary write-back, IM notifications, a basic issue loop, and workbench are delivered; GitHub passed a real-repository end-to-end acceptance run |
| V0.2 Core | Under controlled rollout; delivered capabilities include the complete issue lifecycle and auto-assignment, push review, inline comments, data insights and token usage, project-level authorization, persistent dispatch/recovery, resource budgets, and runtime overview |
| V1.0 enterprise acceptance | Not complete: enterprise identity or a complete account lifecycle, complete sensitive-object business audit and export, data retention and backup/recovery exercises, immutable reports/subscriptions, price versions and quotas, and real-instance acceptance for every provider planned for production |
| Later candidates | Quality-gate shadow evaluation, evidence-preserving insight drill-down and tabular exports, and model/template publishing and rollback governance. Each requires separate scope and acceptance and is not a current capability commitment |

See the [product roadmap](docs/planning/product-roadmap.md) for the full business boundary, dependencies, and acceptance criteria, and [production-readiness governance](docs/planning/production-readiness-governance.md) for the enterprise acceptance truth table.

## Documentation

| Document | What it covers |
|---|---|
| [Deployment](docs/deployment.md) | Docker Compose trial, manual deployment, production configuration, and upgrades |
| [Product roadmap](docs/planning/product-roadmap.md) | Positioning, non-goals, capability truth, and milestone acceptance |
| [Architecture](docs/planning/architecture-scaffold.md) | Module ownership, dependency direction, and workflow boundaries |
| [SQL guide](sql/README.md) | Fresh initialization, existing-database upgrades, and script inventory |
| [CHANGELOG](CHANGELOG.md) | Delivered vertical slices and verification records |
| [Contributing](CONTRIBUTING.md) / [Security policy](SECURITY.md) | How to contribute and report a security issue |

Reference projects: [PR-Agent](https://github.com/qodo-ai/pr-agent) for provider and PR-assistant capabilities, [AI-Codereview-Gitlab](https://github.com/sunmh207/AI-Codereview-Gitlab) for Chinese IM delivery, and [open-code-review](https://github.com/alibaba/open-code-review) as an optional review engine. They inform capability validation and technical trade-offs; this does not imply feature parity.

## Contributing

- Use Conventional Commits (`feat:`, `fix:`, `refactor:`, `docs:`) and name feature branches `feature/<name>`;
- Before committing code, pass `mvn test` and `cd acr-ui && npm run build:prod`;
- Define scope, non-scope, and executable acceptance criteria before starting a business slice.

## License

This project is open-sourced under the [Apache License 2.0](LICENSE); third-party components remain under their original licenses.
