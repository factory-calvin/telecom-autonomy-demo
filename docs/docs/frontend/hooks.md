---
sidebar_position: 3
---

# Hooks

Custom hooks in `hooks/` manage data fetching and state.

## Available Hooks

### useCustomers

```tsx
const { customers, loading, error, refresh, createCustomer, updateCustomer, deleteCustomer } =
  useCustomers()
```

Each `Customer` includes `current_cycle_data_used_gb`, `data_limit_gb`,
`data_usage_percentage`, and `data_usage_state`. The semantic state is one of
`WITHIN_LIMIT`, `AT_RISK`, `OVER_LIMIT`, or `UNLIMITED`; consumers must use that state for
filtering instead of recalculating thresholds.

### usePlans

```tsx
const {
  plans,
  activePlans, // Only active plans
  loading,
  error,
  createPlan,
  updatePlan,
  deletePlan,
} = usePlans()
```

### useDevices

```tsx
const { devices, loading, error, createDevice, updateDevice, deleteDevice } = useDevices()
```

### useTickets

Supports filtering and pagination:

```tsx
const {
  tickets,
  loading,
  loadingMore,
  hasNext,
  totalElements,
  loadMore,
  refresh,
  createTicket,
  updateTicket,
  deleteTicket,
} = useTickets({
  priority: "HIGH",
  status: "OPEN",
  customerId: 123,
  deadlineState: "RESOLUTION_OVERDUE",
})
```

Each ticket includes API-derived `age_hours`, `age_days`, exact acknowledgement and
resolution due timestamps, and one semantic `deadline_state`: `ON_TRACK`, `DUE_SOON`,
`ACKNOWLEDGEMENT_OVERDUE`, or `RESOLUTION_OVERDUE`. Pass `deadlineState` to filter through
`GET /api/tickets`; clients must not recalculate business-day deadlines.

### useUsage

Supports filtering and pagination:

```tsx
const { records, loading, loadingMore, hasNext, loadMore } = useUsage({
  type: "DATA",
  customerId: 123,
  dateFrom: "2024-01-01",
  dateTo: "2024-12-31",
})
```

### useDashboardStats

```tsx
const { stats, loading, error } = useDashboardStats()
// stats also includes at_risk_customers and over_limit_customers for the current cycle
// and acknowledgement_overdue_tickets, resolution_overdue_tickets, due_soon_tickets,
// and as_of for current support-ticket attention counts
```

## Hook Patterns

### Optimistic Updates

Hooks update local state immediately while API calls complete:

```tsx
const deleteCustomer = useCallback(async (id: number) => {
  // Update UI immediately
  setCustomers((prev) => prev.filter((c) => c.id !== id))

  // Then call API
  await fetch(`/api/customers/${id}`, { method: "DELETE" })
}, [])
```

### Structured Logging

Hooks use Pino for observability:

```tsx
import { createLogger } from "@/lib/logger"

const log = createLogger("useCustomers")

log.info({ customerId: id }, "Deleting customer")
```
