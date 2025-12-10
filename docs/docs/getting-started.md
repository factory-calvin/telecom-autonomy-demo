---
sidebar_position: 1
---

# Getting Started

FactoryFone Admin Portal is a telecom user administration system built with Next.js and Spring Boot.

## Prerequisites

- Node.js 22+
- Java 25+
- pnpm
- Python 3 (for database seeding)

## Quick Start

```bash
# Clone the repository
git clone https://github.com/Factory-Academy/NextJS-Springboot-Demo.git
cd NextJS-Springboot-Demo

# Install dependencies and seed database
pnpm setup

# Start development servers
pnpm dev
```

This starts:

- **Frontend**: http://localhost:3000 (Next.js)
- **Backend**: http://localhost:8080 (Spring Boot)
- **API Docs**: http://localhost:8080/swagger-ui.html

## Using Dev Containers

For a consistent development environment:

```bash
pnpm dc:up      # Start dev container
pnpm dc:shell   # Open shell in container
pnpm dc:dev     # Run both servers in container
```

## Project Structure

```
├── app/                    # Next.js pages (App Router)
├── components/             # React components
├── hooks/                  # Custom React hooks
├── lib/                    # Utilities
├── backend/                # Spring Boot application
│   └── src/main/java/
├── docs/                   # Documentation (Docusaurus)
└── scripts/                # Utility scripts
```

## Next Steps

- [Architecture Overview](./architecture) - Understand the system design
- [Frontend Guide](./frontend/overview) - Learn about the React frontend
- [Backend Guide](./backend/overview) - Explore the Spring Boot API
