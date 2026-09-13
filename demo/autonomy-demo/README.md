# FactoryFone autonomy demo: signal → spec → cloud implementation → risk-routed review → closed loop

End-to-end demo of Factory running the delivery loop for a telecom "market rule" on the
FactoryFone Admin Portal. Three scheduled automations run on a Droid Computer; two CI
artifacts do review routing; the rest is live.

Everything here is isolated from the shared Factory-Academy repo:

| Thing            | Where                                                                                                             |
| ---------------- | ----------------------------------------------------------------------------------------------------------------- |
| Repo (this copy) | `factory-calvin/telecom-autonomy-demo` (`origin`). `upstream` = shared repo, never pushed to.                     |
| Droid Computer   | `telecom-automations-droid-computer` (e2b), checkout at `/home/factory-user/repos/Telecom-Demo-NextJS-Springboot` |
| Linear team      | **FactoryFone Autonomy** (`FFA`), labels `agent-ready`, `needs-human`, `from-prod-signal`, `agent-instructions`   |
| Slack            | `#demo-req-channel` (requirement signal + progress), `#demo-alerts-channel` (production signal)                   |
| Notion           | page `Calvin_DemoDoc` (shared with the Factory integration)                                                       |
| Automation state | `/home/factory-user/automation-state/{spec-writer,issue-implementer,loop-closer}/` on the computer                |

## The automations (Factory → Automations)

| #   | Name                          | Cadence      | Watches                                                                                        | Produces                                                                                                                                                                                                       |
| --- | ----------------------------- | ------------ | ---------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | **FFA 1 · Spec Writer**       | every 10 min | Notion page containing `Status: Ready for Spec`; new top-level messages in `#demo-req-channel` | RFC PR (`docs/docs/rfcs/…`), parent + child Linear issues with acceptance criteria, `agent-ready` → Todo / `needs-human` → Backlog, write-back to Notion/Slack                                                 |
| 2   | **FFA 2 · Issue Implementer** | every 10 min | FFA issues in Todo labeled `agent-ready`                                                       | Claims one issue, posts an agent-readiness report, implements on the Droid Computer per AGENTS.md, runs `pnpm lint` + tests, opens PR with risk self-assessment and "Lessons for AGENTS.md", issue → In Review |
| 3   | **FFA 3 · Loop Closer**       | every 15 min | Merged PRs; alerts in `#demo-alerts-channel`; lessons in PR bodies                             | Docs PR for merged changes, Linear → Done, `Prod: …` issue from each alert (routed agent-ready/needs-human), one `agents:` PR proposing an AGENTS.md change for human approval                                 |

Review routing is event-driven, not scheduled:

| Artifact                                                             | What it does                                                                                                                                                                                                                                                                                                                    |
| -------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `.github/workflows/droid-review.yml`                                 | Existing Droid Auto Review (inline findings, approval). Picks up `.factory/skills/review-guidelines/SKILL.md` automatically.                                                                                                                                                                                                    |
| `.github/workflows/droid-risk-router.yml` → `scripts/risk-router.sh` | Read-only `droid exec` scores the PR 0–100; deterministic path rules add floors (CI config 70, schema/deps 50, API 40, agent instructions 40) and a docs-only cap (20). Labels `risk:*` plus `auto-merge-eligible` or `needs-human-review`; high risk fails the check. Comment shows the trailing 30-day **human-review rate**. |
| `Jenkinsfile`                                                        | Same script in a plain `sh` step of a Multibranch Pipeline (`CHANGE_ID` → `PR_NUMBER`). Proves headless Droid is just a CLI.                                                                                                                                                                                                    |

Thresholds are repo variables: `RISK_THRESHOLD_LOW` (30), `RISK_THRESHOLD_HIGH` (70), `RISK_AUTO_MERGE` (`false`). Lowering the thresholds over time is the trust curve. Two things never auto-merge regardless of score: changes to the agent's instructions (`AGENTS.md`, `.factory/`) and changes to the review pipeline itself (workflows, `Jenkinsfile`, the router script and prompt). Those files are also excluded from the docs-only cap.

## One-time setup checklist

