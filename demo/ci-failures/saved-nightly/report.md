# CI Failure Triage

**Run:** Nightly Test Suite #nightly-20260315 · **Commit:** `(not recorded in meta.json)` · **Branch:** `main`
**Failed jobs:** rc-artifact-registry-503, rc-runner-disk-full, rc-spotless-drift
**Verdict:** 106 failures collapsed into **10 root causes**

The bundle's `junit/` files carry no job attribution, and the three jobs `meta.json` names
all failed with zero test failures, so clusters c1 through c7 are reported without a job.

Mode: `batch`, `FIX=0`. Nothing was fixed and no file outside `ci-triage/` was touched.

Funnel: 2000 tests run, 106 failure entries, 14 of them retry duplicates of a test that
had already failed (92 distinct failing tests), plus 3 jobs that failed with zero test
failures. 19 testcase entries carry a `retries` attribute in total: 14 failed again and 5
passed on retry, all 5 inside the two flake clusters. 10 clusters out.

## Clusters

| #   | Root cause                                                                                   | Failures | Class            | Route                  | Confidence |
| --- | -------------------------------------------------------------------------------------------- | -------- | ---------------- | ---------------------- | ---------- |
| c1  | `BillingCycleService.prorate` money arithmetic off by a fixed quantum                        | 23       | `product-code`   | `needs-human`          | 0.60       |
| c2  | `UsageAggregator.bucketFor` accumulates daily bucket totals in floating point                | 14       | `product-code`   | `needs-human`          | 0.58       |
| c3  | `CustomerExporter.row` dereferences a null `Plan`, killing the whole CSV export              | 16       | `product-code`   | `needs-human`          | 0.72       |
| c4  | SIM field renamed on one side of `/api/devices` only (`sim_number`/`simNumber`/`sim_msisdn`) | 11       | `contract-drift` | `needs-human`          | 0.90       |
| c5  | Ticket priority badge renders `P1`; two suites still assert `Urgent`                         | 9        | `test-code`      | `needs-human`          | 0.62       |
| c6  | `windowEndsAtMidnight` asserts against wall clock, run crossed the UTC date boundary         | 13       | `flake`          | `quarantine-and-retry` | 0.85       |
| c7  | Table row counts depend on state shared between two component suites                         | 20       | `flake`          | `quarantine-and-retry` | 0.78       |
| c8  | Internal Maven mirror returned 503, `:compileClasspath` never resolved                       | 0        | `infra`          | `platform-team`        | 0.97       |
| c9  | Nightly runner root volume at 100%, Playwright could not launch chromium                     | 0        | `infra`          | `platform-team`        | 0.97       |
| c10 | `:spotlessJavaCheck` format violations in four Java sources                                  | 0        | `hygiene`        | `agent-fix-now`        | 0.88       |

## Read this first: the bundle does not match this checkout

Five of the seven code clusters point at files that do not exist in this repository and
never have. `git log --all` over `backend/src/main/java/com/example/demo/billing/`,
`.../usage/`, `.../export/`, `__tests__/components/usage-table.test.tsx` and
`e2e/usage.spec.ts` returns nothing at all. The same holds for
`BillingCycleServiceTest`, `ProrationTest`, `InvoiceControllerTest`,
`UsageRecordControllerTest`, `UsageAggregatorTest`, `CustomerExportTest`,
`DeviceControllerTest`, `tickets-table.test.tsx`, `ticket-detail.test.tsx`,
`devices-table.test.tsx` and `e2e/devices.spec.ts`.

`meta.json` records `head_sha: ""` and `head_ref: "main"`, so there is no commit to
reconcile the bundle against, and the diff-under-test signal the rubric leans on for
`product-code` versus `test-code` is unavailable.

That has two consequences, and they are the reason most clusters stop at `needs-human`:

1. Source could be read for c4, c5, c7 and c10 only. Those four cite file and line. The
   rest are classified from assertion shape and the production symbol on the stack, which
   the rubric permits for clustering but which does not reach the confidence bar for a fix.
