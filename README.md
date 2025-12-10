# Demo Template

A full-stack application with a Next.js frontend and FastAPI backend.

## Tech Stack

**Frontend:**
- Next.js 16 with App Router
- React 19
- Tailwind CSS 4
- shadcn/ui components
- TypeScript

**Backend:**
- FastAPI
- SQLAlchemy (SQLite)
- Pydantic

## Getting Started

### Prerequisites

- Node.js 18+
- Python 3.9+
- pnpm

### Setup

```bash
# Install all dependencies (frontend + backend)
pnpm setup
```

Or manually:

```bash
# Frontend
pnpm install

# Backend
cd backend
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

### Development

Run both frontend and backend:

```bash
pnpm dev
```

Or run separately:

```bash
# Frontend (http://localhost:3000)
pnpm dev:frontend

# Backend (http://localhost:8000)
cd backend
source venv/bin/activate
uvicorn main:app --reload
```

### Testing

```bash
# Run all tests
pnpm test

# Frontend only
pnpm test:frontend

# Backend only
pnpm test:backend
```

### Linting

```bash
pnpm lint
```

## Project Structure

```
├── app/                    # Next.js pages and layouts
│   ├── dashboard/          # Dashboard pages
│   │   └── items/          # Items CRUD page
│   └── page.tsx            # Landing page
├── backend/                # FastAPI backend
│   ├── main.py             # API routes
│   ├── models.py           # SQLAlchemy models
│   ├── schemas.py          # Pydantic schemas
│   └── database.py         # Database config
├── components/             # React components
│   └── ui/                 # shadcn/ui components
├── hooks/                  # Custom React hooks
└── lib/                    # Utilities
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/health` | Health check |
| GET | `/api/items` | List all items |
| POST | `/api/items` | Create item |
| GET | `/api/items/{id}` | Get item |
| PUT | `/api/items/{id}` | Update item |
| DELETE | `/api/items/{id}` | Delete item |