- [ ] Repo secret `FACTORY_API_KEY` on `factory-calvin/telecom-autonomy-demo` (Settings → Secrets → Actions).
- [ ] Factory Droid GitHub App installed on the repo (needed by `droid-review.yml`; the router only needs `github.token`).
- [ ] Optional repo variables `RISK_THRESHOLD_LOW/HIGH`, `RISK_AUTO_MERGE`. For a merge-on-stage moment set `RISK_AUTO_MERGE=true` and enable "Allow auto-merge" in repo settings.
- [ ] Branch protection on `main` requiring the `Droid Risk Router / route` check, so high-risk PRs are actually blocked.
- [ ] Disable inherited demo workflows that belong to another storyline: `gh workflow disable "Nightly Triage"` and `gh workflow disable "CI Auto-Triage"` (they expect their own secrets and will fail noisily otherwise).
- [ ] Optional: `/install-wiki` in the repo to add the AutoWiki refresh action (auto-docs beat).
- [ ] Run `/readiness-report` once in this repo from the Factory App or CLI so the readiness dashboard has a baseline.
- [ ] Automations: leave **active** until the dry run passes, then **pause** until show time so the demo starts from a clean state.
- [ ] The router workflow, `scripts/risk-router.sh`, and `demo/autonomy-demo/risk-router-prompt.md` must be on `main` **before** the show. The job runs under `pull_request_target` (workflow file from the base branch), checks out the base branch, and only fetches the PR head into a ref (`HEAD_REF=pr/head`), so it works for any PR regardless of where it branched, and PR-controlled code is never executed with `FACTORY_API_KEY` or the write token in scope. Consequence: a change to the router itself only takes effect after it merges.

## How the pieces hand off

- **Dependencies.** The Spec Writer puts an `agent-ready` issue that consumes another issue's output (frontend reading a new backend field) in **Backlog** with the line `Depends on: FFA-<n>` in its description. When the dependency's PR merges, the Loop Closer moves it to **Todo** with a `🔓 Unblocked by …` comment, and the Implementer picks it up. The Implementer only implements against what is on `origin/main`; In Review is not enough.
- **Shared checkout.** All three automations use `/home/factory-user/repos/Telecom-Demo-NextJS-Springboot`. They take `/home/factory-user/automation-state/checkout.lock` (stale after 45 min) so an overlapping run skips with "Another automation run holds the checkout" instead of touching in-flight work. When you work on this computer yourself, use a separate `git worktree add /home/factory-user/repos/wt-<name> <branch>` rather than that checkout, or an Implementer run may start while you have uncommitted changes (it will skip, but you lose the tick).
- **Java on the Droid Computer.** JDK 25 has no readable default `cacerts`; the Implementer runs Gradle with `JAVA_TOOL_OPTIONS=-Djavax.net.ssl.trustStore=/tmp/droid-java-cacerts …` built from the system CA bundle. Nothing in the repo changes for this.

## Dry run (do this before Monday)

1. Put the requirement in Notion (`Calvin_DemoDoc`): a heading, the rule, effective date, affected plans/customers, and a final line `Status: Ready for Spec`. Or post it as one message in `#demo-req-channel`.
2. Within 10 minutes the Spec Writer opens `RFC: …` and creates FFA issues. Check the Notion callout / Slack thread reply.
3. Within 10 more minutes the Issue Implementer claims the first agent-ready issue, posts the readiness comment, and opens a PR. Watch the run in Factory → Automations → FFA 2 (that view _is_ the cloud runtime).
4. The PR gets Droid Auto Review + the risk router comment/labels.
5. Merge a low/medium PR. Within 15 minutes the Loop Closer opens a docs PR and marks the issue Done.
6. Post an alert in `#demo-alerts-channel` (e.g. `ALERT prod: /api/dashboard/stats 500 after deploy of <sha>; 12 admin sessions affected`). The Loop Closer opens `Prod: …` in FFA and replies in-thread.
7. After a couple of PRs carry "Lessons for AGENTS.md", the Loop Closer opens an `agents:` PR labeled `agent-instructions`.

If anything did not fire, open the automation's latest run session; the run summary says exactly which tool call failed.

## State as staged for Monday (after the dry run)

The dry run completed the full loop once, so the show does not start from zero; it starts from a repo that already carries the first market rule.

