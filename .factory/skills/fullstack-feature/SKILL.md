---
name: fullstack-feature
description: Implement a full-stack feature with FastAPI backend endpoint and React frontend integration. Use when adding new CRUD operations, data models, or UI components that interact with the API.
---

# Full-Stack Feature Implementation

## When to Use
- Adding a new data model/entity to the application
- Creating CRUD operations for a resource
- Building UI components that interact with the backend API
- Extending existing features with new fields or behaviors

## Project Structure

```
backend/
  models.py      # SQLAlchemy models
  schemas.py     # Pydantic schemas for request/response
  main.py        # FastAPI routes
  database.py    # Database connection
  tests/         # Pytest tests

components/      # React components
app/             # Next.js pages and layouts
__tests__/       # Frontend tests
```

## Instructions

### 1. Backend Model
Add SQLAlchemy model in `backend/models.py`:
- Define table name and columns
- Add relationships if needed
- Include timestamps (created_at, updated_at)

### 2. Pydantic Schemas
Add schemas in `backend/schemas.py`:
- `{Entity}Base` - shared fields
- `{Entity}Create` - fields for creation
- `{Entity}Update` - fields for updates  
- `{Entity}` - full response model with `from_attributes = True`

### 3. API Endpoints
Add routes in `backend/main.py`:
- `GET /api/{entities}` - list all
- `POST /api/{entities}` - create new
- `GET /api/{entities}/{id}` - get by ID
- `PUT /api/{entities}/{id}` - update
- `DELETE /api/{entities}/{id}` - delete

### 4. Backend Tests
Add tests in `backend/tests/test_{entity}.py`:
- Test each endpoint
- Test error cases (404, validation)
- Use the `client` fixture from conftest.py

### 5. Frontend Components
Create React components in `components/`:
- List view component
- Form component (create/edit)
- Use shadcn/ui components where applicable

### 6. Frontend Integration
- Add API calls using fetch or a client library
- Handle loading and error states
- Add to navigation if needed

## Verification

Before completing, run:
```bash
pnpm lint        # Must pass
pnpm test        # Must pass
```

Test manually:
- Backend: http://localhost:8000/docs (Swagger UI)
- Frontend: http://localhost:3000

## Constraints

- Follow existing code patterns in the project
- Use TypeScript for all frontend code
- Use Pydantic models for all API request/response types
- Add proper error handling on both frontend and backend
- Do not modify unrelated files
