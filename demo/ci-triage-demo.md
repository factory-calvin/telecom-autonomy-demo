# CI Failure Triage Demo

A pipeline goes red. An agent reads it, decides whether the product, the test, or the
runner is at fault, and either ships a fix for review or hands it to the right person with
its reasoning. Then the same skill runs against a nightly-sized batch and gets scored on
whether it was right.

Built for the ServiceNow conversation, where the stated pain is nightly Jenkins and test
failures triaged by hand into infra versus code versus test, at roughly half a million
tests a night.

## What this demonstrates

| Their words on the call                                                 | What they will see                                          |
| ----------------------------------------------------------------------- | ----------------------------------------------------------- |
| "infra issue or a code issue… give it to product or fix it in the test" | A six-class taxonomy in a written rubric, applied live      |
| "half a million tests, hundreds or thousands of failures"               | ~110 failures collapsing into 10 root causes                |
| "I don't know how good is defined here"                                 | A measured scorecard against a known answer key             |
| "full visibility into cost, token usage"                                | Model, tokens, credits, and wall clock in every report      |
| "developers still have ownership"                                       | A stacked fix PR behind a label gate, never a silent push   |
| "would agents run in our cloud or your cloud"                           | The same skill on a runner and on a laptop, no vendor cloud |

## The moving parts

| Piece                                  | What it is                                                   |
| -------------------------------------- | ------------------------------------------------------------ |
| `demo/ci-triage-rubric.md`             | Classification and routing rules. The artifact to hand them. |
| `.factory/skills/ci-triage/`           | The skill: procedure, report template, verdict schema.       |
| `.github/workflows/ci-autotriage.yml`  | Reacts to a failed CI run, comments, then optionally fixes.  |
| `.github/workflows/nightly-triage.yml` | Batch run with a scorecard.                                  |
| `scripts/ci-triage.sh`                 | Same skill, run locally against a run id or a saved bundle.  |
| `scripts/ci-seed-failure.sh`           | Seeds one fault and opens the PR.                            |
| `scripts/ci-faults.py`                 | The six-fault catalog and its oracle.                        |
| `scripts/ci-nightly-fixture.py`        | Deterministic nightly corpus plus answer key.                |
| `scripts/ci-demo-reset.sh`             | Undo everything.                                             |

## Before the call

```bash
./scripts/ci-demo-reset.sh                    # clean slate
python3 scripts/ci-faults.py status           # expect: no faults applied
```

Repository settings that must be in place:

| Setting                   | Value | Why                                                            |
| ------------------------- | ----- | -------------------------------------------------------------- |
| secret `FACTORY_API_KEY`  | set   | droid CLI auth (already used by the QA workflow)               |
| secret `CI_AUTOFIX_TOKEN` | set   | PAT or app token. `GITHUB_TOKEN`-created PRs do not trigger CI |
| variable `CI_AUTOFIX`     | `on`  | Without it the fix job never runs and triage stays read-only   |

Both triage workflows must be merged to the default branch. `workflow_run` and `schedule`
always execute the copy of the file on `main`, so they cannot be demoed from a branch.

Rehearse once, and keep the artifacts. If Actions is slow on the call, replay offline:

```bash
./scripts/ci-triage.sh --local demo/ci-failures/saved-run
```

## Act 1 · One real bug, end to end (~6 min)

```bash
./scripts/ci-seed-failure.sh F1
gh run watch
```

F1 removes the `Status.ACTIVE` filter from two dashboard aggregates. It looks like a
harmless cleanup and it bills suspended subscribers.

What to narrate as it happens:

1. **CI goes red.** Four failures, two jobs: three backend tests and the e2e cross-check.
   Say out loud that a human triaging this opens four tabs.
2. **The triage comment appears.** Four failures, **one** cluster. It names
   `DashboardController`, states the rule that was broken, gives the exact command to
   verify, and reports its confidence.
   > This is the step Shimon described. It took about ninety seconds and it cost what the
   > footer says it cost.
3. **Labels.** `ci:product-code` and `agent-fix-ready`. Machine-readable, so anything
   downstream can route on them without parsing prose.
4. **The stacked fix PR opens** against the failing branch, with before-and-after test
   output. Point out that it targets the PR's branch, not `main`, so the original author
   keeps their history and their merge.
5. **Merge it.** CI goes green.

## Act 2 · Knowing when not to act (~3 min)

This is the part the other vendors do not show. Run both, they are fast.

```bash
./scripts/ci-seed-failure.sh F4    # order-dependent flake
./scripts/ci-seed-failure.sh F5    # unresolvable dependency pin
```

- **F4** fails in the suite and passes in isolation. The agent classifies it `flake`,
  recommends quarantine and retry, and changes **zero** lines of production code. Show the
  proof live:

  ```bash
  pnpm exec vitest run __tests__/components/plans-table-window.test.tsx
  pnpm exec vitest run __tests__/components/plans-table-window.test.tsx -t "renders the seeded plan window"
  ```

