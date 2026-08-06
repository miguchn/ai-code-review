<div align="center">
    <h1>AI Code Review — Enterprise Code Review Governance Platform</h1>
</div>

<div align="center">
  <h3>Turns AI code review from "one-off comments" into an enterprise governance loop — 4 git platforms × dual engines × Chinese IM × issue ledger.</h3>
</div>

> **See the real thing first (30 seconds)**: [this public PR](https://github.com/miguchn/acr-demo/pull/1) deliberately keeps typical flaws such as SQL injection and hardcoded credentials. The review comment on the PR was written back automatically by a real run of this platform — not a screenshot; open it and browse it yourself.

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

## Why AI Code Review

Most AI review stops at "leave a few comments on the PR": comments get buried, nobody follows up, nobody owns them, nothing is auditable. Enterprises don't need more comments — they need a loop: **detect → reach → remediate → re-verify**, where every conclusion is traceable, actionable, and re-checkable.

| | Ordinary AI review | AI Code Review |
|---|---|---|
| Delivering conclusions | Comment and walk away; the next run buries it | One idempotent summary comment per MR/PR; re-reviews update it in place, no spam |
| Issue follow-up | Relies on memory; gone once scrolled away | Issue ledger: confirm / close / dismiss / false-positive loop, round reconciliation auto-flags rechecks |
| Reach | Developers have to come looking | Tiered notifications to DingTalk / WeCom / Feishu, to people and groups |
| Platform coverage | Often a single platform | GitHub / GitLab / Gitee / Gitea under one contract |
| Deployment & data | Code usually leaves for a SaaS | Self-hosted; credentials encrypted with AES-GCM; model endpoints allow-listed |

## Core Capabilities

| Capability | Description |
|---|---|
| 🧩 Four platforms, one contract | Unified adapter layer for GitHub / GitLab / Gitee / Gitea; repository path matches uniquely across platforms; per-platform webhooks and delivery channels |
| 🔐 Trusted webhooks | Per-platform signature verification, delivery-idempotent dedup, matching by repository path and target branch; forged or misrouted events are rejected |
| ⚙️ Dual engines | Direct LLM review or the open-code-review engine; diff scope policy (include / exclude / expand) with a frozen snapshot per run |
| 💬 Idempotent write-back | Exactly one ACR summary comment per MR/PR, updated in place on re-review; delivery is decoupled from the conclusion — a failed delivery never pollutes it |
| 📣 Tiered IM notifications | DingTalk / WeCom / Feishu, routed by conclusion and risk level |
| 📒 Issue ledger | Full issue lifecycle: confirm, close, dismiss as false positive, reopen; round trail and recheck evidence; batch disposition; workbench rollups |

## How the Loop Works

<p align="center">
  <img src="docs/images/readme-loop-diagram.png" alt="AI Code Review governance loop" width="760" />
</p>

Your git platform remains the system of record for merging; ACR owns the governance loop: MR/PR event → trusted webhook → review execution → conclusion write-back → IM notification → issue ledger → remediation push triggers re-review.

## Screenshots from a Real Run

All screenshots below are from a real environment (the IM notification card is a style sample rendered from the real run); the demo data comes from [the public demo repository above](https://github.com/miguchn/acr-demo/pull/1).

**Summary comment written back to the PR** (high-risk conclusion, top-3 issues, scope stats; full text on the public PR):

<p align="center"><img src="docs/images/readme-pr-comment.png" alt="PR summary comment write-back" width="720" /></p>

**Tiered IM notifications** (WeCom card style sample; content mirrors the real run of PR #5 above): after each run the conclusion is pushed to people and groups by risk level — key issues, recheck reminders for suspected fixes, and scope stats in one card; DingTalk / WeCom / Feishu:

<p align="center"><img src="docs/images/readme-im-notify.png" alt="Tiered IM notification card sample" width="560" /></p>

**Issue ledger** (severity badges, stage, round trail, origin; one-click filters on the summary bar):

<p align="center"><img src="docs/images/readme-issue-ledger.png" alt="Issue ledger" width="720" /></p>

**Review record detail** (four-dimension scoring with model commentary; summary-comment delivery status and external comment ID):

<p align="center"><img src="docs/images/readme-record-detail.png" alt="Review record detail" width="720" /></p>

**Workbench** (today's queue, project risk trend, task status at a glance):

<p align="center"><img src="docs/images/readme-workbench.png" alt="Workbench" width="720" /></p>

## Quick Start (One Command with Docker Compose)

Requires Docker Engine + Compose v2 and a free port 80. The first build pulls full backend and frontend dependencies and takes roughly 5–15 minutes depending on your network.

```bash
cp .env.example .env
# Edit .env and set the master key: ACR_CREDENTIAL_MASTER_KEY=$(openssl rand -base64 32)
docker compose up -d --build
```

Once all four services are healthy, open http://127.0.0.1 and sign in with the default admin `admin / admin123`. This is a trial environment, not a hardened production setup — see [deployment docs](docs/deployment.md) for production.

Connect your own repository (GitHub example, ~5 minutes):

1. Under "Project onboarding", add git credentials and create a project (repository path, target branch);
2. Run the connection test and enable the project; the platform shows the webhook URL and signing secret;
3. Configure the repository webhook to point merge-request / push events at that URL and paste the signing secret back;
4. Open an MR/PR — a minute later, check the summary comment on the PR and the issue ledger.

## How It Differs from Mainstream Tools

| | [PR-Agent](https://github.com/qodo-ai/pr-agent) | [CodeRabbit](https://www.coderabbit.ai/) | Bare [open-code-review](https://github.com/alibaba/open-code-review) | **AI Code Review** |
|---|---|---|---|---|
| Form | Open-source tool | Commercial SaaS (closed source) | Open-source engine / CLI | Open-source, self-hosted platform |
| Chinese IM reach | None | None | None | DingTalk / WeCom / Feishu |
| Issue ledger & recheck loop | None | Partial, on the SaaS side | None | Full lifecycle + round reconciliation |
| Four-platform governance | Community adapters | Yes | DIY integration | One unified contract |
| Self-hosting | DIY | Commercial enterprise tier | Local run | Ready out of the box |

open-code-review is one of the selectable engines inside this platform; AI Code Review adds the platform-side governance on top — events, credentials, write-back, notifications, and the ledger.

## Current Stage & Roadmap

**Honestly stated**: V0.1 (MVP) is delivered and has passed full-chain acceptance against a real GitHub repository; M8 issue lifecycle, M8.1 ledger visualization & batch disposition, and M9 feature assistant are delivered; the project is now in the V0.2 core iteration (review configuration with push-review types, severity-differentiated policies, inline comments and quality-gate shadow evaluation are planned). Open acceptance items: continuous-run accumulation on a real test repository, and real closed loops on GitLab / Gitee / Gitea instances.

- [Product roadmap](docs/planning/product-roadmap.md) — positioning, non-goals, and milestone acceptance criteria in full
- [Architecture notes](docs/planning/architecture-scaffold.md) / [SQL scripts](sql/README.md)

## Docs & References

| Document | Description |
|------|------|
| [Deployment](docs/deployment.md) | One-command trial via Docker Compose, manual deployment, upgrade path |
| [CHANGELOG](CHANGELOG.md) | Delivered slices |
| [Contributing](CONTRIBUTING.md) / [Security policy](SECURITY.md) | How to contribute and report security issues |

Reference projects: [PR-Agent](https://github.com/qodo-ai/pr-agent) (provider capability contract), [AI-Codereview-Gitlab](https://github.com/sunmh207/AI-Codereview-Gitlab) (lightweight IM-reach loop), [alibaba/open-code-review](https://github.com/alibaba/open-code-review) (engine candidate). References are used to verify capabilities and trade-offs, not as feature templates.

## Contributing

- Conventional Commits (`feat:`, `fix:`, `refactor:`, `docs:`), feature branches `feature/<name>`
- Code must pass compilation and unit tests before commit (`mvn test` + `cd acr-ui && npm run build:prod`)
- Scope and acceptance are defined before each business slice starts

## License

This project is open-sourced under the [Apache License 2.0](LICENSE); third-party components keep their original licenses.
