---
name: qa
description: >
  Run QA tests for the FactoryFone Admin Portal (Next.js frontend + Spring Boot backend).
  Analyzes git diff to determine affected apps, runs only the relevant flows from
  qa-web and qa-backend sub-skills, captures evidence, and generates a structured
  PR-ready report. Use when the user says "run QA", "smoke test the app",
  "verify before I merge", or when CI invokes it on a PR.
---

# QA Orchestrator

**SCOPE: This skill performs manual/functional QA only. It verifies the running application by interacting with it as a real user (browser navigation, API calls). It does NOT run lint, typecheck, vitest, gradle test, or any static analysis. Those are handled by the separate `ci.yml` workflow.**

## Step 1: Load configuration

Read `.factory/skills/qa/config.yaml`. It is the single source of truth for environments, apps, personas, integrations, cleanup, and failure-learning policy. Do not hardcode URLs or paths in this skill.

## Step 2: Determine target environment

Use `default_target: local` from the config unless the caller passes a different env. This project has no preview deployments and no remote QA environments. Authentication is disabled, so no credentials are needed.

If invoked in CI (`CI=true`), the workflow is responsible for starting the dev servers; the orchestrator just polls `ready_checks` until they respond.

## Step 3: Analyze git diff

Run `git diff --name-only origin/main...HEAD` (or `git diff --name-only HEAD~1` if no upstream available) to determine which apps are affected.

Map changed files to apps using `apps.<id>.path_patterns` in config:

- Files matching `web` patterns → run `qa-web` sub-skill.
- Files matching `backend` patterns → run `qa-backend` sub-skill.
- Files matching neither (e.g., `.factory/skills/**`, `docs/**`, `.github/**`, root configs) → that app is NOT affected.

If NO app is affected, report the run as INCONCLUSIVE: "No app code changed, QA not applicable for this diff." Skip Steps 4-7 and write the report.

When invoked manually with no args (e.g., user says "run QA"), default to running ALL apps as a full smoke pass.

## Step 4: Pre-flight checks (per affected app)

For each affected app:

- `web`: ensure the frontend is reachable at `environments.local.url`. If not and `auto_start: true`, the orchestrator (or CI) starts it via `pnpm dev:frontend`. Poll `http://localhost:3000` until 200 or 120s timeout.
- `backend`: ensure `http://localhost:8080/api/health` returns 200. If not, start it via `pnpm dev:backend` and poll for up to 120s.

Both web and backend share the same SQLite database. If `cleanup.run_before_each_qa_session: true`, run `pnpm reset:db` BEFORE starting servers (or before the first poll if servers already up). Stream output to `qa-results/logs/reset-db.log`.

If a pre-flight check fails for an affected app, mark all of that app's flows BLOCKED with the exact error and remediation, and continue with the other apps.

## Step 5: Execute diff-relevant flows only

For each affected app, read `.factory/skills/qa-<app>/SKILL.md`. Each sub-skill exposes a MENU of test flows. You must:

1. Read the diff carefully and identify which flows directly verify the change.
2. Run those flows PLUS any adjacent flows that confirm integration (e.g., a backend endpoint change should also trigger the web page that calls it).
3. Do NOT run completely unrelated flows. If only `app/customers/page.tsx` changed, do NOT exercise `/plans`, `/devices`, `/usage`, or `/tickets`.
4. If no existing flow covers the change, write an ad-hoc test that directly verifies the changed behavior.
5. Always include at least one negative or boundary test for the change (e.g., bad ID returns 404, empty state renders).

If invoked as a full smoke pass (no diff context), run every flow in every affected sub-skill.

## Step 6: Evidence capture

For web flows: use the `agent-browser` skill. Capture an accessibility-tree snapshot and a full-page screenshot per page tested. Save screenshots to `qa-results/screenshots/web-<slug>.png`. Embed the snapshot text inline in the report; reference screenshot filenames (do NOT use `![image](url)` — those URLs will not resolve in PR comments).

For backend flows: save raw response bodies to `qa-results/logs/backend-<METHOD>-<slug>.json` (truncate to 64KB). Quote relevant JSON keys/values inline in the report. Do not paste full bodies.

