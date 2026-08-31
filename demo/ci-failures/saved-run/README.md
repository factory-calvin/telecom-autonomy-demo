# Saved run bundle (fault F1)

A real run bundle captured from an actual failing test run with fault `F1` applied:
`DashboardController` aggregating over every customer instead of the active ones.

Contents:

| Path                   | What it is                                                            |
| ---------------------- | --------------------------------------------------------------------- |
| `meta.json`            | Run context, shaped as if a `CI` run on a PR branch had failed        |
| `junit/backend-*.xml`  | The real Gradle JUnit output: 3 failures in `DashboardControllerTest` |
| `junit/frontend-*.xml` | The real vitest output: 24 passing, which is the correct outcome      |
| `logs/backend.txt`     | Trimmed Gradle job log                                                |
| `logs/e2e.txt`         | Playwright job log with the two failing cross-check assertions        |

## Replaying it

Triage reads the **working tree** to confirm a hypothesis, so the fault has to be applied
or the agent will correctly report that the source looks fine:

```bash
./scripts/ci-seed-failure.sh F1 --local      # apply F1 in place, no git
./scripts/ci-triage.sh --local demo/ci-failures/saved-run
python3 scripts/ci-faults.py revert F1       # put it back
```

Expected verdict: 5 failures across two jobs collapse into **one** cluster, classified
`product-code`, routed `agent-fix-now`, naming
`backend/src/main/java/com/example/demo/controller/DashboardController.java`.

Add `--fix` to have it ship the change into the working tree (no commit, no push).

## When to use this

Demo insurance. If GitHub Actions is slow or the network is unhappy, this path needs
neither: no `gh`, no runner, no live pipeline. It is also the cleanest way to answer
"where do your agents actually run", because it is the same skill and the same rubric
producing the same verdict on a laptop.
