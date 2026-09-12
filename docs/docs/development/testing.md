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

The same script runs from `Jenkinsfile` in a multibranch pipeline. Set `PR_NUMBER` when
running it outside GitHub Actions; Jenkins derives that value from `CHANGE_ID`.