- **Already on `main`:** RFC `docs/docs/rfcs/2026-09-12-data-overage-protection.md` (PR #2), FFA-3 usage metrics (PR #4), FFA-8 dashboard index (PR #7), FFA-6 usage-risk UI (PR #8), the router and its fixes (PRs #1, #6, #9), docs PRs (#5, #10), and the `agents:` proposal (PR #3, `AGENTS.md` CI Security section).
- **Linear FFA:** FFA-3, FFA-6, FFA-7, FFA-8 Done. FFA-1, FFA-2, FFA-4, FFA-5 stay in Backlog as `needs-human` (they are the open decisions from the RFC; leave them, they are part of the story).
- **Notion:** `Calvin_DemoDoc` (MR-2026-14) carries its `Droid Spec:` callout. A second page, **"Market rule MR-2026-19: Support Response-Time Transparency"**, sits underneath it with `Status: Draft`. That page is Monday's signal.
- **Automations:** paused. `RISK_AUTO_MERGE=true` and "Allow auto-merge" are on, so `low` PRs merge themselves; `medium`/`high` wait for a human.
- **Ruleset on `main`:** requires the `route` check; admins bypass. Leave "Require an additional approval for unattributed Copilot pull requests" as it is; it does not affect the flow.

### Start-of-show checklist (10 minutes before)

1. Factory → Automations: **resume** FFA 1, FFA 2, FFA 3. Confirm `ls /home/factory-user/automation-state/checkout.lock` fails (no stale lock) and the shared checkout is clean on `main`.
2. `gh pr list` in the repo shows nothing open. `gh workflow view "Droid Risk Router"` is enabled.
3. Open in tabs: the MR-2026-19 Notion page, Linear FFA board, GitHub PR list, Factory Automations (FFA 1 run list), `#demo-req-channel`, `#demo-alerts-channel`.
4. Beat 1: change the first line of the MR-2026-19 page from `Status: Draft` to `Status: Ready for Spec`. The Spec Writer ticks every 10 minutes (`*/10`), so the RFC PR lands within 10 minutes; the Implementer's first PR within roughly 20 to 30 minutes after that. Fill that time with beats 3 and 5 (readiness, Jenkinsfile), which need no new state.
5. Beat 6: when a `medium`/`high` PR arrives, review it on stage and merge with `gh pr merge <n> --squash` (add `--admin` if the ruleset blocks; `route` is required and passes for medium).
6. Beat 7: post in `#demo-alerts-channel`, e.g. `ALERT prod: GET /api/tickets p95 latency 4.1s since deploy of <sha>; Care queue page timing out`. The Loop Closer ticks every 15 minutes (`*/15`).

If you want to rehearse again before Monday, follow "Reset between runs" and re-stage the MR-2026-19 page to `Status: Draft` (delete its `Droid Spec:` callout).

## Show flow (maps to the customer's beats)

1. **Signal in.** Show the Notion doc (or Slack thread). Flip the last line to `Status: Ready for Spec`.
2. **Spec.** Open FFA 1's run. Point at the business context section of the RFC (regulator, deadline, affected plans, exposure) and the `needs-human` issues it refused to guess on. Show the issue slicing table.
3. **Context, head-on.** Run `/readiness-report` live (or show the dashboard). Then open `AGENTS.md`, `.factory/skills/`, `package.json` scripts, `.github/workflows/`. Say it plainly: the agent's confidence comes from the repo's own validation loops (lint, Vitest, JUnit, Playwright, QA skill, Droid review) plus carried instructions, not from grep. FFA 2's readiness comment on the issue repeats this per-run.
4. **Implementation in the cloud.** Open FFA 2's run session while it works. The claim comment names the Droid Computer and host. No laptop involved.
5. **Jenkins.** Open `Jenkinsfile`, then the identical `scripts/risk-router.sh`. If a Jenkins is handy, trigger the PR build; otherwise show the GitHub Actions run of "Droid Risk Router" and note the step is `sh ./scripts/risk-router.sh`.
6. **Review with risk-based routing.** On the PR: Droid Auto Review findings, then the router comment. Read the deterministic rules that fired, the agent's reasons, the score, the route label, and the **human-review rate**. Tie it to the metric: today 100% of PRs are human-reviewed; the thresholds are the dial, the review findings are the evidence for turning it.
7. **Close the loop.** Merge. Show the Loop Closer's docs PR and the Linear issue moving to Done. Post the alert; show the new `Prod:` issue and its routing. Finish on the `agents:` PR: the agent proposing a change to its own instructions, with evidence links, waiting for a human.

## Reset between runs

- Linear: cancel or archive FFA issues you do not want carried over.
- GitHub: close/merge PRs; delete `ffa/*`, `rfc/*`, `docs/*`, `agents/*` branches.
- Notion: remove the `Droid Spec:` callout and reset the status line.
- Computer: `rm -rf /home/factory-user/automation-state/*` to forget processed signals (the automations recreate it). Do this while no run is in flight (`ls /home/factory-user/automation-state/checkout.lock` should fail).
