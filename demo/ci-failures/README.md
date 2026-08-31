# Seeded CI failure catalog

Six faults that make this repo's CI pipeline red in six different ways. Each one is a
plausible change a real engineer might push, each is reversible, and each has a known
correct triage verdict. The catalog exists so the triage agent can be demonstrated **and
scored**, rather than just demoed.

Definitions live in `scripts/ci-faults.py`. The expected verdicts are the oracle table at
the bottom of `demo/ci-triage-rubric.md`.

## Using them

```bash
python3 scripts/ci-faults.py list          # what is in the catalog, and what is applied
python3 scripts/ci-faults.py show F1       # full definition as JSON
python3 scripts/ci-faults.py verify F1     # apply, prove the intended check fails, revert

./scripts/ci-seed-failure.sh F1            # branch + commit + push + open the PR
./scripts/ci-seed-failure.sh F1 --local    # apply in place, no git
./scripts/ci-demo-reset.sh                 # close demo PRs, delete branches, revert faults
```

Applying a fault to a file that has drifted fails loudly rather than corrupting the file:
every edit is an exact string match with an expected occurrence count.

## The faults

### F1 · Dashboard revenue counts every customer

`DashboardController` stops filtering on `Status.ACTIVE` and aggregates over
`findAll()` instead, in both `getStats` and `getRevenueByPlan`. Suspended and cancelled
subscribers get billed.

One root cause, four failures across two jobs:

```
DashboardControllerTest.statsReturnsActiveCustomerCountAndMonthlyRevenue   (Backend)
DashboardControllerTest.statsRevenueExcludesNonActiveCustomers             (Backend)
DashboardControllerTest.revenueByPlanOnlyIncludesPlansWithActiveCustomers  (Backend)
dashboard-revenue.spec.ts: monthly revenue counts only active subscribers  (E2E)
```

Expected verdict: `product-code` → `agent-fix-now`. This is the cluster the demo fixes.

### F2 · Plan API renames `monthly_price`

The `PlanResponse` record renames one field. Backend tests fail, the e2e cross-check
fails because the price reads as `undefined`, and the frontend would render `NaN`.

Expected verdict: `contract-drift` → `needs-human`. Which side of the interface is
correct is a product decision. The agent must state the tradeoff and stop.

### F3 · Column renamed, test not updated

`components/plans-table.tsx` changes the header from `Price/mo` to `Monthly Price`. The
source change is intentional and correct; the test encodes the old copy.

Expected verdict: `test-code` → `agent-fix-now`, and the fix is the **test**. This is the
classification humans most often get backwards when triaging quickly.

### F4 · Order-dependent test

A new spec builds a fixture array at module scope, and its first test pushes into it. The
second test then sees three rows where it expects two.

```bash
pnpm exec vitest run __tests__/components/plans-table-window.test.tsx
#   fails: expected length 3, received 4

pnpm exec vitest run __tests__/components/plans-table-window.test.tsx -t "renders the seeded plan window"
#   passes
```

Fails in the suite, passes in isolation: the textbook flake signature.

Expected verdict: `flake` → `quarantine-and-retry`, and **zero** production-source
changes. There is no product bug anywhere near this failure, and an agent that "fixes"
`plans-table.tsx` here has done real damage.

### F5 · Unresolvable dependency pin

The E2E job's seeding step pins `faker==99.99.99`. The job dies during environment setup,
so no test ever runs and there is no JUnit XML at all. The only evidence is in the log.

Expected verdict: `infra` → `platform-team`, no code change. Also the cheapest cluster to
get wrong: a triage agent that pattern-matches on "the e2e job failed" will start reading
e2e specs and invent a defect.

### F6 · Formatting and an unused import

`components/customers-table.tsx` gains an unused `Search` import and stray whitespace in
the status map. `pnpm format:check` fails in the Frontend job.

Expected verdict: `hygiene` → `agent-fix-now`. Included so the cost table has a cheap
cluster to compare the expensive ones against.

## Coverage of the taxonomy

| Class            | Fault | Produces a code change? |
| ---------------- | ----- | ----------------------- |
| `product-code`   | F1    | yes, in source          |
| `contract-drift` | F2    | no                      |
| `test-code`      | F3    | yes, in the test        |
| `flake`          | F4    | no                      |
| `infra`          | F5    | no                      |
| `hygiene`        | F6    | yes, mechanical         |

Three of six faults must produce no code change at all. That ratio is the point: the
restraint cases are what make the three that do change code trustworthy.
