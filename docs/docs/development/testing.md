---
sidebar_position: 2
---

# Testing

## Frontend Tests

Uses Vitest with React Testing Library.

### Running Tests

```bash
pnpm test:frontend      # Run once
pnpm test:frontend --watch  # Watch mode
```

### Test Location

```
__tests__/
├── setup.ts              # Test setup
└── components/
    └── logo.test.tsx     # Component tests
```

### Writing Tests

```tsx
import { render, screen } from "@testing-library/react"
import { describe, it, expect } from "vitest"
import { Button } from "@/components/ui/button"

describe("Button", () => {
  it("renders children", () => {
    render(<Button>Click me</Button>)
    expect(screen.getByRole("button")).toHaveTextContent("Click me")
  })
})
```

## Backend Tests

Uses JUnit 5 with Spring Boot Test.

### Running Tests

```bash
pnpm test:backend
# Or directly
cd backend && ./gradlew test
```

### Test Location

```
backend/src/test/java/com/example/demo/
└── DemoApplicationTests.java
```

### Writing Tests

```java
@SpringBootTest
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getAllCustomers_returnsOk() throws Exception {
        mockMvc.perform(get("/api/customers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }
}
```

## Running All Tests

```bash
pnpm test
```

This runs frontend tests followed by backend tests.

## Dashboard Stats Performance Benchmark

The current-cycle data aggregation uses the SQLite index
`idx_usage_records_type_customer_recorded_at` on
`usage_records(type, customer_id, recorded_at)`. The Python schema setup builds
the index after bulk loading, while backend startup creates it with
`IF NOT EXISTS`, so existing demo databases are upgraded without deleting or
reseeding data.

With the backend running against a production-like database, run:

```bash
python3 scripts/benchmark-dashboard-stats.py
```

The benchmark makes 5 warmup requests followed by 30 measured requests. It
prints p50 and p95 latency plus `EXPLAIN QUERY PLAN` output. The p95 target is
below 500 ms, and the plan must contain
`USING INDEX idx_usage_records_type_customer_recorded_at`.

## Ticket Deadline Checks

Deadline unit and controller tests use fixed Java `Clock` values. They cover
weekday and weekend deadlines, exact due and 24-hour boundaries, state
precedence, filtered pagination, dashboard reconciliation, reopening, and
idempotent priority escalation.

For a manual audit without modifying the checked-in database, seed a disposable
database and query both APIs with the same instant:

```bash
curl 'http://localhost:8080/api/tickets?deadlineState=RESOLUTION_OVERDUE&asOf=2026-08-18T10:00:01Z'
curl 'http://localhost:8080/api/dashboard/stats?asOf=2026-08-18T10:00:01Z'
curl -X POST 'http://localhost:8080/api/tickets/reconcile-deadlines'
curl -X POST 'http://localhost:8080/api/tickets/reconcile-deadlines'
```

The two GET responses must reconcile at their shared `as_of`. The second POST
must report zero additional updates. The deterministic Python and fallback
Java seed rows have subjects beginning with `SLA audit:`.

## CI Integration

Tests run automatically on:

- Push to `main`
- Pull requests to `main`

See `.github/workflows/ci.yml` for configuration.

Pull requests also run the Droid risk router in
`.github/workflows/droid-risk-router.yml`. The workflow calls:

```bash
./scripts/risk-router.sh
```

The router combines a read-only Droid assessment with deterministic minimum scores for
sensitive paths. It applies a `risk:low`, `risk:medium`, or `risk:high` label and routes
the pull request to `auto-merge-eligible` or `needs-human-review`. A high-risk result
fails the check so a human must review it.

Changes to agent instructions (`AGENTS.md` and `.factory/`) or the review pipeline
(`.github/workflows/`, `Jenkinsfile`, the router script, and its prompt) always route to
`needs-human-review`, regardless of score. These files are excluded from the docs-only
risk cap and never auto-merge.

The same script runs from `Jenkinsfile` in a multibranch pipeline. Set `PR_NUMBER` when
running it outside GitHub Actions; Jenkins derives that value from `CHANGE_ID`.
