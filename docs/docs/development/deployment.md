---
sidebar_position: 3
---

# Deployment

## Building for Production

### Frontend

```bash
pnpm build
```

Output: `.next/` directory

### Backend

```bash
pnpm build:backend
# Or
cd backend && ./gradlew build
```

Output: `backend/build/libs/demo-0.0.1-SNAPSHOT.jar`

### Documentation

```bash
cd docs && pnpm build
```

Output: `docs/build/` directory

## Running Production Builds

### Start All Services

```bash
pnpm start
```

### Individual Services

```bash
# Frontend (requires build first)
pnpm start:frontend

# Backend
pnpm start:backend
# Or
java -jar backend/build/libs/*.jar
```

## Environment Variables

### Frontend

| Variable              | Description  | Default       |
| --------------------- | ------------ | ------------- |
| `NODE_ENV`            | Environment  | `development` |
| `NEXT_PUBLIC_API_URL` | API base URL | `/api`        |

### Backend

| Variable                | Description  | Default              |
| ----------------------- | ------------ | -------------------- |
| `SERVER_PORT`           | HTTP port    | `8080`               |
| `SPRING_DATASOURCE_URL` | Database URL | `jdbc:sqlite:app.db` |

## Docker (Optional)

### Build Images

```bash
# Frontend
docker build -t factoryfone-frontend .

# Backend
docker build -t factoryfone-backend ./backend
```

### Using Dev Container

The dev container provides a complete environment:

```bash
pnpm dc:up      # Start
pnpm dc:dev     # Run dev servers
pnpm dc:down    # Stop
```

## CI/CD Pipeline

GitHub Actions workflow (`.github/workflows/ci.yml`):

1. **Frontend Job**
   - Install dependencies
   - Check formatting
   - Lint
   - Test
   - Build

2. **Backend Job**
   - Setup Java 25
   - Check formatting
   - Lint (SpotBugs + PMD)
   - Test
   - Build

## Documentation Deployment

Docs are configured for GitHub Pages:

```bash
cd docs
pnpm build
pnpm deploy  # Deploys to gh-pages branch
```

URL: `https://factory-academy.github.io/NextJS-Springboot-Demo/`