- **F5** kills the job during environment setup, so there is no JUnit XML at all. The
  agent reads the log, calls it `infra`, and routes it to the platform team. No code change.

> Three of the six seeded faults must produce no code change. That ratio is what makes the
> three that do change code worth trusting.

If there is time, `F2` is the best single fault for a skeptical engineer: a renamed API
field, failures on both sides of the interface, and the agent stopping to ask which side
is the source of truth instead of guessing.

## Act 3 · Nightly scale (~4 min)

**Do not run this live.** The full corpus took 57 minutes. Walk the banked run instead:

```bash
open demo/ci-failures/saved-nightly/score.md      # and report.md
```

If you want something moving on screen, kick off a reduced run in the background before the
call and come back to it:

```bash
gh workflow run "Nightly Triage" -f tests=300
```

Walk the report and scorecard:

- **Funnel:** 2,000 tests, 106 failures, 10 root causes. Point at c1, which merged three
  suites across two packages, and at the flake clusters where 5 of 19 retries passed.
- **Routing table:** what a person actually has to look at tomorrow morning.
- **Scorecard:** 10/10 classification, 10/10 root causes recovered, 0 clusters split or
  merged, and **0 false auto-fixes**. Lead with that last number.
- **The 6/10 routing, said out loud before they find it.** Every miss is `needs-human`
  where the key says `agent-fix-now`, so every miss costs a little human time and none
  ships a wrong change. Three of the four happen because the corpus names files this
  checkout does not contain, so nothing could be verified and the guardrail fired. The run
  diagnoses that itself in `score.md`.
- **Calibration:** the four misrouted clusters carry the four lowest confidences in the
  run. It knew where it was unsure.
- **Run cost:** model, tokens, credits, wall clock. This run: 53k in, 48k out, 1.5M
  credits, 57 minutes.

The corpus is deterministic, so the same seed reproduces the same run. That is the opening
to talk about evaluating an agent the way they evaluate a service.

## If Actions is slow or the network misbehaves

```bash
./scripts/ci-triage.sh --local demo/ci-failures/saved-run          # replay a saved bundle
pnpm test:frontend; ./scripts/ci-triage.sh --collect               # triage local test output
./scripts/ci-triage.sh --run <id> --dry-run                        # show the prompt only
```

Do not treat this as a fallback apology. It is the answer to "where do your agents run":
the same skill, the same rubric, the same verdicts, on a laptop, a self-hosted runner, or
GitHub-hosted infrastructure. Nothing in the loop depends on Factory-hosted compute.

## After the call

```bash
./scripts/ci-demo-reset.sh
```

Closes the demo PRs, deletes the `demo/ci-fault-*` and `fix/ci-triage-*` branches locally
and on the remote, reverts any applied fault, and clears triage output. Run
`--dry-run` first if you want to see the list before it acts.

## Per-person callouts

**Mayank** (wants a reusable framework, cost visibility, and a definition of "good")

- One skill, three entry points: PR triage, nightly batch, local replay. The rubric is a
  markdown file his team can fork per pipeline.
- `verdicts.json` is a schema-validated contract, so this composes with whatever they
  already run instead of replacing it.
- Every run reports its own cost. Every batch run reports its own accuracy.

**Shimon** (does this triage by hand today)

- The clustering, not the fixing, is where his time goes. Lead with the funnel.
- The restraint cases are the credibility test. Show F4 and F5 before F1 if he seems
  skeptical.
- The rubric is conservative on purpose: confidence below 0.75 forces a human.

**Harry and Jordan** (differentiation against cloud-agent vendors)

- Nothing runs in Factory's cloud. The binary runs on their runner, self-hosted or not.
- The skill reads the same `AGENTS.md` and `.factory/skills/` a developer uses locally, so
  CI behavior and laptop behavior do not drift.
- Model choice and hybrid inference stay theirs.
- The fix lands as a PR a human merges, which is the ownership line they asked about.

## Known rough edges

1. **The fix job needs `CI_AUTOFIX_TOKEN`.** With the default token the stacked PR opens
   but no CI runs on it, so the red-to-green moment never lands.
2. **Forked PRs are skipped by design.** The triage job checks out PR code and holds
   secrets. Worth saying out loud, since they will ask.
3. **Seeding needs a clean working tree.** `ci-seed-failure.sh` refuses to run otherwise.
   Use `--local` to apply a fault without touching git.
4. **F5 edits `.github/workflows/ci.yml`.** That is deliberate and it only affects the
   demo branch. The triage skill itself is forbidden from editing workflow files.
5. **`droid exec` output is not deterministic.** Cluster ordering and wording vary between
   runs. The classifications and routes are what should be stable; rehearse at least once
   so nothing in the narration depends on exact phrasing.
6. **The nightly corpus names files this repo does not have.** It is a synthetic corpus
   describing a much larger codebase, so triage cannot run a verification command against
   most clusters and correctly falls back to `needs-human`. It costs routing accuracy on
   the scorecard and it is why the banked run reads 6/10. Fixing it means pointing the
   fixture's root causes at real files in this repo.