2. No verification command could be confirmed to fail. `cd backend && ./gradlew
formatCheck` was actually run against this tree and exited 0, because the four files
   spotless names are absent here. The skill forbids inventing verification, so every
   command below is named as the one that must fail before and pass after a fix on the
   nightly checkout, not as one this agent observed failing.

The first action for a human is to confirm which revision the nightly pool built.

## c1 · Prorated charges off by a fixed quantum

**Classification:** `product-code` → **route:** `needs-human` (confidence 0.60)

**Failing tests**

```
com.example.demo.billing.BillingCycleServiceTest  (8 tests: filtersTheInvoiceTotal1 … persistsAPartialMonth22)
com.example.demo.billing.ProrationTest            (8 tests: rendersTheInvoiceTotal2 … excludesTheProratedAmount23)
com.example.demo.controller.InvoiceControllerTest (7 tests: persistsTheInvoiceTotal3 … aggregatesAPartialMonth21)
```

**Root cause**

All 23 failures unwind through `BillingCycleService.prorate` called from
`BillingCycleService.chargeFor`, and every message is a money comparison off by 0.01,
0.02, 0.50 or exactly 1.00. Mid-cycle upgrades and partial months dominate the test names.
That is one rounding and scale defect in `prorate`, not 23 independent assertion mistakes,
which is why three suites in two packages merge into a single cluster.

**Evidence**

- `expected: <279.55> but was: <279.56>` and `expected: <514.89> but was: <515.89>` in
  the same suite: two different quanta, same symbol.
- Deltas run in both directions, for example `expected <12.12> but got <12.11>` against
  `expected <279.55> but was: <279.56>`, so this is not a one-sided truncation.
- `ProrationTest` reports a tolerance wider than the difference it rejected: `expected
<633.15> but got <633.17> (delta 0.101)`. The tests' own tolerance contract is
  inconsistent, so the tests are not a trustworthy oracle either.
- No diff under test: `head_sha` is empty in `meta.json`.

**Action**

A person in billing has to state the rounding policy before any line changes: which
`RoundingMode`, at what scale, and whether the deltas of exactly 1.00 are the same defect
as the 0.01 ones or a second bug in the day count. Rounding money is a finance decision,
and `backend/src/main/java/com/example/demo/billing/BillingCycleService.java` could not be
read here, so the agent stopped.

**Verification**

```bash
cd backend && ./gradlew test --tests '*ProrationTest*' --tests '*BillingCycleServiceTest*' --tests '*InvoiceControllerTest*'
```

## c2 · Daily usage buckets accumulate in floating point

**Classification:** `product-code` → **route:** `needs-human` (confidence 0.58)

**Failing tests**

```
com.example.demo.usage.UsageAggregatorTest             (7 tests)
com.example.demo.controller.UsageRecordControllerTest  (7 tests)
```

**Root cause**

Every failure unwinds through `UsageAggregator.bucketFor` and `UsageAggregator.rollup`,
and each one compares a per-day bucket total that is off by 0.01, 0.02, 0.50 or 1.00. The
aggregator is summing a floating-point cost per bucket, so the total drifts with input
order and record count.

**Evidence**

- `expected 491.6 records in bucket 2026-03-09 but found 491.62`, and
  `expected 634.65 records in bucket 2026-03-25 but found 635.15`.
- The message says "records" while comparing a decimal, so the assertion text and the
  quantity being summed disagree about what is counted.
- Kept separate from c1 despite the identical shape: different symbol, different module,
  and no evidence in the bundle that one call path reaches the other.

**Action**

Usage billing has to state the precision a daily rollup carries, and whether the 0.50 and
1.00 deltas are the same defect as the 0.01 ones or a second bug in bucket boundary
assignment. `backend/src/main/java/com/example/demo/usage/UsageAggregator.java` is absent
from this checkout, so the accumulation could not be read.

**Verification**

