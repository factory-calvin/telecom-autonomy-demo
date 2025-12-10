#!/bin/bash
# Dev container helper script for local development
# Usage: ./scripts/dc.sh <command>

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
COMPOSE_FILE="$PROJECT_ROOT/.devcontainer/docker-compose.yml"
SERVICE="app"

cd "$PROJECT_ROOT"

case "${1:-}" in
  up)
    echo "Starting dev container..."
    docker compose -f "$COMPOSE_FILE" up -d
    echo "Dev container is running. Use './scripts/dc.sh shell' to enter."
    ;;
  down)
    echo "Stopping dev container..."
    docker compose -f "$COMPOSE_FILE" down
    ;;
  shell)
    echo "Entering dev container shell..."
    docker compose -f "$COMPOSE_FILE" exec $SERVICE bash
    ;;
  build)
    echo "Building dev container..."
    docker compose -f "$COMPOSE_FILE" build
    ;;
  rebuild)
    echo "Rebuilding dev container from scratch..."
    docker compose -f "$COMPOSE_FILE" build --no-cache
    ;;
  logs)
    docker compose -f "$COMPOSE_FILE" logs -f $SERVICE
    ;;
  setup)
    echo "Running setup in dev container..."
    docker compose -f "$COMPOSE_FILE" exec $SERVICE bash -c "cd /workspaces/app && pnpm install && pnpm setup:db"
    ;;
  dev)
    echo "Starting development servers in container..."
    docker compose -f "$COMPOSE_FILE" exec $SERVICE bash -c "cd /workspaces/app && pnpm dev"
    ;;
  dev:frontend)
    echo "Starting frontend dev server in container..."
    docker compose -f "$COMPOSE_FILE" exec $SERVICE bash -c "cd /workspaces/app && pnpm dev:frontend"
    ;;
  dev:backend)
    echo "Starting backend dev server in container..."
    docker compose -f "$COMPOSE_FILE" exec $SERVICE bash -c "cd /workspaces/app && pnpm dev:backend"
    ;;
  test)
    echo "Running tests in container..."
    docker compose -f "$COMPOSE_FILE" exec $SERVICE bash -c "cd /workspaces/app && pnpm test"
    ;;
  lint)
    echo "Running linters in container..."
    docker compose -f "$COMPOSE_FILE" exec $SERVICE bash -c "cd /workspaces/app && pnpm lint"
    ;;
  format)
    echo "Running formatters in container..."
    docker compose -f "$COMPOSE_FILE" exec $SERVICE bash -c "cd /workspaces/app && pnpm format"
    ;;
  reset:db)
    echo "Resetting database in container..."
    docker compose -f "$COMPOSE_FILE" exec $SERVICE bash -c "cd /workspaces/app && pnpm reset:db"
    ;;
  exec)
    shift
    docker compose -f "$COMPOSE_FILE" exec $SERVICE bash -c "cd /workspaces/app && $*"
    ;;
  *)
    echo "Dev Container Helper Script"
    echo ""
    echo "Usage: ./scripts/dc.sh <command>"
    echo ""
    echo "Container Management:"
    echo "  up          Start the dev container"
    echo "  down        Stop the dev container"
    echo "  shell       Open a shell in the container"
    echo "  build       Build the dev container image"
    echo "  rebuild     Rebuild container from scratch (no cache)"
    echo "  logs        View container logs"
    echo ""
    echo "Development:"
    echo "  setup       Install all dependencies and seed database"
    echo "  dev         Start both frontend and backend servers"
    echo "  dev:frontend  Start frontend server only"
    echo "  dev:backend   Start backend server only"
    echo ""
    echo "Quality:"
    echo "  test        Run all tests"
    echo "  lint        Run linters"
    echo "  format      Run formatters"
    echo ""
    echo "Database:"
    echo "  reset:db    Reset and reseed the database"
    echo ""
    echo "Custom:"
    echo "  exec <cmd>  Run any command in the container"
    echo ""
    echo "Example: ./scripts/dc.sh exec pnpm add lodash"
    ;;
esac
