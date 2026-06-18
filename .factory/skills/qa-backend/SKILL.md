---
name: qa-backend
description: >
  QA sub-skill for the FactoryFone Spring Boot REST API. Hits each configured
  endpoint with curl, asserts HTTP status, content-type, and minimal payload
  shape (arrays for list endpoints, object with numeric fields for dashboard
  stats). Invoked by the qa orchestrator.
---

# qa-backend

Backend API QA sub-skill. The orchestrator passes you the run dir, base URL (`http://localhost:8080`), and a subset of flows. It also tells you which flows to run based on the git diff.

## Testing target

This project does NOT use preview deployments. Always test against a locally-running backend:

- The orchestrator/CI runs `pnpm dev:backend` (which calls `./gradlew bootRun`) and polls `/api/health` to 200 before invoking this skill.
- Default base URL: `http://localhost:8080`.
- Never fall back to a remote URL; backend is local-only in this project.

## Authentication

None. No credentials, headers, or tokens. Endpoints are public.

## Tooling

Use `curl` from a small bash or Python wrapper. Do NOT spin up Spring's test runner. The point is to verify the running service from outside.

A reference runner script may be created at `.factory/skills/qa-backend/run.sh` that takes `<run_dir> <base_url>` and reads the endpoint list from `config.yaml`.

## Available test flows (menu)

The orchestrator picks flows based on the git diff. Do not run all flows blindly.

### backend.health

**When to run:** ALWAYS, when this app is affected. This is the gating pre-flight.

- `GET /api/health` → 200, body contains `"status":"UP"` or HTTP 200 with empty body (Spring Actuator default).

### backend.customers_crud

**When to run:** changes under `backend/src/main/java/com/example/demo/controller/CustomerController.java`, `model/Customer.java`, `repository/CustomerRepository.java`.

1. `GET /api/customers` → 200, JSON array, length > 0.
2. `GET /api/customers/{first_id_from_list}` → 200, JSON object with `firstName`, `lastName`, `email`.
3. Negative: `GET /api/customers/999999999` → 404.

### backend.plans_crud

**When to run:** changes to `controller/PlanController.java`, `model/Plan.java`, `repository/PlanRepository.java`.

1. `GET /api/plans` → 200, JSON array, length == 8 (seeded count).
2. `GET /api/plans/{first_id}` → 200, JSON object with `name`, `monthlyPrice`.
3. Negative: `GET /api/plans/999999` → 404.

### backend.devices_crud

**When to run:** changes to `controller/DeviceController.java`, `model/Device.java`, `repository/DeviceRepository.java`.

1. `GET /api/devices` → 200, JSON array, length > 0.
2. `GET /api/devices/{first_id}` → 200, JSON object with `imei`, `model`, `simNumber`.

### backend.usage

**When to run:** changes to `controller/UsageController.java`, `model/UsageRecord.java`, `repository/UsageRepository.java`.

1. `GET /api/usage` → 200, JSON array.
2. Verify length > 0 and each record has `customer`, `type` (one of `CALL`/`DATA`/`SMS`), `quantity`, `cost`.

### backend.tickets_crud

**When to run:** changes to `controller/TicketController.java`, `model/SupportTicket.java`, `repository/SupportTicketRepository.java`.

1. `GET /api/tickets` → 200, JSON array, length > 0.
2. `GET /api/tickets/{first_id}` → 200, JSON object with `subject`, `description`, `priority`, `status`.

### backend.dashboard

**When to run:** changes to `controller/DashboardController.java` or any endpoint under `/api/dashboard/*`.

1. `GET /api/dashboard/stats` → 200, JSON object with at least one numeric field.
2. `GET /api/dashboard/customers-by-plan` → 200, JSON array of objects.
3. `GET /api/dashboard/devices-by-status` → 200, JSON array of objects.
4. `GET /api/dashboard/tickets-by-status` → 200, JSON array of objects.
5. `GET /api/dashboard/revenue-by-plan` → 200, JSON array of objects.

### backend.swagger

**When to run:** changes that add/rename a controller method, change request/response shape, or modify `@Operation` annotations.

1. `GET /v3/api-docs` → 200, JSON.
2. `GET /swagger-ui.html` → 200, content-type contains `text/html`.

### backend.smoke (full pass)

**When to run:** when the orchestrator invokes a full smoke pass with no diff context.

- Run all of the above in order.

## Per-endpoint contract

For every request:

1. Save the raw response body to `qa-results/logs/backend-<METHOD>-<slug>.json` (truncate to 64KB).
2. Record HTTP status code and elapsed seconds.
3. Apply the per-flow assertion above.
4. Latency > 5s is a `:warning: FLAKY` warning, not a FAIL (Spring cold start is slow).

## Output

Persist a structured result block at `qa-results/results/backend.json`:

```json
{
  "app": "backend",
  "base_url": "http://localhost:8080",
  "endpoints": [
    {
      "method": "GET",
      "path": "/api/customers",
      "status": 200,
      "elapsed_seconds": 0.234,
      "body_log": "logs/backend-GET-api-customers.json",
      "checks": [
        { "id": "status_2xx", "result": "pass" },
        { "id": "is_array", "result": "pass" },
        { "id": "non_empty", "result": "pass" }
      ]
    }
  ],
  "flows": [{ "id": "backend.health", "result": "pass", "notes": "" }]
}
```

## Known Failure Modes

1. **Cold start.** First request after `gradle bootRun` can take 10-15s. Always run `backend.health` first and poll until 200.
2. **SQLite locked during reset.** If `pnpm reset:db` is run while the backend is still holding a connection, the new DB may be empty. Always `kill` the backend before `pnpm reset:db`, then restart it.
3. **DataSeeder vs Python seeder mismatch.** If the Python seeder fails, `backend/src/main/java/com/example/demo/DataSeeder.java` runs as a fallback and produces FEWER records (not the production-like 500 customers / 600 devices / 220k usage rows). If endpoint counts are unexpectedly small, suspect this and re-run `pnpm setup:db` manually.
4. **OpenTelemetry exporter timeout.** `io.opentelemetry.exporter-otlp` warnings in logs are harmless when no collector is running. Ignore them.
5. **Empty `/api/health` body.** Spring Actuator returns `{"status":"UP"}` by default but some configs return empty body with 200. The pass criterion is HTTP 200, not body content.

## CI notes

- The workflow runs `./gradlew build -x test` (skipping unit tests) before `pnpm dev:backend`. This makes startup faster.
- Java 25 + Gradle is installed by the workflow.
- Use `curl -sS --max-time 30` for all requests.
- All response logs under `qa-results/logs/` are uploaded as a build artifact.