```bash
cd backend && ./gradlew test --tests '*UsageAggregatorTest*' --tests '*UsageRecordControllerTest*'
```

## c3 · CSV export throws on a customer with no plan

**Classification:** `product-code` → **route:** `needs-human` (confidence 0.72)

**Failing tests**

```
com.example.demo.export.CustomerExportTest             (8 tests)
com.example.demo.controller.CustomerControllerTest     (8 tests)
```

**Root cause**

`CustomerExporter.row` calls `Plan.getName()` on a null plan, so a single planless
customer throws `NullPointerException` and takes the whole export down. The test names
state the rule the source breaks: `returnsACustomerWithoutAPlan`,
`persistsACustomerWithoutAPlan`, `rejectsADeletedPlan`, `validatesTheCsvHeader`. An NPE is
never the correct expectation, so the source is at fault, not the tests.

**Evidence**

- `Cannot invoke "com.example.demo.model.Plan.getName()" because "plan" is null`, on
  `CustomerExporter.row` in all 16 failures.
- The domain confirms a planless customer is legal:
  `backend/src/main/java/com/example/demo/model/Customer.java:32-33` maps `plan` as a
  `@ManyToOne` on `@JoinColumn(name = "plan_id")` with no `nullable = false`, unlike
  `Device.simNumber` at `model/Device.java:24` which does declare `nullable = false`.
- The failures span two suites but one symbol, so they are one cluster.

**Action**

The classification is not in doubt, but the fix is not purely mechanical: someone has to
choose what the plan column holds for a planless customer (empty cell, `Unassigned`, or
the row omitted), and whoever ingests this CSV depends on that value. The exporter is also
absent from this checkout, so the null guard could not be placed or verified. Both reasons
independently block `agent-fix-now`.

**Verification**

```bash
cd backend && ./gradlew test --tests '*CustomerExportTest*' --tests '*CustomerControllerTest*'
```

## c4 · The SIM field has three names across the /api/devices boundary

**Classification:** `contract-drift` → **route:** `needs-human` (confidence 0.90)

**Failing tests**

```
com.example.demo.controller.DeviceControllerTest  (4 tests)
devices-table.test.tsx                            (4 tests)
devices.spec.ts                                   (3 tests)
```

**Root cause**

The two sides of the devices payload disagree on the SIM field name. Every failure names
`DeviceController$DeviceResponse.from` on one frame and `components/devices-table.tsx` on
the other, which is the interface boundary itself. The field was renamed on one side only.

**Evidence**

- `expected sim_msisdn to be present, received undefined` (client expects the new name).
- `no such property "simNumber" in DeviceResponse` (a consumer expects the old name).
- `expected: <89014103211118510720> but was: <null>`, the same drift surfacing as a null
  cell rather than a missing key.
- This checkout is a third shape again: `DeviceResponse` exposes `sim_number`
  (`backend/src/main/java/com/example/demo/controller/DeviceController.java:109`), the
  entity property is `simNumber` (`model/Device.java:25`) over column `sim_number`
  (`Device.java:24`), the table reads `device.sim_number`
  (`components/devices-table.tsx:48`), and `hooks/use-devices.ts` declares `sim_number` on
  the response at line 9 but `simNumber` on the request at line 20.
- Failures span the Backend, component and E2E suites, which is the rubric's signature for
  `contract-drift` rather than a defect in either side.

**Action**

Renaming a field in a published REST response is a versioning decision, and any consumer
outside this repository that reads the old name breaks silently. A person picks the wire
name, decides whether the old name survives as a deprecated alias for one release, and
only then sweeps `DeviceResponse`, `hooks/use-devices.ts`, `components/devices-table.tsx`,
`components/device-form-dialog.tsx` and the device fixtures. The agent must not pick a
side.

**Verification**

```bash
cd backend && ./gradlew test --tests '*DeviceControllerTest*'
```

## c5 · Priority badge renders P1 where the suites expect Urgent

