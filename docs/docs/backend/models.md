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
| quantity | BigDecimal | Amount used |
| cost | BigDecimal | Calculated cost |
| recordedAt | Instant | Usage timestamp |

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
| resolvedAt | Instant | Resolution timestamp |

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
