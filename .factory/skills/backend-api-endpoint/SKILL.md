---
name: backend-api-endpoint
description: Build backend API endpoints for this Spring Boot + Next.js application. Use when adding new REST endpoints, dashboard metrics, or data aggregation features.
---

# Backend API Endpoint Development

Step-by-step process for adding new API endpoints to this codebase.

## Instructions

### Phase 1: Backend Implementation

#### Step 1: Identify Data Requirements

Determine what data the endpoint needs:

- Which entities are involved? (Customer, Plan, Device, UsageRecord, SupportTicket)
- Do existing repository methods cover the query needs?
- Is aggregation or custom JPQL required?

#### Step 2: Add Repository Methods (if needed)

Add custom query methods to the appropriate repository in `backend/src/main/java/com/example/demo/repository/`.

```java
// Example: Add to SupportTicketRepository.java
@Query("SELECT t.customer.id, COUNT(t) FROM SupportTicket t WHERE t.status IN :statuses AND t.createdAt >= :since GROUP BY t.customer.id")
List<Object[]> countOpenTicketsByCustomerSince(@Param("statuses") List<SupportTicket.Status> statuses,
        @Param("since") Instant since);
```

**Conventions:**

- Return `List<Object[]>` for aggregation queries
- Use `@Param` annotations for all query parameters
- Import `java.time.Instant` for date/time parameters

#### Step 3: Add Controller Endpoint

Add the endpoint to the appropriate controller in `backend/src/main/java/com/example/demo/controller/`.

**Structure:**

1. Add any new repository dependencies to the constructor
2. Define response DTO as inner record class
3. Add the endpoint method with `@GetMapping`

```java
// 1. Add repository to constructor (if new dependency)
private final UsageRecordRepository usageRecordRepository;

public DashboardController(..., UsageRecordRepository usageRecordRepository) {
    // ... existing assignments
    this.usageRecordRepository = usageRecordRepository;
}

// 2. Define DTOs as inner records (at bottom of class)
public record MyData(Long id, String name, int value) {
}

public record MyResponse(long totalCount, List<MyData> items) {
}

// 3. Add endpoint
@GetMapping("/my-endpoint")
public MyResponse getMyData() {
    log.info("Fetching my data");
    // Implementation
    log.debug("Found {} items", items.size());
    return new MyResponse(count, items);
}
```

**Conventions:**

- Use `log.info()` at method start, `log.debug()` for details
- Response field names use snake_case for frontend compatibility (Java records auto-convert)
- Keep business logic in Controller (no Service layer in this codebase)

### Phase 2: Frontend Integration

#### Step 4: Add TypeScript Interfaces

Add interfaces to `hooks/use-dashboard-stats.ts` (or create a new hook file):

```typescript
export interface MyData {
  id: number
  name: string
  value: number
}

export interface MyResponse {
  totalCount: number
  items: MyData[]
}
```

#### Step 5: Add State and Fetch Logic

```typescript
const [myData, setMyData] = useState<MyResponse | null>(null)

// In fetchStats callback, add to Promise.all:
const [, /* existing */ myDataRes] = await Promise.all([
  // ... existing fetches
  fetch("/api/dashboard/my-endpoint"),
])

setMyData(await myDataRes.json())

// Add to return object
return {
  // ... existing
  myData,
}
```

#### Step 6: Update UI Components

Add KPI cards, charts, or tables to the appropriate page (e.g., `app/dashboard/page.tsx`).

### Phase 3: Testing

#### Step 7: Backend Unit Test

Create test in `backend/src/test/java/com/example/demo/controller/`:

```java
@ExtendWith(MockitoExtension.class)
class MyFeatureTest {
    @Mock
    private CustomerRepository customerRepository;
    // ... other mocks

    private DashboardController controller;

    @BeforeEach
    void setUp() {
        controller = new DashboardController(/* inject mocks */);
    }

    @Test
    void myMethod_withCondition_returnsExpected() {
        // Arrange
        // Act
        // Assert
    }
}
```

#### Step 8: Frontend Test

Create test in `__tests__/` directory:

```typescript
vi.mock("@/hooks/use-dashboard-stats", () => ({
  useDashboardStats: vi.fn(),
}))

describe("My Feature", () => {
  it("renders expected content", async () => {
    const { useDashboardStats } = await import("@/hooks/use-dashboard-stats")
    vi.mocked(useDashboardStats).mockReturnValue({
      // ... mock data
    })

    const Page = (await import("@/app/dashboard/page")).default
    render(<Page />)

    expect(screen.getByText("Expected Text")).toBeInTheDocument()
  })
})
```

## Verification

Before completing, run these commands:

```bash
# Backend compilation and tests
pnpm test:backend

# Frontend type checking and tests
pnpm lint
pnpm test:frontend

# Full test suite
pnpm test
```

Verify:

- [ ] Backend compiles without errors
- [ ] All existing tests pass
- [ ] New tests cover the business logic
- [ ] API returns expected JSON structure
- [ ] Frontend displays data correctly
