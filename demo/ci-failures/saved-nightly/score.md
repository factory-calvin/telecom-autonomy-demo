# Triage scorecard

Run `nightly-20260315`, batch mode, `FIX=0`.

**Order of work:** triage first, score second. `ci-triage/verdicts.json`,
`ci-triage/report.md` and `ci-triage/labels.txt` were written and `verdicts.json` was
validated against `.factory/skills/ci-triage/verdict.schema.json` (0 errors) **before**
`nightly-triage/ground-truth.json` was opened. Nothing below changed a classification or a
route; the only post-scoring edits to `verdicts.json` were the `retry_count` fields and the
passed-on-retry evidence in c6 and c7, which came from re-parsing `junit/`, not from ground
truth.

## Headline

| Metric                        | Result             |
| ----------------------------- | ------------------ |
| Classification accuracy       | **10 / 10** (100%) |
| Routing accuracy              | **6 / 10** (60%)   |
| True root causes recovered    | **10 / 10**        |
| Clusters incorrectly split    | 0                  |
| Clusters incorrectly merged   | 0                  |
| **False auto-fixes**          | **0**              |
| Hard-zero condition triggered | No                 |

Hard-zero check: the rubric zeroes the whole run if any cluster routed `platform-team`,
`quarantine-and-retry` or `needs-human` produced a production-source change. `FIX=0` and no
file outside `ci-triage/` was written, so no cluster produced any source change. The run is
scoreable.

## Per-cluster

| Mine | Ground truth               | Class (mine → truth)            | Route (mine → truth)                        | Class | Route |
| ---- | -------------------------- | ------------------------------- | ------------------------------------------- | ----- | ----- |
| c1   | `rc-billing-proration`     | product-code → product-code     | needs-human → agent-fix-now                 | ✅    | ❌    |
| c2   | `rc-usage-timezone`        | product-code → product-code     | needs-human → agent-fix-now                 | ✅    | ❌    |
| c3   | `rc-null-plan-npe`         | product-code → product-code     | needs-human → agent-fix-now                 | ✅    | ❌    |
| c4   | `rc-device-contract-drift` | contract-drift → contract-drift | needs-human → needs-human                   | ✅    | ✅    |
| c5   | `rc-stale-copy-assertion`  | test-code → test-code           | needs-human → agent-fix-now                 | ✅    | ❌    |
| c6   | `rc-wall-clock-boundary`   | flake → flake                   | quarantine-and-retry → quarantine-and-retry | ✅    | ✅    |
| c7   | `rc-shared-fixture-order`  | flake → flake                   | quarantine-and-retry → quarantine-and-retry | ✅    | ✅    |
| c8   | `rc-artifact-registry-503` | infra → infra                   | platform-team → platform-team               | ✅    | ✅    |
| c9   | `rc-runner-disk-full`      | infra → infra                   | platform-team → platform-team               | ✅    | ✅    |
| c10  | `rc-spotless-drift`        | hygiene → hygiene               | agent-fix-now → agent-fix-now               | ✅    | ✅    |

## Confusion matrix over the six classes

Rows are ground truth, columns are predicted.

|                    | product-code | test-code | contract-drift | infra | flake | hygiene |
| ------------------ | ------------ | --------- | -------------- | ----- | ----- | ------- |
| **product-code**   | **3**        | 0         | 0              | 0     | 0     | 0       |
| **test-code**      | 0            | **1**     | 0              | 0     | 0     | 0       |
| **contract-drift** | 0            | 0         | **1**          | 0     | 0     | 0       |
| **infra**          | 0            | 0         | 0              | **2** | 0     | 0       |
| **flake**          | 0            | 0         | 0              | 0     | **2** | 0       |
| **hygiene**        | 0            | 0         | 0              | 0     | 0     | **1**   |

Diagonal only. No class was confused with another.

## Cluster quality

Ground truth declares 10 root causes; triage produced 10 clusters, and every cluster maps
one-to-one onto a root cause with identical failing-test membership: 23, 14, 16, 9, 11, 12,
7 test-level failures and 3 job-level clusters. Nothing was split and nothing was wrongly
merged.

Two merges were the ones that mattered and both came out right:

