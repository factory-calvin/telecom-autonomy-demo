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
})
```

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
// stats: { active_customers, monthly_revenue, open_tickets, devices_in_use }
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
