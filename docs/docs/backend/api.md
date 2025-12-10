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
  "created_at": "2024-01-01T00:00:00Z"
}
```

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
| PUT | `/tickets/{id}` | Update ticket |
| DELETE | `/tickets/{id}` | Delete ticket |

### Query Parameters

- `page`, `size` - Pagination
- `priority` - `LOW`, `MEDIUM`, `HIGH`, `URGENT`
- `status` - `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`
- `customerId` - Filter by customer

## Dashboard

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/dashboard/stats` | KPI summary |
| GET | `/dashboard/customers-by-plan` | Chart data |
| GET | `/dashboard/devices-by-status` | Chart data |
| GET | `/dashboard/tickets-by-status` | Chart data |
| GET | `/dashboard/revenue-by-plan` | Chart data |

## Interactive Docs

For full interactive API documentation, visit:

**[Swagger UI](http://localhost:8080/swagger-ui.html)** (when backend is running)
