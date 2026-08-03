# Multi-Git Provider Access Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend the GitHub MVP closed loop to GitLab, Gitee, and Gitea via shared contracts and per-platform adapters, without refactoring the review engine, task state, scoring, IM notify, or issue ledger.

**Architecture:** Introduce `GitAccessContext` + `GitAdapterRegistry` keyed by `providerCode`; store credential `server_url` and project `repository_full_path`; make webhook/delivery/project/credential/execution resolve adapters by provider; keep platform HTTP/signing only inside `git/{github,gitlab,gitee,gitea}`.

**Tech Stack:** Java 17, Spring Boot, MyBatis, OkHttp, Vue 3 / Element Plus, MySQL incremental SQL `29_*`.

## Global Constraints

- No new Maven modules; no OAuth/App, auto webhook create, batch import, inline comments, status checks, merge gates.
- Do not rename stable columns (`pr_number` holds MR IID / PR number).
- Token / webhook secret stay encrypted, never echoed or logged.
- GitHub regression must stay green every slice.
- SQL next number is `29_*`; never edit historical scripts; utf8mb4 for Chinese scripts.
- Delivery channels: keep `GITHUB_PR_SUMMARY_COMMENT`; add `GITLAB_MR_SUMMARY_COMMENT` / `GITEE_PR_SUMMARY_COMMENT` / `GITEA_PR_SUMMARY_COMMENT`.
- Idempotency: `{provider}:{projectId}:{prNumber}:SUMMARY_COMMENT` (GitHub keys unchanged).
- Spec: `docs/superpowers/specs/2026-08-03-multi-git-provider-access-design.md`.

## File Map

**Create**
- `acr-review/.../git/GitAccessContext.java`
- `acr-review/.../git/WebhookRequestHeaders.java`
- `acr-review/.../git/GitAdapterRegistry.java`
- `acr-review/.../git/gitlab/*` (Provider, WebhookAdapter, Diff/Metadata/FileContent/Workspace/Comment)
- `acr-review/.../git/gitee/*` (same set)
- `acr-review/.../git/gitea/*` (same set)
- matching `src/test/.../git/{gitlab,gitee,gitea}/*Test.java`
- `sql/29_multi_git_provider_access.sql`
- admin: generalize `GitHubWebhookController` → `GitWebhookController` (or add mappings)

**Modify (core)**
- All `git/*` interfaces + `GitRepositoryCoordinates` + `GitPullRequestEvent` (+ workspace request)
- All `git/github/*` to use `GitAccessContext` / headers / fullPath
- `GitCredential` / mapper XML / `GitCredentialServiceImpl`
- `ReviewProject` / mapper XML (`selectByFullPath`) / `ReviewProjectServiceImpl`
- `IReviewWebhookService` + `ReviewWebhookServiceImpl` + controller + security permit
- `ReviewTaskExecutionServiceImpl`, `ReviewDeliveryServiceImpl`, `ReviewDeliveryConstants`, `ReviewSummaryContentFactory`
- Frontend: credential/project pages, `reviewDisplay.js`
- Docs: roadmap, architecture-scaffold, README, CHANGELOG, deployment, sql/README
- Design status line → implemented after done

---

### Task 1: Core contracts + GitHub migration (S0)

**Files:** create access context/headers/registry; modify all git interfaces + github impls + coordinates/event; fix compile of services temporarily via registry wrapping GitHub only if needed.

- [x] **Step 1:** Add `GitAccessContext(String token, String serverUrl)`, `WebhookRequestHeaders` (case-insensitive map view), `GitAdapterRegistry` collecting `List<>` of each interface and `require*(providerCode)`.
- [x] **Step 2:** Extend `GitRepositoryCoordinates(owner, repository, fullPath, canonicalUrl)` and `GitPullRequestEvent` with `repositoryFullPath`; update all constructors/call sites.
- [x] **Step 3:** Change interface methods `String token` → `GitAccessContext access`; webhook `verify(secret, payload, WebhookRequestHeaders)`.
- [x] **Step 4:** Migrate GitHub adapters; keep default server `https://github.com` and configurable API URL.
- [x] **Step 5:** Run `mvn -pl acr-review -am test` until GitHub tests green; fix call sites.
- [x] **Step 6:** Commit `feat: introduce GitAccessContext and adapter registry for multi-provider`

### Task 2: SQL + domain fields (S1)

- [x] **Step 1:** Write `sql/29_multi_git_provider_access.sql`: credential `server_url`; project/event `repository_full_path` backfill + unique key switch; widen owner/name to 255; dict `review_git_provider`; delivery channel rows; sys_config for gitlab/gitee/gitea event whitelists.
- [x] **Step 2:** Update domain/mapper XML for new columns; `selectByFullPath`.
- [x] **Step 3:** Update `sql/README.md`.
- [x] **Step 4:** Commit `feat: SQL and domain for multi-git provider identity`

### Task 3: Platform-agnostic services (S2)

- [x] **Step 1:** Credential/Project services accept provider; validate server_url rules; resolve registry + access context.
- [x] **Step 2:** Webhook service `handleWebhook(provider, headers, payload)`; match by fullPath; per-provider event config keys.
- [x] **Step 3:** Controller routes `/webhook/{github,gitlab,gitee,gitea}`; security anonymous permit.
- [x] **Step 4:** Execution + delivery resolve adapters by project.provider; channel + idempotency by provider; PR URL factory multi-platform.
- [x] **Step 5:** Update service unit tests; `mvn test` green for GitHub paths.
- [x] **Step 6:** Commit `feat: route webhook and delivery by providerCode`

### Task 4: GitLab adapter (S3)

- [x] Implement full gitlab package + tests (token header verify, oldrev for synchronize, Notes comments, nested path parse).
- [x] Frontend platform select + server_url + webhook help + MR label + `buildMergeRequestUrl`.
- [x] Commit `feat: add GitLab provider adapter and UI support`

### Task 5: Gitee adapter (S4)

- [x] Implement gitee package + tests (password equality and/or timestamp HMAC sign; synthetic delivery id; PR actions).
- [x] Commit `feat: add Gitee provider adapter`

### Task 6: Gitea adapter (S5)

- [x] Implement gitea package + tests (HMAC body signature; self-hosted server_url).
- [x] Commit `feat: add Gitea provider adapter`

### Task 7: Docs + verification (S6–S7)

- [x] Update roadmap, architecture-scaffold, README, CHANGELOG, deployment; design status → implemented.
- [x] `mvn test` and `cd acr-ui && npm run build:prod`.
- [x] Commit `docs: sync multi-git provider access documentation`

## Platform protocol notes (implement against)

| Platform | Verify | Delivery id | Review actions |
|---|---|---|---|
| GitHub | HMAC-SHA256 `X-Hub-Signature-256` | `X-GitHub-Delivery` | opened/reopened/synchronize |
| GitLab | Prefer signing headers if present; else constant-time `X-Gitlab-Token` == secret | `X-Gitlab-Event-UUID` or sha256 synth | open→opened, reopen→reopened, update+oldrev→synchronize |
| Gitee | Password mode: token header == secret; Sign mode: HMAC-SHA256(timestamp+"\n"+secret) base64(+urlencode) vs `X-Gitee-Token`, check `X-Gitee-Timestamp` skew ≤1h | always sha256 synth | map open/reopen/update/push_update per payload `action` |
| Gitea | HMAC-SHA256 hex of body vs `X-Gitea-Signature` (also accept `sha256=` compat) | `X-Gitea-Delivery` | opened/reopened/synchronized→synchronize |