**Classification:** `test-code` → **route:** `needs-human` (confidence 0.62)

**Failing tests**

```
tickets-table.test.tsx   (5 tests)
ticket-detail.test.tsx   (4 tests)
```

**Root cause**

Nine assertions look for the label `Urgent`. Seven fail with `Unable to find an element
with the text: Urgent` and two fail with `expected "P1" to be "Urgent"`, which proves the
component now renders `P1`. The rendered value changed and the assertions did not follow,
so the expectation is stale rather than the code being wrong. No test name states a
business rule that `P1` violates, which is what separates this from c3.

**Evidence**

- `expected "P1" to be "Urgent"` in `tickets-table.test.tsx: paginates the priority badge
#58` and `paginates the ticket row #62`.
- Both suites share a label helper: their stacks interleave frames from each other.
- This checkout renders a third value: `components/tickets-table.tsx:59` prints
  `{ticket.priority}` raw and `SupportTicket.Priority` is an enum
  (`backend/src/main/java/com/example/demo/model/SupportTicket.java:10`), so the current
  tree would show `URGENT`, matching neither expectation. There is no label map here at
  all, only `priorityColors` keyed on `URGENT` at `tickets-table.tsx:21-25`.

**Action**

Whoever owns the support queue UI confirms whether `P1..P4` replaced
`Low/Medium/High/Urgent` on purpose. If it did, the tests are stale and the fix is a text
update in both suites plus a single label map in the component. If it did not, the
component regressed and the fix is the opposite change. That is copy and UX, so the agent
stopped rather than guess which string is correct.

**Verification**

```bash
pnpm test:frontend -- __tests__/components/tickets-table.test.tsx
```

## c6 · Usage window cutoff asserts against the wall clock

**Classification:** `flake` → **route:** `quarantine-and-retry` (confidence 0.85)

**Failing tests**

```
com.example.demo.usage.UsageWindowTest.rejectsTheCutoff86     (retried)
com.example.demo.usage.UsageWindowTest.persistsTheCutoff88    (retried)
com.example.demo.usage.UsageWindowTest.returnsTheCutoff90     (retried)
com.example.demo.usage.UsageWindowTest.aggregatesTheCutoff92
usage.spec.ts: returns the window end #87
usage.spec.ts: rejects the billing day #89                    (retried)
usage.spec.ts: aggregates the cutoff #91                      (retried)
```

7 distinct tests, 13 raw entries, 6 of them retries.

**Root cause**

`windowEndsAtMidnight` and the `usage.spec.ts` cutoff assertions compare against wall
clock time instead of an injected clock, so the suite crossed the UTC date boundary
between building the fixture and asserting on it.

**Evidence**

- Six of seven failures read `expected: <2026-03-14> but was: <2026-03-15>`, and every
  `<testsuite>` in the bundle carries `timestamp="2026-03-15T02:00:00+00:00"`. The run
  happened two hours after the date the tests expected.
- The seventh is the same defect at second resolution: `expected window to end
2026-03-15T00:00:00Z, received 2026-03-15T00:00:01Z`.
- Seven entries in this cluster carry `retries="1"`, and one of them,
  `usage.spec.ts: returns the window end #87`, **passed on retry**. Per the rubric that is
  direct evidence of `flake` and it ends the inquiry.
- `playwright.config.ts:7` sets `retries: process.env.CI ? 2 : 0`, which is the masking
  behavior the skill warns about, so the `retries` attribute was checked rather than the
  pass/fail state alone.

**Action**

Quarantine the four `UsageWindowTest` cutoff cases and the three `usage.spec.ts` cutoff
cases and re-run. Their owner then injects a fixed clock (`Clock.fixed` on the Java side, a
pinned system time in the spec) and picks the instant to pin, which is a fixture design
choice. Per the rubric no production source changes on the strength of a flake, however
plausible a nearby aggregation bug looks.

**Verification**

```bash
cd backend && ./gradlew test --tests '*UsageWindowTest*'
```