If `imagemagick: true`, the workflow can generate animated GIF diffs of before/after screenshots when comparing two runs. The orchestrator does not invoke ImageMagick directly during a single run.

## Step 7: Test quality gate

Before writing the report, verify your test plan meets these requirements:

1. CHANGE-SPECIFIC FIRST. At least half of executed flows must directly verify the diff.
2. INTEGRATION FLOWS ARE VALID. Tests that confirm the change does not break adjacent features (sidebar nav still works, dashboard still loads) are good.
3. NO UNRELATED FLOWS.
4. NO AUTOMATED TEST SUITES. Do NOT run `pnpm test`, `pnpm test:e2e`, or `gradle test` from this skill.
5. NEGATIVE TESTS. At least one error / boundary test for the changed behavior.
6. INTERACTIVE TESTING. Drive the browser; do not just curl HTML.
7. INCONCLUSIVE IF UNSURE. If you cannot articulate what the diff changes, mark INCONCLUSIVE rather than PASS.

## Step 8: Handle failures

Never silently skip a flow. If a flow cannot complete:

- Mark it BLOCKED with what was tried and how to fix it.
- Continue to the next flow. Never abort the whole run for one failure.

## Step 9: Generate report

Write the report to `qa-results/report.md` using `.factory/skills/qa/REPORT-TEMPLATE.md`.

Required structure:

- Start with `## QA Report` heading followed by the test results table.
- Result column uses emojis: `:white_check_mark:` PASS, `:x:` FAIL, `:no_entry:` BLOCKED, `:warning:` FLAKY, `:grey_question:` INCONCLUSIVE.
- Keep it concise. Table + (optional) "Action Required" section + collapsed `<details>` block with evidence is the entire report.
- Do NOT include "Behavioral Change Summary" prose, "Info" metadata tables, or verbose explanations. The reviewer can read the diff.
- Do NOT report setup steps (server start, DB reset) as test rows. Only report rows for actual user-facing behavior.
- All evidence (snapshots, response excerpts, screenshot filenames) goes inside one collapsed `<details>` block.

Print the absolute path to the report and a final output contract:

```
QA_RUN_ID=<run_id>
QA_REPORT_PATH=<absolute path to qa-results/report.md>
QA_STATUS=<pass|fail|partial|inconclusive>
QA_PASS=<n>
QA_FAIL=<n>
QA_BLOCKED=<n>
```

## Step 10: Suggest skill updates (failure learning)

`failure_learning: suggest_in_report` — if any BLOCKED or FAIL revealed a NEW environment insight that would help future runs (locale quirk, timing issue, missing env var, undocumented state), append a "Suggested Skill Updates" section at the end of the report.

Format as a table:

| #   | Severity        | File     | Issue               | Fix Prompt                                                                           |
| --- | --------------- | -------- | ------------------- | ------------------------------------------------------------------------------------ |
| 1   | <emoji> <level> | `<file>` | <short description> | <details><summary>Copy</summary><br>`<full droid prompt to fix the issue>`</details> |

Severity levels:

- `:red_circle: Breaking` — causes test failures every run
- `:yellow_circle: Degraded` — intermittent or suboptimal behavior
- `:large_blue_circle: Info` — useful new knowledge, no failure caused

Do NOT suggest updates for:

- Bad CSS selectors or skill typos (just fix them directly)
- Expected behavior changes from the PR (already in the diff)
- Anything already in the sub-skill's "Known Failure Modes" section

If no genuine new insights, omit the section. Do NOT write `qa-results/skill-updates.json` — the failure-learning mode is `suggest_in_report`, so the user copy-pastes manually.

## Step 11: Cleanup

If the orchestrator started servers in Step 4, stop them. Leave the SQLite DB and run directory in place.

## Hard rules

1. Always read `config.yaml` first.
2. Never silently skip a flow. BLOCKED is a valid result; SKIP is not.
3. Never edit production data. The only mutation is `pnpm reset:db` against local SQLite.
4. The final report must follow `REPORT-TEMPLATE.md`.

## Token discipline

Orchestrator chat messages should be terse status lines: `[step 3] diff matches web only`, `[step 4] backend healthy`, `[step 5] running 4 flows`, etc. Do not paste full HTTP bodies, screenshots, or sub-skill internals into chat.
