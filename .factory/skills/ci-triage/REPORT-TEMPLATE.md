# CI Failure Triage

<!-- Replace every <placeholder>. Delete sections that do not apply, do not leave them empty. -->

**Run:** [<workflow> #<run_id>](run_url) · **Commit:** `<head_sha>` · **Branch:** `<head_ref>`
**Failed jobs:** <job, job>
**Verdict:** <N> failures across <J> jobs collapsed into **<M> root causes**

## Clusters

| #   | Root cause                                   | Failures | Class     | Route     | Confidence |
| --- | -------------------------------------------- | -------- | --------- | --------- | ---------- |
| c1  | <one line, names the symbol and the mistake> | <n>      | `<class>` | `<route>` | <0.00>     |

## <c1> · <short root-cause title>

**Classification:** `<class>` → **route:** `<route>` (confidence <0.00>)

**Failing tests**

```
<test id>
<test id>
```

**Root cause**

<What is actually wrong, in terms of the symbol and the rule it violates. Cite the file
and the line you read.>

**Evidence**

- <the assertion or log line that proves it>
- <what the diff under test shows>

**Action**

<For agent-fix-now: the exact change. For anything else: what a person has to decide and
why the agent stopped.>

**Verification**

```bash
<command that fails before the fix and passes after>
```

<!-- Repeat one section per cluster. -->

## Not acted on

| #    | Class     | Why the agent stopped | Who decides                           |
| ---- | --------- | --------------------- | ------------------------------------- |
| c<n> | `<class>` | <one line>            | <platform team / PR author / product> |

## Fixes shipped

<!-- Fix mode only. Delete when FIX was not set. -->

| #    | Files changed | Lines     | Verification | Result                    |
| ---- | ------------- | --------- | ------------ | ------------------------- |
| c<n> | <file>        | +<a>/-<b> | `<command>`  | <pass / reverted: reason> |

## Run economics

<!-- Leave these as placeholders. The caller replaces this table from the CLI's own
     completion event; an agent cannot see its own token accounting. -->

| Metric            | Value   |
| ----------------- | ------- |
| Model             | <model> |
| Input tokens      | <n>     |
| Output tokens     | <n>     |
| Cache read tokens | <n>     |
| Factory credits   | <n>     |
| Wall clock        | <n>s    |
| Turns             | <n>     |

<!-- Batch mode only: link the scorecard. -->

## Accuracy

See `score.md`. Headline: <k>/<n> classifications correct, <k>/<n> routes correct,
**<n> false auto-fixes**.