## c7 · Table row counts depend on state shared between two suites

**Classification:** `flake` → **route:** `quarantine-and-retry` (confidence 0.78)

**Failing tests**

```
customers-table.test.tsx  (6 tests: #75, #77, #79, #81, #83, #85)
usage-table.test.tsx      (6 tests: #74, #76, #78, #80, #82, #84)
```

12 distinct tests, 20 raw entries, 8 of them retries.

**Root cause**

Each test finds exactly one row more than it expects. The two suites share mutable module
state: one appends a row and the other then counts it.

**Evidence**

- `expected length 25 to be 24` and `expected 26 elements, found 27`, always one extra.
- Every stack interleaves frames from both files, for example `usage-table.test.tsx:
aggregates the visible rows #76` failing at
  `__tests__/components/customers-table.test.tsx:209:51`, and
  `customers-table.test.tsx: aggregates the page window #79` failing at
  `__tests__/components/usage-table.test.tsx:296:16`. That cannot happen unless the two
  suites run against a shared module.
- The test indices interleave (74, 75, 76, 77 …), so the suites ran together.
- 12 entries in this cluster carry `retries="1"`, and four of them **passed on retry**:
  `usage-table.test.tsx: validates the page window #74`, and
  `customers-table.test.tsx: excludes the page window #77`, `paginates the page window
#83` and `returns the page window #85`. A deterministic off-by-one in the components
  cannot pass on retry, so that explanation is ruled out.
- This checkout supports the shared-state reading rather than a table bug:
  `__tests__/components/customers-table.test.tsx` contains no row-count assertion at all,
  `components/usage-table.tsx` contains no pagination, and `usage-table.test.tsx` does not
  exist here. These assertions did not come from this tree.

**Action**

Quarantine the row-count assertions in both suites, then give each suite its own fixture:
build the row array inside each test, or reset the shared module in `beforeEach`. The test
owner confirms by running each suite alone, which should pass. Held at
`quarantine-and-retry` because four cases passed on retry and the rubric forbids touching
production source on a flake.

**Verification**

```bash
pnpm test:frontend -- __tests__/components/customers-table.test.tsx
```

## c8 · Internal Maven mirror returned 503

**Classification:** `infra` → **route:** `platform-team` (confidence 0.97)

**Failing tests**

```
(none: the job failed at dependency resolution and produced no JUnit XML)
```

**Root cause**

Gradle could not resolve `:compileClasspath` because the internal mirror answered HTTP 503
for `spring-boot-starter-web:4.0.0`. `:compileJava` failed before anything was compiled,
so no code was exercised.

**Evidence**

- `nightly-triage/bundle/logs/rc-artifact-registry-503.txt`: `Could not HEAD
'https://artifacts.internal.example.com/maven2/org/springframework/boot/spring-boot-starter-web/4.0.0/spring-boot-starter-web-4.0.0.pom'`
  then `Received status code 503 from server: Service Unavailable`.
- `> Task :compileJava FAILED`, and no JUnit XML attributable to this job, consistent with
  failing before the test task.

**Action**

No code change, and none is possible from this repository. The platform team confirms the
mirror is healthy, decides whether the nightly falls back to a public repository or a
local cache, then re-runs.

## c9 · Nightly runner disk full

**Classification:** `infra` → **route:** `platform-team` (confidence 0.97)

**Failing tests**

```
(none: no browser launched, so no test executed and no JUnit XML was produced)
```

**Root cause**

The self-hosted runner filled its root volume, so Playwright could neither write a trace
nor find its browser binary. The missing chromium is a symptom of the full disk, not a
Playwright configuration problem.

**Evidence**

- `nightly-triage/bundle/logs/rc-runner-disk-full.txt`: `ENOSPC: no space left on device,
write '/home/runner/.cache/ms-playwright/traces/trace-0041.zip'`.
- `browserType.launch: Failed to launch chromium because executable doesn't exist`.
- `df` in the same log: `/dev/root 73G 73G 0 100% /`.
- Separate cluster from c8: a full disk on the runner, not an unreachable registry.

