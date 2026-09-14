---
sidebar_position: 2
---

# API Reference

Base URL: `http://localhost:8080/api`

## Health

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/health` | Health check |

## Customers

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/customers` | List all customers |
| GET | `/customers/{id}` | Get customer by ID |
| POST | `/customers` | Create customer |
| PUT | `/customers/{id}` | Update customer |
| DELETE | `/customers/{id}` | Delete customer |

### Customer Object

```json
{
  "id": 1,
  "first_name": "John",
  "last_name": "Doe",
  "email": "john@example.com",
  "phone": "+1234567890",
  "plan_id": 1,
  "plan_name": "Premium",
  "status": "ACTIVE",
  "balance": "45.00",
  "activated_at": "2024-01-15T10:30:00Z",
  "created_at": "2024-01-01T00:00:00Z",
  "current_cycle_data_used_gb": 8.5,
  "data_limit_gb": 10,
  "data_usage_percentage": 85,
  "data_usage_state": "AT_RISK"
}
```

Current-cycle data uses the half-open UTC calendar month: the first day at
`00:00:00Z` is included and the first day of the following month is excluded.
`data_usage_percentage` is omitted (`null`) for Unlimited plans, while
`current_cycle_data_used_gb` remains available. The semantic state is one of
`WITHIN_LIMIT`, `AT_RISK`, `OVER_LIMIT`, or `UNLIMITED`; 80% is inclusive for
`AT_RISK`, and 100% is inclusive for `OVER_LIMIT`.

## Plans

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/plans` | List all plans |
| GET | `/plans/active` | List active plans |
| GET | `/plans/{id}` | Get plan by ID |
| POST | `/plans` | Create plan |
| PUT | `/plans/{id}` | Update plan |
| DELETE | `/plans/{id}` | Delete plan |

## Devices

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/devices` | List all devices |
| GET | `/devices/{id}` | Get device by ID |
| POST | `/devices` | Create device |
| PUT | `/devices/{id}` | Update device |
| DELETE | `/devices/{id}` | Delete device |

## Usage Records

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/usage` | List usage records (paginated) |
| GET | `/usage/customer/{id}` | Get usage by customer |

### Query Parameters

- `page` - Page number (default: 0)
- `size` - Page size (default: 50, max: 100)
- `type` - Filter by type: `CALL`, `DATA`, `SMS`
- `customerId` - Filter by customer
- `dateFrom` - Filter from date (YYYY-MM-DD)
- `dateTo` - Filter to date (YYYY-MM-DD)

## Support Tickets

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/tickets` | List tickets (paginated) |
| GET | `/tickets/{id}` | Get ticket by ID |
| POST | `/tickets` | Create ticket |
| POST | `/tickets/reconcile-deadlines` | Raise resolution-overdue Low/Medium tickets to High |
| PUT | `/tickets/{id}` | Update ticket |
| DELETE | `/tickets/{id}` | Delete ticket |

### Query Parameters

- `page`, `size` - Pagination
- `priority` - `LOW`, `MEDIUM`, `HIGH`, `URGENT`
- `status` - `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`
- `customerId` - Filter by customer
- `deadlineState` - `ON_TRACK`, `DUE_SOON`, `ACKNOWLEDGEMENT_OVERDUE`, or `RESOLUTION_OVERDUE`
- `asOf` - Optional ISO-8601 instant used for reproducible projections

The deadline filter is applied before pagination. `totalElements`, `totalPages`,
and `hasNext` therefore describe only the matching rows. The response includes
the effective `as_of` instant, and each ticket includes:

```json
{
  "created_at": "2026-08-03T10:00:00Z",
  "acknowledged_at": "2026-08-04T10:00:00Z",
  "resolved_at": null,
  "priority_escalated_at": null,
  "age_hours": 337,
  "age_days": 14,
  "acknowledgement_due_at": "2026-08-05T10:00:00Z",
  "resolution_due_at": "2026-08-17T10:00:00Z",
  "deadline_state": "RESOLUTION_OVERDUE"
}
```

Age is derived from source timestamps and is never stored. Deadlines count
Monday through Friday in UTC; public holidays are not excluded. At exactly the
due instant a ticket is not overdue. `DUE_SOON` includes exactly 24 hours
of business time remaining, so weekend time does not shorten the due-soon
window. `RESOLUTION_OVERDUE` takes precedence over acknowledgement overdue.

`POST /tickets/reconcile-deadlines` invokes the same idempotent reconciliation
as the 15-minute scheduled job. It returns candidate and update counts. It
raises only Low or Medium resolution-overdue tickets, records
`priority_escalated_at` once, and never lowers Urgent. GET requests do not
perform reconciliation.

## Dashboard

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/dashboard/stats` | KPI summary |
| GET | `/dashboard/customers-by-plan` | Chart data |
| GET | `/dashboard/devices-by-status` | Chart data |
| GET | `/dashboard/tickets-by-status` | Chart data |
| GET | `/dashboard/revenue-by-plan` | Chart data |

`GET /dashboard/stats` includes `at_risk_customers` and
`over_limit_customers`. Both counts use the same current-cycle calculation as
the customer projection, count each customer once, and exclude Unlimited
plans.

The same response also includes `acknowledgement_overdue_tickets`,
`resolution_overdue_tickets`, `due_soon_tickets`, and `as_of`. These are
current active-ticket counts, not a monthly breach-rate denominator. Supply
the same `asOf` value to `/dashboard/stats` and `/tickets` to reconcile each
count to projected rows without changing the existing KPI meanings.

## Interactive Docs

For full interactive API documentation, visit:

**[Swagger UI](http://localhost:8080/swagger-ui.html)** (when backend is running)
