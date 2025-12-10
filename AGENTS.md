# AGENTS.md

Instructions for AI agents working on this codebase.

## Project Overview

FactoryFone Admin Portal - A telecom user administration system with:

- **Frontend**: Next.js 16 (App Router) + React 19 + Tailwind CSS 4 + shadcn/ui + Recharts
- **Backend**: Spring Boot 4.0 + Java 25 + Gradle + SQLite

## Application Domain

This is a telecom admin portal for managing:

- **Customers** - Subscriber accounts with plans, status, and balance
- **Plans** - Service plans (Basic, Standard, Premium, Unlimited, Family)
- **Devices** - Device/SIM inventory with assignment tracking
- **Usage Records** - Call, data, and SMS usage history
- **Support Tickets** - Customer service ticket management

## Code Conventions

### Frontend

- Use TypeScript for all files
- Components go in `components/`, pages in `app/`
- Use shadcn/ui components from `components/ui/`
- Custom hooks go in `hooks/` (e.g., `use-customers.ts`, `use-plans.ts`)
- Use `@/` path alias for imports
- Client components must have `"use client"` directive
- Table components follow pattern: `components/<entity>-table.tsx`
- Charts use Recharts via shadcn/ui chart component

### Backend

- Java code is in `backend/src/main/java/com/example/demo/`
- Package structure: `model/`, `repository/`, `controller/`
- Use `@RestController` for API endpoints
- API routes prefixed with `/api/` (e.g., `/api/customers`, `/api/plans`)
- Use JPA entities with SQLite database (`app.db`)
- Data seeder in `DataSeeder.java` populates demo data on startup

## Data Model

| Entity        | Key Fields                                                        |
| ------------- | ----------------------------------------------------------------- |
| Plan          | name, monthlyPrice, dataLimitGb, minutesLimit, smsLimit, isActive |
| Customer      | firstName, lastName, email, phone, plan, status, balance          |
| Device        | imei, model, simNumber, customer, status                          |
| UsageRecord   | customer, type (CALL/DATA/SMS), quantity, cost, recordedAt        |
| SupportTicket | customer, subject, description, priority, status                  |

## API Endpoints

| Endpoint                               | Description               |
| -------------------------------------- | ------------------------- |
| `GET /api/health`                      | Health check              |
| `GET/POST/PUT/DELETE /api/customers`   | Customer CRUD             |
| `GET/POST/PUT/DELETE /api/plans`       | Plan CRUD                 |
| `GET/POST/PUT/DELETE /api/devices`     | Device CRUD               |
| `GET /api/usage`                       | Usage records (read-only) |
| `GET/POST/PUT/DELETE /api/tickets`     | Support ticket CRUD       |
| `GET /api/dashboard/stats`             | Dashboard KPIs            |
| `GET /api/dashboard/customers-by-plan` | Chart data                |
| `GET /api/dashboard/devices-by-status` | Chart data                |
| `GET /api/dashboard/tickets-by-status` | Chart data                |
| `GET /api/dashboard/revenue-by-plan`   | Chart data                |

## Running Commands

### Dev Container (Recommended)

Use dev containers for consistent environment with all dependencies pre-installed:

```bash
# Container management
pnpm dc:up            # Start dev container
pnpm dc:down          # Stop dev container
pnpm dc:shell         # Open shell in container

# Development (in container)
pnpm dc:setup         # Install deps + seed database
pnpm dc:dev           # Both frontend and backend
pnpm dc:test          # Run all tests
pnpm dc:lint          # Run linters
pnpm dc:format        # Run formatters

# Custom commands
pnpm dc exec <cmd>    # Run any command in container
```

### Local Development (requires Node.js 22, Java 25, Python 3)

```bash
# Setup
pnpm setup            # Install deps + seed database
pnpm setup:db         # Run Python seeder only
pnpm reset:db         # Delete DB + re-seed

# Development
pnpm dev              # Both frontend and backend
pnpm dev:frontend     # Frontend only (port 3000)
pnpm dev:backend      # Backend only (port 8080)

# Building
pnpm build            # Build frontend
pnpm build:backend    # Build backend JAR

# Testing
pnpm test             # All tests
pnpm test:frontend    # Vitest
pnpm test:backend     # Gradle test

# Linting
pnpm lint             # ESLint + TypeScript
```

## Database Seeding

Production-like data is generated via Python script (`scripts/seed-database.py`):

| Entity          | Count | Notes                                                                                |
| --------------- | ----- | ------------------------------------------------------------------------------------ |
| Plans           | 8     | Basic, Standard, Premium, Unlimited, Family, Business Starter, Business Pro, Student |
| Customers       | 500   | Realistic names, emails, phones via faker                                            |
| Devices         | 600   | Unique IMEIs and SIM numbers                                                         |
| Usage Records   | ~220k | 90 days of data with peak hour patterns                                              |
| Support Tickets | 250   | Various priorities and statuses                                                      |

The Python seeder takes precedence over the Java `DataSeeder.java` (which only runs if the database is empty).

## Adding UI Components

Use shadcn CLI to add components:

```bash
pnpm dlx shadcn@latest add <component-name>
```

## File Structure

```
app/
├── dashboard/page.tsx      # KPI cards + charts
├── customers/page.tsx      # Customer management
├── plans/page.tsx          # Plan management
├── devices/page.tsx        # Device inventory
├── usage/page.tsx          # Usage records
└── tickets/page.tsx        # Support tickets

hooks/
├── use-customers.ts
├── use-plans.ts
├── use-devices.ts
├── use-usage.ts
├── use-tickets.ts
└── use-dashboard-stats.ts

components/
├── customers-table.tsx
├── plans-table.tsx
├── devices-table.tsx
├── usage-table.tsx
├── tickets-table.tsx
└── app-sidebar.tsx

backend/src/main/java/com/example/demo/
├── model/                  # JPA entities
├── repository/             # Spring Data repositories
├── controller/             # REST controllers
└── DataSeeder.java         # Fallback demo data seeder

scripts/
├── seed-database.py        # Production-like data seeder (Python + faker)
└── requirements.txt        # Python dependencies
```

## Testing

- Frontend tests use Vitest + React Testing Library
- Test files: `__tests__/*.test.tsx`
- Backend tests use JUnit 5
- Test files: `backend/src/test/java/**/*Test.java`

## Documentation

Documentation is built with Docusaurus and located in the `docs/` directory.

### Running Docs

```bash
pnpm dev:docs     # Start docs server only (port 3001)
pnpm dev:all      # Start frontend, backend, and docs together
```

### Docs Structure

```
docs/
├── docs/
│   ├── getting-started.md      # Quick start guide
│   ├── architecture.md         # System architecture overview
│   ├── frontend/
│   │   ├── overview.md         # Frontend structure
│   │   ├── components.md       # UI components guide
│   │   └── hooks.md            # Custom hooks reference
│   ├── backend/
│   │   ├── overview.md         # Backend structure
│   │   ├── api.md              # REST API reference
│   │   └── models.md           # JPA entity documentation
│   └── development/
│       ├── setup.md            # Development environment setup
│       ├── testing.md          # Testing guide
│       └── deployment.md       # Deployment instructions
├── src/
│   └── pages/
│       └── index.tsx           # Landing page
├── docusaurus.config.ts        # Site configuration
└── sidebars.ts                 # Navigation structure
```

### API Documentation

The backend auto-generates OpenAPI/Swagger documentation:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

Available when the backend is running (`pnpm dev:backend` or `pnpm dev`).