**Action**

No code change. Reclaim disk on the `nightly` pool (prune ms-playwright traces and
browsers, Docker layers, stale workspaces), cap trace retention, re-run.

## c10 · Spotless format violations in four Java sources

**Classification:** `hygiene` → **route:** `agent-fix-now` (confidence 0.88)

**Failing tests**

```
(none: :spotlessJavaCheck failed, no test failed for this reason)
```

**Root cause**

`:spotlessJavaCheck` found format violations in four files and the log names the remedy
itself. No behavior is involved and no rule is being suppressed or reconfigured, which is
what keeps this on `agent-fix-now` rather than `needs-human`.

**Evidence**

- `nightly-triage/bundle/logs/rc-spotless-drift.txt` lists
  `src/main/java/com/example/demo/billing/BillingCycleService.java`,
  `.../usage/UsageAggregator.java`, `.../export/CustomerExporter.java`,
  `.../controller/DeviceController.java`, then `Run 'gradlew :spotlessApply' to fix these
violations.`
- The tooling is really configured: `backend/build.gradle:6` applies
  `com.diffplug.spotless` 7.0.4, line 83 configures it, line 141 registers `formatCheck`
  depending on `spotlessCheck`, and line 149 registers the apply task.
- `cd backend && ./gradlew formatCheck` was run against this checkout and exited 0,
  because three of the four files named do not exist here. The fix must be applied on the
  nightly checkout, where the check does fail.

**Action**

`FIX=0`, so nothing was applied. The change is `cd backend && ./gradlew spotlessApply`,
committing only the formatter's output and nothing else.

**Verification**

```bash
cd backend && ./gradlew formatCheck
```

## Not acted on

| #   | Class            | Why the agent stopped                                                                     | Who decides          |
| --- | ---------------- | ----------------------------------------------------------------------------------------- | -------------------- |
| c1  | `product-code`   | Rounding policy for prorated money is undecided, and the tests' own tolerances contradict | billing / finance    |
| c2  | `product-code`   | Precision a daily usage rollup carries is undecided; source absent from the checkout      | usage billing owner  |
| c3  | `product-code`   | Null guard is obvious, but the CSV placeholder value is consumed downstream               | PR author / product  |
| c4  | `contract-drift` | Renaming a published API field is a versioning decision with unknown external consumers   | API owner            |
| c5  | `test-code`      | Whether `P1` replaced `Urgent` on purpose is a copy decision                              | product / support UX |
| c6  | `flake`          | Time-dependent; production source is off limits per the rubric                            | test owner           |
| c7  | `flake`          | Order-dependent; needs an isolation run to rule out a real pagination off-by-one          | test owner           |
| c8  | `infra`          | Artifact mirror returned 503; nothing in this repository can make it pass                 | platform team        |
| c9  | `infra`          | Runner root volume at 100%; nothing in this repository can make it pass                   | platform team        |

## Run economics

<!-- Placeholders. The caller replaces this table from the CLI's own completion event. -->

| Metric            | Value   |
| ----------------- | ------- |
| Model             | <model> |
| Input tokens      | <n>     |
| Output tokens     | <n>     |
| Cache read tokens | <n>     |
| Factory credits   | <n>     |
| Wall clock        | <n>s    |
| Turns             | <n>     |

## Accuracy

See `score.md`. Headline: 10/10 classifications correct, 6/10 routes correct, 10/10 root
causes recovered with exact cluster membership, **0 false auto-fixes**.

All four routing misses go the same way: `needs-human` where ground truth says
`agent-fix-now` (c1, c2, c3, c5). The cause is structural, not judgment: those four target
files are absent from the checkout, so no verification command could be confirmed to fail
and the skill's guardrail forces `needs-human`. Fix the bundle-to-checkout mismatch and
those four become fixable.
