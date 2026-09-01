---
name: ci-triage
description: >
  Triage failing CI runs for the FactoryFone repo. Clusters test failures by root
  cause, classifies each cluster as product-code, test-code, contract-drift, infra,
  flake, or hygiene per demo/ci-triage-rubric.md, routes it, and emits verdicts plus
  a PR-ready report. Optionally ships a minimal fix for clusters routed
  agent-fix-now. Used by .github/workflows/ci-autotriage.yml, by
  .github/workflows/nightly-triage.yml for batch runs, and by scripts/ci-triage.sh
  locally.
---

# ci-triage

You are replacing the manual step where an engineer opens a red pipeline, reads the logs,
decides whether the failure is the product's fault, the test's fault, or the runner's
fault, and then either fixes it or hands it to someone.

Two modes:

- **`single`** — one failed CI run for one PR. Triage it, then optionally fix.
- **`batch`** — a large corpus of failures from a nightly run. Triage and report only,
  never fix. Score against ground truth when it is present.

The rubric at `demo/ci-triage-rubric.md` is authoritative for classification and routing.
Read it before you do anything else. This file tells you how to gather evidence, where to
write output, and what the fix step may and may not do.

## Inputs

A **run bundle** directory, passed to you as `BUNDLE` (default `ci-triage/bundle`):

```
<BUNDLE>/
├── meta.json          # run context, see below
├── junit/             # every JUnit XML the run produced (any nesting)
└── logs/              # raw job logs as .txt, one file per job
```

`meta.json`:

```json
{
  "mode": "single",
  "repo": "Factory-Academy/Telecom-Demo-NextJS-Springboot",
  "workflow": "CI",
  "run_id": "1234567890",
  "run_url": "https://github.com/.../actions/runs/1234567890",
  "pr_number": 41,
  "head_sha": "abc1234",
  "head_ref": "demo/ci-fault-f1",
  "base_ref": "main",
  "failed_jobs": ["Backend", "E2E Tests"],
  "ground_truth": null
}
```

Any field may be absent. Missing `pr_number` means there is nothing to comment on; still
write the report. Missing `junit/` means you work from logs alone.

## Outputs

Write all of these under `ci-triage/` (create it):

| File            | Contents                                                                            |
| --------------- | ----------------------------------------------------------------------------------- |
| `verdicts.json` | `{ schema_version, run, funnel, clusters[], economics }`, per `verdict.schema.json` |
| `report.md`     | Human-readable report, from `REPORT-TEMPLATE.md`                                    |
| `labels.txt`    | One label per line, per rubric step 5                                               |
| `fixes.json`    | Fix mode only: what you changed, per cluster, and whether verification passed       |
| `score.md`      | Batch mode with ground truth only: accuracy table                                   |

`verdicts.json` is the machine-readable contract. Anything downstream reads that, not the
prose. Validate it against `verdict.schema.json` before you finish.

## Procedure

### 1. Read the evidence before forming a hypothesis

- Parse every file under `junit/`. Collect each `<testcase>` that has a `<failure>` or
  `<error>` child: classname, name, message, stack, and which job it came from.
- Read the tail of each log under `logs/`. Find the step that failed. A job can fail with
  zero test failures, and that is a different kind of cluster.
- Get the diff under test: `git diff <base_ref>...<head_sha>` when both are known. The
  diff is the single strongest signal for `product-code` versus `test-code`. If the diff
  deliberately changed behavior, a test asserting the old behavior is stale.
- Read the actual source files the failures point at. Do not classify from a stack trace
  alone. You are allowed to be slow here and fast later.

### 2. Cluster

Follow rubric step 1. Normalize signatures, group, then merge across jobs when the root
cause is shared. State the funnel in the report: `N failures -> M clusters`.

In batch mode expect duplicated stack traces, retried tests appearing two or three times,
and interleaved infra noise. Deduplicate retries of the same test into one entry, and note
the retry count, since "passed on retry" is direct evidence of `flake`.

### 3. Classify and route

Follow rubric steps 2 and 3. One class and one route per cluster. Write the verdict per
rubric step 4.

Guardrails that override any inference:

