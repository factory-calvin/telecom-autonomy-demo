# CI Failure Triage Rubric

Single source of truth for the **triage** step of the `ci-triage` skill
(`.factory/skills/ci-triage/SKILL.md`). It decides, for each root-cause cluster of CI
failures, what kind of failure it is and who or what should act on it.

The rubric is deliberately conservative. When in doubt, route to a human. Shipping a
wrong fix costs more trust than shipping no fix.

## Step 1: cluster before you classify

Never classify individual test failures. One bad line of source can fail dozens of tests
across several jobs, and triaging them separately is exactly the waste this replaces.

1. Parse every JUnit XML in the run bundle plus the raw job logs.
2. Normalize each failure into a **signature**: assertion type, the failing production
   symbol, and the top project-owned stack frame. Strip absolute paths, line numbers,
   timestamps, durations, hex addresses, and object hashes.
3. Group failures with equal signatures.
4. Merge groups that resolve to the same root cause after reading the code, even when
   their signatures differ (a backend aggregation bug and the e2e assertion that
   cross-checks it are **one** cluster).
5. A job that failed with no test failures at all (dependency resolution, lint, format,
   build, container, network) is its own cluster.

Report the funnel explicitly: total failures in, clusters out.

## Step 2: classify each cluster

Exactly one class per cluster.

| Class            | The failure means                                                                  | Typical evidence                                                                                                       |
| ---------------- | ---------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------- |
| `product-code`   | Production source is wrong. The test encodes the correct expectation.              | The test states a business rule in its name or comment; the source contradicts it; the diff touched the source.        |
| `test-code`      | Production source is right. The test's expectation is stale.                       | The diff deliberately changed behavior or copy; the test asserts the previous value; no rule in the test is violated.  |
| `contract-drift` | Two sides of an interface disagree (API payload vs client, schema vs query).       | Failures span apps; a field, route, or type was renamed on one side only.                                              |
| `infra`          | The runner, toolchain, network, or dependency graph failed. No code was exercised. | Dependency resolution error, image pull, port bind, DNS, disk, OOM, expired credential, timeout before any test ran.   |
| `flake`          | The test's outcome depends on order, time, concurrency, or shared state.           | Passes in isolation and fails in the suite, or vice versa; passes on retry; depends on wall-clock or a shared fixture. |
| `hygiene`        | Lint, formatting, or type-check violation with no behavioral impact.               | `eslint`, `tsc --noEmit`, `prettier --check`, or `spotlessCheck` failed; no test failed.                               |

Tie-breakers:

- Cluster mixes stale assertions **and** a real source bug: split it. Never classify one
  cluster as two things.
- Cannot tell `product-code` from `test-code`: it is `needs-human`. The question "which
  side is correct" is a product decision, not an inference.
- A failure that only appears on retry is `flake`, even if a plausible source bug exists.
  Say so and stop.

## Step 3: route each cluster

| Route                  | Meaning                                                         | Code change? |
| ---------------------- | --------------------------------------------------------------- | ------------ |
| `agent-fix-now`        | Open a stacked fix PR. A human still reviews and merges it.     | Yes          |
| `needs-human`          | Post the analysis and a proposed resolution. Wait for a person. | No           |
| `platform-team`        | Not a code defect. Route to whoever owns the pipeline.          | No           |
| `quarantine-and-retry` | Re-run or skip the test. Do not touch production source.        | No           |

Route `agent-fix-now` only if **all** of these hold:

1. **Objective.** Success is confirmed by an existing automated signal you can name and
   run: a specific test selector, `pnpm lint`, or `pnpm format:check`.
2. **Single correct implementation.** One well-established fix. No taste, copy, product,
   or architecture judgment.
3. **Localized.** Confined to files you have read and named. No new dependencies, no
   schema or migration changes, no CI configuration changes.
4. **Low blast radius.** Mechanical and reversible, typically under ~50 lines.
5. **Verified.** The verification command passes after your change and the previously
   passing tests still pass.

Route `needs-human` if **any** of these are true:

- The correct behavior is ambiguous, or two defensible answers exist.
- A product, copy, brand, or UX decision is required.
- The fix crosses an interface boundary and something upstream may depend on the old shape.
- The fix needs a new dependency, an infrastructure change, or a cross-cutting refactor.
- Confidence is below 0.75 and the class does not already pin a route.

