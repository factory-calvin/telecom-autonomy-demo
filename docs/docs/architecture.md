---
sidebar_position: 2
---

# Architecture

FactoryFone follows a modern full-stack architecture with clear separation between frontend and backend.

## System Overview

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Next.js App   │────▶│  Spring Boot    │────▶│     SQLite      │
│   (Port 3000)   │     │  (Port 8080)    │     │   (app.db)      │
└─────────────────┘     └─────────────────┘     └─────────────────┘
        │                       │
        │                       ▼
        │               ┌─────────────────┐
        │               │  Swagger UI     │
        │               │  /swagger-ui    │
        │               └─────────────────┘
        ▼
┌─────────────────┐
│   Docusaurus    │
│   (Port 3001)   │
└─────────────────┘
```

## Tech Stack

### Frontend

| Technology     | Purpose                         |
| -------------- | ------------------------------- |
| Next.js 16     | React framework with App Router |
| React 19       | UI library                      |
| Tailwind CSS 4 | Utility-first styling           |
| shadcn/ui      | Component library               |
| Recharts       | Data visualization              |
| Pino           | Structured logging              |

### Backend

| Technology      | Purpose               |
| --------------- | --------------------- |
| Spring Boot 4   | Java web framework    |
| Java 25         | Runtime               |
| Spring Data JPA | Database access       |
| SQLite          | Embedded database     |
| SLF4J/Logback   | Structured logging    |
| SpringDoc       | OpenAPI documentation |

## Data Model

```
┌──────────────┐       ┌──────────────┐
│     Plan     │◀──────│   Customer   │
├──────────────┤       ├──────────────┤
│ name         │       │ firstName    │
│ monthlyPrice │       │ lastName     │
│ dataLimitGb  │       │ email        │
│ minutesLimit │       │ status       │
│ smsLimit     │       │ balance      │
└──────────────┘       └──────┬───────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│    Device    │     │ UsageRecord  │     │SupportTicket │
├──────────────┤     ├──────────────┤     ├──────────────┤
│ imei         │     │ type         │     │ subject      │
│ model        │     │ quantity     │     │ description  │
│ simNumber    │     │ cost         │     │ priority     │
│ status       │     │ recordedAt   │     │ status       │
└──────────────┘     └──────────────┘     └──────────────┘
```

## API Design

All REST endpoints follow these conventions:

- **Base URL**: `/api`
- **Format**: JSON
- **Authentication**: None (demo app)
- **Pagination**: `?page=0&size=50`

See [API Reference](./backend/api) for full endpoint documentation.