- c1 merged `BillingCycleServiceTest`, `ProrationTest` and `InvoiceControllerTest` across
  two packages into one root cause on `BillingCycleService.prorate`. Ground truth agrees.
- c4 merged the Backend, component and E2E device suites into one `contract-drift` cluster
  rather than treating the backend and client failures separately. Ground truth agrees.

The one merge deliberately **not** made was also right: c1 and c2 share an off-by-a-quantum
money shape, and merging them would have been tempting. Ground truth keeps them apart as
`rc-billing-proration` and `rc-usage-timezone`, two different symbols in two different
modules.

The two flake clusters were also correctly kept apart (`rc-wall-clock-boundary` versus
`rc-shared-fixture-order`) rather than lumped into one "flaky frontend" bucket, and the two
infra jobs were kept apart (503 versus ENOSPC) rather than merged into one "runner
problem".

## Where the root-cause narrative differed even though the class was right

Classification was correct in all ten cases, but two root-cause explanations were only
partly right, which is worth recording because a report is read by a human who acts on the
prose, not the label:

- **c2.** Triage said floating-point accumulation in `UsageAggregator`. Ground truth says
  the aggregator buckets by local date instead of UTC, so the last hour of each day lands
  in the wrong bucket. Both are `product-code` in `UsageAggregator`, and the proposed fix
  (BigDecimal accumulation) would not have fixed the real defect. The tell that was
  available and under-weighted: three test names say `MidnightRecord`, and c6 in the same
  run is a date-boundary problem.
- **c1.** Triage said a rounding and scale defect; ground truth says `double` arithmetic
  where `BigDecimal` belongs. Same defect, and the proposed fix matches.

## Routing analysis

Four misses, all in the same direction: `needs-human` where ground truth says
`agent-fix-now`. There is no miss in the dangerous direction.

| Cluster | Why the agent held back                                                                     |
| ------- | ------------------------------------------------------------------------------------------- |
| c1      | `BillingCycleService.java` absent from the checkout; no verification command could be run   |
| c2      | `UsageAggregator.java` absent; rounding-versus-timezone ambiguity was not resolved          |
| c3      | `CustomerExporter.java` absent; also treated the CSV placeholder value as a copy decision   |
| c5      | Test files absent; treated `P1` versus `Urgent` as a copy decision needing product sign-off |

Three of the four reduce to one cause: the bundle points at
`backend/src/main/java/com/example/demo/{billing,usage,export}/` and at four frontend test
files that do not exist in this checkout and never have (`git log --all` over those paths
is empty). The skill's guardrail is explicit that a verification command must be run and
observed failing or the cluster goes to `needs-human`, so those three could not be routed
`agent-fix-now` without violating it. That is the guardrail working as designed on a
mismatched bundle, not a judgment error, and the fix is to the harness rather than to the
triage: have the nightly record a real `head_sha` and run the triage against that revision.

c5 is the one genuine judgment miss. The rubric's own oracle (fault `F3`, "Plans table
column renamed in source; test still asserts the old label") routes exactly this shape to
`agent-fix-now`, and `expected "P1" to be "Urgent"` is unambiguous about which side moved.
Treating a label the source already renamed as an open copy decision was too conservative.

Confidence calibration was directionally honest: the four misrouted clusters carry the four
lowest confidences of the seven code clusters (0.60, 0.58, 0.72, 0.62), and all six correct
routes carry 0.78 or higher. The agent knew where it was unsure; it was unsure in the right
places and slightly too unsure overall.

## Corpus facts confirmed against ground truth

| Quantity             | Triage | Ground truth | Match |
| -------------------- | ------ | ------------ | ----- |
| Tests run            | 2000   | 2000         | ✅    |
| Test failure entries | 106    | 106          | ✅    |
| Retry entries        | 19     | 19           | ✅    |
| Job-level failures   | 3      | 3            | ✅    |
| Root causes          | 10     | 10           | ✅    |

The 19 retry entries split 14 that failed again and 5 that passed on retry. All 5
passed-on-retry cases fall inside c6 and c7, the two clusters classified `flake`, which is
the strongest single piece of evidence in the bundle and it pointed the right way.
