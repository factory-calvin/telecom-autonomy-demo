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

| #   | Name                          | Cadence   | Watches                                                                                        | Produces                                                                                                                                                                                                       |
| --- | ----------------------------- | --------- | ---------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | **FFA 1 · Spec Writer**       | `*/10`    | Notion page containing `Status: Ready for Spec`; new top-level messages in `#demo-req-channel` | RFC PR (`docs/docs/rfcs/…`), parent + child Linear issues with acceptance criteria, `agent-ready` → Todo / `needs-human` → Backlog, write-back to Notion/Slack                                                 |
| 2   | **FFA 2 · Issue Implementer** | `5-59/10` | FFA issues in Todo labeled `agent-ready`                                                       | Claims one issue, posts an agent-readiness report, implements on the Droid Computer per AGENTS.md, runs `pnpm lint` + tests, opens PR with risk self-assessment and "Lessons for AGENTS.md", issue → In Review |
| 3   | **FFA 3 · Loop Closer**       | `7-59/15` | Merged PRs; alerts in `#demo-alerts-channel`; lessons in PR bodies                             | Docs PR for merged changes, Linear → Done, `Prod: …` issue from each alert (routed agent-ready/needs-human), one `agents:` PR proposing an AGENTS.md change for human approval                                 |

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

## Guided artifact tour (the show, no live runs)

The loop has run end to end twice. The second run (market rule **MR-2026-19, Support
Response-Time Transparency**) was executed on 2026-09-14 between 02:44 and 06:00 UTC with
the automations live and one human (the reviewer) in the loop. Every artifact it produced
is still where the automation left it. The show walks those artifacts in the order they
were created; nothing needs to run, and the automations stay **paused**.

Automation run sessions live in Factory → Automations → _automation_ → Runs; times below
are UTC. Everything else is a direct link.

### Beat 1 · Signal in