- `infra` and `flake` never touch production source.
- Confidence below 0.75 rules out `agent-fix-now`. It does not override the class: a
  low-confidence `infra` cluster still routes to `platform-team`, not `needs-human`.
- Never invent a `verification` command. Run it and confirm it fails now, or leave the
  cluster as `needs-human`. Commands available in this repo:
  - `cd backend && ./gradlew test --tests '*<ClassName>*'`
  - `pnpm test:frontend -- <path>`
  - `pnpm exec playwright test <spec>`
  - `pnpm lint`
  - `pnpm format:check`
  - `cd backend && ./gradlew formatCheck`

### 4. Write the report and labels

Fill `REPORT-TEMPLATE.md`. Keep the cluster table first: it is what a reviewer reads.
Write `labels.txt` per rubric step 5.

Leave `economics` out of `verdicts.json`, or set it to `{}`. You cannot see your own token
accounting, so anything you write there is a guess. The caller fills it in from the CLI's
completion event after you exit (`scripts/ci-triage-stream.py --finalize`).

### 5. Fix, only when asked

The fix step runs only when the caller sets `FIX=1`. For each cluster routed
`agent-fix-now`, in ascending blast-radius order:

1. Re-read the target files.
2. Make the **minimal** change described in `proposed_fix`. Nothing else. No drive-by
   refactors, no formatting sweeps, no renames, no added tests unless the cluster is
   `test-code` and the fix is the test.
3. Run the cluster's `verification` command. If it fails, revert that cluster's change,
   set `"applied": false` with the reason, and continue to the next cluster.
4. Run the fuller suite for the affected app to confirm you broke nothing:
   `cd backend && ./gradlew test` or `pnpm test:frontend`.
5. Append the outcome to `fixes.json`.

Never modify: `.github/workflows/**`, `demo/ci-failures/**`, `demo/ci-triage-rubric.md`,
`.factory/skills/**`, `scripts/ci-*.sh`, `package.json` dependency versions, or
`backend/build.gradle`. Those are the demo's own machinery and the pipeline definition; a
triage agent that edits its own rubric or the pipeline it is being judged by is not a
triage agent. If a fix appears to require one of those, route the cluster `needs-human`.

Do not commit, push, or open a PR. The caller owns git. Leave the working tree dirty and
describe it in `fixes.json`.

### 6. Score, in batch mode with ground truth

When `meta.json.ground_truth` points at a file, compare your clusters to it and write
`score.md`:

- Classification accuracy, plus a confusion matrix over the six classes.
- Cluster quality: how many true root causes you recovered, how many you split, how many
  you merged incorrectly.
- Routing accuracy.
- **False-autofix count**: clusters you routed `agent-fix-now` that ground truth says
  need a human. This number matters more than the others. Report it even when it is zero.

Do not read ground truth before you have written `verdicts.json`. Triage first, score
second, and say in `score.md` that you did so in that order.

## Non-negotiables

- You run headless. Never call AskUser, never wait for input, never pause for approval.
- Every claim in the report traces to a file you read or a command you ran. No speculation
  presented as finding.
- Do not create tickets, send messages, or call external services. GitHub writes are the
  caller's job.
- If the bundle is empty or unparseable, write a report saying exactly that and exit
  cleanly. A false clean bill of health is the worst possible output.

## Known failure modes

1. **Playwright retries mask flakes.** `playwright.config.ts` sets `retries: 2` in CI, so
   a flaky e2e test can appear as `passed` with retry metadata. Check the `retries`
   attribute before concluding a spec is healthy.
2. **Gradle fails the build before tests run.** A compile error in test sources produces
   no JUnit XML at all. That is `product-code` or `test-code`, never `infra`, and the
   evidence is in the log, not the XML.
3. **Spring cold start.** Backend startup takes 10-15s. A single timeout on the first
   request is `flake`, not `infra`, unless the health check never came up at all.
4. **The Python seeder falls back.** If `scripts/seed-database.py` fails, Java
   `DataSeeder` seeds far fewer rows and count-based e2e assertions fail for a reason that
   has nothing to do with the diff. Check the seeding step in the log first.
5. **Formatting is checked on two toolchains.** `pnpm format:check` covers the frontend
   and `./gradlew formatCheck` covers Java. A `hygiene` cluster may need both.
