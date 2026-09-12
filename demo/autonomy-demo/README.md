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

Thresholds are repo variables: `RISK_THRESHOLD_LOW` (30), `RISK_THRESHOLD_HIGH` (70), `RISK_AUTO_MERGE` (`false`). Lowering the thresholds over time is the trust curve.

## One-time setup checklist

- [ ] Repo secret `FACTORY_API_KEY` on `factory-calvin/telecom-autonomy-demo` (Settings → Secrets → Actions).
- [ ] Factory Droid GitHub App installed on the repo (needed by `droid-review.yml`; the router only needs `github.token`).
- [ ] Optional repo variables `RISK_THRESHOLD_LOW/HIGH`, `RISK_AUTO_MERGE`. For a merge-on-stage moment set `RISK_AUTO_MERGE=true` and enable "Allow auto-merge" in repo settings.
- [ ] Branch protection on `main` requiring the `Droid Risk Router / route` check, so high-risk PRs are actually blocked.
- [ ] Disable inherited demo workflows that belong to another storyline: `gh workflow disable "Nightly Triage"` and `gh workflow disable "CI Auto-Triage"` (they expect their own secrets and will fail noisily otherwise).
- [ ] Optional: `/install-wiki` in the repo to add the AutoWiki refresh action (auto-docs beat).
- [ ] Run `/readiness-report` once in this repo from the Factory App or CLI so the readiness dashboard has a baseline.
- [ ] Automations: leave **active** until the dry run passes, then **pause** until show time so the demo starts from a clean state.

## Dry run (do this before Monday)

1. Put the requirement in Notion (`Calvin_DemoDoc`): a heading, the rule, effective date, affected plans/customers, and a final line `Status: Ready for Spec`. Or post it as one message in `#demo-req-channel`.
2. Within 10 minutes the Spec Writer opens `RFC: …` and creates FFA issues. Check the Notion callout / Slack thread reply.
3. Within 10 more minutes the Issue Implementer claims the first agent-ready issue, posts the readiness comment, and opens a PR. Watch the run in Factory → Automations → FFA 2 (that view _is_ the cloud runtime).
4. The PR gets Droid Auto Review + the risk router comment/labels.
5. Merge a low/medium PR. Within 15 minutes the Loop Closer opens a docs PR and marks the issue Done.
6. Post an alert in `#demo-alerts-channel` (e.g. `ALERT prod: /api/dashboard/stats 500 after deploy of <sha>; 12 admin sessions affected`). The Loop Closer opens `Prod: …` in FFA and replies in-thread.
7. After a couple of PRs carry "Lessons for AGENTS.md", the Loop Closer opens an `agents:` PR labeled `agent-instructions`.

If anything did not fire, open the automation's latest run session; the run summary says exactly which tool call failed.

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
- Computer: `rm -rf /home/factory-user/automation-state/*` to forget processed signals (the automations recreate it).
