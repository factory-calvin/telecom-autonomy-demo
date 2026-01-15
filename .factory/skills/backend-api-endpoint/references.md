# References

Key files to reference when implementing backend API endpoints.

## Backend

### Controllers

- `backend/src/main/java/com/example/demo/controller/DashboardController.java` - Dashboard metrics and aggregations
- `backend/src/main/java/com/example/demo/controller/CustomerController.java` - CRUD endpoint pattern

### Repositories

- `backend/src/main/java/com/example/demo/repository/CustomerRepository.java`
- `backend/src/main/java/com/example/demo/repository/SupportTicketRepository.java`
- `backend/src/main/java/com/example/demo/repository/UsageRecordRepository.java`

### Models

- `backend/src/main/java/com/example/demo/model/Customer.java`
- `backend/src/main/java/com/example/demo/model/SupportTicket.java`
- `backend/src/main/java/com/example/demo/model/UsageRecord.java`

### Tests

- `backend/src/test/java/com/example/demo/controller/ChurnRiskCalculationTest.java` - Unit test pattern

## Frontend

### Hooks

- `hooks/use-dashboard-stats.ts` - Dashboard data fetching pattern
- `hooks/use-customers.ts` - CRUD hook pattern

### Pages

- `app/dashboard/page.tsx` - KPI cards and charts
- `app/customers/page.tsx` - Table page pattern

### Tests

- `__tests__/dashboard/churn-risk.test.tsx` - Dashboard feature test pattern
- `__tests__/components/logo.test.tsx` - Component test pattern

## Documentation

- `AGENTS.md` - Project conventions and API endpoint list
- `docs/backend/api.md` - REST API reference (if exists)
