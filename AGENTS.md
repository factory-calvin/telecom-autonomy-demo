# AGENTS.md

Instructions for AI agents working on this codebase.

## Project Overview

This is a full-stack application with:
- **Frontend**: Next.js 16 (App Router) + React 19 + Tailwind CSS 4 + shadcn/ui
- **Backend**: FastAPI + SQLAlchemy + SQLite

## Code Conventions

### Frontend

- Use TypeScript for all files
- Components go in `components/`, pages in `app/`
- Use shadcn/ui components from `components/ui/`
- Custom hooks go in `hooks/`
- Use `@/` path alias for imports
- Client components must have `"use client"` directive
- Follow existing component patterns (see `components/items-table.tsx`)

### Backend

- Python code is in `backend/`
- Models in `models.py`, schemas in `schemas.py`, routes in `main.py`
- Use Pydantic for request/response validation
- SQLAlchemy for database operations
- API routes should be prefixed with `/api/`

## Running Commands

```bash
# Development
pnpm dev              # Both frontend and backend
pnpm dev:frontend     # Frontend only (port 3000)
pnpm dev:backend      # Backend only (port 8000)

# Testing
pnpm test             # All tests
pnpm test:frontend    # Vitest
pnpm test:backend     # Pytest

# Linting
pnpm lint             # ESLint + TypeScript + Ruff + MyPy
```

## Backend Virtual Environment

The backend uses a Python virtual environment at `backend/venv/`. To run backend commands manually:

```bash
cd backend
source venv/bin/activate
uvicorn main:app --reload
```

## Adding UI Components

Use shadcn CLI to add components:

```bash
pnpm dlx shadcn@latest add <component-name>
```

## File Structure Patterns

- Dashboard pages: `app/dashboard/<feature>/page.tsx`
- API hooks: `hooks/use-<resource>.ts`
- Feature components: `components/<feature>-*.tsx`

## Testing

- Frontend tests use Vitest + React Testing Library
- Test files: `__tests__/*.test.tsx`
- Backend tests use Pytest