`infra` always routes to `platform-team`. `flake` always routes to `quarantine-and-retry`.
Neither ever produces a production-source change, no matter how obvious a nearby bug looks.

`hygiene` routes to `agent-fix-now` when the fix is the mechanical one the tool asks for
(remove the unused symbol, apply the formatter, add the missing type). It routes to
`needs-human` when the only way to make the tool pass is to suppress or reconfigure a
rule, because that is a policy decision.

## Step 4: emit a verdict per cluster

One object per cluster, conforming to `.factory/skills/ci-triage/verdict.schema.json`:

```json
{
  "cluster_id": "c1",
  "signature": "assertEquals|DashboardController.getStats|monthly_revenue",
  "failing_tests": [
    "DashboardControllerTest.statsReturnsActiveCustomerCountAndMonthlyRevenue",
    "DashboardControllerTest.statsRevenueExcludesNonActiveCustomers",
    "dashboard-revenue.spec.ts:monthly revenue counts only active subscribers"
  ],
  "jobs": ["Backend", "E2E Tests"],
  "classification": "product-code",
  "route": "agent-fix-now",
  "confidence": 0.93,
  "root_cause": "getStats aggregates monthlyPrice over customerRepository.findAll() instead of findByStatus(ACTIVE), so suspended and cancelled subscribers are billed.",
  "target_files": ["backend/src/main/java/com/example/demo/controller/DashboardController.java"],
  "proposed_fix": "Restore findByStatus(Customer.Status.ACTIVE) in getStats and getRevenueByPlan.",
  "verification": "cd backend && ./gradlew test --tests '*DashboardControllerTest*'",
  "human_note": null
}
```

Rules for the fields:

- `confidence` is your probability that the classification **and** the proposed fix are
  correct. Below 0.75 rules out `agent-fix-now`. For `product-code`, `test-code`,
  `contract-drift` and `hygiene` that leaves `needs-human`. `infra` and `flake` keep their
  own routes, which are not auto-fixes to begin with, so a low-confidence infra cluster
  still goes to the platform team rather than becoming a routing contradiction.
- `root_cause` names the symbol and the mistake. "Test failed" is not a root cause.
- `verification` must be a command that actually exists in this repo and that fails
  before the fix and passes after it.
- `human_note` is required whenever `route` is not `agent-fix-now`: state what decision
  the person has to make.

## Step 5: labels

Apply one class label per cluster, plus one hand-off label for the run:

| Label               | When                                        |
| ------------------- | ------------------------------------------- |
| `ci:product-code`   | any cluster classified `product-code`       |
| `ci:test-code`      | any cluster classified `test-code`          |
| `ci:contract-drift` | any cluster classified `contract-drift`     |
| `ci:infra`          | any cluster classified `infra`              |
| `ci:flake`          | any cluster classified `flake`              |
| `ci:hygiene`        | any cluster classified `hygiene`            |
| `agent-fix-ready`   | at least one cluster routed `agent-fix-now` |
| `ci:needs-human`    | at least one cluster routed `needs-human`   |

## Expected outcome for the demo faults (oracle)

Ground truth for the seeded faults in `demo/ci-failures/`. Used to score a triage run.

| Fault | What is seeded                                                         | Expected classification | Expected route         | Code change |
| ----- | ---------------------------------------------------------------------- | ----------------------- | ---------------------- | ----------- |
| `F1`  | Dashboard revenue aggregates all customers, not just ACTIVE            | `product-code`          | `agent-fix-now`        | yes         |
| `F2`  | `monthly_price` renamed to `price_monthly` in the Plan API response    | `contract-drift`        | `needs-human`          | no          |
| `F3`  | Plans table column renamed in source; test still asserts the old label | `test-code`             | `agent-fix-now`        | yes (test)  |
| `F4`  | Order-dependent test that mutates shared module state                  | `flake`                 | `quarantine-and-retry` | no          |
| `F5`  | E2E job pins a nonexistent `faker` version                             | `infra`                 | `platform-team`        | no          |
| `F6`  | Unused import plus formatting violation in a component                 | `hygiene`               | `agent-fix-now`        | yes         |

A triage run is scored against this table: one point for the classification, one for the
route, and a hard zero for the whole run if any cluster routed to `platform-team`,
`quarantine-and-retry`, or `needs-human` produced a production-source change.