1. Notion: [Market rule MR-2026-19](https://app.notion.com/p/Market-rule-MR-2026-19-Support-Response-Time-Transparency-3da2517972c7810a9a54fff30cd144d6) (child of `Calvin_DemoDoc`). A regulator memo written by a PM: five requirements, two open decisions, and the last line `Status: Ready for Spec`. That line was the only trigger; it was flipped from `Status: Draft` at 02:44.
2. Same page, bottom: the `Droid Spec:` callout the Spec Writer wrote back, linking the RFC PR and the Linear parent. This is how the PM learns their doc became work.

### Beat 2 · Spec

3. Factory → Automations → **FFA 1 · Spec Writer**, run at **03:20**. The session shows the agent reading the page, the repo, and AGENTS.md before writing a word.
4. [PR #14 `RFC: Support Response-Time Transparency`](https://github.com/factory-calvin/telecom-autonomy-demo/pull/14) (merged `8530f05`). Read the business-context section (regulator, deadline, exposure) and the issue-slicing table. Router: `risk:low` (score 10) → auto-merged; docs only.
5. Linear parent [FFA-10](https://linear.app/factoryai/issue/FFA-10) with children:
   - [FFA-11](https://linear.app/factoryai/issue/FFA-11), [FFA-12](https://linear.app/factoryai/issue/FFA-12), [FFA-13](https://linear.app/factoryai/issue/FFA-13): `needs-human`, created in **Backlog**. The agent refused to guess on clock start, SLA tiers, and escalation mechanics; each comment thread carries the human's decision that unblocked the work.
   - [FFA-14](https://linear.app/factoryai/issue/FFA-14) backend, `agent-ready`, `Depends on: FFA-11/12/13`.
   - [FFA-15](https://linear.app/factoryai/issue/FFA-15) frontend, `agent-ready`, `Depends on: FFA-14`, so it waited in Backlog until the backend merged.

### Beat 3 · Context, head-on

6. FFA-14, first comment: the Implementer's **agent-readiness report**. It names the validation loops it will rely on (lint, JUnit, Vitest, Playwright, Droid review) and the instructions it carries (`AGENTS.md`, `.factory/skills/`). Then open `AGENTS.md` and `.factory/skills/` on `main` and say it plainly: confidence comes from the repo's own feedback loops, not from grep.
7. Optional: `/readiness-report` output from the one-time setup, or the readiness dashboard.

### Beat 4 · Implementation in the cloud

8. Factory → Automations → **FFA 2 · Issue Implementer**, run at **03:35**. The claim comment on FFA-14 names the Droid Computer and host. No laptop.
9. [PR #15 `feat(tickets): FFA-14 add auditable ticket deadline reporting`](https://github.com/factory-calvin/telecom-autonomy-demo/pull/15) (merged `3c11017`, +912/−43, 17 files): `TicketDeadlineService`, a 15-minute `@Scheduled` reconciliation job, schema initializer, seeder, tests, docs. The PR body carries the risk self-assessment and "Lessons for AGENTS.md".

### Beat 5 · Jenkins

10. [`Jenkinsfile`](https://github.com/factory-calvin/telecom-autonomy-demo/blob/main/Jenkinsfile) next to [`scripts/risk-router.sh`](https://github.com/factory-calvin/telecom-autonomy-demo/blob/main/scripts/risk-router.sh): the same script runs in a plain `sh` step. Then the GitHub Actions run "Droid Risk Router" on PR #15: the step is `sh ./scripts/risk-router.sh`. Headless Droid is a CLI.

### Beat 6 · Review with risk-based routing

11. PR #15 conversation, in order:
    - Droid Auto Review inline finding, **P1**: a resolved ticket projected "as of" a time before its own resolution was evaluated at `resolvedAt`, not `asOf`. Real bug, caught before a human looked.
    - Router comment: deterministic floors that fired (schema, API), the agent's reasons, **score 74 → `risk:high` → `needs-human-review`**, the `route` check failing on purpose, and the trailing **human-review rate**.
    - Reviewer's fix commit (`evaluationTime()` = `min(resolvedAt, asOf)` plus a regression test) and the reply on the review thread.
12. [PR #16 `style(tickets): wrap comment to satisfy spotless`](https://github.com/factory-calvin/telecom-autonomy-demo/pull/16) (score 3, auto-merged). Honesty exhibit: the reviewer admin-merged #15 while Backend CI was still red on a formatting check. The pipeline noticed; the human did not. Say so.
13. [PR #18 `feat(tickets): FFA-15 show ticket age and SLA deadlines`](https://github.com/factory-calvin/telecom-autonomy-demo/pull/18) (merged `a8104a3`, +513/−73): frontend half, **score 42 → `risk:medium`**, Droid review found no P0/P1, human reviewed and squash-merged. Contrast with #15: same author, different route.

### Beat 7 · Close the loop

14. Factory → Automations → **FFA 3 · Loop Closer**, run at **04:10**: processed PRs 13–16, opened [PR #17 docs](https://github.com/factory-calvin/telecom-autonomy-demo/pull/17) (score 6, auto-merged), moved FFA-14 → Done, and moved FFA-15 Backlog → Todo with a `🔓 Unblocked by #15` comment. That handoff is what let the Implementer pick up the frontend work in the next tick.
15. Production signal: [alert thread in `#demo-alerts-channel`](https://factory-ai.slack.com/archives/C0C15NZ9C83/p1789358995346009) at 04:09: `GET /api/tickets?deadlineState=RESOLUTION_OVERDUE p95 3.9s since deploy of 3c11017`. The Loop Closer's in-thread reply links the issue it opened.
16. [FFA-16 `Prod: resolution-overdue ticket filter latency`](https://linear.app/factoryai/issue/FFA-16): priority 1, `from-prod-signal`, `agent-ready`. Read the **Hypothesis**: it names the exact method in PR #15 (unpaged `findFiltered` + per-row `project()`) as the likely regression. Signal → root-cause hypothesis → acceptance criteria with a p95 target, without a human.
17. [PR #19 `feat(tickets): FFA-16 optimize overdue ticket filtering`](https://github.com/factory-calvin/telecom-autonomy-demo/pull/19) (merged `d64809c`): native SQL CTE that filters and pages in the database. **Score 68 → `risk:medium`**. Droid review **P1**: `ORDER BY` on a mixed-type SQLite column would break pagination; reviewer fixed it (`cc7c88f`), the integration test that compares the native query to the JPQL reference caught a tie-break difference on the first attempt, and the PR merged only after every check was green.
18. Finish on [PR #12 `chore(agents): document Droid Java trust store`](https://github.com/factory-calvin/telecom-autonomy-demo/pull/12) with [FFA-9](https://linear.app/factoryai/issue/FFA-9): the agent proposing a change to its own instructions after two runs lost time to the same environment quirk, labeled `agent-instructions`, `risk:medium`, `needs-human-review`. Instruction and pipeline files never auto-merge regardless of score (PR #9). It is left open on purpose. Do not merge it before the show.






### Operational lessons from the two runs

- **Lock contention.** All three automations ran on the same minute; the Implementer took the checkout lock before checking Linear, so the Spec Writer skipped twice. Fix: staggered schedules (Spec Writer `*/10`, Implementer `5-59/10`, Loop Closer `7-59/15`) and the Implementer now queries Linear first and only takes the lock when it has work.
- **Slack connector.** `get_conversation_history` returns nothing when `hours_ago`/`days_ago` is set. The Loop Closer read the channel with a time filter and missed the first alert for one tick. Prompts now read unfiltered and filter by `ts` against state.
- **Human error is the risk the router models.** PR #15 was admin-merged with a red Backend check (`spotlessJavaCheck` on a hand-edited comment). PR #16 fixed it two minutes later. The `route` check is the only required check; everything else relies on the reviewer reading the checks.
- **Scorer variance.** The LLM score moves a few points between runs of the same PR (#15 scored 78 then 74; #19 scored 64 then 68). The deterministic floors and the never-auto-merge file rules are what make routing predictable; the score is the tie-breaker within a band.
- **Self-merge incident (first run).** PR #3, the first `agents:` proposal, auto-merged because the docs-only cap applied to `AGENTS.md`. PR #6 and PR #9 fixed the router; PR #12 shows the corrected behaviour. Keep the `AGENTS.md` CI Security section from PR #3; it is correct and harmless.

### State as staged

- **Automations:** paused. Resume only if you want to run a third loop; see "Reset between runs".
- **Open PRs:** exactly one, #12. Shared checkout clean on `main`, no `checkout.lock`.
- **Linear FFA:** FFA-3, 6, 7, 8, 11, 12, 13, 14, 15, 16 Done; FFA-9 open (PR #12); FFA-1, 2, 4, 5 in Backlog as the first RFC's `needs-human` decisions; FFA-10 parent.
- **Notion:** both market-rule pages carry their `Droid Spec:` callout. A tour page with the same links as this section sits under `Calvin_DemoDoc`.
- `RISK_AUTO_MERGE=true`, "Allow auto-merge" on, ruleset on `main` requires `route`, admins bypass.

## Reset between runs

- Linear: cancel or archive FFA issues you do not want carried over.
- GitHub: close/merge PRs; delete `ffa/*`, `rfc/*`, `docs/*`, `agents/*` branches.
- Notion: remove the `Droid Spec:` callout and reset the status line.
- Computer: `rm -rf /home/factory-user/automation-state/*` to forget processed signals (the automations recreate it). Do this while no run is in flight (`ls /home/factory-user/automation-state/checkout.lock` should fail).
