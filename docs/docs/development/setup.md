---
sidebar_position: 1
---

# Development Setup

## Prerequisites

- **Node.js 22+** - JavaScript runtime
- **Java 25+** - Backend runtime
- **pnpm** - Package manager
- **Python 3** - For database seeding

## Local Development

### 1. Install Dependencies

```bash
pnpm install
```

### 2. Seed Database

The Python seeder creates production-like data:

```bash
pnpm setup:db
```

| Entity          | Count    |
| --------------- | -------- |
| Plans           | 8        |
| Customers       | 500      |
| Devices         | 600      |
| Usage Records   | ~220,000 |
| Support Tickets | 250      |

### 3. Start Development Servers

```bash
# Start both frontend and backend
pnpm dev

# Or separately
pnpm dev:frontend  # Port 3000
pnpm dev:backend   # Port 8080
pnpm dev:docs      # Port 3001
```

## Dev Container

For a consistent environment with all tools pre-installed:

```bash
pnpm dc:up      # Start container
pnpm dc:shell   # Open shell
pnpm dc:dev     # Run dev servers
pnpm dc:test    # Run tests
pnpm dc:lint    # Run linters
```

## Available Scripts

| Script          | Description                 |
| --------------- | --------------------------- |
| `pnpm dev`      | Start all dev servers       |
| `pnpm build`    | Build frontend              |
| `pnpm test`     | Run all tests               |
| `pnpm lint`     | Lint frontend               |
| `pnpm format`   | Format all code             |
| `pnpm setup:db` | Seed database               |
| `pnpm reset:db` | Delete and re-seed database |

## IDE Setup

### VS Code Extensions

Recommended extensions (auto-installed in dev container):

- ESLint
- Prettier
- Tailwind CSS IntelliSense
- Java Extension Pack
- Spring Boot Tools

### Settings

```json
{
  "editor.formatOnSave": true,
  "editor.defaultFormatter": "esbenp.prettier-vscode",
  "[java]": {
    "editor.defaultFormatter": "redhat.java"
  }
}
```

## Pre-commit Hooks

Husky runs lint-staged on every commit:

- ESLint + Prettier for TypeScript files
- Prettier for JSON, Markdown, CSS
