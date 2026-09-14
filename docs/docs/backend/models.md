---
sidebar_position: 3
---

# Data Models

## Plan

Service plans available to customers.

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| name | String | Plan name |
| monthlyPrice | BigDecimal | Monthly cost |
| dataLimitGb | Integer | Data limit in GB |
| minutesLimit | Integer | Voice minutes limit |
| smsLimit | Integer | SMS limit |
| isActive | Boolean | Whether plan is available |

**Example Plans**: Basic, Standard, Premium, Unlimited, Family, Business Starter, Business Pro, Student

## Customer

Subscriber accounts.

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| firstName | String | First name |
| lastName | String | Last name |
| email | String | Email address (unique) |
| phone | String | Phone number |
| plan | Plan | Associated service plan |
| status | Enum | ACTIVE, SUSPENDED, CANCELLED |
| balance | BigDecimal | Account balance |
| activatedAt | Instant | Activation timestamp |
| createdAt | Instant | Creation timestamp |

## Device

Device and SIM inventory.

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| imei | String | Device IMEI (unique) |
| model | String | Device model |
| simNumber | String | SIM card number (unique) |
| customer | Customer | Assigned customer (nullable) |
| status | Enum | AVAILABLE, ASSIGNED, MAINTENANCE, RETIRED |
| assignedAt | Instant | Assignment timestamp |
| createdAt | Instant | Creation timestamp |

## UsageRecord

Call, data, and SMS usage history.

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| customer | Customer | Associated customer |
| type | Enum | CALL, DATA, SMS |
| quantity | BigDecimal | Amount used; DATA records use decimal MB (1000 MB = 1 GB) |
| cost | BigDecimal | Calculated cost |
| recordedAt | Instant | Usage timestamp |

Current-cycle usage is the sum of DATA records in the half-open UTC calendar
month. CALL, SMS, and records outside that interval do not contribute. For a
10 GB plan, 8,500 MB produces 8.5 GB, 85%, and `AT_RISK`; 10,200 MB produces
10.2 GB, 102%, and `OVER_LIMIT`. Unlimited plans expose used GB without a
percentage or threshold count.

## SupportTicket

Customer service tickets.

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| customer | Customer | Associated customer |
| subject | String | Ticket subject |
| description | String | Issue description |
| priority | Enum | LOW, MEDIUM, HIGH, URGENT |
| status | Enum | OPEN, IN_PROGRESS, RESOLVED, CLOSED |
| createdAt | Instant | Creation timestamp |
| acknowledgedAt | Instant | First Open-to-In Progress transition, retained once written |
| resolvedAt | Instant | Resolution timestamp |
| priorityEscalatedAt | Instant | First idempotent overdue-priority escalation |

Ticket age and deadline state are projections, not stored fields. The
acknowledgement deadline is two business days after `createdAt`, and the
resolution deadline is ten business days after `createdAt`. Business days are
Monday through Friday in UTC. Public holidays are outside the current policy.

For example, a ticket created Monday 3 August 2026 at 10:00 UTC has an
acknowledgement deadline of Wednesday 5 August at 10:00 UTC and a resolution
deadline of Monday 17 August at 10:00 UTC. A ticket created Friday 31 July at
15:00 UTC has an acknowledgement deadline of Tuesday 4 August at 15:00 UTC,
because Saturday and Sunday do not consume SLA time.

State precedence is `RESOLUTION_OVERDUE`, `ACKNOWLEDGEMENT_OVERDUE`,
`DUE_SOON`, then `ON_TRACK`. A deadline becomes overdue only after its exact
instant. Due soon includes exactly 24 hours of business time remaining, so
weekend time does not shorten that window. Resolved and
Closed tickets retain their source timestamps for audit, but do not appear in
current attention counts. Reopening either status clears stale `resolvedAt`,
while keeping `acknowledgedAt` and the original resolution clock.

## Entity Relationships

```
Plan (1) ◀─────────── (N) Customer
                            │
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
Device (N)          UsageRecord (N)    SupportTicket (N)
```
